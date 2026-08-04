package com.pokelike.idle.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pokelike.idle.ui.screens.buildings.BuildingsRoute
import com.pokelike.idle.ui.screens.home.HomeRoute
import com.pokelike.idle.ui.screens.prestige.PrestigeRoute
import com.pokelike.idle.ui.screens.upgrades.UpgradesRoute

/**
 * Navigationsgraph der App.
 *
 * Der Graph kennt nur Ziele und deren Composables. Er weiss nichts ueber
 * ViewModels oder Spiellogik - diese Bindung passiert in den jeweiligen
 * `*Route`-Composables. Dadurch bleibt der Graph auch bei zwanzig Bildschirmen
 * ueberschaubar und ohne Abhaengigkeiten zu den einzelnen Features.
 */
@Composable
fun PokelikeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = PokelikeDestination.startDestination.route,
        modifier = modifier,
    ) {
        homeScreen()
        buildingsScreen()
        upgradesScreen()
        prestigeScreen()
    }
}

/**
 * Registriert den Hauptbildschirm.
 *
 * Als Erweiterungsfunktion auf [NavGraphBuilder] ausgelegt: Jedes Feature
 * bringt spaeter seine eigene Registrierungsfunktion mit, und der Graph oben
 * bleibt eine reine Aufzaehlung.
 */
private fun NavGraphBuilder.homeScreen() {
    composable(route = PokelikeDestination.Home.route) {
        HomeRoute()
    }
}

/** Registriert die Gebaeudeliste. */
private fun NavGraphBuilder.buildingsScreen() {
    composable(route = PokelikeDestination.Buildings.route) {
        BuildingsRoute()
    }
}

/** Registriert die Upgrade-Liste. */
private fun NavGraphBuilder.upgradesScreen() {
    composable(route = PokelikeDestination.Upgrades.route) {
        UpgradesRoute()
    }
}

/** Registriert den Prestige-Bildschirm. */
private fun NavGraphBuilder.prestigeScreen() {
    composable(route = PokelikeDestination.Prestige.route) {
        PrestigeRoute()
    }
}
