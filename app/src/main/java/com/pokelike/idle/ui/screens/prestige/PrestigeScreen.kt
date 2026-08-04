package com.pokelike.idle.ui.screens.prestige

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokelike.idle.R
import com.pokelike.idle.ui.theme.PokelikeTheme
import com.pokelike.idle.ui.theme.ResourceValueTextStyle

/** Einstiegspunkt des Prestige-Bildschirms im Navigationsgraphen. */
@Composable
fun PrestigeRoute(
    modifier: Modifier = Modifier,
    viewModel: PrestigeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PrestigeScreen(
        uiState = uiState,
        onPrestige = { viewModel.onPrestige() },
        modifier = modifier,
    )
}

/**
 * Zustandsloser Prestige-Bildschirm.
 *
 * Der Reset laeuft ausschliesslich ueber einen Bestaetigungsdialog. Er ist
 * unumkehrbar und loescht den gesamten Gebaeudeausbau - ein versehentliches
 * Antippen waere der schwerwiegendste Fehlgriff, den das Spiel zulaesst.
 */
@Composable
fun PrestigeScreen(
    uiState: PrestigeUiState,
    onPrestige: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    var showConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = gameColors.rarityLegendary,
            modifier = Modifier.size(dimens.logoSize),
        )

        Text(
            text = uiState.currentPoints,
            style = ResourceValueTextStyle,
            color = gameColors.rarityLegendary,
            modifier = Modifier.padding(top = dimens.spaceMd),
        )
        Text(
            text = stringResource(R.string.prestige_points_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PrestigeSummaryCard(
            uiState = uiState,
            modifier = Modifier.padding(top = dimens.spaceLg),
        )

        ProgressCard(
            uiState = uiState,
            modifier = Modifier.padding(top = dimens.spaceMd),
        )

        Button(
            onClick = { showConfirmation = true },
            enabled = uiState.canPrestige,
            modifier = Modifier
                .padding(top = dimens.spaceLg)
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.prestige_action, uiState.pointsOnReset),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (!uiState.canPrestige) {
            Text(
                text = stringResource(R.string.prestige_not_ready),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = dimens.spaceSm),
            )
        }
    }

    if (showConfirmation) {
        PrestigeConfirmationDialog(
            uiState = uiState,
            onConfirm = {
                showConfirmation = false
                onPrestige()
            },
            onDismiss = { showConfirmation = false },
        )
    }
}

/** Vorher-Nachher-Vergleich des Bonus. */
@Composable
private fun PrestigeSummaryCard(
    uiState: PrestigeUiState,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = gameColors.elevatedSurface),
    ) {
        Column(modifier = Modifier.padding(dimens.spaceMd)) {
            LabelledValue(
                label = stringResource(R.string.prestige_current_bonus),
                value = uiState.currentBonus,
            )
            LabelledValue(
                label = stringResource(R.string.prestige_bonus_after_reset),
                value = uiState.bonusAfterReset,
                highlight = true,
            )
            LabelledValue(
                label = stringResource(R.string.prestige_runs),
                value = uiState.prestigeCount.toString(),
            )
        }
    }
}

/** Fortschritt bis zum naechsten Punkt. */
@Composable
private fun ProgressCard(
    uiState: PrestigeUiState,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = gameColors.elevatedSurface),
    ) {
        Column(modifier = Modifier.padding(dimens.spaceMd)) {
            LabelledValue(
                label = stringResource(R.string.prestige_lifetime_coins),
                value = uiState.lifetimeCoins,
            )
            LabelledValue(
                label = stringResource(R.string.prestige_next_point_at),
                value = uiState.coinsForNextPoint,
            )

            LinearProgressIndicator(
                progress = { uiState.progressToNextPoint },
                color = gameColors.rarityLegendary,
                modifier = Modifier
                    .padding(top = dimens.spaceSm)
                    .fillMaxWidth(),
            )
        }
    }
}

/** Beschriftung links, Wert rechts. */
@Composable
private fun LabelledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimens.spaceXs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (highlight) {
                gameColors.rarityLegendary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/**
 * Bestaetigung vor dem Reset.
 *
 * Zaehlt ausdruecklich auf, was verloren geht. Ein Dialog, der nur den Gewinn
 * nennt, waere im Ergebnis eine Falle: Der Spieler bestaetigt und stellt erst
 * danach fest, dass sein gesamter Ausbau weg ist.
 */
@Composable
private fun PrestigeConfirmationDialog(
    uiState: PrestigeUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.prestige_confirm_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.prestige_confirm_message,
                    uiState.pointsOnReset,
                    uiState.bonusAfterReset,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.prestige_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.prestige_cancel))
            }
        },
    )
}

@Preview(name = "Prestige - bereit", showBackground = true)
@Composable
private fun PrestigeScreenReadyPreview() {
    PokelikeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PrestigeScreen(
                uiState = PrestigeUiState(
                    currentPoints = "12",
                    pointsOnReset = "7",
                    currentBonus = "+24 %",
                    bonusAfterReset = "+38 %",
                    lifetimeCoins = "3.610T",
                    coinsForNextPoint = "4.000T",
                    progressToNextPoint = 0.72f,
                    prestigeCount = 3,
                    canPrestige = true,
                ),
                onPrestige = {},
            )
        }
    }
}

@Preview(name = "Prestige - noch nicht bereit", showBackground = true)
@Composable
private fun PrestigeScreenNotReadyPreview() {
    PokelikeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PrestigeScreen(
                uiState = PrestigeUiState(
                    currentPoints = "0",
                    pointsOnReset = "0",
                    currentBonus = "+0 %",
                    bonusAfterReset = "+0 %",
                    lifetimeCoins = "845.2M",
                    coinsForNextPoint = "10.00B",
                    progressToNextPoint = 0.31f,
                    prestigeCount = 0,
                    canPrestige = false,
                ),
                onPrestige = {},
            )
        }
    }
}
