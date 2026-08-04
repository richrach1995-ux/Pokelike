package com.pokelike.idle.domain.model

import com.pokelike.idle.config.GameConfig

/**
 * Alle wirksamen Werte eines Spielstands, an einer Stelle gebuendelt.
 *
 * Der Kern der Erweiterbarkeit: Upgrades, und spaeter auch Booster, Events,
 * Prestige-Boni und Skins, veraendern ausschliesslich dieses Objekt. Die
 * Berechnungen fuer Klick, Einkommen, Preise und Offline-Ertrag lesen daraus -
 * sie kennen keine einzige Quelle dieser Werte.
 *
 * Ohne diese Zwischenschicht muesste jede der vier Berechnungen alle Quellen
 * selbst durchgehen, und eine neue Quelle waere eine Aenderung an vier Stellen.
 *
 * @property click Werte der Klickberechnung.
 * @property incomeMultiplier Gesamtfaktor auf das Leerlaufeinkommen.
 * @property buildingIncomeMultipliers Zusaetzliche Faktoren je Gebaeudeart.
 * @property buildingDiscount Preisnachlass auf Gebaeude, 0 bis unter 1.
 * @property offlineEfficiency Anteil des Einkommens, der offline anfaellt.
 * @property prestigeMultiplier Faktor aus den Prestige-Punkten.
 *
 *   Ausschliesslich zur Anzeige gedacht. Er ist in [click] und
 *   [incomeMultiplier] **bereits enthalten** - wer ihn zusaetzlich anwendet,
 *   rechnet ihn doppelt. Getrennt gefuehrt wird er nur, weil der
 *   Prestige-Bildschirm den erreichten Bonus ausweisen muss.
 */
data class GameModifiers(
    val click: ClickModifiers,
    val incomeMultiplier: Double,
    val buildingIncomeMultipliers: Map<BuildingType, Double>,
    val buildingDiscount: Double,
    val offlineEfficiency: Double,
    val prestigeMultiplier: Double,
) {

    init {
        require(incomeMultiplier >= 0.0) { "Negativer Einkommensfaktor: $incomeMultiplier" }
        require(buildingDiscount in 0.0..MAX_DISCOUNT) {
            "Nachlass ausserhalb des Bereichs: $buildingDiscount"
        }
        require(offlineEfficiency >= 0.0) { "Negative Offline-Effizienz: $offlineEfficiency" }
        require(prestigeMultiplier >= 1.0) {
            "Prestige-Faktor unter 1 wuerde bestrafen: $prestigeMultiplier"
        }
    }

    companion object {

        /**
         * Hoechster zulaessiger Gesamtnachlass.
         *
         * Ein Nachlass von 1.0 wuerde Gebaeude kostenlos machen. Der Deckel
         * begrenzt zugleich, wie weit sich Nachlaesse mehrerer Upgrades
         * aufsummieren duerfen.
         */
        const val MAX_DISCOUNT: Double = 0.95

        /** Werte eines Spielstands ohne jedes Upgrade. */
        fun base(): GameModifiers = GameModifiers(
            click = ClickModifiers.base(),
            incomeMultiplier = 1.0,
            buildingIncomeMultipliers = emptyMap(),
            buildingDiscount = 0.0,
            offlineEfficiency = GameConfig.OFFLINE_EFFICIENCY,
            prestigeMultiplier = 1.0,
        )
    }
}
