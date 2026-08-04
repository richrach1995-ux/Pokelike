package com.pokelike.idle.util

import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zufallsquelle des Spiels.
 *
 * Als Schnittstelle ausgelegt, und das ist keine Formalie: Ein direkter Aufruf
 * von [Random] macht jede Regel, die auf Zufall beruht, praktisch unpruefbar.
 * Ein Test muesste dann tausende Durchlaeufe rechnen und statistisch schaetzen -
 * langsam, und er wuerde gelegentlich ohne Fehler im Code fehlschlagen.
 *
 * Mit dieser Schnittstelle gibt der Test die Zahlen vor und prueft damit exakt
 * die Grenzfaelle, an denen Fehler sitzen: Was passiert bei genau dem Wert, der
 * die Kritschwelle trifft? Zaehlt der Rand als Treffer oder nicht?
 *
 * Ueber Klicks hinaus wird die Quelle noch von Events, Glueckrad, Mystery Box
 * und Beuteverteilung gebraucht - alles Stellen, an denen ein Rechenfehler
 * unmittelbar Geld kostet.
 */
interface RandomProvider {

    /** Gleichverteilter Wert aus dem halboffenen Intervall `[0, 1)`. */
    fun nextDouble(): Double

    /**
     * Gleichverteilte ganze Zahl aus `[0, untilExclusive)`.
     *
     * @throws IllegalArgumentException wenn [untilExclusive] nicht positiv ist.
     */
    fun nextInt(untilExclusive: Int): Int

    /**
     * Wuerfelt gegen eine Wahrscheinlichkeit.
     *
     * Bewusst hier und nicht an jeder Aufrufstelle erneut: Die Regel, ob der
     * Rand als Treffer zaehlt, muss ueberall dieselbe sein. Bei einer
     * Wahrscheinlichkeit von 0 darf nie ausgeloest werden, bei 1 immer - beides
     * folgt hier direkt aus dem halboffenen Intervall von [nextDouble].
     */
    fun rollChance(probability: Double): Boolean {
        require(probability in 0.0..1.0) { "Wahrscheinlichkeit ausserhalb von 0..1: $probability" }
        if (probability <= 0.0) return false
        if (probability >= 1.0) return true
        return nextDouble() < probability
    }
}

/** Produktionsimplementierung auf Basis des Standard-Zufallsgenerators. */
@Singleton
class DefaultRandomProvider @Inject constructor() : RandomProvider {

    override fun nextDouble(): Double = Random.nextDouble()

    override fun nextInt(untilExclusive: Int): Int {
        require(untilExclusive > 0) { "Obergrenze muss positiv sein: $untilExclusive" }
        return Random.nextInt(untilExclusive)
    }
}
