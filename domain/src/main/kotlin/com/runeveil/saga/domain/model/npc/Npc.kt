package com.runeveil.saga.domain.model.npc

import com.runeveil.saga.domain.battle.AiProfile
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.world.UnlockRequirement

/**
 * Any named character in the world: merchants, trainers, quest givers, lore
 * keepers, gods and antagonists.
 */
data class Npc(
    val id: String,
    val nameKey: String,
    val titleKey: String? = null,
    val descriptionKey: String,
    val roles: Set<NpcRole>,
    val locationId: String,
    val regionId: String,
    val portraitKey: String,
    val spriteKey: String,
    val factionId: String? = null,
    val dialogueTreeIds: List<String> = emptyList(),
    val shopId: String? = null,
    val trainerTeamId: String? = null,
    val questIds: List<String> = emptyList(),
    val appearsDuringPhases: Set<DayPhase> = emptySet(),
    val unlockRequirement: UnlockRequirement = UnlockRequirement.None,
    val loreKeys: List<String> = emptyList(),
    val isStoryCritical: Boolean = false,
    val rematchable: Boolean = false,
) {
    fun isPresentAt(phase: DayPhase): Boolean =
        appearsDuringPhases.isEmpty() || phase in appearsDuringPhases

    val isMerchant: Boolean get() = NpcRole.MERCHANT in roles && shopId != null
    val isTrainer: Boolean get() = NpcRole.TRAINER in roles && trainerTeamId != null
}

enum class NpcRole(val displayKey: String) {
    MERCHANT("npc_role_merchant"),
    TRAINER("npc_role_trainer"),
    QUEST_GIVER("npc_role_quest_giver"),
    LORE_KEEPER("npc_role_lore"),
    HEALER("npc_role_healer"),
    SMITH("npc_role_smith"),
    BREEDER("npc_role_breeder"),
    TUTOR("npc_role_tutor"),
    GUARD("npc_role_guard"),
    DEITY("npc_role_deity"),
    ANTAGONIST("npc_role_antagonist"),
    COMPANION("npc_role_companion"),
    WANDERER("npc_role_wanderer"),
}

/**
 * A trainer's team plus the AI settings and rewards used when fighting them.
 */
data class TrainerTeam(
    val id: String,
    val npcId: String,
    val aiProfile: AiProfile,
    val members: List<TrainerMonster>,
    val rewardGold: Int,
    val rewardItemIds: List<String> = emptyList(),
    val introDialogueNodeId: String? = null,
    val defeatDialogueNodeId: String? = null,
    val victoryDialogueNodeId: String? = null,
    val rematchLevelBonus: Int = 0,
    val battleMusicKey: String? = null,
)

/**
 * One monster on a trainer's roster. Everything except the species is optional
 * so ordinary trainers stay terse in content while bosses can be tuned exactly.
 */
data class TrainerMonster(
    val speciesId: String,
    val level: Int,
    val moveIds: List<String> = emptyList(),
    val abilityId: String? = null,
    val heldItemId: String? = null,
    val temperamentId: String? = null,
    val geneQuality: Int = 15,
    val isShiny: Boolean = false,
    val nickname: String? = null,
)

/**
 * The six great powers of the Nine. Reputation with each unlocks shops,
 * quests, recipes and — at the extremes — different endings.
 */
data class Faction(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val crestKey: String,
    val colorHex: Long,
    val opposingFactionIds: List<String> = emptyList(),
    val ranks: List<FactionRank>,
    val homeRegionId: String? = null,
) {
    /** The rank matching [reputation], or the lowest rank as a fallback. */
    fun rankFor(reputation: Int): FactionRank =
        ranks.lastOrNull { reputation >= it.minReputation } ?: ranks.first()
}

data class FactionRank(
    val id: String,
    val nameKey: String,
    val minReputation: Int,
    val unlocksShopId: String? = null,
    val unlocksQuestIds: List<String> = emptyList(),
    val discountPercent: Int = 0,
)

/** The player's live standing with one faction. */
data class FactionStanding(
    val factionId: String,
    val reputation: Int,
) {
    companion object {
        const val MIN_REPUTATION = -1000
        const val MAX_REPUTATION = 1000
    }
}
