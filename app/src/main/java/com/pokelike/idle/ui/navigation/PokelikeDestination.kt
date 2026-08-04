package com.pokelike.idle.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.ui.graphics.vector.ImageVector
import com.pokelike.idle.R

/**
 * Registry aller Hauptziele der Navigation.
 *
 * Ein sealed interface statt loser Routen-Strings sorgt dafuer, dass der
 * Compiler jede Route kennt: Tippfehler in einem String faellt erst zur
 * Laufzeit auf, ein fehlender `when`-Zweig dagegen sofort beim Uebersetzen.
 *
 * Ein neues Hauptziel hinzuzufuegen bedeutet damit genau drei Schritte:
 *   1. Hier ein `data object` ergaenzen.
 *   2. Es in [bottomBarDestinations] eintragen, falls es in die untere Leiste soll.
 *   3. In [PokelikeNavHost] einen `composable`-Eintrag anlegen.
 *
 * @property route Eindeutiger Bezeichner fuer Navigation Compose.
 * @property labelRes Beschriftung. Als Ressourcen-ID, damit die spaetere
 *   Lokalisierung keine Code-Aenderung erfordert.
 * @property selectedIcon Symbol, wenn das Ziel aktiv ist (gefuellte Variante).
 * @property unselectedIcon Symbol im inaktiven Zustand (Outline-Variante). Der
 *   Wechsel zwischen beiden ist das von Material 3 vorgesehene Signal dafuer,
 *   welcher Tab gerade offen ist.
 */
sealed interface PokelikeDestination {

    val route: String

    @get:StringRes
    val labelRes: Int

    val selectedIcon: ImageVector

    val unselectedIcon: ImageVector

    /** Hauptbildschirm mit dem Klick-Button und der Ressourcenanzeige. */
    data object Home : PokelikeDestination {
        override val route: String = "home"
        override val labelRes: Int = R.string.nav_home
        override val selectedIcon: ImageVector = Icons.Filled.TouchApp
        override val unselectedIcon: ImageVector = Icons.Outlined.TouchApp
    }

    /** Gebaeudeliste mit Kaufmoeglichkeit. */
    data object Buildings : PokelikeDestination {
        override val route: String = "buildings"
        override val labelRes: Int = R.string.nav_buildings
        override val selectedIcon: ImageVector = Icons.Filled.Store
        override val unselectedIcon: ImageVector = Icons.Outlined.Store
    }

    /** Upgrade-Liste mit Kaufmoeglichkeit. */
    data object Upgrades : PokelikeDestination {
        override val route: String = "upgrades"
        override val labelRes: Int = R.string.nav_upgrades
        override val selectedIcon: ImageVector = Icons.Filled.Upgrade
        override val unselectedIcon: ImageVector = Icons.Outlined.Upgrade
    }

    /** Prestige mit Punkteuebersicht und Reset. */
    data object Prestige : PokelikeDestination {
        override val route: String = "prestige"
        override val labelRes: Int = R.string.nav_prestige
        override val selectedIcon: ImageVector = Icons.Filled.AutoAwesome
        override val unselectedIcon: ImageVector = Icons.Outlined.AutoAwesome
    }

    companion object {

        /** Ziel, das beim Start der App angezeigt wird. */
        val startDestination: PokelikeDestination = Home

        /**
         * Ziele der unteren Navigationsleiste, in Anzeigereihenfolge.
         *
         * Die Leiste erscheint erst ab zwei Eintraegen (siehe
         * [PokelikeBottomBar]); bei einem einzigen Ziel gaebe es nichts zu
         * waehlen, und sie wuerde nur Hoehe kosten.
         */
        val bottomBarDestinations: List<PokelikeDestination> = listOf(Home, Buildings, Upgrades, Prestige)
    }
}
