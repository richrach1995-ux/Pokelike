package com.runeveil.saga.domain.model.battle

import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Stat

/**
 * A single attack, spell or support action.
 *
 * Moves are shipped as content (`assets/content/moves.json`). Their behaviour
 * is fully described by [effects]; the battle engine has **no** per-move
 * special cases — every mechanic is one of the [MoveEffect] variants.
 *
 * @property power base power; `0` for pure support moves.
 * @property accuracy 0…100. `ALWAYS_HITS` (=0 with [alwaysHits]) bypasses the
 *   accuracy roll entirely.
 * @property priority higher goes first regardless of speed (−7 … +5).
 * @property critStageBonus added to the base critical-hit stage.
 * @property contact whether the move makes physical contact (matters for
 *   thorn-style abilities and for the Blood status).
 * @property comboTag moves sharing a tag can chain into a Combination Strike
 *   when used by two allies in the same turn (see `ComboRules`).
 */
data class Move(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val element: Element,
    val category: MoveCategory,
    val power: Int,
    val accuracy: Int,
    val maxPp: Int,
    val priority: Int = 0,
    val target: MoveTarget = MoveTarget.SINGLE_OPPONENT,
    val effects: List<MoveEffect> = emptyList(),
    val critStageBonus: Int = 0,
    val contact: Boolean = category == MoveCategory.PHYSICAL,
    val alwaysHits: Boolean = false,
    val ignoresProtection: Boolean = false,
    val animationKey: String,
    val soundKey: String,
    val comboTag: String? = null,
    val tier: Int = 1,
    val flavourKey: String? = null,
) {
    val isDamaging: Boolean get() = category != MoveCategory.SUPPORT && power > 0

    /** Which offensive/defensive stat pair this move uses. */
    val offensiveStat: Stat
        get() = if (category == MoveCategory.MAGICAL) Stat.MAGIC else Stat.ATTACK

    val defensiveStat: Stat
        get() = if (category == MoveCategory.MAGICAL) Stat.RESISTANCE else Stat.DEFENSE

    fun ppWithUps(ppUps: Int): Int = maxPp + (maxPp * ppUps / 5)

    companion object {
        /** Sentinel used by content for "cannot miss". */
        const val ALWAYS_HITS = 0
    }
}

/** Physical, magical or non-damaging. */
enum class MoveCategory(val displayKey: String) {
    PHYSICAL("category_physical"),
    MAGICAL("category_magical"),
    SUPPORT("category_support"),
}

/** Who a move can be aimed at. Single battles use a subset, but the engine is
 *  written for up to three monsters per side (used by boss and raid fights). */
enum class MoveTarget {
    SELF,
    SINGLE_ALLY,
    ALL_ALLIES,
    SINGLE_OPPONENT,
    ALL_OPPONENTS,
    EVERYONE,
    FIELD,
    RANDOM_OPPONENT,
    ;

    val hitsOpponents: Boolean
        get() = this == SINGLE_OPPONENT || this == ALL_OPPONENTS || this == EVERYONE || this == RANDOM_OPPONENT
}

/**
 * Every mechanic a move can carry.
 *
 * Sealed so the engine's `when` is exhaustive: adding a new mechanic breaks the
 * build until it is implemented, which is exactly what we want for a rule set
 * this large.
 *
 * @property chance probability 0.0…1.0 that the effect triggers when the move
 *   connects. Effects with `chance = 1.0` are guaranteed.
 */
sealed interface MoveEffect {
    val chance: Double

    /** Inflicts a status condition on the target. */
    data class InflictStatus(
        val condition: StatusCondition,
        override val chance: Double,
        val durationTurns: Int = 0,
    ) : MoveEffect

    /** Raises or lowers a stat by [stages] on the resolved target. */
    data class ModifyStat(
        val stat: Stat,
        val stages: Int,
        val onSelf: Boolean,
        override val chance: Double = 1.0,
    ) : MoveEffect

    /** Raises or lowers accuracy/evasion. */
    data class ModifyRatio(
        val kind: RatioKind,
        val stages: Int,
        val onSelf: Boolean,
        override val chance: Double = 1.0,
    ) : MoveEffect

    /** Heals the user for [fraction] of the damage dealt. */
    data class Drain(val fraction: Double, override val chance: Double = 1.0) : MoveEffect

    /** Damages the user for [fraction] of the damage dealt. */
    data class Recoil(val fraction: Double, override val chance: Double = 1.0) : MoveEffect

    /** Restores [fraction] of the target's max HP. */
    data class Heal(
        val fraction: Double,
        val onSelf: Boolean = true,
        override val chance: Double = 1.0,
    ) : MoveEffect

    /** Hits between [min] and [max] times; each hit rolls damage separately. */
    data class MultiHit(val min: Int, val max: Int, override val chance: Double = 1.0) : MoveEffect

    /** Sets the field weather. */
    data class SetWeather(val weather: BattleWeather, val turns: Int, override val chance: Double = 1.0) : MoveEffect

    /** Raises a damage-absorbing shield worth [fraction] of the user's max HP. */
    data class RaiseShield(val fraction: Double, val turns: Int, override val chance: Double = 1.0) : MoveEffect

    /** Blocks the next incoming attack outright. */
    data class Protect(val turns: Int = 1, override val chance: Double = 1.0) : MoveEffect

    /** Removes all stat changes from the target. */
    data class ClearStatChanges(val positiveOnly: Boolean, override val chance: Double = 1.0) : MoveEffect

    /** Cures the target's status condition. */
    data class CureStatus(val onSelf: Boolean, override val chance: Double = 1.0) : MoveEffect

    /** Damage is fixed instead of computed from stats. */
    data class FixedDamage(val amount: Int, override val chance: Double = 1.0) : MoveEffect

    /** Damage equals [fraction] of the target's *current* HP. */
    data class PercentDamage(val fraction: Double, override val chance: Double = 1.0) : MoveEffect

    /** The user must recharge and loses its next turn. */
    data class Recharge(override val chance: Double = 1.0) : MoveEffect

    /** The move needs one turn of charging before it lands. */
    data class Charge(val messageKey: String, override val chance: Double = 1.0) : MoveEffect

    /** Guarantees a critical hit. */
    data class AlwaysCritical(override val chance: Double = 1.0) : MoveEffect

    /** Ignores the target's positive defensive stat stages. */
    data class IgnoreDefenceStages(override val chance: Double = 1.0) : MoveEffect

    /** Swaps the user out for a chosen party member after damage. */
    data class SwitchOut(override val chance: Double = 1.0) : MoveEffect

    /** Raises the capture chance of the target for [turns] turns. */
    data class WeakenForCapture(val multiplier: Double, val turns: Int, override val chance: Double = 1.0) : MoveEffect

    /** Steals the target's held item when the user holds none. */
    data class StealItem(override val chance: Double = 1.0) : MoveEffect

    /** Copies the target's most recently used move into the user's slot. */
    data class MirrorMove(override val chance: Double = 1.0) : MoveEffect

    /** Deals extra damage when the target already suffers from [condition]. */
    data class BonusVersusStatus(
        val condition: StatusCondition,
        val multiplier: Double,
        override val chance: Double = 1.0,
    ) : MoveEffect

    /** Power scales with the user's missing HP (glass-cannon finishers). */
    data class ScaleWithMissingHp(val maxMultiplier: Double, override val chance: Double = 1.0) : MoveEffect

    /** Power scales with the user's positive stat stages. */
    data class ScaleWithBuffs(val perStage: Double, override val chance: Double = 1.0) : MoveEffect
}

/** Accuracy and evasion are tracked separately from the seven core stats. */
enum class RatioKind { ACCURACY, EVASION }
