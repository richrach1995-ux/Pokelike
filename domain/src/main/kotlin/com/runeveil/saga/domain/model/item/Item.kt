package com.runeveil.saga.domain.model.item

import com.runeveil.saga.domain.model.battle.StatusCondition
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.StatBlock

/**
 * Anything that can sit in the player's satchel.
 *
 * Items are pure content; their behaviour is expressed through [effects] and
 * executed by `UseItemUseCase` (out of battle) or by the battle engine
 * (in battle).
 *
 * @property price 0 means "cannot be bought"; [sellValue] defaults to half.
 * @property stackLimit per-slot cap; key items always use 1.
 */
data class Item(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val category: ItemCategory,
    val price: Int = 0,
    val sellValueOverride: Int? = null,
    val stackLimit: Int = 99,
    val usableInBattle: Boolean = false,
    val usableOnMonster: Boolean = true,
    val consumedOnUse: Boolean = true,
    val effects: List<ItemEffect> = emptyList(),
    val orbSpec: OrbSpec? = null,
    val equipmentSpec: EquipmentSpec? = null,
    val runeSpec: RuneSpec? = null,
    val iconKey: String,
    val rarityTier: Int = 1,
    val questId: String? = null,
) {
    val sellValue: Int get() = sellValueOverride ?: (price / 2)
    val isKeyItem: Boolean get() = category == ItemCategory.KEY || category == ItemCategory.QUEST
    val isOrb: Boolean get() = category == ItemCategory.RUNE_ORB && orbSpec != null
    val isEquipment: Boolean get() = category == ItemCategory.EQUIPMENT && equipmentSpec != null
}

/** Top-level grouping used by the satchel tabs. */
enum class ItemCategory(val displayKey: String, val sortOrder: Int) {
    HEALING("item_cat_healing", 0),
    RUNE_ORB("item_cat_orb", 1),
    BATTLE("item_cat_battle", 2),
    RUNE("item_cat_rune", 3),
    EQUIPMENT("item_cat_equipment", 4),
    MATERIAL("item_cat_material", 5),
    EVOLUTION("item_cat_evolution", 6),
    QUEST("item_cat_quest", 7),
    KEY("item_cat_key", 8),
    TREASURE("item_cat_treasure", 9),
}

/** Declarative item behaviour. */
sealed interface ItemEffect {
    /** Restores a flat amount of HP. */
    data class RestoreHp(val amount: Int) : ItemEffect

    /** Restores a fraction (0…1) of max HP. */
    data class RestoreHpFraction(val fraction: Double) : ItemEffect

    /** Restores PP of one move, or all moves when [allMoves]. */
    data class RestorePp(val amount: Int, val allMoves: Boolean = false) : ItemEffect

    /** Cures [conditions]; an empty set means "all". */
    data class CureStatus(val conditions: Set<StatusCondition> = emptySet()) : ItemEffect

    /** Revives a fainted monster with [hpFraction] of its max HP. */
    data class Revive(val hpFraction: Double) : ItemEffect

    /** Grants experience directly. */
    data class GrantExperience(val amount: Long) : ItemEffect

    /** Permanently raises training values. */
    data class GrantTraining(val gains: StatBlock) : ItemEffect

    /** Raises friendship. */
    data class GrantFriendship(val amount: Int) : ItemEffect

    /** Raises a stat by whole stages for the rest of the battle. */
    data class BattleStatBoost(val stat: Stat, val stages: Int) : ItemEffect

    /** Triggers an evolution path that requires this item. */
    data class TriggerEvolution(val allowedSpeciesIds: List<String> = emptyList()) : ItemEffect

    /** Teaches a specific move if the species may learn it. */
    data class TeachMove(val moveId: String) : ItemEffect

    /** Permanently raises the max PP of one move. */
    data class RaiseMaxPp(val steps: Int = 1) : ItemEffect

    /** Repels or attracts wild encounters for [steps] steps. */
    data class EncounterModifier(val multiplier: Double, val steps: Int) : ItemEffect

    /** Guarantees a capture — reserved for a single story item. */
    data object GuaranteedCapture : ItemEffect
}

/**
 * The eight grades of Runenkugel.
 *
 * @property catchMultiplier flat multiplier on the capture score.
 * @property conditionalBonus extra multiplier applied when [condition] holds.
 */
data class OrbSpec(
    val grade: OrbGrade,
    val catchMultiplier: Double,
    val conditionalBonus: Double = 1.0,
    val condition: OrbCondition = OrbCondition.NONE,
    val shakeAnimationKey: String,
)

enum class OrbGrade(val displayKey: String, val tier: Int) {
    WOOD("orb_wood", 1),
    STONE("orb_stone", 2),
    IRON("orb_iron", 3),
    SILVER("orb_silver", 4),
    GOLD("orb_gold", 5),
    MYTHIC("orb_mythic", 6),
    DIVINE("orb_divine", 7),
    LEGENDARY("orb_legendary", 8),
}

/** Situational bonuses that make the higher orb grades interesting. */
enum class OrbCondition(val displayKey: String) {
    NONE("orb_cond_none"),
    AT_NIGHT("orb_cond_night"),
    LOW_TARGET_HP("orb_cond_low_hp"),
    TARGET_HAS_STATUS("orb_cond_status"),
    HIGH_LEVEL_TARGET("orb_cond_high_level"),
    LOW_LEVEL_TARGET("orb_cond_low_level"),
    LONG_BATTLE("orb_cond_long_battle"),
    FIRST_TURN("orb_cond_first_turn"),
    LEGENDARY_TARGET("orb_cond_legendary"),
    MATCHING_WORLD("orb_cond_world"),
}

/** Gear worn by the *player character*, not by monsters. */
data class EquipmentSpec(
    val slot: EquipmentSlot,
    val statBonus: StatBlock = StatBlock.ZERO,
    val elementAffinity: Element? = null,
    val affinityBonusPercent: Int = 0,
    val captureBonusPercent: Int = 0,
    val encounterRateModifier: Double = 1.0,
    val setId: String? = null,
    val levelRequirement: Int = 1,
)

enum class EquipmentSlot(val displayKey: String) {
    WEAPON("slot_weapon"),
    ARMOR("slot_armor"),
    HELMET("slot_helmet"),
    RELIC("slot_relic"),
    RUNE("slot_rune"),
    AMULET("slot_amulet"),
}

/**
 * A rune that can be bound to a *monster* (max three), granting a stat bonus
 * and, when the element matches, the elevated affinity bonus in
 * [com.runeveil.saga.domain.rules.DamageCalculator].
 */
data class RuneSpec(
    val element: Element?,
    val statBonus: StatBlock = StatBlock.ZERO,
    val grantsAbilityId: String? = null,
    val bindCost: Int = 0,
    val tier: Int = 1,
)
