package com.pokelike.idle.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.domain.model.DailyRewardType
import com.pokelike.idle.domain.model.GameSettings
import com.pokelike.idle.domain.model.OfflineProgress
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.RewardSource
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.repository.SettingsRepository
import com.pokelike.idle.manager.DailyRewardManager
import com.pokelike.idle.manager.GameSessionManager
import com.pokelike.idle.manager.RewardManager
import com.pokelike.idle.ui.components.DailyRewardUiState
import com.pokelike.idle.ui.components.RewardLineItem
import com.pokelike.idle.ui.components.RewardPart
import com.pokelike.idle.ui.components.toRewardParts
import com.pokelike.idle.ui.screens.goals.nameRes
import com.pokelike.idle.util.DurationFormatter
import com.pokelike.idle.util.NumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Zustand, den die Activity vor dem Aufbau der Oberflaeche kennen muss.
 *
 * @property settings Einstellungen. Bestimmen unter anderem die Farbgebung.
 * @property isReady Ob der Spielstand geladen ist. Solange nicht, bleibt der
 *   Splashscreen stehen.
 * @property offlineEarned Ertrag der Abwesenheit, formatiert. `null`, wenn
 *   nichts anzuzeigen ist.
 * @property offlineDuration Dauer der Abwesenheit, formatiert.
 * @property offlineWasCapped Ob die Obergrenze gegriffen hat.
 * @property rewards Anliegende Belohnungsmeldungen. Die Betraege sind bereits
 *   gutgeschrieben; der Dialog meldet nur.
 * @property dailyReward Anstehender Tagesbonus, `null`, wenn heute nichts
 *   abzuholen ist. Anders als [rewards] ist er noch **nicht** gutgeschrieben -
 *   der Spieler holt ihn selbst ab.
 */
@Immutable
data class MainUiState(
    val settings: GameSettings = GameSettings(),
    val isReady: Boolean = false,
    val offlineEarned: String? = null,
    val offlineDuration: String = "",
    val offlineWasCapped: Boolean = false,
    val rewards: List<RewardLineItem> = emptyList(),
    val dailyReward: DailyRewardUiState? = null,
)

/**
 * ViewModel der Activity.
 *
 * Liefert ausschliesslich, was vor dem ersten Bild feststehen muss: die
 * Farbgebung und die Frage, ob der Splashscreen weichen darf. Spielinhalte
 * gehoeren in die ViewModels der einzelnen Bildschirme.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    gameRepository: GameRepository,
    private val sessionManager: GameSessionManager,
    private val rewardManager: RewardManager,
    private val dailyRewardManager: DailyRewardManager,
    private val numberFormatter: NumberFormatter,
    private val durationFormatter: DurationFormatter,
) : ViewModel() {

    /**
     * Zustand der Activity.
     *
     * [SharingStarted.Eagerly] statt `WhileSubscribed`: Die Bedingung des
     * Splashscreens liest den Wert, bevor irgendein Composable den Flow
     * beobachtet. Bei traeger Auswertung stuende dort dauerhaft der
     * Ausgangswert, und der Splashscreen wuerde nie verschwinden.
     */
    val uiState: StateFlow<MainUiState> = combine(
        settingsRepository.settings,
        gameRepository.isLoaded,
        sessionManager.offlineProgress,
        rewardManager.pending,
        dailyRewardManager.pending,
    ) { settings, isLoaded, offline, pendingRewards, dailyReward ->
        MainUiState(
            settings = settings,
            isReady = isLoaded,
            rewards = pendingRewards.map { reward ->
                RewardLineItem(
                    titleRes = when (val source = reward.source) {
                        is RewardSource.Achievement -> source.type.nameRes
                        is RewardSource.Quest -> source.type.nameRes
                    },
                    reward = reward.bundle.toRewardParts(numberFormatter),
                )
            },
            offlineEarned = offline?.let { formatEarned(it) },
            offlineDuration = offline?.let {
                durationFormatter.formatCompact(it.creditedMillis)
            }.orEmpty(),
            offlineWasCapped = offline?.wasCapped ?: false,
            dailyReward = dailyReward?.let { status ->
                DailyRewardUiState(
                    streak = status.streakAfterClaim,
                    cycleDay = status.day.day,
                    cycleLength = DailyRewardType.cycleLength,
                    reward = status.reward.toRewardParts(numberFormatter),
                    protectionUsed = status.protectionUsed,
                    streakBroken = status.streakBroken,
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainUiState(),
    )

    /** Bestaetigt, dass der Willkommensdialog gezeigt wurde. */
    fun onOfflineProgressDismissed() {
        sessionManager.consumeOfflineProgress()
    }

    /** Bestaetigt, dass die Belohnungsmeldung gezeigt wurde. */
    fun onRewardsDismissed() {
        rewardManager.consumeAll()
    }

    /** Holt den Tagesbonus ab. */
    fun onDailyRewardClaimed() {
        dailyRewardManager.claim()
    }

    /**
     * Schliesst den Tagesbonus, ohne abzuholen.
     *
     * Der Anspruch bleibt bestehen und steht beim naechsten Start wieder an.
     */
    fun onDailyRewardDismissed() {
        dailyRewardManager.dismiss()
    }

    /**
     * Fasst den Offline-Ertrag als Text zusammen.
     *
     * Zeigt bewusst nur die Muenzen: Weitere Ressourcen fallen offline nicht
     * an, und eine Aufzaehlung mit einem einzigen Eintrag waere unnoetig
     * umstaendlich.
     */
    private fun formatEarned(progress: OfflineProgress): String =
        "+" + numberFormatter.format(progress.earned[ResourceType.COINS])
}
