package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.ClickModifiers
import com.pokelike.idle.domain.model.GameModifiers
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.UpgradeEffect
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Leitet aus dem Spielstand die wirksamen Werte ab.
 *
 * Die einzige Stelle, an der [UpgradeEffect] ausgewertet wird. Alles Weitere -
 * Klick, Einkommen, Preise, Offline - liest ausschliesslich das Ergebnis.
 *
 * Upgrades und laufende Booster landen dabei im selben Topf. Ein Booster ist
 * inhaltlich ein Upgrade auf Zeit, und beide Quellen getrennt auszuwerten
 * hiesse, zwei Kopien derselben Balancing-Regeln zu fuehren.
 *
 * **Die Uhrzeit ist hier kein Parameter.** Ausgewertet werden alle Booster im
 * Spielstand; das Ablaufen erledigt
 * [com.pokelike.idle.manager.BoosterManager], indem er sie im Sekundentakt aus
 * dem Spielstand entfernt. Das ist die verlaesslichere Aufteilung: Waere das
 * Ablaufen nur hier beruecksichtigt, blieben abgelaufene Booster im Spielstand
 * stehen, wuerden mitgesichert und muessten von jeder Anzeige erneut gefiltert
 * werden.
 *
 * Zur Verrechnungsart, denn sie ist Balancing und keine Geschmacksfrage:
 *
 * - **Faktoren werden multipliziert.** Zweimal "doppelt" ergibt vierfach.
 *   Additiv waeren spaete Upgrades wirkungslos, weil ihr Beitrag neben dem
 *   bereits Erreichten verschwaende.
 * - **Zuschlaege werden addiert.** Fuenf Prozent Kritchance plus fuenf Prozent
 *   ergeben zehn - so liest es der Spieler, und so soll es sein.
 * - **Nachlaesse werden addiert und gedeckelt.** Multiplikativ ergaeben fuenf
 *   und zehn Prozent 14,5 statt 15 und wirkten wie ein Rechenfehler.
 *
 * Der Prestige-Bonus wird zuletzt auf Klickertrag und Leerlaufeinkommen
 * angewendet. Er wirkt damit auf alles, was Upgrades bereits bewirkt haben -
 * genau die Belohnung, die einen Neuanfang schneller macht als den vorigen
 * Durchlauf.
 */
@Singleton
class CalculateModifiersUseCase @Inject constructor(
    private val calculatePrestige: CalculatePrestigeUseCase,
) {

    /**
     * @param includeBoosters Ob laufende Booster mitgerechnet werden. Der
     *   Offline-Ertrag setzt das auf `false`: Booster belohnen aktives Spielen,
     *   und ein Ertrag, der auch bei geschlossener App anfaellt, machte das
     *   Schliessen der App zur besten Nutzung eines Boosters.
     */
    operator fun invoke(state: GameState, includeBoosters: Boolean = true): GameModifiers {
        val base = ClickModifiers.base()

        val accumulator = Accumulator(
            clickMultiplier = base.multiplier,
            clickFlatBonus = base.flatBonus,
            criticalChance = base.criticalChance,
            criticalMultiplier = base.criticalMultiplier,
            offlineEfficiency = GameModifiers.base().offlineEfficiency,
        )

        state.upgrades.asSet().forEach { upgrade -> accumulator.apply(upgrade.effect) }

        if (includeBoosters) {
            state.boosters.active.values.forEach { booster ->
                accumulator.apply(booster.type.effect)
            }
        }

        val prestigeMultiplier = calculatePrestige.bonusFor(state[ResourceType.PRESTIGE_POINTS])

        return GameModifiers(
            click = ClickModifiers(
                baseValue = base.baseValue,
                flatBonus = accumulator.clickFlatBonus,
                multiplier = accumulator.clickMultiplier * prestigeMultiplier,
                // Eine Kritchance ueber 100 Prozent ist nicht darstellbar; die
                // Wuerfelprobe wuerde sie ohnehin abweisen.
                criticalChance = accumulator.criticalChance.coerceAtMost(1.0),
                criticalMultiplier = accumulator.criticalMultiplier,
            ),
            incomeMultiplier = accumulator.incomeMultiplier * prestigeMultiplier,
            buildingIncomeMultipliers = accumulator.buildingMultipliers.toMap(),
            buildingDiscount = accumulator.buildingDiscount
                .coerceAtMost(GameModifiers.MAX_DISCOUNT),
            offlineEfficiency = accumulator.offlineEfficiency,
            prestigeMultiplier = prestigeMultiplier,
        )
    }

    /**
     * Sammelt die Wirkungen aller Quellen.
     *
     * Als eigener Typ statt einer Handvoll lokaler Variablen, damit Upgrades
     * und Booster durch dieselbe Auswertung laufen koennen, statt sie zweimal
     * zu schreiben.
     */
    private class Accumulator(
        var clickMultiplier: Double,
        var clickFlatBonus: BigNumber,
        var criticalChance: Double,
        var criticalMultiplier: Double,
        var offlineEfficiency: Double,
    ) {
        var incomeMultiplier: Double = 1.0
        var buildingDiscount: Double = 0.0
        val buildingMultipliers: MutableMap<BuildingType, Double> = mutableMapOf()

        fun apply(effect: UpgradeEffect) {
            when (effect) {
                is UpgradeEffect.ClickMultiplier ->
                    clickMultiplier *= effect.factor

                is UpgradeEffect.ClickFlatBonus ->
                    clickFlatBonus += effect.amount

                is UpgradeEffect.CriticalChanceBonus ->
                    criticalChance += effect.amount

                is UpgradeEffect.CriticalMultiplierBonus ->
                    criticalMultiplier += effect.amount

                is UpgradeEffect.IncomeMultiplier ->
                    incomeMultiplier *= effect.factor

                is UpgradeEffect.BuildingIncomeMultiplier ->
                    buildingMultipliers[effect.building] =
                        (buildingMultipliers[effect.building] ?: 1.0) * effect.factor

                is UpgradeEffect.BuildingDiscount ->
                    buildingDiscount += effect.fraction

                is UpgradeEffect.OfflineEfficiencyBonus ->
                    offlineEfficiency += effect.amount
            }
        }
    }
}
