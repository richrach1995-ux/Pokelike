package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
import org.junit.Test
import kotlin.test.assertFailsWith

class CalculateIncomeUseCaseTest {

    private val calculateIncome = CalculateIncomeUseCase()

    @Test
    fun `liefert ohne Gebaeude null`() {
        assertThat(calculateIncome(BuildingInventory.EMPTY)).isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `summiert den Ertrag aller Gebaeude`() {
        val inventory = BuildingInventory.of(
            BuildingType.FINGER to 10,
            BuildingType.CURSOR to 5,
        )

        // 10 * 0.1 + 5 * 1.0 = 6.0
        assertThat(calculateIncome(inventory)).isEqualTo(BigNumber.of(6.0))
    }

    @Test
    fun `wendet den Faktor auf die Gesamtsumme an`() {
        val inventory = BuildingInventory.of(BuildingType.CURSOR to 10)

        assertThat(calculateIncome(inventory, multiplier = 2.5))
            .isEqualTo(BigNumber.of(25.0))
    }

    @Test
    fun `liefert bei Faktor null keinen Ertrag`() {
        val inventory = BuildingInventory.of(BuildingType.CURSOR to 10)

        assertThat(calculateIncome(inventory, multiplier = 0.0)).isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `rechnet auch bei sehr vielen Gebaeuden`() {
        // Zeitmaschinen bringen 1.6 Millionen pro Sekunde; mit hundertausend
        // Exemplaren verlaesst der Ertrag den Bereich handlicher Zahlen.
        val inventory = BuildingInventory.of(BuildingType.TIME_MACHINE to 100_000)

        val income = calculateIncome(inventory)

        assertThat(income).isEqualTo(BigNumber.of(1.6e11))
    }

    @Test
    fun `liefert den Ertrag eines einzelnen Gebaeudetyps`() {
        assertThat(calculateIncome.incomeFor(BuildingType.MINE, count = 3))
            .isEqualTo(BigNumber.of(141.0))
        assertThat(calculateIncome.incomeFor(BuildingType.MINE, count = 0))
            .isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `weist negative Faktoren ab`() {
        assertFailsWith<IllegalArgumentException> {
            calculateIncome(BuildingInventory.EMPTY, multiplier = -1.0)
        }
    }
}

class PurchaseBuildingUseCaseTest {

    private val costCalculator = BuildingCostCalculator()
    private val purchase = PurchaseBuildingUseCase(costCalculator)

    private fun stateWithCoins(amount: Double): GameState = GameState.newGame(nowMillis = 0L)
        .copy(resources = ResourcePool.of(ResourceType.COINS to BigNumber.of(amount)))

    @Test
    fun `kauft ein Gebaeude und bucht den Preis ab`() {
        val state = stateWithCoins(100.0)

        val result = purchase(state, BuildingType.FINGER, BuyAmount.ONE)

        assertThat(result).isInstanceOf(PurchaseResult.Success::class.java)
        val success = result as PurchaseResult.Success
        assertThat(success.count).isEqualTo(1)
        assertThat(success.state.buildings[BuildingType.FINGER]).isEqualTo(1)
        assertThat(success.state[ResourceType.COINS]).isEqualTo(BigNumber.of(85))
    }

    @Test
    fun `verweigert den Kauf bei zu wenig Muenzen`() {
        val state = stateWithCoins(10.0)

        assertThat(purchase(state, BuildingType.FINGER, BuyAmount.ONE))
            .isEqualTo(PurchaseResult.NotAffordable)
    }

    @Test
    fun `laesst den Zustand bei fehlgeschlagenem Kauf unveraendert`() {
        val state = stateWithCoins(10.0)

        purchase(state, BuildingType.FINGER, BuyAmount.ONE)

        assertThat(state[ResourceType.COINS]).isEqualTo(BigNumber.of(10))
        assertThat(state.buildings.isEmpty).isTrue()
    }

    @Test
    fun `kauft zehn Exemplare am Stueck`() {
        val state = stateWithCoins(100_000.0)

        val result = purchase(state, BuildingType.FINGER, BuyAmount.TEN) as PurchaseResult.Success

        assertThat(result.count).isEqualTo(10)
        assertThat(result.state.buildings[BuildingType.FINGER]).isEqualTo(10)
    }

    @Test
    fun `kauft bei Maximum so viel wie moeglich`() {
        val state = stateWithCoins(1_000.0)

        val result = purchase(state, BuildingType.FINGER, BuyAmount.MAX) as PurchaseResult.Success

        // Nach dem Kauf darf kein weiteres Exemplar mehr bezahlbar sein.
        val remaining = result.state[ResourceType.COINS]
        val nextPrice = costCalculator.priceFor(
            building = BuildingType.FINGER,
            owned = result.state.buildings[BuildingType.FINGER],
            count = 1,
        )
        assertThat(nextPrice).isGreaterThan(remaining)
    }

    @Test
    fun `meldet bei Maximum ohne Guthaben, dass nichts zu kaufen ist`() {
        val state = stateWithCoins(1.0)

        // Von NotAffordable getrennt, damit die Oberflaeche den Knopf
        // abschalten kann, statt bei jedem Tippen eine Meldung zu zeigen.
        assertThat(purchase(state, BuildingType.FINGER, BuyAmount.MAX))
            .isEqualTo(PurchaseResult.NothingToBuy)
    }

    @Test
    fun `schreibt die ausgegebene Summe in die Statistik`() {
        val state = stateWithCoins(100.0)

        val result = purchase(state, BuildingType.FINGER, BuyAmount.ONE) as PurchaseResult.Success

        assertThat(result.state.statistics.lifetimeSpent[ResourceType.COINS])
            .isEqualTo(BigNumber.of(15))
    }

    @Test
    fun `verteuert jeden weiteren Kauf`() {
        var state = stateWithCoins(1_000.0)

        val first = purchase(state, BuildingType.FINGER, BuyAmount.ONE) as PurchaseResult.Success
        state = first.state
        val second = purchase(state, BuildingType.FINGER, BuyAmount.ONE) as PurchaseResult.Success

        assertThat(second.paid).isGreaterThan(first.paid)
    }
}

class BuildingUnlockTest {

    @Test
    fun `zeigt das erste Gebaeude ab der halben Preisschwelle`() {
        val finger = BuildingType.FINGER

        // Der Spieler soll sein naechstes Ziel sehen, bevor er es sich leisten
        // kann - das ist der Moment, der ihn weiterspielen laesst.
        assertThat(finger.isUnlocked(owned = 0, lifetimeCoins = BigNumber.of(7))).isFalse()
        assertThat(finger.isUnlocked(owned = 0, lifetimeCoins = BigNumber.of(8))).isTrue()
    }

    @Test
    fun `zeigt besessene Gebaeude immer`() {
        // Massgeblich ist die Lebenszeitsumme, nicht der Kontostand. Sonst
        // verschwaende ein Gebaeude wieder, sobald der Spieler sein Geld
        // ausgibt.
        assertThat(
            BuildingType.TIME_MACHINE.isUnlocked(owned = 1, lifetimeCoins = BigNumber.ZERO),
        ).isTrue()
    }

    @Test
    fun `haelt spaete Gebaeude zu Beginn verborgen`() {
        assertThat(
            BuildingType.SPACESHIP.isUnlocked(owned = 0, lifetimeCoins = BigNumber.of(1_000)),
        ).isFalse()
    }

    @Test
    fun `haelt die Schluessel eindeutig`() {
        assertThat(BuildingType.entries.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `steigt in Preis und Ertrag durchgehend an`() {
        // Ein Gebaeude, das mehr kostet und weniger bringt als sein Vorgaenger,
        // waere ein Balancing-Fehler, der im Spiel sofort auffaellt.
        BuildingType.entries.zipWithNext().forEach { (earlier, later) ->
            assertThat(later.basePrice).isGreaterThan(earlier.basePrice)
            assertThat(later.baseIncomePerSecond).isGreaterThan(earlier.baseIncomePerSecond)
        }
    }

    @Test
    fun `haelt die Preissprünge zwischen den Stufen im Rahmen`() {
        // Bewusst KEINE Monotonie des Preis-Ertrag-Verhaeltnisses geprueft:
        // Der Cursor ist mit 100 pro Ertragspunkt absichtlich guenstiger als
        // der Finger mit 150. Diese frühe Belohnung zieht den Spieler in die
        // zweite Stufe und ist im Genre Standard.
        //
        // Geprueft wird stattdessen, dass keine Stufe den Sprung zur naechsten
        // unerreichbar macht.
        BuildingType.entries.zipWithNext().forEach { (earlier, later) ->
            val priceJump = later.basePrice / earlier.basePrice
            assertThat(priceJump).isAtLeast(MIN_PRICE_JUMP)
            assertThat(priceJump).isAtMost(MAX_PRICE_JUMP)
        }
    }

    private companion object {
        /** Ohne spuerbaren Abstand waere die naechste Stufe kein Ziel. */
        const val MIN_PRICE_JUMP = 5.0

        /** Ein groesserer Sprung liesse den Fortschritt stocken. */
        const val MAX_PRICE_JUMP = 20.0
    }
}
