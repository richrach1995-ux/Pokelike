package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.battle.Battler
import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.battle.Move
import com.runeveil.saga.domain.model.battle.MoveEffect
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.util.Rng
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The single source of truth for how much a hit hurts.
 *
 *  core       = ((2 × level / 5 + 2) × power × ATK / DEF) / 50 + 2
 *  damage     = core × affinity × effectiveness × weather × phase × crit ×
 *               variance × situational
 *
 * All intermediate multipliers are surfaced on [DamageResult] so the UI can
 * explain a hit ("2× effective, critical, boosted by the storm") and so the AI
 * can score a move without duplicating the formula.
 */
object DamageCalculator {

    /** Damage bonus when the move's element matches one of the user's. */
    const val AFFINITY_BONUS = 1.5

    /** Bonus when the user additionally carries a bound rune of that element. */
    const val RUNE_AFFINITY_BONUS = 2.0

    /** Damage multiplier of a critical hit. */
    const val CRITICAL_MULTIPLIER = 1.75

    /** Lower bound of the random damage roll (upper bound is 1.0). */
    const val VARIANCE_MIN = 0.85

    /** Base critical-hit chance per critical stage. */
    private val CRIT_CHANCE_BY_STAGE = doubleArrayOf(0.0625, 0.125, 0.25, 0.375, 0.5)

    data class DamageResult(
        val damage: Int,
        val effectiveness: Effectiveness,
        val effectivenessMultiplier: Double,
        val critical: Boolean,
        val affinity: Double,
        val weatherMultiplier: Double,
        val phaseMultiplier: Double,
        val variance: Double,
        val blocked: Boolean = false,
    ) {
        val isImmune: Boolean get() = effectivenessMultiplier <= 0.0
    }

    /**
     * Full damage roll for one hit of [move] from [attacker] onto [defender].
     *
     * @param comboBonus multiplier granted by a Combination Strike (1.0 = none).
     * @param ignoreDefenceStages set by moves carrying
     *   [MoveEffect.IgnoreDefenceStages].
     */
    fun compute(
        attacker: Battler,
        defender: Battler,
        move: Move,
        weather: BattleWeather,
        dayPhase: DayPhase,
        rng: Rng,
        comboBonus: Double = 1.0,
        forceCritical: Boolean = false,
        ignoreDefenceStages: Boolean = false,
        extraMultiplier: Double = 1.0,
    ): DamageResult {
        val effectivenessMultiplier = TypeChart.multiplier(
            attacker = move.element,
            primary = defender.monster.primaryElement,
            secondary = defender.monster.secondaryElement,
        )
        val effectiveness = TypeChart.describe(effectivenessMultiplier)
        if (effectivenessMultiplier <= 0.0) {
            return DamageResult(
                damage = 0,
                effectiveness = effectiveness,
                effectivenessMultiplier = 0.0,
                critical = false,
                affinity = 1.0,
                weatherMultiplier = 1.0,
                phaseMultiplier = 1.0,
                variance = 1.0,
            )
        }

        val level = attacker.monster.level
        val attackStat = attacker.effectiveStat(move.offensiveStat)
        val rawDefence = defender.monster.stats[move.defensiveStat]
        val defenceStat = if (ignoreDefenceStages) {
            // Positive stages are ignored, negative ones still count.
            val stage = defender.stages[move.defensiveStat].coerceAtMost(0)
            (rawDefence * com.runeveil.saga.domain.model.monster.StatStages.stageMultiplier(stage))
                .toInt().coerceAtLeast(1)
        } else {
            defender.effectiveStat(move.defensiveStat)
        }

        val critical = forceCritical || rollCritical(attacker, move, rng)
        // A critical hit ignores the defender's positive defence stages.
        val effectiveDefence = if (critical && !ignoreDefenceStages) {
            minOf(defenceStat, rawDefence.coerceAtLeast(1))
        } else {
            defenceStat
        }

        val core = floor(
            floor((2.0 * level / 5.0 + 2.0) * move.power * attackStat / effectiveDefence) / 50.0,
        ) + 2.0

        val affinity = affinityBonus(attacker, move.element)
        val weatherMultiplier = weather.damageMultiplier(move.element)
        val phaseMultiplier = phaseBonus(move.element, dayPhase)
        val criticalMultiplier = if (critical) CRITICAL_MULTIPLIER else 1.0
        val variance = VARIANCE_MIN + rng.nextDouble() * (1.0 - VARIANCE_MIN)

        val total = core *
            affinity *
            effectivenessMultiplier *
            weatherMultiplier *
            phaseMultiplier *
            criticalMultiplier *
            comboBonus *
            extraMultiplier *
            variance

        return DamageResult(
            damage = total.roundToInt().coerceAtLeast(1),
            effectiveness = effectiveness,
            effectivenessMultiplier = effectivenessMultiplier,
            critical = critical,
            affinity = affinity,
            weatherMultiplier = weatherMultiplier,
            phaseMultiplier = phaseMultiplier,
            variance = variance,
        )
    }

    /**
     * Expected damage without any randomness — used by the AI, by the bestiary
     * "matchup" preview and by the balance test suite.
     */
    fun expectedDamage(
        attacker: Battler,
        defender: Battler,
        move: Move,
        weather: BattleWeather = BattleWeather.CLEAR,
        dayPhase: DayPhase = DayPhase.DAY,
    ): Int {
        val effectiveness = TypeChart.multiplier(
            move.element,
            defender.monster.primaryElement,
            defender.monster.secondaryElement,
        )
        if (effectiveness <= 0.0 || move.power <= 0) return 0
        val level = attacker.monster.level
        val attackStat = attacker.effectiveStat(move.offensiveStat)
        val defenceStat = defender.effectiveStat(move.defensiveStat)
        val core = floor(
            floor((2.0 * level / 5.0 + 2.0) * move.power * attackStat / defenceStat) / 50.0,
        ) + 2.0
        val meanVariance = (VARIANCE_MIN + 1.0) / 2.0
        val total = core *
            affinityBonus(attacker, move.element) *
            effectiveness *
            weather.damageMultiplier(move.element) *
            phaseBonus(move.element, dayPhase) *
            meanVariance
        return total.roundToInt().coerceAtLeast(1)
    }

    /** Same-element bonus, raised further when a matching rune is bound. */
    fun affinityBonus(attacker: Battler, element: Element): Double {
        val matchesType = element == attacker.monster.primaryElement ||
            element == attacker.monster.secondaryElement
        if (!matchesType) return 1.0
        val hasMatchingRune = attacker.monster.boundRuneIds.any {
            it.endsWith("_${element.name.lowercase()}")
        }
        return if (hasMatchingRune) RUNE_AFFINITY_BONUS else AFFINITY_BONUS
    }

    /**
     * Day/night affinity: Light and Divine burn brighter by day, Shadow and
     * Spirit by night. ±10 % — enough to matter, small enough not to dictate
     * team building.
     */
    fun phaseBonus(element: Element, phase: DayPhase): Double = when (element) {
        Element.LIGHT, Element.DIVINE -> if (phase.isNight) 0.90 else 1.10
        Element.SHADOW, Element.SPIRIT -> if (phase.isNight) 1.10 else 0.90
        else -> 1.0
    }

    /** Critical-hit roll. Luck adds up to ~4 percentage points at 300 LCK. */
    fun rollCritical(attacker: Battler, move: Move, rng: Rng): Boolean {
        val stage = (move.critStageBonus + attacker.monster.boundRuneIds.count { it == "rune_keen" })
            .coerceIn(0, CRIT_CHANCE_BY_STAGE.lastIndex)
        val luckBonus = attacker.effectiveStat(Stat.LUCK) / 7500.0
        return rng.chance(CRIT_CHANCE_BY_STAGE[stage] + luckBonus)
    }

    /**
     * Damage taken when a confused monster hits itself: a 40-power typeless
     * physical hit against its own defence.
     */
    fun confusionSelfDamage(battler: Battler, rng: Rng): Int {
        val level = battler.monster.level
        val attack = battler.effectiveStat(Stat.ATTACK)
        val defence = battler.effectiveStat(Stat.DEFENSE)
        val core = floor(floor((2.0 * level / 5.0 + 2.0) * 40.0 * attack / defence) / 50.0) + 2.0
        val variance = VARIANCE_MIN + rng.nextDouble() * (1.0 - VARIANCE_MIN)
        return (core * variance).roundToInt().coerceAtLeast(1)
    }

    /**
     * Struggle-style fallback used when every move is out of PP: a 50-power
     * typeless hit that costs the user a quarter of the damage dealt.
     */
    fun desperationDamage(attacker: Battler, defender: Battler, rng: Rng): Int {
        val level = attacker.monster.level
        val attack = attacker.effectiveStat(Stat.ATTACK)
        val defence = defender.effectiveStat(Stat.DEFENSE)
        val core = floor(floor((2.0 * level / 5.0 + 2.0) * 50.0 * attack / defence) / 50.0) + 2.0
        val variance = VARIANCE_MIN + rng.nextDouble() * (1.0 - VARIANCE_MIN)
        return (core * variance).roundToInt().coerceAtLeast(1)
    }
}
