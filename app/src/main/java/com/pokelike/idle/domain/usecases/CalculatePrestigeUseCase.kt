package com.pokelike.idle.domain.usecases

import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.PrestigeInfo
import com.pokelike.idle.domain.model.ResourceType
import kotlin.math.floor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Berechnet den Stand des Prestige-Systems.
 *
 * Die Punkte folgen der Wurzelformel:
 *
 * ```
 * gesamtpunkte = abrunden(wurzel(lebenssumme / basis))
 * ```
 *
 * Die Wurzel daempft den Zuwachs bewusst: Der zehnfache Ertrag bringt nur etwa
 * dreifach so viele Punkte. Ohne diese Daempfung waere ein einziger sehr langer
 * Durchlauf jedem regelmaessigen Spielen ueberlegen - und das Prestige-System
 * belohnt gerade das Gegenteil.
 *
 * Zur Ermittlung der noch offenen Punkte:
 * Abgezogen wird nicht der aktuelle Kontostand an Prestige-Punkten, sondern die
 * Lebenssumme der je erhaltenen Punkte. Das ist wesentlich fuer die Zukunft:
 * Sobald sich Prestige-Punkte ausgeben lassen, wuerde der Kontostand sinken -
 * und der Spieler bekaeme dieselben Punkte ein zweites Mal.
 */
@Singleton
class CalculatePrestigeUseCase @Inject constructor() {

    operator fun invoke(
        state: GameState,
        baseCoins: Double = GameConfig.PRESTIGE_BASE_COINS,
        bonusPerPoint: Double = GameConfig.PRESTIGE_BONUS_PER_POINT,
        minimumPoints: Double = GameConfig.PRESTIGE_MIN_POINTS,
    ): PrestigeInfo {
        require(baseCoins > 0.0) { "Basis muss positiv sein: $baseCoins" }
        require(bonusPerPoint >= 0.0) { "Negativer Bonus: $bonusPerPoint" }

        val lifetimeCoins = state.statistics.lifetimeEarned[ResourceType.COINS]
        val claimedPoints = state.statistics.lifetimeEarned[ResourceType.PRESTIGE_POINTS]
        val currentPoints = state[ResourceType.PRESTIGE_POINTS]

        val totalPoints = totalPointsFor(lifetimeCoins, baseCoins)
        val pointsOnReset = (totalPoints - claimedPoints).coerceAtLeast(BigNumber.ZERO)

        val nextPointTarget = coinsRequiredFor(totalPoints + BigNumber.ONE, baseCoins)
        val currentPointTarget = coinsRequiredFor(totalPoints, baseCoins)

        return PrestigeInfo(
            currentPoints = currentPoints,
            pointsOnReset = pointsOnReset,
            currentBonus = bonusFor(currentPoints, bonusPerPoint),
            bonusAfterReset = bonusFor(currentPoints + pointsOnReset, bonusPerPoint),
            coinsForNextPoint = nextPointTarget,
            progressToNextPoint = progressBetween(
                lifetimeCoins = lifetimeCoins,
                from = currentPointTarget,
                to = nextPointTarget,
            ),
            canPrestige = pointsOnReset >= BigNumber.of(minimumPoints),
        )
    }

    /**
     * Dauerhafter Faktor aus einer Punktzahl.
     *
     * Additiv: Hundert Punkte ergeben den dreifachen Ertrag. Multiplikativ
     * waeren die Zahlen nach wenigen Durchlaeufen so gross, dass jedes weitere
     * Balancing wirkungslos bliebe.
     *
     * Die Umwandlung nach [Double] ist gedeckelt. Erreichbar ist eine solche
     * Punktzahl nicht, aber ein `Infinity` an dieser Stelle wuerde jede
     * nachfolgende Rechnung unbrauchbar machen.
     */
    fun bonusFor(
        points: BigNumber,
        bonusPerPoint: Double = GameConfig.PRESTIGE_BONUS_PER_POINT,
    ): Double {
        if (!points.isPositive) return 1.0
        val plainPoints = points.toDouble()
        val safePoints = if (plainPoints.isFinite()) plainPoints else MAX_POINTS_FOR_BONUS
        return 1.0 + safePoints.coerceAtMost(MAX_POINTS_FOR_BONUS) * bonusPerPoint
    }

    /** Gesamtpunkte, die eine Lebenssumme wert ist. */
    private fun totalPointsFor(lifetimeCoins: BigNumber, baseCoins: Double): BigNumber {
        if (!lifetimeCoins.isPositive) return BigNumber.ZERO
        val ratio = lifetimeCoins / BigNumber.of(baseCoins)
        if (ratio < BigNumber.ONE) return BigNumber.ZERO
        return floorToInteger(ratio.sqrt())
    }

    /** Lebenssumme, die fuer eine bestimmte Punktzahl noetig ist. */
    private fun coinsRequiredFor(points: BigNumber, baseCoins: Double): BigNumber {
        if (!points.isPositive) return BigNumber.of(baseCoins)
        return points.pow(2.0) * baseCoins
    }

    /**
     * Fortschritt zwischen zwei Schwellen.
     *
     * Gerechnet im Logarithmus statt linear. Zwischen zwei Punktschwellen
     * liegen im spaeten Spiel Groessenordnungen; ein linearer Balken stuende
     * dort ueber Stunden bei nahezu null und traege keine Information mehr.
     */
    private fun progressBetween(
        lifetimeCoins: BigNumber,
        from: BigNumber,
        to: BigNumber,
    ): Float {
        if (!lifetimeCoins.isPositive || !to.isPositive) return 0f
        if (lifetimeCoins >= to) return 1f

        val start = if (from.isPositive) from.log10() else 0.0
        val end = to.log10()
        val current = lifetimeCoins.log10()

        val span = end - start
        if (span <= 0.0) return 0f

        return ((current - start) / span).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Rundet auf eine ganze Zahl ab.
     *
     * Oberhalb von etwa 1e15 hat ein [Double] ohnehin keine Nachkommastellen
     * mehr; dort waere das Abrunden wirkungslos und die Umwandlung nur ein
     * Genauigkeitsverlust.
     */
    private fun floorToInteger(value: BigNumber): BigNumber {
        if (value.exponent >= INTEGRAL_EXPONENT) return value
        return BigNumber.of(floor(value.toDouble()))
    }

    private companion object {
        const val INTEGRAL_EXPONENT = 15

        /**
         * Obergrenze der Punktzahl, die in den Bonus eingeht.
         *
         * Weit jenseits jedes erreichbaren Spielstands. Sie verhindert, dass
         * eine unerwartet grosse Zahl den Faktor auf `Infinity` treibt.
         */
        const val MAX_POINTS_FOR_BONUS = 1e15
    }
}
