package com.pokelike.idle.domain.model

import com.pokelike.idle.config.GameConfig

/**
 * Vollstaendiger Spielstand.
 *
 * Wurzel des Domaenenmodells: Alles, was einen Neustart der App ueberdauern
 * muss, haengt hier. Ein einziges unveraenderliches Objekt statt verstreuter
 * Einzelwerte hat zwei praktische Vorteile:
 *
 * - Speichern heisst, genau ein Objekt zu schreiben. Es kann keinen Zustand
 *   geben, in dem der Kontostand schon gesichert ist, die Statistik aber noch
 *   nicht.
 * - Der Offline-Fortschritt ist eine reine Funktion `(Zustand, Zeit) -> Zustand`
 *   und laesst sich ohne Datenbank und ohne Android testen.
 *
 * Gebaeude, Upgrades, Booster, Quests und Achievements kommen in den folgenden
 * Schritten als weitere Felder hinzu - jeweils dann, wenn das zugehoerige
 * System tatsaechlich existiert.
 *
 * @property schemaVersion Version des Spielstandformats. Wird beim Laden
 *   geprueft, um eine Migration anzustossen. Ohne dieses Feld liesse sich ein
 *   alter Spielstand nach einem Update nicht von einem beschaedigten
 *   unterscheiden.
 * @property resources Aktueller Kontostand.
 * @property statistics Lebenslange Kennzahlen.
 * @property createdAtMillis Zeitpunkt des ersten Starts (Systemzeit).
 * @property lastSeenAtMillis Zeitpunkt der letzten Sicherung (Systemzeit).
 *   Grundlage der Offline-Berechnung.
 */
data class GameState(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val resources: ResourcePool = ResourcePool.EMPTY,
    val statistics: GameStatistics = GameStatistics(),
    val createdAtMillis: Long = 0L,
    val lastSeenAtMillis: Long = 0L,
) {

    /**
     * Kontostand einer Ressource.
     *
     * Abkuerzung fuer `state.resources[type]`. Der Zugriff kommt in Use Cases
     * und Anzeigen so haeufig vor, dass die Zwischenstufe nur Rauschen waere.
     */
    operator fun get(type: ResourceType): BigNumber = resources[type]

    /**
     * Fuehrt einen Prestige-Reset aus.
     *
     * Zurueckgesetzt wird ausschliesslich, was
     * [ResourceType.resetOnPrestige] als vergaenglich markiert. Die Statistik
     * bleibt vollstaendig erhalten und zaehlt lediglich den Durchlauf hoch -
     * sonst verloere der Spieler mit jedem Prestige seine Achievements.
     *
     * @param prestigePointsEarned Gutschrift fuer diesen Durchlauf. Die
     *   Berechnung dieser Menge gehoert in den Prestige-Use-Case, nicht hierher:
     *   Sie ist Balancing und wird sich oft aendern, waehrend die Reset-Regel
     *   selbst stabil bleibt.
     */
    fun afterPrestige(prestigePointsEarned: BigNumber): GameState {
        require(!prestigePointsEarned.isNegative) {
            "Negative Prestige-Gutschrift: $prestigePointsEarned"
        }

        return copy(
            resources = resources
                .resetForPrestige()
                .grant(ResourceType.PRESTIGE_POINTS, prestigePointsEarned),
            statistics = statistics.copy(
                prestigeCount = statistics.prestigeCount + 1,
                lifetimeEarned = statistics.lifetimeEarned + ResourceBundle.single(
                    type = ResourceType.PRESTIGE_POINTS,
                    amount = prestigePointsEarned,
                ),
            ),
        )
    }

    companion object {

        /**
         * Aktuelle Version des Spielstandformats.
         *
         * Muss erhoeht werden, sobald sich die Struktur so aendert, dass ein
         * alter Spielstand nicht mehr unveraendert gelesen werden kann.
         */
        const val CURRENT_SCHEMA_VERSION: Int = 1

        /**
         * Spielstand fuer einen neuen Spieler.
         *
         * @param nowMillis Aktuelle Systemzeit. Wird hereingereicht statt hier
         *   abgefragt, damit die Funktion rein bleibt und im Test ein fester
         *   Zeitpunkt vorgegeben werden kann.
         */
        fun newGame(nowMillis: Long): GameState = GameState(
            resources = ResourcePool.of(
                ResourceType.COINS to BigNumber.of(GameConfig.STARTING_COINS),
                ResourceType.DIAMONDS to BigNumber.of(GameConfig.STARTING_DIAMONDS),
            ),
            statistics = GameStatistics(sessionCount = 1),
            createdAtMillis = nowMillis,
            lastSeenAtMillis = nowMillis,
        )
    }
}
