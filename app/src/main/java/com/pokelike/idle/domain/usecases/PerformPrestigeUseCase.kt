package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.GameState
import javax.inject.Inject

/**
 * Ergebnis eines Prestige-Versuchs.
 */
sealed interface PrestigeResult {

    data class Success(
        val state: GameState,
        val pointsEarned: BigNumber,
    ) : PrestigeResult

    /**
     * Die Mindestpunktzahl ist nicht erreicht.
     *
     * Der einzige Fehlerfall, und ein wichtiger: Ohne diese Pruefung koennte
     * ein Spieler fuer null Punkte zuruecksetzen und seinen gesamten
     * Fortschritt ohne Gegenwert verlieren.
     */
    data object NotEnoughPoints : PrestigeResult
}

/**
 * Fuehrt einen Prestige-Reset aus.
 *
 * Berechnung und Ausfuehrung liegen in einer Operation. Waeren sie getrennt,
 * koennte zwischen beiden ein Takt Leerlaufeinkommen eintreffen, und der
 * Spieler bekaeme eine andere Punktzahl gutgeschrieben als die, die ihm
 * angezeigt wurde - bei einem unumkehrbaren Vorgang die schlechteste
 * denkbare Abweichung.
 */
class PerformPrestigeUseCase @Inject constructor(
    private val calculatePrestige: CalculatePrestigeUseCase,
) {

    operator fun invoke(state: GameState): PrestigeResult {
        val info = calculatePrestige(state)
        if (!info.canPrestige) return PrestigeResult.NotEnoughPoints

        return PrestigeResult.Success(
            state = state.afterPrestige(info.pointsOnReset),
            pointsEarned = info.pointsOnReset,
        )
    }
}
