package com.pokelike.idle.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pokelike.idle.domain.model.GameSettings
import com.pokelike.idle.domain.model.ThemeMode
import com.pokelike.idle.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Einstellungen in DataStore.
 *
 * DataStore statt SharedPreferences: Es schreibt asynchron ohne Blockade des
 * UI-Threads, meldet Aenderungen als Flow und ist gegen abgebrochene
 * Schreibvorgaenge abgesichert. SharedPreferences bietet nichts davon.
 *
 * Warum die Einstellungen nicht in der Datenbank liegen: Sie sind
 * Geraeteeigenschaften, kein Fortschritt. Sie duerfen im Geraete-Backup
 * mitgesichert werden - anders als der Spielstand -, und sie ueberstehen ein
 * Zuruecksetzen des Spiels.
 */
@Singleton
class SettingsDataStoreRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<GameSettings> = dataStore.data
        // Ein beschaedigter Einstellungsspeicher darf den Start nicht
        // verhindern. In diesem Fall gelten die Voreinstellungen; das ist
        // deutlich besser als eine App, die sich nicht mehr oeffnen laesst.
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences.toGameSettings() }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[KEY_THEME_MODE] = mode.id }
    }

    override suspend fun setMusicEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_MUSIC] = enabled }
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_SOUND] = enabled }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_VIBRATION] = enabled }
    }

    override suspend fun setShowFps(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_SHOW_FPS] = enabled }
    }

    /**
     * Liest die Einstellungen und faellt je Wert einzeln auf die Voreinstellung
     * zurueck.
     *
     * Wichtig ist die Einzelbetrachtung: Ein nach einem Update neu
     * hinzugekommener Schluessel fehlt im gespeicherten Bestand. Wuerde daraus
     * das gesamte Objekt verworfen, verloere der Spieler mit jedem Update alle
     * uebrigen Einstellungen.
     */
    private fun Preferences.toGameSettings(): GameSettings {
        val defaults = GameSettings()
        return GameSettings(
            themeMode = ThemeMode.fromId(this[KEY_THEME_MODE]),
            musicEnabled = this[KEY_MUSIC] ?: defaults.musicEnabled,
            soundEnabled = this[KEY_SOUND] ?: defaults.soundEnabled,
            vibrationEnabled = this[KEY_VIBRATION] ?: defaults.vibrationEnabled,
            showFps = this[KEY_SHOW_FPS] ?: defaults.showFps,
        )
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_MUSIC = booleanPreferencesKey("music_enabled")
        val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
        val KEY_SHOW_FPS = booleanPreferencesKey("show_fps")
    }
}
