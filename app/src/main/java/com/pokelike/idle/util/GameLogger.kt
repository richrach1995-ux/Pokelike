package com.pokelike.idle.util

/**
 * Protokollierung von Stoerungen.
 *
 * Als Schnittstelle ausgelegt, aus zwei Gruenden:
 *
 * - `android.util.Log` liefert in JVM-Tests keine Ausgabe und macht jede
 *   Klasse, die es direkt aufruft, schlechter testbar. Ein Test kann hier
 *   stattdessen pruefen, dass ein Fehlerfall ueberhaupt gemeldet wurde.
 * - Sobald Crashlytics eingebunden ist, wird es hinter dieser Schnittstelle
 *   ergaenzt. Kein Aufrufer muss dafuer angefasst werden.
 *
 * Bewusst nur zwei Stufen. Ausfuehrliche Ablaufprotokolle gehoeren nicht in
 * eine Anwendung, die auf Geraeten von Spielern laeuft; hier interessiert
 * ausschliesslich, was schiefgegangen ist.
 */
interface GameLogger {

    /** Stoerung, die die App abfangen konnte. */
    fun warn(tag: String, message: String, throwable: Throwable? = null)

    /** Fehler, der zu Datenverlust oder fehlerhaftem Verhalten fuehren kann. */
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
