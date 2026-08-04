package com.pokelike.idle.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokelike.idle.R
import com.pokelike.idle.ui.theme.PokelikeTheme
import com.pokelike.idle.ui.theme.ResourceValueTextStyle

/**
 * Einstiegspunkt des Hauptbildschirms im Navigationsgraphen.
 *
 * Die Trennung in eine `Route`- und eine `Screen`-Ebene ist Absicht: Nur die
 * Route kennt das ViewModel. [HomeScreen] bekommt reine Daten und ist dadurch
 * in Previews und UI-Tests ohne Hilt darstellbar.
 */
@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle statt collectAsState: Die Sammlung wird
    // angehalten, sobald der Bildschirm nicht mehr sichtbar ist. Mit
    // collectAsState liefe sie im Hintergrund weiter und wuerde Arbeit fuer
    // eine Anzeige verrichten, die niemand sieht.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        modifier = modifier,
    )
}

/**
 * Zustandsloser Hauptbildschirm.
 *
 * In diesem Schritt zeigt er den Zustand der Spiel-Engine an und weist damit
 * nach, dass die Kette Hilt -> GameClock -> ViewModel -> StateFlow -> Compose
 * vollstaendig traegt. Klick-Button und Ressourcenanzeige ersetzen diesen
 * Inhalt im naechsten Schritt.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimens.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.cd_app_logo),
            // Das Logo bringt eigene Farben mit und darf nicht eingefaerbt werden.
            tint = Color.Unspecified,
            modifier = Modifier
                .size(dimens.logoSize)
                .clip(CircleShape),
        )

        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = dimens.spaceXs),
        )

        EngineStatusCard(
            uiState = uiState,
            modifier = Modifier.padding(top = dimens.spaceXl),
        )
    }
}

/**
 * Zeigt den Laufzustand der Spiel-Engine.
 *
 * Als eigener Composable ausgelagert, damit die zehnmal pro Sekunde
 * aktualisierten Werte nur diesen Teilbaum neu zeichnen und nicht den gesamten
 * Bildschirm samt Logo und Ueberschriften.
 */
@Composable
private fun EngineStatusCard(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = gameColors.elevatedSurface),
    ) {
        Column(
            modifier = Modifier.padding(dimens.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (uiState.isEngineRunning) {
                        gameColors.positive
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(dimens.statusDotSize),
                    content = {},
                )

                Text(
                    text = stringResource(
                        if (uiState.isEngineRunning) {
                            R.string.home_engine_running
                        } else {
                            R.string.home_engine_paused
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = dimens.spaceSm),
                )
            }

            Text(
                text = stringResource(R.string.home_session_time, uiState.sessionTime),
                style = ResourceValueTextStyle,
                color = gameColors.coin,
                modifier = Modifier.padding(top = dimens.spaceMd),
            )

            Text(
                text = stringResource(R.string.home_tick_count, uiState.tickCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = dimens.spaceXs),
            )
        }
    }
}

@Preview(name = "Home - dunkel", showBackground = true)
@Composable
private fun HomeScreenDarkPreview() {
    PokelikeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                uiState = HomeUiState(
                    isEngineRunning = true,
                    sessionTime = "12:34",
                    tickCount = 7_540L,
                ),
            )
        }
    }
}

@Preview(name = "Home - hell", showBackground = true)
@Composable
private fun HomeScreenLightPreview() {
    PokelikeTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                uiState = HomeUiState(
                    isEngineRunning = false,
                    sessionTime = "01:02:03",
                    tickCount = 36_123L,
                ),
            )
        }
    }
}
