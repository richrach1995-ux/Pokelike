package com.pokelike.idle.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.domain.model.ClickOutcome
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.manager.ClickManager
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.NumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel des Hauptbildschirms.
 *
 * Fuehrt Spielstand, Combo und Klickwerte zu genau einem [HomeUiState]
 * zusammen. Der Screen bekommt dadurch einen einzigen Zustand statt mehrerer
 * Fluesse, die er selbst kombinieren muesste - was bei mehreren Quellen
 * unweigerlich zu kurzzeitig widerspruechlichen Anzeigen fuehrt.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clickManager: ClickManager,
    gameRepository: GameRepository,
    private val numberFormatter: NumberFormatter,
    dispatchers: DispatcherProvider,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        gameRepository.gameState,
        gameRepository.isLoaded,
        clickManager.combo,
        clickManager.modifiers,
    ) { gameState, isLoaded, combo, modifiers ->
        HomeUiState(
            isReady = isLoaded,
            coins = numberFormatter.format(gameState[ResourceType.COINS]),
            diamonds = numberFormatter.format(gameState[ResourceType.DIAMONDS]),
            coinsPerClick = numberFormatter.format(
                modifiers.expectedValuePerClick(combo.multiplier),
            ),
            comboCount = combo.count,
            comboMultiplier = formatMultiplier(combo.multiplier),
            comboRemaining = combo.remainingFraction,
        )
    }
        // Formatierung gehoert nicht auf den UI-Thread: Bei laufender Combo
        // aendert sich der Zustand zehnmal pro Sekunde.
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(),
        )

    /**
     * Verarbeitet einen Klick und liefert das Ergebnis fuer die Rueckmeldung.
     *
     * Der Rueckgabewert ist Absicht. Der uebliche Weg waere ein Ereignisfluss,
     * den der Bildschirm beobachtet - hier aber der falsche: Ein Klick muss
     * noch im selben Frame sichtbar werden. Der Umweg ueber einen Fluss
     * verschoebe die Rueckmeldung um mindestens einen Frame und braechte bei
     * schnellem Tippen zusaetzlich Pufferfragen mit sich.
     *
     * Der Aufruf ist nicht `suspend`, weil alle Schritte im Arbeitsspeicher
     * ablaufen; geschrieben wird ueber den Autosave.
     */
    fun onClick(): ClickOutcome = clickManager.click()

    /** Formatiert einen Klickertrag fuer den schwebenden Hinweis. */
    fun formatEarned(outcome: ClickOutcome): String =
        "+${numberFormatter.format(outcome.earned)}"

    private fun formatMultiplier(multiplier: Double): String =
        String.format(Locale.US, "%.2fx", multiplier)

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
