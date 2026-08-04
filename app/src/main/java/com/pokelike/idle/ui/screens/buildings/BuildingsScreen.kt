package com.pokelike.idle.ui.screens.buildings

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.usecases.BuyAmount
import com.pokelike.idle.ui.theme.PokelikeTheme
import com.pokelike.idle.ui.theme.ResourceValueTextStyle

/** Einstiegspunkt der Gebaeudeliste im Navigationsgraphen. */
@Composable
fun BuildingsRoute(
    modifier: Modifier = Modifier,
    viewModel: BuildingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BuildingsScreen(
        uiState = uiState,
        onBuyAmountSelected = viewModel::onBuyAmountSelected,
        onBuy = { building -> viewModel.onBuy(building) },
        modifier = modifier,
    )
}

/**
 * Zustandslose Gebaeudeliste.
 *
 * Die Liste ist eine [LazyColumn] mit stabilem Schluessel je Zeile. Der
 * Schluessel ist nicht optional: Ohne ihn ordnet Compose die Zeilen nach
 * Position zu, und sobald ein neues Gebaeude freigeschaltet wird und die Liste
 * waechst, springen Zustand und Animationen aller nachfolgenden Zeilen um eine
 * Position.
 */
@Composable
fun BuildingsScreen(
    uiState: BuildingsUiState,
    onBuyAmountSelected: (BuyAmount) -> Unit,
    onBuy: (BuildingType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens

    Column(modifier = modifier.fillMaxSize()) {
        BuildingsHeader(
            coins = uiState.coins,
            totalIncome = uiState.totalIncomePerSecond,
            buyAmount = uiState.buyAmount,
            onBuyAmountSelected = onBuyAmountSelected,
            modifier = Modifier.padding(
                start = dimens.spaceMd,
                end = dimens.spaceMd,
                top = dimens.spaceMd,
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(dimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            items(
                items = uiState.rows,
                key = { row -> row.type.id },
            ) { row ->
                BuildingCard(row = row, onBuy = onBuy)
            }
        }
    }
}

/** Kontostand, Gesamtertrag und Auswahl der Kaufmenge. */
@Composable
private fun BuildingsHeader(
    coins: String,
    totalIncome: String,
    buyAmount: BuyAmount,
    onBuyAmountSelected: (BuyAmount) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Column(modifier = modifier) {
        Text(
            text = coins,
            style = ResourceValueTextStyle,
            color = gameColors.coin,
        )
        Text(
            text = stringResource(R.string.buildings_income_per_second, totalIncome),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.padding(top = dimens.spaceSm),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            BuyAmount.entries.forEach { amount ->
                FilterChip(
                    selected = amount == buyAmount,
                    onClick = { onBuyAmountSelected(amount) },
                    label = { Text(text = amount.id) },
                )
            }
        }
    }
}

/** Eine Gebaeudezeile mit Kaufknopf. */
@Composable
private fun BuildingCard(
    row: BuildingRow,
    onBuy: (BuildingType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = gameColors.elevatedSurface),
    ) {
        Row(
            modifier = Modifier.padding(dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = row.type.icon,
                // Name und Beschreibung stehen unmittelbar daneben; eine
                // eigene Beschreibung wuerde von Screenreadern doppelt
                // vorgelesen.
                contentDescription = null,
                tint = gameColors.coin,
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.buildings_owned_and_income,
                        row.owned,
                        row.incomePerSecond,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = gameColors.positive,
                )
            }

            Button(
                onClick = { onBuy(row.type) },
                enabled = row.isAffordable,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(
                            R.string.buildings_buy_count,
                            row.purchasableCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = row.price,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Preview(name = "Gebaeude", showBackground = true)
@Composable
private fun BuildingsScreenPreview() {
    PokelikeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            BuildingsScreen(
                uiState = BuildingsUiState(
                    rows = listOf(
                        BuildingRow(
                            type = BuildingType.FINGER,
                            owned = 12,
                            price = "58.3",
                            purchasableCount = 1,
                            incomePerSecond = "1.20",
                            isAffordable = true,
                        ),
                        BuildingRow(
                            type = BuildingType.CURSOR,
                            owned = 0,
                            price = "100",
                            purchasableCount = 1,
                            incomePerSecond = "0",
                            isAffordable = false,
                        ),
                    ),
                    buyAmount = BuyAmount.ONE,
                    totalIncomePerSecond = "1.20",
                    coins = "845",
                ),
                onBuyAmountSelected = {},
                onBuy = {},
            )
        }
    }
}
