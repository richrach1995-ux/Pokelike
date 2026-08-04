package com.pokelike.idle.domain.model

/**
 * Besitzstand an Gebaeuden.
 *
 * Unveraenderlich, wie [ResourcePool]. Jeder Kauf liefert ein neues Inventar.
 *
 * Anzahlen sind [Int] und nicht [BigNumber]: Gebaeude werden einzeln gekauft,
 * und der Preis waechst mit 1,15 hoch Anzahl. Bereits bei wenigen tausend
 * Exemplaren uebersteigt der Preis jede erreichbare Muenzmenge, ein Ueberlauf
 * ist also ausgeschlossen. Der grosse Zahlentyp waere hier nur Aufwand ohne
 * Nutzen.
 */
class BuildingInventory private constructor(
    private val counts: Map<BuildingType, Int>,
) {

    /** Anzahl eines Gebaeudes. Nicht gefuehrte Gebaeude stehen auf null. */
    operator fun get(type: BuildingType): Int = counts[type] ?: 0

    /** Gesamtzahl aller Gebaeude. Grundlage mehrerer Achievements. */
    val totalCount: Int get() = counts.values.sum()

    /** Anzahl verschiedener Gebaeudearten im Besitz. */
    val distinctCount: Int get() = counts.size

    val isEmpty: Boolean get() = counts.isEmpty()

    /**
     * Fuegt Exemplare hinzu.
     *
     * @throws IllegalArgumentException bei negativer Anzahl. Ein Kauf mit
     *   negativer Menge wuerde Gebaeude entfernen und dabei den Kaufpreis
     *   gutschreiben.
     */
    fun plus(type: BuildingType, count: Int): BuildingInventory {
        require(count >= 0) { "Negative Anzahl fuer ${type.id}: $count" }
        if (count == 0) return this
        return BuildingInventory(counts + (type to (this[type] + count)))
    }

    /** Momentaufnahme, etwa zum Speichern. */
    fun asMap(): Map<BuildingType, Int> = counts

    /**
     * Setzt alle Gebaeude zurueck.
     *
     * Der Prestige-Reset entfernt den gesamten Besitz. Das ist der Kern des
     * Systems: Der Spieler gibt seinen Ausbau auf und erhaelt dafuer dauerhafte
     * Boni.
     */
    fun cleared(): BuildingInventory = EMPTY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BuildingInventory) return false
        return counts == other.counts
    }

    override fun hashCode(): Int = counts.hashCode()

    override fun toString(): String =
        counts.entries.joinToString(prefix = "BuildingInventory(", postfix = ")") { (type, count) ->
            "${type.id}=$count"
        }

    companion object {

        val EMPTY: BuildingInventory = BuildingInventory(emptyMap())

        /**
         * Erzeugt ein Inventar aus vorhandenen Anzahlen.
         *
         * Eintraege mit null werden entfernt, damit zwei inhaltlich gleiche
         * Inventare auch als gleich gelten - dieselbe Ueberlegung wie bei
         * [ResourcePool].
         */
        fun of(counts: Map<BuildingType, Int>): BuildingInventory {
            val sanitized = buildMap {
                counts.forEach { (type, count) ->
                    require(count >= 0) { "Negative Anzahl fuer ${type.id}: $count" }
                    if (count > 0) put(type, count)
                }
            }
            return if (sanitized.isEmpty()) EMPTY else BuildingInventory(sanitized)
        }

        fun of(vararg entries: Pair<BuildingType, Int>): BuildingInventory = of(entries.toMap())
    }
}
