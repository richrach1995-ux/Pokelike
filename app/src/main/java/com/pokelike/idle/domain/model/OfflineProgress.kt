package com.pokelike.idle.domain.model

/**
 * Ergebnis der Offline-Berechnung.
 *
 * Enthaelt nicht nur den Ertrag, sondern auch die Umstaende seiner Entstehung.
 * Beides wird gebraucht: Der Willkommensdialog zeigt "Du warst 6 Stunden weg
 * und hast X verdient", und bei erreichtem Deckel gehoert der Hinweis dazu,
 * dass ein Offline-Booster mehr gebracht haette - der ehrlichste und zugleich
 * wirksamste Verkaufsmoment des Genres.
 *
 * @property creditedMillis Tatsaechlich angerechnete Zeit, nach Deckelung.
 * @property elapsedMillis Vergangene Zeit vor der Deckelung.
 * @property earned Gutgeschriebene Ressourcen.
 * @property wasCapped Ob die Obergrenze gegriffen hat.
 * @property clockTamperingDetected Ob die Systemuhr manipuliert wirkt. In
 *   diesem Fall wird nichts gutgeschrieben.
 */
data class OfflineProgress(
    val creditedMillis: Long,
    val elapsedMillis: Long,
    val earned: ResourceBundle,
    val wasCapped: Boolean,
    val clockTamperingDetected: Boolean,
) {

    /**
     * Ob dem Spieler ueberhaupt etwas angezeigt werden soll.
     *
     * Ein Dialog ohne Ertrag ist reine Stoerung. Bei erkannter Manipulation
     * wird bewusst ebenfalls nichts angezeigt: Ein Hinweis darauf verraet nur,
     * wo die Pruefung sitzt.
     */
    val isWorthShowing: Boolean
        get() = !clockTamperingDetected && !earned.isEmpty

    companion object {
        /** Kein Offline-Fortschritt. */
        val NONE = OfflineProgress(
            creditedMillis = 0L,
            elapsedMillis = 0L,
            earned = ResourceBundle.EMPTY,
            wasCapped = false,
            clockTamperingDetected = false,
        )
    }
}
