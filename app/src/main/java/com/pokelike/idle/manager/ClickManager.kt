package com.pokelike.idle.manager

import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.domain.model.ClickOutcome
import com.pokelike.idle.domain.model.ComboSnapshot
import com.pokelike.idle.domain.model.ComboState
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.PerformClickUseCase
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Haelt den Combo-Stand und fuehrt Klicks aus.
 *
 * Warum ein prozessweiter Dienst und kein Zustand im ViewModel:
 * Ein ViewModel wird bei einer Bildschirmdrehung zwar behalten, aber beim
 * Wechsel auf einen anderen Bildschirm verworfen. Die Combo wuerde dann beim
 * Blick in den Shop verfallen - ein Verhalten, das der Spieler als Fehler
 * erlebt. Hier ueberdauert sie jeden Bildschirmwechsel und endet nur, wenn das
 * Zeitfenster tatsaechlich ablaeuft.
 *
 * Der Combo-Zerfall haengt am Takt von [GameClock] und pausiert damit
 * automatisch, sobald die App in den Hintergrund geht.
 */
@Singleton
class ClickManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val timeSource: TimeSource,
    private val gameClock: GameClock,
    private val repository: GameRepository,
    private val modifierManager: ModifierManager,
    private val performClick: PerformClickUseCase,
) {

    /**
     * Interner Combo-Stand.
     *
     * Ein [MutableStateFlow] und kein einfaches Feld, weil zwei Seiten darauf
     * zugreifen: [click] vom UI-Thread und die Zerfallsschleife vom
     * Default-Dispatcher.
     */
    private val comboState = MutableStateFlow(ComboState.NONE)

    private val _combo = MutableStateFlow(ComboSnapshot.NONE)

    /** Combo-Stand fuer die Anzeige. */
    val combo: StateFlow<ComboSnapshot> = _combo.asStateFlow()

    private var decayJob: Job? = null

    /**
     * Fuehrt einen Klick aus und liefert das Ergebnis fuer die Rueckmeldung.
     *
     * Bewusst nicht `suspend` und ohne Dispatcher-Wechsel: Der Aufruf kommt aus
     * einer Compose-Rueckmeldung und muss noch im selben Frame wirken. Alle
     * Schritte laufen im Arbeitsspeicher; das Schreiben uebernimmt spaeter der
     * Autosave.
     */
    fun click(): ClickOutcome {
        val now = timeSource.elapsedRealtime()
        val comboBefore = comboState.value
        val currentModifiers = modifierManager.modifiers.value.click

        lateinit var outcome: ClickOutcome

        // Zur Zuweisung innerhalb der Transformation: MutableStateFlow.update
        // wiederholt die Transformation, falls ein anderer Schreibvorgang
        // dazwischenkommt. Massgeblich ist immer der letzte Durchlauf - genau
        // der, dessen Ergebnis auch uebernommen wurde. Nach dem Aufruf steht in
        // outcome deshalb das Ergebnis, das tatsaechlich gilt.
        repository.update { state ->
            outcome = performClick(
                state = state,
                combo = comboBefore,
                modifiers = currentModifiers,
                nowMillis = now,
            )
            outcome.state
        }

        comboState.value = outcome.combo
        _combo.value = ComboSnapshot.from(outcome.combo, now)

        return outcome
    }

    /**
     * Startet die Ueberwachung des Combo-Zerfalls.
     *
     * Mehrfache Aufrufe sind unschaedlich.
     */
    fun start() {
        if (decayJob?.isActive == true) return

        decayJob = scope.launch(dispatchers.default) {
            gameClock.tick.collect {
                val now = timeSource.elapsedRealtime()
                val current = comboState.value

                if (current.isActive && current.isExpired(now)) {
                    comboState.value = ComboState.NONE
                }

                _combo.value = ComboSnapshot.from(comboState.value, now)
            }
        }
    }

    /**
     * Haelt die Ueberwachung an und beendet eine laufende Combo.
     *
     * Das Zuruecksetzen ist wichtig: Beim Wechsel in den Hintergrund steht die
     * Uhr, der Combo-Stand bliebe also eingefroren. Kaeme der Spieler Stunden
     * spaeter zurueck, setzte er eine laengst verfallene Combo fort.
     */
    fun stop() {
        decayJob?.cancel()
        decayJob = null
        comboState.value = ComboState.NONE
        _combo.value = ComboSnapshot.NONE
    }
}
