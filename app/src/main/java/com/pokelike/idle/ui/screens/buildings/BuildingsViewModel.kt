package com.pokelike.idle.ui.screens.buildings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.BuildingCostCalculator
import com.pokelike.idle.domain.usecases.BuyAmount
import com.pokelike.idle.domain.usecases.CalculateIncomeUseCase
import com.pokelike.idle.domain.usecases.PurchaseBuildingUseCase
import com.pokelike.idle.domain.usecases.PurchaseResult
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
 * ViewModel der Gebaeudeliste.
 *
 * Berechnet je Zeile Preis, Ertrag und Kaufbarkeit. Das gehoert hierher und
 * nicht in den Bildschirm: Die Preisformel arbeitet mit [BuildingCostCalculator]
 * und damit mit Potenzrechnung ueber grosse Zahlen - Arbeit, die nicht waehrend
 * der Zusammensetzung auf dem UI-Thread stattfinden darf.
 */
@HiltViewModel
class BuildingsViewModel @Inject constructor(
    private val repository: GameRepository,
    private val purchaseBuilding: PurchaseBuildingUseCase,
    private val costCalculator: BuildingCostCalculator,
    private val calculateIncome: CalculateIncomeUseCase,
    private val numberFormatter: NumberFormatter,
    dispatchers: DispatcherProvider,
) : ViewModel() {

    /**
     * Gewaehlte Kaufmenge.
     *
     * Im ViewModel und nicht im Bildschirm gehalten, damit die Wahl eine
     * Bildschirmdrehung uebersteht. Ein Spieler, der auf "max" gestellt hat und
     * nach dem Drehen wieder bei "x1" landet, kauft versehentlich ein einzelnes
     * Gebaeude.
     */
    private val buyAmount = MutableStateFlow(BuyAmount.ONE)

    val uiState: StateFlow<BuildingsUiState> = combine(
        repository.gameState,
        buyAmount,
    ) { state, amount ->
        buildUiState(state, amount)
    }
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = BuildingsUiState(),
        )

    fun onBuyAmountSelected(amount: BuyAmount) {
        buyAmount.value = amount
    }

    /**
     * Kauft ein Gebaeude.
     *
     * Der Kauf laeuft vollstaendig innerhalb von [GameRepository.update] und
     * damit atomar auf dem aktuellen Zustand. Wuerde er ausserhalb gerechnet
     * und das Ergebnis anschliessend gesetzt, ginge das Leerlaufeinkommen
     * verloren, das zwischen Berechnung und Zuweisung eintrifft.
     *
     * @return `true`, wenn tatsaechlich gekauft wurde.
     */
    fun onBuy(building: BuildingType): Boolean {
        val amount = buyAmount.value
        var purchased = false

        repository.update { state ->
            when (val result = purchaseBuilding(state, building, amount)) {
                is PurchaseResult.Success -> {
                    purchased = true
                    result.state
                }

                PurchaseResult.NotAffordable,
                PurchaseResult.NothingToBuy,
                -> {
                    purchased = false
                    state
                }
            }
        }

        return purchased
    }

    private fun buildUiState(state: GameState, amount: BuyAmount): BuildingsUiState {
        val coins = state[ResourceType.COINS]
        val lifetimeCoins = state.statistics.lifetimeEarned[ResourceType.COINS]

        val rows = BuildingType.entries
            .filter { type -> type.isUnlocked(state.buildings[type], lifetimeCoins) }
            .map { type ->
                val owned = state.buildings[type]

                val count = when (amount) {
                    BuyAmount.ONE -> 1
                    BuyAmount.TEN -> 10
                    BuyAmount.MAX -> costCalculator
                        .maxAffordable(type, owned, coins)
                        // Bei leerem Konto waere die Menge null und der Preis
                        // ebenfalls. Angezeigt wird dann der Preis fuer ein
                        // Exemplar, damit der Spieler sein naechstes Ziel sieht.
                        .coerceAtLeast(1)
                }

                val price = costCalculator.priceFor(type, owned, count)

                BuildingRow(
                    type = type,
                    owned = owned,
                    price = numberFormatter.format(price),
                    purchasableCount = count,
                    incomePerSecond = numberFormatter.format(
                        calculateIncome.incomeFor(type, owned),
                    ),
                    isAffordable = coins >= price,
                )
            }

        return BuildingsUiState(
            rows = rows,
            buyAmount = amount,
            totalIncomePerSecond = numberFormatter.format(calculateIncome(state.buildings)),
            coins = numberFormatter.format(coins),
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
