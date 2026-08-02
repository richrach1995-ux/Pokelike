package com.runeveil.saga.presentation.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.runeveil.saga.domain.repository.Difficulty
import com.runeveil.saga.domain.repository.GameSettings
import com.runeveil.saga.domain.repository.SettingsRepository
import com.runeveil.saga.ui.components.RunePanel
import com.runeveil.saga.ui.components.SectionHeader
import com.runeveil.saga.ui.components.contentText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Audio, gameplay and accessibility settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_audio))
            RunePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    SliderRow(stringResource(R.string.settings_music), settings.musicVolume) { value ->
                        viewModel.update { it.copy(musicVolume = value) }
                    }
                    SliderRow(stringResource(R.string.settings_sound), settings.soundVolume) { value ->
                        viewModel.update { it.copy(soundVolume = value) }
                    }
                    SliderRow(stringResource(R.string.settings_voice), settings.voiceVolume) { value ->
                        viewModel.update { it.copy(voiceVolume = value) }
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_gameplay))
            RunePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    SliderRow(
                        stringResource(R.string.settings_animation_speed),
                        settings.battleAnimationSpeed / 2f,
                    ) { value ->
                        viewModel.update { it.copy(battleAnimationSpeed = (value * 2f).coerceIn(0.25f, 2f)) }
                    }
                    SliderRow(stringResource(R.string.settings_text_speed), settings.textSpeed / 2f) { value ->
                        viewModel.update { it.copy(textSpeed = (value * 2f).coerceIn(0.25f, 2f)) }
                    }
                    SwitchRow(stringResource(R.string.settings_autosave), settings.autosaveEnabled) { value ->
                        viewModel.update { it.copy(autosaveEnabled = value) }
                    }
                    SwitchRow(
                        stringResource(R.string.settings_damage_numbers),
                        settings.showDamageNumbers,
                    ) { value -> viewModel.update { it.copy(showDamageNumbers = value) } }
                    SwitchRow(stringResource(R.string.settings_type_hints), settings.showTypeHints) { value ->
                        viewModel.update { it.copy(showTypeHints = value) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.settings_difficulty),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Difficulty.entries.forEach { difficulty ->
                            FilterChip(
                                selected = settings.difficulty == difficulty,
                                onClick = { viewModel.update { it.copy(difficulty = difficulty) } },
                                label = { Text(contentText(difficulty.displayKey)) },
                            )
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_accessibility))
            RunePanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    SwitchRow(stringResource(R.string.settings_reduced_motion), settings.reducedMotion) { value ->
                        viewModel.update { it.copy(reducedMotion = value) }
                    }
                    SwitchRow(stringResource(R.string.settings_high_contrast), settings.highContrast) { value ->
                        viewModel.update { it.copy(highContrast = value) }
                    }
                    SwitchRow(stringResource(R.string.settings_screen_shake), settings.screenShake) { value ->
                        viewModel.update { it.copy(screenShake = value) }
                    }
                    SwitchRow(stringResource(R.string.settings_haptics), settings.hapticsEnabled) { value ->
                        viewModel.update { it.copy(hapticsEnabled = value) }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${(value * 100).toInt()} %", style = MaterialTheme.typography.labelSmall)
        }
        Slider(value = value.coerceIn(0f, 1f), onValueChange = onChange)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<GameSettings> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameSettings())

    fun update(transform: (GameSettings) -> GameSettings) {
        viewModelScope.launch { repository.update(transform) }
    }
}
