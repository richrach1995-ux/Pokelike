package com.pokelike.idle.testing

import com.pokelike.idle.util.RandomProvider

/**
 * Zufallsquelle mit vorgegebenen Werten.
 *
 * Genau der Grund, warum [RandomProvider] eine Schnittstelle ist: Ein Test kann
 * damit gezielt den Wert setzen, der die Kritschwelle trifft, statt ueber
 * tausende Durchlaeufe zu schaetzen und gelegentlich ohne Fehler im Code
 * fehlzuschlagen.
 *
 * Die Werte werden zyklisch wiederholt. Damit laesst sich eine Folge wie
 * "kritisch, normal, normal" fuer beliebig viele Klicks vorgeben, ohne die
 * Liste entsprechend lang zu machen.
 */
class FakeRandomProvider(
    private val doubles: List<Double> = listOf(NEVER_HITS),
    private val ints: List<Int> = listOf(0),
) : RandomProvider {

    private var doubleIndex = 0
    private var intIndex = 0

    /** Anzahl der bisher entnommenen Gleitkommawerte. */
    val doubleDraws: Int get() = doubleIndex

    override fun nextDouble(): Double {
        check(doubles.isNotEmpty()) { "Keine Werte vorgegeben" }
        return doubles[doubleIndex++ % doubles.size]
    }

    override fun nextInt(untilExclusive: Int): Int {
        require(untilExclusive > 0) { "Obergrenze muss positiv sein: $untilExclusive" }
        check(ints.isNotEmpty()) { "Keine Werte vorgegeben" }
        return ints[intIndex++ % ints.size].coerceIn(0, untilExclusive - 1)
    }

    companion object {

        /**
         * Wert, der jede Wahrscheinlichkeit unter 1.0 verfehlt.
         *
         * `rollChance` vergleicht mit `<`, also loest 0.999999 nur bei einer
         * Wahrscheinlichkeit von 1.0 aus - und die wird ohnehin gesondert
         * behandelt.
         */
        const val NEVER_HITS = 0.999_999

        /** Wert, der jede Wahrscheinlichkeit ueber 0 trifft. */
        const val ALWAYS_HITS = 0.0

        /** Jeder Wurf ist ein Treffer. */
        fun alwaysHitting() = FakeRandomProvider(doubles = listOf(ALWAYS_HITS))

        /** Kein Wurf ist ein Treffer. */
        fun neverHitting() = FakeRandomProvider(doubles = listOf(NEVER_HITS))
    }
}
