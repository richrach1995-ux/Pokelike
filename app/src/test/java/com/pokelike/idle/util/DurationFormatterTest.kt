package com.pokelike.idle.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DurationFormatterTest {

    private val formatter = DurationFormatter()

    @Test
    fun `formatiert null als Nullzeit`() {
        assertThat(formatter.formatCompact(0L)).isEqualTo("00:00")
    }

    @Test
    fun `schneidet angebrochene Sekunden ab`() {
        // 1999 ms sind eine volle Sekunde und ein Rest. Aufrunden wuerde einen
        // Zaehler kurz vor Ablauf faelschlich als abgelaufen anzeigen.
        assertThat(formatter.formatCompact(1_999L)).isEqualTo("00:01")
    }

    @Test
    fun `fuellt Minuten und Sekunden auf zwei Stellen auf`() {
        assertThat(formatter.formatCompact(65_000L)).isEqualTo("01:05")
    }

    @Test
    fun `zeigt Stunden erst ab einer vollen Stunde an`() {
        assertThat(formatter.formatCompact(3_599_000L)).isEqualTo("59:59")
        assertThat(formatter.formatCompact(3_600_000L)).isEqualTo("1:00:00")
    }

    @Test
    fun `formatiert mehrstellige Stundenwerte`() {
        // 100 Stunden, 1 Minute, 1 Sekunde - realistisch fuer Offline-Zeiten.
        val millis = 100L * 3_600_000L + 61_000L
        assertThat(formatter.formatCompact(millis)).isEqualTo("100:01:01")
    }

    @Test
    fun `klemmt negative Eingaben auf null`() {
        // Kann entstehen, wenn zwei Zeitstempel aus verschiedenen Quellen
        // verrechnet werden. Darf in der UI nie als negative Dauer erscheinen.
        assertThat(formatter.formatCompact(-5_000L)).isEqualTo("00:00")
    }
}
