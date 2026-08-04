package com.pokelike.idle.manager

import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.CalculateIncomeUseCase
import com.pokelike.idle.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schreibt das Leerlaufeinkommen laufend gut.
 *
 * Der Takt kommt aus [GameClock] und damit aus derselben Quelle wie
 * Combo-Zerfall und Autosave. Ein eigener Zeitgeber wuerde im Hintergrund
 * weiterlaufen und dort Ertrag erzeugen, den die Offline-Berechnung beim
 * naechsten Start ein zweites Mal gutschreibt.
 *
 * Gerechnet wird mit [com.pokelike.idle.manager.GameTick.deltaMillis], nicht
 * mit dem Sollintervall. Auf einem ausgelasteten Geraet vergeht zwischen zwei
 * Takten mehr Zeit als geplant; mit dem Sollwert liefe das Spiel dort
 * langsamer, und der Spieler haette messbar weniger Ertrag als auf einem
 * schnellen Geraet.
 */
@Singleton
class IdleIncomeManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val gameClock: GameClock,
    private val repository: GameRepository,
    private val modifierManager: ModifierManager,
    private val calculateIncome: CalculateIncomeUseCase,
) {

    /**
     * Aktueller Ertrag pro Sekunde.
     *
     * Aus dem Spielstand abgeleitet statt getrennt gefuehrt: So kann die
     * Anzeige nie von dem abweichen, was tatsaechlich gutgeschrieben wird.
     *
     * [distinctUntilChanged] ist hier wesentlich. Der Spielstand aendert sich
     * zehnmal pro Sekunde durch die Gutschrift selbst; der Ertragswert dagegen
     * nur beim Kauf eines Gebaeudes. Ohne den Filter wuerde jede Anzeige, die
     * daran haengt, zehnmal pro Sekunde neu gezeichnet.
     */
    val incomePerSecond: StateFlow<BigNumber> = combine(
        repository.gameState,
        modifierManager.modifiers,
    ) { state, modifiers ->
        calculateIncome(
            buildings = state.buildings,
            multiplier = modifiers.incomeMultiplier,
            perBuildingMultipliers = modifiers.buildingIncomeMultipliers,
        )
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            // Eagerly, weil der Wert auch dann stimmen muss, wenn gerade kein
            // Bildschirm ihn beobachtet - die Tick-Schleife liest ihn direkt.
            started = SharingStarted.Eagerly,
            initialValue = BigNumber.ZERO,
        )

    private val _isRunning = MutableStateFlow(false)

    /** Ob die Gutschrift laeuft. */
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var incomeJob: Job? = null

    /**
     * Startet die laufende Gutschrift.
     *
     * Mehrfache Aufrufe sind unschaedlich. Zwei parallele Schleifen wuerden den
     * Ertrag verdoppeln.
     */
    fun start() {
        if (incomeJob?.isActive == true) return

        incomeJob = scope.launch(dispatchers.default) {
            _isRunning.value = true
            try {
                gameClock.tick.collect { tick ->
                    if (tick.deltaMillis <= 0L) return@collect

                    val perSecond = incomePerSecond.value
                    if (perSecond.isZero) return@collect

                    val earned = perSecond * (tick.deltaMillis / MILLIS_PER_SECOND)
                    if (earned.isZero) return@collect

                    repository.update { state ->
                        state.grant(ResourceBundle.single(ResourceType.COINS, earned))
                    }
                }
            } finally {
                _isRunning.value = false
            }
        }
    }

    /** Haelt die Gutschrift an. */
    fun stop() {
        incomeJob?.cancel()
        incomeJob = null
        _isRunning.value = false
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000.0
    }
}
