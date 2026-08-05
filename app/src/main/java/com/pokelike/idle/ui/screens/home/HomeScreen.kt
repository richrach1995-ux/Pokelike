package com.pokelike.idle.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.ui.components.ClickButton
import com.pokelike.idle.ui.components.ComboIndicator
import com.pokelike.idle.ui.components.FloatingText
import com.pokelike.idle.ui.components.FloatingTextLayer
import com.pokelike.idle.ui.theme.PokelikeTheme
import com.pokelike.idle.ui.theme.ResourceValueTextStyle
import kotlin.random.Random

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
    val boosters by viewModel.boosters.collectAsStateWithLifecycle()
    val boosterOffers by viewModel.boosterOffers.collectAsStateWithLifecycle()
    val adOffer by viewModel.adOffer.collectAsStateWithLifecycle()

    val haptics = LocalHapticFeedback.current

    // Reine Darstellung: Ob das Blatt offen ist, ueberdauert eine Drehung,
    // gehoert aber nicht in den Spielstand.
    var isShopOpen by rememberSaveable { mutableStateOf(false) }

    if (isShopOpen) {
        BoosterShopSheet(
            offers = boosterOffers,
            adOffer = adOffer,
            onBuy = { type -> viewModel.onBuyBooster(type) },
            onWatchAd = viewModel::onWatchAd,
            onDismiss = {
                isShopOpen = false
                // Ein Fehlerhinweis gilt fuer den Versuch, nicht fuer die
                // Sitzung. Bliebe er stehen, begruesste er den Spieler beim
                // naechsten Oeffnen mit einer Meldung ueber ein Netzproblem,
                // das es laengst nicht mehr gibt.
                viewModel.onAdFailureShown()
            },
        )
    }

    // Die schwebenden Hinweise sind reine Darstellung und gehoeren deshalb
    // hierher und nicht in das ViewModel: Sie ueberleben eine Drehung nicht,
    // und das sollen sie auch nicht - eine halb abgelaufene Animation nach
    // einer Drehung fortzusetzen saehe fehlerhaft aus.
    val floatingTexts = remember { mutableStateListOf<FloatingText>() }
    val nextFloatingId = remember { mutableLongStateOf(0L) }

    HomeScreen(
        uiState = uiState,
        boosters = boosters,
        onOpenBoosterShop = { isShopOpen = true },
        floatingTexts = floatingTexts,
        onClick = {
            val outcome = viewModel.onClick()

            // Haptische Rueckmeldung nur bei kritischen Treffern. Bei jedem
            // Klick zu vibrieren wuerde bei mehreren Klicks pro Sekunde zu
            // einem Dauerbrummen verschmelzen, das den Akku belastet und den
            // besonderen Moment entwertet.
            if (outcome.wasCritical) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            if (floatingTexts.size < MAX_FLOATING_TEXTS) {
                nextFloatingId.longValue += 1L
                floatingTexts.add(
                    FloatingText(
                        id = nextFloatingId.longValue,
                        text = viewModel.formatEarned(outcome),
                        isCritical = outcome.wasCritical,
                        // Rein optische Streuung. Hier genuegt der
                        // Standardgenerator: Das Ergebnis beeinflusst keine
                        // Spielregel und muss deshalb nicht testbar sein.
                        horizontalBias = Random.nextDouble(-1.0, 1.0).toFloat(),
                    ),
                )
            }
        },
        onFloatingTextFinished = { id ->
            floatingTexts.removeAll { it.id == id }
        },
        modifier = modifier,
    )
}

/**
 * Zustandsloser Hauptbildschirm.
 *
 * Zeigt die Kontostaende, den Klick-Button und die laufende Combo. Damit ist
 * die Kette vom Speicher bis zur Anzeige vollstaendig sichtbar:
 * Datenbank -> Repository -> ClickManager -> ViewModel -> Compose.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    boosters: List<BoosterRow>,
    onOpenBoosterShop: () -> Unit,
    floatingTexts: List<FloatingText>,
    onClick: () -> Unit,
    onFloatingTextFinished: (Long) -> Unit,
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
        ResourceRow(uiState = uiState)

        Text(
            text = stringResource(R.string.home_coins_per_click, uiState.coinsPerClick),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = dimens.spaceSm),
        )

        Text(
            text = stringResource(R.string.home_coins_per_second, uiState.coinsPerSecond),
            style = MaterialTheme.typography.labelLarge,
            color = PokelikeTheme.gameColors.positive,
        )

        // Button und schwebende Hinweise teilen sich dieselbe Flaeche, damit
        // der Ertrag genau dort erscheint, wo der Spieler hinsieht.
        Box(
            modifier = Modifier.padding(top = dimens.spaceXl),
            contentAlignment = Alignment.Center,
        ) {
            ClickButton(
                onClick = onClick,
                enabled = uiState.isReady,
            )

            FloatingTextLayer(
                texts = floatingTexts,
                onFinished = onFloatingTextFinished,
            )
        }

        ComboIndicator(
            count = uiState.comboCount,
            multiplierText = uiState.comboMultiplier,
            remainingFraction = uiState.comboRemaining,
            modifier = Modifier.padding(top = dimens.spaceLg),
        )

        BoosterBar(
            boosters = boosters,
            onOpenShop = onOpenBoosterShop,
            modifier = Modifier.padding(top = dimens.spaceLg),
        )
    }
}

/**
 * Zeigt die Kontostaende.
 *
 * Die Betraege kommen bereits formatiert aus dem ViewModel; hier wird nur noch
 * gezeichnet. Ausgelagert, damit ein steigender Kontostand nicht den gesamten
 * Bildschirm neu zeichnen laesst.
 */
@Composable
private fun ResourceRow(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg),
    ) {
        ResourceValue(
            label = stringResource(R.string.resource_coins),
            value = uiState.coins,
            color = gameColors.coin,
        )
        ResourceValue(
            label = stringResource(R.string.resource_diamonds),
            value = uiState.diamonds,
            color = gameColors.diamond,
        )
    }
}

/** Einzelner Kontostand mit Beschriftung. */
@Composable
private fun ResourceValue(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = ResourceValueTextStyle,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Obergrenze gleichzeitig sichtbarer Ertragshinweise.
 *
 * Ohne Deckel koennte schnelles Tippen beliebig viele Animationen gleichzeitig
 * erzeugen. Zwoelf sind mehr, als das Auge ohnehin unterscheiden kann.
 */
private const val MAX_FLOATING_TEXTS = 12

@Preview(name = "Home - dunkel", showBackground = true)
@Composable
private fun HomeScreenDarkPreview() {
    PokelikeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                uiState = HomeUiState(
                    isReady = true,
                    coins = "1.234M",
                    diamonds = "25",
                    coinsPerClick = "12.40",
                    comboCount = 17,
                    comboMultiplier = "1.32x",
                    comboRemaining = 0.6f,
                    coinsPerSecond = "4.82K",
                ),
                boosters = listOf(
                    BoosterRow(
                        type = BoosterType.DOUBLE_INCOME,
                        remainingText = "12:31",
                        progress = 0.42f,
                    ),
                ),
                onOpenBoosterShop = {},
                floatingTexts = emptyList(),
                onClick = {},
                onFloatingTextFinished = {},
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
                    isReady = true,
                    coins = "987.6K",
                    diamonds = "142",
                    coinsPerClick = "1.20",
                    comboCount = 0,
                    comboMultiplier = "1.00x",
                    comboRemaining = 0f,
                    coinsPerSecond = "312.5",
                ),
                boosters = emptyList(),
                onOpenBoosterShop = {},
                floatingTexts = emptyList(),
                onClick = {},
                onFloatingTextFinished = {},
            )
        }
    }
}
