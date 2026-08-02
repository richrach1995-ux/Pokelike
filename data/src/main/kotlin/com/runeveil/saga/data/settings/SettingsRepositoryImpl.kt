package com.runeveil.saga.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.runeveil.saga.domain.repository.Difficulty
import com.runeveil.saga.domain.repository.GameSettings
import com.runeveil.saga.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences, persisted with DataStore.
 *
 * Settings live outside the save slots on purpose: volume and accessibility
 * choices belong to the *player*, not to a play-through.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeSettings(): Flow<GameSettings> =
        dataStore.data.map { it.toSettings() }

    override suspend fun settings(): GameSettings = dataStore.data.first().toSettings()

    override suspend fun update(transform: (GameSettings) -> GameSettings) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toSettings())
            preferences[Keys.MUSIC] = updated.musicVolume
            preferences[Keys.SOUND] = updated.soundVolume
            preferences[Keys.VOICE] = updated.voiceVolume
            preferences[Keys.ANIMATION_SPEED] = updated.battleAnimationSpeed
            preferences[Keys.TEXT_SPEED] = updated.textSpeed
            preferences[Keys.AUTOSAVE] = updated.autosaveEnabled
            preferences[Keys.AUTOSAVE_INTERVAL] = updated.autosaveIntervalMinutes
            preferences[Keys.HAPTICS] = updated.hapticsEnabled
            preferences[Keys.DAMAGE_NUMBERS] = updated.showDamageNumbers
            preferences[Keys.TYPE_HINTS] = updated.showTypeHints
            preferences[Keys.CONFIRM_RELEASE] = updated.confirmBeforeRelease
            preferences[Keys.LANGUAGE] = updated.language
            preferences[Keys.HIGH_CONTRAST] = updated.highContrast
            preferences[Keys.REDUCED_MOTION] = updated.reducedMotion
            preferences[Keys.SCREEN_SHAKE] = updated.screenShake
            preferences[Keys.SKIP_SEEN_CUTSCENES] = updated.skipSeenCutscenes
            preferences[Keys.DIFFICULTY] = updated.difficulty.name
        }
    }

    private fun Preferences.toSettings(): GameSettings {
        val defaults = GameSettings()
        return GameSettings(
            musicVolume = this[Keys.MUSIC] ?: defaults.musicVolume,
            soundVolume = this[Keys.SOUND] ?: defaults.soundVolume,
            voiceVolume = this[Keys.VOICE] ?: defaults.voiceVolume,
            battleAnimationSpeed = this[Keys.ANIMATION_SPEED] ?: defaults.battleAnimationSpeed,
            textSpeed = this[Keys.TEXT_SPEED] ?: defaults.textSpeed,
            autosaveEnabled = this[Keys.AUTOSAVE] ?: defaults.autosaveEnabled,
            autosaveIntervalMinutes = this[Keys.AUTOSAVE_INTERVAL] ?: defaults.autosaveIntervalMinutes,
            hapticsEnabled = this[Keys.HAPTICS] ?: defaults.hapticsEnabled,
            showDamageNumbers = this[Keys.DAMAGE_NUMBERS] ?: defaults.showDamageNumbers,
            showTypeHints = this[Keys.TYPE_HINTS] ?: defaults.showTypeHints,
            confirmBeforeRelease = this[Keys.CONFIRM_RELEASE] ?: defaults.confirmBeforeRelease,
            language = this[Keys.LANGUAGE] ?: defaults.language,
            highContrast = this[Keys.HIGH_CONTRAST] ?: defaults.highContrast,
            reducedMotion = this[Keys.REDUCED_MOTION] ?: defaults.reducedMotion,
            screenShake = this[Keys.SCREEN_SHAKE] ?: defaults.screenShake,
            skipSeenCutscenes = this[Keys.SKIP_SEEN_CUTSCENES] ?: defaults.skipSeenCutscenes,
            difficulty = this[Keys.DIFFICULTY]
                ?.let { name -> Difficulty.entries.firstOrNull { it.name == name } }
                ?: defaults.difficulty,
        )
    }

    private object Keys {
        val MUSIC = floatPreferencesKey("music_volume")
        val SOUND = floatPreferencesKey("sound_volume")
        val VOICE = floatPreferencesKey("voice_volume")
        val ANIMATION_SPEED = floatPreferencesKey("battle_animation_speed")
        val TEXT_SPEED = floatPreferencesKey("text_speed")
        val AUTOSAVE = booleanPreferencesKey("autosave_enabled")
        val AUTOSAVE_INTERVAL = intPreferencesKey("autosave_interval_minutes")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val DAMAGE_NUMBERS = booleanPreferencesKey("show_damage_numbers")
        val TYPE_HINTS = booleanPreferencesKey("show_type_hints")
        val CONFIRM_RELEASE = booleanPreferencesKey("confirm_before_release")
        val LANGUAGE = stringPreferencesKey("language")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val SCREEN_SHAKE = booleanPreferencesKey("screen_shake")
        val SKIP_SEEN_CUTSCENES = booleanPreferencesKey("skip_seen_cutscenes")
        val DIFFICULTY = stringPreferencesKey("difficulty")
    }
}
