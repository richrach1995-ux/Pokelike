package com.pokelike.idle.ui.screens.settings

import androidx.annotation.StringRes
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.ThemeMode

/**
 * Beschriftung der Darstellungsvarianten.
 *
 * Wie bei Gebaeuden, Upgrades, Zielen und Boostern in der UI-Schicht, damit
 * [ThemeMode] frei von Android bleibt. Als `when` ohne `else`: Eine neue
 * Variante meldet der Compiler, statt sie im Spiel ohne Namen erscheinen zu
 * lassen.
 */
@get:StringRes
val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }
