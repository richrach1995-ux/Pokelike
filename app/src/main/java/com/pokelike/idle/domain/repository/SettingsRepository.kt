package com.pokelike.idle.domain.repository

import com.pokelike.idle.domain.model.GameSettings
import com.pokelike.idle.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Zugriff auf die Geraeteeinstellungen.
 *
 * Getrennt von [GameRepository], weil Einstellungen und Spielstand
 * unterschiedliche Lebensdauern, Sicherungsregeln und Schreibhaeufigkeiten
 * haben. Eine gemeinsame Schnittstelle wuerde beide Seiten zu Kompromissen
 * zwingen.
 *
 * Jede Einstellung hat eine eigene Schreibmethode statt eines gemeinsamen
 * `save(settings)`. Das vermeidet verlorene Aenderungen: Zwei Bildschirme, die
 * jeweils ein vollstaendiges Einstellungsobjekt zurueckschreiben, wuerden sich
 * gegenseitig ueberschreiben.
 */
interface SettingsRepository {

    /** Laufender Strom der Einstellungen. Meldet jede Aenderung sofort. */
    val settings: Flow<GameSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setMusicEnabled(enabled: Boolean)

    suspend fun setSoundEnabled(enabled: Boolean)

    suspend fun setVibrationEnabled(enabled: Boolean)

    suspend fun setShowFps(enabled: Boolean)
}
