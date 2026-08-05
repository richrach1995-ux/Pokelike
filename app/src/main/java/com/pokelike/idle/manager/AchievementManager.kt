package com.pokelike.idle.manager

import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.domain.model.PendingReward
import com.pokelike.idle.domain.model.RewardSource
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.AchievementCheckResult
import com.pokelike.idle.domain.usecases.CheckAchievementsUseCase
import com.pokelike.idle.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prueft laufend, ob Achievements faellig sind.
 *
 * Der Takt kommt aus [GameClock], geprueft wird aber nur jede Sekunde statt bei
 * jedem Takt. Der Grund ist nicht die Rechenzeit - zwanzig Vergleiche sind
 * belanglos -, sondern die Aenderungsmarkierung des Spielstands: Jede Pruefung,
 * die etwas freischaltet, loest einen Schreibvorgang aus. Zehnmal pro Sekunde
 * zu pruefen brachte dem Spieler nichts, wuerde aber die Wahrscheinlichkeit
 * erhoehen, dass ein Autosave mitten in eine Freischaltung faellt.
 *
 * Freigeschaltete Achievements werden sofort gutgeschrieben und anschliessend
 * bei [RewardManager] zur Anzeige angemeldet.
 */
@Singleton
class AchievementManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val gameClock: GameClock,
    private val repository: GameRepository,
    private val rewardManager: RewardManager,
    private val checkAchievements: CheckAchievementsUseCase,
) {

    private var checkJob: Job? = null

    /**
     * Startet die laufende Pruefung.
     *
     * Mehrfache Aufrufe sind unschaedlich.
     */
    fun start() {
        if (checkJob?.isActive == true) return

        checkJob = scope.launch(dispatchers.default) {
            gameClock.tick.collect { tick ->
                if (tick.index % TICKS_PER_CHECK != 0L) return@collect
                checkNow()
            }
        }
    }

    /** Haelt die Pruefung an. */
    fun stop() {
        checkJob?.cancel()
        checkJob = null
    }

    /**
     * Fuehrt eine Pruefung sofort aus.
     *
     * Wird zusaetzlich beim Start der Sitzung aufgerufen: Ein Spieler, der
     * offline die Schwelle eines Achievements ueberschritten hat, soll die
     * Meldung beim Oeffnen sehen und nicht erst eine Sekunde spaeter.
     */
    fun checkNow() {
        lateinit var result: AchievementCheckResult

        // Zur Zuweisung innerhalb der Transformation gilt dasselbe wie im
        // ClickManager: Bei gleichzeitigem Schreibzugriff wiederholt update die
        // Transformation, und massgeblich ist der letzte Durchlauf - genau der,
        // dessen Ergebnis uebernommen wurde.
        repository.update { state ->
            result = checkAchievements(state)
            result.state
        }

        rewardManager.offerAll(
            result.newlyUnlocked.map { achievement ->
                PendingReward(
                    source = RewardSource.Achievement(achievement),
                    bundle = achievement.reward,
                )
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
