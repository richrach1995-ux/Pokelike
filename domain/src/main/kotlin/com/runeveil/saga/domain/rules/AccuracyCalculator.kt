package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.battle.Battler
import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.battle.Move
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.util.Rng

/**
 * Hit/miss and turn-order mathematics.
 *
 * Accuracy is a product of the move's printed accuracy, the attacker's and
 * defender's accuracy/evasion stages, the weather and a small speed-derived
 * evasion term — so a very fast, very lucky monster is genuinely slippery
 * without ever becoming untouchable (the result is clamped to 33…100 %).
 */
object AccuracyCalculator {

    /** Nothing can drop below this hit chance. */
    const val MIN_HIT_CHANCE = 0.33

    /** Nothing above this either, except moves flagged [Move.alwaysHits]. */
    const val MAX_HIT_CHANCE = 1.0

    fun hitChance(
        attacker: Battler,
        defender: Battler,
        move: Move,
        weather: BattleWeather,
    ): Double {
        if (move.alwaysHits) return MAX_HIT_CHANCE
        val printed = move.accuracy.coerceIn(1, 100) / 100.0
        val stageRatio = attacker.stages.accuracyMultiplier / defender.stages.evasionMultiplier
        val speedEvasion = speedEvasionFactor(attacker, defender)
        val luckEvasion = 1.0 - (defender.effectiveStat(Stat.LUCK) / 12000.0)
        val chance = printed * stageRatio * speedEvasion * luckEvasion *
            weather.accuracyModifier / weather.evasionModifier
        return chance.coerceIn(MIN_HIT_CHANCE, MAX_HIT_CHANCE)
    }

    fun rollHit(
        attacker: Battler,
        defender: Battler,
        move: Move,
        weather: BattleWeather,
        rng: Rng,
    ): Boolean = rng.chance(hitChance(attacker, defender, move, weather))

    /**
     * A defender that is faster than the attacker gains up to 6 % evasion;
     * a slower one loses up to 6 %.
     */
    private fun speedEvasionFactor(attacker: Battler, defender: Battler): Double {
        val attackerSpeed = attacker.effectiveStat(Stat.SPEED).toDouble()
        val defenderSpeed = defender.effectiveStat(Stat.SPEED).toDouble()
        if (attackerSpeed <= 0.0) return 1.0
        val ratio = (defenderSpeed - attackerSpeed) / (defenderSpeed + attackerSpeed)
        return (1.0 - ratio * 0.12).coerceIn(0.88, 1.12)
    }

    /**
     * Turn order: priority first, then effective speed, then a coin flip on a
     * tie. Paralysis already quarters speed via
     * [com.runeveil.saga.domain.model.battle.StatusCondition.statPenalty].
     */
    fun initiativeScore(battler: Battler, priority: Int, weather: BattleWeather): Double =
        priority * 10_000.0 + battler.effectiveStat(Stat.SPEED) * weather.speedModifier

    /**
     * Chance to flee a wild battle. Rises with every failed attempt so the
     * player is never stuck.
     */
    fun fleeChance(runner: Battler, fastestEnemy: Battler, attempts: Int): Double {
        val runnerSpeed = runner.effectiveStat(Stat.SPEED).toDouble()
        val enemySpeed = fastestEnemy.effectiveStat(Stat.SPEED).toDouble().coerceAtLeast(1.0)
        val base = (runnerSpeed / enemySpeed) * 0.55 + attempts * 0.15
        return base.coerceIn(0.20, 0.98)
    }
}
