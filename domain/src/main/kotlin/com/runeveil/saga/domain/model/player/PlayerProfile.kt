package com.runeveil.saga.domain.model.player

import com.runeveil.saga.domain.model.item.EquipmentSlot
import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.monster.StatBlock

/**
 * The player character.
 *
 * The player has their own level, gear and titles: gear changes encounter
 * rates, capture odds and the affinity bonus of a matching element, so
 * dressing for a region is a real decision.
 */
data class PlayerProfile(
    val name: String,
    val gender: Gender,
    val appearance: Appearance,
    val level: Int = 1,
    val experience: Long = 0L,
    val gold: Int = 500,
    val equipment: Map<EquipmentSlot, String> = emptyMap(),
    val activeTitleId: String? = null,
    val unlockedTitleIds: Set<String> = emptySet(),
    val moralScore: Int = 0,
    val playtimeSeconds: Long = 0L,
    val stepsWalked: Long = 0L,
    val currentRegionId: String = "midgard_heartlands",
    val currentLocationId: String = "loc_wanderers_rest",
    val respawnLocationId: String = "loc_wanderers_rest",
    val statistics: PlayerStatistics = PlayerStatistics(),
) {
    /** Player level raises the party's out-of-battle bonuses, not battle stats. */
    val runeCapacity: Int get() = 1 + level / 15

    companion object {
        const val MAX_LEVEL = 60
        const val MAX_PARTY_SIZE = 6
    }
}

/**
 * Character customisation. Every field is an asset key resolved by the
 * character renderer; adding options is a content-only change.
 */
data class Appearance(
    val bodyKey: String = "body_01",
    val hairKey: String = "hair_01",
    val hairColorHex: Long = 0xFF3A2A1E,
    val skinToneKey: String = "skin_02",
    val eyeKey: String = "eyes_01",
    val eyeColorHex: Long = 0xFF4A6B8A,
    val outfitKey: String = "outfit_wanderer",
    val markingKey: String? = null,
    val accessoryKey: String? = null,
)

/** Aggregated lifetime statistics; feeds achievements and the save screen. */
data class PlayerStatistics(
    val battlesWon: Int = 0,
    val battlesLost: Int = 0,
    val monstersCaught: Int = 0,
    val monstersSeen: Int = 0,
    val shiniesFound: Int = 0,
    val eggsHatched: Int = 0,
    val evolutionsPerformed: Int = 0,
    val itemsCrafted: Int = 0,
    val questsCompleted: Int = 0,
    val bossesDefeated: Int = 0,
    val criticalHitsLanded: Int = 0,
    val stepsInEachWorld: Map<String, Long> = emptyMap(),
    val deepestEndlessFloor: Int = 0,
    val tournamentsWon: Int = 0,
    val newGamePlusCount: Int = 0,
)

/**
 * An earnable title shown next to the player's name. Titles can carry small
 * mechanical bonuses, which is what makes them worth chasing.
 */
data class Title(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val unlockConditionKey: String,
    val captureBonusPercent: Int = 0,
    val goldBonusPercent: Int = 0,
    val experienceBonusPercent: Int = 0,
    val statBonus: StatBlock = StatBlock.ZERO,
    val rarityTier: Int = 1,
)

/**
 * An achievement. [progressTarget] > 1 turns it into a counter achievement
 * with a progress bar.
 */
data class Achievement(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val iconKey: String,
    val category: AchievementCategory,
    val progressTarget: Int = 1,
    val hidden: Boolean = false,
    val rewardTitleId: String? = null,
    val rewardItemId: String? = null,
    val points: Int = 10,
)

enum class AchievementCategory(val displayKey: String) {
    STORY("achv_cat_story"),
    COLLECTION("achv_cat_collection"),
    BATTLE("achv_cat_battle"),
    BREEDING("achv_cat_breeding"),
    EXPLORATION("achv_cat_exploration"),
    CRAFTING("achv_cat_crafting"),
    MASTERY("achv_cat_mastery"),
}

/** Live progress of one achievement. */
data class AchievementProgress(
    val achievementId: String,
    val progress: Int = 0,
    val unlockedAtEpochMs: Long? = null,
) {
    val isUnlocked: Boolean get() = unlockedAtEpochMs != null
}
