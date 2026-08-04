package com.pokelike.idle.ui.screens.upgrades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.UpgradeType
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.PurchaseUpgradeUseCase
import com.pokelike.idle.domain.usecases.UpgradePurchaseResult
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.NumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel der Upgrade-Liste.
 */
@HiltViewModel
class UpgradesViewModel @Inject constructor(
    private val repository: GameRepository,
    private val purchaseUpgrade: PurchaseUpgradeUseCase,
    private val numberFormatter: NumberFormatter,
    dispatchers: DispatcherProvider,
) : ViewModel() {

    val uiState: StateFlow<UpgradesUiState> = repository.gameState
        .map { state -> buildUiState(state) }
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UpgradesUiState(),
        )

    /**
     * Kauft ein Upgrade.
     *
     * Wie beim Gebaeudekauf laeuft die gesamte Pruefung innerhalb von
     * [GameRepository.update] und damit atomar auf dem aktuellen Zustand.
     *
     * @return `true`, wenn tatsaechlich gekauft wurde.
     */
    fun onBuy(upgrade: UpgradeType): Boolean {
        var purchased = false

        repository.update { state ->
            when (val result = purchaseUpgrade(state, upgrade)) {
                is UpgradePurchaseResult.Success -> {
                    purchased = true
                    result.state
                }

                UpgradePurchaseResult.NotAffordable,
                UpgradePurchaseResult.AlreadyOwned,
                UpgradePurchaseResult.NotUnlocked,
                -> {
                    purchased = false
                    state
                }
            }
        }

        return purchased
    }

    private fun buildUiState(state: GameState): UpgradesUiState {
        val coins = state[ResourceType.COINS]

        val rows = UpgradeType.entries
            // Nicht freigeschaltete Upgrades bleiben verborgen. Sie alle von
            // Beginn an zu zeigen wuerde die Liste unuebersichtlich machen und
            // die Freude an einem neu erscheinenden Eintrag nehmen.
            .filter { upgrade -> upgrade in state.upgrades || upgrade.unlockCondition.isMet(state) }
            .map { upgrade ->
                UpgradeRow(
                    type = upgrade,
                    price = numberFormatter.format(upgrade.priceAsBigNumber),
                    isOwned = upgrade in state.upgrades,
                    isAffordable = coins >= upgrade.priceAsBigNumber,
                )
            }
            // Gekaufte nach hinten: Oben steht, was der Spieler als Naechstes
            // erreichen kann.
            .sortedWith(compareBy({ it.isOwned }, { it.type.price }))

        return UpgradesUiState(
            rows = rows,
            coins = numberFormatter.format(coins),
            ownedCount = state.upgrades.count,
            totalCount = UpgradeType.entries.size,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
