package com.pokelike.idle.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.manager.GameClock
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.DurationFormatter
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
 * Fuehrt die Quellen der Spiel-Engine zu genau einem [HomeUiState] zusammen.
 * Der Screen bekommt dadurch einen einzigen Zustand statt mehrerer Flows, die
 * er selbst kombinieren muesste - was bei mehreren Quellen unweigerlich zu
 * kurzzeitig widerspruechlichen Anzeigen fuehrt (etwa "pausiert", waehrend die
 * Zeit noch weiterlaeuft).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    gameClock: GameClock,
    durationFormatter: DurationFormatter,
    dispatchers: DispatcherProvider,
) : ViewModel() {

    /**
     * Anzeigezustand des Bildschirms.
     *
     * Zur Konstruktion im Einzelnen:
     *
     * - [combine] fuehrt Takt und Laufzustand zusammen.
     * - [flowOn] verlagert die Formatierung auf den Default-Dispatcher. Bei
     *   zehn Aktualisierungen pro Sekunde soll die Zeichenkettenerzeugung nicht
     *   auf dem UI-Thread liegen.
     * - [distinctUntilChanged] unterdrueckt Aktualisierungen, bei denen sich am
     *   sichtbaren Zustand nichts geaendert hat. Das greift, sobald der Screen
     *   nur noch sekundengenaue Werte anzeigt, und erspart Compose die
     *   Neuzeichnung.
     * - [stateIn] mit [SharingStarted.WhileSubscribed] beendet die Sammlung
     *   fuenf Sekunden nachdem der letzte Beobachter verschwunden ist. Das
     *   Zeitfenster ueberbrueckt eine Bildschirmdrehung, ohne dass der Flow
     *   dabei neu aufgebaut wird.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        gameClock.tick,
        gameClock.isRunning,
    ) { tick, isRunning ->
        HomeUiState(
            isEngineRunning = isRunning,
            sessionTime = durationFormatter.formatCompact(tick.elapsedMillis),
            tickCount = tick.index,
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
