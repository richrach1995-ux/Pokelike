package com.pokelike.idle

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokelike.idle.ui.MainViewModel
import com.pokelike.idle.ui.PokelikeApp
import com.pokelike.idle.ui.theme.PokelikeTheme
import com.pokelike.idle.ui.theme.resolveIsDark
import dagger.hilt.android.AndroidEntryPoint

/**
 * Einzige Activity der App.
 *
 * Warum Single-Activity:
 * Navigation Compose verwaltet den Backstack innerhalb einer Activity. Mehrere
 * Activities wuerden einen zweiten, davon unabhaengigen Stack einfuehren -
 * spaetestens beim Zurueckkehren aus einem Kauf- oder Werbe-Flow fuehrt das zu
 * schwer nachvollziehbarem Verhalten. Ausserdem behaelt ein einziges
 * Activity-Fenster die laufenden Animationen ueber Bildschirmwechsel hinweg.
 *
 * Die Klasse enthaelt bewusst keine Spiellogik. Sie richtet den Splashscreen
 * ein, schaltet auf Edge-to-Edge und uebergibt an Compose.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Muss vor super.onCreate aufgerufen werden, sonst uebernimmt die
        // Bibliothek das Startfenster nicht mehr.
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Haelt den Splashscreen, bis der Spielstand geladen ist. Andernfalls
        // saehe der Spieler fuer einen Moment einen Kontostand von null und
        // danach seinen echten Stand - ein Aufblitzen, das wie ein verlorener
        // Spielstand aussieht.
        splashScreen.setKeepOnScreenCondition { !viewModel.uiState.value.isReady }

        // Zeichnet die App hinter Status- und Navigationsleiste. Das Scaffold in
        // PokelikeApp bekommt die noetigen Innenabstaende automatisch ueber
        // WindowInsets und schneidet dadurch keine Inhalte ab.
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PokelikeTheme(darkTheme = uiState.settings.themeMode.resolveIsDark()) {
                PokelikeApp()
            }
        }
    }
}
