package com.pokelike.idle.domain.model

/**
 * Ergebnis eines Klicks.
 *
 * Enthaelt sowohl den neuen Zustand als auch alles, was die Oberflaeche fuer
 * die Rueckmeldung braucht. Beides gehoert zusammen: Ein Klick, der Muenzen
 * bringt, aber keinen sichtbaren Ertrag anzeigt, fuehlt sich an, als haette er
 * nicht funktioniert - und das Klickgefuehl ist bei diesem Genre das Erste,
 * woran ein Spieler die Qualitaet misst.
 *
 * @property state Spielstand nach dem Klick.
 * @property combo Combo-Stand nach dem Klick.
 * @property earned Ertrag dieses einen Klicks.
 * @property wasCritical Ob es ein kritischer Treffer war. Steuert Farbe,
 *   Groesse und Klang der Rueckmeldung.
 * @property comboMultiplier Wirksamer Combo-Faktor zum Zeitpunkt des Klicks.
 */
data class ClickOutcome(
    val state: GameState,
    val combo: ComboState,
    val earned: BigNumber,
    val wasCritical: Boolean,
    val comboMultiplier: Double,
)
