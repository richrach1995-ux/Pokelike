package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.model.monster.Temperament
import kotlin.math.floor

/**
 * Converts species base stats + individual genes + earned training into the
 * concrete numbers used in battle.
 *
 * The curve was tuned so that a fully-invested level-100 monster with a
 * 120-base stat lands around 340, and HP around 420 — numbers small enough to
 * stay readable on a phone screen and large enough for meaningful granularity.
 *
 *  core   = (base × 2 + gene + training / 4) × level / 100
 *  HP     = core + level + 12                     (never temperament-modified)
 *  other  = (core + 6) × temperament × talents
 */
object StatCalculator {

    /** Flat HP offset that keeps low-level monsters from dying to chip damage. */
    private const val HP_OFFSET = 12

    /** Flat offset for the six non-HP stats. */
    private const val STAT_OFFSET = 6

    fun resolve(
        base: StatBlock,
        genes: StatBlock,
        training: StatBlock,
        level: Int,
        temperament: Temperament,
        talentBonus: StatBlock = StatBlock.ZERO,
    ): StatBlock {
        val clampedLevel = level.coerceIn(1, MAX_LEVEL)
        return Stat.entries.fold(StatBlock.ZERO) { acc, stat ->
            acc.with(
                stat,
                resolveSingle(
                    stat = stat,
                    base = base[stat],
                    gene = genes[stat],
                    training = training[stat],
                    level = clampedLevel,
                    temperament = temperament,
                    talentBonus = talentBonus[stat],
                ),
            )
        }
    }

    fun resolveSingle(
        stat: Stat,
        base: Int,
        gene: Int,
        training: Int,
        level: Int,
        temperament: Temperament,
        talentBonus: Int = 0,
    ): Int {
        val safeGene = gene.coerceIn(0, StatBlock.MAX_GENE)
        val safeTraining = training.coerceIn(0, StatBlock.MAX_TRAINING_PER_STAT)
        val core = floor((base * 2.0 + safeGene + safeTraining / 4.0) * level / 100.0)
        return if (stat.isVital) {
            (core + level + HP_OFFSET).toInt() + talentBonus
        } else {
            val modified = (core + STAT_OFFSET) * temperament.multiplierFor(stat)
            floor(modified).toInt() + talentBonus
        }.coerceAtLeast(1)
    }

    /**
     * The training value awarded for defeating a monster of [defeatedBase].
     * Split across the two highest base stats of the defeated species, which
     * gives training a natural, discoverable pattern.
     */
    fun trainingYield(defeatedBase: StatBlock, level: Int): StatBlock {
        val amount = (1 + level / 25).coerceIn(1, 3)
        val topTwo = Stat.entries
            .sortedByDescending { defeatedBase[it] }
            .take(2)
        return topTwo.fold(StatBlock.ZERO) { acc, stat -> acc.with(stat, amount) }
    }

    /**
     * Adds [gain] to [current] while honouring the per-stat and total caps.
     * Excess points are discarded (matching the in-game "sated" message).
     */
    fun applyTraining(current: StatBlock, gain: StatBlock): StatBlock {
        var remainingTotal = StatBlock.MAX_TRAINING_TOTAL - current.total
        if (remainingTotal <= 0) return current
        var result = current
        for (stat in Stat.entries) {
            if (remainingTotal <= 0) break
            val room = (StatBlock.MAX_TRAINING_PER_STAT - result[stat]).coerceAtLeast(0)
            val applied = minOf(gain[stat], room, remainingTotal)
            if (applied > 0) {
                result = result.with(stat, result[stat] + applied)
                remainingTotal -= applied
            }
        }
        return result
    }

    const val MAX_LEVEL = 100
}
