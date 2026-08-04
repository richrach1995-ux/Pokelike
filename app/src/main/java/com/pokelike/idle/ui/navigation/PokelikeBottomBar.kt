package com.pokelike.idle.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pokelike.idle.ui.theme.PokelikeTheme

/**
 * Untere Navigationsleiste.
 *
 * Rendert sich vollstaendig aus [PokelikeDestination.bottomBarDestinations].
 * Damit taucht jedes neue Hauptziel automatisch auf, sobald es dort eingetragen
 * ist - dieser Composable muss dafuer nie geaendert werden.
 *
 * Bei nur einem Ziel zeichnet die Funktion bewusst nichts: Eine Leiste mit
 * einem einzigen Eintrag bietet keine Auswahl und nimmt dem Spielfeld nur Hoehe.
 *
 * @param currentRoute Route des gerade sichtbaren Ziels, oder `null` waehrend
 *   des allerersten Aufbaus, bevor der NavController einen Eintrag hat.
 * @param onDestinationSelected Wird mit dem angetippten Ziel aufgerufen. Die
 *   Navigation selbst passiert bewusst nicht hier, damit dieser Composable ohne
 *   NavController in einer Preview oder einem UI-Test darstellbar bleibt.
 */
@Composable
fun PokelikeBottomBar(
    currentRoute: String?,
    onDestinationSelected: (PokelikeDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val destinations = PokelikeDestination.bottomBarDestinations
    if (destinations.size < 2) return

    NavigationBar(
        modifier = modifier,
        containerColor = PokelikeTheme.gameColors.elevatedSurface,
    ) {
        destinations.forEach { destination ->
            val selected = currentRoute == destination.route

            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) {
                            destination.selectedIcon
                        } else {
                            destination.unselectedIcon
                        },
                        // Die Beschriftung steht direkt darunter und benennt das
                        // Ziel bereits. Eine zusaetzliche Icon-Beschreibung
                        // wuerde von Screenreadern doppelt vorgelesen.
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(destination.labelRes)) },
                colors = NavigationBarItemDefaults.colors(),
            )
        }
    }
}
