package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.ClickModifiers
import com.pokelike.idle.domain.model.ClickOutcome
import com.pokelike.idle.domain.model.ComboState
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.util.RandomProvider
import javax.inject.Inject

/**
 * Verrechnet einen einzelnen Klick.
 *
 * Reine Funktion bis auf die Zufallsquelle, und die ist injiziert. Dadurch
 * laesst sich jede Regel exakt pruefen, statt sie ueber tausende Durchlaeufe zu
 * schaetzen.
 *
 * Zur Reihenfolge der Verrechnung - sie ist Balancing, kein Zufall:
 *
 * ```
 * (Grundwert + flacher Zuschlag) * Faktor * Combo * Kritfaktor
 * ```
 *
 * Erst addieren, dann multiplizieren. Andersherum waeren flache Zuschlaege im
 * spaeten Spiel wertlos, weil sie nicht mehr von den Faktoren erfasst wuerden -
 * und ein Upgrade, das nichts mehr bewirkt, ist schlimmer als gar keines.
 *
 * Der Combo-Faktor wird aus dem Stand *nach* dem Klick berechnet. Der Klick,
 * der die Combo fortsetzt, soll den erreichten Bonus bereits erhalten;
 * andernfalls haenge der Spieler dauerhaft eine Stufe hinterher.
 */
class PerformClickUseCase @Inject constructor(
    private val random: RandomProvider,
) {

    /**
     * @param nowMillis Monotone Zeit. Fuer die Combo darf ausdruecklich nicht
     *   die Systemzeit verwendet werden, sonst liesse sie sich durch Verstellen
     *   der Geraeteuhr beliebig verlaengern.
     */
    operator fun invoke(
        state: GameState,
        combo: ComboState,
        modifiers: ClickModifiers,
        nowMillis: Long,
    ): ClickOutcome {
        val advancedCombo = combo.advance(nowMillis)
        val comboMultiplier = advancedCombo.multiplier()

        val isCritical = random.rollChance(modifiers.criticalChance)
        val criticalMultiplier = if (isCritical) modifiers.criticalMultiplier else 1.0

        val earned = (modifiers.baseValue + modifiers.flatBonus) *
            modifiers.multiplier *
            comboMultiplier *
            criticalMultiplier

        // grant schreibt die Lebenszeitsumme fort, withClick den Klickzaehler.
        // Beide veraendern dasselbe Statistikobjekt und muessen deshalb
        // nacheinander auf demselben Zwischenstand angewendet werden - nicht
        // beide auf dem Ausgangszustand, sonst geht eine der Aenderungen
        // verloren oder wird doppelt gezaehlt.
        val granted = state.grant(ResourceBundle.single(ResourceType.COINS, earned))
        val updated = granted.copy(statistics = granted.statistics.withClick(isCritical))

        return ClickOutcome(
            state = updated,
            combo = advancedCombo,
            earned = earned,
            wasCritical = isCritical,
            comboMultiplier = comboMultiplier,
        )
    }
}
