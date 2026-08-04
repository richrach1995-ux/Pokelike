package com.pokelike.idle

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.pokelike.idle.manager.GameClock
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Einstiegspunkt der Anwendung und Wurzel des Dependency-Graphen.
 *
 * Die Klasse haelt bewusst keine Spiellogik. Ihre einzige Aufgabe neben der
 * Hilt-Initialisierung ist es, die Spiel-Uhr an den Lebenszyklus des
 * *Prozesses* zu koppeln - nicht an den einer Activity.
 *
 * Der Unterschied ist wesentlich: Bei einer Bildschirmdrehung wird die Activity
 * zerstoert und neu erzeugt. Haenge die Uhr am Activity-Lifecycle, wuerde sie
 * dabei jedes Mal anhalten und neu starten, und der Spieler verlaere bei jeder
 * Drehung Ertrag. [ProcessLifecycleOwner] meldet dagegen nur den echten Wechsel
 * zwischen Vorder- und Hintergrund.
 */
@HiltAndroidApp
class PokelikeApplication : Application(), DefaultLifecycleObserver {

    /**
     * Feldinjektion ist hier notwendig, weil das Framework die Application
     * selbst instanziiert und Hilt deshalb keinen Konstruktor aufrufen kann.
     */
    @Inject
    lateinit var gameClock: GameClock

    override fun onCreate() {
        // Der Aufruf von Application.onCreate muss explizit qualifiziert
        // werden, da DefaultLifecycleObserver eine gleichnamige Methode mit
        // LifecycleOwner-Parameter beisteuert.
        super<Application>.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** App ist in den Vordergrund gekommen: Engine takten lassen. */
    override fun onStart(owner: LifecycleOwner) {
        gameClock.start()
    }

    /**
     * App ist in den Hintergrund gegangen: Engine anhalten.
     *
     * Weiterlaufen zu lassen waere doppelt schaedlich - es wuerde Akku
     * verbrauchen und trotzdem keine verlaessliche Zeitbasis liefern, weil
     * Android Hintergrundprozesse jederzeit drosseln oder beenden darf. Die im
     * Hintergrund vergangene Zeit wird stattdessen beim naechsten Start als
     * Offline-Fortschritt verrechnet.
     */
    override fun onStop(owner: LifecycleOwner) {
        gameClock.stop()
    }
}
