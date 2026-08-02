package com.runeveil.saga.presentation.bestiary

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
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.repository.BestiaryEntry
import com.runeveil.saga.domain.repository.BestiaryRepository
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.rules.TypeChart
import com.runeveil.saga.ui.components.ElementBadge
import com.runeveil.saga.ui.components.EmptyState
import com.runeveil.saga.ui.components.RarityDot
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.SectionHeader
import com.runeveil.saga.ui.components.StatRow
import com.runeveil.saga.ui.components.contentText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The Bestiarium: every species, with seen/caught state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryScreen(
    onBack: () -> Unit,
    onOpenSpecies: (String) -> Unit,
    viewModel: BestiaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bestiary_title)) },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.bestiary_progress, state.caught, state.total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(state.entries, key = { it.species.id }) { row ->
                BestiaryRow(row) { onOpenSpecies(row.species.id) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BestiaryRow(row: BestiaryRowUi, onClick: () -> Unit) {
    RunePanel(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "#%03d  %s".format(
                        row.species.dexNumber,
                        if (row.seen) contentText(row.species.nameKey)
                        else stringResource(R.string.bestiary_unknown),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (row.seen) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.species.elements.forEach { ElementBadge(it) }
                    }
                }
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                RarityDot(row.species.rarity)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        row.caught -> stringResource(R.string.bestiary_caught)
                        row.seen -> stringResource(R.string.bestiary_seen)
                        else -> "—"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (row.shiny) {
                    Text("✦", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** One species in full: lore, stats, matchups, evolutions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryDetailScreen(
    speciesId: String,
    onBack: () -> Unit,
    viewModel: BestiaryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(speciesId) { viewModel.load(speciesId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.species?.nameKey?.let { contentText(it) }.orEmpty()) },
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
        val species = state.species
        if (species == null) {
            EmptyState(stringResource(R.string.common_loading), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            species.elements.forEach { ElementBadge(it) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(contentText(species.descriptionKey), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            contentText(species.loreKey),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.party_summary)) }
            item {
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        com.runeveil.saga.domain.model.monster.Stat.entries.forEach { stat ->
                            StatRow(contentText(stat.displayKey), species.baseStats[stat].toString())
                        }
                        StatRow("Größe", "${species.heightCm} cm")
                        StatRow("Gewicht", "${species.weightHg / 10.0} kg")
                        StatRow("Fangrate", species.catchRate.toString())
                    }
                }
            }
            item { SectionHeader("Schwächen") }
            item {
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        val profile = TypeChart.defensiveProfile(
                            species.primaryElement, species.secondaryElement,
                        )
                        profile.filterValues { it != 1.0 }
                            .entries.sortedByDescending { it.value }
                            .forEach { (element, multiplier) ->
                                StatRow(
                                    label = contentText(element.displayKey),
                                    value = "×$multiplier",
                                    highlight = multiplier > 1.0,
                                )
                            }
                    }
                }
            }
            if (state.evolutions.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.bestiary_evolutions)) }
                items(state.evolutions) { (target, descriptionKey) ->
                    RunePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(contentText(target.nameKey), style = MaterialTheme.typography.titleMedium)
                            Text(
                                contentText(descriptionKey),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------

data class BestiaryRowUi(
    val species: MonsterSpecies,
    val seen: Boolean,
    val caught: Boolean,
    val shiny: Boolean,
)

data class BestiaryUiState(
    val entries: List<BestiaryRowUi> = emptyList(),
    val caught: Int = 0,
    val total: Int = 0,
)

@HiltViewModel
class BestiaryViewModel @Inject constructor(
    private val content: ContentRepository,
    private val bestiary: BestiaryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BestiaryUiState())
    val state: StateFlow<BestiaryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            val species = content.allSpecies()
            val known: Map<String, BestiaryEntry> = species.mapNotNull { bestiary.entry(it.id) }
                .associateBy { it.speciesId }
            _state.value = BestiaryUiState(
                entries = species.map { entry ->
                    val record = known[entry.id]
                    BestiaryRowUi(
                        species = entry,
                        seen = record?.seen == true,
                        caught = record?.caught == true,
                        shiny = record?.shinyCaught == true,
                    )
                },
                caught = known.values.count { it.caught },
                total = species.size,
            )
        }
    }
}

data class BestiaryDetailUiState(
    val species: MonsterSpecies? = null,
    val evolutions: List<Pair<MonsterSpecies, String>> = emptyList(),
)

@HiltViewModel
class BestiaryDetailViewModel @Inject constructor(
    private val content: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BestiaryDetailUiState())
    val state: StateFlow<BestiaryDetailUiState> = _state.asStateFlow()

    fun load(speciesId: String) {
        viewModelScope.launch {
            content.ensureLoaded()
            val species = content.species(speciesId) ?: return@launch
            _state.value = BestiaryDetailUiState(
                species = species,
                evolutions = species.evolutions.mapNotNull { path ->
                    content.species(path.targetSpeciesId)?.let { it to path.descriptionKey }
                },
            )
        }
    }
}
