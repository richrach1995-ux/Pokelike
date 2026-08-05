package com.pokelike.idle.manager

import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.domain.model.ActiveBooster
import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.BoosterPurchaseResult
import com.pokelike.idle.domain.usecases.PurchaseBoosterUseCase
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
 * Ein laufender Booster mit seiner Restlaufzeit.
 *
 * @property remainingMillis Verbleibende Zeit zum Zeitpunkt der letzten
 *   Aktualisierung.
 * @property progress Anteil der bereits verstrichenen Laufzeit, 0 bis 1. Fuer
 *   den ablaufenden Balken in der Anzeige.
 */
data class BoosterSnapshot(
    val type: BoosterType,
    val remainingMillis: Long,
    val progress: Float,
)

/**
 * Haelt die laufenden Booster aktuell.
 *
 * Zwei Aufgaben, die dieselbe sind: Er entfernt abgelaufene Booster aus dem
 * Spielstand und stellt die Restlaufzeiten zur Anzeige bereit. Beides braucht
 * genau eine Information - wie spaet es ist -, und beides muss im selben Takt
 * geschehen; getrennt gefuehrt koennte die Anzeige einen Booster zeigen, den
 * die Berechnung nicht mehr kennt.
 *
 * **Warum das Ablaufen ueberhaupt in den Spielstand geschrieben wird.** Die
 * Modifikatoren werden aus dem Spielstand berechnet und kennen die Uhrzeit
 * nicht. Erst das Entfernen macht das Ablaufen zu einer Aenderung des
 * Spielstands - und damit zu etwas, das Modifikatoren, Anzeige und Autosave
 * gleichermassen mitbekommen. Der umgekehrte Weg, jede Leseseite die Uhrzeit
 * pruefen zu lassen, waere derselbe Filter an fuenf Stellen.
 *
 * Geprueft wird im Sekundentakt statt in jedem Takt. Die Anzeige zeigt Sekunden
 * an, feiner braucht es niemand, und jede Pruefung, die etwas entfernt, loest
 * einen Schreibvorgang aus.
 */
@Singleton
class BoosterManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val gameClock: GameClock,
    private val repository: GameRepository,
    private val timeSource: TimeSource,
    private val purchaseBooster: PurchaseBoosterUseCase,
) {

    private val _active = MutableStateFlow<List<BoosterSnapshot>>(emptyList())

    /** Laufende Booster, nach Restlaufzeit aufsteigend. */
    val active: StateFlow<List<BoosterSnapshot>> = _active.asStateFlow()

    private var tickJob: Job? = null

    /** Startet die laufende Pruefung. Mehrfache Aufrufe sind unschaedlich. */
    fun start() {
        if (tickJob?.isActive == true) return

        tickJob = scope.launch(dispatchers.default) {
            gameClock.tick.collect { tick ->
                if (tick.index % TICKS_PER_CHECK != 0L) return@collect
                refresh()
            }
        }
    }

    /** Haelt die Pruefung an. */
    fun stop() {
        tickJob?.cancel()
        tickJob = null
    }

    /**
     * Entfernt abgelaufene Booster und schreibt die Restlaufzeiten fort.
     *
     * Wird zusaetzlich beim Start der Sitzung aufgerufen: Ein Booster, der
     * waehrend der Abwesenheit abgelaufen ist, darf beim Oeffnen nicht erst
     * noch eine Sekunde lang wirken - und schon gar nicht in die
     * Offline-Berechnung eingehen.
     */
    fun refresh() {
        val now = timeSource.wallClock()

        repository.update { state ->
            val pruned = state.boosters.pruned(now)
            if (pruned == state.boosters) state else state.copy(boosters = pruned)
        }

        _active.value = repository.gameState.value.boosters
            .activeAt(now)
            .map { booster -> booster.toSnapshot(now) }
    }

    /**
     * Kauft einen Booster und startet ihn.
     *
     * Die Pruefung laeuft innerhalb von [GameRepository.update] und damit
     * atomar auf dem aktuellen Zustand: Zwischen Deckungspruefung und
     * Abbuchung darf kein anderer Vorgang den Diamantenstand veraendern.
     *
     * @return Das Ergebnis, damit die Oberflaeche einen fehlgeschlagenen Kauf
     *   erklaeren kann.
     */
    fun purchase(type: BoosterType): BoosterPurchaseResult {
        val now = timeSource.wallClock()
        lateinit var result: BoosterPurchaseResult

        repository.update { state ->
            result = purchaseBooster(state, type, now)
            when (val outcome = result) {
                is BoosterPurchaseResult.Success -> outcome.state
                BoosterPurchaseResult.NotAffordable,
                BoosterPurchaseResult.AlreadyAtMaximum,
                -> state
            }
        }

        // Sofort und nicht erst beim naechsten Takt: Der Spieler soll den
        // Booster in dem Moment laufen sehen, in dem er ihn kauft.
        refresh()

        return result
    }

    private fun ActiveBooster.toSnapshot(nowMillis: Long): BoosterSnapshot {
        val remaining = remainingAt(nowMillis)
        val granted = grantedDurationMillis

        return BoosterSnapshot(
            type = type,
            remainingMillis = remaining,
            progress = if (granted <= 0L) {
                0f
            } else {
                (remaining.toDouble() / granted).toFloat().coerceIn(0f, 1f)
            },
        )
    }

    private companion object {
        /**
         * Takte zwischen zwei Pruefungen.
         *
         * Bei 100 ms Taktrate entspricht das einer Sekunde.
         */
        const val TICKS_PER_CHECK = 10L
    }
}
