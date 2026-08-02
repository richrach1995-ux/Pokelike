package com.runeveil.saga.presentation.battle

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.runeveil.saga.R
import com.runeveil.saga.audio.AudioEngine
import com.runeveil.saga.data.i18n.LocalizationRepository
import com.runeveil.saga.audio.AudioKeys
import com.runeveil.saga.domain.battle.AiProfile
import com.runeveil.saga.domain.battle.BattleContent
import com.runeveil.saga.domain.model.battle.BattleAction
import com.runeveil.saga.domain.model.battle.BattleEvent
import com.runeveil.saga.domain.model.battle.BattleOutcome
import com.runeveil.saga.domain.model.battle.BattleType
import com.runeveil.saga.domain.model.battle.Battler
import com.runeveil.saga.domain.model.monster.DayPhase
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.InventoryRepository
import com.runeveil.saga.domain.repository.MonsterRepository
import com.runeveil.saga.domain.repository.PlayerRepository
import com.runeveil.saga.domain.repository.SettingsRepository
import com.runeveil.saga.domain.repository.WorldStateRepository
import com.runeveil.saga.domain.rules.Effectiveness
import com.runeveil.saga.domain.usecase.ApplyBattleResultsUseCase
import com.runeveil.saga.domain.usecase.BattleSession
import com.runeveil.saga.domain.usecase.RegisterCaptureUseCase
import com.runeveil.saga.domain.util.Rng
import com.runeveil.saga.navigation.Destination
import com.runeveil.saga.ui.components.ElementBadge
import com.runeveil.saga.ui.components.HealthBar
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicButton
import com.runeveil.saga.ui.components.RunicOutlinedButton
import com.runeveil.saga.ui.components.contentText
import com.runeveil.saga.ui.theme.RuneGold
import com.runeveil.saga.ui.theme.RuneNight
import com.runeveil.saga.ui.theme.RuneNightSunken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The battle screen.
 *
 * The engine resolves a whole turn at once; this screen then *replays* the
 * resulting event list with pacing, so the player sees "Attacke → Treffer →
 * sehr effektiv → besiegt" instead of a single state jump.
 */
@Composable
fun BattleScreen(
    onFinished: () -> Unit,
    viewModel: BattleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val logState = rememberLazyListState()

    LaunchedEffect(state.log.size) {
        if (state.log.isNotEmpty()) logState.animateScrollToItem(state.log.lastIndex)
    }
    LaunchedEffect(state.finished) {
        if (state.finished) {
            delay(1400)
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RuneNightSunken, RuneNight))),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.battle_turn, state.turn),
                style = MaterialTheme.typography.labelSmall,
                color = RuneGold,
            )
            Spacer(Modifier.height(8.dp))

            state.enemy?.let { BattlerCard(it, isPlayer = false) }
            Spacer(Modifier.height(12.dp))

            RunePanel(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    state = logState,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.log) { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            state.player?.let { BattlerCard(it, isPlayer = true) }
            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = !state.busy && !state.finished) {
                BattleCommands(state = state, viewModel = viewModel)
            }

            if (state.finished) {
                Text(
                    text = when (state.outcome) {
                        BattleOutcome.VICTORY -> stringResource(R.string.battle_victory)
                        BattleOutcome.DEFEAT -> stringResource(R.string.battle_defeat)
                        BattleOutcome.CAPTURED -> stringResource(R.string.battle_captured)
                        BattleOutcome.FLED -> stringResource(R.string.battle_fled)
                        else -> ""
                    },
                    style = MaterialTheme.typography.displayMedium,
                    color = RuneGold,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BattlerCard(battler: Battler, isPlayer: Boolean) {
    RunePanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = contentText(battler.monster.displayNameKey),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.common_level, battler.monster.level),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    battler.monster.species.elements.forEach { ElementBadge(it) }
                }
            }
            Spacer(Modifier.height(8.dp))
            HealthBar(
                current = battler.monster.currentHp,
                max = battler.monster.maxHp,
                showNumbers = isPlayer,
                contentDescriptionText = null,
            )
            battler.monster.status?.let { status ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = contentText(status.shortKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (battler.shieldHp > 0) {
                Text(
                    text = "◈ ${battler.shieldHp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun BattleCommands(state: BattleUiState, viewModel: BattleViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state.menu) {
            BattleMenu.ROOT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RunicButton(
                        text = stringResource(R.string.battle_fight),
                        onClick = { viewModel.openMenu(BattleMenu.MOVES) },
                        modifier = Modifier.weight(1f),
                    )
                    RunicOutlinedButton(
                        text = stringResource(R.string.battle_orb),
                        onClick = { viewModel.openMenu(BattleMenu.ORBS) },
                        modifier = Modifier.weight(1f),
                        enabled = state.canCapture,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RunicOutlinedButton(
                        text = stringResource(R.string.battle_switch),
                        onClick = { viewModel.openMenu(BattleMenu.SWITCH) },
                        modifier = Modifier.weight(1f),
                        enabled = state.benchSize > 0,
                    )
                    RunicOutlinedButton(
                        text = stringResource(R.string.battle_flee),
                        onClick = viewModel::flee,
                        modifier = Modifier.weight(1f),
                        enabled = state.canFlee,
                    )
                }
            }

            BattleMenu.MOVES -> {
                state.moves.forEachIndexed { index, move ->
                    RunicOutlinedButton(
                        text = "${contentText(move.nameKey)}  ·  " +
                            stringResource(R.string.battle_pp, move.currentPp, move.maxPp),
                        onClick = { viewModel.useMove(index) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = move.currentPp > 0,
                    )
                }
                BackRow(viewModel)
            }

            BattleMenu.ORBS -> {
                state.orbs.forEach { orb ->
                    RunicOutlinedButton(
                        text = "${contentText(orb.nameKey)} ×${orb.count}",
                        onClick = { viewModel.throwOrb(orb.itemId) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                BackRow(viewModel)
            }

            BattleMenu.SWITCH -> {
                state.bench.forEachIndexed { index, monster ->
                    RunicOutlinedButton(
                        text = "${contentText(monster.displayNameKey)} · " +
                            "${monster.currentHp}/${monster.maxHp}",
                        onClick = { viewModel.switchTo(monster.uid) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !monster.isFainted,
                    )
                }
                BackRow(viewModel)
            }
        }
    }
}

@Composable
private fun BackRow(viewModel: BattleViewModel) {
    RunicOutlinedButton(
        text = stringResource(R.string.common_back),
        onClick = { viewModel.openMenu(BattleMenu.ROOT) },
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// State and ViewModel
// ---------------------------------------------------------------------------

enum class BattleMenu { ROOT, MOVES, ORBS, SWITCH }

data class MoveButton(val nameKey: String, val currentPp: Int, val maxPp: Int)
data class OrbButton(val itemId: String, val nameKey: String, val count: Int)

data class BattleUiState(
    val player: Battler? = null,
    val enemy: Battler? = null,
    val bench: List<MonsterInstance> = emptyList(),
    val moves: List<MoveButton> = emptyList(),
    val orbs: List<OrbButton> = emptyList(),
    val log: List<String> = emptyList(),
    val turn: Int = 1,
    val menu: BattleMenu = BattleMenu.ROOT,
    val busy: Boolean = true,
    val finished: Boolean = false,
    val outcome: BattleOutcome = BattleOutcome.ONGOING,
    val canCapture: Boolean = false,
    val canFlee: Boolean = false,
) {
    val benchSize: Int get() = bench.count { !it.isFainted }
}

@HiltViewModel
class BattleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val content: ContentRepository,
    private val battleContent: BattleContent,
    private val monsters: MonsterRepository,
    private val inventory: InventoryRepository,
    private val player: PlayerRepository,
    private val worldState: WorldStateRepository,
    private val settings: SettingsRepository,
    private val applyResults: ApplyBattleResultsUseCase,
    private val registerCapture: RegisterCaptureUseCase,
    private val audio: AudioEngine,
    private val localization: LocalizationRepository,
    private val rng: Rng,
) : ViewModel() {

    private val encounterId: String =
        savedStateHandle[Destination.Battle.ARG_ENCOUNTER] ?: ""
    private val trainerId: String? = savedStateHandle[Destination.Battle.ARG_TRAINER]

    private val _state = MutableStateFlow(BattleUiState())
    val state: StateFlow<BattleUiState> = _state.asStateFlow()

    private var session: BattleSession? = null
    private var animationSpeed = 1f

    init {
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        content.ensureLoaded()
        animationSpeed = settings.settings().battleAnimationSpeed
        val party = monsters.party().filter { !it.isEgg }
        if (party.isEmpty()) {
            _state.update { it.copy(finished = true, outcome = BattleOutcome.DEFEAT) }
            return
        }

        val profile = player.profile()
        val weather = worldState.weatherFor(profile.currentRegionId).weather
        val difficulty = settings.settings().difficulty

        val (enemies, type, aiProfile) = buildEnemies(difficulty.enemyLevelDelta)
        if (enemies.isEmpty()) {
            _state.update { it.copy(finished = true) }
            return
        }

        val created = BattleSession(
            content = battleContent,
            rng = rng,
            initialState = BattleSession.buildState(
                battleId = encounterId,
                type = type,
                party = party,
                enemies = enemies,
                weather = weather,
                dayPhase = DayPhase.forHour(12),
                rngSeed = System.currentTimeMillis(),
                locationId = profile.currentLocationId,
                enemyTrainerId = trainerId,
            ),
            enemyProfile = aiProfile,
        )
        session = created
        audio.pushBattleMusic(AudioKeys.battleMusic(type.musicKey))
        refreshFromSession()
        playEvents(created.openingEvents)
        _state.update { it.copy(busy = false) }
    }

    /** Decodes the navigation argument into concrete opponents. */
    private suspend fun buildEnemies(levelDelta: Int): Triple<List<MonsterInstance>, BattleType, AiProfile> {
        if (encounterId.startsWith(WILD_PREFIX)) {
            val parts = encounterId.removePrefix(WILD_PREFIX).split(":")
            val species = content.species(parts.getOrElse(0) { "" })
                ?: return Triple(emptyList(), BattleType.WILD, AiProfile.FERAL)
            val level = (parts.getOrNull(1)?.toIntOrNull() ?: 5) + levelDelta
            val wild = com.runeveil.saga.domain.rules.EncounterRules.generateTrained(
                species = species,
                uid = "wild-${rng.nextInt(Int.MAX_VALUE)}",
                level = level.coerceIn(1, 100),
                moveIds = species.movesAtLevel(level),
                abilityId = species.abilityIds.firstOrNull(),
                heldItemId = null,
                temperament = com.runeveil.saga.domain.model.monster.Temperament.EVEN,
                geneQuality = 12,
                isShiny = false,
                nickname = null,
                movePpResolver = { content.move(it)?.maxPp },
            )
            val profile = if (species.rarity.ordinal >=
                com.runeveil.saga.domain.model.monster.Rarity.LEGENDARY.ordinal
            ) {
                AiProfile.MYTHIC
            } else {
                AiProfile.INSTINCTIVE
            }
            val type = if (profile == AiProfile.MYTHIC) BattleType.LEGENDARY else BattleType.WILD
            return Triple(listOf(wild), type, profile)
        }

        val team = content.trainerTeam(encounterId) ?: return Triple(emptyList(), BattleType.TRAINER, AiProfile.TACTICAL)
        val members = team.members.mapNotNull { member ->
            val species = content.species(member.speciesId) ?: return@mapNotNull null
            com.runeveil.saga.domain.rules.EncounterRules.generateTrained(
                species = species,
                uid = "foe-${member.speciesId}-${rng.nextInt(Int.MAX_VALUE)}",
                level = (member.level + levelDelta).coerceIn(1, 100),
                moveIds = member.moveIds,
                abilityId = member.abilityId,
                heldItemId = member.heldItemId,
                temperament = com.runeveil.saga.domain.model.monster.Temperament.EVEN,
                geneQuality = member.geneQuality,
                isShiny = member.isShiny,
                nickname = member.nickname,
                movePpResolver = { content.move(it)?.maxPp },
            )
        }
        return Triple(members, BattleType.TRAINER, team.aiProfile)
    }

    fun openMenu(menu: BattleMenu) {
        _state.update { it.copy(menu = menu) }
    }

    fun useMove(slotIndex: Int) {
        val current = session ?: return
        val actor = current.state.activePlayers.firstOrNull() ?: return
        val target = current.state.activeEnemies.firstOrNull() ?: return
        submit(BattleAction.UseMove(actor.id, slotIndex, listOf(target.id)))
    }

    fun throwOrb(itemId: String) {
        val current = session ?: return
        val actor = current.state.activePlayers.firstOrNull() ?: return
        val target = current.state.activeEnemies.firstOrNull() ?: return
        audio.playSound(AudioKeys.ORB_THROW)
        submit(BattleAction.ThrowOrb(actor.id, itemId, target.id))
    }

    fun switchTo(uid: String) {
        val current = session ?: return
        val actor = current.state.activePlayers.firstOrNull() ?: return
        val index = current.state.playerTeam.indexOfFirst { it.monster.uid == uid }
        if (index < 0) return
        submit(BattleAction.Switch(actor.id, index))
    }

    fun flee() {
        val current = session ?: return
        val actor = current.state.activePlayers.firstOrNull() ?: return
        submit(BattleAction.Flee(actor.id))
    }

    private fun submit(action: BattleAction) {
        val current = session ?: return
        _state.update { it.copy(busy = true, menu = BattleMenu.ROOT) }
        viewModelScope.launch {
            val result = current.submit(action)
            refreshFromSession()
            playEvents(result.events)
            if (current.outcome != BattleOutcome.ONGOING) {
                finish()
            } else {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    /** Replays the engine's events with pacing, sound and log lines. */
    private suspend fun playEvents(events: List<BattleEvent>) {
        for (event in events) {
            val line = describe(event)
            if (line != null) _state.update { it.copy(log = it.log + line) }
            when (event) {
                is BattleEvent.DamageDealt -> {
                    audio.playSound(
                        when (event.effectiveness) {
                            Effectiveness.SUPER_EFFECTIVE, Effectiveness.DEVASTATING -> AudioKeys.HIT_SUPER
                            Effectiveness.RESISTED, Effectiveness.DOUBLE_RESISTED -> AudioKeys.HIT_WEAK
                            else -> AudioKeys.HIT_NEUTRAL
                        },
                    )
                    if (event.critical) audio.playSound(AudioKeys.CRITICAL)
                }
                is BattleEvent.Fainted -> audio.playSound(AudioKeys.FAINT)
                is BattleEvent.Healed -> audio.playSound(AudioKeys.HEAL)
                is BattleEvent.ShieldRaised -> audio.playSound(AudioKeys.SHIELD)
                is BattleEvent.StatusInflicted -> audio.playSound(AudioKeys.STATUS)
                is BattleEvent.OrbShake -> audio.playSound(AudioKeys.ORB_SHAKE)
                is BattleEvent.CaptureSucceeded -> audio.playSound(AudioKeys.ORB_CAUGHT)
                is BattleEvent.CaptureFailed -> audio.playSound(AudioKeys.ORB_BREAK)
                else -> Unit
            }
            refreshFromSession()
            delay((EVENT_DELAY_MS / animationSpeed.coerceAtLeast(0.25f)).toLong())
        }
    }

    /** Turns an engine event into a log line; null means "not worth showing". */
    private fun describe(event: BattleEvent): String? = when (event) {
        is BattleEvent.MoveDeclared -> "▸ ${resolve(event.moveNameKey)}"
        is BattleEvent.MoveMissed -> "… daneben!"
        is BattleEvent.MoveHadNoEffect -> "… ohne Wirkung."
        is BattleEvent.DamageDealt -> buildString {
            append("−${event.amount}")
            if (event.critical) append(" · Volltreffer!")
            when (event.effectiveness) {
                Effectiveness.SUPER_EFFECTIVE -> append(" · sehr effektiv")
                Effectiveness.DEVASTATING -> append(" · verheerend")
                Effectiveness.RESISTED -> append(" · kaum wirksam")
                Effectiveness.DOUBLE_RESISTED -> append(" · fast wirkungslos")
                Effectiveness.IMMUNE -> append(" · wirkungslos")
                Effectiveness.NEUTRAL -> Unit
            }
        }
        is BattleEvent.Healed -> "+${event.amount} KP"
        is BattleEvent.StatusInflicted -> resolve(event.condition.displayKey)
        is BattleEvent.StatusCured -> "${resolve(event.condition.displayKey)} geheilt"
        is BattleEvent.StatusTicked -> "${resolve(event.condition.displayKey)}: −${event.damage}"
        is BattleEvent.StatChanged -> if (event.applied > 0) "▲ ${resolve(event.stat.displayKey)}"
        else if (event.applied < 0) "▼ ${resolve(event.stat.displayKey)}" else null
        is BattleEvent.WeatherChanged -> resolve(event.weather.displayKey)
        is BattleEvent.WeatherTicked -> "Wetter: −${event.damage}"
        is BattleEvent.Fainted -> "${resolve(event.nameKey)} wurde besiegt."
        is BattleEvent.SwitchedIn -> "${resolve(event.nameKey)} betritt das Feld."
        is BattleEvent.ComboTriggered -> "Kombinationsschlag! +${event.bonusPercent} %"
        is BattleEvent.OrbShake -> "… ${event.shakeIndex}/${event.totalNeeded}"
        is BattleEvent.CaptureSucceeded -> "Gefangen!"
        is BattleEvent.CaptureFailed -> "Es hat sich befreit!"
        is BattleEvent.FleeSucceeded -> "Entkommen!"
        is BattleEvent.FleeFailed -> "Flucht misslungen!"
        is BattleEvent.Protected -> "Abgewehrt!"
        is BattleEvent.ShieldRaised -> "Schild (+${event.amount})"
        is BattleEvent.AbilityTriggered -> resolve(event.nameKey)
        is BattleEvent.Charging -> resolve(event.messageKey)
        is BattleEvent.Recharging -> "… muss sich sammeln."
        is BattleEvent.ConfusionSelfHit -> "Trifft sich selbst: −${event.damage}"
        is BattleEvent.ActionBlocked -> "${resolve(event.condition.displayKey)} verhindert die Aktion."
        else -> null
    }

    /** Content strings are resolved here so the log lines are ready to render. */
    private fun resolve(key: String): String = localization[key]

    private fun refreshFromSession() {
        val current = session ?: return
        val battleState = current.state
        val activePlayer = battleState.activePlayers.firstOrNull()
        _state.update { ui ->
            ui.copy(
                player = activePlayer,
                enemy = battleState.activeEnemies.firstOrNull(),
                bench = battleState.playerTeam
                    .filterIndexed { index, _ -> index !in battleState.activePlayerSlots }
                    .map { it.monster },
                moves = activePlayer?.monster?.moves.orEmpty().mapNotNull { slot ->
                    content.move(slot.moveId)?.let {
                        MoveButton(it.nameKey, slot.currentPp, slot.maxPp)
                    }
                },
                turn = battleState.turn,
                outcome = battleState.outcome,
                canCapture = battleState.type.allowsCapture,
                canFlee = battleState.type.allowsFlee,
            )
        }
    }

    /** Writes experience, captures and party changes back to the repositories. */
    private suspend fun finish() {
        val current = session ?: return
        audio.popBattleMusic()

        val spoils = current.spoils()
        val experience = spoils.filterIsInstance<BattleEvent.ExperienceGained>()
            .associate { it.monsterUid to it.amount }

        val defeated = current.state.enemyTeam.filter { it.isFainted }
        applyResults(
            party = current.survivingParty(),
            experienceByUid = experience,
            defeatedSpecies = defeated.map { it.monster.species },
            defeatedLevels = defeated.map { it.monster.level },
            won = current.outcome == BattleOutcome.VICTORY,
        ).forEach { levelUp ->
            audio.playSound(AudioKeys.LEVEL_UP)
            _state.update {
                it.copy(log = it.log + "${resolve(levelUp.monster.displayNameKey)} → Stufe ${levelUp.newLevel}")
            }
        }

        current.capturedMonster()?.let { captured ->
            registerCapture(
                captured = captured,
                orbItemId = captured.caughtWithOrbId ?: "orb_wood",
                locationId = player.profile().currentLocationId,
                trainerName = player.profile().name,
                nowEpochMs = System.currentTimeMillis(),
            )
        }
        _state.update { it.copy(busy = true, finished = true) }
    }

    private suspend fun loadOrbs() {
        val owned = inventory.stacks()
        val orbs = owned.mapNotNull { stack ->
            val item = content.item(stack.itemId) ?: return@mapNotNull null
            if (!item.isOrb) return@mapNotNull null
            OrbButton(item.id, item.nameKey, stack.quantity)
        }
        _state.update { it.copy(orbs = orbs) }
    }

    init {
        viewModelScope.launch { loadOrbs() }
    }

    private companion object {
        const val WILD_PREFIX = "wild:"
        const val EVENT_DELAY_MS = 520
    }
}
