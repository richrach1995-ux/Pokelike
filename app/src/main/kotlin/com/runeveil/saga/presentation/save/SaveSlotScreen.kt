package com.runeveil.saga.presentation.save

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.runeveil.saga.R
import com.runeveil.saga.domain.model.save.SaveSlotSummary
import com.runeveil.saga.domain.repository.SaveRepository
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

/**
 * Save-slot browser. The same screen serves loading and saving; [mode]
 * decides which action a tap performs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveSlotScreen(
    mode: String,
    onBack: () -> Unit,
    onLoaded: () -> Unit,
    viewModel: SaveViewModel = hiltViewModel(),
) {
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    var pendingOverwrite by remember { mutableStateOf<Int?>(null) }
    var pendingDelete by remember { mutableStateOf<Int?>(null) }
    val saving = mode == "save"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (saving) R.string.common_save else R.string.title_load),
                    )
                },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader(stringResource(R.string.title_load)) }
            items(slots, key = { it.slotIndex }) { slot ->
                SaveSlotCard(
                    slot = slot,
                    onClick = {
                        when {
                            saving && slot.exists -> pendingOverwrite = slot.slotIndex
                            saving -> viewModel.save(slot.slotIndex)
                            slot.exists -> viewModel.load(slot.slotIndex, onLoaded)
                        }
                    },
                    onDelete = { pendingDelete = slot.slotIndex },
                )
            }
            if (slots.none { it.exists }) {
                item { EmptyState(stringResource(R.string.save_empty_slot)) }
            }
        }
    }

    pendingOverwrite?.let { index ->
        AlertDialog(
            onDismissRequest = { pendingOverwrite = null },
            title = { Text(stringResource(R.string.save_overwrite_title)) },
            text = { Text(stringResource(R.string.save_overwrite_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.save(index)
                    pendingOverwrite = null
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverwrite = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    pendingDelete?.let { index ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.save_delete_title)) },
            text = { Text(stringResource(R.string.save_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(index)
                    pendingDelete = null
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun SaveSlotCard(
    slot: SaveSlotSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    RunePanel(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (slot.isAutosave && slot.slotIndex == 0) {
                        stringResource(R.string.save_autosave)
                    } else {
                        stringResource(R.string.save_slot, slot.slotIndex)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (slot.exists) {
                    Text(
                        text = stringResource(
                            R.string.save_playtime,
                            slot.playtimeSeconds / 3600,
                            (slot.playtimeSeconds % 3600) / 60,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (!slot.exists) {
                Text(
                    stringResource(R.string.save_empty_slot),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "${slot.playerName} · ${stringResource(R.string.common_level, slot.level)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (slot.chapterTitleKey.isNotEmpty()) {
                    Text(
                        text = contentText(slot.chapterTitleKey),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.save_completion, slot.completionPercent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (slot.newGamePlusCount > 0) {
                    Text(
                        text = "NG+${slot.newGamePlusCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                RunicOutlinedButton(
                    text = stringResource(R.string.common_delete),
                    onClick = onDelete,
                )
            }
        }
    }
}

@HiltViewModel
class SaveViewModel @Inject constructor(
    private val saves: SaveRepository,
) : ViewModel() {

    private val _slots = MutableStateFlow<List<SaveSlotSummary>>(emptyList())
    val slots: StateFlow<List<SaveSlotSummary>> = _slots.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch { _slots.value = saves.slots() }
    }

    fun save(slotIndex: Int) {
        viewModelScope.launch {
            saves.save(slotIndex, isAutosave = false)
            refresh()
        }
    }

    fun load(slotIndex: Int, onLoaded: () -> Unit) {
        viewModelScope.launch {
            saves.load(slotIndex).onSuccess { onLoaded() }
        }
    }

    fun delete(slotIndex: Int) {
        viewModelScope.launch {
            saves.delete(slotIndex)
            refresh()
        }
    }
}
