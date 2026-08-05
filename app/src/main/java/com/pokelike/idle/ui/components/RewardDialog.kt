package com.pokelike.idle.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pokelike.idle.R
import com.pokelike.idle.ui.theme.PokelikeTheme

/**
 * Eine Zeile der Belohnungsmeldung.
 *
 * @property titleRes Woher die Belohnung stammt, etwa der Achievement-Name.
 * @property reward Was gutgeschrieben wurde.
 */
@Immutable
data class RewardLineItem(
    @StringRes val titleRes: Int,
    val reward: List<RewardPart>,
)

/**
 * Sammelmeldung ueber gutgeschriebene Belohnungen.
 *
 * Zeigt alle anliegenden Belohnungen in einem Dialog statt in mehreren
 * nacheinander. Ein Spieler, der nach einer laengeren Pause zurueckkehrt, hat
 * moeglicherweise mehrere Achievements auf einmal erreicht; einzelne Dialoge
 * waeren dann eine Kette von Bestaetigungen, bevor er das Spiel sieht.
 *
 * Der Wortlaut sagt bewusst "erhalten" und nicht "einsammeln": Die Betraege
 * sind zu diesem Zeitpunkt bereits auf dem Konto.
 */
@Composable
fun RewardDialog(
    items: List<RewardLineItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(text = stringResource(R.string.reward_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                items.forEach { item ->
                    Text(
                        text = stringResource(item.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = dimens.spaceSm),
                    )
                    // Erst die Ressourcennamen aufloesen, dann zusammensetzen:
                    // stringResource ist ein Composable und laesst sich nur in
                    // inline-Lambdas aufrufen - map ist eines, joinToString
                    // nicht.
                    val parts = item.reward.map { part ->
                        "+" + part.amount + " " + stringResource(part.labelRes)
                    }
                    Text(
                        text = parts.joinToString(separator = "  "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = gameColors.diamond,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.reward_confirm))
            }
        },
    )
}
