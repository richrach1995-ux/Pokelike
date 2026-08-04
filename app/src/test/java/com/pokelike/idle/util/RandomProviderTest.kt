package com.pokelike.idle.util

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.testing.FakeRandomProvider
import org.junit.Test
import kotlin.test.assertFailsWith

class RandomProviderTest {

    @Test
    fun `loest bei Wahrscheinlichkeit null nie aus`() {
        // Ein Spieler ohne Kritupgrade darf keinen kritischen Treffer landen,
        // egal welche Zahl faellt.
        val random = FakeRandomProvider.alwaysHitting()

        assertThat(random.rollChance(0.0)).isFalse()
    }

    @Test
    fun `loest bei Wahrscheinlichkeit eins immer aus`() {
        val random = FakeRandomProvider.neverHitting()

        assertThat(random.rollChance(1.0)).isTrue()
    }

    @Test
    fun `verbraucht bei den Grenzwerten keinen Wurf`() {
        // Wichtig fuer die Vorhersagbarkeit von Folgen: Ein Effekt mit
        // Wahrscheinlichkeit 0 oder 1 darf die Zufallsfolge nicht verschieben
        // und damit alle nachfolgenden Wuerfe veraendern.
        val random = FakeRandomProvider(doubles = listOf(0.5))

        random.rollChance(0.0)
        random.rollChance(1.0)

        assertThat(random.doubleDraws).isEqualTo(0)
    }

    @Test
    fun `wertet den Rand als Fehlschlag`() {
        // rollChance vergleicht mit `<`. Faellt genau der Schwellenwert, ist
        // es kein Treffer - sonst laege die tatsaechliche Rate minimal ueber
        // der angezeigten.
        val random = FakeRandomProvider(doubles = listOf(0.05))

        assertThat(random.rollChance(0.05)).isFalse()
    }

    @Test
    fun `wertet knapp unterhalb des Randes als Treffer`() {
        val random = FakeRandomProvider(doubles = listOf(0.049_999))

        assertThat(random.rollChance(0.05)).isTrue()
    }

    @Test
    fun `weist Wahrscheinlichkeiten ausserhalb von null bis eins ab`() {
        val random = FakeRandomProvider.neverHitting()

        assertFailsWith<IllegalArgumentException> { random.rollChance(-0.1) }
        assertFailsWith<IllegalArgumentException> { random.rollChance(1.1) }
    }

    @Test
    fun `liefert Werte im halboffenen Einheitsintervall`() {
        val random = DefaultRandomProvider()

        repeat(1_000) {
            val value = random.nextDouble()
            assertThat(value).isAtLeast(0.0)
            assertThat(value).isLessThan(1.0)
        }
    }

    @Test
    fun `liefert ganze Zahlen unterhalb der Obergrenze`() {
        val random = DefaultRandomProvider()

        repeat(1_000) {
            val value = random.nextInt(untilExclusive = 10)
            assertThat(value).isAtLeast(0)
            assertThat(value).isLessThan(10)
        }
    }

    @Test
    fun `weist eine nicht positive Obergrenze ab`() {
        val random = DefaultRandomProvider()

        assertFailsWith<IllegalArgumentException> { random.nextInt(untilExclusive = 0) }
        assertFailsWith<IllegalArgumentException> { random.nextInt(untilExclusive = -5) }
    }
}
