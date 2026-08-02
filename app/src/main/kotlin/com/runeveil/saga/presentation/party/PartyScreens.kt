package com.runeveil.saga.presentation.party

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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.model.monster.Stat
import com.runeveil.saga.domain.model.monster.TalentNode
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.MonsterRepository
import com.runeveil.saga.domain.repository.PlayerRepository
import com.runeveil.saga.domain.rules.ProgressionRules
import com.runeveil.saga.ui.components.ElementBadge
import com.runeveil.saga.ui.components.EmptyState
import com.runeveil.saga.ui.components.ExperienceBar
import com.runeveil.saga.ui.components.HealthBar
import com.runeveil.saga.ui.components.RarityDot
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicOutlinedButton
import com.runeveil.saga.ui.components.SectionHeader
import com.runeveil.saga.ui.components.StatRow
import com.runeveil.saga.ui.components.contentText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Party and vault management. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyScreen(
    onBack: () -> Unit,
    onOpenMonster: (String) -> Unit,
    viewModel: PartyViewModel = hiltViewModel(),
) {
    val party by viewModel.party.collectAsStateWithLifecycle()
    val vault by viewModel.vault.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.party_title)) },
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
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) {
                    Text(stringResource(R.string.party_title), Modifier.padding(14.dp))
                }
                Tab(selected = tab == 1, onClick = { tab = 1 }) {
                    Text(stringResource(R.string.world_vault), Modifier.padding(14.dp))
                }
            }
            val monsters = if (tab == 0) party else vault
            if (monsters.isEmpty()) {
                EmptyState(stringResource(R.string.common_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(monsters, key = { it.uid }) { monster ->
                        MonsterRow(
                            monster = monster,
                            actionLabel = if (tab == 0) {
                                stringResource(R.string.party_to_vault)
                            } else {
                                stringResource(R.string.party_to_party)
                            },
                            onAction = {
                                if (tab == 0) viewModel.toVault(monster.uid) else viewModel.toParty(monster.uid)
                            },
                            onOpen = { onOpenMonster(monster.uid) },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MonsterRow(
    monster: MonsterInstance,
    actionLabel: String,
    onAction: () -> Unit,
    onOpen: () -> Unit,
) {
    RunePanel(Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RarityDot(monster.species.rarity)
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = "  " + contentText(monster.displayNameKey) +
                            if (monster.isShiny) " ✦" else "",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.common_level, monster.level),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                monster.species.elements.forEach { ElementBadge(it) }
            }
            Spacer(Modifier.height(8.dp))
            if (monster.isEgg) {
                Text(
                    text = stringResource(R.string.party_egg, monster.eggHatchStepsRemaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                HealthBar(monster.currentHp, monster.maxHp, showNumbers = true)
                Spacer(Modifier.height(4.dp))
                ExperienceBar(monster.levelProgress)
            }
            Spacer(Modifier.height(8.dp))
            RunicOutlinedButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Full detail view: stats, moves, talents and runes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonsterDetailScreen(
    uid: String,
    onBack: () -> Unit,
    viewModel: MonsterDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(uid) { viewModel.load(uid) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.monster?.displayNameKey?.let { contentText(it) }.orEmpty())
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
        val monster = state.monster
        if (monster == null) {
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
                        Text(
                            contentText(monster.species.loreKey),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        HealthBar(monster.currentHp, monster.maxHp)
                        Spacer(Modifier.height(6.dp))
                        ExperienceBar(monster.levelProgress)
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.party_summary)) }
            item {
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Stat.entries.forEach { stat ->
                            StatRow(
                                label = contentText(stat.displayKey),
                                value = monster.stats[stat].toString(),
                                highlight = stat == monster.temperament.raises,
                            )
                        }
                        StatRow("Freundschaft", monster.friendship.toString())
                        StatRow("Temperament", contentText(monster.temperament.displayKey))
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.party_moves)) }
            items(state.moves) { move ->
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(contentText(move.nameKey), style = MaterialTheme.typography.titleMedium)
                            ElementBadge(move.element)
                        }
                        Text(
                            contentText(move.descriptionKey),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${move.power} · ${move.accuracy}% · AP ${move.maxPp}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            item {
                SectionHeader(
                    stringResource(R.string.party_talent_points, monster.availableTalentPoints),
                )
            }
            items(state.talents) { node ->
                val unlocked = node.id in monster.unlockedTalentIds
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(contentText(node.nameKey), style = MaterialTheme.typography.titleMedium)
                        Text(
                            contentText(node.descriptionKey),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        RunicOutlinedButton(
                            text = if (unlocked) "✔" else "${node.cost} P",
                            onClick = { viewModel.unlockTalent(node) },
                            enabled = !unlocked && monster.availableTalentPoints >= node.cost,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------

@HiltViewModel
class PartyViewModel @Inject constructor(
    private val monsters: MonsterRepository,
) : ViewModel() {

    val party: StateFlow<List<MonsterInstance>> = monsters.observeParty()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val vault: StateFlow<List<MonsterInstance>> = monsters.observeVault()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toVault(uid: String) = viewModelScope.launch { monsters.moveToVault(uid) }
    fun toParty(uid: String) = viewModelScope.launch { monsters.moveToParty(uid) }
    fun release(uid: String) = viewModelScope.launch { monsters.release(uid) }
}

data class MonsterDetailUiState(
    val monster: MonsterInstance? = null,
    val moves: List<com.runeveil.saga.domain.model.battle.Move> = emptyList(),
    val talents: List<TalentNode> = emptyList(),
)

@HiltViewModel
class MonsterDetailViewModel @Inject constructor(
    private val monsters: MonsterRepository,
    private val content: ContentRepository,
    private val player: PlayerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MonsterDetailUiState())
    val state: StateFlow<MonsterDetailUiState> = _state.asStateFlow()

    fun load(uid: String) {
        viewModelScope.launch {
            val monster = monsters.byUid(uid) ?: return@launch
            _state.value = MonsterDetailUiState(
                monster = monster,
                moves = monster.moves.mapNotNull { content.move(it.moveId) },
                talents = monster.species.talentTreeId?.let { content.talentTree(it) }.orEmpty(),
            )
        }
    }

    fun unlockTalent(node: TalentNode) {
        val monster = _state.value.monster ?: return
        viewModelScope.launch {
            val tree = monster.species.talentTreeId?.let { content.talentTree(it) }.orEmpty()
            val updated = ProgressionRules.unlockTalent(monster, node, tree) ?: return@launch
            monsters.update(updated)
            _state.value = _state.value.copy(monster = updated)
        }
    }

    /** Binds a rune, respecting the capacity granted by the player's level. */
    fun bindRune(runeItemId: String) {
        val monster = _state.value.monster ?: return
        viewModelScope.launch {
            val capacity = player.profile().runeCapacity
            val updated = ProgressionRules.bindRune(monster, runeItemId, capacity) ?: return@launch
            monsters.update(updated)
            _state.value = _state.value.copy(monster = updated)
        }
    }
}
