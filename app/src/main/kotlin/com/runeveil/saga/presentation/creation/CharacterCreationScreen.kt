package com.runeveil.saga.presentation.creation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.runeveil.saga.R
import com.runeveil.saga.domain.model.monster.Gender
import com.runeveil.saga.domain.model.monster.MonsterSpecies
import com.runeveil.saga.domain.model.player.Appearance
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.SaveRepository
import com.runeveil.saga.ui.components.ElementBadge
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.RunicButton
import com.runeveil.saga.ui.components.SectionHeader
import com.runeveil.saga.ui.components.contentText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Character creation: name, gender, appearance and the choice of the first
 * companion — three starters, one per classic role, so the first hour plays
 * differently depending on the pick.
 */
@Composable
fun CharacterCreationScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: CreationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.creation_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.creation_name)) },
                placeholder = { Text(stringResource(R.string.creation_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader(stringResource(R.string.creation_gender))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GenderChip(Gender.MALE, R.string.creation_gender_male, state.gender, viewModel::setGender)
                GenderChip(Gender.FEMALE, R.string.creation_gender_female, state.gender, viewModel::setGender)
                GenderChip(Gender.UNKNOWN, R.string.creation_gender_other, state.gender, viewModel::setGender)
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(stringResource(R.string.creation_appearance))
            Spacer(Modifier.height(8.dp))
            AppearanceRow("Haar", HAIR_OPTIONS, state.appearance.hairKey) { viewModel.setHair(it) }
            AppearanceRow("Haut", SKIN_OPTIONS, state.appearance.skinToneKey) { viewModel.setSkin(it) }
            AppearanceRow("Kleidung", OUTFIT_OPTIONS, state.appearance.outfitKey) { viewModel.setOutfit(it) }

            Spacer(Modifier.height(24.dp))
            SectionHeader(stringResource(R.string.creation_starter))
            Spacer(Modifier.height(10.dp))
            state.starters.forEach { species ->
                StarterCard(
                    species = species,
                    selected = state.starterId == species.id,
                    onSelect = { viewModel.setStarter(species.id) },
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))
            RunicButton(
                text = stringResource(R.string.creation_begin),
                onClick = { viewModel.create(onCreated) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canBegin,
                glyph = "ᚦ",
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun GenderChip(
    gender: Gender,
    labelRes: Int,
    selected: Gender,
    onSelect: (Gender) -> Unit,
) {
    FilterChip(
        selected = selected == gender,
        onClick = { onSelect(gender) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun AppearanceRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { index, option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text("${index + 1}") },
                )
            }
        }
    }
}

@Composable
private fun StarterCard(species: MonsterSpecies, selected: Boolean, onSelect: () -> Unit) {
    RunePanel(
        modifier = Modifier.fillMaxWidth(),
        accent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        onClick = onSelect,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = contentText(species.nameKey),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    species.elements.forEach { ElementBadge(it) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = contentText(species.descriptionKey),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = contentText(species.loreKey),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val HAIR_OPTIONS = listOf("hair_01", "hair_02", "hair_03", "hair_04", "hair_05")
private val SKIN_OPTIONS = listOf("skin_01", "skin_02", "skin_03", "skin_04", "skin_05")
private val OUTFIT_OPTIONS = listOf("outfit_wanderer", "outfit_hunter", "outfit_seer", "outfit_smith")

data class CreationUiState(
    val name: String = "",
    val gender: Gender = Gender.UNKNOWN,
    val appearance: Appearance = Appearance(),
    val starters: List<MonsterSpecies> = emptyList(),
    val starterId: String? = null,
) {
    val canBegin: Boolean get() = name.isNotBlank() && starterId != null
}

@HiltViewModel
class CreationViewModel @Inject constructor(
    private val content: ContentRepository,
    private val saves: SaveRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreationUiState())
    val state: StateFlow<CreationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            content.ensureLoaded()
            // The three starters are the base forms of the authored Midgard
            // fire, nature and water lines — attacker, tank and speedster.
            val starters = STARTER_IDS.mapNotNull { content.species(it) }
            _state.update { it.copy(starters = starters, starterId = starters.firstOrNull()?.id) }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value.take(MAX_NAME_LENGTH)) }
    fun setGender(value: Gender) = _state.update { it.copy(gender = value) }
    fun setStarter(id: String) = _state.update { it.copy(starterId = id) }
    fun setHair(key: String) = _state.update { it.copy(appearance = it.appearance.copy(hairKey = key)) }
    fun setSkin(key: String) = _state.update { it.copy(appearance = it.appearance.copy(skinToneKey = key)) }
    fun setOutfit(key: String) = _state.update { it.copy(appearance = it.appearance.copy(outfitKey = key)) }

    fun create(onCreated: () -> Unit) {
        val current = _state.value
        val starter = current.starterId ?: return
        viewModelScope.launch {
            saves.createNewGame(
                playerName = current.name.trim(),
                gender = current.gender,
                appearance = current.appearance,
                starterSpeciesId = starter,
            ).onSuccess { onCreated() }
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 16
        val STARTER_IDS = listOf("glutwelp", "moosling", "bachotter")
    }
}
