package com.pokelike.idle.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pokelike.idle.R
import com.pokelike.idle.ui.theme.PokelikeTheme
import com.pokelike.idle.ui.theme.ResourceValueTextStyle

/**
 * Willkommensdialog nach einer Abwesenheit.
 *
 * Ohne diese Rueckmeldung waere der Offline-Ertrag fuer den Spieler
 * unsichtbar - der Kontostand waere nur irgendwie hoeher. Genau dieser Moment
 * ist der Grund, warum Spieler zurueckkehren, und er gehoert deshalb sichtbar
 * gemacht.
 *
 * Bei erreichter Obergrenze erscheint ein zusaetzlicher Hinweis. Er ist
 * ehrlich - der Spieler hat tatsaechlich Ertrag verloren - und zugleich der
 * natuerlichste Anlass, spaeter einen Offline-Booster anzubieten.
 *
 * @param earnedText Bereits formatierter Ertrag.
 * @param durationText Bereits formatierte Abwesenheitsdauer.
 * @param wasCapped Ob die Obergrenze gegriffen hat.
 */
@Composable
fun OfflineProgressDialog(
    earnedText: String,
    durationText: String,
    wasCapped: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(text = stringResource(R.string.offline_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.offline_duration, durationText),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = earnedText,
                    style = ResourceValueTextStyle,
                    color = gameColors.coin,
                    modifier = Modifier.padding(top = dimens.spaceSm),
                )
                if (wasCapped) {
                    Text(
                        text = stringResource(R.string.offline_capped),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = dimens.spaceSm),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.offline_collect))
            }
        },
    )
}
