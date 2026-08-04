package com.pokelike.idle.domain.usecases

import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Berechnet Gebaeudepreise.
 *
 * Der Preis des n-ten Exemplars ist `grundpreis * wachstum^n`. Fuer einen
 * Sammelkauf von [count] Exemplaren ab Bestand [owned] ergibt sich damit eine
 * geometrische Reihe:
 *
 * ```
 * preis = grundpreis * wachstum^owned * (wachstum^count - 1) / (wachstum - 1)
 * ```
 *
 * Die geschlossene Formel statt einer Schleife ist hier nicht Feinschliff,
 * sondern notwendig: Ein Kauf von zehntausend Exemplaren waere sonst
 * zehntausend Multiplikationen mit [BigNumber] - waehrend eines Frames.
 *
 * Alles laeuft in [BigNumber], weil `1.15^n` bereits ab etwa 5.300 Exemplaren
 * den Bereich von [Double] verlaesst. Mit `Math.pow` waere der Preis ab dort
 * `Infinity` und das Gebaeude unkaufbar.
 */
@Singleton
class BuildingCostCalculator @Inject constructor() {

    /**
     * Preis fuer [count] weitere Exemplare.
     *
     * @param discount Preisnachlass von 0 bis unter 1. Angriffspunkt fuer
     *   Upgrades der Art "Gebaeude sind guenstiger".
     */
    fun priceFor(
        building: BuildingType,
        owned: Int,
        count: Int = 1,
        growth: Double = GameConfig.BUILDING_COST_GROWTH,
        discount: Double = 0.0,
    ): BigNumber {
        require(owned >= 0) { "Negativer Bestand: $owned" }
        require(count >= 0) { "Negative Kaufmenge: $count" }
        require(growth > 1.0) { "Wachstum muss ueber 1 liegen: $growth" }
        require(discount in 0.0..MAX_DISCOUNT) { "Nachlass ausserhalb des Bereichs: $discount" }

        if (count == 0) return BigNumber.ZERO

        val startPrice = BigNumber.of(building.basePrice) * BigNumber.pow(growth, owned.toDouble())
        val seriesFactor = (BigNumber.pow(growth, count.toDouble()) - BigNumber.ONE) /
            BigNumber.of(growth - 1.0)

        return startPrice * seriesFactor * (1.0 - discount)
    }

    /**
     * Groesste Menge, die sich mit [coins] noch bezahlen laesst.
     *
     * Statt hochzuzaehlen wird die Reihe nach der Menge aufgeloest:
     *
     * ```
     * count = log(1 + coins * (wachstum - 1) / startpreis) / log(wachstum)
     * ```
     *
     * Das Hochzaehlen waere bei grossen Kontostaenden untragbar - der Spieler
     * kann sich im spaeten Spiel zehntausende Exemplare leisten, und jede
     * Pruefung waere eine BigNumber-Rechnung.
     *
     * Das Ergebnis wird abgerundet und anschliessend gegengeprueft. Der
     * Logarithmus arbeitet mit [Double]-Genauigkeit; ohne die Korrektur koennte
     * er um ein Exemplar danebenliegen und der Kauf im Aufrufer scheitern.
     */
    fun maxAffordable(
        building: BuildingType,
        owned: Int,
        coins: BigNumber,
        growth: Double = GameConfig.BUILDING_COST_GROWTH,
        discount: Double = 0.0,
        limit: Int = GameConfig.MAX_BULK_PURCHASE,
    ): Int {
        require(limit >= 0) { "Negative Obergrenze: $limit" }
        if (coins.isZero || coins.isNegative || limit == 0) return 0

        val startPrice = priceFor(building, owned, count = 1, growth = growth, discount = discount)
        if (coins < startPrice) return 0

        // 1 + coins * (wachstum - 1) / startpreis
        val ratio = BigNumber.ONE + (coins * (growth - 1.0)) / startPrice
        val estimate = (ratio.log10() / kotlin.math.log10(growth)).toInt()

        var count = estimate.coerceIn(1, limit)

        // Nach unten korrigieren, solange der Preis den Kontostand uebersteigt.
        while (count > 1 &&
            priceFor(building, owned, count, growth, discount) > coins
        ) {
            count--
        }

        // Nach oben korrigieren, solange noch ein weiteres Exemplar passt.
        while (count < limit &&
            priceFor(building, owned, count + 1, growth, discount) <= coins
        ) {
            count++
        }

        return count
    }

    private companion object {
        /**
         * Hoechster zulaessiger Nachlass.
         *
         * Ein Nachlass von 1.0 wuerde Gebaeude kostenlos machen und das
         * gesamte Wirtschaftssystem aushebeln.
         */
        const val MAX_DISCOUNT = 0.95
    }
}
