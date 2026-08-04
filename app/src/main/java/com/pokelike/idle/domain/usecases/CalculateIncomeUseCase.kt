package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Berechnet den Ertrag pro Sekunde aus dem Gebaeudebestand.
 *
 * Reine Funktion ohne Zustand. Sie wird an drei Stellen gebraucht, und alle
 * drei muessen dasselbe Ergebnis liefern: die laufende Gutschrift je Takt, die
 * Anzeige "Muenzen pro Sekunde" und die Offline-Berechnung. Waere die Formel
 * mehrfach vorhanden, liefen die Werte irgendwann auseinander - und der Spieler
 * bemerkt genau das sofort.
 *
 * Faktoren kommen als Parameter herein statt aus dem Zustand gelesen zu werden.
 * Die Rechnung bleibt dadurch rein, und es lassen sich beliebige Kombinationen
 * durchspielen, ohne einen passenden Spielstand bauen zu muessen.
 */
@Singleton
class CalculateIncomeUseCase @Inject constructor() {

    /**
     * @param multiplier Gesamtfaktor auf alle Gebaeude.
     * @param perBuildingMultipliers Zusaetzliche Faktoren je Gebaeudeart. Sie
     *   wirken nur auf die jeweilige Art und werden vor dem Gesamtfaktor
     *   angewendet.
     */
    operator fun invoke(
        buildings: BuildingInventory,
        multiplier: Double = 1.0,
        perBuildingMultipliers: Map<BuildingType, Double> = emptyMap(),
    ): BigNumber {
        require(multiplier >= 0.0) { "Negativer Faktor: $multiplier" }
        if (buildings.isEmpty || multiplier == 0.0) return BigNumber.ZERO

        var total = BigNumber.ZERO
        buildings.asMap().forEach { (type, count) ->
            total += incomeFor(
                building = type,
                count = count,
                multiplier = perBuildingMultipliers[type] ?: 1.0,
            )
        }

        return total * multiplier
    }

    /**
     * Ertrag eines einzelnen Gebaeudetyps pro Sekunde.
     *
     * Getrennt zugaenglich, weil die Gebaeudeliste ihn je Zeile anzeigt - der
     * Spieler soll erkennen koennen, welches Gebaeude tatsaechlich traegt.
     */
    fun incomeFor(
        building: BuildingType,
        count: Int,
        multiplier: Double = 1.0,
    ): BigNumber {
        require(count >= 0) { "Negative Anzahl: $count" }
        require(multiplier >= 0.0) { "Negativer Faktor: $multiplier" }
        if (count == 0 || multiplier == 0.0) return BigNumber.ZERO
        return BigNumber.of(building.baseIncomePerSecond) * count.toDouble() * multiplier
    }
}
