package com.pokelike.idle.domain.model

import com.pokelike.idle.config.GameConfig

/**
 * Alle Gebaeude des Spiels.
 *
 * Wie bei [ResourceType] ein Enum mit Daten statt einer Klassenhierarchie: Ein
 * neues Gebaeude ist eine zusaetzliche Zeile, und der Compiler kennt jedes
 * einzelne. Weder Preis- noch Ertragsberechnung enthalten irgendwo eine
 * Fallunterscheidung nach Gebaeudeart.
 *
 * Die Zahlen folgen dem im Genre bewaehrten Verhaeltnis: Jede Stufe kostet
 * etwa das Zehn- bis Fuenfzehnfache der vorigen und bringt etwa das Sechs- bis
 * Achtfache. Daraus ergibt sich die typische Kaufkurve - jedes Gebaeude lohnt
 * sich zunaechst, wird spaeter von der naechsten Stufe abgeloest, bleibt aber
 * nie ganz wertlos.
 *
 * Ab dem Remote-Config-Schritt liefert Firebase diese Werte und die Angaben
 * hier dienen als Rueckfallebene. Deshalb sind sie so gewaehlt, dass das Spiel
 * auch vollstaendig offline sinnvoll spielbar bleibt.
 *
 * @property id Stabiler Schluessel fuer Spielstand und Analytics. Bewusst
 *   getrennt vom Enum-Namen: Eine Umbenennung im Code darf keinen Spielstand
 *   unlesbar machen.
 * @property basePrice Preis des ersten Exemplars.
 * @property baseIncomePerSecond Ertrag eines Exemplars pro Sekunde.
 */
enum class BuildingType(
    val id: String,
    val basePrice: Double,
    val baseIncomePerSecond: Double,
) {
    FINGER("finger", basePrice = 15.0, baseIncomePerSecond = 0.1),
    CURSOR("cursor", basePrice = 100.0, baseIncomePerSecond = 1.0),
    MOUSE("mouse", basePrice = 1_100.0, baseIncomePerSecond = 8.0),
    MINE("mine", basePrice = 12_000.0, baseIncomePerSecond = 47.0),
    FARM("farm", basePrice = 130_000.0, baseIncomePerSecond = 260.0),
    FACTORY("factory", basePrice = 1_400_000.0, baseIncomePerSecond = 1_400.0),
    BANK("bank", basePrice = 20_000_000.0, baseIncomePerSecond = 7_800.0),
    LAB("lab", basePrice = 330_000_000.0, baseIncomePerSecond = 44_000.0),
    SPACESHIP("spaceship", basePrice = 5_100_000_000.0, baseIncomePerSecond = 260_000.0),
    TIME_MACHINE("time_machine", basePrice = 75_000_000_000.0, baseIncomePerSecond = 1_600_000.0);

    /**
     * Ob das Gebaeude dem Spieler angezeigt wird.
     *
     * Sichtbar, sobald er es besitzt oder sich dem Preis genaehert hat. Alle
     * zehn Gebaeude von Beginn an zu zeigen waere gleich doppelt schaedlich:
     * Der Einstieg wirkt erschlagend, und die Freude an einer neu
     * erscheinenden Stufe faellt weg - einer der wenigen echten
     * Fortschrittsmomente des Genres.
     *
     * Massgeblich ist die Lebenszeitsumme, nicht der aktuelle Kontostand. Sonst
     * verschwaende ein Gebaeude wieder, sobald der Spieler sein Geld ausgibt.
     */
    fun isUnlocked(owned: Int, lifetimeCoins: BigNumber): Boolean {
        if (owned > 0) return true
        return lifetimeCoins >= BigNumber.of(basePrice * GameConfig.BUILDING_UNLOCK_FRACTION)
    }

    companion object {

        /** Nachschlagetabelle, damit [fromId] nicht jedes Mal linear sucht. */
        private val byId: Map<String, BuildingType> = entries.associateBy { it.id }

        /**
         * Sucht ein Gebaeude anhand seines stabilen Schluessels.
         *
         * Liefert bewusst `null` statt einer Ausnahme: Ein Spielstand kann
         * Gebaeude aus einer neueren Version enthalten, etwa nachdem der
         * Spieler auf eine aeltere App-Version zurueckgewechselt ist.
         */
        fun fromId(id: String): BuildingType? = byId[id]
    }
}
