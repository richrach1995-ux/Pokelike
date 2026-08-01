package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.MoveSlot
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.model.monster.Temperament
import com.runeveil.saga.domain.model.world.EncounterEntry
import com.runeveil.saga.domain.model.world.EncounterTable
import com.runeveil.saga.domain.model.world.Location
import com.runeveil.saga.domain.util.Rng

/**
 * Wild-encounter generation: whether a step triggers a fight, which species
 * appears, and how that individual is rolled.
 */
object EncounterRules {

    /** Base odds of a shiny wild monster: 1 in 4096. */
    const val SHINY_DENOMINATOR = 4096

    /** Improved odds while the Glücksrune charm is held. */
    const val SHINY_DENOMINATOR_WITH_CHARM = 1365

    /** Chance that a rolled encounter is a "rare spawn" variant. */
    const val RARE_SPAWN_CHANCE = 0.04

    /** Level bonus applied to rare spawns. */
    const val RARE_SPAWN_LEVEL_BONUS = 3

    /**
     * Whether the current step triggers an encounter.
     *
     * @param stepsSinceLastEncounter grants a small pity bonus so long walks
     *   through tall grass do not feel dead.
     * @param repelMultiplier from items/equipment; 0.0 blocks encounters.
     */
    fun rollEncounter(
        location: Location,
        stepsSinceLastEncounter: Int,
        repelMultiplier: Double,
        rng: Rng,
    ): Boolean {
        if (!location.hasWildEncounters) return false
        if (repelMultiplier <= 0.0) return false
        if (stepsSinceLastEncounter < MIN_STEPS_BETWEEN_ENCOUNTERS) return false
        val pity = (stepsSinceLastEncounter - MIN_STEPS_BETWEEN_ENCOUNTERS) * 0.004
        return rng.chance((location.encounterRate + pity) * repelMultiplier)
    }

    /** Picks one entry from [table] honouring day phase, weather and floor. */
    fun pickEntry(
        table: EncounterTable,
        phase: DayPhase,
        weather: BattleWeather,
        floor: Int,
        storyFlags: Set<String>,
        rng: Rng,
    ): EncounterEntry? {
        val candidates = table.candidatesFor(phase, weather, floor)
            .filter { it.requiresStoryFlag == null || it.requiresStoryFlag in storyFlags }
        return rng.pickWeighted(candidates) { it.weight }
    }

    /**
     * Materialises a wild monster from an encounter entry.
     *
     * Genes are rolled with a mild positive bias for rare spawns, which is what
     * makes hunting them worthwhile.
     */
    fun generateWild(
        entry: EncounterEntry,
        species: MonsterSpecies,
        uid: String,
        rng: Rng,
        movePpResolver: (String) -> Int?,
        hasShinyCharm: Boolean = false,
        levelBoost: Int = 0,
    ): MonsterInstance {
        val isRare = entry.isRareSpawn || rng.chance(RARE_SPAWN_CHANCE)
        val level = (rng.nextInt(entry.minLevel, entry.maxLevel + 1) +
            levelBoost + if (isRare) RARE_SPAWN_LEVEL_BONUS else 0)
            .coerceIn(1, MonsterSpecies.MAX_LEVEL)

        val geneFloor = if (isRare) 12 else 0
        var genes = StatBlock.ZERO
        for (stat in Stat.entries) {
            genes = genes.with(stat, rng.nextInt(geneFloor, StatBlock.MAX_GENE + 1))
        }

        val denominator = if (hasShinyCharm) SHINY_DENOMINATOR_WITH_CHARM else SHINY_DENOMINATOR
        val isShiny = rng.nextInt(denominator) == 0

        val moves = species.movesAtLevel(level).mapNotNull { moveId ->
            movePpResolver(moveId)?.let { pp -> MoveSlot(moveId, pp, pp) }
        }

        val abilityPool = species.abilityIds
        val ability = when {
            species.hiddenAbilityId != null && rng.chance(0.05) -> species.hiddenAbilityId
            abilityPool.isNotEmpty() -> rng.pick(abilityPool)
            else -> null
        }

        val instance = MonsterInstance(
            uid = uid,
            species = species,
            level = level,
            experience = ExperienceCurve.totalAt(species.growthRate, level),
            genes = genes,
            temperament = rng.pick(Temperament.entries.toList()) ?: Temperament.EVEN,
            gender = BreedingRules.rollGender(species.genderRatio, rng),
            isShiny = isShiny,
            abilityId = ability,
            moves = moves,
            friendship = 70,
            originLocationId = entry.speciesId,
        )
        return instance.copy(currentHp = instance.maxHp)
    }

    /**
     * Builds a trainer-owned monster: fixed level, chosen moves, tuned genes.
     */
    fun generateTrained(
        species: MonsterSpecies,
        uid: String,
        level: Int,
        moveIds: List<String>,
        abilityId: String?,
        heldItemId: String?,
        temperament: Temperament,
        geneQuality: Int,
        isShiny: Boolean,
        nickname: String?,
        movePpResolver: (String) -> Int?,
    ): MonsterInstance {
        val genes = StatBlock.uniform(geneQuality.coerceIn(0, StatBlock.MAX_GENE))
        val resolvedMoves = moveIds.ifEmpty { species.movesAtLevel(level) }
            .mapNotNull { moveId -> movePpResolver(moveId)?.let { MoveSlot(moveId, it, it) } }
        val instance = MonsterInstance(
            uid = uid,
            species = species,
            nickname = nickname,
            level = level.coerceIn(1, MonsterSpecies.MAX_LEVEL),
            experience = ExperienceCurve.totalAt(species.growthRate, level),
            genes = genes,
            temperament = temperament,
            gender = Gender.UNKNOWN,
            isShiny = isShiny,
            abilityId = abilityId ?: species.abilityIds.firstOrNull(),
            moves = resolvedMoves,
            heldItemId = heldItemId,
            friendship = 120,
        )
        return instance.copy(currentHp = instance.maxHp)
    }

    /** Steps that must pass before another encounter can trigger. */
    const val MIN_STEPS_BETWEEN_ENCOUNTERS = 6
}
