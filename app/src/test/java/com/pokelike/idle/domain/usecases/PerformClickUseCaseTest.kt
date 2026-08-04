package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.ClickModifiers
import com.pokelike.idle.domain.model.ComboState
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.testing.FakeRandomProvider
import org.junit.Test

class PerformClickUseCaseTest {

    private val baseState = GameState.newGame(nowMillis = 0L)

    private fun useCase(random: FakeRandomProvider) = PerformClickUseCase(random)

    @Test
    fun `bucht den Grundertrag ohne kritischen Treffer`() {
        val outcome = useCase(FakeRandomProvider.neverHitting())(
            state = baseState,
            combo = ComboState.NONE,
            modifiers = ClickModifiers.base(),
            nowMillis = NOW,
        )

        assertThat(outcome.wasCritical).isFalse()
        assertThat(outcome.earned).isEqualTo(BigNumber.of(GameConfig.BASE_COINS_PER_CLICK))
        assertThat(outcome.state[ResourceType.COINS])
            .isEqualTo(BigNumber.of(GameConfig.BASE_COINS_PER_CLICK))
    }

    @Test
    fun `vervielfacht den Ertrag bei kritischem Treffer`() {
        val outcome = useCase(FakeRandomProvider.alwaysHitting())(
            state = baseState,
            combo = ComboState.NONE,
            modifiers = ClickModifiers.base(),
            nowMillis = NOW,
        )

        assertThat(outcome.wasCritical).isTrue()
        assertThat(outcome.earned).isEqualTo(
            BigNumber.of(GameConfig.BASE_COINS_PER_CLICK) * GameConfig.BASE_CRITICAL_MULTIPLIER,
        )
    }

    @Test
    fun `zaehlt jeden Klick genau einmal`() {
        // Regression: Eine fruehere Fassung wendete die Statistik zweimal an -
        // einmal ueber grant, einmal ueber withClick auf demselben
        // Ausgangszustand. Der Klickzaehler lief dadurch doppelt, was jede
        // spaetere Achievement- und Questpruefung verfaelscht haette.
        val subject = useCase(FakeRandomProvider.neverHitting())

        var state = baseState
        var combo = ComboState.NONE
        repeat(5) { index ->
            val outcome = subject(state, combo, ClickModifiers.base(), NOW + index * 100L)
            state = outcome.state
            combo = outcome.combo
        }

        assertThat(state.statistics.totalClicks).isEqualTo(5L)
    }

    @Test
    fun `zaehlt kritische Treffer getrennt mit`() {
        // Abwechselnd kritisch und normal.
        val random = FakeRandomProvider(
            doubles = listOf(FakeRandomProvider.ALWAYS_HITS, FakeRandomProvider.NEVER_HITS),
        )
        val subject = useCase(random)

        var state = baseState
        var combo = ComboState.NONE
        repeat(4) { index ->
            val outcome = subject(state, combo, ClickModifiers.base(), NOW + index * 100L)
            state = outcome.state
            combo = outcome.combo
        }

        assertThat(state.statistics.totalClicks).isEqualTo(4L)
        assertThat(state.statistics.totalCriticalClicks).isEqualTo(2L)
        assertThat(state.statistics.criticalRate).isWithin(TOLERANCE).of(0.5)
    }

    @Test
    fun `schreibt die Lebenszeitsumme fort`() {
        val subject = useCase(FakeRandomProvider.neverHitting())

        var state = baseState
        var combo = ComboState.NONE
        repeat(3) { index ->
            val outcome = subject(state, combo, ClickModifiers.base(), NOW + index * 100L)
            state = outcome.state
            combo = outcome.combo
        }

        // Drei Klicks zu je einer Muenze, jeweils mit wachsender Combo:
        // 1 + 1.02 + 1.04.
        assertThat(state.statistics.lifetimeEarned[ResourceType.COINS])
            .isEqualTo(state[ResourceType.COINS])
    }

    @Test
    fun `beruecksichtigt die Combo bereits beim ausloesenden Klick`() {
        // Der Klick, der die Combo fortsetzt, soll den erreichten Bonus schon
        // bekommen. Andernfalls haenge der Spieler dauerhaft eine Stufe
        // hinterher, und der Bonus fuehlte sich verzoegert an.
        val subject = useCase(FakeRandomProvider.neverHitting())

        val first = subject(baseState, ComboState.NONE, ClickModifiers.base(), NOW)
        val second = subject(first.state, first.combo, ClickModifiers.base(), NOW + 100L)

        assertThat(second.comboMultiplier)
            .isWithin(TOLERANCE).of(1.0 + GameConfig.COMBO_STEP_BONUS)
        assertThat(second.earned).isEqualTo(
            BigNumber.of(GameConfig.BASE_COINS_PER_CLICK) * second.comboMultiplier,
        )
    }

    @Test
    fun `setzt die Combo nach Ablauf des Fensters zurueck`() {
        val subject = useCase(FakeRandomProvider.neverHitting())

        val first = subject(baseState, ComboState.NONE, ClickModifiers.base(), NOW)
        val late = subject(
            first.state,
            first.combo,
            ClickModifiers.base(),
            NOW + GameConfig.COMBO_WINDOW_MS + 1L,
        )

        assertThat(late.combo.count).isEqualTo(1)
        assertThat(late.comboMultiplier).isWithin(TOLERANCE).of(1.0)
    }

    @Test
    fun `addiert flache Zuschlaege vor den Faktoren`() {
        // Reihenfolge ist Balancing: Wuerde erst multipliziert, waeren flache
        // Zuschlaege im spaeten Spiel wertlos.
        val modifiers = ClickModifiers.base().copy(
            baseValue = BigNumber.of(10),
            flatBonus = BigNumber.of(5),
            multiplier = 3.0,
        )

        val outcome = useCase(FakeRandomProvider.neverHitting())(
            state = baseState,
            combo = ComboState.NONE,
            modifiers = modifiers,
            nowMillis = NOW,
        )

        // (10 + 5) * 3 = 45, nicht 10 * 3 + 5 = 35.
        assertThat(outcome.earned).isEqualTo(BigNumber.of(45))
    }

    @Test
    fun `rechnet auch mit Betraegen jenseits des Double-Bereichs`() {
        val modifiers = ClickModifiers.base().copy(baseValue = BigNumber.of(1.0, 400))

        val outcome = useCase(FakeRandomProvider.alwaysHitting())(
            state = baseState,
            combo = ComboState.NONE,
            modifiers = modifiers,
            nowMillis = NOW,
        )

        // 1e400 * 5 = 5e400. Mit Double waere hier bereits Infinity.
        assertThat(outcome.earned.exponent).isEqualTo(400)
        assertThat(outcome.earned.mantissa).isWithin(TOLERANCE).of(5.0)
    }

    @Test
    fun `wuerfelt genau einmal je Klick`() {
        // Mehrere Wuerfe je Klick wuerden die tatsaechliche Kritrate
        // gegenueber dem angezeigten Wert verschieben.
        val random = FakeRandomProvider.neverHitting()
        val subject = useCase(random)

        var state = baseState
        var combo = ComboState.NONE
        repeat(7) { index ->
            val outcome = subject(state, combo, ClickModifiers.base(), NOW + index * 100L)
            state = outcome.state
            combo = outcome.combo
        }

        assertThat(random.doubleDraws).isEqualTo(7)
    }

    @Test
    fun `liefert den Erwartungswert fuer die Anzeige`() {
        // Fuenf Prozent Chance auf fuenffachen Ertrag ergeben im Schnitt
        // zwanzig Prozent ueber dem Grundwert.
        val expected = ClickModifiers.base().expectedValuePerClick()

        assertThat(expected).isEqualTo(BigNumber.of(1.2))
    }

    private companion object {
        const val NOW = 1_000_000L
        const val TOLERANCE = 1e-9
    }
}
