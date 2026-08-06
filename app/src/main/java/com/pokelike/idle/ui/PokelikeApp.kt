package com.pokelike.idle.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pokelike.idle.ui.navigation.PokelikeBottomBar
import com.pokelike.idle.ui.navigation.PokelikeDestination
import com.pokelike.idle.ui.navigation.PokelikeNavHost
import com.pokelike.idle.ui.navigation.PokelikeTopBar

/**
 * Wurzel-Composable der App.
 *
 * Haelt das Geruest, das ueber allen Bildschirmen liegt: den NavController, die
 * untere Navigationsleiste und das Scaffold. Die Activity bleibt dadurch auf
 * ihre Plattformaufgaben beschraenkt, und dieser Composable laesst sich in
 * einem UI-Test ohne Activity aufbauen.
 */
@Composable
fun PokelikeApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Beobachtet den obersten Eintrag des Backstacks, damit die untere Leiste
    // weiss, welcher Tab hervorzuheben ist.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = PokelikeDestination.fromRoute(currentRoute)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PokelikeTopBar(
                destination = currentDestination,
                onOpenSettings = {
                    // Bewusst ohne popUpTo: Die Einstellungen liegen ueber dem
                    // aktuellen Tab, und die Zurueck-Taste soll genau dorthin
                    // zurueckfuehren. Ein Tab-Wechsel wuerde stattdessen den
                    // Backstack flach halten und den Weg zurueck verlieren.
                    navController.navigate(PokelikeDestination.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        },
        bottomBar = {
            PokelikeBottomBar(
                currentRoute = currentRoute,
                onDestinationSelected = { destination ->
                    navController.navigateToTab(destination)
                },
            )
        },
    ) { innerPadding ->
        PokelikeNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Wechselt zu einem Hauptziel der unteren Leiste.
 *
 * Die drei Optionen sind fuer Tab-Navigation zwingend, sonst waechst der
 * Backstack unbegrenzt und die Zurueck-Taste fuehrt den Spieler durch jeden je
 * besuchten Tab:
 *
 * - `popUpTo(startDestination) { saveState = true }` haelt den Backstack flach
 *   und merkt sich den Zustand des verlassenen Tabs.
 * - `launchSingleTop` verhindert, dass wiederholtes Antippen desselben Tabs den
 *   Bildschirm mehrfach auf den Stack legt.
 * - `restoreState` stellt Scrollposition und Zustand beim Zurueckwechseln
 *   wieder her.
 */
private fun NavHostController.navigateToTab(
    destination: PokelikeDestination,
) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
