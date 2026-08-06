package com.pokelike.idle.ui.screens.settings

import androidx.compose.runtime.Immutable
import com.pokelike.idle.domain.model.ThemeMode

/**
 * Anzeigezustand der Einstellungen.
 *
 * **Warum hier nur zwei Schalter stehen, obwohl [com.pokelike.idle.domain.model.GameSettings]
 * mehr Felder hat.** Musik, Klangeffekte und die Bildratenanzeige sind im
 * Datenmodell vorhanden, aber es gibt bisher nichts, was sie ausliest - kein
 * Ton, keine Bildratenanzeige. Ein Schalter, der nichts bewirkt, ist schlimmer
 * als ein fehlender: Der Spieler stellt ihn um, hoert keinen Unterschied und
 * haelt die App fuer kaputt. Sie kommen dazu, sobald es etwas zu schalten gibt.
 *
 * @property themeMode Gewaehlte Darstellungsvariante.
 * @property vibrationEnabled Haptische Rueckmeldung bei kritischen Treffern.
 * @property versionName Version der installierten App.
 * @property saveVersion Version des Spielstandformats. Fuer Fehlerberichte:
 *   Sie sagt mehr ueber einen kaputten Spielstand aus als die App-Version.
 */
@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val vibrationEnabled: Boolean = true,
    val versionName: String = "",
    val saveVersion: Int = 0,
)
