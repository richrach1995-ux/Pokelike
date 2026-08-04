package com.pokelike.idle.util

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Produktionsimplementierung von [TimeSource] auf Basis der Android-Systemuhren.
 *
 * Bewusst in einer eigenen Datei: Sie ist der einzige Teil der Zeitabstraktion,
 * der Android-Klassen benoetigt. Die Trennung haelt [TimeSource] selbst frei von
 * Framework-Abhaengigkeiten.
 */
@Singleton
class SystemTimeSource @Inject constructor() : TimeSource {

    /**
     * [SystemClock.elapsedRealtime] laeuft im Gegensatz zu `uptimeMillis` auch
     * waehrend des Deep Sleep weiter. Fuer ein Idle-Spiel ist genau das noetig:
     * Die Zeit soll auch dann zaehlen, wenn der Bildschirm aus ist.
     */
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()

    override fun wallClock(): Long = System.currentTimeMillis()
}
