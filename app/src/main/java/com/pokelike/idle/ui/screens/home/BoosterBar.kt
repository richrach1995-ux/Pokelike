package com.pokelike.idle.ui.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pokelike.idle.R
import com.pokelike.idle.ui.theme.PokelikeTheme

/**
 * Leiste der laufenden Booster mit Zugang zum Angebot.
 *
 * Sie steht immer da, auch ohne laufenden Booster: Waere sie nur dann
 * sichtbar, wenn schon einer laeuft, faende ein Spieler das Angebot nie - und
 * der Bildschirm spraenge bei jedem Ablauf um die Hoehe der Leiste.
 *
 * Angezeigt wird je Booster nur Sinnbild und Restlaufzeit. Vier Namen
 * nebeneinander waeren auf einem schmalen Geraet breiter als der Bildschirm,
 * und den Namen kennt der Spieler ohnehin - er hat den Booster gerade gekauft.
 */
@Composable
fun BoosterBar(
    boosters: List<BoosterRow>,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        FilledTonalIconButton(onClick = onOpenShop) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.booster_shop_open),
            )
        }

        if (boosters.isEmpty()) {
            Text(
                text = stringResource(R.string.booster_none_active),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Waagerecht scrollbar: Vier gleichzeitig laufende Booster sind
            // zusammen breiter als ein schmales Geraet, und der letzte waere
            // sonst abgeschnitten.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                boosters.forEach { row -> BoosterTimer(row = row) }
            }
        }
    }
}

/** Sinnbild, Restlaufzeit und ablaufender Balken eines Boosters. */
@Composable
private fun BoosterTimer(
    row: BoosterRow,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = row.type.icon,
            contentDescription = stringResource(row.type.nameRes),
            tint = gameColors.rarityEpic,
            modifier = Modifier.size(dimens.boosterIconSize),
        )

        Column(
            modifier = Modifier
                .padding(start = dimens.spaceXs)
                .width(dimens.boosterTimerWidth),
        ) {
            Text(
                text = row.remainingText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LinearProgressIndicator(
                progress = { row.progress },
                color = gameColors.rarityEpic,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
