package com.pokelike.idle.manager

import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sichert den Spielstand in regelmaessigen Abstaenden.
 *
 * Der Zeittakt kommt bewusst aus [GameClock] und nicht aus einem eigenen
 * `delay`. Das hat zwei Folgen, die beide gewollt sind:
 *
 * - Der Autosave pausiert automatisch, sobald die App in den Hintergrund geht.
 *   Ein eigener Zeitgeber wuerde dort weiterlaufen und wiederholt denselben
 *   unveraenderten Stand schreiben.
 * - Gezaehlt wird die bereits gedeckelte Spielzeit. Ein angehaltener Prozess
 *   loest dadurch nicht sofort beim Zurueckkehren einen Schreibvorgang aus.
 *
 * Das Speichern beim Wechsel in den Hintergrund ist nicht Aufgabe dieser
 * Klasse - dort taktet die Uhr nicht mehr. Das uebernimmt
 * [GameSessionManager].
 */
@Singleton
class AutosaveManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val gameClock: GameClock,
    private val repository: GameRepository,
) {

    private var autosaveJob: Job? = null

    /**
     * Startet den Autosave.
     *
     * Mehrfache Aufrufe sind unschaedlich. Zwei parallele Schleifen waeren hier
     * zwar nicht gefaehrlich, wuerden aber doppelt so oft schreiben wie
     * beabsichtigt.
     */
    fun start() {
        if (autosaveJob?.isActive == true) return

        autosaveJob = scope.launch(dispatchers.default) {
            var millisSinceLastSave = 0L

            gameClock.tick.collect { tick ->
                millisSinceLastSave += tick.deltaMillis
                if (millisSinceLastSave < GameConfig.AUTOSAVE_INTERVAL_MS) return@collect

                // Zuruecksetzen vor dem Schreiben: Dauert der Schreibvorgang
                // laenger als ein Takt, soll der naechste Takt nicht sofort
                // einen weiteren ausloesen.
                millisSinceLastSave = 0L
                repository.save()
            }
        }
    }

    /** Haelt den Autosave an. */
    fun stop() {
        autosaveJob?.cancel()
        autosaveJob = null
    }
}
