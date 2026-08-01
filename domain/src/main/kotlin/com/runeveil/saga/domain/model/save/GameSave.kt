package com.runeveil.saga.domain.model.save

import com.runeveil.saga.domain.model.monster.World
import com.runeveil.saga.domain.model.player.PlayerProfile

/**
 * Lightweight header of a save slot — everything the load screen needs without
 * deserialising the whole game state.
 */
data class SaveSlotSummary(
    val slotIndex: Int,
    val exists: Boolean,
    val playerName: String,
    val chapter: Int,
    val chapterTitleKey: String,
    val level: Int,
    val playtimeSeconds: Long,
    val bestiaryCaught: Int,
    val bestiaryTotal: Int,
    val world: World,
    val locationNameKey: String,
    val savedAtEpochMs: Long,
    val isAutosave: Boolean,
    val newGamePlusCount: Int,
    val partyPreviewSpriteKeys: List<String> = emptyList(),
) {
    val completionPercent: Int
        get() = if (bestiaryTotal <= 0) 0 else (bestiaryCaught * 100 / bestiaryTotal)

    companion object {
        fun empty(slotIndex: Int) = SaveSlotSummary(
            slotIndex = slotIndex,
            exists = false,
            playerName = "",
            chapter = 0,
            chapterTitleKey = "",
            level = 0,
            playtimeSeconds = 0L,
            bestiaryCaught = 0,
            bestiaryTotal = 0,
            world = World.MIDGARD,
            locationNameKey = "",
            savedAtEpochMs = 0L,
            isAutosave = false,
            newGamePlusCount = 0,
        )
    }
}

/**
 * The full game state of one slot.
 *
 * Note that monsters, inventory and quest progress are *not* embedded here:
 * they live in their own Room tables keyed by [slotIndex], which keeps saving
 * incremental (an autosave writes a handful of rows, not a megabyte blob).
 * This class holds the singleton-ish state only.
 */
data class GameSave(
    val slotIndex: Int,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val player: PlayerProfile,
    val chapter: Int,
    val storyFlags: Set<String>,
    val discoveredLocationIds: Set<String>,
    val fastTravelLocationIds: Set<String>,
    val defeatedTrainerIds: Set<String>,
    val openedChestIds: Set<String>,
    val unlockedRecipeIds: Set<String>,
    val factionReputation: Map<String, Int>,
    val partyMonsterUids: List<String>,
    val inGameMinutes: Long,
    val savedAtEpochMs: Long,
    val isAutosave: Boolean,
    val endlessBestFloor: Int = 0,
    val activeEndingId: String? = null,
) {
    /** In-game hour 0…23, derived from [inGameMinutes]. */
    val inGameHour: Int get() = ((inGameMinutes / 60) % 24).toInt()

    companion object {
        /** Bump when the save layout changes; migrations live in :data. */
        const val CURRENT_SCHEMA_VERSION = 1

        /** Manual slots; the autosave uses index [AUTOSAVE_SLOT]. */
        const val MANUAL_SLOT_COUNT = 3
        const val AUTOSAVE_SLOT = 0

        /** Real-time seconds that advance one in-game minute. */
        const val SECONDS_PER_GAME_MINUTE = 2
    }
}
