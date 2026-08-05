package com.pokelike.idle.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.AchievementType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.PendingReward
import com.pokelike.idle.domain.model.QuestPeriod
import com.pokelike.idle.domain.model.QuestType
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.RewardSource
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.ClaimQuestUseCase
import com.pokelike.idle.domain.usecases.PurchaseStreakProtectionUseCase
import com.pokelike.idle.domain.usecases.QuestClaimResult
import com.pokelike.idle.domain.usecases.StreakProtectionPurchaseResult
import com.pokelike.idle.manager.RewardManager
import com.pokelike.idle.ui.components.toRewardParts
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.NumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel des Ziele-Bildschirms.
 *
 * Quests und Achievements teilen sich einen Bildschirm. Zwei getrennte
 * Eintraege in der unteren Leiste waeren bei fuenf Zielen zu viel - Material
 * empfiehlt drei bis fuenf -, und inhaltlich gehoeren sie ohnehin zusammen:
 * beides sind Ziele mit Belohnung.
 */
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: GameRepository,
    private val claimQuest: ClaimQuestUseCase,
    private val purchaseStreakProtection: PurchaseStreakProtectionUseCase,
    private val rewardManager: RewardManager,
    private val numberFormatter: NumberFormatter,
    dispatchers: DispatcherProvider,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(GoalsTab.QUESTS)

    val uiState: StateFlow<GoalsUiState> = combine(
        repository.gameState,
        selectedTab,
    ) { state, tab ->
        buildUiState(state, tab)
    }
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = GoalsUiState(),
        )

    fun onTabSelected(tab: GoalsTab) {
        selectedTab.value = tab
    }

    /**
     * Holt die Belohnung einer Quest ab.
     *
     * Die Pruefung laeuft innerhalb von [GameRepository.update] und damit atomar
     * auf dem aktuellen Zustand. Anschliessend wird die Belohnung wie jede
     * andere ueber [RewardManager] gemeldet - der Spieler sieht dieselbe
     * Darstellung wie bei einem Achievement.
     *
     * @return `true`, wenn abgeholt wurde.
     */
    fun onClaim(quest: QuestType): Boolean {
        var claimedReward: ResourceBundle? = null

        repository.update { state ->
            when (val result = claimQuest(state, quest)) {
                is QuestClaimResult.Success -> {
                    claimedReward = result.reward
                    result.state
                }

                QuestClaimResult.NotComplete,
                QuestClaimResult.AlreadyClaimed,
                -> {
                    claimedReward = null
                    state
                }
            }
        }

        val reward = claimedReward ?: return false
        rewardManager.offer(
            PendingReward(source = RewardSource.Quest(quest), bundle = reward),
        )
        return true
    }

    /**
     * Kauft eine Ladung Serienschutz.
     *
     * Wie bei den Quests laeuft die Pruefung innerhalb von
     * [GameRepository.update] und damit atomar auf dem aktuellen Zustand. Ohne
     * das koennte zwischen Pruefung und Abbuchung ein Klick den Diamantenstand
     * veraendern.
     *
     * @return `true`, wenn gekauft wurde.
     */
    fun onBuyProtection(): Boolean {
        var bought = false

        repository.update { state ->
            when (val result = purchaseStreakProtection(state)) {
                is StreakProtectionPurchaseResult.Success -> {
                    bought = true
                    result.state
                }

                StreakProtectionPurchaseResult.NotAffordable,
                StreakProtectionPurchaseResult.AtMaximum,
                -> {
                    bought = false
                    state
                }
            }
        }

        return bought
    }

    private fun buildUiState(state: GameState, tab: GoalsTab): GoalsUiState {
        val questRows = QuestType.entries.map { quest ->
            val progress = state.quests.progressOf(quest, state)
            QuestRow(
                type = quest,
                progressText = "${numberFormatter.format(progress)} / " +
                    numberFormatter.format(quest.target),
                progressFraction = progress.ratioTo(quest.target).toFloat().coerceIn(0f, 1f),
                reward = quest.reward.toRewardParts(numberFormatter),
                isComplete = state.quests.isComplete(quest, state),
                isClaimed = state.quests.isClaimed(quest),
            )
        }

        val achievementRows = AchievementType.entries
            .map { achievement ->
                AchievementRow(
                    type = achievement,
                    reward = achievement.reward.toRewardParts(numberFormatter),
                    isUnlocked = achievement in state.achievements,
                )
            }
            // Erreichte nach hinten: Oben steht, was noch offen ist.
            .sortedBy { it.isUnlocked }

        return GoalsUiState(
            selectedTab = tab,
            streak = buildStreakRow(state),
            // Feste Reihenfolge statt der des Enums, damit taegliche Ziele
            // immer oben stehen - sie sind die, die taeglich neu erledigt
            // werden wollen.
            questsByPeriod = PERIOD_ORDER.associateWith { period ->
                questRows.filter { it.type.period == period }
            },
            achievements = achievementRows,
            unlockedCount = state.achievements.count,
            totalCount = AchievementType.entries.size,
            claimableCount = questRows.count { it.isComplete && !it.isClaimed },
        )
    }

    private fun buildStreakRow(state: GameState): StreakRow {
        val isFull = state.login.protectionCharges >= GameConfig.STREAK_PROTECTION_MAX_CHARGES
        val price = purchaseStreakProtection.price

        return StreakRow(
            streak = state.login.streak,
            longestStreak = state.login.longestStreak,
            protectionCharges = state.login.protectionCharges,
            protectionPrice = numberFormatter.format(price[ResourceType.DIAMONDS]),
            // Der Knopf ist nur dann aktiv, wenn der Kauf auch gelingen wuerde.
            // Ein Knopf, der nach dem Tippen eine Fehlermeldung zeigt, ist
            // schlechter als einer, der von vornherein ausgegraut ist.
            canBuyProtection = !isFull && state.resources.canAfford(price),
            isProtectionFull = isFull,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        val PERIOD_ORDER = listOf(QuestPeriod.DAILY, QuestPeriod.WEEKLY, QuestPeriod.LIFETIME)
    }
}
