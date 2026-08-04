package com.pokelike.idle.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow
import kotlin.test.assertFailsWith

class BigNumberTest {

    // --- Normalisierung ---------------------------------------------------

    @Test
    fun `normalisiert die Mantisse auf eine Vorkommastelle`() {
        val value = BigNumber.of(1234.0)

        assertThat(value.mantissa).isWithin(TOLERANCE).of(1.234)
        assertThat(value.exponent).isEqualTo(3)
    }

    @Test
    fun `stellt Werte unter eins mit negativem Exponenten dar`() {
        val value = BigNumber.of(0.5)

        assertThat(value.mantissa).isWithin(TOLERANCE).of(5.0)
        assertThat(value.exponent).isEqualTo(-1)
    }

    @Test
    fun `bildet null auf ZERO ab`() {
        assertThat(BigNumber.of(0.0)).isEqualTo(BigNumber.ZERO)
        assertThat(BigNumber.of(0L)).isEqualTo(BigNumber.ZERO)
        assertThat(BigNumber.ZERO.isZero).isTrue()
    }

    @Test
    fun `behaelt das Vorzeichen bei der Normalisierung`() {
        val value = BigNumber.of(-500)

        assertThat(value.mantissa).isWithin(TOLERANCE).of(-5.0)
        assertThat(value.exponent).isEqualTo(2)
        assertThat(value.isNegative).isTrue()
    }

    @Test
    fun `weist nicht darstellbare Werte ab`() {
        assertFailsWith<IllegalArgumentException> { BigNumber.of(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { BigNumber.of(Double.POSITIVE_INFINITY) }
    }

    // --- Addition ---------------------------------------------------------

    @Test
    fun `addiert Werte gleicher Groessenordnung`() {
        assertThat(BigNumber.of(100) + BigNumber.of(23)).isEqualTo(BigNumber.of(123))
    }

    @Test
    fun `addiert Werte unterschiedlicher Groessenordnung`() {
        val result = BigNumber.of(1.0, 6) + BigNumber.of(2.5, 5)

        assertThat(result.exponent).isEqualTo(6)
        assertThat(result.mantissa).isWithin(TOLERANCE).of(1.25)
    }

    @Test
    fun `verwirft Summanden ausserhalb der Genauigkeit`() {
        // Eine Muenze aendert an 1e50 Muenzen nichts mehr. Das Ergebnis waere
        // bei einer naiven Rechnung nicht nur unveraendert, sondern durch
        // Rundungsfehler sogar leicht falsch.
        val huge = BigNumber.of(1.0, 50)

        assertThat(huge + BigNumber.ONE).isEqualTo(huge)
        assertThat(BigNumber.ONE + huge).isEqualTo(huge)
    }

    @Test
    fun `behandelt ZERO als neutrales Element`() {
        val value = BigNumber.of(42)

        assertThat(value + BigNumber.ZERO).isEqualTo(value)
        assertThat(BigNumber.ZERO + value).isEqualTo(value)
    }

    // --- Subtraktion ------------------------------------------------------

    @Test
    fun `subtrahiert und laesst negative Ergebnisse zu`() {
        assertThat(BigNumber.of(5) - BigNumber.of(8)).isEqualTo(BigNumber.of(-3))
    }

    @Test
    fun `liefert bei Ganzzahlrechnungen exakte Ergebnisse`() {
        // Regression: Die Normalisierung teilte durch 10^shift, und 0.7 / 0.1
        // ergibt in Fliesskommaarithmetik 6.999999999999999. Damit war
        // 100 - 30 nicht gleich 70 - im Spiel haette die Deckungspruefung
        // eines exakt passenden Kontostands fehlgeschlagen.
        assertThat(BigNumber.of(100) - BigNumber.of(30)).isEqualTo(BigNumber.of(70))
        assertThat(BigNumber.of(1000) - BigNumber.of(1)).isEqualTo(BigNumber.of(999))
        assertThat(BigNumber.of(0.1) + BigNumber.of(0.2)).isEqualTo(BigNumber.of(0.3))
    }

    @Test
    fun `normalisiert auch sehr kleine Werte`() {
        // Die Skalierung muss in Schritten erfolgen: 10.0.pow(320) waere
        // bereits Infinity und wuerde die Mantisse zerstoeren.
        val value = BigNumber.of(1e-300)

        assertThat(value.exponent).isEqualTo(-300)
        assertThat(value.mantissa).isWithin(TOLERANCE).of(1.0)
    }

    @Test
    fun `ergibt bei gleichen Werten exakt null`() {
        val value = BigNumber.of(1.234, 42)

        assertThat((value - value).isZero).isTrue()
    }

    // --- Multiplikation und Division --------------------------------------

    @Test
    fun `multipliziert ueber Exponentenaddition`() {
        val result = BigNumber.of(2.0, 10) * BigNumber.of(3.0, 10)

        assertThat(result.exponent).isEqualTo(20)
        assertThat(result.mantissa).isWithin(TOLERANCE).of(6.0)
    }

    @Test
    fun `normalisiert nach der Multiplikation`() {
        // 5 * 4 = 20, die Mantisse muss zurueck in den Bereich 1 bis 10.
        val result = BigNumber.of(5.0, 3) * BigNumber.of(4.0, 3)

        assertThat(result.mantissa).isWithin(TOLERANCE).of(2.0)
        assertThat(result.exponent).isEqualTo(7)
    }

    @Test
    fun `multipliziert mit einem gewoehnlichen Faktor`() {
        val result = BigNumber.of(1.0, 20) * 2.5

        assertThat(result.mantissa).isWithin(TOLERANCE).of(2.5)
        assertThat(result.exponent).isEqualTo(20)
    }

    @Test
    fun `dividiert ueber Exponentensubtraktion`() {
        val result = BigNumber.of(6.0, 20) / BigNumber.of(3.0, 10)

        assertThat(result.exponent).isEqualTo(10)
        assertThat(result.mantissa).isWithin(TOLERANCE).of(2.0)
    }

    @Test
    fun `meldet Division durch null`() {
        assertFailsWith<ArithmeticException> { BigNumber.ONE / BigNumber.ZERO }
        assertFailsWith<ArithmeticException> { BigNumber.ONE / 0.0 }
    }

    // --- Potenzieren ------------------------------------------------------

    @Test
    fun `potenziert im Double-Bereich genau wie die Standardbibliothek`() {
        val viaBigNumber = BigNumber.pow(base = 1.15, power = 100.0).toDouble()
        val viaDouble = 1.15.pow(100.0)

        // Relativer Vergleich: Bei Werten um 1e6 waere ein absoluter
        // Toleranzwert bedeutungslos.
        assertThat(viaBigNumber).isWithin(viaDouble * RELATIVE_TOLERANCE).of(viaDouble)
    }

    @Test
    fun `potenziert weit jenseits des Double-Bereichs`() {
        // Genau der Grund fuer diesen Zahlentyp: Ab etwa 5.300 Gebaeuden
        // liefert Math.pow fuer 1.15^n nur noch Infinity, und der Preis waere
        // nicht mehr berechenbar.
        assertThat(1.15.pow(10_000.0)).isEqualTo(Double.POSITIVE_INFINITY)

        val result = BigNumber.pow(base = 1.15, power = 10_000.0)

        assertThat(result.exponent).isEqualTo(606)
        assertThat(result.isPositive).isTrue()
    }

    @Test
    fun `behandelt Sonderfaelle beim Potenzieren`() {
        assertThat(BigNumber.pow(base = 5.0, power = 0.0)).isEqualTo(BigNumber.ONE)
        assertThat(BigNumber.pow(base = 0.0, power = 3.0)).isEqualTo(BigNumber.ZERO)
        assertFailsWith<ArithmeticException> { BigNumber.of(-2).pow(2.0) }
    }

    // --- Vergleich --------------------------------------------------------

    @Test
    fun `vergleicht ueber den Exponenten`() {
        assertThat(BigNumber.of(1.0, 100) > BigNumber.of(9.0, 99)).isTrue()
        assertThat(BigNumber.of(9.0, 99) < BigNumber.of(1.0, 100)).isTrue()
    }

    @Test
    fun `vergleicht negative Werte in der richtigen Richtung`() {
        // Ein groesserer Exponent bedeutet bei negativen Zahlen einen
        // kleineren Wert.
        assertThat(BigNumber.of(-1.0, 5) < BigNumber.of(-1.0, 3)).isTrue()
        assertThat(BigNumber.of(-1) < BigNumber.ZERO).isTrue()
        assertThat(BigNumber.ZERO < BigNumber.ONE).isTrue()
    }

    @Test
    fun `sortiert eine gemischte Liste korrekt`() {
        val values = listOf(
            BigNumber.of(1.0, 10),
            BigNumber.of(-5),
            BigNumber.ZERO,
            BigNumber.of(1.0, 5),
            BigNumber.of(3),
        )

        assertThat(values.sorted()).containsExactly(
            BigNumber.of(-5),
            BigNumber.ZERO,
            BigNumber.of(3),
            BigNumber.of(1.0, 5),
            BigNumber.of(1.0, 10),
        ).inOrder()
    }

    @Test
    fun `haelt Gleichheit und Vergleich konsistent`() {
        val left = BigNumber.of(1500.0)
        val right = BigNumber.of(1.5, 3)

        assertThat(left).isEqualTo(right)
        assertThat(left.compareTo(right)).isEqualTo(0)
        assertThat(left.hashCode()).isEqualTo(right.hashCode())
    }

    // --- Umwandlung -------------------------------------------------------

    @Test
    fun `berechnet Verhaeltnisse auch jenseits des Double-Bereichs`() {
        val current = BigNumber.of(5.0, 400)
        val target = BigNumber.of(1.0, 401)

        // Beide Werte einzeln als Double waeren Infinity; das Verhaeltnis
        // bleibt trotzdem exakt bestimmbar.
        assertThat(current.toDouble()).isEqualTo(Double.POSITIVE_INFINITY)
        assertThat(current.ratioTo(target)).isWithin(TOLERANCE).of(0.5)
    }

    @Test
    fun `liefert null als Verhaeltnis zu null`() {
        assertThat(BigNumber.ONE.ratioTo(BigNumber.ZERO)).isEqualTo(0.0)
    }

    @Test
    fun `berechnet den dekadischen Logarithmus`() {
        assertThat(BigNumber.of(1.0, 42).log10()).isWithin(TOLERANCE).of(42.0)
        assertThat(BigNumber.of(100).log10()).isWithin(TOLERANCE).of(2.0)
    }

    private companion object {
        const val TOLERANCE = 1e-9
        const val RELATIVE_TOLERANCE = 1e-10
    }
}
