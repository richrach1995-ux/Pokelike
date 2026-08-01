package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.monster.GenderRatio
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.MoveSlot
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.model.monster.Temperament
import com.runeveil.saga.domain.util.Rng
import kotlin.math.roundToInt

/**
 * The Brutstätte (breeding roost) rule set.
 *
 * Two compatible monsters left at a roost produce an egg whose contents are
 * determined here. The system is deliberately deep — it is the endgame's main
 * progression loop — but every rule is explicit and testable:
 *
 *  * **Gene inheritance** — 3 of 7 genes are copied from the parents (5 with a
 *    Gene Sigil bound), the rest rolled fresh.
 *  * **Species** — the offspring is the base form of the mother's family, or of
 *    the father's when the mother is a Seidr Vessel.
 *  * **Move inheritance** — egg moves of the child's species that either parent
 *    knows are passed on.
 *  * **Talent inheritance** — a talent both parents share is pre-unlocked.
 *  * **Mutation** — a small chance to shift one gene to its maximum or to
 *    change the hatchling's element (a Hybrid).
 *  * **Shiny odds** — improve when the parents come from different worlds
 *    (the "Fremdblut" bonus).
 */
object BreedingRules {

    /** Base odds of a shiny hatchling: 1 in 2048. */
    const val BASE_SHINY_DENOMINATOR = 2048

    /** Odds when the parents originate from different worlds: 1 in 512. */
    const val FOREIGN_BLOOD_SHINY_DENOMINATOR = 512

    /** Chance that a gene mutates to its maximum value. */
    const val GENE_MUTATION_CHANCE = 0.06

    /** Chance that the hatchling becomes a Hybrid (takes the father's
     *  secondary element). */
    const val HYBRID_CHANCE = 0.045

    /** Number of genes inherited from the parents by default. */
    const val INHERITED_GENES = 3

    /** Inherited genes when a Gene Sigil is held by either parent. */
    const val INHERITED_GENES_WITH_SIGIL = 5

    /** Item id that raises the number of inherited genes. */
    const val GENE_SIGIL_ITEM_ID = "sigil_of_lineage"

    /** Item id that guarantees the mother's temperament is passed on. */
    const val TEMPERAMENT_CHARM_ITEM_ID = "charm_of_temper"

    /** Egg group that makes a monster compatible with everything. */
    const val UNIVERSAL_EGG_GROUP = "seidr_vessel"

    /** Reason a pair cannot breed, or [Compatibility.Compatible]. */
    sealed interface Compatibility {
        data object Compatible : Compatibility
        data object SameGender : Compatibility
        data object NoSharedEggGroup : Compatibility
        data object Genderless : Compatibility
        data object LegendaryLineage : Compatibility
        data object IsEgg : Compatibility
    }

    /** Checks whether [a] and [b] may produce an egg. */
    fun compatibility(a: MonsterInstance, b: MonsterInstance): Compatibility {
        if (a.isEgg || b.isEgg) return Compatibility.IsEgg
        if (a.species.rarity.isStoryBound || b.species.rarity.isStoryBound) {
            return Compatibility.LegendaryLineage
        }
        val universalA = UNIVERSAL_EGG_GROUP in a.species.eggGroups
        val universalB = UNIVERSAL_EGG_GROUP in b.species.eggGroups
        if (universalA || universalB) {
            // A Seidr Vessel breeds with anything that has any egg group.
            if (a.species.eggGroups.isEmpty() || b.species.eggGroups.isEmpty()) {
                return Compatibility.NoSharedEggGroup
            }
            return Compatibility.Compatible
        }
        if (a.gender == Gender.UNKNOWN || b.gender == Gender.UNKNOWN) return Compatibility.Genderless
        if (a.gender == b.gender) return Compatibility.SameGender
        val shared = a.species.eggGroups.intersect(b.species.eggGroups.toSet())
        if (shared.isEmpty()) return Compatibility.NoSharedEggGroup
        return Compatibility.Compatible
    }

    /** Everything the roost needs to create an egg. */
    data class EggBlueprint(
        val speciesId: String,
        val genes: StatBlock,
        val temperament: Temperament,
        val gender: Gender,
        val isShiny: Boolean,
        val inheritedMoveIds: List<String>,
        val inheritedTalentIds: Set<String>,
        val isHybrid: Boolean,
        val hatchSteps: Int,
        val mutatedStats: Set<Stat>,
    )

    /**
     * Produces the blueprint of an egg from [mother] and [father].
     *
     * @param speciesResolver maps a species id to its species; used to walk
     *   back to the base form of the family.
     * @param familyBaseResolver returns the base-form species id of a family.
     */
    fun breed(
        mother: MonsterInstance,
        father: MonsterInstance,
        speciesResolver: (String) -> MonsterSpecies?,
        familyBaseResolver: (familyId: String) -> String?,
        rng: Rng,
    ): EggBlueprint? {
        if (compatibility(mother, father) != Compatibility.Compatible) return null

        // --- species --------------------------------------------------------
        val motherIsVessel = UNIVERSAL_EGG_GROUP in mother.species.eggGroups
        val lineageParent = if (motherIsVessel) father else mother
        val speciesId = familyBaseResolver(lineageParent.species.familyId)
            ?: lineageParent.species.id
        val childSpecies = speciesResolver(speciesId) ?: return null

        // --- genes ----------------------------------------------------------
        val hasSigil = mother.heldItemId == GENE_SIGIL_ITEM_ID || father.heldItemId == GENE_SIGIL_ITEM_ID
        val inheritCount = if (hasSigil) INHERITED_GENES_WITH_SIGIL else INHERITED_GENES
        val inheritedStats = rng.shuffled(Stat.entries.toList()).take(inheritCount).toSet()
        val mutated = mutableSetOf<Stat>()
        var genes = StatBlock.ZERO
        for (stat in Stat.entries) {
            val value = when {
                rng.chance(GENE_MUTATION_CHANCE) -> {
                    mutated += stat
                    StatBlock.MAX_GENE
                }
                stat in inheritedStats -> {
                    val fromMother = rng.chance(0.5)
                    if (fromMother) mother.genes[stat] else father.genes[stat]
                }
                else -> rng.nextInt(StatBlock.MAX_GENE + 1)
            }
            genes = genes.with(stat, value.coerceIn(0, StatBlock.MAX_GENE))
        }

        // --- temperament ----------------------------------------------------
        val temperament = when {
            mother.heldItemId == TEMPERAMENT_CHARM_ITEM_ID -> mother.temperament
            father.heldItemId == TEMPERAMENT_CHARM_ITEM_ID -> father.temperament
            rng.chance(0.5) -> mother.temperament
            else -> rng.pick(Temperament.entries.toList()) ?: Temperament.EVEN
        }

        // --- gender ---------------------------------------------------------
        val gender = rollGender(childSpecies.genderRatio, rng)

        // --- shiny ----------------------------------------------------------
        val foreignBlood = mother.species.nativeWorld != father.species.nativeWorld
        val denominator = if (foreignBlood) FOREIGN_BLOOD_SHINY_DENOMINATOR else BASE_SHINY_DENOMINATOR
        val isShiny = rng.nextInt(denominator) == 0

        // --- moves ----------------------------------------------------------
        val parentMoves = (mother.moves + father.moves).map { it.moveId }.toSet()
        val inheritedMoves = buildList {
            addAll(childSpecies.eggMoveIds.filter { it in parentMoves })
            // Level-1 moves are always known.
            addAll(childSpecies.learnset.filter { it.level <= 1 }.map { it.moveId })
        }.distinct().take(MonsterSpecies.MAX_KNOWN_MOVES)

        // --- talents --------------------------------------------------------
        val inheritedTalents = mother.unlockedTalentIds.intersect(father.unlockedTalentIds)
            .take(2).toSet()

        // --- hybrid ---------------------------------------------------------
        val isHybrid = rng.chance(HYBRID_CHANCE) &&
            father.species.primaryElement != childSpecies.primaryElement

        val hatchSteps = childSpecies.eggCycles * EGG_STEPS_PER_CYCLE

        return EggBlueprint(
            speciesId = childSpecies.id,
            genes = genes,
            temperament = temperament,
            gender = gender,
            isShiny = isShiny,
            inheritedMoveIds = inheritedMoves,
            inheritedTalentIds = inheritedTalents,
            isHybrid = isHybrid,
            hatchSteps = hatchSteps,
            mutatedStats = mutated,
        )
    }

    /** Rolls a gender from a [GenderRatio]. */
    fun rollGender(ratio: GenderRatio, rng: Rng): Gender = when {
        ratio.isGenderless -> Gender.UNKNOWN
        rng.nextDouble() < ratio.maleChance -> Gender.MALE
        else -> Gender.FEMALE
    }

    /**
     * How many steps are shaved off the hatch counter per walked step.
     * A Flame Cradle in the party doubles the rate.
     */
    fun hatchStepsPerMove(hasFlameCradle: Boolean): Int = if (hasFlameCradle) 2 else 1

    /**
     * Turns a blueprint into a real, level-1 egg instance.
     *
     * @param uid caller-supplied unique id (the repository owns id generation).
     */
    fun materialise(
        blueprint: EggBlueprint,
        species: MonsterSpecies,
        uid: String,
        moveResolver: (String) -> Int?,
        originalTrainerName: String,
        nowEpochMs: Long,
    ): MonsterInstance {
        val moves = blueprint.inheritedMoveIds.mapNotNull { moveId ->
            moveResolver(moveId)?.let { pp -> MoveSlot(moveId = moveId, currentPp = pp, maxPp = pp) }
        }
        val instance = MonsterInstance(
            uid = uid,
            species = species,
            level = 1,
            experience = 0L,
            genes = blueprint.genes,
            temperament = blueprint.temperament,
            gender = blueprint.gender,
            isShiny = blueprint.isShiny,
            abilityId = species.abilityIds.firstOrNull(),
            moves = moves,
            currentHp = 1,
            friendship = 120,
            unlockedTalentIds = blueprint.inheritedTalentIds,
            originalTrainerName = originalTrainerName,
            caughtAtEpochMs = nowEpochMs,
            eggHatchStepsRemaining = blueprint.hatchSteps,
            isEgg = true,
        )
        return instance.copy(currentHp = instance.maxHp)
    }

    /** Estimated egg quality in 0…1, shown as stars in the roost UI. */
    fun eggQuality(blueprint: EggBlueprint): Double {
        val geneScore = blueprint.genes.total.toDouble() / (StatBlock.MAX_GENE * Stat.entries.size)
        val bonus = (if (blueprint.isShiny) 0.25 else 0.0) +
            (if (blueprint.isHybrid) 0.10 else 0.0) +
            blueprint.mutatedStats.size * 0.05
        return (geneScore + bonus).coerceIn(0.0, 1.0)
    }

    /** Number of walked steps represented by one egg cycle. */
    const val EGG_STEPS_PER_CYCLE = 256

    /** Cost in gold to leave a pair at the roost, scaled by their levels. */
    fun roostFee(a: MonsterInstance, b: MonsterInstance): Int =
        (200 + (a.level + b.level) * 12 *
            (a.species.rarity.ordinal + b.species.rarity.ordinal + 2) / 2.0).roundToInt()
}
