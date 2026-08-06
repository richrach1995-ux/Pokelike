package com.pokelike.idle.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.ThemeMode
import com.pokelike.idle.ui.theme.PokelikeTheme

/** Einstiegspunkt der Einstellungen im Navigationsgraphen. */
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onVibrationChanged = viewModel::onVibrationChanged,
        onResetGame = viewModel::onResetGame,
        modifier = modifier,
    )
}

/** Zustandsloser Einstellungsbildschirm. */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onResetGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        item(key = "appearance") {
            SettingsSection(titleRes = R.string.settings_section_appearance) {
                ThemeChooser(
                    selected = uiState.themeMode,
                    onSelected = onThemeModeSelected,
                )
            }
        }

        item(key = "game") {
            SettingsSection(titleRes = R.string.settings_section_game) {
                SwitchRow(
                    titleRes = R.string.settings_vibration,
                    descriptionRes = R.string.settings_vibration_description,
                    checked = uiState.vibrationEnabled,
                    onCheckedChange = onVibrationChanged,
                )
            }
        }

        item(key = "save") {
            SettingsSection(titleRes = R.string.settings_section_save) {
                ResetRow(onResetGame = onResetGame)
            }
        }

        item(key = "about") {
            SettingsSection(titleRes = R.string.settings_section_about) {
                InfoRow(
                    labelRes = R.string.settings_version,
                    value = uiState.versionName,
                )
                InfoRow(
                    labelRes = R.string.settings_save_version,
                    value = uiState.saveVersion.toString(),
                )
            }
        }
    }
}

/**
 * Ein Abschnitt mit Ueberschrift.
 *
 * Die Gliederung ist kein Schmuck: Ohne sie stuenden Darstellung, Bedienung und
 * das Loeschen des Spielstands als gleichrangige Zeilen untereinander - und der
 * folgenschwerste Eintrag saehe aus wie jeder andere.
 */
@Composable
private fun SettingsSection(
    titleRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dimens = PokelikeTheme.dimens

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = dimens.spaceXs),
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = PokelikeTheme.gameColors.elevatedSurface,
            ),
        ) {
            Column(modifier = Modifier.padding(dimens.spaceMd)) { content() }
        }
    }
}

/** Auswahl der Darstellungsvariante. */
@Composable
private fun ThemeChooser(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = ThemeMode.entries.size,
                ),
            ) {
                Text(text = stringResource(mode.labelRes))
            }
        }
    }
}

/** Eine Zeile mit Beschriftung, Erklaerung und Schalter. */
@Composable
private fun SwitchRow(
    titleRes: Int,
    descriptionRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = PokelikeTheme.dimens

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = dimens.spaceMd),
        )
    }
}

/**
 * Das Loeschen des Spielstands, mit Rueckfrage.
 *
 * Der Knopf traegt die Fehlerfarbe und die Rueckfrage benennt den Verlust
 * vollstaendig. Das ist die einzige Stelle der App, an der sich Fortschritt
 * unwiederbringlich vernichten laesst - anders als beim Prestige gibt es hier
 * keinen Gegenwert.
 */
@Composable
private fun ResetRow(
    onResetGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isConfirming by rememberSaveable { mutableStateOf(false) }

    if (isConfirming) {
        AlertDialog(
            onDismissRequest = { isConfirming = false },
            title = { Text(text = stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(text = stringResource(R.string.settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isConfirming = false
                        onResetGame()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isConfirming = false }) {
                    Text(text = stringResource(R.string.settings_reset_cancel))
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_reset_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { isConfirming = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.padding(top = PokelikeTheme.dimens.spaceSm),
        ) {
            Text(text = stringResource(R.string.settings_reset_action))
        }
    }
}

/** Eine Zeile mit Beschriftung und unveraenderlichem Wert. */
@Composable
private fun InfoRow(
    labelRes: Int,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = PokelikeTheme.dimens.spaceXs),
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(name = "Einstellungen", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PokelikeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(
                uiState = SettingsUiState(
                    themeMode = ThemeMode.DARK,
                    vibrationEnabled = true,
                    versionName = "0.1.0-debug",
                    saveVersion = 7,
                ),
                onThemeModeSelected = {},
                onVibrationChanged = {},
                onResetGame = {},
            )
        }
    }
}
