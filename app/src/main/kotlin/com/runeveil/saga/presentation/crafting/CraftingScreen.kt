package com.runeveil.saga.presentation.crafting

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.runeveil.saga.R
import com.runeveil.saga.audio.AudioEngine
import com.runeveil.saga.audio.AudioKeys
import com.runeveil.saga.domain.model.item.CraftingStation
import com.runeveil.saga.domain.model.item.Recipe
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.InventoryRepository
import com.runeveil.saga.domain.usecase.CraftUseCase
import com.runeveil.saga.ui.components.EmptyState
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicOutlinedButton
import com.runeveil.saga.ui.components.contentText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Forge, alchemy table, rune loom and altar share this screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftingScreen(
    station: String,
    onBack: () -> Unit,
    viewModel: CraftingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(station) { viewModel.load(station) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.craft_title)) },
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
        if (state.recipes.isEmpty()) {
            EmptyState(stringResource(R.string.common_loading), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.message?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            items(state.recipes, key = { it.recipe.id }) { entry ->
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                contentText(entry.recipe.nameKey),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (entry.recipe.canFail) {
                                Text(
                                    stringResource(
                                        R.string.craft_chance,
                                        (entry.recipe.successChance * 100).toInt(),
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        entry.ingredients.forEach { (name, have, need) ->
                            Text(
                                text = "· $name  $have/$need",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (have >= need) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                        if (entry.recipe.goldCost > 0) {
                            Text(
                                stringResource(R.string.common_gold, entry.recipe.goldCost),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        RunicOutlinedButton(
                            text = stringResource(R.string.craft_make),
                            onClick = { viewModel.craft(entry.recipe.id) },
                            enabled = entry.craftable,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

data class RecipeUi(
    val recipe: Recipe,
    val ingredients: List<Triple<String, Int, Int>>,
    val craftable: Boolean,
)

data class CraftingUiState(
    val recipes: List<RecipeUi> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class CraftingViewModel @Inject constructor(
    private val content: ContentRepository,
    private val inventory: InventoryRepository,
    private val craftUseCase: CraftUseCase,
    private val audio: AudioEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(CraftingUiState())
    val state: StateFlow<CraftingUiState> = _state.asStateFlow()

    private var station: CraftingStation = CraftingStation.FORGE

    fun load(stationName: String) {
        station = CraftingStation.entries.firstOrNull { it.name == stationName } ?: CraftingStation.FORGE
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            content.ensureLoaded()
            val recipes = content.allRecipes().filter { it.station == station }
            val entries = recipes.map { recipe ->
                val ingredients = recipe.ingredients.map { (itemId, need) ->
                    val name = content.item(itemId)?.nameKey ?: itemId
                    Triple(name, inventory.countOf(itemId), need)
                }
                RecipeUi(recipe, ingredients, ingredients.all { it.second >= it.third })
            }
            _state.value = _state.value.copy(recipes = entries)
        }
    }

    fun craft(recipeId: String) {
        viewModelScope.launch {
            when (val result = craftUseCase(recipeId)) {
                is CraftUseCase.Result.Success -> {
                    audio.playSound(AudioKeys.CRAFT)
                    _state.value = _state.value.copy(
                        message = content.item(result.itemId)?.nameKey ?: result.itemId,
                    )
                }
                is CraftUseCase.Result.Failed -> {
                    audio.playSound(AudioKeys.UI_ERROR)
                    _state.value = _state.value.copy(message = "craft_failed")
                }
                else -> {
                    audio.playSound(AudioKeys.UI_ERROR)
                    _state.value = _state.value.copy(message = "craft_missing")
                }
            }
            refresh()
        }
    }
}
