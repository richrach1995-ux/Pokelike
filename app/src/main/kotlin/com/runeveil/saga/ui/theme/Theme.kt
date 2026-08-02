package com.runeveil.saga.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Runeveil's visual identity: dark fantasy, Norse ornament, gold on night.
 *
 * The game is dark-first — the light scheme exists for accessibility and for
 * the few daylight screens (bestiary, journal) rather than as the default.
 */

// --- Palette ---------------------------------------------------------------

/** Deep night sky over Midgard — the base surface of the whole game. */
val RuneNight = Color(0xFF0E0B14)
val RuneNightRaised = Color(0xFF171221)
val RuneNightSunken = Color(0xFF08060D)

/** Aged gold of carved runes — the primary accent. */
val RuneGold = Color(0xFFC79A4B)
val RuneGoldBright = Color(0xFFE8D27A)
val RuneGoldDim = Color(0xFF8A6A32)

/** Bifröst frost — the secondary accent used for interactive elements. */
val RuneFrost = Color(0xFF7FD8E8)
val RuneFrostDim = Color(0xFF3E7C8A)

/** Blood of the fallen — errors, damage, danger. */
val RuneBlood = Color(0xFFB02A37)
val RuneEmber = Color(0xFFE2552B)

/** Yggdrasil's green — healing, growth, success. */
val RuneLeaf = Color(0xFF4CA64C)

val RuneParchment = Color(0xFFF3E9D2)
val RuneAsh = Color(0xFF9A93A6)

private val DarkColors = darkColorScheme(
    primary = RuneGold,
    onPrimary = RuneNight,
    primaryContainer = RuneGoldDim,
    onPrimaryContainer = RuneParchment,
    secondary = RuneFrost,
    onSecondary = RuneNight,
    secondaryContainer = RuneFrostDim,
    onSecondaryContainer = RuneParchment,
    tertiary = RuneLeaf,
    onTertiary = RuneNight,
    background = RuneNight,
    onBackground = RuneParchment,
    surface = RuneNightRaised,
    onSurface = RuneParchment,
    surfaceVariant = RuneNightSunken,
    onSurfaceVariant = RuneAsh,
    error = RuneBlood,
    onError = RuneParchment,
    outline = RuneGoldDim,
    outlineVariant = Color(0xFF33293F),
)

private val LightColors = lightColorScheme(
    primary = RuneGoldDim,
    onPrimary = RuneParchment,
    primaryContainer = RuneGoldBright,
    onPrimaryContainer = RuneNight,
    secondary = RuneFrostDim,
    onSecondary = RuneParchment,
    tertiary = RuneLeaf,
    background = RuneParchment,
    onBackground = RuneNight,
    surface = Color(0xFFFFF9EC),
    onSurface = RuneNight,
    surfaceVariant = Color(0xFFE7DCC4),
    onSurfaceVariant = Color(0xFF4A4235),
    error = RuneBlood,
    outline = RuneGoldDim,
)

/** High-contrast variant offered in the accessibility settings. */
private val HighContrastColors = darkColorScheme(
    primary = Color(0xFFFFD770),
    onPrimary = Color.Black,
    secondary = Color(0xFF9FF0FF),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF101010),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = Color(0xFFFF6B6B),
    outline = Color(0xFFFFD770),
)

// --- Typography ------------------------------------------------------------

/**
 * A single family is used throughout; weight and letter spacing carry the
 * "carved into stone" feel instead of a decorative display font, which keeps
 * long German compound names legible on small screens.
 */
private val RuneTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = 2.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 1.5.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.8.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
    ),
)

private val RuneShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Motion preferences from the settings screen, exposed to every composable so
 * animations can be shortened or skipped without threading a parameter through
 * the whole tree.
 */
data class MotionSettings(
    val reducedMotion: Boolean = false,
    val screenShake: Boolean = true,
    val battleSpeed: Float = 1f,
) {
    /** Scales a duration by the player's battle-speed preference. */
    fun scale(durationMs: Int): Int =
        if (reducedMotion) 0 else (durationMs / battleSpeed.coerceAtLeast(0.25f)).toInt()
}

val LocalMotionSettings = staticCompositionLocalOf { MotionSettings() }

@Composable
fun RuneveilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    motion: MotionSettings = MotionSettings(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        highContrast -> HighContrastColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalMotionSettings provides motion) {
        MaterialTheme(
            colorScheme = colors,
            typography = RuneTypography,
            shapes = RuneShapes,
            content = content,
        )
    }
}
