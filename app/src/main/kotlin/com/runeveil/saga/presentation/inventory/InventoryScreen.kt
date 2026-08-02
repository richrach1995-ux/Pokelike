package com.runeveil.saga.presentation.inventory

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.item.ItemCategory
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.InventoryRepository
import com.runeveil.saga.domain.repository.MonsterRepository
import com.runeveil.saga.domain.usecase.UseItemUseCase
import com.runeveil.saga.ui.components.EmptyState
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicOutlinedButton
import com.runeveil.saga.ui.components.contentText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The satchel, grouped by item category. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inventory_title)) },
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ItemCategory.entries.sortedBy { it.sortOrder }.take(5).forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(contentText(category.displayKey)) },
                    )
                }
            }
            val visible = state.stacks.filter { it.item.category == state.category }
            if (visible.isEmpty()) {
                EmptyState(stringResource(R.string.inventory_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(visible, key = { it.item.id }) { entry ->
                        RunePanel(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        contentText(entry.item.nameKey),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        stringResource(R.string.inventory_count, entry.quantity),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                Text(
                                    contentText(entry.item.descriptionKey),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (entry.item.usableOnMonster && entry.item.effects.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    RunicOutlinedButton(
                                        text = stringResource(R.string.inventory_use),
                                        onClick = { viewModel.useOnFirstPartyMember(entry.item.id) },
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

data class InventoryEntryUi(val item: Item, val quantity: Int)

data class InventoryUiState(
    val stacks: List<InventoryEntryUi> = emptyList(),
    val category: ItemCategory = ItemCategory.HEALING,
    val message: String? = null,
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventory: InventoryRepository,
    private val content: ContentRepository,
    private val monsters: MonsterRepository,
    private val useItem: UseItemUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryUiState())
    val state: StateFlow<InventoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            refresh()
        }
    }

    private suspend fun refresh() {
        val entries = inventory.stacks().mapNotNull { stack ->
            content.item(stack.itemId)?.let { InventoryEntryUi(it, stack.quantity) }
        }.sortedBy { it.item.category.sortOrder }
        _state.update { it.copy(stacks = entries) }
    }

    fun selectCategory(category: ItemCategory) = _state.update { it.copy(category = category) }

    /** Convenience action: applies a healing item to the first hurt member. */
    fun useOnFirstPartyMember(itemId: String) {
        viewModelScope.launch {
            val target = monsters.party().firstOrNull { it.currentHp < it.maxHp || it.status != null }
                ?: monsters.party().firstOrNull()
            useItem(
                itemId = itemId,
                targetUid = target?.uid,
                dayPhase = DayPhase.forHour(12),
                locationId = null,
            )
            refresh()
        }
    }
}
