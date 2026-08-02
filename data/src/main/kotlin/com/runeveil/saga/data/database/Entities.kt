package com.runeveil.saga.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room schema of Runeveil.
 *
 * Everything is keyed by `saveSlot`, which is what makes multiple save games
 * and the autosave slot work without duplicating the database: a "save" is a
 * partition of these tables, not a separate file.
 *
 * The seven stat values are stored as an [EmbeddedStats] block rather than JSON
 * so they can be queried and sorted (the vault sorts by stat).
 */

@Entity(
    tableName = "monsters",
    indices = [
        Index(value = ["saveSlot"]),
        Index(value = ["saveSlot", "inParty"]),
        Index(value = ["speciesId"]),
    ],
)
data class MonsterEntity(
    @PrimaryKey val uid: String,
    val saveSlot: Int,
    val speciesId: String,
    val nickname: String? = null,
    val level: Int,
    val experience: Long,
    @Embedded(prefix = "gene_") val genes: EmbeddedStats,
    @Embedded(prefix = "train_") val training: EmbeddedStats,
    @Embedded(prefix = "talentbonus_") val talentBonus: EmbeddedStats,
    val temperament: String,
    val gender: String,
    val isShiny: Boolean,
    val abilityId: String? = null,
    /** JSON array of `{moveId, currentPp, maxPp, ppUps}`. */
    val movesJson: String,
    val currentHp: Int,
    val status: String? = null,
    val statusTurns: Int = 0,
    val friendship: Int,
    /** Comma-separated talent node ids. */
    val unlockedTalents: String = "",
    val spentTalentPoints: Int = 0,
    val heldItemId: String? = null,
    /** Comma-separated rune item ids. */
    val boundRunes: String = "",
    val originLocationId: String? = null,
    val originalTrainerName: String? = null,
    val caughtAtEpochMs: Long = 0L,
    val caughtWithOrbId: String? = null,
    val eggHatchStepsRemaining: Int = 0,
    val isEgg: Boolean = false,
    val inParty: Boolean = false,
    val partyOrder: Int = 0,
    /** Set while the monster is left at the breeding roost. */
    val atRoost: Boolean = false,
)

/** Seven stat values, embedded with a column prefix. */
data class EmbeddedStats(
    val hp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val magic: Int = 0,
    val resistance: Int = 0,
    val speed: Int = 0,
    val luck: Int = 0,
)

@Entity(tableName = "inventory", primaryKeys = ["saveSlot", "itemId"])
data class InventoryEntity(
    val saveSlot: Int,
    val itemId: String,
    val quantity: Int,
)

@Entity(tableName = "quest_progress", primaryKeys = ["saveSlot", "questId"])
data class QuestProgressEntity(
    val saveSlot: Int,
    val questId: String,
    val state: String,
    /** JSON object of `objectiveId -> count`. */
    val objectiveCountsJson: String = "{}",
    val startedAtEpochMs: Long = 0L,
    val completedAtEpochMs: Long? = null,
    val lastResetEpochMs: Long = 0L,
)

@Entity(tableName = "bestiary", primaryKeys = ["saveSlot", "speciesId"])
data class BestiaryEntity(
    val saveSlot: Int,
    val speciesId: String,
    val seen: Boolean = false,
    val caught: Boolean = false,
    val shinyCaught: Boolean = false,
    val caughtCount: Int = 0,
    val firstSeenLocationId: String? = null,
    val firstCaughtEpochMs: Long? = null,
)

@Entity(tableName = "achievements", primaryKeys = ["saveSlot", "achievementId"])
data class AchievementEntity(
    val saveSlot: Int,
    val achievementId: String,
    val progress: Int = 0,
    val unlockedAtEpochMs: Long? = null,
)

@Entity(tableName = "region_weather", primaryKeys = ["saveSlot", "regionId"])
data class RegionWeatherEntity(
    val saveSlot: Int,
    val regionId: String,
    val weather: String,
    val startedAtEpochMs: Long,
    val durationMs: Long,
)

/**
 * The singleton row per save slot.
 *
 * Sets (flags, discovered locations …) are stored as JSON arrays: they are
 * always read and written as a whole, never queried by element, so a join
 * table would only add cost.
 */
@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val saveSlot: Int,
    val schemaVersion: Int,
    val playerName: String,
    val gender: String,
    /** JSON object with the appearance keys. */
    val appearanceJson: String,
    val playerLevel: Int,
    val playerExperience: Long,
    val gold: Int,
    /** JSON object `slot -> itemId`. */
    val equipmentJson: String = "{}",
    val activeTitleId: String? = null,
    val unlockedTitlesJson: String = "[]",
    val moralScore: Int = 0,
    val playtimeSeconds: Long = 0L,
    val stepsWalked: Long = 0L,
    val currentRegionId: String,
    val currentLocationId: String,
    val respawnLocationId: String,
    /** JSON object with the [com.runeveil.saga.domain.model.player.PlayerStatistics]. */
    val statisticsJson: String = "{}",
    val chapter: Int = 0,
    val storyFlagsJson: String = "[]",
    val discoveredLocationsJson: String = "[]",
    val fastTravelLocationsJson: String = "[]",
    val defeatedTrainersJson: String = "[]",
    val openedChestsJson: String = "[]",
    val unlockedRecipesJson: String = "[]",
    val factionReputationJson: String = "{}",
    val inGameMinutes: Long = 8 * 60,
    @ColumnInfo(defaultValue = "0") val endlessBestFloor: Int = 0,
    val activeEndingId: String? = null,
    val newGamePlusCount: Int = 0,
    val savedAtEpochMs: Long,
    val isAutosave: Boolean,
)
