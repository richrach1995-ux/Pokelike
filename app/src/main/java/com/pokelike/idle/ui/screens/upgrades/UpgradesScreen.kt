package com.pokelike.idle.ui.screens.upgrades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.UpgradeType
import com.pokelike.idle.ui.theme.PokelikeTheme
import com.pokelike.idle.ui.theme.ResourceValueTextStyle

/** Einstiegspunkt der Upgrade-Liste im Navigationsgraphen. */
@Composable
fun UpgradesRoute(
    modifier: Modifier = Modifier,
    viewModel: UpgradesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UpgradesScreen(
        uiState = uiState,
        onBuy = { upgrade -> viewModel.onBuy(upgrade) },
        modifier = modifier,
    )
}

/** Zustandslose Upgrade-Liste. */
@Composable
fun UpgradesScreen(
    uiState: UpgradesUiState,
    onBuy: (UpgradeType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(
                start = dimens.spaceMd,
                end = dimens.spaceMd,
                top = dimens.spaceMd,
            ),
        ) {
            Text(
                text = uiState.coins,
                style = ResourceValueTextStyle,
                color = gameColors.coin,
            )
            Text(
                text = stringResource(
                    R.string.upgrades_progress,
                    uiState.ownedCount,
                    uiState.totalCount,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(dimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            items(
                items = uiState.rows,
                // Stabiler Schluessel: Ohne ihn ordnet Compose die Zeilen nach
                // Position zu, und beim Kauf eines Upgrades - das die Liste
                // umsortiert - springen Zustand und Animationen der uebrigen.
                key = { row -> row.type.id },
            ) { row ->
                UpgradeCard(row = row, onBuy = onBuy)
            }
        }
    }
}

/** Eine Upgrade-Zeile mit Kaufknopf oder Besitzhaken. */
@Composable
private fun UpgradeCard(
    row: UpgradeRow,
    onBuy: (UpgradeType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (row.isOwned) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                gameColors.elevatedSurface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = row.type.category.icon,
                // Name und Wirkung stehen unmittelbar daneben; eine eigene
                // Beschreibung wuerde von Screenreadern doppelt vorgelesen.
                contentDescription = null,
                tint = if (row.isOwned) gameColors.positive else gameColors.coin,
                modifier = Modifier.size(dimens.minTouchTarget),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.spaceMd),
            ) {
                Text(
                    text = stringResource(row.type.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(row.type.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (row.isOwned) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.upgrades_owned),
                    tint = gameColors.positive,
                    modifier = Modifier.size(dimens.minTouchTarget),
                )
            } else {
                Button(
                    onClick = { onBuy(row.type) },
                    enabled = row.isAffordable,
                ) {
                    Text(
                        text = row.price,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Preview(name = "Upgrades", showBackground = true)
@Composable
private fun UpgradesScreenPreview() {
    PokelikeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            UpgradesScreen(
                uiState = UpgradesUiState(
                    rows = listOf(
                        UpgradeRow(
                            type = UpgradeType.STRONGER_FINGERS,
                            price = "100",
                            isOwned = false,
                            isAffordable = true,
                        ),
                        UpgradeRow(
                            type = UpgradeType.IRON_FINGERS,
                            price = "5.000K",
                            isOwned = false,
                            isAffordable = false,
                        ),
                        UpgradeRow(
                            type = UpgradeType.NIMBLE_FINGERS,
                            price = "500",
                            isOwned = true,
                            isAffordable = true,
                        ),
                    ),
                    coins = "1.234K",
                    ownedCount = 1,
                    totalCount = 19,
                ),
                onBuy = {},
            )
        }
    }
}
