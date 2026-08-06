package com.pokelike.idle.testing

import com.pokelike.idle.domain.model.GameSettings
import com.pokelike.idle.domain.model.ThemeMode
import com.pokelike.idle.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Einstellungen im Arbeitsspeicher.
 *
 * Ersetzt DataStore, das im JVM-Test eine Datei anlegen wuerde. Jede
 * Schreibmethode wirkt sofort, damit ein Test die Auswirkung ohne Umweg
 * pruefen kann.
 */
class FakeSettingsRepository(
    initial: GameSettings = GameSettings(),
) : SettingsRepository {

    private val _settings = MutableStateFlow(initial)
    override val settings: Flow<GameSettings> = _settings

    /** Aktueller Stand, ohne den Fluss einsammeln zu muessen. */
    val current: GameSettings get() = _settings.value

    override suspend fun setThemeMode(mode: ThemeMode) {
        _settings.update { it.copy(themeMode = mode) }
    }

    override suspend fun setMusicEnabled(enabled: Boolean) {
        _settings.update { it.copy(musicEnabled = enabled) }
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        _settings.update { it.copy(soundEnabled = enabled) }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        _settings.update { it.copy(vibrationEnabled = enabled) }
    }

    override suspend fun setShowFps(enabled: Boolean) {
        _settings.update { it.copy(showFps = enabled) }
    }
}
