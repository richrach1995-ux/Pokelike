package com.runeveil.saga.presentation.dialogue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.runeveil.saga.R
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.story.DialogueAction
import com.runeveil.saga.domain.model.story.DialogueChoice
import com.runeveil.saga.domain.model.story.DialogueCondition
import com.runeveil.saga.domain.model.story.DialogueNode
import com.runeveil.saga.domain.model.story.DialogueTree
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.InventoryRepository
import com.runeveil.saga.domain.repository.MonsterRepository
import com.runeveil.saga.domain.repository.PlayerRepository
import com.runeveil.saga.domain.repository.QuestRepository
import com.runeveil.saga.domain.rules.QuestRules
import com.runeveil.saga.domain.usecase.StartQuestUseCase
import com.runeveil.saga.domain.usecase.TrackQuestEventUseCase
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicOutlinedButton
import com.runeveil.saga.ui.components.contentText
import com.runeveil.saga.ui.theme.RuneNightSunken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Conversation view. Walks the NPC's dialogue graph, evaluating conditions and
 * firing actions (quests, items, shops, battles) as the player picks answers.
 */
@Composable
fun DialogueScreen(
    npcId: String,
    onFinished: () -> Unit,
    onOpenShop: (String) -> Unit,
    onStartBattle: (String) -> Unit,
    viewModel: DialogueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(npcId) { viewModel.start(npcId) }
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    LaunchedEffect(state.openShopId) { state.openShopId?.let { viewModel.consumeShop(); onOpenShop(it) } }
    LaunchedEffect(state.startBattleId) {
        state.startBattleId?.let { viewModel.consumeBattle(); onStartBattle(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RuneNightSunken)
            .padding(20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val node = state.node
        if (node != null) {
            Column(Modifier.fillMaxWidth()) {
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            contentText(node.speakerNameKey),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(contentText(node.textKey), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.choices.isEmpty()) {
                        RunicOutlinedButton(
                            text = stringResource(R.string.common_continue),
                            onClick = viewModel::advance,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        state.choices.forEach { choice ->
                            RunicOutlinedButton(
                                text = contentText(choice.textKey),
                                onClick = { viewModel.choose(choice) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DialogueUiState(
    val node: DialogueNode? = null,
    val choices: List<DialogueChoice> = emptyList(),
    val finished: Boolean = false,
    val openShopId: String? = null,
    val startBattleId: String? = null,
)

@HiltViewModel
class DialogueViewModel @Inject constructor(
    private val content: ContentRepository,
    private val player: PlayerRepository,
    private val inventory: InventoryRepository,
    private val monsters: MonsterRepository,
    private val quests: QuestRepository,
    private val startQuest: StartQuestUseCase,
    private val questTracker: TrackQuestEventUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DialogueUiState())
    val state: StateFlow<DialogueUiState> = _state.asStateFlow()

    private var tree: DialogueTree? = null

    fun start(npcId: String) {
        viewModelScope.launch {
            content.ensureLoaded()
            questTracker(QuestRules.GameEvent.TalkedToNpc(npcId))
            val npc = content.npc(npcId) ?: run { finish(); return@launch }
            // The highest-priority tree whose conditions hold wins.
            val candidates = npc.dialogueTreeIds.mapNotNull { content.dialogueTree(it) }
            val chosen = candidates
                .filter { evaluateAll(it.conditions) }
                .maxByOrNull { it.priority }
                ?: candidates.firstOrNull()
                ?: run { finish(); return@launch }
            tree = chosen
            show(chosen.entryNodeId)
        }
    }

    private fun show(nodeId: String?) {
        val currentTree = tree ?: return finish()
        if (nodeId == null) return finish()
        viewModelScope.launch {
            // Nodes may be conditional; pick the first matching variant.
            val candidates = currentTree.nodes.filter { it.id == nodeId || it.id.startsWith("${nodeId}_") }
            val node = candidates.firstOrNull { evaluateAll(it.conditions) }
                ?: currentTree.node(nodeId)
                ?: return@launch finish()
            node.actions.forEach { apply(it) }
            val visibleChoices = node.choices.filter { evaluateAll(it.conditions) }
            _state.value = _state.value.copy(node = node, choices = visibleChoices)
        }
    }

    fun advance() {
        val node = _state.value.node ?: return finish()
        show(node.nextNodeId)
    }

    fun choose(choice: DialogueChoice) {
        viewModelScope.launch {
            choice.actions.forEach { apply(it) }
            if (choice.moralWeight != 0) {
                val profile = player.profile()
                player.update(profile.copy(moralScore = profile.moralScore + choice.moralWeight))
            }
            show(choice.nextNodeId)
        }
    }

    fun consumeShop() = _state.update { it.copy(openShopId = null) }
    fun consumeBattle() = _state.update { it.copy(startBattleId = null) }

    private fun finish() {
        _state.value = _state.value.copy(finished = true)
    }

    private suspend fun apply(action: DialogueAction) {
        when (action) {
            is DialogueAction.GrantFlag -> player.grantFlag(action.flag)
            is DialogueAction.ClearFlag -> player.clearFlag(action.flag)
            is DialogueAction.StartQuest -> startQuest(action.questId, System.currentTimeMillis())
            is DialogueAction.CompleteObjective -> {
                val progress = quests.progressFor(action.questId) ?: return
                val counts = progress.objectiveCounts.toMutableMap()
                counts[action.objectiveId] = (counts[action.objectiveId] ?: 0) + action.amount
                quests.upsert(progress.copy(objectiveCounts = counts))
            }
            is DialogueAction.GiveItem -> inventory.add(action.itemId, action.count)
            is DialogueAction.TakeItem -> inventory.remove(action.itemId, action.count)
            is DialogueAction.GiveGold -> player.addGold(action.amount)
            is DialogueAction.TakeGold -> player.spendGold(action.amount)
            is DialogueAction.ChangeReputation -> player.changeReputation(action.factionId, action.amount)
            is DialogueAction.OpenShop -> _state.update { it.copy(openShopId = action.shopId) }
            is DialogueAction.StartBattle -> _state.update { it.copy(startBattleId = action.encounterId) }
            is DialogueAction.HealParty -> monsters.healParty()
            is DialogueAction.ChangeMoralScore -> {
                val profile = player.profile()
                player.update(profile.copy(moralScore = profile.moralScore + action.amount))
            }
            is DialogueAction.GiveMonster -> Unit // Handled by quest rewards.
            is DialogueAction.UnlockFastTravel -> Unit
            is DialogueAction.PlayCutscene -> Unit
            is DialogueAction.EndDialogue -> finish()
        }
    }

    private suspend fun evaluateAll(conditions: List<DialogueCondition>): Boolean =
        conditions.all { evaluate(it) }

    private suspend fun evaluate(condition: DialogueCondition): Boolean = when (condition) {
        is DialogueCondition.HasFlag -> condition.flag in player.storyFlags()
        is DialogueCondition.MissingFlag -> condition.flag !in player.storyFlags()
        is DialogueCondition.QuestInState ->
            quests.progressFor(condition.questId)?.state == condition.state
        is DialogueCondition.HasItem -> inventory.countOf(condition.itemId) >= condition.count
        is DialogueCondition.HasGold -> player.profile().gold >= condition.amount
        is DialogueCondition.MinLevel -> player.profile().level >= condition.level
        is DialogueCondition.TimeOfDay -> condition.phase == DayPhase.forHour(12)
        is DialogueCondition.MinReputation -> true
        is DialogueCondition.MaxReputation -> true
        is DialogueCondition.HasSpecies ->
            monsters.party().any { it.species.id == condition.speciesId }
        is DialogueCondition.PartySize -> monsters.party().size in condition.min..condition.max
        is DialogueCondition.BestiaryCount -> true
        is DialogueCondition.ChapterAtLeast -> player.currentChapter() >= condition.chapter
    }
}

private fun MutableStateFlow<DialogueUiState>.update(transform: (DialogueUiState) -> DialogueUiState) {
    value = transform(value)
}
