package com.pokelike.idle.data.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SaveSignatureTest {

    private val saveSignature = SaveSignature()

    @Test
    fun `liefert fuer gleiche Eingaben dieselbe Signatur`() {
        // Ohne diese Eigenschaft schluege die Pruefung beim Laden zufaellig
        // fehl und der Spieler verloere seinen Fortschritt.
        val first = saveSignature.sign(PAYLOAD)
        val second = saveSignature.sign(PAYLOAD)

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `liefert fuer verschiedene Eingaben verschiedene Signaturen`() {
        val original = saveSignature.sign(PAYLOAD)
        val tampered = saveSignature.sign(PAYLOAD.replace("coins=1.0^3", "coins=1.0^30"))

        assertThat(tampered).isNotEqualTo(original)
    }

    @Test
    fun `bestaetigt eine gueltige Signatur`() {
        assertThat(saveSignature.verify(PAYLOAD, saveSignature.sign(PAYLOAD))).isTrue()
    }

    @Test
    fun `weist eine Signatur nach Manipulation der Daten zurueck`() {
        // Der Fall, um den es geht: Jemand zieht die Datenbankdatei vom Geraet,
        // setzt den Muenzstand hoch und spielt sie zurueck.
        val signature = saveSignature.sign(PAYLOAD)
        val tampered = PAYLOAD.replace("coins=1.0^3", "coins=9.9^99")

        assertThat(saveSignature.verify(tampered, signature)).isFalse()
    }

    @Test
    fun `weist eine veraenderte Signatur zurueck`() {
        assertThat(saveSignature.verify(PAYLOAD, "0".repeat(SIGNATURE_LENGTH))).isFalse()
    }

    @Test
    fun `weist eine leere Signatur zurueck`() {
        assertThat(saveSignature.verify(PAYLOAD, "")).isFalse()
    }

    @Test
    fun `erzeugt eine Signatur in HMAC-SHA256-Laenge`() {
        val signature = saveSignature.sign(PAYLOAD)

        // 32 Byte als Hexadezimaltext.
        assertThat(signature).hasLength(SIGNATURE_LENGTH)
        assertThat(signature).matches("[0-9a-f]+")
    }

    @Test
    fun `verarbeitet auch eine leere Eingabe`() {
        assertThat(saveSignature.verify("", saveSignature.sign(""))).isTrue()
    }

    private companion object {
        const val PAYLOAD = "v=1|created=100|seen=200|current:coins=1.0^3"
        const val SIGNATURE_LENGTH = 64
    }
}
