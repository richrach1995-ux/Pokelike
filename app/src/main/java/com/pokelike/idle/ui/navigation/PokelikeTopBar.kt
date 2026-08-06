package com.pokelike.idle.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pokelike.idle.R
import com.pokelike.idle.ui.theme.PokelikeTheme

/**
 * Obere Leiste mit Titel und Zugang zu den Einstellungen.
 *
 * **Warum es sie ueberhaupt gibt.** Die Einstellungen brauchen einen
 * Einstiegspunkt, der von jedem Bildschirm aus erreichbar ist. Ein sechster
 * Eintrag in der unteren Leiste kam dafuer nicht in Frage - Material empfiehlt
 * dort drei bis fuenf, und Einstellungen werden selten geoeffnet. Sie in einen
 * einzelnen Bildschirm zu legen waere die schlechtere Loesung: Der Spieler
 * muesste sich merken, in welchem.
 *
 * Auf den Hauptbildschirmen zeigt die Leiste den Namen des Ziels und rechts das
 * Zahnrad. Auf den Einstellungen selbst tritt an dessen Stelle der Zurueck-Pfeil
 * - dort waere ein Zahnrad, das auf den eigenen Bildschirm fuehrt, sinnlos.
 *
 * @param destination Sichtbares Ziel, oder `null` waehrend des allerersten
 *   Aufbaus.
 * @param onOpenSettings Wird beim Antippen des Zahnrads aufgerufen.
 * @param onNavigateBack Wird beim Antippen des Zurueck-Pfeils aufgerufen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokelikeTopBar(
    destination: PokelikeDestination?,
    onOpenSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSettings = destination == PokelikeDestination.Settings

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PokelikeTheme.gameColors.elevatedSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(
                text = destination?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.app_name),
            )
        },
        navigationIcon = {
            if (isSettings) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            }
        },
        actions = {
            if (!isSettings) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                    )
                }
            }
        },
    )
}
