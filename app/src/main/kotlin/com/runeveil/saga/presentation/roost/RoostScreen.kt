package com.runeveil.saga.presentation.roost

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
import com.runeveil.saga.domain.model.monster.MonsterInstance
import com.runeveil.saga.domain.repository.MonsterRepository
import com.runeveil.saga.domain.rules.BreedingRules
import com.runeveil.saga.domain.usecase.BreedUseCase
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicButton
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
 * The Brutstätte: pick two parents, pay the fee, receive an egg.
 * The compatibility reason is shown up front so the system is learnable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoostScreen(
    onBack: () -> Unit,
    viewModel: RoostViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.roost_title)) },
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
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = state.motherUid?.let { uid ->
                                state.candidates.firstOrNull { it.uid == uid }
                                    ?.let { contentText(it.displayNameKey) }
                            } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = state.fatherUid?.let { uid ->
                                state.candidates.firstOrNull { it.uid == uid }
                                    ?.let { contentText(it.displayNameKey) }
                            } ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        state.fee?.let {
                            Text(
                                stringResource(R.string.roost_fee, it),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        state.message?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        RunicButton(
                            text = stringResource(R.string.roost_breed),
                            onClick = viewModel::breed,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.canBreed,
                            glyph = "ᛖ",
                        )
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.party_title)) }
            items(state.candidates, key = { it.uid }) { monster ->
                RunePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(contentText(monster.displayNameKey), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${contentText(monster.gender.displayKey)} · " +
                                stringResource(R.string.common_level, monster.level),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RunicOutlinedButton("♀", { viewModel.setMother(monster.uid) })
                            RunicOutlinedButton("♂", { viewModel.setFather(monster.uid) })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

data class RoostUiState(
    val candidates: List<MonsterInstance> = emptyList(),
    val motherUid: String? = null,
    val fatherUid: String? = null,
    val fee: Int? = null,
    val message: String? = null,
    val canBreed: Boolean = false,
)

@HiltViewModel
class RoostViewModel @Inject constructor(
    private val monsters: MonsterRepository,
    private val breedUseCase: BreedUseCase,
    private val audio: AudioEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(RoostUiState())
    val state: StateFlow<RoostUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                candidates = (monsters.party() + monsters.vault()).filterNot { it.isEgg },
            )
        }
    }

    fun setMother(uid: String) {
        _state.value = _state.value.copy(motherUid = uid)
        evaluate()
    }

    fun setFather(uid: String) {
        _state.value = _state.value.copy(fatherUid = uid)
        evaluate()
    }

    /** Shows the fee and the compatibility verdict before any gold is spent. */
    private fun evaluate() {
        val current = _state.value
        val mother = current.candidates.firstOrNull { it.uid == current.motherUid }
        val father = current.candidates.firstOrNull { it.uid == current.fatherUid }
        if (mother == null || father == null || mother.uid == father.uid) {
            _state.value = current.copy(canBreed = false, fee = null, message = null)
            return
        }
        val compatibility = BreedingRules.compatibility(mother, father)
        val compatible = compatibility == BreedingRules.Compatibility.Compatible
        _state.value = current.copy(
            canBreed = compatible,
            fee = if (compatible) BreedingRules.roostFee(mother, father) else null,
            message = if (compatible) null else "roost_incompatible",
        )
    }

    fun breed() {
        val current = _state.value
        val mother = current.motherUid ?: return
        val father = current.fatherUid ?: return
        viewModelScope.launch {
            when (val result = breedUseCase(mother, father, System.currentTimeMillis())) {
                is BreedUseCase.Result.Egg -> {
                    audio.playSound(AudioKeys.EGG_HATCH)
                    _state.value = _state.value.copy(message = "roost_egg_received")
                }
                is BreedUseCase.Result.NotEnoughGold ->
                    _state.value = _state.value.copy(message = "shop_not_enough_gold")
                is BreedUseCase.Result.Rejected ->
                    _state.value = _state.value.copy(message = "roost_incompatible")
            }
        }
    }
}
