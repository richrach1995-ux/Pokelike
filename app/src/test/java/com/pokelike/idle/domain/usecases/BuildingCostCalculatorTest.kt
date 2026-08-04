package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingType
import org.junit.Test
import kotlin.math.pow
import kotlin.test.assertFailsWith

class BuildingCostCalculatorTest {

    private val calculator = BuildingCostCalculator()

    @Test
    fun `verlangt beim ersten Exemplar den Grundpreis`() {
        val price = calculator.priceFor(BuildingType.FINGER, owned = 0)

        assertThat(price).isEqualTo(BigNumber.of(BuildingType.FINGER.basePrice))
    }

    @Test
    fun `steigert den Preis mit jedem Exemplar`() {
        val price = calculator.priceFor(BuildingType.FINGER, owned = 10)

        val expected = BuildingType.FINGER.basePrice * GameConfig.BUILDING_COST_GROWTH.pow(10.0)
        assertThat(price.toDouble()).isWithin(expected * TOLERANCE).of(expected)
    }

    @Test
    fun `summiert den Preis eines Sammelkaufs`() {
        // Zehn Exemplare am Stueck muessen genauso viel kosten wie zehn
        // Einzelkaeufe nacheinander - sonst waere einer der beiden Wege
        // guenstiger und der Spieler wuerde ihn ausnutzen.
        val bulk = calculator.priceFor(BuildingType.CURSOR, owned = 3, count = 10)

        var stepwise = BigNumber.ZERO
        repeat(10) { index ->
            stepwise += calculator.priceFor(BuildingType.CURSOR, owned = 3 + index, count = 1)
        }

        assertThat(bulk.toDouble()).isWithin(stepwise.toDouble() * TOLERANCE).of(stepwise.toDouble())
    }

    @Test
    fun `kostet nichts bei einer Menge von null`() {
        assertThat(calculator.priceFor(BuildingType.FINGER, owned = 5, count = 0))
            .isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `bleibt jenseits des Double-Bereichs rechenbar`() {
        // Genau der Grund fuer BigNumber: 1.15^10000 ist als Double bereits
        // Infinity, und das Gebaeude waere dauerhaft unkaufbar.
        assertThat(GameConfig.BUILDING_COST_GROWTH.pow(10_000.0))
            .isEqualTo(Double.POSITIVE_INFINITY)

        val price = calculator.priceFor(BuildingType.FINGER, owned = 10_000)

        assertThat(price.isPositive).isTrue()
        assertThat(price.exponent).isGreaterThan(600)
    }

    @Test
    fun `beruecksichtigt einen Preisnachlass`() {
        val full = calculator.priceFor(BuildingType.MINE, owned = 7, count = 5)
        val discounted = calculator.priceFor(BuildingType.MINE, owned = 7, count = 5, discount = 0.25)

        assertThat(discounted.toDouble())
            .isWithin(full.toDouble() * TOLERANCE)
            .of(full.toDouble() * 0.75)
    }

    @Test
    fun `weist unsinnige Parameter ab`() {
        assertFailsWith<IllegalArgumentException> {
            calculator.priceFor(BuildingType.FINGER, owned = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            calculator.priceFor(BuildingType.FINGER, owned = 0, count = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            calculator.priceFor(BuildingType.FINGER, owned = 0, growth = 1.0)
        }
        assertFailsWith<IllegalArgumentException> {
            // Ein Nachlass von 1.0 waere ein kostenloses Gebaeude und wuerde
            // das gesamte Wirtschaftssystem aushebeln.
            calculator.priceFor(BuildingType.FINGER, owned = 0, discount = 1.0)
        }
    }

    // --- Groesstmoegliche Kaufmenge ---------------------------------------

    @Test
    fun `liefert null wenn nicht einmal ein Exemplar bezahlbar ist`() {
        val count = calculator.maxAffordable(
            building = BuildingType.FINGER,
            owned = 0,
            coins = BigNumber.of(14),
        )

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `liefert genau eins beim exakt passenden Kontostand`() {
        val price = calculator.priceFor(BuildingType.FINGER, owned = 0, count = 1)

        val count = calculator.maxAffordable(BuildingType.FINGER, owned = 0, coins = price)

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `bestimmt die Menge so, dass der Kauf tatsaechlich gelingt`() {
        // Der Logarithmus arbeitet mit Double-Genauigkeit und kann um ein
        // Exemplar danebenliegen. Ohne die Nachkorrektur wuerde der Kauf im
        // Aufrufer scheitern, obwohl die Oberflaeche ihn angeboten hat.
        listOf(100, 5_000, 1_000_000, 987_654_321).forEach { coinAmount ->
            val coins = BigNumber.of(coinAmount)
            val count = calculator.maxAffordable(BuildingType.FINGER, owned = 3, coins = coins)

            val priceForCount = calculator.priceFor(BuildingType.FINGER, owned = 3, count = count)
            val priceForOneMore =
                calculator.priceFor(BuildingType.FINGER, owned = 3, count = count + 1)

            assertThat(priceForCount).isAtMost(coins)
            assertThat(priceForOneMore).isGreaterThan(coins)
        }
    }

    @Test
    fun `haelt die Obergrenze ein`() {
        // Ohne Deckel koennte ein sehr hoher Kontostand eine Menge liefern,
        // deren Berechnung die Oberflaeche blockiert.
        val count = calculator.maxAffordable(
            building = BuildingType.FINGER,
            owned = 0,
            coins = BigNumber.of(1.0, 300),
            limit = 50,
        )

        assertThat(count).isEqualTo(50)
    }

    @Test
    fun `liefert null bei leerem Konto`() {
        assertThat(calculator.maxAffordable(BuildingType.FINGER, 0, BigNumber.ZERO)).isEqualTo(0)
    }

    private companion object {
        /** Relative Toleranz. Absolute Werte waeren bei 1e300 bedeutungslos. */
        const val TOLERANCE = 1e-9
    }
}
