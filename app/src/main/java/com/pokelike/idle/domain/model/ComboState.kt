package com.pokelike.idle.domain.model

import com.pokelike.idle.config.GameConfig

/**
 * Stand der laufenden Klick-Combo.
 *
 * Bewusst nicht Teil von [GameState] und damit nicht gespeichert: Eine Combo
 * lebt Sekunden. Sie ueber einen Neustart zu retten waere technisch moeglich,
 * aber sinnwidrig - sie belohnt aufmerksames Spielen im Moment, nicht Besitz.
 *
 * Die Zeitangaben beziehen sich auf die monotone Uhr
 * ([com.pokelike.idle.util.TimeSource.elapsedRealtime]), nicht auf die
 * Systemzeit. Ein Verstellen der Geraeteuhr darf eine abgelaufene Combo weder
 * verlaengern noch beenden.
 *
 * @property count Anzahl der Klicks in ununterbrochener Folge.
 * @property lastClickAtMillis Zeitpunkt des letzten Klicks, monotone Uhr.
 */
data class ComboState(
    val count: Int,
    val lastClickAtMillis: Long,
) {

    val isActive: Boolean get() = count > 0

    /**
     * Ob das Zeitfenster abgelaufen ist.
     *
     * Der Vergleich ist bewusst `>=`: Bei genau abgelaufenem Fenster gilt die
     * Combo als beendet. Andernfalls haenge das Ergebnis davon ab, ob ein Takt
     * eine Millisekunde frueher oder spaeter eintrifft.
     */
    fun isExpired(
        nowMillis: Long,
        windowMillis: Long = GameConfig.COMBO_WINDOW_MS,
    ): Boolean = !isActive || (nowMillis - lastClickAtMillis) >= windowMillis

    /**
     * Verrechnet einen Klick.
     *
     * Faellt er in das Zeitfenster, waechst die Combo. Andernfalls beginnt sie
     * neu bei eins - der Klick selbst zaehlt bereits als erste Stufe.
     */
    fun advance(
        nowMillis: Long,
        windowMillis: Long = GameConfig.COMBO_WINDOW_MS,
    ): ComboState = if (isExpired(nowMillis, windowMillis)) {
        ComboState(count = 1, lastClickAtMillis = nowMillis)
    } else {
        ComboState(count = count + 1, lastClickAtMillis = nowMillis)
    }

    /**
     * Ertragsfaktor aus der Combo.
     *
     * Die erste Stufe bringt noch keinen Bonus: Ein einzelner Klick ist keine
     * Serie. Gezaehlt werden die Fortsetzungen, gedeckelt bei [maxSteps].
     */
    fun multiplier(
        stepBonus: Double = GameConfig.COMBO_STEP_BONUS,
        maxSteps: Int = GameConfig.COMBO_MAX_STEPS,
    ): Double {
        if (!isActive) return 1.0
        val steps = (count - 1).coerceIn(0, maxSteps)
        return 1.0 + steps * stepBonus
    }

    /**
     * Verbleibender Anteil des Zeitfensters, von 1.0 auf 0.0.
     *
     * Grundlage der ablaufenden Anzeige. Die Anzeige ist nicht Zierde: Ohne
     * sie kann der Spieler nicht einschaetzen, wie viel Zeit ihm bleibt, und
     * die Combo wirkt willkuerlich.
     */
    fun remainingFraction(
        nowMillis: Long,
        windowMillis: Long = GameConfig.COMBO_WINDOW_MS,
    ): Float {
        if (!isActive || windowMillis <= 0L) return 0f
        val elapsed = nowMillis - lastClickAtMillis
        val remaining = (windowMillis - elapsed).toFloat() / windowMillis
        return remaining.coerceIn(0f, 1f)
    }

    companion object {
        /** Keine laufende Combo. */
        val NONE = ComboState(count = 0, lastClickAtMillis = 0L)
    }
}
