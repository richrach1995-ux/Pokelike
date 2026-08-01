package com.runeveil.saga.domain.model.story

import com.runeveil.saga.domain.model.world.UnlockRequirement

/**
 * A quest — main, side, daily, faction, legendary or secret.
 *
 * Quests are pure data. Progress lives in `QuestProgress` (persisted), and the
 * rules that advance it live in `QuestTracker`.
 */
data class Quest(
    val id: String,
    val nameKey: String,
    val summaryKey: String,
    val descriptionKey: String,
    val category: QuestCategory,
    val chapter: Int? = null,
    val giverNpcId: String? = null,
    val turnInNpcId: String? = null,
    val regionId: String? = null,
    val recommendedLevel: Int = 1,
    val objectives: List<QuestObjective>,
    val rewards: List<QuestReward> = emptyList(),
    val prerequisite: UnlockRequirement = UnlockRequirement.None,
    val prerequisiteQuestIds: List<String> = emptyList(),
    val grantsStoryFlags: List<String> = emptyList(),
    val repeatable: Boolean = false,
    val timeLimitMinutes: Int? = null,
    val isHidden: Boolean = false,
    val factionId: String? = null,
    val factionReputationDelta: Int = 0,
    val nextQuestId: String? = null,
    val cutsceneIdOnStart: String? = null,
    val cutsceneIdOnComplete: String? = null,
)

enum class QuestCategory(val displayKey: String, val sortOrder: Int) {
    MAIN("quest_cat_main", 0),
    SIDE("quest_cat_side", 1),
    FACTION("quest_cat_faction", 2),
    DAILY("quest_cat_daily", 3),
    LEGENDARY("quest_cat_legendary", 4),
    SECRET("quest_cat_secret", 5),
}

/**
 * One step of a quest. The tracker matches game events against the objective's
 * type and target id.
 */
data class QuestObjective(
    val id: String,
    val descriptionKey: String,
    val type: ObjectiveType,
    val targetId: String? = null,
    val requiredCount: Int = 1,
    val optional: Boolean = false,
    val hiddenUntilPrevious: Boolean = false,
)

enum class ObjectiveType {
    DEFEAT_SPECIES,
    DEFEAT_TRAINER,
    DEFEAT_BOSS,
    CAPTURE_SPECIES,
    CAPTURE_ANY_OF_ELEMENT,
    COLLECT_ITEM,
    DELIVER_ITEM,
    TALK_TO_NPC,
    REACH_LOCATION,
    REACH_LEVEL,
    HATCH_EGG,
    EVOLVE_MONSTER,
    CRAFT_ITEM,
    WIN_BATTLES,
    COMPLETE_DUNGEON,
    BIND_RUNE,
    DISCOVER_SECRET,
    SURVIVE_TURNS,
}

/** What a quest pays out. */
sealed interface QuestReward {
    data class Gold(val amount: Int) : QuestReward
    data class Items(val itemIds: Map<String, Int>) : QuestReward
    data class Experience(val amount: Long) : QuestReward
    data class Monster(val speciesId: String, val level: Int) : QuestReward
    data class Reputation(val factionId: String, val amount: Int) : QuestReward
    data class UnlockTitle(val titleId: String) : QuestReward
    data class UnlockRegion(val regionId: String) : QuestReward
    data class UnlockRecipe(val recipeId: String) : QuestReward
    data class StoryFlag(val flag: String) : QuestReward
}

/** Live progress of one quest for the current save. */
data class QuestProgress(
    val questId: String,
    val state: QuestState,
    val objectiveCounts: Map<String, Int> = emptyMap(),
    val startedAtEpochMs: Long = 0L,
    val completedAtEpochMs: Long? = null,
) {
    fun countFor(objectiveId: String): Int = objectiveCounts[objectiveId] ?: 0

    fun isObjectiveComplete(objective: QuestObjective): Boolean =
        countFor(objective.id) >= objective.requiredCount

    /** Overall completion in 0f…1f across all non-optional objectives. */
    fun progress(quest: Quest): Float {
        val required = quest.objectives.filterNot { it.optional }
        if (required.isEmpty()) return if (state == QuestState.COMPLETED) 1f else 0f
        val done = required.sumOf { objective ->
            countFor(objective.id).coerceAtMost(objective.requiredCount).toDouble() /
                objective.requiredCount
        }
        return (done / required.size).toFloat().coerceIn(0f, 1f)
    }
}

enum class QuestState { AVAILABLE, ACTIVE, READY_TO_TURN_IN, COMPLETED, FAILED }

/**
 * A chapter of the main story. Chapters gate regions, change the world's
 * weather profile and unlock cutscenes.
 */
data class StoryChapter(
    val number: Int,
    val titleKey: String,
    val synopsisKey: String,
    val regionId: String,
    val mainQuestIds: List<String>,
    val openingCutsceneId: String? = null,
    val closingCutsceneId: String? = null,
    val unlocksRegionIds: List<String> = emptyList(),
    val bossId: String? = null,
    val musicKey: String? = null,
)

/**
 * A cutscene: an ordered list of beats the presentation layer plays back.
 * Beats are declarative so the same data drives both the full-screen cinematic
 * and the "recap" list in the journal.
 */
data class Cutscene(
    val id: String,
    val titleKey: String,
    val beats: List<CutsceneBeat>,
    val musicKey: String? = null,
    val skippable: Boolean = true,
)

sealed interface CutsceneBeat {
    data class Narration(val textKey: String, val durationMs: Int = 3200) : CutsceneBeat
    data class Speech(
        val speakerNpcId: String?,
        val speakerNameKey: String,
        val textKey: String,
        val portraitKey: String? = null,
        val emotion: String = "neutral",
    ) : CutsceneBeat
    data class ShowImage(val imageKey: String, val durationMs: Int = 2600) : CutsceneBeat
    data class PlaySound(val soundKey: String) : CutsceneBeat
    data class ChangeMusic(val musicKey: String?, val fadeMs: Int = 900) : CutsceneBeat
    data class ScreenEffect(val effect: String, val durationMs: Int = 800) : CutsceneBeat
    data class Choice(val promptKey: String, val options: List<CutsceneChoice>) : CutsceneBeat
    data class GrantFlag(val flag: String) : CutsceneBeat
    data class StartBattle(val encounterId: String) : CutsceneBeat
}

data class CutsceneChoice(
    val id: String,
    val textKey: String,
    val grantsFlag: String? = null,
    val moralWeight: Int = 0,
)

/**
 * One of the game's endings. The epilogue chosen at the end of chapter 9
 * depends on flags, faction standing and the moral score accumulated through
 * dialogue choices.
 */
data class Ending(
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val cutsceneId: String,
    val requiredFlags: Set<String> = emptySet(),
    val forbiddenFlags: Set<String> = emptySet(),
    val minMoralScore: Int? = null,
    val maxMoralScore: Int? = null,
    val minFactionStanding: Map<String, Int> = emptyMap(),
    val requiresAllRunes: Boolean = false,
    val priority: Int = 0,
)
