package com.runeveil.saga.domain.model.monster

/**
 * How rare a species is in the wild. Rarity feeds encounter tables, capture
 * difficulty, breeding cost and shiny odds.
 *
 * @property encounterWeight relative weight inside an encounter table.
 * @property captureResistance divisor applied to the capture score.
 * @property experienceMultiplier bonus experience granted when defeated.
 */
enum class Rarity(
    val displayKey: String,
    val encounterWeight: Int,
    val captureResistance: Double,
    val experienceMultiplier: Double,
) {
    COMMON("rarity_common", 1000, 1.00, 1.00),
    UNCOMMON("rarity_uncommon", 480, 1.15, 1.10),
    RARE("rarity_rare", 190, 1.35, 1.25),
    EPIC("rarity_epic", 70, 1.60, 1.45),
    LEGENDARY("rarity_legendary", 18, 2.10, 1.80),
    MYTHIC("rarity_mythic", 5, 2.60, 2.20),
    DIVINE("rarity_divine", 1, 3.20, 2.80),
    ;

    /** Divine and mythic monsters never appear in ordinary encounter tables. */
    val isStoryBound: Boolean get() = this == MYTHIC || this == DIVINE
}

/**
 * Temperaments raise one stat by 10 % and lower another by 10 %.
 * Neutral temperaments (raise == lower) exist so that every roll is valid.
 */
enum class Temperament(
    val displayKey: String,
    val raises: Stat,
    val lowers: Stat,
) {
    BOLD("temperament_bold", Stat.ATTACK, Stat.MAGIC),
    STOIC("temperament_stoic", Stat.DEFENSE, Stat.SPEED),
    ARCANE("temperament_arcane", Stat.MAGIC, Stat.ATTACK),
    WARDED("temperament_warded", Stat.RESISTANCE, Stat.ATTACK),
    SWIFT("temperament_swift", Stat.SPEED, Stat.DEFENSE),
    BLESSED("temperament_blessed", Stat.LUCK, Stat.DEFENSE),
    BRUTAL("temperament_brutal", Stat.ATTACK, Stat.RESISTANCE),
    GRIM("temperament_grim", Stat.DEFENSE, Stat.MAGIC),
    WILD("temperament_wild", Stat.SPEED, Stat.RESISTANCE),
    CUNNING("temperament_cunning", Stat.MAGIC, Stat.DEFENSE),
    PATIENT("temperament_patient", Stat.RESISTANCE, Stat.SPEED),
    LUCKLESS("temperament_luckless", Stat.ATTACK, Stat.LUCK),
    EVEN("temperament_even", Stat.SPEED, Stat.SPEED),
    CALM("temperament_calm", Stat.LUCK, Stat.LUCK),
    ;

    val isNeutral: Boolean get() = raises == lowers

    /** Multiplier applied to [stat] after the base formula. */
    fun multiplierFor(stat: Stat): Double = when {
        isNeutral -> 1.0
        stat == raises -> 1.10
        stat == lowers -> 0.90
        else -> 1.0
    }
}

/** Biological sex; needed by the breeding system. */
enum class Gender(val displayKey: String) {
    MALE("gender_male"),
    FEMALE("gender_female"),
    /** Genderless monsters (constructs, spirits) breed only with a Seidr Vessel. */
    UNKNOWN("gender_unknown"),
}

/** Size class — cosmetic, but it gates a handful of quests and habitats. */
enum class SizeClass(val displayKey: String) {
    TINY("size_tiny"),
    SMALL("size_small"),
    MEDIUM("size_medium"),
    LARGE("size_large"),
    HUGE("size_huge"),
    COLOSSAL("size_colossal"),
}

/** How fast a species climbs the level curve. */
enum class GrowthRate(val displayKey: String, val factor: Double) {
    SWIFT("growth_swift", 0.80),
    STEADY("growth_steady", 1.00),
    SLOW("growth_slow", 1.25),
    GLACIAL("growth_glacial", 1.55),
}

/**
 * The nine worlds. Regions own maps, music, weather profiles and encounter
 * tables; a species declares its native world for the bestiary.
 */
enum class World(val displayKey: String, val order: Int) {
    MIDGARD("world_midgard", 0),
    ASGARD("world_asgard", 1),
    VANAHEIM("world_vanaheim", 2),
    ALFHEIM("world_alfheim", 3),
    JOTUNHEIM("world_jotunheim", 4),
    MUSPELHEIM("world_muspelheim", 5),
    NIFLHEIM("world_niflheim", 6),
    HELHEIM("world_helheim", 7),
    SVARTALFHEIM("world_svartalfheim", 8),
    ;

    companion object {
        fun fromKeyOrNull(raw: String): World? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}

/**
 * A passive ability. Abilities hook into the battle engine through
 * [AbilityTrigger]; the engine dispatches on [effectId], and every id is
 * implemented in `AbilityEffects` — there is no generic fallback, so an unknown
 * id fails loudly during content validation.
 */
data class Ability(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val trigger: AbilityTrigger,
    val effectId: String,
    val magnitude: Double = 1.0,
)

/** Points in the turn pipeline at which passive abilities may fire. */
enum class AbilityTrigger {
    ON_BATTLE_START,
    ON_TURN_START,
    ON_BEFORE_ATTACK,
    ON_DEALING_DAMAGE,
    ON_TAKING_DAMAGE,
    ON_AFTER_DAMAGE,
    ON_STATUS_APPLIED,
    ON_TURN_END,
    ON_FAINT,
    ON_WEATHER_CHANGE,
    PASSIVE_STAT,
}

/**
 * One node of a species' talent tree. Talent points are earned every second
 * level and are refundable at a Seidr Loom (see `RespecTalentsUseCase`).
 */
data class TalentNode(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val tier: Int,
    val cost: Int,
    val requires: List<String> = emptyList(),
    val statBonus: StatBlock = StatBlock.ZERO,
    val grantsAbilityId: String? = null,
    val grantsMoveId: String? = null,
)
