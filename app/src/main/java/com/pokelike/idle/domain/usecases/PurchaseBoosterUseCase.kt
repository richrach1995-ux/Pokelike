package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.domain.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

/** Ergebnis eines Booster-Kaufs. */
sealed interface BoosterPurchaseResult {

    data class Success(val state: GameState, val type: BoosterType) : BoosterPurchaseResult

    /** Der Diamantenstand reicht nicht. */
    data object NotAffordable : BoosterPurchaseResult

    /**
     * Die Laufzeit ist bereits ausgereizt.
     *
     * Von [NotAffordable] getrennt, weil die Oberflaeche beides
     * unterschiedlich erklaeren muss: Im einen Fall fehlen Diamanten, im
     * anderen wuerde der Kauf verpuffen. Ohne diese Unterscheidung koennte ein
     * Spieler zahlen und nichts dafuer bekommen.
     */
    data object AlreadyAtMaximum : BoosterPurchaseResult
}

/**
 * Kauft einen Booster mit Diamanten.
 *
 * Pruefung, Abbuchung und Start liegen in einer Operation. Waeren es getrennte
 * Schritte, koennte zwischen Abbuchung und Start etwas dazwischenkommen - und
 * der Spieler haette bezahlt, ohne dass etwas laeuft.
 */
@Singleton
class PurchaseBoosterUseCase @Inject constructor(
    private val startBooster: StartBoosterUseCase,
) {

    operator fun invoke(
        state: GameState,
        type: BoosterType,
        nowMillis: Long,
    ): BoosterPurchaseResult {
        val running = state.boosters[type]
        if (running != null &&
            running.remainingAt(nowMillis) >= type.maxStackedDurationMillis
        ) {
            return BoosterPurchaseResult.AlreadyAtMaximum
        }

        // spend prueft den Bestand und bucht in einem Schritt ab.
        val paid = state.spend(type.price) ?: return BoosterPurchaseResult.NotAffordable

        return BoosterPurchaseResult.Success(
            state = startBooster(paid, type, nowMillis),
            type = type,
        )
    }
}
