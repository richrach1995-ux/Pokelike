package com.pokelike.idle.domain.model

import com.pokelike.idle.config.GameConfig

/**
 * Alle Werte, die den Ertrag eines Klicks bestimmen.
 *
 * Als eigenes Objekt und nicht als lose Parameterliste: Es ist die
 * Schnittstelle zwischen dem Klick-System und allem, was darauf einwirkt -
 * Upgrades, Booster, Prestige-Boni, Events, Skins. Jede dieser Quellen wird
 * spaeter dieses Objekt veraendern, statt in die Klickberechnung
 * hineinzugreifen. Damit bleibt die Berechnung selbst unveraendert, egal wie
 * viele Einfluesse hinzukommen.
 *
 * Die Reihenfolge der Verrechnung liegt in [PerformClickUseCase] fest und ist
 * Teil des Balancings: erst Grundwert und flache Zuschlaege addieren, dann
 * multiplizieren. Andersherum waeren flache Boni im spaeten Spiel wertlos.
 *
 * @property baseValue Grundertrag je Klick.
 * @property flatBonus Zuschlag, der vor allen Faktoren addiert wird.
 * @property multiplier Gesamtfaktor aus Upgrades, Boostern und Prestige.
 * @property criticalChance Wahrscheinlichkeit eines kritischen Treffers, 0..1.
 * @property criticalMultiplier Faktor eines kritischen Treffers.
 */
data class ClickModifiers(
    val baseValue: BigNumber,
    val flatBonus: BigNumber,
    val multiplier: Double,
    val criticalChance: Double,
    val criticalMultiplier: Double,
) {

    init {
        require(criticalChance in 0.0..1.0) {
            "Kritwahrscheinlichkeit ausserhalb von 0..1: $criticalChance"
        }
        require(criticalMultiplier >= 1.0) {
            "Kritfaktor unter 1 wuerde einen Treffer bestrafen: $criticalMultiplier"
        }
        require(multiplier >= 0.0) { "Negativer Faktor: $multiplier" }
    }

    /**
     * Erwarteter Ertrag pro Klick, ohne Zufallsanteil.
     *
     * Fuer die Anzeige "Muenzen pro Klick". Dort gehoert der Erwartungswert
     * hin und nicht das Ergebnis des letzten Klicks: Ein Wert, der bei jedem
     * kritischen Treffer springt, ist als Kennzahl unbrauchbar - der Spieler
     * kann damit nicht beurteilen, ob sich ein Upgrade gelohnt hat.
     *
     * Der kritische Treffer geht als Erwartungswert ein: Bei fuenf Prozent
     * Chance auf fuenffachen Ertrag liegt der Durchschnitt zwanzig Prozent
     * ueber dem Grundwert.
     */
    fun expectedValuePerClick(comboMultiplier: Double = 1.0): BigNumber {
        val criticalFactor = 1.0 + criticalChance * (criticalMultiplier - 1.0)
        return (baseValue + flatBonus) * multiplier * comboMultiplier * criticalFactor
    }

    companion object {

        /**
         * Werte eines Spielers ohne jede Verbesserung.
         *
         * Ab dem Upgrade-Schritt wird dieses Objekt aus dem Spielstand
         * berechnet und diese Funktion nur noch als Ausgangspunkt genutzt.
         */
        fun base(): ClickModifiers = ClickModifiers(
            baseValue = BigNumber.of(GameConfig.BASE_COINS_PER_CLICK),
            flatBonus = BigNumber.ZERO,
            multiplier = 1.0,
            criticalChance = GameConfig.BASE_CRITICAL_CHANCE,
            criticalMultiplier = GameConfig.BASE_CRITICAL_MULTIPLIER,
        )
    }
}
