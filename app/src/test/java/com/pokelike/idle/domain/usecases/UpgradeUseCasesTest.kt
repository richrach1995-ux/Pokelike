package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameModifiers
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.GameStatistics
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.UpgradeInventory
import com.pokelike.idle.domain.model.UpgradeType
import org.junit.Test

class CalculateModifiersUseCaseTest {

    private val calculateModifiers = CalculateModifiersUseCase(CalculatePrestigeUseCase())

    private fun stateWith(vararg upgrades: UpgradeType): GameState =
        GameState.newGame(nowMillis = 0L).copy(upgrades = UpgradeInventory.of(upgrades.toSet()))

    @Test
    fun `liefert ohne Upgrades die Grundwerte`() {
        assertThat(calculateModifiers(GameState.newGame(0L))).isEqualTo(GameModifiers.base())
    }

    @Test
    fun `multipliziert Klickfaktoren`() {
        // Zweimal doppelt ergibt vierfach. Additiv waeren spaete Upgrades
        // wirkungslos.
        val modifiers = calculateModifiers(
            stateWith(UpgradeType.STRONGER_FINGERS, UpgradeType.IRON_FINGERS),
        )

        assertThat(modifiers.click.multiplier).isWithin(TOLERANCE).of(4.0)
    }

    @Test
    fun `addiert flache Klickzuschlaege`() {
        val modifiers = calculateModifiers(
            stateWith(UpgradeType.HEAVY_HANDS, UpgradeType.THUNDER_STRIKE),
        )

        assertThat(modifiers.click.flatBonus).isEqualTo(BigNumber.of(260))
    }

    @Test
    fun `addiert Kritzuschlaege`() {
        // Fuenf Prozent plus fuenf Prozent ergeben zehn - so liest es der
        // Spieler, und so soll es sein.
        val modifiers = calculateModifiers(
            stateWith(UpgradeType.LUCKY_CHARM, UpgradeType.FOUR_LEAF_CLOVER),
        )

        assertThat(modifiers.click.criticalChance)
            .isWithin(TOLERANCE).of(GameConfig.BASE_CRITICAL_CHANCE + 0.10)
    }

    @Test
    fun `erhoeht den Kritfaktor`() {
        val modifiers = calculateModifiers(stateWith(UpgradeType.CRITICAL_MASS))

        assertThat(modifiers.click.criticalMultiplier)
            .isWithin(TOLERANCE).of(GameConfig.BASE_CRITICAL_MULTIPLIER + 5.0)
    }

    @Test
    fun `multipliziert Einkommensfaktoren`() {
        val modifiers = calculateModifiers(
            stateWith(UpgradeType.PRODUCTION_LINE, UpgradeType.AUTOMATION),
        )

        assertThat(modifiers.incomeMultiplier).isWithin(TOLERANCE).of(4.0)
    }

    @Test
    fun `haelt Gebaeudefaktoren getrennt`() {
        val modifiers = calculateModifiers(
            stateWith(UpgradeType.NIMBLE_FINGERS, UpgradeType.DEEP_DRILLING),
        )

        assertThat(modifiers.buildingIncomeMultipliers[BuildingType.FINGER])
            .isWithin(TOLERANCE).of(3.0)
        assertThat(modifiers.buildingIncomeMultipliers[BuildingType.MINE])
            .isWithin(TOLERANCE).of(3.0)
        assertThat(modifiers.buildingIncomeMultipliers[BuildingType.FARM]).isNull()
    }

    @Test
    fun `addiert Preisnachlaesse`() {
        // Fuenf plus zehn Prozent ergeben fuenfzehn. Multiplikativ waeren es
        // 14,5 und wirkten wie ein Rechenfehler.
        val modifiers = calculateModifiers(
            stateWith(UpgradeType.BULK_DISCOUNT, UpgradeType.WHOLESALE),
        )

        assertThat(modifiers.buildingDiscount).isWithin(TOLERANCE).of(0.15)
    }

    @Test
    fun `addiert die Offline-Effizienz auf den Grundwert`() {
        val modifiers = calculateModifiers(
            stateWith(UpgradeType.NIGHT_SHIFT, UpgradeType.DREAM_FACTORY),
        )

        assertThat(modifiers.offlineEfficiency)
            .isWithin(TOLERANCE).of(GameConfig.OFFLINE_EFFICIENCY + 0.5)
    }

    @Test
    fun `deckelt die Kritchance bei hundert Prozent`() {
        // Eine Chance ueber 1.0 ist nicht darstellbar, und die Wuerfelprobe
        // wuerde sie ohnehin abweisen.
        val allCritical = UpgradeType.entries
            .filter { it.category == com.pokelike.idle.domain.model.UpgradeCategory.CRITICAL }

        val modifiers = calculateModifiers(stateWith(*allCritical.toTypedArray()))

        assertThat(modifiers.click.criticalChance).isAtMost(1.0)
    }

    @Test
    fun `rechnet den Prestige-Bonus in Klick und Einkommen ein`() {
        // Der Bonus ist in click.multiplier und incomeMultiplier bereits
        // enthalten; prestigeMultiplier dient nur der Anzeige. Wer ihn
        // zusaetzlich anwendet, rechnet ihn doppelt.
        val withPoints = GameState.newGame(0L).copy(
            resources = ResourcePool.of(ResourceType.PRESTIGE_POINTS to BigNumber.of(50)),
        )

        val modifiers = calculateModifiers(withPoints)

        assertThat(modifiers.prestigeMultiplier).isWithin(TOLERANCE).of(2.0)
        assertThat(modifiers.click.multiplier).isWithin(TOLERANCE).of(2.0)
        assertThat(modifiers.incomeMultiplier).isWithin(TOLERANCE).of(2.0)
    }

    @Test
    fun `verbindet Prestige-Bonus und Upgrade-Faktoren`() {
        val state = GameState.newGame(0L).copy(
            resources = ResourcePool.of(ResourceType.PRESTIGE_POINTS to BigNumber.of(50)),
            upgrades = UpgradeInventory.of(
                UpgradeType.STRONGER_FINGERS,
                UpgradeType.PRODUCTION_LINE,
            ),
        )

        val modifiers = calculateModifiers(state)

        // Upgrade verdoppelt, Prestige verdoppelt: vierfach.
        assertThat(modifiers.click.multiplier).isWithin(TOLERANCE).of(4.0)
        assertThat(modifiers.incomeMultiplier).isWithin(TOLERANCE).of(4.0)
    }

    @Test
    fun `wirkt sich unmittelbar auf die Klickberechnung aus`() {
        // Der eigentliche Zweck: Ein gekauftes Upgrade muss sofort im Ertrag
        // ankommen, ohne dass die Klickberechnung Upgrades kennt.
        val withoutUpgrade = calculateModifiers(GameState.newGame(0L)).click
        val withUpgrade = calculateModifiers(stateWith(UpgradeType.STRONGER_FINGERS)).click

        assertThat(withUpgrade.expectedValuePerClick())
            .isEqualTo(withoutUpgrade.expectedValuePerClick() * 2.0)
    }
}

class PurchaseUpgradeUseCaseTest {

    private val purchase = PurchaseUpgradeUseCase()

    /**
     * Spielstand, in dem jede Freischaltbedingung erfuellt ist.
     *
     * Statistik und Gebaeudebestand sind bewusst grosszuegig gesetzt, damit
     * die Tests den Kaufablauf pruefen und nicht die Freischaltung - die hat
     * ihre eigenen Tests.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun stateReadyFor(upgrade: UpgradeType, coins: Double): GameState =
        GameState.newGame(nowMillis = 0L).copy(
            resources = ResourcePool.of(ResourceType.COINS to BigNumber.of(coins)),
            // Grosszuegige Statistik und Gebaeude, damit jede
            // Freischaltbedingung erfuellt ist.
            statistics = GameStatistics(totalClicks = 1_000_000L),
            buildings = BuildingInventory.of(
                BuildingType.FINGER to 500,
                BuildingType.CURSOR to 500,
                BuildingType.MINE to 500,
                BuildingType.FARM to 500,
            ),
        )

    @Test
    fun `kauft ein freigeschaltetes Upgrade`() {
        val upgrade = UpgradeType.STRONGER_FINGERS
        val state = stateReadyFor(upgrade, coins = 1_000.0)

        val result = purchase(state, upgrade)

        assertThat(result).isInstanceOf(UpgradePurchaseResult.Success::class.java)
        val success = result as UpgradePurchaseResult.Success
        assertThat(upgrade in success.state.upgrades).isTrue()
        assertThat(success.state[ResourceType.COINS])
            .isEqualTo(BigNumber.of(1_000.0 - upgrade.price))
    }

    @Test
    fun `verweigert den Kauf bei zu wenig Muenzen`() {
        val upgrade = UpgradeType.STRONGER_FINGERS
        val state = stateReadyFor(upgrade, coins = 1.0)

        assertThat(purchase(state, upgrade)).isEqualTo(UpgradePurchaseResult.NotAffordable)
    }

    @Test
    fun `verweigert den Doppelkauf`() {
        val upgrade = UpgradeType.STRONGER_FINGERS
        val state = stateReadyFor(upgrade, coins = 1_000.0)
            .let { (purchase(it, upgrade) as UpgradePurchaseResult.Success).state }

        // Ohne diese Pruefung wuerde erneut abgebucht, waehrend die Wirkung
        // unveraendert bliebe.
        assertThat(purchase(state, upgrade)).isEqualTo(UpgradePurchaseResult.AlreadyOwned)
    }

    @Test
    fun `verweigert den Kauf eines nicht freigeschalteten Upgrades`() {
        // Die Pruefung liegt bewusst auch hier und nicht nur in der
        // Oberflaeche: Ein Fehler bei der Filterung der Liste darf keinen Kauf
        // ermoeglichen, den das Spiel nie erlauben wollte.
        val upgrade = UpgradeType.GOLDEN_TOUCH
        val state = GameState.newGame(nowMillis = 0L).copy(
            resources = ResourcePool.of(ResourceType.COINS to BigNumber.of(1.0, 30)),
        )

        assertThat(purchase(state, upgrade)).isEqualTo(UpgradePurchaseResult.NotUnlocked)
    }

    @Test
    fun `schreibt die ausgegebene Summe in die Statistik`() {
        val upgrade = UpgradeType.STRONGER_FINGERS
        val state = stateReadyFor(upgrade, coins = 1_000.0)

        val success = purchase(state, upgrade) as UpgradePurchaseResult.Success

        assertThat(success.state.statistics.lifetimeSpent[ResourceType.COINS])
            .isEqualTo(upgrade.priceAsBigNumber)
    }

    @Test
    fun `laesst den Zustand bei fehlgeschlagenem Kauf unveraendert`() {
        val upgrade = UpgradeType.STRONGER_FINGERS
        val state = stateReadyFor(upgrade, coins = 1.0)

        purchase(state, upgrade)

        assertThat(state.upgrades.isEmpty).isTrue()
        assertThat(state[ResourceType.COINS]).isEqualTo(BigNumber.of(1))
    }

    @Test
    fun `ueberlebt den Prestige-Reset`() {
        // Upgrades sind der Grund, warum ein Neuanfang schneller laeuft als
        // der vorige Durchlauf. Ohne sie waere Prestige eine reine Bestrafung.
        val upgrade = UpgradeType.STRONGER_FINGERS
        val state = stateReadyFor(upgrade, coins = 1_000.0)
            .let { (purchase(it, upgrade) as UpgradePurchaseResult.Success).state }

        val afterPrestige = state.afterPrestige(BigNumber.of(10))

        assertThat(upgrade in afterPrestige.upgrades).isTrue()
        assertThat(afterPrestige.buildings.isEmpty).isTrue()
    }
}

private const val TOLERANCE = 1e-9
