package com.pokelike.idle.manager

import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verbindet den Lebenszyklus des Prozesses mit dem Zustand des Spiels.
 *
 * Bringt die drei Vorgaenge in eine feste Reihenfolge, die beim Wechsel
 * zwischen Vorder- und Hintergrund eingehalten werden muss: Spielstand laden,
 * Uhr takten lassen, Autosave starten - und in umgekehrter Richtung Uhr
 * anhalten, Autosave beenden, ein letztes Mal speichern.
 *
 * Die Reihenfolge ist nicht beliebig. Wuerde die Uhr vor dem Laden starten,
 * liefe die Spielzeit auf einem leeren Ausgangszustand, und der erste Autosave
 * ueberschriebe den gespeicherten Fortschritt mit einem leeren Stand.
 *
 * Diese Klasse ist der einzige Ansprechpartner von
 * [com.pokelike.idle.PokelikeApplication]. Dadurch bleibt die
 * Application-Klasse frei von Ablauflogik.
 */
@Singleton
class GameSessionManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val timeSource: TimeSource,
    private val repository: GameRepository,
    private val gameClock: GameClock,
    private val autosaveManager: AutosaveManager,
) {

    /**
     * Verhindert, dass sich Vorder- und Hintergrundwechsel ueberholen.
     *
     * Ein schneller Wechsel - etwa durch das Aufklappen der
     * Benachrichtigungsleiste - kann beide Ablaeufe kurz hintereinander
     * ausloesen. Ohne diese Sperre koennte das abschliessende Speichern des
     * einen nach dem Laden des anderen laufen und den frisch geladenen Stand
     * ueberschreiben.
     */
    private val transitionLock = Mutex()

    /**
     * Die App ist in den Vordergrund gekommen.
     *
     * Kehrt sofort zurueck; die eigentliche Arbeit laeuft im prozessweiten
     * Scope. Der Lebenszyklus-Rueckruf darf nicht blockieren, sonst verzoegert
     * sich der Start sichtbar.
     */
    fun onEnterForeground() {
        scope.launch(dispatchers.default) {
            transitionLock.withLock {
                if (!repository.isLoaded.value) {
                    repository.load(timeSource.wallClock())
                }
                gameClock.start()
                autosaveManager.start()
            }
        }
    }

    /**
     * Die App geht in den Hintergrund.
     *
     * Das abschliessende Speichern laeuft im prozessweiten Scope weiter, auch
     * wenn die Activity bereits zerstoert ist. Der Zeitstempel wird dabei
     * fortgeschrieben - er ist die Grundlage der spaeteren
     * Offline-Berechnung.
     */
    fun onEnterBackground() {
        scope.launch(dispatchers.default) {
            transitionLock.withLock {
                gameClock.stop()
                autosaveManager.stop()

                if (repository.isLoaded.value) {
                    val now = timeSource.wallClock()
                    repository.update { state -> state.copy(lastSeenAtMillis = now) }
                    repository.save()
                }
            }
        }
    }
}
