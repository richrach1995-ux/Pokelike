package com.pokelike.idle.domain.model

/**
 * Vom Spieler gewaehlte Darstellungsvariante.
 *
 * [SYSTEM] ist die Voreinstellung: Wer sein Geraet auf Dunkelmodus gestellt
 * hat, erwartet ihn auch hier, ohne ihn erneut zu waehlen.
 */
enum class ThemeMode(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        /**
         * Faellt bei unbekanntem Schluessel auf [SYSTEM] zurueck.
         *
         * Kann auftreten, wenn ein Nutzer nach einem Update auf eine aeltere
         * App-Version zurueckwechselt. Eine Einstellung ist es nicht wert,
         * dafuer den Start abzubrechen.
         */
        fun fromId(id: String?): ThemeMode =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/**
 * Geraetebezogene Einstellungen.
 *
 * Bewusst getrennt vom Spielstand: Diese Werte beschreiben, wie der Spieler
 * sein Geraet bedienen moechte, nicht seinen Fortschritt. Deshalb liegen sie in
 * DataStore statt in der Datenbank, duerfen im Geraete-Backup mitgesichert
 * werden und sind vom Prestige-Reset nicht betroffen.
 *
 * @property themeMode Helles oder dunkles Erscheinungsbild.
 * @property musicEnabled Hintergrundmusik.
 * @property soundEnabled Klangeffekte fuer Klicks, Kaeufe und Events.
 * @property vibrationEnabled Haptisches Feedback beim Klicken.
 * @property showFps Blendet die Bildrate ein. Fuer Fehlerberichte nuetzlich.
 */
data class GameSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showFps: Boolean = false,
)
