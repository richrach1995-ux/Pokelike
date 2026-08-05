package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.GameModifiers
import com.pokelike.idle.domain.model.GameState
import java.time.ZoneId
import javax.inject.Inject

/** Ergebnis einer Abholung des Tagesbonus. */
sealed interface DailyRewardClaimResult {

    data class Success(
        val state: GameState,
        val status: DailyRewardStatus,
    ) : DailyRewardClaimResult

    /**
     * Heute ist nichts abzuholen.
     *
     * Tritt regulaer auf: Der Spieler kann die App am selben Tag mehrfach
     * oeffnen, und ein zweiter Tippen auf einen stehengebliebenen Dialog darf
     * keine zweite Gutschrift ausloesen.
     */
    data object NotDue : DailyRewardClaimResult
}

/**
 * Holt den Tagesbonus ab.
 *
 * Bewusst kein Automatismus beim Start: Die Abholung ist im Genre ein bewusst
 * gesetzter Moment, und der Zyklus wirkt nur, wenn der Spieler sieht, wie weit
 * er gekommen ist. Ein still gutgeschriebener Bonus waere fuer den Spieler
 * nicht von einem Fehler zu unterscheiden.
 *
 * Pruefung und Gutschrift liegen in einer Operation. Wuerde die Oberflaeche
 * erst pruefen und dann buchen, koennte zwischen beidem der Tageswechsel
 * liegen - und der Spieler bekaeme den Bonus fuer einen Tag, an dem er ihn
 * schon hatte.
 */
class ClaimDailyRewardUseCase @Inject constructor(
    private val evaluate: EvaluateDailyRewardUseCase,
) {

    operator fun invoke(
        state: GameState,
        modifiers: GameModifiers,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DailyRewardClaimResult {
        val status = evaluate(state, modifiers, nowMillis, zone)
            ?: return DailyRewardClaimResult.NotDue

        val granted = state.grant(status.reward)

        return DailyRewardClaimResult.Success(
            state = granted.copy(
                login = granted.login.withClaim(
                    streak = status.streakAfterClaim,
                    claimedAtMillis = nowMillis,
                    protectionSpent = status.protectionUsed,
                ),
            ),
            status = status,
        )
    }
}
