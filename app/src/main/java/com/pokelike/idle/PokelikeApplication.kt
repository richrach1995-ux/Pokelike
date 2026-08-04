package com.pokelike.idle

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.pokelike.idle.manager.GameSessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Einstiegspunkt der Anwendung und Wurzel des Dependency-Graphen.
 *
 * Die Klasse haelt bewusst keine Ablauflogik. Ihre einzige Aufgabe neben der
 * Hilt-Initialisierung ist es, den Lebenszyklus des *Prozesses* an
 * [GameSessionManager] weiterzureichen.
 *
 * Der Unterschied zum Activity-Lebenszyklus ist wesentlich: Bei einer
 * Bildschirmdrehung wird die Activity zerstoert und neu erzeugt. Haenge die
 * Sitzung daran, wuerde bei jeder Drehung der Spielstand neu geladen und die
 * Uhr neu gestartet. [ProcessLifecycleOwner] meldet dagegen nur den echten
 * Wechsel zwischen Vorder- und Hintergrund.
 */
@HiltAndroidApp
class PokelikeApplication : Application(), DefaultLifecycleObserver {

    /**
     * Feldinjektion ist hier notwendig, weil das Framework die Application
     * selbst instanziiert und Hilt deshalb keinen Konstruktor aufrufen kann.
     */
    @Inject
    lateinit var gameSessionManager: GameSessionManager

    override fun onCreate() {
        // Der Aufruf muss explizit qualifiziert werden, da
        // DefaultLifecycleObserver eine gleichnamige Methode mit
        // LifecycleOwner-Parameter beisteuert.
        super<Application>.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** App ist im Vordergrund: Spielstand laden, Uhr und Autosave starten. */
    override fun onStart(owner: LifecycleOwner) {
        gameSessionManager.onEnterForeground()
    }

    /**
     * App geht in den Hintergrund: Uhr anhalten und ein letztes Mal speichern.
     *
     * Die Uhr weiterlaufen zu lassen waere doppelt schaedlich - es wuerde Akku
     * verbrauchen und trotzdem keine verlaessliche Zeitbasis liefern, weil
     * Android Hintergrundprozesse jederzeit drosseln oder beenden darf. Die im
     * Hintergrund vergangene Zeit wird stattdessen beim naechsten Start als
     * Offline-Fortschritt verrechnet.
     */
    override fun onStop(owner: LifecycleOwner) {
        gameSessionManager.onEnterBackground()
    }
}
