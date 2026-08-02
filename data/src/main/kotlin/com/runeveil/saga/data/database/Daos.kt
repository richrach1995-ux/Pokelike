package com.runeveil.saga.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MonsterDao {

    @Query("SELECT * FROM monsters WHERE saveSlot = :slot AND inParty = 1 ORDER BY partyOrder ASC")
    fun observeParty(slot: Int): Flow<List<MonsterEntity>>

    @Query("SELECT * FROM monsters WHERE saveSlot = :slot AND inParty = 0 ORDER BY speciesId ASC")
    fun observeVault(slot: Int): Flow<List<MonsterEntity>>

    @Query("SELECT * FROM monsters WHERE saveSlot = :slot AND inParty = 1 ORDER BY partyOrder ASC")
    suspend fun party(slot: Int): List<MonsterEntity>

    @Query("SELECT * FROM monsters WHERE saveSlot = :slot AND inParty = 0 ORDER BY speciesId ASC")
    suspend fun vault(slot: Int): List<MonsterEntity>

    @Query("SELECT * FROM monsters WHERE uid = :uid")
    suspend fun byUid(uid: String): MonsterEntity?

    @Query("SELECT COUNT(*) FROM monsters WHERE saveSlot = :slot AND inParty = 1")
    suspend fun partySize(slot: Int): Int

    @Query("SELECT COALESCE(MAX(partyOrder), -1) FROM monsters WHERE saveSlot = :slot AND inParty = 1")
    suspend fun maxPartyOrder(slot: Int): Int

    @Query("SELECT * FROM monsters WHERE saveSlot = :slot AND atRoost = 1")
    suspend fun atRoost(slot: Int): List<MonsterEntity>

    @Upsert
    suspend fun upsert(monster: MonsterEntity)

    @Upsert
    suspend fun upsertAll(monsters: List<MonsterEntity>)

    @Query("DELETE FROM monsters WHERE uid = :uid")
    suspend fun delete(uid: String)

    @Query("DELETE FROM monsters WHERE saveSlot = :slot")
    suspend fun deleteSlot(slot: Int)

    @Query("UPDATE monsters SET inParty = :inParty, partyOrder = :order WHERE uid = :uid")
    suspend fun setPartyState(uid: String, inParty: Boolean, order: Int)

    @Query("UPDATE monsters SET atRoost = :atRoost WHERE uid = :uid")
    suspend fun setRoostState(uid: String, atRoost: Boolean)

    /**
     * Clears status conditions for the whole party in one statement.
     * Hit points cannot be restored here — the maximum depends on species,
     * genes, training and level, which only the rule layer knows — so
     * `MonsterRepositoryImpl.healParty()` writes the HP values itself.
     */
    @Query(
        """
        UPDATE monsters
        SET status = NULL, statusTurns = 0
        WHERE saveSlot = :slot AND inParty = 1
        """,
    )
    suspend fun clearStatusForParty(slot: Int)

    @Transaction
    suspend fun reorderParty(slot: Int, orderedUids: List<String>) {
        orderedUids.forEachIndexed { index, uid ->
            setPartyState(uid, inParty = true, order = index)
        }
    }
}

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory WHERE saveSlot = :slot AND quantity > 0")
    fun observeAll(slot: Int): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE saveSlot = :slot AND quantity > 0")
    suspend fun all(slot: Int): List<InventoryEntity>

    @Query("SELECT quantity FROM inventory WHERE saveSlot = :slot AND itemId = :itemId")
    suspend fun quantityOf(slot: Int, itemId: String): Int?

    @Upsert
    suspend fun upsert(stack: InventoryEntity)

    @Upsert
    suspend fun upsertAll(stacks: List<InventoryEntity>)

    @Query("DELETE FROM inventory WHERE saveSlot = :slot AND itemId = :itemId")
    suspend fun delete(slot: Int, itemId: String)

    @Query("DELETE FROM inventory WHERE saveSlot = :slot")
    suspend fun deleteSlot(slot: Int)

    @Query("DELETE FROM inventory WHERE quantity <= 0")
    suspend fun pruneEmpty()

    /**
     * Adds [amount] to a stack, creating it when missing. Kept as a transaction
     * so two concurrent pickups cannot lose an item.
     */
    @Transaction
    suspend fun add(slot: Int, itemId: String, amount: Int, stackLimit: Int) {
        val current = quantityOf(slot, itemId) ?: 0
        val updated = (current + amount).coerceAtMost(stackLimit * MAX_STACKS_PER_ITEM)
        upsert(InventoryEntity(slot, itemId, updated))
    }

    /** Removes [amount]; returns false when the satchel does not hold enough. */
    @Transaction
    suspend fun remove(slot: Int, itemId: String, amount: Int): Boolean {
        val current = quantityOf(slot, itemId) ?: 0
        if (current < amount) return false
        val remaining = current - amount
        if (remaining <= 0) delete(slot, itemId) else upsert(InventoryEntity(slot, itemId, remaining))
        return true
    }

    private companion object {
        /** Items stack visually in slots of `stackLimit`, but the row holds the total. */
        const val MAX_STACKS_PER_ITEM = 20
    }
}

@Dao
interface QuestDao {

    @Query("SELECT * FROM quest_progress WHERE saveSlot = :slot")
    fun observeAll(slot: Int): Flow<List<QuestProgressEntity>>

    @Query("SELECT * FROM quest_progress WHERE saveSlot = :slot AND questId = :questId")
    suspend fun byId(slot: Int, questId: String): QuestProgressEntity?

    @Query("SELECT * FROM quest_progress WHERE saveSlot = :slot AND state = :state")
    suspend fun byState(slot: Int, state: String): List<QuestProgressEntity>

    @Upsert
    suspend fun upsert(progress: QuestProgressEntity)

    @Upsert
    suspend fun upsertAll(progress: List<QuestProgressEntity>)

    @Query("DELETE FROM quest_progress WHERE saveSlot = :slot")
    suspend fun deleteSlot(slot: Int)
}

@Dao
interface BestiaryDao {

    @Query("SELECT * FROM bestiary WHERE saveSlot = :slot")
    fun observeAll(slot: Int): Flow<List<BestiaryEntity>>

    @Query("SELECT * FROM bestiary WHERE saveSlot = :slot AND speciesId = :speciesId")
    suspend fun byId(slot: Int, speciesId: String): BestiaryEntity?

    @Query("SELECT COUNT(*) FROM bestiary WHERE saveSlot = :slot AND caught = 1")
    suspend fun caughtCount(slot: Int): Int

    @Query("SELECT COUNT(*) FROM bestiary WHERE saveSlot = :slot AND seen = 1")
    suspend fun seenCount(slot: Int): Int

    @Query("SELECT COUNT(*) FROM bestiary WHERE saveSlot = :slot AND shinyCaught = 1")
    suspend fun shinyCount(slot: Int): Int

    @Upsert
    suspend fun upsert(entry: BestiaryEntity)

    @Query("DELETE FROM bestiary WHERE saveSlot = :slot")
    suspend fun deleteSlot(slot: Int)
}

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements WHERE saveSlot = :slot")
    fun observeAll(slot: Int): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE saveSlot = :slot AND achievementId = :id")
    suspend fun byId(slot: Int, id: String): AchievementEntity?

    @Upsert
    suspend fun upsert(entry: AchievementEntity)

    @Query("DELETE FROM achievements WHERE saveSlot = :slot")
    suspend fun deleteSlot(slot: Int)
}

@Dao
interface WorldStateDao {

    @Query("SELECT * FROM region_weather WHERE saveSlot = :slot")
    fun observeWeather(slot: Int): Flow<List<RegionWeatherEntity>>

    @Query("SELECT * FROM region_weather WHERE saveSlot = :slot AND regionId = :regionId")
    suspend fun weatherFor(slot: Int, regionId: String): RegionWeatherEntity?

    @Query("SELECT * FROM region_weather WHERE saveSlot = :slot")
    suspend fun allWeather(slot: Int): List<RegionWeatherEntity>

    @Upsert
    suspend fun upsertWeather(entity: RegionWeatherEntity)

    @Upsert
    suspend fun upsertAllWeather(entities: List<RegionWeatherEntity>)

    @Query("DELETE FROM region_weather WHERE saveSlot = :slot")
    suspend fun deleteSlot(slot: Int)
}

@Dao
interface GameStateDao {

    @Query("SELECT * FROM game_state ORDER BY saveSlot ASC")
    fun observeAll(): Flow<List<GameStateEntity>>

    @Query("SELECT * FROM game_state ORDER BY saveSlot ASC")
    suspend fun all(): List<GameStateEntity>

    @Query("SELECT * FROM game_state WHERE saveSlot = :slot")
    fun observe(slot: Int): Flow<GameStateEntity?>

    @Query("SELECT * FROM game_state WHERE saveSlot = :slot")
    suspend fun bySlot(slot: Int): GameStateEntity?

    @Upsert
    suspend fun upsert(state: GameStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: GameStateEntity)

    @Query("DELETE FROM game_state WHERE saveSlot = :slot")
    suspend fun deleteSlot(slot: Int)
}
