package com.pokelike.idle.ui.screens.prestige

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.CalculatePrestigeUseCase
import com.pokelike.idle.domain.usecases.PerformPrestigeUseCase
import com.pokelike.idle.domain.usecases.PrestigeResult
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.NumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel des Prestige-Bildschirms.
 */
@HiltViewModel
class PrestigeViewModel @Inject constructor(
    private val repository: GameRepository,
    private val calculatePrestige: CalculatePrestigeUseCase,
    private val performPrestige: PerformPrestigeUseCase,
    private val numberFormatter: NumberFormatter,
    dispatchers: DispatcherProvider,
) : ViewModel() {

    val uiState: StateFlow<PrestigeUiState> = repository.gameState
        .map { state -> buildUiState(state) }
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PrestigeUiState(),
        )

    /**
     * Fuehrt den Prestige-Reset aus.
     *
     * Wie bei allen Kaeufen laeuft die Pruefung innerhalb von
     * [GameRepository.update] und damit atomar auf dem aktuellen Zustand. Hier
     * ist das besonders wichtig: Zwischen Anzeige und Ausfuehrung koennte ein
     * Takt Leerlaufeinkommen eintreffen und die Punktzahl veraendern.
     *
     * @return `true`, wenn zurueckgesetzt wurde.
     */
    fun onPrestige(): Boolean {
        var performed = false

        repository.update { state ->
            when (val result = performPrestige(state)) {
                is PrestigeResult.Success -> {
                    performed = true
                    result.state
                }

                PrestigeResult.NotEnoughPoints -> {
                    performed = false
                    state
                }
            }
        }

        return performed
    }

    private fun buildUiState(state: GameState): PrestigeUiState {
        val info = calculatePrestige(state)

        return PrestigeUiState(
            currentPoints = numberFormatter.format(info.currentPoints),
            pointsOnReset = numberFormatter.format(info.pointsOnReset),
            currentBonus = formatBonus(info.currentBonus),
            bonusAfterReset = formatBonus(info.bonusAfterReset),
            lifetimeCoins = numberFormatter.format(
                state.statistics.lifetimeEarned[ResourceType.COINS],
            ),
            coinsForNextPoint = numberFormatter.format(info.coinsForNextPoint),
            progressToNextPoint = info.progressToNextPoint,
            prestigeCount = state.statistics.prestigeCount,
            canPrestige = info.canPrestige,
        )
    }

    /**
     * Stellt einen Faktor als Zuwachs dar.
     *
     * "+24 %" statt "1,24x": Der Spieler will wissen, was er gewinnt, nicht
     * womit multipliziert wird. Bei kleinen Boni ist der Unterschied auch der
     * zwischen einer sichtbaren und einer unscheinbaren Zahl.
     */
    private fun formatBonus(multiplier: Double): String =
        String.format(Locale.US, "+%.0f %%", (multiplier - 1.0) * PERCENT)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val PERCENT = 100.0
    }
}
