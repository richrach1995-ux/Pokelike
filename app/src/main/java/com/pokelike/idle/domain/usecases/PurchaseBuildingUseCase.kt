package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import javax.inject.Inject

/**
 * Gewuenschte Kaufmenge.
 *
 * Sammelkaeufe sind kein Komfort, sondern Notwendigkeit: Im spaeten Spiel
 * kauft ein Spieler dutzende Exemplare am Stueck, und jedes einzeln
 * anzutippen waere unzumutbar.
 */
enum class BuyAmount(val id: String) {
    ONE("x1"),
    TEN("x10"),

    /** So viele, wie der Kontostand hergibt. */
    MAX("max"),
}

/**
 * Ergebnis eines Kaufversuchs.
 *
 * Ein eigener Typ statt eines `GameState?`, weil die Oberflaeche den Grund
 * kennen muss: "zu teuer" verlangt eine andere Rueckmeldung als "keine Menge
 * bestimmbar".
 */
sealed interface PurchaseResult {

    /** Kauf erfolgreich. */
    data class Success(
        val state: GameState,
        val building: BuildingType,
        val count: Int,
        val paid: BigNumber,
    ) : PurchaseResult

    /** Kontostand reicht nicht. */
    data object NotAffordable : PurchaseResult

    /**
     * Es gibt nichts zu kaufen.
     *
     * Tritt bei [BuyAmount.MAX] auf, wenn nicht einmal ein Exemplar bezahlbar
     * ist. Von [NotAffordable] getrennt, damit die Oberflaeche den Knopf
     * abschalten kann, statt bei jedem Tippen eine Fehlermeldung zu zeigen.
     */
    data object NothingToBuy : PurchaseResult
}

/**
 * Kauft Gebaeude.
 *
 * Preisermittlung und Abbuchung liegen bewusst in einer Operation. Waeren sie
 * getrennt, koennte zwischen beiden ein Takt Leerlaufeinkommen eintreffen und
 * der Preis waere gegen einen anderen Kontostand geprueft worden als der, von
 * dem abgebucht wird.
 */
class PurchaseBuildingUseCase @Inject constructor(
    private val costCalculator: BuildingCostCalculator,
) {

    operator fun invoke(
        state: GameState,
        building: BuildingType,
        amount: BuyAmount,
        discount: Double = 0.0,
    ): PurchaseResult {
        val owned = state.buildings[building]
        val coins = state[ResourceType.COINS]

        val count = when (amount) {
            BuyAmount.ONE -> 1
            BuyAmount.TEN -> 10
            BuyAmount.MAX -> costCalculator.maxAffordable(
                building = building,
                owned = owned,
                coins = coins,
                discount = discount,
            )
        }

        if (count <= 0) return PurchaseResult.NothingToBuy

        val price = costCalculator.priceFor(
            building = building,
            owned = owned,
            count = count,
            discount = discount,
        )

        val paidState = state.spend(ResourceBundle.single(ResourceType.COINS, price))
            ?: return PurchaseResult.NotAffordable

        return PurchaseResult.Success(
            state = paidState.copy(
                buildings = paidState.buildings.plus(building, count),
                statistics = paidState.statistics.withBuildingsPurchased(count),
            ),
            building = building,
            count = count,
            paid = price,
        )
    }
}
