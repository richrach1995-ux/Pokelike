package com.pokelike.idle.domain.model

/**
 * Die freigeschalteten Achievements.
 *
 * Eine Menge, kein Zaehler: Ein Achievement wird genau einmal erreicht. Der
 * Aufbau entspricht [UpgradeInventory] - dieselbe Frage, dieselbe Antwort.
 */
class AchievementInventory private constructor(
    private val unlocked: Set<AchievementType>,
) {

    operator fun contains(type: AchievementType): Boolean = type in unlocked

    val count: Int get() = unlocked.size

    val isEmpty: Boolean get() = unlocked.isEmpty()

    /**
     * Fuegt ein Achievement hinzu.
     *
     * Ein bereits vorhandenes laesst die Menge unveraendert. Diese Zusicherung
     * ist hier besonders wichtig: Die Pruefung laeuft im Sekundentakt, und eine
     * doppelte Aufnahme wuerde die Belohnung erneut ausschuetten.
     */
    fun plus(type: AchievementType): AchievementInventory =
        if (type in unlocked) this else AchievementInventory(unlocked + type)

    fun asSet(): Set<AchievementType> = unlocked

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AchievementInventory) return false
        return unlocked == other.unlocked
    }

    override fun hashCode(): Int = unlocked.hashCode()

    override fun toString(): String =
        unlocked.joinToString(prefix = "AchievementInventory(", postfix = ")") { it.id }

    companion object {

        val EMPTY: AchievementInventory = AchievementInventory(emptySet())

        fun of(types: Set<AchievementType>): AchievementInventory =
            if (types.isEmpty()) EMPTY else AchievementInventory(types.toSet())

        fun of(vararg types: AchievementType): AchievementInventory = of(types.toSet())
    }
}
