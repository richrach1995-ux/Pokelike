package com.pokelike.idle.ui.screens.goals

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.AchievementType
import com.pokelike.idle.domain.model.QuestPeriod
import com.pokelike.idle.domain.model.QuestType
import com.pokelike.idle.ui.components.RewardPart
import com.pokelike.idle.ui.theme.PokelikeTheme

/** Einstiegspunkt des Ziele-Bildschirms im Navigationsgraphen. */
@Composable
fun GoalsRoute(
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GoalsScreen(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onClaim = { quest -> viewModel.onClaim(quest) },
        onBuyProtection = { viewModel.onBuyProtection() },
        modifier = modifier,
    )
}

/** Zustandsloser Ziele-Bildschirm mit Umschalter zwischen Quests und Achievements. */
@Composable
fun GoalsScreen(
    uiState: GoalsUiState,
    onTabSelected: (GoalsTab) -> Unit,
    onClaim: (QuestType) -> Unit,
    onBuyProtection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .padding(horizontal = dimens.spaceMd, vertical = dimens.spaceSm)
                .fillMaxWidth(),
        ) {
            GoalsTab.entries.forEachIndexed { index, tab ->
                SegmentedButton(
                    selected = tab == uiState.selectedTab,
                    onClick = { onTabSelected(tab) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = GoalsTab.entries.size,
                    ),
                ) {
                    Text(
                        text = when (tab) {
                            GoalsTab.QUESTS -> stringResource(
                                R.string.goals_tab_quests,
                                uiState.claimableCount,
                            )

                            GoalsTab.ACHIEVEMENTS -> stringResource(
                                R.string.goals_tab_achievements,
                                uiState.unlockedCount,
                                uiState.totalCount,
                            )
                        },
                    )
                }
            }
        }

        when (uiState.selectedTab) {
            GoalsTab.QUESTS -> QuestList(
                uiState = uiState,
                onClaim = onClaim,
                onBuyProtection = onBuyProtection,
            )

            GoalsTab.ACHIEVEMENTS -> AchievementList(rows = uiState.achievements)
        }
    }
}

/** Quests, nach Zeitraum gegliedert. */
@Composable
private fun QuestList(
    uiState: GoalsUiState,
    onClaim: (QuestType) -> Unit,
    onBuyProtection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        item(key = "streak") {
            StreakCard(row = uiState.streak, onBuyProtection = onBuyProtection)
        }

        uiState.questsByPeriod.forEach { (period, rows) ->
            if (rows.isEmpty()) return@forEach

            item(key = "header:${period.id}") {
                Text(
                    text = stringResource(period.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = dimens.spaceSm),
                )
            }

            items(items = rows, key = { row -> row.type.id }) { row ->
                QuestCard(row = row, onClaim = onClaim)
            }
        }
    }
}

/**
 * Stand der Anmeldeserie mit Kauf des Serienschutzes.
 *
 * Der Kauf fragt nach. Fuenfzig Diamanten sind ein spuerbarer Betrag, und ein
 * Knopf, der ihn ohne Rueckfrage abbucht, waere bei einem Fehlgriff genau die
 * Art von Aergernis, die Spieler das Spiel loeschen laesst. Die Rueckfrage
 * nennt Preis und Wirkung vollstaendig.
 */
@Composable
private fun StreakCard(
    row: StreakRow,
    onBuyProtection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    var isConfirming by rememberSaveable { mutableStateOf(false) }

    if (isConfirming) {
        AlertDialog(
            onDismissRequest = { isConfirming = false },
            title = { Text(text = stringResource(R.string.streak_confirm_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.streak_confirm_message,
                        row.protectionPrice,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isConfirming = false
                        onBuyProtection()
                    },
                ) {
                    Text(text = stringResource(R.string.streak_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirming = false }) {
                    Text(text = stringResource(R.string.streak_cancel))
                }
            },
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = gameColors.elevatedSurface),
    ) {
        Row(
            modifier = Modifier.padding(dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (row.streak > 0) gameColors.coin else gameColors.rarityCommon,
                modifier = Modifier.size(dimens.minTouchTarget),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimens.spaceMd),
            ) {
                Text(
                    text = stringResource(R.string.streak_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.streak_card_current,
                        row.streak,
                        row.longestStreak,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (row.protectionCharges == 0) {
                        stringResource(R.string.streak_card_protection_none)
                    } else {
                        val charges = row.protectionCharges
                        pluralStringResource(
                            R.plurals.streak_card_protection,
                            charges,
                            charges,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = gameColors.diamond,
                )
            }

            Button(
                onClick = { isConfirming = true },
                enabled = row.canBuyProtection,
                modifier = Modifier.padding(start = dimens.spaceSm),
            ) {
                Text(
                    text = if (row.isProtectionFull) {
                        stringResource(R.string.streak_buy_full)
                    } else {
                        stringResource(R.string.streak_buy_protection, row.protectionPrice)
                    },
                )
            }
        }
    }
}

/** Eine Quest mit Fortschrittsbalken und Abholknopf. */
@Composable
private fun QuestCard(
    row: QuestRow,
    onClaim: (QuestType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (row.isClaimed) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(row.type.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                RewardLine(parts = row.reward)

                LinearProgressIndicator(
                    progress = { row.progressFraction },
                    color = if (row.isComplete) gameColors.positive else gameColors.coin,
                    modifier = Modifier
                        .padding(top = dimens.spaceXs)
                        .fillMaxWidth(),
                )
                Text(
                    text = row.progressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                row.isClaimed -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.goals_claimed),
                    tint = gameColors.positive,
                    modifier = Modifier
                        .padding(start = dimens.spaceMd)
                        .size(dimens.minTouchTarget),
                )

                else -> Button(
                    onClick = { onClaim(row.type) },
                    enabled = row.isComplete,
                    modifier = Modifier.padding(start = dimens.spaceMd),
                ) {
                    Text(text = stringResource(R.string.goals_claim))
                }
            }
        }
    }
}

/** Achievements, offene zuerst. */
@Composable
private fun AchievementList(
    rows: List<AchievementRow>,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens
    val gameColors = PokelikeTheme.gameColors

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        items(items = rows, key = { row -> row.type.id }) { row ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (row.isUnlocked) {
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
                        imageVector = if (row.isUnlocked) {
                            Icons.Filled.EmojiEvents
                        } else {
                            Icons.Filled.Lock
                        },
                        contentDescription = null,
                        tint = if (row.isUnlocked) {
                            gameColors.rarityLegendary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(dimens.minTouchTarget),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = dimens.spaceMd),
                    ) {
                        Text(
                            text = stringResource(row.type.nameRes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        RewardLine(parts = row.reward)
                    }
                }
            }
        }
    }
}

/** Belohnung als eine Zeile, etwa "5 Diamanten". */
@Composable
private fun RewardLine(
    parts: List<RewardPart>,
    modifier: Modifier = Modifier,
) {
    if (parts.isEmpty()) return

    // stringResource ist ein Composable und laesst sich nur in inline-Lambdas
    // aufrufen. map ist eines, joinToString nicht - deshalb der Zwischenschritt.
    val texts = parts.map { part -> "${part.amount} ${stringResource(part.labelRes)}" }

    Text(
        text = texts.joinToString(separator = " + "),
        style = MaterialTheme.typography.bodyMedium,
        color = PokelikeTheme.gameColors.diamond,
        modifier = modifier,
    )
}

@Preview(name = "Ziele - Quests", showBackground = true)
@Composable
private fun GoalsScreenPreview() {
    PokelikeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            GoalsScreen(
                uiState = GoalsUiState(
                    selectedTab = GoalsTab.QUESTS,
                    streak = StreakRow(
                        streak = 5,
                        longestStreak = 12,
                        protectionCharges = 1,
                        protectionPrice = "50",
                        canBuyProtection = true,
                        isProtectionFull = false,
                    ),
                    questsByPeriod = mapOf(
                        QuestPeriod.DAILY to listOf(
                            QuestRow(
                                type = QuestType.DAILY_HUNDRED_CLICKS,
                                progressText = "100 / 100",
                                progressFraction = 1f,
                                reward = listOf(
                                    RewardPart(R.string.resource_coins, "5.000K"),
                                    RewardPart(R.string.resource_diamonds, "2"),
                                ),
                                isComplete = true,
                                isClaimed = false,
                            ),
                            QuestRow(
                                type = QuestType.DAILY_TEN_BUILDINGS,
                                progressText = "3 / 10",
                                progressFraction = 0.3f,
                                reward = listOf(RewardPart(R.string.resource_diamonds, "3")),
                                isComplete = false,
                                isClaimed = false,
                            ),
                        ),
                    ),
                    achievements = listOf(
                        AchievementRow(
                            type = AchievementType.FIRST_CLICK,
                            reward = listOf(RewardPart(R.string.resource_diamonds, "5")),
                            isUnlocked = true,
                        ),
                    ),
                    unlockedCount = 1,
                    totalCount = 22,
                    claimableCount = 1,
                ),
                onTabSelected = {},
                onClaim = {},
                onBuyProtection = {},
            )
        }
    }
}
