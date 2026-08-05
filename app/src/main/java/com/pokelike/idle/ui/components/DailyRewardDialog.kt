package com.pokelike.idle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.pokelike.idle.R
import com.pokelike.idle.ui.theme.PokelikeTheme

/**
 * Anzeigezustand des Tagesbonus.
 *
 * Alle Zahlen sind bereits formatiert und alle Texte als Ressourcen-ID
 * hinterlegt: Ein ViewModel hat keinen Context und darf keine Zeichenketten
 * aufloesen.
 *
 * @property streak Laenge der Serie nach dem Abholen.
 * @property cycleDay Tag im Zyklus, eins bis [cycleLength].
 * @property cycleLength Laenge des Zyklus.
 * @property reward Was die Abholung einbringt.
 * @property protectionUsed Verbrauchte Ladungen Serienschutz.
 * @property streakBroken Ob die Serie neu beginnt.
 */
@Immutable
data class DailyRewardUiState(
    val streak: Int,
    val cycleDay: Int,
    val cycleLength: Int,
    val reward: List<RewardPart>,
    val protectionUsed: Int,
    val streakBroken: Boolean,
)

/**
 * Dialog des taeglichen Bonus.
 *
 * Die Leiste der Zyklustage ist der eigentliche Inhalt: Sie zeigt, dass der
 * heutige Tag Teil einer Folge ist und dass der letzte Tag der grosse ist. Ein
 * Dialog, der nur den heutigen Betrag nennt, waere eine Gutschrift ohne
 * Erzaehlung - und genau die Erzaehlung ist der Grund, warum Spieler
 * wiederkommen.
 *
 * Der Dialog laesst sich wegtippen, ohne abzuholen. Der Anspruch bleibt dann
 * bestehen; er verfaellt nicht.
 */
@Composable
fun DailyRewardDialog(
    uiState: DailyRewardUiState,
    onClaim: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(text = stringResource(R.string.daily_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.daily_streak, uiState.streak),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                CycleStrip(
                    currentDay = uiState.cycleDay,
                    cycleLength = uiState.cycleLength,
                    modifier = Modifier.padding(vertical = dimens.spaceMd),
                )

                // Erst die Namen aufloesen, dann zusammensetzen: stringResource
                // ist ein Composable und laesst sich nur in inline-Lambdas
                // aufrufen - map ist eines, joinToString nicht.
                val rewardTexts = uiState.reward.map { part ->
                    "+" + part.amount + " " + stringResource(part.labelRes)
                }
                Text(
                    text = rewardTexts.joinToString(separator = "  "),
                    style = MaterialTheme.typography.titleLarge,
                    color = gameColors.coin,
                )

                // Der Hinweis erscheint nur, wenn er zutrifft. Der Spieler soll
                // vor dem Abholen wissen, ob sein Schutz greift oder die Serie
                // neu beginnt - hinterher waere die Auskunft wertlos.
                when {
                    uiState.streakBroken -> Note(
                        text = stringResource(R.string.daily_streak_broken),
                    )

                    uiState.protectionUsed > 0 -> {
                        val days = uiState.protectionUsed
                        Note(
                            text = pluralStringResource(
                                R.plurals.daily_protection_used,
                                days,
                                days,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClaim) {
                Text(text = stringResource(R.string.daily_claim))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.daily_later))
            }
        },
    )
}

/** Ein kurzer Hinweis unterhalb des Betrags. */
@Composable
private fun Note(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = PokelikeTheme.dimens.spaceSm),
    )
}

/**
 * Die Tage des Zyklus als Leiste.
 *
 * Bereits abgeholte Tage tragen einen Haken, der heutige ist hervorgehoben,
 * kommende bleiben blass. Der letzte Tag ist zusaetzlich eingefaerbt - er ist
 * das Ziel, auf das die Leiste hinauslaeuft.
 */
@Composable
private fun CycleStrip(
    currentDay: Int,
    cycleLength: Int,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs, Alignment.CenterHorizontally),
    ) {
        (1..cycleLength).forEach { day ->
            val isPast = day < currentDay
            val isCurrent = day == currentDay
            val isFinal = day == cycleLength

            val background = when {
                isCurrent -> gameColors.coin
                isPast -> gameColors.positive
                isFinal -> gameColors.rarityLegendary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val foreground = when {
                isCurrent || isPast || isFinal -> MaterialTheme.colorScheme.scrim
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .size(dimens.cycleTileSize)
                    .background(
                        color = background,
                        shape = RoundedCornerShape(dimens.cornerRadiusSmall),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isPast) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.daily_day_claimed, day),
                        tint = foreground,
                    )
                } else {
                    Text(
                        text = day.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = foreground,
                    )
                }
            }
        }
    }
}

@Preview(name = "Tagesbonus", showBackground = true)
@Composable
private fun DailyRewardDialogPreview() {
    PokelikeTheme(darkTheme = true) {
        DailyRewardDialog(
            uiState = DailyRewardUiState(
                streak = 4,
                cycleDay = 4,
                cycleLength = 7,
                reward = listOf(
                    RewardPart(R.string.resource_coins, "12.4M"),
                    RewardPart(R.string.resource_diamonds, "7"),
                ),
                protectionUsed = 0,
                streakBroken = false,
            ),
            onClaim = {},
            onDismiss = {},
        )
    }
}
