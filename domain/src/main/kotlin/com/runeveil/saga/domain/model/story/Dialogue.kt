package com.runeveil.saga.domain.model.story

import com.runeveil.saga.domain.model.monster.DayPhase

/**
 * A branching conversation.
 *
 * Every NPC owns at least one tree. Nodes are addressed by id; the runtime
 * walks the graph, evaluating [DialogueCondition]s to pick the first node whose
 * conditions hold. This makes day/night variants, quest-state variants and
 * faction-reputation variants pure content.
 */
data class DialogueTree(
    val id: String,
    val npcId: String,
    val entryNodeId: String,
    val nodes: List<DialogueNode>,
    val priority: Int = 0,
    val conditions: List<DialogueCondition> = emptyList(),
) {
    fun node(id: String): DialogueNode? = nodes.firstOrNull { it.id == id }
}

/**
 * @property textKey localisation key of the spoken line.
 * @property choices when empty the node auto-advances to [nextNodeId].
 * @property actions side effects fired when the node is shown.
 */
data class DialogueNode(
    val id: String,
    val speakerNameKey: String,
    val textKey: String,
    val portraitKey: String? = null,
    val emotion: String = "neutral",
    val choices: List<DialogueChoice> = emptyList(),
    val nextNodeId: String? = null,
    val actions: List<DialogueAction> = emptyList(),
    val conditions: List<DialogueCondition> = emptyList(),
    val voiceKey: String? = null,
)

data class DialogueChoice(
    val id: String,
    val textKey: String,
    val nextNodeId: String?,
    val conditions: List<DialogueCondition> = emptyList(),
    val actions: List<DialogueAction> = emptyList(),
    val moralWeight: Int = 0,
    val disabledHintKey: String? = null,
)

/** Guard on a node, choice or whole tree. */
sealed interface DialogueCondition {
    data class HasFlag(val flag: String) : DialogueCondition
    data class MissingFlag(val flag: String) : DialogueCondition
    data class QuestInState(val questId: String, val state: QuestState) : DialogueCondition
    data class HasItem(val itemId: String, val count: Int = 1) : DialogueCondition
    data class HasGold(val amount: Int) : DialogueCondition
    data class MinLevel(val level: Int) : DialogueCondition
    data class TimeOfDay(val phase: DayPhase) : DialogueCondition
    data class MinReputation(val factionId: String, val amount: Int) : DialogueCondition
    data class MaxReputation(val factionId: String, val amount: Int) : DialogueCondition
    data class HasSpecies(val speciesId: String) : DialogueCondition
    data class PartySize(val min: Int, val max: Int = 6) : DialogueCondition
    data class BestiaryCount(val min: Int) : DialogueCondition
    data class ChapterAtLeast(val chapter: Int) : DialogueCondition
}

/** Side effect of showing a node or picking a choice. */
sealed interface DialogueAction {
    data class GrantFlag(val flag: String) : DialogueAction
    data class ClearFlag(val flag: String) : DialogueAction
    data class StartQuest(val questId: String) : DialogueAction
    data class CompleteObjective(val questId: String, val objectiveId: String, val amount: Int = 1) : DialogueAction
    data class GiveItem(val itemId: String, val count: Int = 1) : DialogueAction
    data class TakeItem(val itemId: String, val count: Int = 1) : DialogueAction
    data class GiveGold(val amount: Int) : DialogueAction
    data class TakeGold(val amount: Int) : DialogueAction
    data class GiveMonster(val speciesId: String, val level: Int) : DialogueAction
    data class ChangeReputation(val factionId: String, val amount: Int) : DialogueAction
    data class OpenShop(val shopId: String) : DialogueAction
    data class StartBattle(val encounterId: String) : DialogueAction
    data class PlayCutscene(val cutsceneId: String) : DialogueAction
    data class HealParty(val full: Boolean = true) : DialogueAction
    data class UnlockFastTravel(val locationId: String) : DialogueAction
    data class ChangeMoralScore(val amount: Int) : DialogueAction
    data object EndDialogue : DialogueAction
}
