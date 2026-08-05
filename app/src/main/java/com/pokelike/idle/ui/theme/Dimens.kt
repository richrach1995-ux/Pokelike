package com.pokelike.idle.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Abstandsraster und wiederkehrende Groessen.
 *
 * Ein festes Raster statt frei gewaehlter dp-Werte im Screen sorgt dafuer, dass
 * Bildschirme optisch zusammenpassen, auch wenn sie zu unterschiedlichen
 * Zeitpunkten entstehen. Ausserdem laesst sich das Layout fuer Tablets spaeter
 * an einer Stelle skalieren, statt an hunderten.
 */
@Immutable
data class Dimens(
    /** 4 dp - Abstand innerhalb eng zusammengehoerender Elemente. */
    val spaceXs: Dp = 4.dp,
    /** 8 dp - Standardabstand zwischen Elementen einer Gruppe. */
    val spaceSm: Dp = 8.dp,
    /** 16 dp - Abstand zwischen Gruppen, Standard-Innenabstand von Karten. */
    val spaceMd: Dp = 16.dp,
    /** 24 dp - Abstand zwischen Abschnitten. */
    val spaceLg: Dp = 24.dp,
    /** 32 dp - Grosszuegiger Abstand, etwa oberhalb des Klick-Buttons. */
    val spaceXl: Dp = 32.dp,

    /** Eckenradius von Karten und Flaechen. */
    val cornerRadius: Dp = 20.dp,
    /** Eckenradius kleiner Elemente wie Chips oder Badges. */
    val cornerRadiusSmall: Dp = 12.dp,

    /**
     * Mindestgroesse fuer antippbare Flaechen.
     *
     * 48 dp ist die Vorgabe der Android-Accessibility-Richtlinien. Kleinere
     * Ziele werden auf Geraeten mit grossen Displays regelmaessig verfehlt.
     */
    val minTouchTarget: Dp = 48.dp,

    /** Kantenlaenge des Logos auf dem Startbildschirm. */
    val logoSize: Dp = 120.dp,
    /** Durchmesser kleiner Statusanzeigen (z. B. der Engine-Zustandspunkt). */
    val statusDotSize: Dp = 10.dp,

    /**
     * Durchmesser des Klick-Buttons.
     *
     * 220 dp sind bewusst gross. Der Button wird ueber lange Sitzungen
     * tausendfach getroffen; jede Fehlbetaetigung faellt dabei staerker ins
     * Gewicht als der gewonnene Platz. Auf kleinen Geraeten bleibt daneben
     * genug Raum fuer Kontostand und Combo-Anzeige.
     */
    val clickButtonSize: Dp = 220.dp,

    /** Steighoehe des schwebenden Textes ueber dem Klick-Button. */
    val floatingTextRise: Dp = 120.dp,

    /** Hoehe des ablaufenden Combo-Balkens. */
    val comboBarHeight: Dp = 6.dp,

    /**
     * Kantenlaenge einer Kachel im Zyklus des Tagesbonus.
     *
     * Bewusst kleiner als [minTouchTarget]: Die Kacheln sind reine Anzeige und
     * werden nicht angetippt. Bei Beruehrungsgroesse waere die Leiste aus
     * sieben Kacheln breiter als ein Dialog auf einem schmalen Geraet, und die
     * letzten Tage wuerden abgeschnitten - ausgerechnet der wichtigste.
     */
    val cycleTileSize: Dp = 36.dp,

    /** Kantenlaenge des Sinnbilds eines laufenden Boosters. */
    val boosterIconSize: Dp = 24.dp,

    /**
     * Breite der Restlaufzeit eines Boosters.
     *
     * Fest und nicht vom Inhalt abgeleitet: Die Anzeige zaehlt jede Sekunde
     * herunter, und eine mitwandernde Breite liesse die gesamte Leiste bei
     * jedem Stellenwechsel springen.
     */
    val boosterTimerWidth: Dp = 52.dp,
)

/**
 * Zugriffspunkt fuer [Dimens] innerhalb der Composition.
 *
 * Wie bei den Farben `static`, weil sich die Werte zur Laufzeit nicht aendern.
 */
val LocalDimens = staticCompositionLocalOf { Dimens() }
