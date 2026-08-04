package com.pokelike.idle.ui.screens.home

import androidx.compose.runtime.Immutable

/**
 * Vollstaendiger Anzeigezustand des Hauptbildschirms.
 *
 * Der Screen liest ausschliesslich aus diesem Objekt und rechnet nichts selbst
 * aus. Das hat zwei Gruende: Der Zustand ist damit in einem Unit-Test ohne
 * Compose pruefbar, und aufwaendige Arbeit wie Zahlen- und Zeitformatierung
 * passiert im ViewModel auf einem Hintergrund-Dispatcher statt waehrend der
 * Composition auf dem UI-Thread.
 *
 * [Immutable] teilt Compose mit, dass sich die Felder nach der Erzeugung nicht
 * mehr aendern. Compose darf Composables dadurch ueberspringen, deren Eingaben
 * gleich geblieben sind.
 *
 * @property isEngineRunning Ob die Spiel-Uhr gerade taktet.
 * @property sessionTime Aktive Spielzeit dieser Sitzung, bereits als `mm:ss`
 *   formatiert.
 * @property tickCount Anzahl verarbeiteter Takte seit Prozessstart.
 */
@Immutable
data class HomeUiState(
    val isEngineRunning: Boolean = false,
    val sessionTime: String = "00:00",
    val tickCount: Long = 0L,
)
