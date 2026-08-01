package com.runeveil.saga.domain.model.world

import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.Element
import com.runeveil.saga.domain.model.monster.World

/**
 * One of the Nine Worlds as it exists on the world map.
 *
 * A region is a **graph of locations**, not a tile grid: the player travels
 * between nodes along [LocationLink]s. This keeps traversal readable on a
 * phone, makes the map fully data-driven, and lets a location own its own
 * encounter table, music and weather profile.
 */
data class Region(
    val id: String,
    val world: World,
    val nameKey: String,
    val descriptionKey: String,
    val loreKey: String,
    val recommendedLevelRange: IntRange,
    val dominantElements: List<Element>,
    val musicKey: String,
    val nightMusicKey: String? = null,
    val ambienceKey: String? = null,
    val mapAssetKey: String,
    val paletteKey: String,
    val weatherProfile: WeatherProfile,
    val locations: List<Location>,
    val links: List<LocationLink>,
    val unlockRequirement: UnlockRequirement = UnlockRequirement.None,
    val legendaryMonsterIds: List<String> = emptyList(),
    val collectibleIds: List<String> = emptyList(),
    val secretIds: List<String> = emptyList(),
) {
    fun location(id: String): Location? = locations.firstOrNull { it.id == id }

    /** Locations reachable from [locationId] in one step. */
    fun neighboursOf(locationId: String): List<LocationLink> =
        links.filter { it.fromLocationId == locationId }

    val towns: List<Location> get() = locations.filter { it.type == LocationType.TOWN }
    val dungeons: List<Location> get() = locations.filter { it.type == LocationType.DUNGEON }
}

/** What must be true before a region or location can be entered. */
sealed interface UnlockRequirement {
    data object None : UnlockRequirement
    data class StoryFlag(val flag: String) : UnlockRequirement
    data class Chapter(val chapter: Int) : UnlockRequirement
    data class KeyItem(val itemId: String) : UnlockRequirement
    data class RuneCount(val count: Int) : UnlockRequirement
    data class Reputation(val factionId: String, val minimum: Int) : UnlockRequirement
    data class All(val requirements: List<UnlockRequirement>) : UnlockRequirement
}

/** A single node of a region's map. */
data class Location(
    val id: String,
    val regionId: String,
    val nameKey: String,
    val descriptionKey: String,
    val type: LocationType,
    val mapX: Float,
    val mapY: Float,
    val backgroundKey: String,
    val musicKeyOverride: String? = null,
    val encounterTableId: String? = null,
    val encounterRate: Double = 0.0,
    val npcIds: List<String> = emptyList(),
    val shopId: String? = null,
    val healerAvailable: Boolean = false,
    val roostAvailable: Boolean = false,
    val forgeAvailable: Boolean = false,
    val storageAvailable: Boolean = false,
    val chestIds: List<String> = emptyList(),
    val bossId: String? = null,
    val floors: Int = 1,
    val unlockRequirement: UnlockRequirement = UnlockRequirement.None,
    val fastTravel: Boolean = false,
    val secretHint: String? = null,
) {
    val hasWildEncounters: Boolean get() = encounterTableId != null && encounterRate > 0.0
    val isSettlement: Boolean get() = type == LocationType.TOWN || type == LocationType.SANCTUARY
}

enum class LocationType(val displayKey: String, val iconKey: String) {
    TOWN("loc_town", "ic_map_town"),
    ROUTE("loc_route", "ic_map_route"),
    DUNGEON("loc_dungeon", "ic_map_dungeon"),
    LANDMARK("loc_landmark", "ic_map_landmark"),
    SANCTUARY("loc_sanctuary", "ic_map_sanctuary"),
    RIFT("loc_rift", "ic_map_rift"),
    ARENA("loc_arena", "ic_map_arena"),
    SECRET("loc_secret", "ic_map_secret"),
}

/** A directed edge of the region graph. */
data class LocationLink(
    val fromLocationId: String,
    val toLocationId: String,
    val travelSteps: Int = 30,
    val requirement: UnlockRequirement = UnlockRequirement.None,
    val bidirectional: Boolean = true,
)

/**
 * Which weather a region rolls, and how often.
 *
 * Weights are relative; the sum does not need to be 100. [seasonalOverrides]
 * lets a region force one weather during a story chapter (e.g. Niflheim's
 * eternal blizzard after chapter 6).
 */
data class WeatherProfile(
    val weights: Map<BattleWeather, Int>,
    val changeIntervalMinutes: Int = 12,
    val seasonalOverrides: Map<String, BattleWeather> = emptyMap(),
) {
    fun weightOf(weather: BattleWeather): Int = weights[weather] ?: 0
}

/**
 * A wild-encounter table. Entries are filtered by day phase and weather before
 * being weighted, so the same route genuinely feels different at night or in a
 * storm.
 */
data class EncounterTable(
    val id: String,
    val entries: List<EncounterEntry>,
) {
    fun candidatesFor(phase: DayPhase, weather: BattleWeather, floor: Int = 1): List<EncounterEntry> =
        entries.filter { entry ->
            (entry.dayPhases.isEmpty() || phase in entry.dayPhases) &&
                (entry.weathers.isEmpty() || weather in entry.weathers) &&
                floor in entry.floorRange
        }
}

data class EncounterEntry(
    val speciesId: String,
    val minLevel: Int,
    val maxLevel: Int,
    val weight: Int,
    val dayPhases: Set<DayPhase> = emptySet(),
    val weathers: Set<BattleWeather> = emptySet(),
    val floorRange: IntRange = 1..99,
    val requiresStoryFlag: String? = null,
    val isRareSpawn: Boolean = false,
)

/** Live overworld weather of one region. */
data class RegionWeatherState(
    val regionId: String,
    val weather: BattleWeather,
    val startedAtEpochMs: Long,
    val durationMs: Long,
) {
    fun isExpired(nowEpochMs: Long): Boolean = nowEpochMs - startedAtEpochMs >= durationMs
}

/** A shop's stock. Prices come from the item; [markup] adjusts per region. */
data class Shop(
    val id: String,
    val nameKey: String,
    val ownerNpcId: String?,
    val itemIds: List<String>,
    val markup: Double = 1.0,
    val unlockRequirement: UnlockRequirement = UnlockRequirement.None,
    val restockDaily: Boolean = false,
    val factionId: String? = null,
    val factionDiscountPercent: Int = 0,
)

/** A findable container in the world. */
data class TreasureChest(
    val id: String,
    val locationId: String,
    val itemIds: List<String>,
    val goldAmount: Int = 0,
    val requiresKeyItemId: String? = null,
    val isHidden: Boolean = false,
    val respawnsDaily: Boolean = false,
)
