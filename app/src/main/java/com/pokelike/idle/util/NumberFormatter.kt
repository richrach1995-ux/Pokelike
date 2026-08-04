package com.pokelike.idle.util

import com.pokelike.idle.domain.model.BigNumber
import java.util.Locale
import kotlin.math.floor
import kotlin.math.pow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bringt [BigNumber]-Werte in eine lesbare Form.
 *
 * Bewusst getrennt vom Zahlentyp selbst: Die Darstellung ist eine Frage der
 * Praesentation und wird sich aendern (Lokalisierung, abschaltbare Kurzform,
 * wissenschaftliche Notation als Einstellung). Das Domaenenmodell darf davon
 * nichts wissen.
 *
 * Die Kurzform folgt der kurzen Leiter (K, M, B, T, ...), die im Genre
 * etabliert ist. Angezeigt werden vier signifikante Stellen - genug, damit
 * Spieler Fortschritt wahrnehmen, ohne dass die Zahl unruhig wirkt.
 */
@Singleton
class NumberFormatter @Inject constructor() {

    /**
     * Formatiert einen Wert fuer die Anzeige.
     *
     * Beispiele:
     * ```
     * 0        -> "0"
     * 523      -> "523"
     * 1234     -> "1.234K"
     * 45678    -> "45.68K"
     * 1.5e12   -> "1.500T"
     * 1e66     -> "1.000e66"
     * ```
     */
    fun format(value: BigNumber): String {
        if (value.isZero) return ZERO_TEXT

        val sign = if (value.isNegative) "-" else ""
        val magnitude = value.absoluteValue

        return sign + when {
            magnitude.exponent < 0 -> formatBelowOne(magnitude)
            magnitude.exponent < SUFFIX_STEP -> formatBelowThousand(magnitude.toDouble())
            else -> formatWithSuffix(magnitude)
        }
    }

    /**
     * Formatiert einen Wert mit ausdruecklichem Vorzeichen.
     *
     * Fuer Gewinn- und Verlustanzeigen, bei denen ein "+" die Information
     * traegt - etwa der schwebende Text ueber dem Klick-Button.
     */
    fun formatSigned(value: BigNumber): String =
        if (value.isPositive) "+${format(value)}" else format(value)

    /**
     * Werte unterhalb von 1.
     *
     * Unterhalb von 0.01 wird nicht weiter aufgeloest, sondern eine Schranke
     * angezeigt. Eine Zahl wie "0.0003" traegt fuer den Spieler keine
     * Information mehr, kostet aber Platz und Lesezeit.
     */
    private fun formatBelowOne(value: BigNumber): String {
        val plain = value.toDouble()
        return if (plain < SMALLEST_DISPLAYED) {
            BELOW_THRESHOLD_TEXT
        } else {
            String.format(Locale.US, "%.2f", plain)
        }
    }

    /**
     * Werte von 1 bis unter 1000.
     *
     * Ganze Zahlen erscheinen ohne Nachkommastelle: Ein Kontostand von "523"
     * liest sich besser als "523.0", und im fruehen Spiel sind Muenzbetraege
     * praktisch immer ganzzahlig.
     */
    private fun formatBelowThousand(value: Double): String =
        if (value == floor(value)) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.1f", value)
        }

    /** Werte ab 1000: vier signifikante Stellen plus Kurzzeichen. */
    private fun formatWithSuffix(value: BigNumber): String {
        var tier = value.exponent / SUFFIX_STEP
        var scaled = value.mantissa * TEN.pow((value.exponent % SUFFIX_STEP).toDouble())

        // Ein Wert wie 999.97 wuerde auf eine Nachkommastelle gerundet als
        // "1000.0K" erscheinen. In diesem Fall gehoert er bereits in die
        // naechste Stufe.
        if (scaled >= ROUNDING_LIMIT) {
            scaled /= TIER_FACTOR
            tier++
        }

        if (tier >= SUFFIXES.size) return formatScientific(value)

        val decimals = when {
            scaled >= 100.0 -> 1
            scaled >= 10.0 -> 2
            else -> 3
        }
        return String.format(Locale.US, "%.${decimals}f", scaled) + SUFFIXES[tier]
    }

    /**
     * Rueckfallform jenseits der Kurzzeichentabelle.
     *
     * Ab hier ist jede Kurzform ohnehin nur noch eine Buchstabenfolge ohne
     * Wiedererkennungswert; die wissenschaftliche Schreibweise ist dann
     * eindeutiger.
     */
    private fun formatScientific(value: BigNumber): String =
        String.format(Locale.US, "%.3fe%d", value.mantissa, value.exponent)

    private companion object {

        const val ZERO_TEXT = "0"
        const val BELOW_THRESHOLD_TEXT = "<0.01"
        const val SMALLEST_DISPLAYED = 0.01
        const val TEN = 10.0

        /** Ein Kurzzeichen deckt drei Zehnerpotenzen ab. */
        const val SUFFIX_STEP = 3

        const val TIER_FACTOR = 1000.0

        /** Ab diesem Wert rundet die Anzeige in die naechste Stufe auf. */
        const val ROUNDING_LIMIT = 999.95

        /**
         * Kurzzeichen nach der kurzen Leiter. Der Index entspricht der Stufe:
         * Index 1 sind Tausender, Index 2 Millionen und so fort.
         *
         * Die Tabelle reicht bis 10^63. Spielstaende jenseits davon sind
         * moeglich, aber selbst fuer Vielspieler weit entfernt; sie werden
         * wissenschaftlich dargestellt.
         */
        val SUFFIXES = arrayOf(
            "", "K", "M", "B", "T",
            "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc",
            "UDc", "DDc", "TDc", "QaDc", "QiDc", "SxDc", "SpDc", "OcDc", "NoDc",
            "Vg",
        )
    }
}
