package com.runeveil.saga.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * The single Room database of the game.
 *
 * Schema files are exported to `data/schemas/` (see `ksp { arg(...) }` in the
 * module's build script) so every future migration can be diffed and tested
 * with `MigrationTestHelper`.
 *
 * Version history:
 *  * **1** — initial schema (monsters, inventory, quests, bestiary,
 *    achievements, region weather, game state).
 */
@Database(
    entities = [
        MonsterEntity::class,
        InventoryEntity::class,
        QuestProgressEntity::class,
        BestiaryEntity::class,
        AchievementEntity::class,
        RegionWeatherEntity::class,
        GameStateEntity::class,
    ],
    version = RuneveilDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(RuneveilConverters::class)
abstract class RuneveilDatabase : RoomDatabase() {

    abstract fun monsterDao(): MonsterDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun questDao(): QuestDao
    abstract fun bestiaryDao(): BestiaryDao
    abstract fun achievementDao(): AchievementDao
    abstract fun worldStateDao(): WorldStateDao
    abstract fun gameStateDao(): GameStateDao

    companion object {
        const val VERSION = 1
        const val NAME = "runeveil.db"
    }
}

/**
 * Converters for the handful of non-primitive columns.
 *
 * Everything structural is stored as JSON text; these converters only handle
 * the simple list/set columns so the DAOs stay free of manual parsing.
 */
class RuneveilConverters {

    @TypeConverter
    fun stringListToText(value: List<String>?): String =
        value?.joinToString(SEPARATOR).orEmpty()

    @TypeConverter
    fun textToStringList(value: String?): List<String> =
        value?.takeIf { it.isNotEmpty() }?.split(SEPARATOR).orEmpty()

    @TypeConverter
    fun stringSetToText(value: Set<String>?): String =
        value?.joinToString(SEPARATOR).orEmpty()

    @TypeConverter
    fun textToStringSet(value: String?): Set<String> =
        value?.takeIf { it.isNotEmpty() }?.split(SEPARATOR)?.toSet().orEmpty()

    private companion object {
        /** Ids are slugs, so a comma can never appear inside one. */
        const val SEPARATOR = ","
    }
}
