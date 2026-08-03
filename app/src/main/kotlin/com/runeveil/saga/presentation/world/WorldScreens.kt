package com.runeveil.saga.presentation.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.runeveil.saga.audio.AudioEngine
import com.runeveil.saga.audio.AudioKeys
import com.runeveil.saga.domain.model.battle.BattleWeather
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.world.Location
import com.runeveil.saga.domain.model.world.LocationType
import com.runeveil.saga.domain.model.world.Region
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.MonsterRepository
import com.runeveil.saga.domain.repository.PlayerRepository
import com.runeveil.saga.domain.repository.WorldStateRepository
import com.runeveil.saga.domain.rules.QuestRules
import com.runeveil.saga.domain.usecase.AdvanceEggsUseCase
import com.runeveil.saga.domain.usecase.AdvanceWorldClockUseCase
import com.runeveil.saga.domain.rules.BattleReadiness
import com.runeveil.saga.domain.rules.BattleReadinessRules
import com.runeveil.saga.domain.usecase.RollEncounterUseCase
import com.runeveil.saga.domain.usecase.TrackQuestEventUseCase
import com.runeveil.saga.ui.components.EmptyState
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicButton
import com.runeveil.saga.ui.components.RunicOutlinedButton
import com.runeveil.saga.ui.components.SectionHeader
import com.runeveil.saga.ui.components.contentText
import com.runeveil.saga.ui.components.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The world map: the nine regions as a vertical list of gates, each showing
 * its level band, weather and lock state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldMapScreen(
    onOpenLocation: (String) -> Unit,
    onOpenParty: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenBestiary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSaves: () -> Unit,
    viewModel: WorldViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.currentRegionId) { viewModel.onRegionShown() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.world_map)) },
                actions = {
                    IconButton(onClick = onOpenSaves) {
                        Icon(Icons.Filled.Done, contentDescription = stringResource(R.string.common_save))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.world_map)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenParty,
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text(stringResource(R.string.party_title)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenInventory,
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    label = { Text(stringResource(R.string.inventory_title)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenQuests,
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.quest_title)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenBestiary,
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text(stringResource(R.string.bestiary_title)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PlayerBanner(
                    name = state.playerName,
                    gold = state.gold,
                    phase = state.dayPhase,
                    weather = state.weather,
                )
            }
            items(state.regions, key = { it.id }) { region ->
                RegionCard(
                    region = region,
                    unlocked = region.id in state.unlockedRegionIds,
                    isCurrent = region.id == state.currentRegionId,
                    onOpen = { locationId -> onOpenLocation(locationId) },
                )
            }
        }
    }
}

@Composable
private fun PlayerBanner(name: String, gold: Int, phase: DayPhase, weather: BattleWeather) {
    RunePanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.common_gold, gold),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(contentText(phase.displayKey), style = MaterialTheme.typography.labelSmall)
                Text(
                    text = contentText(weather.displayKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun RegionCard(
    region: Region,
    unlocked: Boolean,
    isCurrent: Boolean,
    onOpen: (String) -> Unit,
) {
    RunePanel(
        modifier = Modifier.fillMaxWidth(),
        accent = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(contentText(region.nameKey), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${region.recommendedLevelRange.first}–${region.recommendedLevelRange.last}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = contentText(region.loreKey),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            if (!unlocked) {
                Text(
                    text = stringResource(R.string.world_locked),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    region.locations.filter { it.type != LocationType.SECRET }.forEach { location ->
                        RunicOutlinedButton(
                            text = contentText(location.nameKey),
                            onClick = { onOpen(location.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single location: walking (which rolls encounters), services and NPCs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    locationId: String,
    onBack: () -> Unit,
    onEncounter: (String) -> Unit,
    onTrainerBattle: (String, String) -> Unit,
    onOpenShop: (String) -> Unit,
    onOpenForge: (String) -> Unit,
    onOpenRoost: () -> Unit,
    onTalkTo: (String) -> Unit,
    onCutscene: (String) -> Unit,
    viewModel: LocationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(locationId) { viewModel.load(locationId) }
    LaunchedEffect(state.pendingEncounterId) {
        state.pendingEncounterId?.let {
            viewModel.consumeEncounter()
            onEncounter(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.location?.nameKey?.let { contentText(it) }.orEmpty()) },
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
        val location = state.location
        if (location == null) {
            EmptyState(stringResource(R.string.common_loading), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            contentText(location.descriptionKey),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.world_steps, state.steps),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            (state.readiness as? BattleReadiness.Blocked)?.let { blocked ->
                item {
                    // Stated up front, not on tap: the player should know their
                    // team cannot fight before they walk into something.
                    RunePanel(Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(blocked.reason.messageRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            if (location.hasWildEncounters) {
                item {
                    RunicButton(
                        text = stringResource(R.string.world_explore),
                        onClick = viewModel::explore,
                        modifier = Modifier.fillMaxWidth(),
                        glyph = "ᚹ",
                    )
                }
            }

            item { ServicesSection(location, onOpenShop, onOpenForge, onOpenRoost, viewModel) }

            if (state.npcs.isNotEmpty()) {
                item { SectionHeader("NPCs") }
                items(state.npcs, key = { it.id }) { npc ->
                    RunePanel(Modifier.fillMaxWidth(), onClick = {
                        val teamId = npc.trainerTeamId
                        val challengeable = npc.isTrainer &&
                            teamId != null &&
                            npc.id !in state.defeatedTrainers
                        // A trainer whose team cannot be fought is simply
                        // talked to instead — the battle never begins. The
                        // banner above already says why.
                        if (challengeable && state.readiness.isReady) {
                            onTrainerBattle(teamId, npc.id)
                        } else {
                            onTalkTo(npc.id)
                        }
                    }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(contentText(npc.nameKey), style = MaterialTheme.typography.titleMedium)
                            npc.titleKey?.let {
                                Text(
                                    contentText(it),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ServicesSection(
    location: Location,
    onOpenShop: (String) -> Unit,
    onOpenForge: (String) -> Unit,
    onOpenRoost: () -> Unit,
    viewModel: LocationViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (location.healerAvailable) {
            RunicOutlinedButton(
                text = stringResource(R.string.world_rest),
                onClick = viewModel::rest,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        location.shopId?.let { shopId ->
            RunicOutlinedButton(
                text = stringResource(R.string.world_shop),
                onClick = { onOpenShop(shopId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (location.forgeAvailable) {
            RunicOutlinedButton(
                text = stringResource(R.string.world_forge),
                onClick = { onOpenForge("FORGE") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (location.roostAvailable) {
            RunicOutlinedButton(
                text = stringResource(R.string.world_roost),
                onClick = onOpenRoost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ViewModels
// ---------------------------------------------------------------------------

data class WorldUiState(
    val playerName: String = "",
    val gold: Int = 0,
    val regions: List<Region> = emptyList(),
    val unlockedRegionIds: Set<String> = emptySet(),
    val currentRegionId: String = "",
    val dayPhase: DayPhase = DayPhase.DAY,
    val weather: BattleWeather = BattleWeather.CLEAR,
)

@HiltViewModel
class WorldViewModel @Inject constructor(
    private val content: ContentRepository,
    private val player: PlayerRepository,
    private val worldState: WorldStateRepository,
    private val clock: AdvanceWorldClockUseCase,
    private val audio: AudioEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(WorldUiState())
    val state: StateFlow<WorldUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            refresh()
        }
    }

    private suspend fun refresh() {
        val profile = player.profile()
        val chapter = player.currentChapter()
        val regions = content.allRegions()
        val unlocked = regions.filterIndexed { index, _ -> index <= chapter }.map { it.id }.toSet()
        val minutes = worldState.weatherFor(profile.currentRegionId)
        _state.update {
            it.copy(
                playerName = profile.name,
                gold = profile.gold,
                regions = regions,
                unlockedRegionIds = unlocked + profile.currentRegionId,
                currentRegionId = profile.currentRegionId,
                weather = minutes.weather,
            )
        }
    }

    /** Plays the region theme and rolls the world clock forward. */
    fun onRegionShown() {
        viewModelScope.launch {
            clock(elapsedRealSeconds = 0, nowEpochMs = System.currentTimeMillis())
            val region = content.region(_state.value.currentRegionId) ?: return@launch
            val night = _state.value.dayPhase.isNight
            audio.playMusic(if (night) region.nightMusicKey ?: region.musicKey else region.musicKey)
        }
    }
}

data class LocationUiState(
    val location: Location? = null,
    val npcs: List<com.runeveil.saga.domain.model.npc.Npc> = emptyList(),
    val steps: Int = 0,
    val defeatedTrainers: Set<String> = emptySet(),
    val pendingEncounterId: String? = null,
    val message: String? = null,
    /**
     * Whether the party could fight *right now*. Kept in the state rather than
     * checked on tap so the screen can say so before the player tries.
     */
    val readiness: BattleReadiness = BattleReadiness.Ready,
)

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val content: ContentRepository,
    private val player: PlayerRepository,
    private val monsters: MonsterRepository,
    private val worldState: WorldStateRepository,
    private val rollEncounter: RollEncounterUseCase,
    private val advanceEggs: AdvanceEggsUseCase,
    private val questTracker: TrackQuestEventUseCase,
    private val audio: AudioEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    private var stepsSinceEncounter = 0

    fun load(locationId: String) {
        viewModelScope.launch {
            content.ensureLoaded()
            val location = content.locationOf(locationId) ?: return@launch
            worldState.discoverLocation(locationId)
            questTracker(QuestRules.GameEvent.ReachedLocation(locationId))
            _state.update {
                it.copy(
                    location = location,
                    npcs = content.npcsAt(locationId),
                    defeatedTrainers = worldState.defeatedTrainers(),
                    readiness = BattleReadinessRules.forParty(monsters.party()),
                )
            }
        }
    }

    /**
     * One "step": advances eggs and the clock, and may run into a wild monster.
     *
     * Walking stays possible with a beaten team — eggs still need steps, and
     * locking the player out of the location would be worse than the problem.
     * The *encounter roll* is what is skipped: a battle that cannot be fought
     * is never rolled, so nothing is generated and nothing is navigated to.
     */
    fun explore() {
        val location = _state.value.location ?: return
        viewModelScope.launch {
            audio.playSound(AudioKeys.STEP, pitch = 0.9f + (stepsSinceEncounter % 5) * 0.05f)
            stepsSinceEncounter += STEPS_PER_EXPLORE
            _state.update { it.copy(steps = it.steps + STEPS_PER_EXPLORE) }

            val hatched = advanceEggs(STEPS_PER_EXPLORE)
            if (hatched.isNotEmpty()) {
                audio.playSound(AudioKeys.EGG_HATCH)
                questTracker(QuestRules.GameEvent.HatchedEgg)
            }

            val readiness = BattleReadinessRules.forParty(monsters.party())
            _state.update { it.copy(readiness = readiness) }
            if (readiness is BattleReadiness.Blocked) return@launch

            val phase = DayPhase.forHour(((worldState.weatherFor(location.regionId).startedAtEpochMs / 3_600_000) % 24).toInt())
            val encounter = rollEncounter(
                location = location,
                stepsSinceLastEncounter = stepsSinceEncounter,
                storyFlags = player.storyFlags(),
                dayPhase = phase,
            )
            if (encounter != null) {
                stepsSinceEncounter = 0
                audio.playSound(AudioKeys.ENCOUNTER)
                _state.update { it.copy(pendingEncounterId = "wild:${encounter.species.id}:${encounter.level}") }
            }
        }
    }

    fun consumeEncounter() = _state.update { it.copy(pendingEncounterId = null) }

    fun rest() {
        viewModelScope.launch {
            monsters.healParty()
            audio.playSound(AudioKeys.HEAL)
            _state.update {
                it.copy(
                    message = "world_rested",
                    readiness = BattleReadinessRules.forParty(monsters.party()),
                )
            }
        }
    }

    private companion object {
        /** How many walked steps one tap of "Erkunden" represents. */
        const val STEPS_PER_EXPLORE = 8
    }
}
