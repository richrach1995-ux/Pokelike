package com.runeveil.saga.domain.repository

import com.runeveil.saga.domain.model.item.InventoryStack
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.item.Recipe
import com.runeveil.saga.domain.model.monster.Ability
import com.runeveil.saga.domain.model.battle.Move
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.monster.TalentNode
import com.runeveil.saga.domain.model.npc.Faction
import com.runeveil.saga.domain.model.npc.Npc
import com.runeveil.saga.domain.model.npc.TrainerTeam
import com.runeveil.saga.domain.model.player.Achievement
import com.runeveil.saga.domain.model.player.AchievementProgress
import com.runeveil.saga.domain.model.player.PlayerProfile
import com.runeveil.saga.domain.model.player.Title
import com.runeveil.saga.domain.model.save.GameSave
import com.runeveil.saga.domain.model.save.SaveSlotSummary
import com.runeveil.saga.domain.model.story.Cutscene
import com.runeveil.saga.domain.model.story.DialogueTree
import com.runeveil.saga.domain.model.story.Ending
import com.runeveil.saga.domain.model.story.Quest
import com.runeveil.saga.domain.model.story.QuestProgress
import com.runeveil.saga.domain.model.story.StoryChapter
import com.runeveil.saga.domain.model.world.EncounterTable
import com.runeveil.saga.domain.model.world.Region
import com.runeveil.saga.domain.model.world.RegionWeatherState
import com.runeveil.saga.domain.model.world.Shop
import com.runeveil.saga.domain.model.world.TreasureChest
import kotlinx.coroutines.flow.Flow

/**
 * Static, read-only game content shipped with the app.
 *
 * Implementations load the JSON assets once, validate them and keep them in
 * memory (the whole content set is a few megabytes and is needed constantly).
 */
interface ContentRepository {
    suspend fun ensureLoaded()

    fun species(id: String): MonsterSpecies?
    fun allSpecies(): List<MonsterSpecies>
    fun speciesByFamily(familyId: String): List<MonsterSpecies>
    fun baseFormOf(familyId: String): MonsterSpecies?

    fun move(id: String): Move?
    fun allMoves(): List<Move>

    fun ability(id: String): Ability?
    fun talentTree(id: String): List<TalentNode>

    fun item(id: String): Item?
    fun allItems(): List<Item>

    fun recipe(id: String): Recipe?
    fun allRecipes(): List<Recipe>

    fun region(id: String): Region?
    fun allRegions(): List<Region>
    fun locationOf(locationId: String): com.runeveil.saga.domain.model.world.Location?
    fun encounterTable(id: String): EncounterTable?
    fun shop(id: String): Shop?
    fun chest(id: String): TreasureChest?

    fun npc(id: String): Npc?
    fun npcsAt(locationId: String): List<Npc>
    fun trainerTeam(id: String): TrainerTeam?
    fun faction(id: String): Faction?
    fun allFactions(): List<Faction>

    fun quest(id: String): Quest?
    fun allQuests(): List<Quest>
    fun dialogueTree(id: String): DialogueTree?
    fun chapter(number: Int): StoryChapter?
    fun allChapters(): List<StoryChapter>
    fun cutscene(id: String): Cutscene?
    fun allEndings(): List<Ending>

    fun title(id: String): Title?
    fun allTitles(): List<Title>
    fun allAchievements(): List<Achievement>
}

/** Owned monsters: party, storage vault and eggs. */
interface MonsterRepository {
    fun observeParty(): Flow<List<MonsterInstance>>
    fun observeVault(): Flow<List<MonsterInstance>>
    suspend fun party(): List<MonsterInstance>
    suspend fun vault(): List<MonsterInstance>
    suspend fun byUid(uid: String): MonsterInstance?
    suspend fun insert(monster: MonsterInstance, toParty: Boolean): Boolean
    suspend fun update(monster: MonsterInstance)
    suspend fun updateAll(monsters: List<MonsterInstance>)
    suspend fun release(uid: String)
    suspend fun moveToParty(uid: String): Boolean
    suspend fun moveToVault(uid: String): Boolean
    suspend fun reorderParty(uids: List<String>)
    suspend fun nextUid(): String
    suspend fun healParty()
}

/** The satchel. */
interface InventoryRepository {
    fun observeInventory(): Flow<List<InventoryStack>>
    suspend fun stacks(): List<InventoryStack>
    suspend fun countOf(itemId: String): Int
    suspend fun add(itemId: String, amount: Int)
    suspend fun remove(itemId: String, amount: Int): Boolean
    suspend fun clear()
}

/** Player profile, flags, gold and faction standing. */
interface PlayerRepository {
    fun observeProfile(): Flow<PlayerProfile>
    suspend fun profile(): PlayerProfile
    suspend fun update(profile: PlayerProfile)
    suspend fun addGold(amount: Int)
    suspend fun spendGold(amount: Int): Boolean

    fun observeStoryFlags(): Flow<Set<String>>
    suspend fun storyFlags(): Set<String>
    suspend fun grantFlag(flag: String)
    suspend fun clearFlag(flag: String)

    fun observeReputation(): Flow<Map<String, Int>>
    suspend fun changeReputation(factionId: String, delta: Int)

    suspend fun currentChapter(): Int
    suspend fun setChapter(chapter: Int)
}

/** Quest log. */
interface QuestRepository {
    fun observeAll(): Flow<List<QuestProgress>>
    suspend fun progressFor(questId: String): QuestProgress?
    suspend fun upsert(progress: QuestProgress)
    suspend fun activeQuests(): List<QuestProgress>
    suspend fun resetDailies(nowEpochMs: Long)
}

/** Bestiarium: what has been seen and caught. */
interface BestiaryRepository {
    fun observeEntries(): Flow<List<BestiaryEntry>>
    suspend fun entry(speciesId: String): BestiaryEntry?
    suspend fun markSeen(speciesId: String, locationId: String?)
    suspend fun markCaught(speciesId: String, shiny: Boolean, locationId: String?)
    suspend fun caughtCount(): Int
    suspend fun seenCount(): Int
}

/** One row of the bestiary. */
data class BestiaryEntry(
    val speciesId: String,
    val seen: Boolean,
    val caught: Boolean,
    val shinyCaught: Boolean,
    val caughtCount: Int,
    val firstSeenLocationId: String?,
    val firstCaughtEpochMs: Long?,
)

/** World state that changes as the player plays. */
interface WorldStateRepository {
    fun observeWeather(): Flow<Map<String, RegionWeatherState>>
    suspend fun weatherFor(regionId: String): RegionWeatherState
    suspend fun advanceWeather(nowEpochMs: Long)
    suspend fun setWeather(state: RegionWeatherState)

    fun observeInGameMinutes(): Flow<Long>
    suspend fun advanceTime(minutes: Long)

    suspend fun discoveredLocations(): Set<String>
    suspend fun discoverLocation(locationId: String)
    suspend fun unlockFastTravel(locationId: String)
    suspend fun fastTravelLocations(): Set<String>
    suspend fun openedChests(): Set<String>
    suspend fun markChestOpened(chestId: String)
    suspend fun defeatedTrainers(): Set<String>
    suspend fun markTrainerDefeated(trainerId: String)
}

/** Save slots. */
interface SaveRepository {
    fun observeSlots(): Flow<List<SaveSlotSummary>>
    suspend fun slots(): List<SaveSlotSummary>
    suspend fun save(slotIndex: Int, isAutosave: Boolean): Result<Unit>
    suspend fun load(slotIndex: Int): Result<GameSave>
    suspend fun delete(slotIndex: Int)
    suspend fun createNewGame(
        playerName: String,
        gender: com.runeveil.saga.domain.model.monster.Gender,
        appearance: com.runeveil.saga.domain.model.player.Appearance,
        starterSpeciesId: String,
    ): Result<Unit>
    suspend fun startNewGamePlus(): Result<Unit>
    suspend fun activeSlot(): Int
    suspend fun setActiveSlot(slotIndex: Int)
}

/** Achievements. */
interface AchievementRepository {
    fun observeProgress(): Flow<List<AchievementProgress>>
    suspend fun increment(achievementId: String, amount: Int = 1): Boolean
    suspend fun unlock(achievementId: String): Boolean
    suspend fun progressFor(achievementId: String): AchievementProgress?
}

/** User-facing settings, persisted with DataStore. */
interface SettingsRepository {
    fun observeSettings(): Flow<GameSettings>
    suspend fun settings(): GameSettings
    suspend fun update(transform: (GameSettings) -> GameSettings)
}

/**
 * Persisted player preferences.
 *
 * @property battleAnimationSpeed 0.5 (slow) … 2.0 (fast); 0.0 disables
 *   animations entirely for accessibility.
 */
data class GameSettings(
    val musicVolume: Float = 0.7f,
    val soundVolume: Float = 0.85f,
    val voiceVolume: Float = 0.9f,
    val battleAnimationSpeed: Float = 1.0f,
    val textSpeed: Float = 1.0f,
    val autosaveEnabled: Boolean = true,
    val autosaveIntervalMinutes: Int = 5,
    val hapticsEnabled: Boolean = true,
    val showDamageNumbers: Boolean = true,
    val showTypeHints: Boolean = true,
    val confirmBeforeRelease: Boolean = true,
    val language: String = "system",
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val screenShake: Boolean = true,
    val skipSeenCutscenes: Boolean = false,
    val difficulty: Difficulty = Difficulty.NORMAL,
)

/**
 * Difficulty scales enemy levels and item availability. It never changes the
 * rules themselves, so a save can switch difficulty at any time.
 */
enum class Difficulty(
    val displayKey: String,
    val enemyLevelDelta: Int,
    val enemyStatMultiplier: Double,
    val experienceMultiplier: Double,
    val captureMultiplier: Double,
) {
    STORY("difficulty_story", -3, 0.85, 1.30, 1.30),
    NORMAL("difficulty_normal", 0, 1.00, 1.00, 1.00),
    VETERAN("difficulty_veteran", 2, 1.10, 0.90, 0.90),
    RAGNAROK("difficulty_ragnarok", 5, 1.25, 0.80, 0.80),
}
