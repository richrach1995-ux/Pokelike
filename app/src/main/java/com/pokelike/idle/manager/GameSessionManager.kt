package com.pokelike.idle.manager

import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.domain.model.OfflineProgress
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.CalculateIncomeUseCase
import com.pokelike.idle.domain.usecases.CalculateOfflineProgressUseCase
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verbindet den Lebenszyklus des Prozesses mit dem Zustand des Spiels.
 *
 * Bringt die Vorgaenge beim Wechsel zwischen Vorder- und Hintergrund in eine
 * feste Reihenfolge: Spielstand laden, Offline-Ertrag verrechnen, Uhr takten
 * lassen, Dienste starten - und in umgekehrter Richtung Uhr anhalten, Dienste
 * beenden, ein letztes Mal speichern.
 *
 * Die Reihenfolge ist nicht beliebig:
 *
 * - Liefe die Uhr vor dem Laden, arbeitete die Spielzeit auf einem leeren
 *   Ausgangszustand, und der erste Autosave ueberschriebe den gespeicherten
 *   Fortschritt.
 * - Wuerde der Offline-Ertrag nach dem Start der laufenden Gutschrift
 *   verrechnet, zaehlte die Zeit zwischen beiden Schritten doppelt.
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
    private val clickManager: ClickManager,
    private val idleIncomeManager: IdleIncomeManager,
    private val modifierManager: ModifierManager,
    private val calculateIncome: CalculateIncomeUseCase,
    private val calculateOfflineProgress: CalculateOfflineProgressUseCase,
) {

    private val _offlineProgress = MutableStateFlow<OfflineProgress?>(null)

    /**
     * Ergebnis der letzten Offline-Berechnung, sofern es sich zu zeigen lohnt.
     *
     * Die Oberflaeche zeigt es als Willkommensdialog und ruft danach
     * [consumeOfflineProgress] auf. Als Zustand und nicht als Ereignis, damit
     * eine Bildschirmdrehung waehrend des Dialogs ihn nicht verschwinden
     * laesst.
     */
    val offlineProgress: StateFlow<OfflineProgress?> = _offlineProgress.asStateFlow()

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
                    val loaded = repository.load(timeSource.wallClock())
                    applyOfflineProgress(loaded.lastSeenAtMillis)
                }
                gameClock.start()
                autosaveManager.start()
                clickManager.start()
                idleIncomeManager.start()
            }
        }
    }

    /**
     * Die App geht in den Hintergrund.
     *
     * Das abschliessende Speichern laeuft im prozessweiten Scope weiter, auch
     * wenn die Activity bereits zerstoert ist. Der Zeitstempel wird dabei
     * fortgeschrieben - er ist die Grundlage der Offline-Berechnung beim
     * naechsten Start.
     */
    fun onEnterBackground() {
        scope.launch(dispatchers.default) {
            transitionLock.withLock {
                gameClock.stop()
                autosaveManager.stop()
                clickManager.stop()
                idleIncomeManager.stop()

                if (repository.isLoaded.value) {
                    val now = timeSource.wallClock()
                    repository.update { state -> state.copy(lastSeenAtMillis = now) }
                    repository.save()
                }
            }
        }
    }

    /** Bestaetigt, dass der Willkommensdialog gezeigt wurde. */
    fun consumeOfflineProgress() {
        _offlineProgress.value = null
    }

    /**
     * Verrechnet die Abwesenheit seit dem letzten Speicherpunkt.
     *
     * Der Ertrag wird immer gutgeschrieben, angezeigt aber nur, wenn er
     * nennenswert ist. Ein Dialog ueber null Muenzen nach einer Minute
     * Abwesenheit waere reine Stoerung.
     */
    private fun applyOfflineProgress(lastSeenAtMillis: Long) {
        val state = repository.gameState.value
        val modifiers = modifierManager.modifiers.value

        val progress = calculateOfflineProgress(
            lastSeenAtMillis = lastSeenAtMillis,
            nowMillis = timeSource.wallClock(),
            incomePerSecond = calculateIncome(
                buildings = state.buildings,
                multiplier = modifiers.incomeMultiplier,
                perBuildingMultipliers = modifiers.buildingIncomeMultipliers,
            ),
            efficiency = modifiers.offlineEfficiency,
        )

        if (!progress.earned.isEmpty) {
            repository.update { current -> current.grant(progress.earned) }
        }

        if (progress.isWorthShowing) {
            _offlineProgress.value = progress
        }
    }
}
