package com.pokelike.idle.data.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signiert Spielstaende und prueft sie beim Laden.
 *
 * Was diese Klasse leistet und was nicht - unmissverstaendlich, weil eine
 * falsche Erwartung hier zu falschen Entscheidungen an anderer Stelle fuehrt:
 *
 * Sie verhindert das Bearbeiten der Datenbankdatei. Wer die App-Daten von einem
 * gerooteten Geraet zieht, den Muenzstand in der SQLite-Datei hochsetzt und sie
 * zurueckspielt, scheitert an der Pruefsumme. Das deckt praktisch alle
 * verbreiteten Werkzeuge zum Bearbeiten von Spielstaenden ab.
 *
 * Sie verhindert nicht das Manipulieren des laufenden Prozesses. Der Schluessel
 * steckt im Programm und laesst sich mit genug Aufwand herausloesen; danach
 * lassen sich Spielstaende gueltig signieren. Ein Gegenmittel dagegen gibt es
 * auf dem Geraet grundsaetzlich nicht - der Client gehoert dem Nutzer.
 *
 * Belastbaren Schutz gibt es nur mit einer Serverinstanz, die den Spielstand
 * fuehrt und Kaeufe prueft. Genau darauf ist die Architektur vorbereitet: Der
 * Spielstand ist ein einziges Objekt mit Versionsnummer, und alle Zugriffe
 * laufen ueber [com.pokelike.idle.domain.repository.GameRepository]. Eine
 * serverseitige Umsetzung tritt spaeter an dessen Stelle, ohne dass Spiellogik
 * angepasst werden muss.
 */
@Singleton
class SaveSignature @Inject constructor() {

    /**
     * Berechnet die Signatur ueber [payload].
     *
     * @return Signatur als Hexadezimalzeichenkette.
     */
    fun sign(payload: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(secretKey(), ALGORITHM))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).toHexString()
    }

    /**
     * Prueft eine Signatur.
     *
     * Der Vergleich laeuft ueber [MessageDigest.isEqual] und damit in
     * konstanter Zeit. Ein zeichenweiser Vergleich wuerde ueber die
     * Ausfuehrungsdauer verraten, wie viele Stellen bereits stimmen, und liesse
     * sich Stelle fuer Stelle erraten.
     */
    fun verify(payload: String, signature: String): Boolean {
        val expected = sign(payload).toByteArray(Charsets.UTF_8)
        val actual = signature.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(expected, actual)
    }

    /**
     * Setzt den Schluessel zur Laufzeit zusammen.
     *
     * Die Bestandteile stehen getrennt und werden verrechnet, damit der
     * fertige Schluessel nicht als zusammenhaengende Zeichenkette in der
     * Programmdatei auftaucht - ein `strings`-Aufruf auf dem APK findet ihn so
     * nicht. Das ist Verschleierung, keine Sicherheit: Wer den Code liest,
     * rechnet ihn in Minuten nach. Der Aufwand lohnt trotzdem, weil er die
     * grosse Mehrheit der Gelegenheitsversuche ausschliesst.
     */
    private fun secretKey(): ByteArray = ByteArray(KEY_PARTS.size) { index ->
        (KEY_PARTS[index] xor KEY_MASK[index % KEY_MASK.size]).toByte()
    }

    private fun ByteArray.toHexString(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {

        const val ALGORITHM = "HmacSHA256"

        /**
         * Verschleierter Schluessel. Ergibt erst nach der Verrechnung mit
         * [KEY_MASK] das eigentliche Material.
         *
         * Als [IntArray] notiert, weil Bytewerte oberhalb von 0x7F in Kotlin
         * ausserhalb des [Byte]-Bereichs liegen. Die Umwandlung passiert in
         * [secretKey].
         */
        val KEY_PARTS = intArrayOf(
            0x7A, 0x1C, 0x4E, 0x39, 0x08, 0xB5, 0x62, 0xD1,
            0x3F, 0x90, 0x27, 0xEE, 0x54, 0x0B, 0xC3, 0x76,
            0x18, 0xA2, 0x5D, 0x81, 0x44, 0xF7, 0x2B, 0x69,
            0x9C, 0x30, 0xD8, 0x05, 0xB1, 0x6F, 0x23, 0xCA,
        )

        val KEY_MASK = intArrayOf(
            0x5B, 0xE4, 0x11, 0x7D, 0xA6, 0x38, 0xCF, 0x02,
        )
    }
}
