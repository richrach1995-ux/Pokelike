package com.pokelike.idle.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.BuildConfig
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ThemeMode
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.repository.SettingsRepository
import com.pokelike.idle.util.TimeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel der Einstellungen.
 *
 * Jede Aenderung wird sofort geschrieben; es gibt kein "Speichern". Ein
 * Bildschirm mit Schaltern und einem Bestaetigungsknopf laesst den Spieler
 * ruecken, ob seine Auswahl schon gilt - und wer ihn ueber die Zurueck-Taste
 * verlaesst, verlaere sie.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val gameRepository: GameRepository,
    private val timeSource: TimeSource,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { settings ->
            SettingsUiState(
                themeMode = settings.themeMode,
                vibrationEnabled = settings.vibrationEnabled,
                versionName = BuildConfig.VERSION_NAME,
                saveVersion = GameState.CURRENT_SCHEMA_VERSION,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState(
                versionName = BuildConfig.VERSION_NAME,
                saveVersion = GameState.CURRENT_SCHEMA_VERSION,
            ),
        )

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun onVibrationChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
    }

    /**
     * Loescht den Spielstand und beginnt von vorn.
     *
     * Die laufenden Dienste lesen den Spielstand ueber [GameRepository] und
     * bekommen den neuen Stand dadurch von selbst mit - es ist kein Neustart
     * der App noetig.
     *
     * Der Aufruf laeuft ueber `viewModelScope` und nicht ueber den
     * prozessweiten Scope: Wird der Bildschirm waehrenddessen verlassen, ist
     * der Abbruch die richtige Antwort. Ein halb geloeschter Spielstand
     * entsteht dabei nicht, denn das Loeschen selbst laeuft in einer
     * Datenbanktransaktion.
     */
    fun onResetGame() {
        viewModelScope.launch {
            gameRepository.resetGame(timeSource.wallClock())
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
