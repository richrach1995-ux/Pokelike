package com.pokelike.idle.util

/**
 * Abstraktion ueber die beiden Zeitquellen, die das Spiel braucht.
 *
 * Die Trennung ist kein Selbstzweck, sondern eine Sicherheitsmassnahme:
 *
 * - [elapsedRealtime] ist monoton und laeuft auch im Deep Sleep weiter. Der
 *   Nutzer kann sie nicht verstellen. Alles, was Spielmechanik beeinflusst
 *   (Tick-Deltas, Booster-Restlaufzeiten), muss sie verwenden.
 * - [wallClock] ist die verstellbare Systemzeit. Sie wird nur dort gebraucht,
 *   wo echte Kalendertage relevant sind (Login-Bonus, taegliche Quests) - und
 *   dort spaeter gegen einen Serverzeitstempel geprueft.
 *
 * Wuerde Spielmechanik auf [wallClock] laufen, reichte ein Vorstellen der
 * Geraeteuhr, um beliebig viel Offline-Ertrag zu erzeugen.
 *
 * Das Interface ist bewusst frei von Android-Typen. Dadurch bleibt der gesamte
 * Engine-Kern, der es verwendet, in einer reinen JVM-Umgebung uebersetzbar und
 * testbar - ohne Emulator und ohne Android-Framework.
 */
interface TimeSource {

    /** Monotone Millisekunden seit Systemstart. Nicht manipulierbar. */
    fun elapsedRealtime(): Long

    /** Systemzeit in Millisekunden seit Epoch. Vom Nutzer verstellbar. */
    fun wallClock(): Long
}
