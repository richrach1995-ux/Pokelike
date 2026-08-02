package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.story.ObjectiveType
import com.runeveil.saga.domain.model.story.Quest
import com.runeveil.saga.domain.model.story.QuestProgress
import com.runeveil.saga.domain.model.story.QuestState
import com.runeveil.saga.domain.model.world.UnlockRequirement

/**
 * Quest bookkeeping: which quests are available, how world events advance
 * objectives, and when a quest becomes turn-in-able.
 */
object QuestRules {

    /** A world event that objectives can listen to. */
    sealed interface GameEvent {
        data class DefeatedSpecies(val speciesId: String) : GameEvent
        data class DefeatedTrainer(val trainerId: String) : GameEvent
        data class DefeatedBoss(val bossId: String) : GameEvent
        data class CapturedSpecies(val speciesId: String, val element: String) : GameEvent
        data class CollectedItem(val itemId: String, val amount: Int) : GameEvent
        data class DeliveredItem(val itemId: String, val npcId: String) : GameEvent
        data class TalkedToNpc(val npcId: String) : GameEvent
        data class ReachedLocation(val locationId: String) : GameEvent
        data class ReachedLevel(val level: Int) : GameEvent
        data object HatchedEgg : GameEvent
        data class EvolvedMonster(val speciesId: String) : GameEvent
        data class CraftedItem(val itemId: String) : GameEvent
        data object WonBattle : GameEvent
        data class CompletedDungeon(val locationId: String) : GameEvent
        data class BoundRune(val runeId: String) : GameEvent
        data class DiscoveredSecret(val secretId: String) : GameEvent
        data class SurvivedTurns(val turns: Int) : GameEvent
    }

    /** Everything needed to decide whether a quest may be started. */
    data class AvailabilityContext(
        val storyFlags: Set<String>,
        val chapter: Int,
        val completedQuestIds: Set<String>,
        val keyItemIds: Set<String>,
        val runeCount: Int,
        val reputation: Map<String, Int>,
    )

    fun isAvailable(quest: Quest, context: AvailabilityContext): Boolean {
        if (!quest.prerequisiteQuestIds.all { it in context.completedQuestIds }) return false
        return meets(quest.prerequisite, context)
    }

    /** Recursive evaluation of an [UnlockRequirement]. */
    fun meets(requirement: UnlockRequirement, context: AvailabilityContext): Boolean =
        when (requirement) {
            is UnlockRequirement.None -> true
            is UnlockRequirement.StoryFlag -> requirement.flag in context.storyFlags
            is UnlockRequirement.Chapter -> context.chapter >= requirement.chapter
            is UnlockRequirement.KeyItem -> requirement.itemId in context.keyItemIds
            is UnlockRequirement.RuneCount -> context.runeCount >= requirement.count
            is UnlockRequirement.Reputation ->
                (context.reputation[requirement.factionId] ?: 0) >= requirement.minimum
            is UnlockRequirement.All -> requirement.requirements.all { meets(it, context) }
        }

    /**
     * Applies [event] to [progress]. Returns the updated progress, or the same
     * instance when nothing matched (so callers can skip a database write).
     */
    fun applyEvent(quest: Quest, progress: QuestProgress, event: GameEvent): QuestProgress {
        if (progress.state != QuestState.ACTIVE) return progress
        var counts = progress.objectiveCounts
        var changed = false

        for (objective in quest.objectives) {
            if (progress.isObjectiveComplete(objective)) continue
            val increment = matchIncrement(objective.type, objective.targetId, event)
            if (increment <= 0) continue
            val current = counts[objective.id] ?: 0
            counts = counts + (objective.id to (current + increment).coerceAtMost(objective.requiredCount))
            changed = true
        }
        if (!changed) return progress

        val updated = progress.copy(objectiveCounts = counts)
        val allRequiredDone = quest.objectives
            .filterNot { it.optional }
            .all { updated.isObjectiveComplete(it) }
        return if (allRequiredDone) updated.copy(state = QuestState.READY_TO_TURN_IN) else updated
    }

    private fun matchIncrement(type: ObjectiveType, targetId: String?, event: GameEvent): Int =
        when (event) {
            is GameEvent.DefeatedSpecies ->
                if (type == ObjectiveType.DEFEAT_SPECIES && matches(targetId, event.speciesId)) 1 else 0

            is GameEvent.DefeatedTrainer ->
                if (type == ObjectiveType.DEFEAT_TRAINER && matches(targetId, event.trainerId)) 1 else 0

            is GameEvent.DefeatedBoss ->
                if (type == ObjectiveType.DEFEAT_BOSS && matches(targetId, event.bossId)) 1 else 0

            is GameEvent.CapturedSpecies -> when (type) {
                ObjectiveType.CAPTURE_SPECIES -> if (matches(targetId, event.speciesId)) 1 else 0
                ObjectiveType.CAPTURE_ANY_OF_ELEMENT ->
                    if (targetId.equals(event.element, ignoreCase = true)) 1 else 0
                else -> 0
            }

            is GameEvent.CollectedItem ->
                if (type == ObjectiveType.COLLECT_ITEM && matches(targetId, event.itemId)) event.amount else 0

            is GameEvent.DeliveredItem ->
                if (type == ObjectiveType.DELIVER_ITEM && matches(targetId, event.itemId)) 1 else 0

            is GameEvent.TalkedToNpc ->
                if (type == ObjectiveType.TALK_TO_NPC && matches(targetId, event.npcId)) 1 else 0

            is GameEvent.ReachedLocation ->
                if (type == ObjectiveType.REACH_LOCATION && matches(targetId, event.locationId)) 1 else 0

            is GameEvent.ReachedLevel ->
                if (type == ObjectiveType.REACH_LEVEL && event.level >= (targetId?.toIntOrNull() ?: 0)) 1 else 0

            is GameEvent.HatchedEgg -> if (type == ObjectiveType.HATCH_EGG) 1 else 0

            is GameEvent.EvolvedMonster ->
                if (type == ObjectiveType.EVOLVE_MONSTER && matches(targetId, event.speciesId)) 1 else 0

            is GameEvent.CraftedItem ->
                if (type == ObjectiveType.CRAFT_ITEM && matches(targetId, event.itemId)) 1 else 0

            is GameEvent.WonBattle -> if (type == ObjectiveType.WIN_BATTLES) 1 else 0

            is GameEvent.CompletedDungeon ->
                if (type == ObjectiveType.COMPLETE_DUNGEON && matches(targetId, event.locationId)) 1 else 0

            is GameEvent.BoundRune ->
                if (type == ObjectiveType.BIND_RUNE && matches(targetId, event.runeId)) 1 else 0

            is GameEvent.DiscoveredSecret ->
                if (type == ObjectiveType.DISCOVER_SECRET && matches(targetId, event.secretId)) 1 else 0

            is GameEvent.SurvivedTurns ->
                if (type == ObjectiveType.SURVIVE_TURNS && event.turns >= (targetId?.toIntOrNull() ?: 0)) 1 else 0
        }

    /** A null target id means "any". */
    private fun matches(targetId: String?, actual: String): Boolean =
        targetId == null || targetId == actual

    /** Daily quests reset at 04:00 local time. */
    fun shouldResetDaily(lastResetEpochMs: Long, nowEpochMs: Long): Boolean {
        if (lastResetEpochMs <= 0L) return true
        val dayMs = 24L * 60 * 60 * 1000
        val offsetMs = 4L * 60 * 60 * 1000
        return (nowEpochMs - offsetMs) / dayMs > (lastResetEpochMs - offsetMs) / dayMs
    }
}
