package com.runeveil.saga.data.repository

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.runeveil.saga.data.di.IoDispatcher
import com.runeveil.saga.data.database.AchievementDao
import com.runeveil.saga.data.database.AchievementEntity
import com.runeveil.saga.data.database.AppearanceJson
import com.runeveil.saga.data.database.BestiaryDao
import com.runeveil.saga.data.database.BestiaryEntity
import com.runeveil.saga.data.database.GameStateDao
import com.runeveil.saga.data.database.GameStateEntity
import com.runeveil.saga.data.database.InventoryDao
import com.runeveil.saga.data.database.InventoryEntity
import com.runeveil.saga.data.database.QuestDao
import com.runeveil.saga.data.database.QuestProgressEntity
import com.runeveil.saga.data.database.RegionWeatherEntity
import com.runeveil.saga.data.database.StatisticsJson
import com.runeveil.saga.data.database.WorldStateDao
import com.runeveil.saga.data.database.enumValueOrDefault
import com.runeveil.saga.data.database.toDomain
import com.runeveil.saga.data.database.toJsonModel
import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.item.EquipmentSlot
import com.runeveil.saga.domain.model.item.InventoryStack
import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.player.AchievementProgress
import com.runeveil.saga.domain.model.player.PlayerProfile
import com.runeveil.saga.domain.model.story.QuestProgress
import com.runeveil.saga.domain.model.story.QuestState
import com.runeveil.saga.domain.model.world.RegionWeatherState
import com.runeveil.saga.domain.repository.AchievementRepository
import com.runeveil.saga.domain.repository.BestiaryEntry
import com.runeveil.saga.domain.repository.BestiaryRepository
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.InventoryRepository
import com.runeveil.saga.domain.repository.PlayerRepository
import com.runeveil.saga.domain.repository.QuestRepository
import com.runeveil.saga.domain.repository.WorldStateRepository
import com.runeveil.saga.domain.rules.QuestRules
import com.runeveil.saga.domain.util.Rng
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// ---------------------------------------------------------------------------
// Inventory
// ---------------------------------------------------------------------------

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val dao: InventoryDao,
    private val content: ContentRepository,
    private val activeSlot: ActiveSlot,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InventoryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeInventory(): Flow<List<InventoryStack>> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observeAll(slot).map { rows -> rows.map { InventoryStack(it.itemId, it.quantity) } }
        }

    override suspend fun stacks(): List<InventoryStack> = withContext(ioDispatcher) {
        dao.all(activeSlot.current).map { InventoryStack(it.itemId, it.quantity) }
    }

    override suspend fun countOf(itemId: String): Int = withContext(ioDispatcher) {
        dao.quantityOf(activeSlot.current, itemId) ?: 0
    }

    override suspend fun add(itemId: String, amount: Int) = withContext(ioDispatcher) {
        if (amount <= 0) return@withContext
        val limit = content.item(itemId)?.stackLimit ?: 99
        dao.add(activeSlot.current, itemId, amount, limit)
    }

    override suspend fun remove(itemId: String, amount: Int): Boolean = withContext(ioDispatcher) {
        if (amount <= 0) return@withContext true
        dao.remove(activeSlot.current, itemId, amount)
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        dao.deleteSlot(activeSlot.current)
    }
}

// ---------------------------------------------------------------------------
// Player
// ---------------------------------------------------------------------------

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val dao: GameStateDao,
    private val activeSlot: ActiveSlot,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PlayerRepository {

    private val stringListSerializer = kotlinx.serialization.builtins.ListSerializer(String.serializer())
    private val stringIntMapSerializer = MapSerializer(String.serializer(), Int.serializer())
    private val stringStringMapSerializer = MapSerializer(String.serializer(), String.serializer())

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeProfile(): Flow<PlayerProfile> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observe(slot).filterNotNull().map { it.toProfile() }
        }

    override suspend fun profile(): PlayerProfile = withContext(ioDispatcher) {
        requireState().toProfile()
    }

    override suspend fun update(profile: PlayerProfile) = withContext(ioDispatcher) {
        val state = requireState()
        dao.upsert(
            state.copy(
                playerName = profile.name,
                gender = profile.gender.name,
                appearanceJson = json.encodeToString(profile.appearance.toJsonModel()),
                playerLevel = profile.level,
                playerExperience = profile.experience,
                gold = profile.gold,
                equipmentJson = json.encodeToString(
                    stringStringMapSerializer,
                    profile.equipment.mapKeys { it.key.name },
                ),
                activeTitleId = profile.activeTitleId,
                unlockedTitlesJson = json.encodeToString(
                    stringListSerializer, profile.unlockedTitleIds.toList(),
                ),
                moralScore = profile.moralScore,
                playtimeSeconds = profile.playtimeSeconds,
                stepsWalked = profile.stepsWalked,
                currentRegionId = profile.currentRegionId,
                currentLocationId = profile.currentLocationId,
                respawnLocationId = profile.respawnLocationId,
                statisticsJson = json.encodeToString(profile.statistics.toJsonModel()),
            ),
        )
    }

    override suspend fun addGold(amount: Int) = withContext(ioDispatcher) {
        val state = requireState()
        dao.upsert(state.copy(gold = (state.gold + amount).coerceAtLeast(0)))
    }

    override suspend fun spendGold(amount: Int): Boolean = withContext(ioDispatcher) {
        val state = requireState()
        if (state.gold < amount) return@withContext false
        dao.upsert(state.copy(gold = state.gold - amount))
        true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeStoryFlags(): Flow<Set<String>> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observe(slot).filterNotNull().map { decodeSet(it.storyFlagsJson) }
        }

    override suspend fun storyFlags(): Set<String> = withContext(ioDispatcher) {
        decodeSet(requireState().storyFlagsJson)
    }

    override suspend fun grantFlag(flag: String) = withContext(ioDispatcher) {
        val state = requireState()
        val flags = decodeSet(state.storyFlagsJson) + flag
        dao.upsert(state.copy(storyFlagsJson = encodeSet(flags)))
    }

    override suspend fun clearFlag(flag: String) = withContext(ioDispatcher) {
        val state = requireState()
        val flags = decodeSet(state.storyFlagsJson) - flag
        dao.upsert(state.copy(storyFlagsJson = encodeSet(flags)))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeReputation(): Flow<Map<String, Int>> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observe(slot).filterNotNull().map {
                json.decodeFromString(stringIntMapSerializer, it.factionReputationJson)
            }
        }

    override suspend fun changeReputation(factionId: String, delta: Int) = withContext(ioDispatcher) {
        val state = requireState()
        val current = json.decodeFromString(stringIntMapSerializer, state.factionReputationJson)
        val updated = current + (factionId to ((current[factionId] ?: 0) + delta).coerceIn(-1000, 1000))
        dao.upsert(state.copy(factionReputationJson = json.encodeToString(stringIntMapSerializer, updated)))
    }

    override suspend fun currentChapter(): Int = withContext(ioDispatcher) {
        requireState().chapter
    }

    override suspend fun setChapter(chapter: Int) = withContext(ioDispatcher) {
        dao.upsert(requireState().copy(chapter = chapter))
    }

    private suspend fun requireState(): GameStateEntity =
        dao.bySlot(activeSlot.current)
            ?: error("Kein Spielstand in Slot ${activeSlot.current} — zuerst createNewGame() aufrufen.")

    private fun GameStateEntity.toProfile(): PlayerProfile = PlayerProfile(
        name = playerName,
        gender = enumValueOrDefault(gender, Gender.UNKNOWN),
        appearance = json.decodeFromString<AppearanceJson>(appearanceJson).toDomain(),
        level = playerLevel,
        experience = playerExperience,
        gold = gold,
        equipment = json.decodeFromString(stringStringMapSerializer, equipmentJson)
            .mapNotNull { (key, value) ->
                EquipmentSlot.entries.firstOrNull { it.name == key }?.let { it to value }
            }.toMap(),
        activeTitleId = activeTitleId,
        unlockedTitleIds = decodeSet(unlockedTitlesJson),
        moralScore = moralScore,
        playtimeSeconds = playtimeSeconds,
        stepsWalked = stepsWalked,
        currentRegionId = currentRegionId,
        currentLocationId = currentLocationId,
        respawnLocationId = respawnLocationId,
        statistics = json.decodeFromString<StatisticsJson>(statisticsJson).toDomain(),
    )

    private fun decodeSet(raw: String): Set<String> =
        runCatching { json.decodeFromString(stringListSerializer, raw).toSet() }.getOrDefault(emptySet())

    private fun encodeSet(values: Set<String>): String =
        json.encodeToString(stringListSerializer, values.toList())
}

// ---------------------------------------------------------------------------
// Quests
// ---------------------------------------------------------------------------

@Singleton
class QuestRepositoryImpl @Inject constructor(
    private val dao: QuestDao,
    private val content: ContentRepository,
    private val activeSlot: ActiveSlot,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : QuestRepository {

    private val countsSerializer = MapSerializer(String.serializer(), Int.serializer())

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<QuestProgress>> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observeAll(slot).map { rows -> rows.map { it.toDomain() } }
        }

    override suspend fun progressFor(questId: String): QuestProgress? = withContext(ioDispatcher) {
        dao.byId(activeSlot.current, questId)?.toDomain()
    }

    override suspend fun upsert(progress: QuestProgress) = withContext(ioDispatcher) {
        dao.upsert(progress.toEntity(activeSlot.current))
    }

    override suspend fun activeQuests(): List<QuestProgress> = withContext(ioDispatcher) {
        dao.byState(activeSlot.current, QuestState.ACTIVE.name).map { it.toDomain() }
    }

    /**
     * Resets daily quests once per in-game day boundary (04:00 local time),
     * as decided by [QuestRules.shouldResetDaily].
     */
    override suspend fun resetDailies(nowEpochMs: Long) = withContext(ioDispatcher) {
        val slot = activeSlot.current
        val dailyIds = content.allQuests()
            .filter { it.category == com.runeveil.saga.domain.model.story.QuestCategory.DAILY }
            .map { it.id }
            .toSet()
        if (dailyIds.isEmpty()) return@withContext
        val rows = dailyIds.mapNotNull { dao.byId(slot, it) }
        val toReset = rows.filter { QuestRules.shouldResetDaily(it.lastResetEpochMs, nowEpochMs) }
        if (toReset.isEmpty()) return@withContext
        dao.upsertAll(
            toReset.map {
                it.copy(
                    state = QuestState.AVAILABLE.name,
                    objectiveCountsJson = "{}",
                    completedAtEpochMs = null,
                    lastResetEpochMs = nowEpochMs,
                )
            },
        )
    }

    private fun QuestProgressEntity.toDomain(): QuestProgress = QuestProgress(
        questId = questId,
        state = enumValueOrDefault(state, QuestState.AVAILABLE),
        objectiveCounts = runCatching {
            json.decodeFromString(countsSerializer, objectiveCountsJson)
        }.getOrDefault(emptyMap()),
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
    )

    private fun QuestProgress.toEntity(slot: Int): QuestProgressEntity = QuestProgressEntity(
        saveSlot = slot,
        questId = questId,
        state = state.name,
        objectiveCountsJson = json.encodeToString(countsSerializer, objectiveCounts),
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
    )
}

// ---------------------------------------------------------------------------
// Bestiary
// ---------------------------------------------------------------------------

@Singleton
class BestiaryRepositoryImpl @Inject constructor(
    private val dao: BestiaryDao,
    private val activeSlot: ActiveSlot,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BestiaryRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeEntries(): Flow<List<BestiaryEntry>> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observeAll(slot).map { rows -> rows.map { it.toDomain() } }
        }

    override suspend fun entry(speciesId: String): BestiaryEntry? = withContext(ioDispatcher) {
        dao.byId(activeSlot.current, speciesId)?.toDomain()
    }

    override suspend fun markSeen(speciesId: String, locationId: String?) = withContext(ioDispatcher) {
        val slot = activeSlot.current
        val existing = dao.byId(slot, speciesId)
        if (existing?.seen == true) return@withContext
        dao.upsert(
            existing?.copy(seen = true, firstSeenLocationId = existing.firstSeenLocationId ?: locationId)
                ?: BestiaryEntity(slot, speciesId, seen = true, firstSeenLocationId = locationId),
        )
    }

    override suspend fun markCaught(speciesId: String, shiny: Boolean, locationId: String?) =
        withContext(ioDispatcher) {
            val slot = activeSlot.current
            val existing = dao.byId(slot, speciesId)
            val now = System.currentTimeMillis()
            dao.upsert(
                existing?.copy(
                    seen = true,
                    caught = true,
                    shinyCaught = existing.shinyCaught || shiny,
                    caughtCount = existing.caughtCount + 1,
                    firstCaughtEpochMs = existing.firstCaughtEpochMs ?: now,
                    firstSeenLocationId = existing.firstSeenLocationId ?: locationId,
                ) ?: BestiaryEntity(
                    saveSlot = slot,
                    speciesId = speciesId,
                    seen = true,
                    caught = true,
                    shinyCaught = shiny,
                    caughtCount = 1,
                    firstSeenLocationId = locationId,
                    firstCaughtEpochMs = now,
                ),
            )
        }

    override suspend fun caughtCount(): Int = withContext(ioDispatcher) {
        dao.caughtCount(activeSlot.current)
    }

    override suspend fun seenCount(): Int = withContext(ioDispatcher) {
        dao.seenCount(activeSlot.current)
    }

    suspend fun shinyCount(): Int = withContext(ioDispatcher) {
        dao.shinyCount(activeSlot.current)
    }

    private fun BestiaryEntity.toDomain(): BestiaryEntry = BestiaryEntry(
        speciesId = speciesId,
        seen = seen,
        caught = caught,
        shinyCaught = shinyCaught,
        caughtCount = caughtCount,
        firstSeenLocationId = firstSeenLocationId,
        firstCaughtEpochMs = firstCaughtEpochMs,
    )
}

// ---------------------------------------------------------------------------
// World state
// ---------------------------------------------------------------------------

@Singleton
class WorldStateRepositoryImpl @Inject constructor(
    private val dao: WorldStateDao,
    private val gameStateDao: GameStateDao,
    private val content: ContentRepository,
    private val activeSlot: ActiveSlot,
    private val json: Json,
    private val rng: Rng,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WorldStateRepository {

    private val listSerializer = kotlinx.serialization.builtins.ListSerializer(String.serializer())

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeWeather(): Flow<Map<String, RegionWeatherState>> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observeWeather(slot).map { rows ->
                rows.associate { it.regionId to it.toDomain() }
            }
        }

    override suspend fun weatherFor(regionId: String): RegionWeatherState =
        withContext(ioDispatcher) {
            dao.weatherFor(activeSlot.current, regionId)?.toDomain() ?: rollWeather(regionId)
        }

    /** Re-rolls the weather of every region whose current spell has expired. */
    override suspend fun advanceWeather(nowEpochMs: Long) = withContext(ioDispatcher) {
        val slot = activeSlot.current
        val existing = dao.allWeather(slot).associateBy { it.regionId }
        val updates = content.allRegions().mapNotNull { region ->
            val current = existing[region.id]
            if (current != null && !current.toDomain().isExpired(nowEpochMs)) return@mapNotNull null
            rollWeather(region.id, nowEpochMs).toEntity(slot)
        }
        if (updates.isNotEmpty()) dao.upsertAllWeather(updates)
    }

    override suspend fun setWeather(state: RegionWeatherState) = withContext(ioDispatcher) {
        dao.upsertWeather(state.toEntity(activeSlot.current))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeInGameMinutes(): Flow<Long> =
        activeSlot.slot.flatMapLatest { slot ->
            gameStateDao.observe(slot).filterNotNull().map { it.inGameMinutes }
        }

    override suspend fun advanceTime(minutes: Long) = withContext(ioDispatcher) {
        val state = requireState()
        gameStateDao.upsert(state.copy(inGameMinutes = state.inGameMinutes + minutes))
    }

    override suspend fun discoveredLocations(): Set<String> = withContext(ioDispatcher) {
        decode(requireState().discoveredLocationsJson)
    }

    override suspend fun discoverLocation(locationId: String) = withContext(ioDispatcher) {
        val state = requireState()
        val updated = decode(state.discoveredLocationsJson) + locationId
        gameStateDao.upsert(state.copy(discoveredLocationsJson = encode(updated)))
    }

    override suspend fun unlockFastTravel(locationId: String) = withContext(ioDispatcher) {
        val state = requireState()
        val updated = decode(state.fastTravelLocationsJson) + locationId
        gameStateDao.upsert(state.copy(fastTravelLocationsJson = encode(updated)))
    }

    override suspend fun fastTravelLocations(): Set<String> = withContext(ioDispatcher) {
        decode(requireState().fastTravelLocationsJson)
    }

    override suspend fun openedChests(): Set<String> = withContext(ioDispatcher) {
        decode(requireState().openedChestsJson)
    }

    override suspend fun markChestOpened(chestId: String) = withContext(ioDispatcher) {
        val state = requireState()
        val updated = decode(state.openedChestsJson) + chestId
        gameStateDao.upsert(state.copy(openedChestsJson = encode(updated)))
    }

    override suspend fun defeatedTrainers(): Set<String> = withContext(ioDispatcher) {
        decode(requireState().defeatedTrainersJson)
    }

    override suspend fun markTrainerDefeated(trainerId: String) = withContext(ioDispatcher) {
        val state = requireState()
        val updated = decode(state.defeatedTrainersJson) + trainerId
        gameStateDao.upsert(state.copy(defeatedTrainersJson = encode(updated)))
    }

    /** Weighted roll from the region's weather profile. */
    private fun rollWeather(regionId: String, nowEpochMs: Long = System.currentTimeMillis()):
        RegionWeatherState {
        val region = content.region(regionId)
        val profile = region?.weatherProfile
        val weights = profile?.weights?.filterValues { it > 0 }.orEmpty()
        val weather = if (weights.isEmpty()) {
            BattleWeather.CLEAR
        } else {
            val entries = weights.entries.toList()
            rng.pickWeighted(entries) { it.value }?.key ?: BattleWeather.CLEAR
        }
        val minutes = profile?.changeIntervalMinutes ?: 12
        return RegionWeatherState(
            regionId = regionId,
            weather = weather,
            startedAtEpochMs = nowEpochMs,
            durationMs = minutes * 60_000L,
        )
    }

    private suspend fun requireState(): GameStateEntity =
        gameStateDao.bySlot(activeSlot.current)
            ?: error("Kein Spielstand in Slot ${activeSlot.current}.")

    private fun RegionWeatherEntity.toDomain() = RegionWeatherState(
        regionId = regionId,
        weather = enumValueOrDefault(weather, BattleWeather.CLEAR),
        startedAtEpochMs = startedAtEpochMs,
        durationMs = durationMs,
    )

    private fun RegionWeatherState.toEntity(slot: Int) = RegionWeatherEntity(
        saveSlot = slot,
        regionId = regionId,
        weather = weather.name,
        startedAtEpochMs = startedAtEpochMs,
        durationMs = durationMs,
    )

    private fun decode(raw: String): Set<String> =
        runCatching { json.decodeFromString(listSerializer, raw).toSet() }.getOrDefault(emptySet())

    private fun encode(values: Set<String>): String =
        json.encodeToString(listSerializer, values.toList())
}

// ---------------------------------------------------------------------------
// Achievements
// ---------------------------------------------------------------------------

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val dao: AchievementDao,
    private val content: ContentRepository,
    private val activeSlot: ActiveSlot,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AchievementRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeProgress(): Flow<List<AchievementProgress>> =
        activeSlot.slot.flatMapLatest { slot ->
            dao.observeAll(slot).map { rows ->
                rows.map { AchievementProgress(it.achievementId, it.progress, it.unlockedAtEpochMs) }
            }
        }

    /** @return true when this call completed the achievement. */
    override suspend fun increment(achievementId: String, amount: Int): Boolean =
        withContext(ioDispatcher) {
            val slot = activeSlot.current
            val definition = content.allAchievements().firstOrNull { it.id == achievementId }
                ?: return@withContext false
            val existing = dao.byId(slot, achievementId)
            if (existing?.unlockedAtEpochMs != null) return@withContext false
            val progress = (existing?.progress ?: 0) + amount
            val unlocked = progress >= definition.progressTarget
            dao.upsert(
                AchievementEntity(
                    saveSlot = slot,
                    achievementId = achievementId,
                    progress = progress.coerceAtMost(definition.progressTarget),
                    unlockedAtEpochMs = if (unlocked) System.currentTimeMillis() else null,
                ),
            )
            unlocked
        }

    override suspend fun unlock(achievementId: String): Boolean = withContext(ioDispatcher) {
        val slot = activeSlot.current
        val definition = content.allAchievements().firstOrNull { it.id == achievementId }
            ?: return@withContext false
        val existing = dao.byId(slot, achievementId)
        if (existing?.unlockedAtEpochMs != null) return@withContext false
        dao.upsert(
            AchievementEntity(
                saveSlot = slot,
                achievementId = achievementId,
                progress = definition.progressTarget,
                unlockedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        true
    }

    override suspend fun progressFor(achievementId: String): AchievementProgress? =
        withContext(ioDispatcher) {
            dao.byId(activeSlot.current, achievementId)?.let {
                AchievementProgress(it.achievementId, it.progress, it.unlockedAtEpochMs)
            }
        }
}
