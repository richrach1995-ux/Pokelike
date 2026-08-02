package com.runeveil.saga.data.repository

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.runeveil.saga.data.di.IoDispatcher
import com.runeveil.saga.data.database.AchievementDao
import com.runeveil.saga.data.database.AppearanceJson
import com.runeveil.saga.data.database.BestiaryDao
import com.runeveil.saga.data.database.GameStateDao
import com.runeveil.saga.data.database.GameStateEntity
import com.runeveil.saga.data.database.InventoryDao
import com.runeveil.saga.data.database.MonsterDao
import com.runeveil.saga.data.database.QuestDao
import com.runeveil.saga.data.database.StatisticsJson
import com.runeveil.saga.data.database.WorldStateDao
import com.runeveil.saga.data.database.enumValueOrDefault
import com.runeveil.saga.data.database.toDomain
import com.runeveil.saga.data.database.toEntity
import com.runeveil.saga.data.database.toJsonModel
import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.monster.World
import com.runeveil.saga.domain.model.player.Appearance
import com.runeveil.saga.domain.model.player.PlayerProfile
import com.runeveil.saga.domain.model.save.GameSave
import com.runeveil.saga.domain.model.save.SaveSlotSummary
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.SaveRepository
import com.runeveil.saga.domain.rules.EncounterRules
import com.runeveil.saga.domain.util.Rng
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Save slots.
 *
 * A "save" is a partition of the Room tables identified by `saveSlot`, so
 * saving is cheap: the singleton [GameStateEntity] row is rewritten and the
 * timestamp updated — the monsters, inventory and quest rows were already
 * persisted the moment they changed. That is what makes autosave feel free.
 *
 * Slot 0 is the autosave; slots 1…3 are manual.
 */
@Singleton
class SaveRepositoryImpl @Inject constructor(
    private val gameStateDao: GameStateDao,
    private val monsterDao: MonsterDao,
    private val inventoryDao: InventoryDao,
    private val questDao: QuestDao,
    private val bestiaryDao: BestiaryDao,
    private val achievementDao: AchievementDao,
    private val worldStateDao: WorldStateDao,
    private val content: ContentRepository,
    private val activeSlot: ActiveSlot,
    private val json: Json,
    private val rng: Rng,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SaveRepository {

    private val listSerializer = ListSerializer(String.serializer())
    private val intMapSerializer = MapSerializer(String.serializer(), Int.serializer())
    private val stringMapSerializer = MapSerializer(String.serializer(), String.serializer())

    override fun observeSlots(): Flow<List<SaveSlotSummary>> =
        gameStateDao.observeAll().map { rows -> buildSummaries(rows) }

    override suspend fun slots(): List<SaveSlotSummary> = withContext(ioDispatcher) {
        buildSummaries(gameStateDao.all())
    }

    private suspend fun buildSummaries(rows: List<GameStateEntity>): List<SaveSlotSummary> {
        val total = content.allSpecies().size
        val bySlot = rows.associateBy { it.saveSlot }
        return (0..ActiveSlot.MAX_SLOT).map { index ->
            val row = bySlot[index] ?: return@map SaveSlotSummary.empty(index)
            val caught = bestiaryDao.caughtCount(index)
            val party = monsterDao.party(index)
            SaveSlotSummary(
                slotIndex = index,
                exists = true,
                playerName = row.playerName,
                chapter = row.chapter,
                chapterTitleKey = content.chapter(row.chapter)?.titleKey.orEmpty(),
                level = row.playerLevel,
                playtimeSeconds = row.playtimeSeconds,
                bestiaryCaught = caught,
                bestiaryTotal = total,
                world = content.region(row.currentRegionId)?.world ?: World.MIDGARD,
                locationNameKey = content.locationOf(row.currentLocationId)?.nameKey.orEmpty(),
                savedAtEpochMs = row.savedAtEpochMs,
                isAutosave = row.isAutosave,
                newGamePlusCount = row.newGamePlusCount,
                partyPreviewSpriteKeys = party.mapNotNull { content.species(it.speciesId)?.spriteKey },
            )
        }
    }

    /**
     * Copies the live state into [slotIndex].
     *
     * Saving into a *different* slot duplicates the rows of the active slot,
     * which is what "Save as" must do.
     */
    override suspend fun save(slotIndex: Int, isAutosave: Boolean): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val source = activeSlot.current
                val state = gameStateDao.bySlot(source)
                    ?: error("Kein aktiver Spielstand zum Speichern.")
                val now = System.currentTimeMillis()

                if (slotIndex == source) {
                    gameStateDao.upsert(state.copy(savedAtEpochMs = now, isAutosave = isAutosave))
                    return@runCatching
                }

                // Copy every partitioned table into the target slot.
                clearSlotTables(slotIndex)
                gameStateDao.upsert(
                    state.copy(saveSlot = slotIndex, savedAtEpochMs = now, isAutosave = isAutosave),
                )
                monsterDao.upsertAll(
                    (monsterDao.party(source) + monsterDao.vault(source))
                        .map { it.copy(saveSlot = slotIndex) },
                )
                inventoryDao.upsertAll(inventoryDao.all(source).map { it.copy(saveSlot = slotIndex) })
                worldStateDao.upsertAllWeather(
                    worldStateDao.allWeather(source).map { it.copy(saveSlot = slotIndex) },
                )
                // Quest, bestiary and achievement rows are copied through their
                // DAOs one by one; the row counts here are small (<300).
                content.allQuests().forEach { quest ->
                    questDao.byId(source, quest.id)?.let {
                        questDao.upsert(it.copy(saveSlot = slotIndex))
                    }
                }
                content.allSpecies().forEach { species ->
                    bestiaryDao.byId(source, species.id)?.let {
                        bestiaryDao.upsert(it.copy(saveSlot = slotIndex))
                    }
                }
                content.allAchievements().forEach { achievement ->
                    achievementDao.byId(source, achievement.id)?.let {
                        achievementDao.upsert(it.copy(saveSlot = slotIndex))
                    }
                }
            }
        }

    override suspend fun load(slotIndex: Int): Result<GameSave> = withContext(ioDispatcher) {
        runCatching {
            val state = gameStateDao.bySlot(slotIndex)
                ?: error("Slot $slotIndex ist leer.")
            activeSlot.set(slotIndex)
            state.toGameSave()
        }
    }

    override suspend fun delete(slotIndex: Int) = withContext(ioDispatcher) {
        clearSlotTables(slotIndex)
        gameStateDao.deleteSlot(slotIndex)
    }

    /**
     * Starts a fresh game in the autosave slot: creates the player, hands out
     * the starter monster, the first orbs and the bestiary key item.
     */
    override suspend fun createNewGame(
        playerName: String,
        gender: Gender,
        appearance: Appearance,
        starterSpeciesId: String,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val slot = GameSave.AUTOSAVE_SLOT
            activeSlot.set(slot)
            clearSlotTables(slot)

            val startRegion = content.allRegions().first()
            val startLocation = startRegion.locations.first { it.type.name == "TOWN" }
            val now = System.currentTimeMillis()

            gameStateDao.upsert(
                GameStateEntity(
                    saveSlot = slot,
                    schemaVersion = GameSave.CURRENT_SCHEMA_VERSION,
                    playerName = playerName,
                    gender = gender.name,
                    appearanceJson = json.encodeToString(appearance.toJsonModel()),
                    playerLevel = 1,
                    playerExperience = 0L,
                    gold = STARTING_GOLD,
                    equipmentJson = "{}",
                    activeTitleId = "titel_wanderer",
                    unlockedTitlesJson = json.encodeToString(listSerializer, listOf("titel_wanderer")),
                    currentRegionId = startRegion.id,
                    currentLocationId = startLocation.id,
                    respawnLocationId = startLocation.id,
                    statisticsJson = json.encodeToString(StatisticsJson()),
                    chapter = 0,
                    storyFlagsJson = json.encodeToString(listSerializer, emptyList()),
                    discoveredLocationsJson = json.encodeToString(
                        listSerializer, listOf(startLocation.id),
                    ),
                    fastTravelLocationsJson = json.encodeToString(
                        listSerializer, listOf(startLocation.id),
                    ),
                    defeatedTrainersJson = json.encodeToString(listSerializer, emptyList()),
                    openedChestsJson = json.encodeToString(listSerializer, emptyList()),
                    unlockedRecipesJson = json.encodeToString(
                        listSerializer,
                        content.allRecipes().filter { it.tier <= 1 }.map { it.id },
                    ),
                    factionReputationJson = json.encodeToString(
                        intMapSerializer,
                        content.allFactions().associate { it.id to 0 },
                    ),
                    inGameMinutes = START_HOUR * 60L,
                    savedAtEpochMs = now,
                    isAutosave = true,
                ),
            )

            val species = content.species(starterSpeciesId)
                ?: error("Unbekannte Startart '$starterSpeciesId'.")
            val starter = EncounterRules.generateTrained(
                species = species,
                uid = UUID.randomUUID().toString(),
                level = STARTER_LEVEL,
                moveIds = species.movesAtLevel(STARTER_LEVEL),
                abilityId = species.abilityIds.firstOrNull(),
                heldItemId = null,
                temperament = com.runeveil.saga.domain.model.monster.Temperament.EVEN,
                geneQuality = 20,
                isShiny = false,
                nickname = null,
                movePpResolver = { content.move(it)?.maxPp },
            ).copy(originalTrainerName = playerName, caughtAtEpochMs = now)

            monsterDao.upsert(starter.toEntity(json, slot, inParty = true, partyOrder = 0))

            STARTING_ITEMS.forEach { (itemId, amount) ->
                val limit = content.item(itemId)?.stackLimit ?: 99
                inventoryDao.add(slot, itemId, amount, limit)
            }
        }
    }

    /**
     * New Game Plus: keeps the bestiary, titles and achievements, resets the
     * story, party and inventory, and raises the difficulty implicitly through
     * the `newGamePlusCount` used by the encounter scaler.
     */
    override suspend fun startNewGamePlus(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val slot = activeSlot.current
            val state = gameStateDao.bySlot(slot) ?: error("Kein Spielstand vorhanden.")
            val startRegion = content.allRegions().first()
            val startLocation = startRegion.locations.first { it.type.name == "TOWN" }

            monsterDao.deleteSlot(slot)
            inventoryDao.deleteSlot(slot)
            questDao.deleteSlot(slot)
            worldStateDao.deleteSlot(slot)

            gameStateDao.upsert(
                state.copy(
                    chapter = 0,
                    storyFlagsJson = json.encodeToString(listSerializer, emptyList()),
                    currentRegionId = startRegion.id,
                    currentLocationId = startLocation.id,
                    respawnLocationId = startLocation.id,
                    gold = STARTING_GOLD * 2,
                    newGamePlusCount = state.newGamePlusCount + 1,
                    activeEndingId = null,
                    savedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun activeSlot(): Int = activeSlot.current

    override suspend fun setActiveSlot(slotIndex: Int) {
        activeSlot.set(slotIndex)
    }

    private suspend fun clearSlotTables(slotIndex: Int) {
        monsterDao.deleteSlot(slotIndex)
        inventoryDao.deleteSlot(slotIndex)
        questDao.deleteSlot(slotIndex)
        bestiaryDao.deleteSlot(slotIndex)
        achievementDao.deleteSlot(slotIndex)
        worldStateDao.deleteSlot(slotIndex)
    }

    private fun GameStateEntity.toGameSave(): GameSave = GameSave(
        slotIndex = saveSlot,
        schemaVersion = schemaVersion,
        player = PlayerProfile(
            name = playerName,
            gender = enumValueOrDefault(gender, Gender.UNKNOWN),
            appearance = json.decodeFromString<AppearanceJson>(appearanceJson).toDomain(),
            level = playerLevel,
            experience = playerExperience,
            gold = gold,
            equipment = json.decodeFromString(stringMapSerializer, equipmentJson)
                .mapNotNull { (key, value) ->
                    com.runeveil.saga.domain.model.item.EquipmentSlot.entries
                        .firstOrNull { it.name == key }?.let { it to value }
                }.toMap(),
            activeTitleId = activeTitleId,
            unlockedTitleIds = decode(unlockedTitlesJson),
            moralScore = moralScore,
            playtimeSeconds = playtimeSeconds,
            stepsWalked = stepsWalked,
            currentRegionId = currentRegionId,
            currentLocationId = currentLocationId,
            respawnLocationId = respawnLocationId,
            statistics = json.decodeFromString<StatisticsJson>(statisticsJson).toDomain(),
        ),
        chapter = chapter,
        storyFlags = decode(storyFlagsJson),
        discoveredLocationIds = decode(discoveredLocationsJson),
        fastTravelLocationIds = decode(fastTravelLocationsJson),
        defeatedTrainerIds = decode(defeatedTrainersJson),
        openedChestIds = decode(openedChestsJson),
        unlockedRecipeIds = decode(unlockedRecipesJson),
        factionReputation = json.decodeFromString(intMapSerializer, factionReputationJson),
        partyMonsterUids = emptyList(),
        inGameMinutes = inGameMinutes,
        savedAtEpochMs = savedAtEpochMs,
        isAutosave = isAutosave,
        endlessBestFloor = endlessBestFloor,
        activeEndingId = activeEndingId,
    )

    private fun decode(raw: String): Set<String> =
        runCatching { json.decodeFromString(listSerializer, raw).toSet() }.getOrDefault(emptySet())

    private companion object {
        const val STARTING_GOLD = 800
        const val STARTER_LEVEL = 5
        const val START_HOUR = 8

        val STARTING_ITEMS = listOf(
            "orb_wood" to 10,
            "elixier_klein" to 5,
            "werkzeug_bestiarium" to 1,
        )
    }
}
