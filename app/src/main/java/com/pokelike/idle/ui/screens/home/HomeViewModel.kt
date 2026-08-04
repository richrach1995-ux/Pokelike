package com.pokelike.idle.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.manager.GameClock
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.DurationFormatter
import com.pokelike.idle.util.NumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel des Hauptbildschirms.
 *
 * Fuehrt die Quellen der Spiel-Engine und den Spielstand zu genau einem
 * [HomeUiState] zusammen. Der Screen bekommt dadurch einen einzigen Zustand
 * statt mehrerer Flows, die er selbst kombinieren muesste - was bei mehreren
 * Quellen unweigerlich zu kurzzeitig widerspruechlichen Anzeigen fuehrt (etwa
 * "pausiert", waehrend die Zeit noch weiterlaeuft).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    gameClock: GameClock,
    gameRepository: GameRepository,
    durationFormatter: DurationFormatter,
    numberFormatter: NumberFormatter,
    dispatchers: DispatcherProvider,
) : ViewModel() {

    /**
     * Anzeigezustand des Bildschirms.
     *
     * Zur Konstruktion im Einzelnen:
     *
     * - [combine] fuehrt Takt, Laufzustand und Spielstand zusammen.
     * - [flowOn] verlagert Zeit- und Zahlenformatierung auf den
     *   Default-Dispatcher. Bei zehn Aktualisierungen pro Sekunde soll die
     *   Zeichenkettenerzeugung nicht auf dem UI-Thread liegen.
     * - [distinctUntilChanged] unterdrueckt Aktualisierungen, bei denen sich am
     *   sichtbaren Zustand nichts geaendert hat, und erspart Compose die
     *   Neuzeichnung.
     * - [stateIn] mit [SharingStarted.WhileSubscribed] beendet die Sammlung
     *   fuenf Sekunden nachdem der letzte Beobachter verschwunden ist. Das
     *   Zeitfenster ueberbrueckt eine Bildschirmdrehung, ohne dass der Flow
     *   dabei neu aufgebaut wird.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        gameClock.tick,
        gameClock.isRunning,
        gameRepository.gameState,
    ) { tick, isRunning, gameState ->
        HomeUiState(
            isEngineRunning = isRunning,
            sessionTime = durationFormatter.formatCompact(tick.elapsedMillis),
            tickCount = tick.index,
            coins = numberFormatter.format(gameState[ResourceType.COINS]),
            diamonds = numberFormatter.format(gameState[ResourceType.DIAMONDS]),
        )
    }
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(),
        )

    private companion object {
        /**
         * Wartezeit, bevor der Flow nach dem letzten Beobachter stoppt.
         *
         * Fuenf Sekunden sind die uebliche Wahl: lang genug fuer eine
         * Konfigurationsaenderung, kurz genug, um im Hintergrund nicht
         * unnoetig zu rechnen.
         */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
