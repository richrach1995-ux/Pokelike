package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.battle.Battler
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.item.ItemEffect
import com.runeveil.saga.domain.model.item.OrbCondition
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.World
import com.runeveil.saga.domain.util.Rng
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The Runenkugel capture system.
 *
 * A throw is resolved in two stages so the animation can be dramatic:
 *
 *  1. a **capture score** `a` is computed from the target's HP, status, level,
 *     rarity and the orb used;
 *  2. that score is converted into a **shake threshold**; the orb then rolls
 *     four independent shakes. Four successes = capture. The number of shakes
 *     that succeeded is reported so the UI can play 0…3 wobbles before a break.
 *
 * The two-stage design means a near-miss genuinely *was* a near-miss — the
 * shake count is real information, not theatre.
 */
object CaptureCalculator {

    /** Number of successful shakes required for a capture. */
    const val REQUIRED_SHAKES = 4

    /** Maximum value of the capture score. */
    private const val MAX_SCORE = 255.0

    data class CaptureResult(
        val captured: Boolean,
        val shakes: Int,
        val captureChancePerShake: Double,
        val overallChance: Double,
        val guaranteed: Boolean = false,
    )

    /**
     * @param orbItem the thrown orb; must carry an [Item.orbSpec].
     * @param turnCount how many turns the battle has lasted (for
     *   [OrbCondition.LONG_BATTLE] / [OrbCondition.FIRST_TURN]).
     * @param playerCaptureBonusPercent bonus from equipment and titles.
     */
    fun resolve(
        target: Battler,
        orbItem: Item,
        dayPhase: DayPhase,
        weather: BattleWeather,
        turnCount: Int,
        currentWorld: World?,
        playerCaptureBonusPercent: Int,
        rng: Rng,
    ): CaptureResult {
        if (orbItem.effects.any { it is ItemEffect.GuaranteedCapture }) {
            return CaptureResult(
                captured = true,
                shakes = REQUIRED_SHAKES,
                captureChancePerShake = 1.0,
                overallChance = 1.0,
                guaranteed = true,
            )
        }

        val chance = captureChance(
            target = target,
            orbItem = orbItem,
            dayPhase = dayPhase,
            weather = weather,
            turnCount = turnCount,
            currentWorld = currentWorld,
            playerCaptureBonusPercent = playerCaptureBonusPercent,
        )
        // Per-shake probability such that shake^4 == overall chance.
        val perShake = chance.pow(1.0 / REQUIRED_SHAKES)

        var shakes = 0
        while (shakes < REQUIRED_SHAKES) {
            if (!rng.chance(perShake)) break
            shakes++
        }
        return CaptureResult(
            captured = shakes >= REQUIRED_SHAKES,
            shakes = shakes,
            captureChancePerShake = perShake,
            overallChance = chance,
        )
    }

    /**
     * The probability (0…1) that a throw succeeds. Exposed separately so the
     * UI can show a "Fangchance" estimate once the player owns the Seher-Auge
     * relic, and so tests can assert monotonicity.
     */
    fun captureChance(
        target: Battler,
        orbItem: Item,
        dayPhase: DayPhase,
        weather: BattleWeather,
        turnCount: Int,
        currentWorld: World?,
        playerCaptureBonusPercent: Int,
    ): Double {
        val spec = orbItem.orbSpec ?: return 0.0
        val species = target.monster.species
        val maxHp = target.monster.maxHp.coerceAtLeast(1)
        val currentHp = target.monster.currentHp.coerceIn(0, maxHp)

        // Classic "how hurt is it" term, in 1/3 … 1 territory.
        val hpTerm = (3.0 * maxHp - 2.0 * currentHp) / (3.0 * maxHp)

        val statusBonus = target.monster.status?.captureBonus ?: 1.0
        val orbBonus = spec.catchMultiplier *
            if (conditionMet(spec.condition, target, dayPhase, turnCount, currentWorld)) {
                spec.conditionalBonus
            } else {
                1.0
            }

        // Higher-level targets resist; the term is gentle (level 100 ≈ 0.55×).
        val levelTerm = (1.0 - target.monster.level / 220.0).coerceIn(0.45, 1.0)
        val rarityTerm = 1.0 / species.rarity.captureResistance
        val weatherTerm = if (weather == BattleWeather.FOG) 1.10 else 1.0
        val weakenTerm = target.captureWeakness
        val playerTerm = 1.0 + playerCaptureBonusPercent / 100.0

        val score = species.catchRate.coerceIn(1, 255) *
            hpTerm * statusBonus * orbBonus * levelTerm *
            rarityTerm * weatherTerm * weakenTerm * playerTerm

        return (score / MAX_SCORE).coerceIn(0.001, 0.995)
    }

    private fun conditionMet(
        condition: OrbCondition,
        target: Battler,
        dayPhase: DayPhase,
        turnCount: Int,
        currentWorld: World?,
    ): Boolean = when (condition) {
        OrbCondition.NONE -> false
        OrbCondition.AT_NIGHT -> dayPhase.isNight
        OrbCondition.LOW_TARGET_HP -> target.monster.hpFraction <= 0.25f
        OrbCondition.TARGET_HAS_STATUS -> target.monster.status != null
        OrbCondition.HIGH_LEVEL_TARGET -> target.monster.level >= 40
        OrbCondition.LOW_LEVEL_TARGET -> target.monster.level <= 20
        OrbCondition.LONG_BATTLE -> turnCount >= 8
        OrbCondition.FIRST_TURN -> turnCount <= 1
        OrbCondition.LEGENDARY_TARGET -> target.monster.species.rarity.ordinal >=
            com.runeveil.saga.domain.model.monster.Rarity.LEGENDARY.ordinal
        OrbCondition.MATCHING_WORLD -> currentWorld != null &&
            currentWorld == target.monster.species.nativeWorld
    }

    /** Human-readable percentage for the capture preview. */
    fun asPercent(chance: Double): Int = (chance * 100).roundToInt().coerceIn(0, 100)
}
