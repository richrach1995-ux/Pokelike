package com.runeveil.saga.presentation.quest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.runeveil.saga.R
import com.runeveil.saga.domain.model.story.Quest
import com.runeveil.saga.domain.model.story.QuestProgress
import com.runeveil.saga.domain.model.story.QuestState
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.QuestRepository
import com.runeveil.saga.domain.usecase.CompleteQuestUseCase
import com.runeveil.saga.ui.components.EmptyState
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicOutlinedButton
import com.runeveil.saga.ui.components.SectionHeader
import com.runeveil.saga.ui.components.contentText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The quest log, grouped by state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestLogScreen(
    onBack: () -> Unit,
    viewModel: QuestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quest_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.entries.isEmpty()) {
            EmptyState(stringResource(R.string.quest_none), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val grouped = state.entries.groupBy { it.progress.state }
            listOf(
                QuestState.READY_TO_TURN_IN to R.string.quest_turn_in,
                QuestState.ACTIVE to R.string.quest_active,
                QuestState.COMPLETED to R.string.quest_completed,
            ).forEach { (questState, titleRes) ->
                val bucket = grouped[questState].orEmpty()
                if (bucket.isNotEmpty()) {
                    item { SectionHeader(stringResource(titleRes)) }
                    items(bucket, key = { it.quest.id }) { entry ->
                        QuestCard(entry, onTurnIn = { viewModel.turnIn(entry.quest.id) })
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun QuestCard(entry: QuestEntryUi, onTurnIn: () -> Unit) {
    RunePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(contentText(entry.quest.nameKey), style = MaterialTheme.typography.titleMedium)
                Text(
                    contentText(entry.quest.category.displayKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                contentText(entry.quest.summaryKey),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            entry.quest.objectives.forEach { objective ->
                val done = entry.progress.countFor(objective.id)
                Text(
                    text = "· ${contentText(objective.descriptionKey)} ($done/${objective.requiredCount})",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { entry.progress.progress(entry.quest) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (entry.progress.state == QuestState.READY_TO_TURN_IN) {
                Spacer(Modifier.height(10.dp))
                RunicOutlinedButton(text = stringResource(R.string.quest_turn_in), onClick = onTurnIn)
            }
        }
    }
}

data class QuestEntryUi(val quest: Quest, val progress: QuestProgress)
data class QuestUiState(val entries: List<QuestEntryUi> = emptyList())

@HiltViewModel
class QuestViewModel @Inject constructor(
    private val quests: QuestRepository,
    private val content: ContentRepository,
    private val completeQuest: CompleteQuestUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(QuestUiState())
    val state: StateFlow<QuestUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            content.ensureLoaded()
            val entries = content.allQuests().mapNotNull { quest ->
                quests.progressFor(quest.id)
                    ?.takeIf { it.state != QuestState.AVAILABLE }
                    ?.let { QuestEntryUi(quest, it) }
            }
            _state.value = QuestUiState(entries)
        }
    }

    fun turnIn(questId: String) {
        viewModelScope.launch {
            completeQuest(questId, System.currentTimeMillis())
            refresh()
        }
    }
}
