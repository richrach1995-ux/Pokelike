package com.pokelike.idle.domain.model

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import org.junit.Test

class ComboStateTest {

    @Test
    fun `beginnt beim ersten Klick bei eins`() {
        val combo = ComboState.NONE.advance(nowMillis = NOW)

        assertThat(combo.count).isEqualTo(1)
        assertThat(combo.lastClickAtMillis).isEqualTo(NOW)
    }

    @Test
    fun `waechst bei Klicks innerhalb des Zeitfensters`() {
        var combo = ComboState.NONE.advance(NOW)
        combo = combo.advance(NOW + 500L)
        combo = combo.advance(NOW + 1_000L)

        assertThat(combo.count).isEqualTo(3)
    }

    @Test
    fun `beginnt nach Ablauf des Fensters neu`() {
        val combo = ComboState.NONE
            .advance(NOW)
            .advance(NOW + GameConfig.COMBO_WINDOW_MS + 1L)

        assertThat(combo.count).isEqualTo(1)
    }

    @Test
    fun `gilt bei exakt abgelaufenem Fenster als beendet`() {
        // Der Grenzfall muss eindeutig entschieden sein, sonst haengt das
        // Ergebnis davon ab, ob ein Takt eine Millisekunde frueher oder
        // spaeter eintrifft.
        val combo = ComboState(count = 5, lastClickAtMillis = NOW)

        assertThat(combo.isExpired(NOW + GameConfig.COMBO_WINDOW_MS)).isTrue()
        assertThat(combo.isExpired(NOW + GameConfig.COMBO_WINDOW_MS - 1L)).isFalse()
    }

    @Test
    fun `gibt beim ersten Klick noch keinen Bonus`() {
        // Ein einzelner Klick ist keine Serie.
        val combo = ComboState.NONE.advance(NOW)

        assertThat(combo.multiplier()).isWithin(TOLERANCE).of(1.0)
    }

    @Test
    fun `steigert den Faktor je Fortsetzung`() {
        val combo = ComboState(count = 11, lastClickAtMillis = NOW)

        // Zehn Fortsetzungen zu je zwei Prozent.
        assertThat(combo.multiplier()).isWithin(TOLERANCE).of(1.20)
    }

    @Test
    fun `deckelt den Faktor`() {
        val atCap = ComboState(count = GameConfig.COMBO_MAX_STEPS + 1, lastClickAtMillis = NOW)
        val farBeyond = ComboState(count = 100_000, lastClickAtMillis = NOW)

        // Ohne Deckel waere dauerhaftes Tippen jedem Gebaeudeausbau ueberlegen
        // und das Spiel verloere seinen Idle-Charakter.
        assertThat(farBeyond.multiplier()).isEqualTo(atCap.multiplier())
        assertThat(atCap.multiplier()).isWithin(TOLERANCE).of(
            1.0 + GameConfig.COMBO_MAX_STEPS * GameConfig.COMBO_STEP_BONUS,
        )
    }

    @Test
    fun `liefert ohne Combo den Faktor eins`() {
        assertThat(ComboState.NONE.multiplier()).isEqualTo(1.0)
        assertThat(ComboState.NONE.isActive).isFalse()
    }

    @Test
    fun `zaehlt den Restanteil des Zeitfensters herunter`() {
        val combo = ComboState(count = 3, lastClickAtMillis = NOW)
        val halfWindow = GameConfig.COMBO_WINDOW_MS / 2L

        assertThat(combo.remainingFraction(NOW)).isWithin(FLOAT_TOLERANCE).of(1f)
        assertThat(combo.remainingFraction(NOW + halfWindow))
            .isWithin(FLOAT_TOLERANCE).of(0.5f)
        assertThat(combo.remainingFraction(NOW + GameConfig.COMBO_WINDOW_MS))
            .isWithin(FLOAT_TOLERANCE).of(0f)
    }

    @Test
    fun `klemmt den Restanteil bei einem Zeitsprung`() {
        val combo = ComboState(count = 3, lastClickAtMillis = NOW)

        // Darf die Anzeige nicht ueber den Rand hinaus zeichnen lassen.
        assertThat(combo.remainingFraction(NOW + 10 * GameConfig.COMBO_WINDOW_MS))
            .isEqualTo(0f)
        assertThat(combo.remainingFraction(NOW - 1_000L)).isEqualTo(1f)
    }

    private companion object {
        const val NOW = 5_000_000L
        const val TOLERANCE = 1e-9
        const val FLOAT_TOLERANCE = 1e-6f
    }
}
