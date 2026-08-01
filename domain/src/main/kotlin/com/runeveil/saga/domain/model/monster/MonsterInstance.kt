package com.runeveil.saga.domain.model.monster

import com.runeveil.saga.domain.model.battle.StatusCondition
import com.runeveil.saga.domain.rules.ExperienceCurve
import com.runeveil.saga.domain.rules.StatCalculator

/**
 * A concrete, owned monster — the thing that lives in the player's party, in
 * the Rune Vault, or in an enemy team.
 *
 * Everything that distinguishes one Emberling from another lives here: genes,
 * training, temperament, friendship, learned moves, shiny flag and battle
 * state. The [species] is looked up from the content repository by id; this
 * class carries a resolved reference so battle code never needs a repository.
 *
 * The class is immutable — every mutation returns a copy. That makes the
 * battle engine trivially replayable and the save format deterministic.
 *
 * @property uid globally unique instance id (UUID string), stable across saves.
 * @property genes inherited potential, 0…31 per stat.
 * @property training earned values, ≤255 per stat and ≤510 in total.
 * @property friendship 0…255; raised by battling, healing and walking together.
 * @property currentHp never exceeds [maxHp]; 0 means fainted.
 */
data class MonsterInstance(
    val uid: String,
    val species: MonsterSpecies,
    val nickname: String? = null,
    val level: Int = 5,
    val experience: Long = 0L,
    val genes: StatBlock = StatBlock.ZERO,
    val training: StatBlock = StatBlock.ZERO,
    val temperament: Temperament = Temperament.EVEN,
    val gender: Gender = Gender.UNKNOWN,
    val isShiny: Boolean = false,
    val abilityId: String? = null,
    val moves: List<MoveSlot> = emptyList(),
    val currentHp: Int = 1,
    val status: StatusCondition? = null,
    val statusTurns: Int = 0,
    val friendship: Int = 70,
    val unlockedTalentIds: Set<String> = emptySet(),
    val spentTalentPoints: Int = 0,
    val heldItemId: String? = null,
    val boundRuneIds: List<String> = emptyList(),
    val originLocationId: String? = null,
    val originalTrainerName: String? = null,
    val caughtAtEpochMs: Long = 0L,
    val caughtWithOrbId: String? = null,
    val eggHatchStepsRemaining: Int = 0,
    val isEgg: Boolean = false,
    val talentStatBonus: StatBlock = StatBlock.ZERO,
) {
    /** Display name: nickname if the player set one, otherwise the species name key. */
    val displayNameKey: String get() = nickname ?: species.nameKey

    val primaryElement: Element get() = species.primaryElement
    val secondaryElement: Element? get() = species.secondaryElement

    /** Fully resolved battle statistics at the current level. */
    val stats: StatBlock by lazy(LazyThreadSafetyMode.NONE) {
        StatCalculator.resolve(
            base = species.baseStats,
            genes = genes,
            training = training,
            level = level,
            temperament = temperament,
            talentBonus = talentStatBonus,
        )
    }

    val maxHp: Int get() = stats.hp

    val isFainted: Boolean get() = currentHp <= 0

    val hpFraction: Float get() = if (maxHp <= 0) 0f else currentHp.toFloat() / maxHp

    /** Talent points earned so far (one per two levels) minus the spent ones. */
    val availableTalentPoints: Int
        get() = (level / 2) - spentTalentPoints

    /** Experience still required to reach the next level; 0 at level 100. */
    val experienceToNextLevel: Long
        get() = ExperienceCurve.remainingToNextLevel(species.growthRate, level, experience)

    /** Progress towards the next level in 0f…1f, used by the XP bar. */
    val levelProgress: Float
        get() = ExperienceCurve.progressWithinLevel(species.growthRate, level, experience)

    fun withDamage(amount: Int): MonsterInstance =
        copy(currentHp = (currentHp - amount).coerceAtLeast(0))

    fun withHeal(amount: Int): MonsterInstance =
        copy(currentHp = (currentHp + amount).coerceIn(0, maxHp))

    fun fullyRestored(): MonsterInstance =
        copy(
            currentHp = maxHp,
            status = null,
            statusTurns = 0,
            moves = moves.map { it.copy(currentPp = it.maxPp) },
        )

    fun withStatus(condition: StatusCondition?, turns: Int = 0): MonsterInstance =
        copy(status = condition, statusTurns = turns)

    fun withFriendship(delta: Int): MonsterInstance =
        copy(friendship = (friendship + delta).coerceIn(0, MAX_FRIENDSHIP))

    /** Spends one PP of the move in [slotIndex]; no-op when the slot is empty. */
    fun withPpSpent(slotIndex: Int, amount: Int = 1): MonsterInstance {
        if (slotIndex !in moves.indices) return this
        val updated = moves.toMutableList()
        val slot = updated[slotIndex]
        updated[slotIndex] = slot.copy(currentPp = (slot.currentPp - amount).coerceAtLeast(0))
        return copy(moves = updated)
    }

    fun knowsMove(moveId: String): Boolean = moves.any { it.moveId == moveId }

    /** True when at least one move still has PP left. */
    val hasUsableMove: Boolean get() = moves.any { it.currentPp > 0 }

    companion object {
        const val MAX_FRIENDSHIP = 255
        const val HIGH_FRIENDSHIP = 220
    }
}

/**
 * One of the four move slots of a monster.
 *
 * @property ppUps 0…3 PP boosters applied at a Rune Tutor; each adds 20 % of
 *   the move's printed PP.
 */
data class MoveSlot(
    val moveId: String,
    val currentPp: Int,
    val maxPp: Int,
    val ppUps: Int = 0,
) {
    val isDepleted: Boolean get() = currentPp <= 0
    val ppFraction: Float get() = if (maxPp <= 0) 0f else currentPp.toFloat() / maxPp
}
