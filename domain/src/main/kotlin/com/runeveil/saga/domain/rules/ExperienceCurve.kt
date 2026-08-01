package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.monster.GrowthRate
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Total-experience curve for levels 1…100.
 *
 * `total(level) = round(factor × (level³ × 0.8 + level² × 12))`
 *
 * The cubic term dominates late, the quadratic term keeps the early game brisk.
 * Values are pre-computed once per [GrowthRate] because they are queried on
 * every experience gain, every UI frame of the XP bar and by the AI when it
 * estimates enemy strength.
 */
object ExperienceCurve {

    const val MAX_LEVEL = 100

    private val tables: Map<GrowthRate, LongArray> =
        GrowthRate.entries.associateWith { rate ->
            LongArray(MAX_LEVEL + 1) { level ->
                if (level <= 1) 0L else totalForLevel(rate, level)
            }
        }

    private fun totalForLevel(rate: GrowthRate, level: Int): Long {
        val l = level.toDouble()
        return (rate.factor * (l.pow(3) * 0.8 + l.pow(2) * 12.0)).roundToLong()
    }

    /** Cumulative experience required to *be* [level]. */
    fun totalAt(rate: GrowthRate, level: Int): Long =
        tables.getValue(rate)[level.coerceIn(1, MAX_LEVEL)]

    /** Experience needed to go from [level] to `level + 1`. */
    fun stepFrom(rate: GrowthRate, level: Int): Long {
        if (level >= MAX_LEVEL) return 0L
        val table = tables.getValue(rate)
        return table[level + 1] - table[level]
    }

    /** The level a monster with [experience] total experience should be. */
    fun levelFor(rate: GrowthRate, experience: Long): Int {
        val table = tables.getValue(rate)
        // Binary search: table is strictly increasing above level 1.
        var low = 1
        var high = MAX_LEVEL
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (table[mid] <= experience) low = mid else high = mid - 1
        }
        return low
    }

    /** Experience still missing before [level] becomes `level + 1`. */
    fun remainingToNextLevel(rate: GrowthRate, level: Int, experience: Long): Long {
        if (level >= MAX_LEVEL) return 0L
        return (totalAt(rate, level + 1) - experience).coerceAtLeast(0L)
    }

    /** Progress inside the current level, 0f…1f. */
    fun progressWithinLevel(rate: GrowthRate, level: Int, experience: Long): Float {
        if (level >= MAX_LEVEL) return 1f
        val floorXp = totalAt(rate, level)
        val step = stepFrom(rate, level)
        if (step <= 0L) return 1f
        return ((experience - floorXp).toFloat() / step).coerceIn(0f, 1f)
    }

    /**
     * Experience awarded for defeating an enemy.
     *
     * `xp = baseExperience × enemyLevel × rarity × participationShare ×
     *       levelDifferenceScaling × trainerBonus`
     *
     * The level-difference term keeps grinding on weak monsters unrewarding
     * without punishing players who are slightly under-levelled.
     */
    fun rewardFor(
        baseExperience: Int,
        enemyLevel: Int,
        rarityMultiplier: Double,
        participants: Int,
        winnerLevel: Int,
        isTrainerBattle: Boolean,
        friendshipBonus: Boolean,
    ): Long {
        val share = if (participants <= 0) 1.0 else 1.0 / participants
        val levelRatio = (2.0 * enemyLevel + 10.0) / (enemyLevel + winnerLevel + 10.0)
        val scaling = levelRatio.pow(2.2).coerceIn(0.15, 2.5)
        val trainerBonus = if (isTrainerBattle) 1.5 else 1.0
        val bond = if (friendshipBonus) 1.2 else 1.0
        val raw = baseExperience * enemyLevel / 5.0 *
            rarityMultiplier * share * scaling * trainerBonus * bond
        return raw.roundToLong().coerceAtLeast(1L)
    }
}
