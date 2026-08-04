package com.pokelike.idle.util

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.domain.model.BigNumber
import org.junit.Test

class NumberFormatterTest {

    private val formatter = NumberFormatter()

    @Test
    fun `stellt null schlicht dar`() {
        assertThat(formatter.format(BigNumber.ZERO)).isEqualTo("0")
    }

    @Test
    fun `zeigt ganze Zahlen unter tausend ohne Nachkommastelle`() {
        assertThat(formatter.format(BigNumber.of(523))).isEqualTo("523")
        assertThat(formatter.format(BigNumber.of(1))).isEqualTo("1")
    }

    @Test
    fun `zeigt gebrochene Zahlen unter tausend mit einer Nachkommastelle`() {
        assertThat(formatter.format(BigNumber.of(523.5))).isEqualTo("523.5")
    }

    @Test
    fun `kuerzt ab tausend mit vier signifikanten Stellen`() {
        assertThat(formatter.format(BigNumber.of(1234))).isEqualTo("1.234K")
        assertThat(formatter.format(BigNumber.of(45678))).isEqualTo("45.68K")
        assertThat(formatter.format(BigNumber.of(123456))).isEqualTo("123.5K")
    }

    @Test
    fun `hebt bei der Rundung in die naechste Stufe`() {
        // 999999 wuerde auf eine Nachkommastelle gerundet als "1000.0K"
        // erscheinen. Richtig ist die naechste Stufe.
        assertThat(formatter.format(BigNumber.of(999_999))).isEqualTo("1.000M")
    }

    @Test
    fun `nutzt die Kurzzeichen der kurzen Leiter`() {
        assertThat(formatter.format(BigNumber.of(1.0, 6))).isEqualTo("1.000M")
        assertThat(formatter.format(BigNumber.of(1.0, 9))).isEqualTo("1.000B")
        assertThat(formatter.format(BigNumber.of(1.5, 12))).isEqualTo("1.500T")
        assertThat(formatter.format(BigNumber.of(1.0, 15))).isEqualTo("1.000Qa")
        assertThat(formatter.format(BigNumber.of(1.0, 18))).isEqualTo("1.000Qi")
    }

    @Test
    fun `weicht jenseits der Kurzzeichentabelle auf die Exponentialform aus`() {
        // Die Tabelle endet bei 10^63.
        assertThat(formatter.format(BigNumber.of(1.0, 63))).isEqualTo("1.000Vg")
        assertThat(formatter.format(BigNumber.of(1.0, 66))).isEqualTo("1.000e66")
    }

    @Test
    fun `stellt negative Werte mit Vorzeichen dar`() {
        assertThat(formatter.format(BigNumber.of(-2500))).isEqualTo("-2.500K")
        assertThat(formatter.format(BigNumber.of(-42))).isEqualTo("-42")
    }

    @Test
    fun `stellt Werte unter eins mit zwei Nachkommastellen dar`() {
        assertThat(formatter.format(BigNumber.of(0.35))).isEqualTo("0.35")
    }

    @Test
    fun `fasst sehr kleine Werte zu einer Schranke zusammen`() {
        // "0.0003" traegt fuer den Spieler keine Information mehr.
        assertThat(formatter.format(BigNumber.of(0.0003))).isEqualTo("<0.01")
    }

    @Test
    fun `setzt bei Bedarf ein ausdrueckliches Pluszeichen`() {
        assertThat(formatter.formatSigned(BigNumber.of(100))).isEqualTo("+100")
        assertThat(formatter.formatSigned(BigNumber.of(-100))).isEqualTo("-100")
        assertThat(formatter.formatSigned(BigNumber.ZERO)).isEqualTo("0")
    }
}
