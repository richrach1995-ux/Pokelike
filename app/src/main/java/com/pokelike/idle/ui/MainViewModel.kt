package com.pokelike.idle.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.domain.model.GameSettings
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Zustand, den die Activity vor dem Aufbau der Oberflaeche kennen muss.
 *
 * @property settings Einstellungen. Bestimmen unter anderem die Farbgebung.
 * @property isReady Ob der Spielstand geladen ist. Solange nicht, bleibt der
 *   Splashscreen stehen.
 */
@Immutable
data class MainUiState(
    val settings: GameSettings = GameSettings(),
    val isReady: Boolean = false,
)

/**
 * ViewModel der Activity.
 *
 * Liefert ausschliesslich, was vor dem ersten Bild feststehen muss: die
 * Farbgebung und die Frage, ob der Splashscreen weichen darf. Spielinhalte
 * gehoeren in die ViewModels der einzelnen Bildschirme.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    gameRepository: GameRepository,
) : ViewModel() {

    /**
     * Zustand der Activity.
     *
     * [SharingStarted.Eagerly] statt `WhileSubscribed`: Die Bedingung des
     * Splashscreens liest den Wert, bevor irgendein Composable den Flow
     * beobachtet. Bei traeger Auswertung stuende dort dauerhaft der
     * Ausgangswert, und der Splashscreen wuerde nie verschwinden.
     */
    val uiState: StateFlow<MainUiState> = combine(
        settingsRepository.settings,
        gameRepository.isLoaded,
    ) { settings, isLoaded ->
        MainUiState(settings = settings, isReady = isLoaded)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainUiState(),
    )
}
