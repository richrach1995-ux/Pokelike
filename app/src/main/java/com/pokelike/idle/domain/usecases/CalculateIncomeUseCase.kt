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
 * @param multiplier Gesamtfaktor aus Upgrades, Boostern, Prestige und Events.
 *   Als Parameter statt aus dem Zustand gelesen, damit die Rechnung rein bleibt
 *   und sich beliebige Faktoren durchspielen lassen.
 */
@Singleton
class CalculateIncomeUseCase @Inject constructor() {

    operator fun invoke(
        buildings: BuildingInventory,
        multiplier: Double = 1.0,
    ): BigNumber {
        require(multiplier >= 0.0) { "Negativer Faktor: $multiplier" }
        if (buildings.isEmpty || multiplier == 0.0) return BigNumber.ZERO

        var total = BigNumber.ZERO
        buildings.asMap().forEach { (type, count) ->
            total += incomeFor(type, count)
        }

        return total * multiplier
    }

    /**
     * Ertrag eines einzelnen Gebaeudetyps pro Sekunde.
     *
     * Getrennt zugaenglich, weil die Gebaeudeliste ihn je Zeile anzeigt - der
     * Spieler soll erkennen koennen, welches Gebaeude tatsaechlich traegt.
     */
    fun incomeFor(building: BuildingType, count: Int): BigNumber {
        require(count >= 0) { "Negative Anzahl: $count" }
        if (count == 0) return BigNumber.ZERO
        return BigNumber.of(building.baseIncomePerSecond) * count.toDouble()
    }
}
