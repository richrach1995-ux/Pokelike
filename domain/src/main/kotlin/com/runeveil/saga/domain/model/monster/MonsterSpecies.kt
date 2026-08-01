package com.runeveil.saga.domain.model.monster

/**
 * Immutable, content-defined description of a *kind* of monster.
 *
 * Species are shipped as JSON in `assets/content/monsters.json`, validated at
 * build time by `tools/contentgen/validate_content.py` and at runtime by
 * `ContentValidator` before they ever reach the database.
 *
 * @property id stable slug, e.g. `emberling`. Never reused, never renamed.
 * @property dexNumber position in the Bestiarium (1-based, unique).
 * @property familyId groups an evolution line so the bestiary can show it.
 * @property stage 1-based position within the family (1 = base form).
 * @property baseStats the species' innate power budget.
 * @property catchRate 1 (nearly impossible) … 255 (trivial).
 * @property baseExperience experience yield factor when defeated.
 * @property eggGroups shared groups make two species breedable together.
 * @property eggCycles steps (×256) until an egg hatches.
 * @property learnset moves learned by levelling.
 * @property tutorMoves moves teachable at a Rune Tutor.
 * @property eggMoves moves only obtainable through breeding.
 * @property spriteKey asset key; the renderer resolves `<spriteKey>.webp` and
 *   `<spriteKey>_shiny.webp` plus the animation atlas `<spriteKey>_atlas.json`.
 */
data class MonsterSpecies(
    val id: String,
    val dexNumber: Int,
    val nameKey: String,
    val descriptionKey: String,
    val loreKey: String,
    val primaryElement: Element,
    val secondaryElement: Element? = null,
    val familyId: String,
    val stage: Int,
    val baseStats: StatBlock,
    val rarity: Rarity,
    val growthRate: GrowthRate,
    val catchRate: Int,
    val baseExperience: Int,
    val sizeClass: SizeClass,
    val heightCm: Int,
    val weightHg: Int,
    val nativeWorld: World,
    val habitats: List<String> = emptyList(),
    val genderRatio: GenderRatio = GenderRatio.BALANCED,
    val eggGroups: List<String> = emptyList(),
    val eggCycles: Int = 20,
    val abilityIds: List<String> = emptyList(),
    val hiddenAbilityId: String? = null,
    val talentTreeId: String? = null,
    val learnset: List<LearnsetEntry> = emptyList(),
    val tutorMoveIds: List<String> = emptyList(),
    val eggMoveIds: List<String> = emptyList(),
    val evolutions: List<EvolutionPath> = emptyList(),
    val spriteKey: String,
    val cryKey: String,
    val idleAnimation: AnimationSpec = AnimationSpec.DEFAULT_IDLE,
    val attackAnimation: AnimationSpec = AnimationSpec.DEFAULT_ATTACK,
    val shinyPaletteKey: String? = null,
    val bestiaryFlavourKeys: List<String> = emptyList(),
) {
    val elements: List<Element>
        get() = listOfNotNull(primaryElement, secondaryElement)

    val isDualType: Boolean get() = secondaryElement != null

    /** Species that cannot be caught with an ordinary rune orb. */
    val requiresRitualCapture: Boolean get() = rarity.isStoryBound

    /** Moves this species knows on being generated at [level]. */
    fun movesAtLevel(level: Int, maxMoves: Int = MAX_KNOWN_MOVES): List<String> =
        learnset.asSequence()
            .filter { it.level <= level }
            .sortedByDescending { it.level }
            .map { it.moveId }
            .distinct()
            .take(maxMoves)
            .toList()

    /** Moves that become available exactly at [level] (used for level-up prompts). */
    fun movesLearnedAt(level: Int): List<String> =
        learnset.filter { it.level == level }.map { it.moveId }

    companion object {
        const val MAX_KNOWN_MOVES = 4
        const val MAX_LEVEL = 100
    }
}

/** A single `level -> move` pair of a species' learnset. */
data class LearnsetEntry(val level: Int, val moveId: String)

/** Probability of rolling a male individual, expressed as content-friendly buckets. */
enum class GenderRatio(val maleChance: Double) {
    MALE_ONLY(1.0),
    MOSTLY_MALE(0.875),
    BALANCED(0.5),
    MOSTLY_FEMALE(0.125),
    FEMALE_ONLY(0.0),
    GENDERLESS(-1.0),
    ;

    val isGenderless: Boolean get() = this == GENDERLESS
}

/**
 * A path from one species to another.
 *
 * Every path carries **all** of its conditions; the evolution check is an AND
 * over the non-null fields. This keeps content declarative and the rule engine
 * (`EvolutionRules`) free of special cases.
 */
data class EvolutionPath(
    val targetSpeciesId: String,
    val trigger: EvolutionTrigger,
    val minLevel: Int? = null,
    val requiredItemId: String? = null,
    val consumesItem: Boolean = true,
    val minFriendship: Int? = null,
    val requiredTimeOfDay: DayPhase? = null,
    val requiredWorld: World? = null,
    val requiredLocationId: String? = null,
    val requiredStoryFlag: String? = null,
    val requiredRuneId: String? = null,
    val requiredGender: Gender? = null,
    val requiredHigherStat: Stat? = null,
    val requiredKnownMoveId: String? = null,
    val requiredWeather: String? = null,
    val descriptionKey: String,
)

/** What kind of interaction starts an evolution — drives the UI hint text. */
enum class EvolutionTrigger {
    LEVEL_UP,
    USE_ITEM,
    FRIENDSHIP,
    TRADE_RITE,
    STORY_EVENT,
    BREEDING,
    RUNE_BINDING,
    SACRED_SITE,
    TIME_OF_DAY,
}

/** Phase of the in-game day. */
enum class DayPhase(val displayKey: String, val startHour: Int, val endHour: Int) {
    DAWN("phase_dawn", 5, 8),
    DAY("phase_day", 8, 17),
    DUSK("phase_dusk", 17, 20),
    NIGHT("phase_night", 20, 5),
    ;

    /** True while [hour] (0…23) falls inside this phase. */
    fun contains(hour: Int): Boolean =
        if (startHour < endHour) hour in startHour until endHour
        else hour >= startHour || hour < endHour

    /** Coarse day/night split used by encounter tables and NPC dialogue. */
    val isNight: Boolean get() = this == NIGHT || this == DUSK

    companion object {
        fun forHour(hour: Int): DayPhase =
            entries.first { it.contains(((hour % 24) + 24) % 24) }
    }
}

/**
 * Declarative animation description. The Compose renderer turns this into a
 * frame-timed [androidx.compose.animation] sequence; keeping it in :domain lets
 * content define pacing without touching UI code.
 *
 * @property frameCount number of frames in the sprite atlas row.
 * @property frameDurationMs milliseconds per frame (16 ms ≙ one 60 fps frame).
 * @property loop whether the clip repeats.
 * @property easing named easing curve resolved by the UI layer.
 */
data class AnimationSpec(
    val frameCount: Int,
    val frameDurationMs: Int,
    val loop: Boolean,
    val easing: String = "linear",
    val offsetYDp: Int = 0,
) {
    val totalDurationMs: Int get() = frameCount * frameDurationMs

    companion object {
        val DEFAULT_IDLE = AnimationSpec(frameCount = 8, frameDurationMs = 96, loop = true, easing = "sine")
        val DEFAULT_ATTACK = AnimationSpec(frameCount = 6, frameDurationMs = 64, loop = false, easing = "accelerate")
    }
}
