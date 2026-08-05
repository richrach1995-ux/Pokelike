package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.RewardedAdPlacement
import javax.inject.Inject
import javax.inject.Singleton

/** Ergebnis der Gutschrift nach einem Belohnungsvideo. */
sealed interface AdRewardResult {

    data class Success(
        val state: GameState,
        val placement: RewardedAdPlacement,
    ) : AdRewardResult

    /**
     * Die Wartezeit laeuft noch.
     *
     * Tritt regulaer auf und ist keine Fehlbedienung: Zwischen dem Antippen
     * und dem Ende des Videos vergehen Sekunden, und in einem zweiten Fenster
     * kann in der Zwischenzeit dieselbe Belohnung abgeholt worden sein.
     */
    data object OnCooldown : AdRewardResult
}

/**
 * Schreibt die Belohnung eines Videos gut und startet die Wartezeit.
 *
 * Beides in einer Operation. Getrennt koennte zwischen Gutschrift und
 * Wartezeit ein zweiter Aufruf durchrutschen, und der Spieler bekaeme zwei
 * Booster fuer ein Video.
 *
 * Die Wartezeit beginnt ausdruecklich erst hier - also nur, wenn das Video
 * auch zu Ende gesehen wurde. Sie ist der Preis der Belohnung und nicht die
 * Strafe fuer einen Abbruch oder fuer fehlendes Netz.
 */
@Singleton
class GrantAdRewardUseCase @Inject constructor(
    private val startBooster: StartBoosterUseCase,
) {

    operator fun invoke(
        state: GameState,
        placement: RewardedAdPlacement,
        nowMillis: Long,
    ): AdRewardResult {
        if (!state.ads.isReadyAt(placement, nowMillis)) return AdRewardResult.OnCooldown

        val boosted = startBooster(state, placement.rewardedBooster, nowMillis)

        return AdRewardResult.Success(
            state = boosted.copy(ads = boosted.ads.withCooldownStarted(placement, nowMillis)),
            placement = placement,
        )
    }
}
