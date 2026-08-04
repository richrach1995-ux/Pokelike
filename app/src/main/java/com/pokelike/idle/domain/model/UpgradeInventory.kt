package com.pokelike.idle.domain.model

/**
 * Die gekauften Upgrades.
 *
 * Eine Menge und keine Abbildung mit Anzahlen: Upgrades werden genau einmal
 * gekauft. Waere eine Anzahl moeglich, muesste jede Wirkung zusaetzlich
 * festlegen, wie sie sich bei Mehrfachbesitz verhaelt - eine Frage, die sich
 * gar nicht erst stellen soll.
 *
 * Unveraenderlich wie [ResourcePool] und [BuildingInventory].
 */
class UpgradeInventory private constructor(
    private val owned: Set<UpgradeType>,
) {

    operator fun contains(type: UpgradeType): Boolean = type in owned

    val count: Int get() = owned.size

    val isEmpty: Boolean get() = owned.isEmpty()

    /**
     * Fuegt ein Upgrade hinzu.
     *
     * Ein bereits vorhandenes Upgrade laesst die Menge unveraendert. Der
     * Kaufablauf verhindert den Doppelkauf ohnehin; diese Zusicherung stellt
     * sicher, dass ein Fehler dort nicht zu doppelter Wirkung fuehrt.
     */
    fun plus(type: UpgradeType): UpgradeInventory =
        if (type in owned) this else UpgradeInventory(owned + type)

    /** Alle gekauften Upgrades, etwa zum Speichern oder Auswerten. */
    fun asSet(): Set<UpgradeType> = owned

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UpgradeInventory) return false
        return owned == other.owned
    }

    override fun hashCode(): Int = owned.hashCode()

    override fun toString(): String =
        owned.joinToString(prefix = "UpgradeInventory(", postfix = ")") { it.id }

    companion object {

        val EMPTY: UpgradeInventory = UpgradeInventory(emptySet())

        fun of(types: Set<UpgradeType>): UpgradeInventory =
            if (types.isEmpty()) EMPTY else UpgradeInventory(types.toSet())

        fun of(vararg types: UpgradeType): UpgradeInventory = of(types.toSet())
    }
}
