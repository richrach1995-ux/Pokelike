package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.MoveSlot
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.StatBlock
import com.runeveil.saga.domain.model.monster.TalentNode

/**
 * Everything that happens to a monster *between* battles: gaining experience,
 * levelling, learning moves, spending talent points and binding runes.
 */
object ProgressionRules {

    /** The outcome of adding experience to one monster. */
    data class LevelUpResult(
        val monster: MonsterInstance,
        val levelsGained: Int,
        val statGains: Map<Stat, Int>,
        val learnableMoveIds: List<String>,
        val talentPointsGained: Int,
    )

    /**
     * Adds [amount] experience, applying every level-up that follows.
     *
     * HP is increased by exactly the amount the max-HP grew, so a monster is
     * never healed by levelling but also never left proportionally weaker.
     */
    fun grantExperience(monster: MonsterInstance, amount: Long): LevelUpResult {
        if (monster.level >= MonsterSpecies.MAX_LEVEL || monster.isEgg) {
            return LevelUpResult(monster, 0, emptyMap(), emptyList(), 0)
        }
        val statsBefore = monster.stats
        val newExperience = monster.experience + amount.coerceAtLeast(0)
        val newLevel = ExperienceCurve.levelFor(monster.species.growthRate, newExperience)
            .coerceAtMost(MonsterSpecies.MAX_LEVEL)
        val cappedExperience = if (newLevel >= MonsterSpecies.MAX_LEVEL) {
            ExperienceCurve.totalAt(monster.species.growthRate, MonsterSpecies.MAX_LEVEL)
        } else {
            newExperience
        }

        val levelled = monster.copy(experience = cappedExperience, level = newLevel)
        val levelsGained = newLevel - monster.level
        if (levelsGained <= 0) {
            return LevelUpResult(levelled, 0, emptyMap(), emptyList(), 0)
        }

        val statsAfter = levelled.stats
        val gains = Stat.entries.associateWith { statsAfter[it] - statsBefore[it] }
            .filterValues { it != 0 }
        val healed = levelled.copy(
            currentHp = (monster.currentHp + (statsAfter.hp - statsBefore.hp)).coerceIn(
                if (monster.isFainted) 0 else 1,
                statsAfter.hp,
            ),
        )
        val learnable = ((monster.level + 1)..newLevel)
            .flatMap { monster.species.movesLearnedAt(it) }
            .distinct()
            .filterNot { healed.knowsMove(it) }

        val talentPoints = (newLevel / 2) - (monster.level / 2)
        return LevelUpResult(healed, levelsGained, gains, learnable, talentPoints)
    }

    /**
     * Teaches [moveId], replacing the move in [replaceSlotIndex] when all four
     * slots are taken. Returns null when the move is already known.
     */
    fun learnMove(
        monster: MonsterInstance,
        moveId: String,
        maxPp: Int,
        replaceSlotIndex: Int? = null,
    ): MonsterInstance? {
        if (monster.knowsMove(moveId)) return null
        val slot = MoveSlot(moveId = moveId, currentPp = maxPp, maxPp = maxPp)
        return when {
            monster.moves.size < MonsterSpecies.MAX_KNOWN_MOVES -> monster.copy(moves = monster.moves + slot)
            replaceSlotIndex != null && replaceSlotIndex in monster.moves.indices -> {
                val updated = monster.moves.toMutableList()
                updated[replaceSlotIndex] = slot
                monster.copy(moves = updated)
            }
            else -> null
        }
    }

    /** Applies the training values earned from defeating [defeated]. */
    fun applyBattleTraining(monster: MonsterInstance, defeated: MonsterSpecies, defeatedLevel: Int): MonsterInstance {
        val yield = StatCalculator.trainingYield(defeated.baseStats, defeatedLevel)
        return monster.copy(training = StatCalculator.applyTraining(monster.training, yield))
    }

    /**
     * Friendship changes. Winning battles, levelling and healing raise it;
     * fainting and being released lower it.
     */
    fun friendshipDelta(event: FriendshipEvent, currentFriendship: Int): Int {
        // Gains shrink as friendship approaches the cap — the last points are
        // the hardest, which makes friendship evolutions feel earned.
        val tier = when {
            currentFriendship < 100 -> 1.0
            currentFriendship < 200 -> 0.6
            else -> 0.35
        }
        val base = when (event) {
            FriendshipEvent.WON_BATTLE -> 2
            FriendshipEvent.LEVEL_UP -> 3
            FriendshipEvent.HEALED -> 1
            FriendshipEvent.WALKED_1000_STEPS -> 2
            FriendshipEvent.USED_VITAMIN -> 4
            FriendshipEvent.EVOLVED -> 6
            FriendshipEvent.FAINTED -> -5
            FriendshipEvent.STORED_IN_VAULT -> -2
            FriendshipEvent.BITTER_MEDICINE -> -4
        }
        return if (base > 0) (base * tier).toInt().coerceAtLeast(1) else base
    }

    enum class FriendshipEvent {
        WON_BATTLE, LEVEL_UP, HEALED, WALKED_1000_STEPS, USED_VITAMIN,
        EVOLVED, FAINTED, STORED_IN_VAULT, BITTER_MEDICINE,
    }

    /**
     * Unlocks a talent node.
     *
     * @return the updated monster, or null when the node is unaffordable, its
     *   prerequisites are unmet or it is already unlocked.
     */
    fun unlockTalent(
        monster: MonsterInstance,
        node: TalentNode,
        tree: List<TalentNode>,
    ): MonsterInstance? {
        if (node.id in monster.unlockedTalentIds) return null
        if (monster.availableTalentPoints < node.cost) return null
        if (!node.requires.all { it in monster.unlockedTalentIds }) return null
        // Tier gating: a tier-N node needs at least N−1 unlocked nodes below it.
        val unlockedBelow = tree.count { it.id in monster.unlockedTalentIds && it.tier < node.tier }
        if (node.tier > 1 && unlockedBelow < node.tier - 1) return null

        return monster.copy(
            unlockedTalentIds = monster.unlockedTalentIds + node.id,
            spentTalentPoints = monster.spentTalentPoints + node.cost,
            talentStatBonus = monster.talentStatBonus + node.statBonus,
            abilityId = node.grantsAbilityId ?: monster.abilityId,
        )
    }

    /** Refunds every talent point (Seidr Loom service). */
    fun respecTalents(monster: MonsterInstance): MonsterInstance = monster.copy(
        unlockedTalentIds = emptySet(),
        spentTalentPoints = 0,
        talentStatBonus = StatBlock.ZERO,
    )

    /** Binds a rune to a monster; capacity comes from the player's level. */
    fun bindRune(monster: MonsterInstance, runeItemId: String, capacity: Int): MonsterInstance? {
        if (runeItemId in monster.boundRuneIds) return null
        if (monster.boundRuneIds.size >= capacity) return null
        return monster.copy(boundRuneIds = monster.boundRuneIds + runeItemId)
    }

    fun unbindRune(monster: MonsterInstance, runeItemId: String): MonsterInstance =
        monster.copy(boundRuneIds = monster.boundRuneIds - runeItemId)

    /**
     * Player-level experience. The player levels far more slowly than monsters
     * and mostly through quests, which keeps gear progression story-paced.
     */
    fun playerExperienceForNextLevel(level: Int): Long =
        (140L * level * level + 600L * level)

    fun playerLevelFor(experience: Long): Int {
        var level = 1
        var required = playerExperienceForNextLevel(1)
        var remaining = experience
        while (remaining >= required && level < com.runeveil.saga.domain.model.player.PlayerProfile.MAX_LEVEL) {
            remaining -= required
            level++
            required = playerExperienceForNextLevel(level)
        }
        return level
    }
}
