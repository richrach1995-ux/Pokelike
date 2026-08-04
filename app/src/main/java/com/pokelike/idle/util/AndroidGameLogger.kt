package com.pokelike.idle.util

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Protokollierung ueber `android.util.Log`.
 *
 * Bewusst in einer eigenen Datei: Sie ist der einzige Teil der Protokollierung,
 * der Android-Klassen benoetigt, und haelt [GameLogger] damit frei von
 * Framework-Abhaengigkeiten.
 *
 * Sobald Crashlytics eingebunden ist, meldet diese Klasse zusaetzlich dorthin.
 * Aufrufer bleiben davon unberuehrt.
 */
@Singleton
class AndroidGameLogger @Inject constructor() : GameLogger {

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag.withPrefix(), message, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag.withPrefix(), message, throwable)
    }

    /**
     * Setzt ein gemeinsames Praefix vor jede Markierung.
     *
     * Damit laesst sich die Ausgabe der App im Logcat mit einem einzigen
     * Filter von der des Systems und anderer Anwendungen trennen.
     *
     * Die Laenge wird begrenzt: Vor Android 8 wirft `Log` bei Markierungen
     * ueber 23 Zeichen eine Ausnahme, und die App unterstuetzt ab API 24.
     */
    private fun String.withPrefix(): String = "$LOG_PREFIX$this".take(MAX_TAG_LENGTH)

    private companion object {
        const val LOG_PREFIX = "Pokelike-"
        const val MAX_TAG_LENGTH = 23
    }
}
