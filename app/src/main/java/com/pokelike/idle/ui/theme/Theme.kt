package com.pokelike.idle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.pokelike.idle.domain.model.ThemeMode

/**
 * Wurzel-Theme der App.
 *
 * Dynamic Color (Material You) ist bewusst NICHT aktiviert. Bei einem Spiel ist
 * die Farbgebung Teil der Markenidentitaet und der Spielinformation: Gold
 * bedeutet Muenzen, Violett bedeutet Event-Token. Wuerde das System die Palette
 * nach dem Hintergrundbild des Nutzers umfaerben, gingen diese Zuordnungen
 * verloren und Screenshots im Store saehen auf jedem Geraet anders aus.
 *
 * @param darkTheme Ob die dunkle Palette verwendet wird. Standard ist die
 *   Systemeinstellung; sobald der Einstellungs-Screen existiert, reicht die App
 *   hier die gespeicherte Nutzerpraeferenz herein.
 */
@Composable
fun PokelikeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val gameColors = if (darkTheme) DarkGameColors else LightGameColors

    CompositionLocalProvider(
        LocalGameColors provides gameColors,
        LocalDimens provides Dimens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PokelikeTypography,
            content = content,
        )
    }
}

/**
 * Loest die gewaehlte Darstellungsvariante in einen konkreten Modus auf.
 *
 * Als eigene Funktion statt eines `when` im Screen: Der Systemzustand darf nur
 * dann gelesen werden, wenn er auch gilt. Eine feste Wahl des Spielers soll
 * keine Neuzusammensetzung ausloesen, bloss weil das Geraet in den Dunkelmodus
 * wechselt.
 */
@Composable
fun ThemeMode.resolveIsDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Erweiterung von [MaterialTheme] um die spielspezifischen Werte.
 *
 * Dadurch liest sich der Zugriff im Screen einheitlich:
 * `MaterialTheme.colorScheme.primary` neben `PokelikeTheme.gameColors.coin`.
 */
object PokelikeTheme {

    /** Spielspezifische Farben (Ressourcen, Seltenheit). */
    val gameColors: GameColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGameColors.current

    /** Abstandsraster und wiederkehrende Groessen. */
    val dimens: Dimens
        @Composable
        @ReadOnlyComposable
        get() = LocalDimens.current

    /** Typografie, inklusive der spielspezifischen Zusatzstile. */
    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

// --- Material-3-Farbschemata ----------------------------------------------
// Die Zuordnung der Rohfarben aus Color.kt zu den Material-Rollen. Getrennt
// gehalten, damit ein Skin spaeter nur diese Zuordnung ersetzen muss.

private val DarkColorScheme = darkColorScheme(
    primary = BrandViolet80,
    onPrimary = BrandViolet20,
    primaryContainer = BrandViolet40,
    onPrimaryContainer = BrandViolet80,
    secondary = BrandTeal80,
    onSecondary = BrandTeal40,
    secondaryContainer = BrandTeal40,
    onSecondaryContainer = BrandTeal80,
    tertiary = BrandPink80,
    onTertiary = BrandPink40,
    background = SurfaceDark,
    onBackground = SurfaceLight,
    surface = SurfaceDark,
    onSurface = SurfaceLight,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = SurfaceLightElevated,
    error = ErrorRed80,
    onError = ErrorRed40,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandViolet40,
    onPrimary = SurfaceLight,
    primaryContainer = BrandViolet80,
    onPrimaryContainer = BrandViolet20,
    secondary = BrandTeal40,
    onSecondary = SurfaceLight,
    secondaryContainer = BrandTeal80,
    onSecondaryContainer = BrandTeal40,
    tertiary = BrandPink40,
    onTertiary = SurfaceLight,
    background = SurfaceLight,
    onBackground = SurfaceDark,
    surface = SurfaceLight,
    onSurface = SurfaceDark,
    surfaceVariant = SurfaceLightElevated,
    onSurfaceVariant = SurfaceDarkElevated,
    error = ErrorRed40,
    onError = SurfaceLight,
)
