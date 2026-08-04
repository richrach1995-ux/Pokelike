package com.pokelike.idle.domain.usecases

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

    operator fun invoke(state: GameState): GameModifiers {
        val base = ClickModifiers.base()

        var clickMultiplier = base.multiplier
        var clickFlatBonus = base.flatBonus
        var criticalChance = base.criticalChance
        var criticalMultiplier = base.criticalMultiplier

        var incomeMultiplier = 1.0
        val buildingMultipliers = mutableMapOf<BuildingType, Double>()
        var buildingDiscount = 0.0
        var offlineEfficiency = GameModifiers.base().offlineEfficiency

        state.upgrades.asSet().forEach { upgrade ->
            when (val effect = upgrade.effect) {
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

        val prestigeMultiplier = calculatePrestige.bonusFor(state[ResourceType.PRESTIGE_POINTS])

        return GameModifiers(
            click = ClickModifiers(
                baseValue = base.baseValue,
                flatBonus = clickFlatBonus,
                multiplier = clickMultiplier * prestigeMultiplier,
                // Eine Kritchance ueber 100 Prozent ist nicht darstellbar; die
                // Wuerfelprobe wuerde sie ohnehin abweisen.
                criticalChance = criticalChance.coerceAtMost(1.0),
                criticalMultiplier = criticalMultiplier,
            ),
            incomeMultiplier = incomeMultiplier * prestigeMultiplier,
            buildingIncomeMultipliers = buildingMultipliers,
            buildingDiscount = buildingDiscount.coerceAtMost(GameModifiers.MAX_DISCOUNT),
            offlineEfficiency = offlineEfficiency,
            prestigeMultiplier = prestigeMultiplier,
        )
    }
}
