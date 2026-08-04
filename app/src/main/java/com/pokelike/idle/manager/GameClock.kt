package com.pokelike.idle.manager

import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zentrale Taktquelle der Spiel-Engine.
 *
 * Warum genau eine Uhr fuer die gesamte App:
 * Idle-Einkommen, Booster-Ablauf, Event-Spawns, Quest-Fortschritt und Autosave
 * muessen sich auf dieselbe Zeitbasis beziehen. Haette jedes System einen
 * eigenen Timer, wuerden sie auseinanderlaufen, sobald das System einen der
 * Timer drosselt - mit dem Ergebnis, dass ein Booster laut Anzeige noch aktiv
 * ist, waehrend die Einkommensberechnung ihn bereits ignoriert.
 *
 * Die Uhr wird vom Prozess-Lifecycle gesteuert (siehe
 * [com.pokelike.idle.PokelikeApplication]): Sie laeuft, solange die App im
 * Vordergrund ist, und haelt an, sobald sie in den Hintergrund geht. Zeit, die
 * waehrend der Pause vergeht, ist per Definition Offline-Zeit und wird beim
 * naechsten Start vom Offline-Progress-Pfad in einem Zug verrechnet - nicht
 * durch nachtraegliches Aufholen einzelner Ticks.
 *
 * Alle Deltas stammen aus [TimeSource.elapsedRealtime] und sind damit gegen
 * ein Verstellen der Geraeteuhr immun.
 */
@Singleton
class GameClock @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val timeSource: TimeSource,
) {

    private val _tick = MutableStateFlow(GameTick.ZERO)

    /**
     * Aktueller Takt.
     *
     * Bewusst ein [StateFlow] und kein `SharedFlow`: Neu hinzukommende
     * Beobachter (etwa ein gerade geoeffneter Screen) bekommen sofort den
     * letzten Stand, statt bis zum naechsten Takt eine leere Anzeige zu zeigen.
     */
    val tick: StateFlow<GameTick> = _tick.asStateFlow()

    private val _isRunning = MutableStateFlow(false)

    /** Ob die Engine gerade taktet. Wird von der UI zur Anzeige genutzt. */
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** Referenz auf die laufende Tick-Schleife, damit [stop] sie beenden kann. */
    private var tickJob: Job? = null

    /**
     * Startet die Tick-Schleife.
     *
     * Mehrfache Aufrufe sind unschaedlich: Laeuft bereits eine Schleife, kehrt
     * die Methode ohne Wirkung zurueck. Das ist wichtig, weil Lifecycle-Events
     * bei Konfigurationswechseln mehrfach eintreffen koennen und zwei parallele
     * Schleifen den Ertrag verdoppeln wuerden.
     */
    fun start() {
        if (tickJob?.isActive == true) return

        tickJob = scope.launch(dispatchers.default) {
            // Referenzpunkt fuer das erste Delta. Wird bei jedem Takt neu
            // gesetzt, damit sich Ungenauigkeiten von `delay` nicht aufsummieren.
            var lastTimestamp = timeSource.elapsedRealtime()
            _isRunning.value = true

            try {
                while (isActive) {
                    delay(GameConfig.TICK_INTERVAL_MS)

                    val now = timeSource.elapsedRealtime()

                    // Deckel gegen Ausreisser: Wurde der Prozess zwischenzeitlich
                    // angehalten, darf ein einzelner Takt nicht die gesamte
                    // Pause als Ertrag gutschreiben.
                    val delta = (now - lastTimestamp)
                        .coerceIn(0L, GameConfig.MAX_TICK_DELTA_MS)
                    lastTimestamp = now

                    val previous = _tick.value
                    _tick.value = GameTick(
                        index = previous.index + 1L,
                        deltaMillis = delta,
                        elapsedMillis = previous.elapsedMillis + delta,
                    )
                }
            } finally {
                // Laeuft auch bei Abbruch der Coroutine, damit die Anzeige nicht
                // faelschlich "laeuft" meldet, nachdem die Schleife beendet wurde.
                _isRunning.value = false
            }
        }
    }

    /**
     * Haelt die Tick-Schleife an.
     *
     * Der bisherige Stand in [tick] bleibt erhalten, damit die UI beim
     * Zurueckkehren nicht auf null zurueckspringt.
     */
    fun stop() {
        tickJob?.cancel()
        tickJob = null
        _isRunning.value = false
    }
}
