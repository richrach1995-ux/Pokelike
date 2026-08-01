package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.EvolutionPath
import com.runeveil.saga.domain.model.monster.EvolutionTrigger
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.World

/**
 * Decides whether — and into what — a monster evolves.
 *
 * A [EvolutionPath] is a conjunction of all of its non-null conditions, which
 * keeps content declarative. This object is the only place that knows how to
 * evaluate them.
 */
object EvolutionRules {

    /** Everything about the world that an evolution check may depend on. */
    data class Context(
        val dayPhase: DayPhase,
        val world: World?,
        val locationId: String?,
        val weatherId: String?,
        val storyFlags: Set<String>,
        val usedItemId: String? = null,
        val triggeredBy: EvolutionTrigger = EvolutionTrigger.LEVEL_UP,
        val heldRuneIds: Set<String> = emptySet(),
    )

    /**
     * Returns the first path of [monster] whose conditions are all satisfied,
     * or null when it does not evolve right now.
     *
     * Paths are evaluated in content order, so the most specific path of a
     * species must be declared first — `ContentValidator` enforces that
     * unconditional paths come last.
     */
    fun evaluate(monster: MonsterInstance, context: Context): EvolutionPath? {
        if (monster.isEgg) return null
        return monster.species.evolutions.firstOrNull { path -> matches(monster, path, context) }
    }

    /** All paths of the species with a human-readable "why not yet" reason. */
    fun explain(monster: MonsterInstance, context: Context): List<Pair<EvolutionPath, Boolean>> =
        monster.species.evolutions.map { it to matches(monster, it, context) }

    internal fun matches(monster: MonsterInstance, path: EvolutionPath, context: Context): Boolean {
        if (path.trigger != context.triggeredBy) return false
        path.minLevel?.let { if (monster.level < it) return false }
        path.requiredItemId?.let { if (context.usedItemId != it) return false }
        path.minFriendship?.let { if (monster.friendship < it) return false }
        path.requiredTimeOfDay?.let { if (context.dayPhase != it) return false }
        path.requiredWorld?.let { if (context.world != it) return false }
        path.requiredLocationId?.let { if (context.locationId != it) return false }
        path.requiredStoryFlag?.let { if (it !in context.storyFlags) return false }
        path.requiredRuneId?.let {
            if (it !in context.heldRuneIds && it !in monster.boundRuneIds) return false
        }
        path.requiredGender?.let { if (monster.gender != it) return false }
        path.requiredKnownMoveId?.let { if (!monster.knowsMove(it)) return false }
        path.requiredWeather?.let { if (context.weatherId != it) return false }
        path.requiredHigherStat?.let { stat ->
            val statValue = monster.stats[stat]
            val highest = monster.stats.asMap()
                .filterKeys { !it.isVital }
                .maxByOrNull { it.value }
                ?.value ?: statValue
            if (statValue < highest) return false
        }
        return true
    }

    /**
     * Evolution keeps the monster's identity: nickname, genes, training,
     * friendship, moves and talents all carry over. Only the species — and
     * therefore the derived stats — change. Current HP is scaled so an
     * evolution never heals or hurts proportionally.
     */
    fun applyEvolution(
        monster: MonsterInstance,
        newSpecies: com.runeveil.saga.domain.model.monster.MonsterSpecies,
    ): MonsterInstance {
        val hpFraction = monster.hpFraction
        val evolved = monster.copy(
            species = newSpecies,
            abilityId = monster.abilityId?.takeIf { it in newSpecies.abilityIds }
                ?: newSpecies.abilityIds.firstOrNull(),
        )
        val newHp = (evolved.maxHp * hpFraction).toInt().coerceIn(1, evolved.maxHp)
        return evolved.copy(currentHp = if (monster.isFainted) 0 else newHp)
    }
}
