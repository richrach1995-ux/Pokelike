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
 * Alle Betraege stehen bereits als fertige Zeichenketten hier. Sie im
 * Composable zu formatieren wuerde bedeuten, bei jedem Klick eine Zeichenkette
 * auf dem UI-Thread zu erzeugen - bei einem Spiel mit mehreren Klicks pro
 * Sekunde genau der falsche Ort.
 *
 * [Immutable] teilt Compose mit, dass sich die Felder nach der Erzeugung nicht
 * mehr aendern. Compose darf Composables dadurch ueberspringen, deren Eingaben
 * gleich geblieben sind.
 *
 * @property isReady Ob der Spielstand geladen ist. Solange nicht, bleibt der
 *   Klick-Button abgeschaltet - ein Klick auf einen ungeladenen Stand wuerde
 *   auf dem leeren Ausgangszustand rechnen.
 * @property coins Muenzstand, formatiert.
 * @property diamonds Diamantenstand, formatiert.
 * @property coinsPerClick Erwarteter Ertrag je Klick, formatiert. Bewusst der
 *   Erwartungswert und nicht das letzte Ergebnis: Ein bei jedem kritischen
 *   Treffer springender Wert taugt nicht zur Beurteilung von Upgrades.
 * @property comboCount Klicks in Folge.
 * @property comboMultiplier Wirksamer Combo-Faktor, formatiert.
 * @property comboRemaining Verbleibender Anteil des Combo-Fensters.
 * @property coinsPerSecond Leerlaufeinkommen aus Gebaeuden, formatiert.
 * @property vibrationEnabled Ob bei kritischen Treffern geruettelt werden soll.
 *   Gehoert hierher und nicht in den Composable: Der Bildschirm liest
 *   ausschliesslich aus diesem Objekt und fragt keine Einstellung selbst ab.
 */
@Immutable
data class HomeUiState(
    val isReady: Boolean = false,
    val coins: String = "0",
    val diamonds: String = "0",
    val coinsPerClick: String = "0",
    val comboCount: Int = 0,
    val comboMultiplier: String = "1.00x",
    val comboRemaining: Float = 0f,
    val coinsPerSecond: String = "0",
    val vibrationEnabled: Boolean = true,
)
