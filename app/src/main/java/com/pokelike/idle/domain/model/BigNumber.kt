package com.pokelike.idle.domain.model

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/**
 * Zahlentyp fuer alle Spielwerte mit unbegrenztem Wertebereich.
 *
 * Dargestellt wird `mantisse * 10^exponent`, wobei die Mantisse stets auf
 * `1 <= |mantisse| < 10` normalisiert ist (Ausnahme: die Null).
 *
 * Warum ein eigener Typ und keine Standardklasse:
 *
 * - [Double] bricht bei etwa 1e308. Ein Idle-Spiel mit Prestige-Multiplikatoren
 *   erreicht diesen Bereich regelmaessig. Schon deutlich vorher werden
 *   Vergleiche ungenau, was sich im Spiel als Kauf-Button zeigt, der bei exakt
 *   passendem Kontostand ausgegraut bleibt.
 * - [java.math.BigDecimal] ist exakt, aber fuer zehn Berechnungen pro Sekunde
 *   ueber dutzende Gebaeude zu langsam und erzeugt erheblichen GC-Druck.
 *
 * Dieser Typ hat rund 15 signifikante Stellen - genug, denn im Spiel wird nie
 * eine Billiarde plus eins unterschieden, wohl aber 1e40 von 2e40.
 *
 * Die Klasse ist unveraenderlich. Jede Rechenoperation liefert eine neue
 * Instanz. Diese Objekte sind kurzlebig und bleiben in der Young Generation,
 * ihre Erzeugung ist deshalb billig - allokationsfrei ist sie aber nicht. Auf
 * dem heissen Pfad (Einkommensverrechnung pro Takt) bleibt die Anzahl der
 * Operationen daher bewusst klein.
 */
class BigNumber private constructor(
    /** Normalisierte Mantisse, `1 <= |mantissa| < 10`, oder exakt 0.0. */
    val mantissa: Double,
    /** Zehnerexponent. */
    val exponent: Int,
) : Comparable<BigNumber> {

    /** Vorzeichen: -1, 0 oder 1. */
    val signum: Int
        get() = when {
            mantissa > 0.0 -> 1
            mantissa < 0.0 -> -1
            else -> 0
        }

    val isZero: Boolean get() = mantissa == 0.0

    val isPositive: Boolean get() = mantissa > 0.0

    val isNegative: Boolean get() = mantissa < 0.0

    /** Betrag dieser Zahl. */
    val absoluteValue: BigNumber
        get() = if (isNegative) BigNumber(-mantissa, exponent) else this

    // --- Grundrechenarten -------------------------------------------------

    /**
     * Addition.
     *
     * Unterscheiden sich die Exponenten um mehr als [SIGNIFICANT_DIGITS], geht
     * der kleinere Summand in der Genauigkeit des groesseren vollstaendig
     * unter. Statt ihn mit Rundungsfehlern zu verrechnen, wird er verworfen -
     * das ist sowohl korrekter als auch schneller.
     */
    operator fun plus(other: BigNumber): BigNumber {
        if (isZero) return other
        if (other.isZero) return this

        val exponentDifference = exponent.toLong() - other.exponent.toLong()
        return when {
            exponentDifference > SIGNIFICANT_DIGITS -> this
            exponentDifference < -SIGNIFICANT_DIGITS -> other
            // Bewusst dividiert statt mit einer negativen Zehnerpotenz
            // multipliziert: 10^-1 ist als Double nicht exakt darstellbar,
            // 10^1 dagegen schon. Die Division ist hier also der genauere Weg.
            exponentDifference >= 0L -> normalize(
                mantissa = mantissa + other.mantissa / TEN_BASE.pow(exponentDifference.toDouble()),
                exponent = exponent.toLong(),
            )

            else -> normalize(
                mantissa = other.mantissa + mantissa / TEN_BASE.pow(-exponentDifference.toDouble()),
                exponent = other.exponent.toLong(),
            )
        }
    }

    /** Subtraktion. Das Ergebnis darf negativ werden. */
    operator fun minus(other: BigNumber): BigNumber = this + other.negated()

    operator fun times(other: BigNumber): BigNumber {
        if (isZero || other.isZero) return ZERO
        return normalize(
            mantissa = mantissa * other.mantissa,
            exponent = exponent.toLong() + other.exponent.toLong(),
        )
    }

    /** Multiplikation mit einem gewoehnlichen Faktor, etwa einem Multiplikator. */
    operator fun times(factor: Double): BigNumber {
        if (isZero || factor == 0.0) return ZERO
        return normalize(mantissa * factor, exponent.toLong())
    }

    /**
     * Division.
     *
     * @throws ArithmeticException bei Division durch null. Ein stiller
     *   Ersatzwert waere hier gefaehrlich: Er wuerde einen Rechenfehler in eine
     *   plausibel aussehende Zahl verwandeln und erst viel spaeter auffallen.
     */
    operator fun div(other: BigNumber): BigNumber {
        if (other.isZero) throw ArithmeticException("Division durch null")
        if (isZero) return ZERO
        return normalize(
            mantissa = mantissa / other.mantissa,
            exponent = exponent.toLong() - other.exponent.toLong(),
        )
    }

    operator fun div(divisor: Double): BigNumber {
        if (divisor == 0.0) throw ArithmeticException("Division durch null")
        if (isZero) return ZERO
        return normalize(mantissa / divisor, exponent.toLong())
    }

    /** Kehrt das Vorzeichen um. */
    fun negated(): BigNumber = if (isZero) ZERO else BigNumber(-mantissa, exponent)

    /**
     * Potenziert diese Zahl.
     *
     * Gerechnet wird im Logarithmus: `x^p = 10^(p * log10(x))`. Wiederholtes
     * Multiplizieren waere bei grossen Exponenten sowohl langsam als auch
     * ungenau.
     *
     * @throws ArithmeticException bei negativer Basis - fuer sie ist die
     *   Potenz mit gebrochenem Exponenten nicht definiert. Im Spiel treten
     *   negative Basen nicht auf; ein Aufruf damit ist ein Programmierfehler.
     */
    fun pow(power: Double): BigNumber {
        if (isNegative) throw ArithmeticException("Potenz einer negativen Basis")
        if (isZero) return if (power == 0.0) ONE else ZERO
        return pow(base = this, power = power)
    }

    // --- Umwandlung -------------------------------------------------------

    /**
     * Wert als [Double].
     *
     * Liefert `Infinity`, sobald der Wert den Double-Bereich uebersteigt.
     * Deshalb nur fuer Werte einsetzen, die nachweislich klein sind - fuer
     * Verhaeltnisse (Fortschrittsbalken) ist [ratioTo] der sichere Weg.
     */
    fun toDouble(): Double = mantissa * TEN_BASE.pow(exponent.toDouble())

    /**
     * Verhaeltnis dieser Zahl zu [other], geeignet fuer Fortschrittsbalken.
     *
     * Die Division findet im BigNumber-Raum statt und kann deshalb auch dann
     * nicht ueberlaufen, wenn beide Werte weit jenseits des Double-Bereichs
     * liegen. Erst das Ergebnis - eine Zahl nahe 0 bis 1 - wird umgewandelt.
     */
    fun ratioTo(other: BigNumber): Double {
        if (other.isZero) return 0.0
        val ratio = (this / other).toDouble()
        return if (ratio.isFinite()) ratio else Double.MAX_VALUE
    }

    /** Dekadischer Logarithmus. Basis fuer Fortschrittskurven und Balancing. */
    fun log10(): Double = log10(abs(mantissa)) + exponent

    // --- Vergleich und Identitaet ----------------------------------------

    override fun compareTo(other: BigNumber): Int {
        val ownSignum = signum
        val otherSignum = other.signum
        if (ownSignum != otherSignum) return ownSignum.compareTo(otherSignum)
        if (ownSignum == 0) return 0

        val exponentComparison = exponent.compareTo(other.exponent)
        // Bei negativen Zahlen bedeutet ein groesserer Exponent einen
        // kleineren Wert: -1e5 liegt unter -1e3.
        if (exponentComparison != 0) {
            return if (ownSignum > 0) exponentComparison else -exponentComparison
        }
        return mantissa.compareTo(other.mantissa)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BigNumber) return false
        return mantissa == other.mantissa && exponent == other.exponent
    }

    override fun hashCode(): Int = 31 * mantissa.hashCode() + exponent

    /**
     * Technische Darstellung fuer Logausgaben und Fehlermeldungen.
     *
     * Die Anzeige im Spiel uebernimmt
     * [com.pokelike.idle.util.NumberFormatter] - Formatierung ist bewusst nicht
     * Aufgabe des Zahlentyps, sonst haengt das Domaenenmodell an
     * Darstellungsfragen.
     */
    override fun toString(): String = "${mantissa}e$exponent"

    companion object {

        val ZERO: BigNumber = BigNumber(0.0, 0)
        val ONE: BigNumber = BigNumber(1.0, 0)
        val TEN: BigNumber = BigNumber(1.0, 1)

        /**
         * Grenze, ab der ein Summand gegenueber dem anderen bedeutungslos wird.
         *
         * Ein Double traegt rund 15 bis 17 signifikante Dezimalstellen. Liegen
         * zwei Werte weiter auseinander, aendert die Addition das Ergebnis
         * nicht mehr.
         */
        private const val SIGNIFICANT_DIGITS = 17L

        /**
         * Obergrenze fuer den Exponenten.
         *
         * Weit jenseits jedes erreichbaren Spielstands, aber klein genug, dass
         * Summen und Differenzen zweier Exponenten sicher in einen [Int]
         * passen. Ohne diese Grenze koennte ein Ueberlauf eine riesige Zahl
         * unbemerkt in eine winzige verwandeln.
         */
        private const val MAX_EXPONENT = 1_000_000_000L

        private const val TEN_BASE = 10.0

        /**
         * Groesster Skalierungsschritt in einem Durchgang. Darueber hinaus
         * liefert `10.0.pow(...)` bereits `Infinity`.
         */
        private const val MAX_SCALING_STEP = 300L

        /** 10^13 - die Mantisse liegt in [1, 10), das ergibt 14 Stellen. */
        private const val ROUNDING_SCALE = 1e13

        /** Erzeugt eine Zahl aus einem [Double]. */
        fun of(value: Double): BigNumber {
            require(value.isFinite()) { "Nicht darstellbarer Wert: $value" }
            return normalize(value, 0L)
        }

        /** Erzeugt eine Zahl aus einem [Long]. */
        fun of(value: Long): BigNumber = normalize(value.toDouble(), 0L)

        /** Erzeugt eine Zahl aus einem [Int]. */
        fun of(value: Int): BigNumber = normalize(value.toDouble(), 0L)

        /**
         * Erzeugt `mantisse * 10^exponent` und normalisiert das Ergebnis.
         *
         * Nuetzlich beim Laden eines Spielstands, in dem beide Bestandteile
         * getrennt abgelegt sind.
         */
        fun of(mantissa: Double, exponent: Int): BigNumber {
            require(mantissa.isFinite()) { "Nicht darstellbare Mantisse: $mantissa" }
            return normalize(mantissa, exponent.toLong())
        }

        /**
         * Berechnet `base^power` direkt im BigNumber-Raum.
         *
         * Genau der Fall der Gebaeudepreise: `basispreis * 1.15^anzahl`. Bei
         * etwa 5.300 gekauften Gebaeuden ueberschreitet `1.15^n` den
         * Double-Bereich - mit `Math.pow` waere der Preis ab dort `Infinity`
         * und das Gebaeude unkaufbar.
         *
         * @throws ArithmeticException bei negativer Basis.
         */
        fun pow(base: Double, power: Double): BigNumber {
            if (base < 0.0) throw ArithmeticException("Potenz einer negativen Basis")
            if (base == 0.0) return if (power == 0.0) ONE else ZERO
            return pow(of(base), power)
        }

        private fun pow(base: BigNumber, power: Double): BigNumber {
            if (power == 0.0) return ONE

            val resultLog = base.log10() * power
            require(resultLog.isFinite()) { "Potenz nicht darstellbar" }
            val resultExponent = floor(resultLog)

            // Der gebrochene Anteil des Logarithmus ergibt die Mantisse,
            // der ganzzahlige den Exponenten.
            return normalize(
                mantissa = TEN_BASE.pow(resultLog - resultExponent),
                exponent = resultExponent.toLong(),
            )
        }

        /**
         * Bringt eine beliebige Mantisse auf `1 <= |mantisse| < 10` und
         * verrechnet die Verschiebung mit dem Exponenten.
         *
         * Die beiden Korrekturschleifen am Ende fangen Rundungsfehler ab: Der
         * Logarithmus kann fuer Werte knapp unterhalb einer Zehnerpotenz eine
         * Mantisse von exakt 10.0 liefern, was die Invariante verletzen wuerde.
         */
        private fun normalize(mantissa: Double, exponent: Long): BigNumber {
            if (mantissa == 0.0 || mantissa.isNaN()) return ZERO
            if (mantissa.isInfinite()) {
                throw ArithmeticException("Zwischenergebnis ausserhalb des Double-Bereichs")
            }

            var normalizedMantissa = mantissa
            var normalizedExponent = exponent

            val shift = floor(log10(abs(normalizedMantissa))).toLong()
            if (shift != 0L) {
                normalizedMantissa = scaleByPowerOfTen(normalizedMantissa, -shift)
                normalizedExponent += shift
            }

            normalizedMantissa = roundToSignificantDigits(normalizedMantissa)

            while (abs(normalizedMantissa) >= TEN_BASE) {
                normalizedMantissa /= TEN_BASE
                normalizedExponent++
            }
            while (abs(normalizedMantissa) < 1.0) {
                normalizedMantissa *= TEN_BASE
                normalizedExponent--
            }

            if (normalizedExponent > MAX_EXPONENT || normalizedExponent < -MAX_EXPONENT) {
                throw ArithmeticException("Exponent ausserhalb des zulaessigen Bereichs: $normalizedExponent")
            }

            return BigNumber(normalizedMantissa, normalizedExponent.toInt())
        }

        /**
         * Multipliziert [value] mit `10^power`.
         *
         * Zwei Feinheiten, die sich beide als notwendig erwiesen haben:
         *
         * - Positive Zehnerpotenzen sind als Double bis 10^22 exakt, negative
         *   dagegen nie (0.1 ist nicht darstellbar). Deshalb wird bei
         *   negativer Potenz dividiert statt mit dem Kehrwert multipliziert.
         * - Die Skalierung erfolgt in Schritten von hoechstens 300 Zehnerpotenzen.
         *   `10.0.pow(320)` waere bereits `Infinity`, und ein sehr kleiner
         *   Ausgangswert liesse sich dann nicht mehr normalisieren.
         */
        private fun scaleByPowerOfTen(value: Double, power: Long): Double {
            var result = value
            var remaining = abs(power)
            val scaleUp = power > 0L

            while (remaining > 0L) {
                val step = minOf(remaining, MAX_SCALING_STEP)
                val factor = TEN_BASE.pow(step.toDouble())
                result = if (scaleUp) result * factor else result / factor
                remaining -= step
            }
            return result
        }

        /**
         * Rundet die Mantisse auf die tatsaechlich belastbare Stellenzahl.
         *
         * Ohne diesen Schritt schleppt jede Rechnung Darstellungsrauschen mit:
         * `100 - 30` ergibt in Fliesskommaarithmetik eine Mantisse von
         * 6.999999999999999 statt 7.0. Fuer die Anzeige waere das belanglos,
         * fuer Vergleiche nicht - ein Kontostand von exakt dem Kaufpreis wuerde
         * die Deckungspruefung nicht bestehen und der Kauf-Button grau bleiben.
         *
         * Gerundet wird auf 14 signifikante Stellen. Der Typ verspricht rund 15;
         * die letzte Stelle aufzugeben kostet nichts und macht Ergebnisse
         * reproduzierbar vergleichbar.
         */
        private fun roundToSignificantDigits(mantissa: Double): Double =
            round(mantissa * ROUNDING_SCALE) / ROUNDING_SCALE
    }
}
