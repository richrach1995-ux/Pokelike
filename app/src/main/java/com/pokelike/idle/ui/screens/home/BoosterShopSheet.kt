package com.pokelike.idle.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.ui.theme.PokelikeTheme

/**
 * Das Booster-Angebot als Blatt von unten.
 *
 * Ein Blatt und kein eigener Bildschirm: Booster werden waehrend des Spielens
 * gekauft, und ein Bildschirmwechsel wuerde den Spieler aus der laufenden
 * Sitzung reissen - inklusive Combo, die dabei ablaufen wuerde.
 *
 * Der Preis steht auf dem Knopf. Eine zusaetzliche Rueckfrage gibt es hier
 * bewusst nicht: Anders als beim Serienschutz sieht der Spieler die Wirkung
 * sofort in der Booster-Leiste, und bei einem Angebot, das er selbst geoeffnet
 * hat, waere die Rueckfrage eine Huerde ohne Erkenntnisgewinn.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoosterShopSheet(
    offers: List<BoosterOffer>,
    adOffer: AdOffer,
    onBuy: (BoosterType) -> Unit,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = dimens.spaceMd)
                .padding(bottom = dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
            Text(
                text = stringResource(R.string.booster_shop_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Das Videoangebot steht oben und nicht bei den Kaeufen: Es ist der
            // Weg, der niemanden etwas kostet, und Spieler, die nie zahlen,
            // sollen ihn zuerst sehen.
            AdOfferCard(offer = adOffer, onWatchAd = onWatchAd)

            offers.forEach { offer ->
                BoosterOfferRow(offer = offer, onBuy = onBuy)
            }
        }
    }
}

/**
 * Das Belohnungsvideo als eigene Karte.
 *
 * Der Knopf hat vier Zustaende, und alle vier sind sichtbar zu machen: bereit,
 * laedt, laeuft gerade, Wartezeit. Ein Knopf, der bei drei davon gleich
 * aussieht, laesst den Spieler tippen und nichts geschehen sehen - der
 * haeufigste Grund, warum Videoangebote als kaputt wahrgenommen werden.
 */
@Composable
private fun AdOfferCard(
    offer: AdOffer,
    onWatchAd: () -> Unit,
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
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = gameColors.positive,
                modifier = Modifier.size(dimens.minTouchTarget),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.spaceMd),
            ) {
                Text(
                    text = stringResource(R.string.ad_free_booster_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.ad_free_booster_description,
                        stringResource(offer.rewardedBooster.nameRes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (offer.lastFailed) {
                    Text(
                        text = stringResource(R.string.ad_failed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            when {
                // Der laufende Ladevorgang bekommt einen eigenen Anzeiger statt
                // eines abgeblendeten Knopfes: Der Spieler soll erkennen, dass
                // etwas geschieht und Warten sich lohnt.
                offer.isLoading || offer.isShowing -> CircularProgressIndicator(
                    modifier = Modifier.size(dimens.boosterIconSize),
                )

                else -> Button(
                    onClick = onWatchAd,
                    enabled = offer.isReady,
                ) {
                    Text(
                        text = offer.cooldownText
                            ?: stringResource(R.string.ad_watch),
                    )
                }
            }
        }
    }
}

/** Ein Angebot mit Beschreibung, Laufzeit und Kaufknopf. */
@Composable
private fun BoosterOfferRow(
    offer: BoosterOffer,
    onBuy: (BoosterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = offer.type.icon,
            contentDescription = null,
            tint = gameColors.rarityEpic,
            modifier = Modifier.size(dimens.minTouchTarget),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimens.spaceMd),
        ) {
            Text(
                text = stringResource(offer.type.nameRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(offer.type.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                // Laeuft bereits einer, steht dort die Restlaufzeit statt der
                // Grundlaufzeit: Der Spieler soll sehen, was er verlaengert.
                text = offer.remainingText?.let { remaining ->
                    stringResource(R.string.booster_running, remaining)
                } ?: stringResource(R.string.booster_duration, offer.durationText),
                style = MaterialTheme.typography.labelMedium,
                color = if (offer.remainingText == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    gameColors.positive
                },
            )
        }

        Button(
            onClick = { onBuy(offer.type) },
            enabled = offer.canBuy,
        ) {
            Text(
                text = if (offer.isAtMaximum) {
                    stringResource(R.string.booster_at_maximum)
                } else {
                    stringResource(R.string.booster_buy, offer.priceText)
                },
            )
        }
    }
}
