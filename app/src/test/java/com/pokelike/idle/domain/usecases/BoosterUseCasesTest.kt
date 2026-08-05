package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.ActiveBooster
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BoosterState
import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.UpgradeEffect
import com.pokelike.idle.domain.model.UpgradeInventory
import com.pokelike.idle.domain.model.UpgradeType
import org.junit.Test
import kotlin.test.assertFailsWith

private const val NOW = 1_700_000_000_000L

private fun stateWithDiamonds(amount: Long): GameState = GameState.newGame(0L).copy(
    resources = ResourcePool.of(ResourceType.DIAMONDS to BigNumber.of(amount)),
)

class BoosterStateTest {

    @Test
    fun `startet einen Booster mit seiner Grundlaufzeit`() {
        val state = BoosterState.EMPTY.withStarted(BoosterType.DOUBLE_CLICKS, NOW)

        val booster = state[BoosterType.DOUBLE_CLICKS]
        assertThat(booster?.remainingAt(NOW))
            .isEqualTo(BoosterType.DOUBLE_CLICKS.durationMillis)
    }

    @Test
    fun `verlaengert statt zu verwerfen`() {
        // Der zweite Start darf nicht verpuffen: Der Spieler hat bezahlt.
        val duration = BoosterType.DOUBLE_CLICKS.durationMillis
        val state = BoosterState.EMPTY
            .withStarted(BoosterType.DOUBLE_CLICKS, NOW)
            .withStarted(BoosterType.DOUBLE_CLICKS, NOW + 60_000L)

        assertThat(state[BoosterType.DOUBLE_CLICKS]?.remainingAt(NOW + 60_000L))
            .isEqualTo(2L * duration - 60_000L)
    }

    @Test
    fun `legt keinen zweiten Eintrag derselben Art an`() {
        // Sonst multiplizierten sich die Faktoren, und zweimal "doppelt"
        // ergaebe vierfach - eine Wirkung, die niemand verkauft hat.
        val state = BoosterState.EMPTY
            .withStarted(BoosterType.DOUBLE_INCOME, NOW)
            .withStarted(BoosterType.DOUBLE_INCOME, NOW + 1_000L)

        assertThat(state.active).hasSize(1)
    }

    @Test
    fun `deckelt die gestapelte Laufzeit`() {
        var state = BoosterState.EMPTY
        repeat(20) { state = state.withStarted(BoosterType.GOLD_RUSH, NOW) }

        assertThat(state[BoosterType.GOLD_RUSH]?.remainingAt(NOW))
            .isEqualTo(BoosterType.GOLD_RUSH.maxStackedDurationMillis)
    }

    @Test
    fun `laesst verschiedene Arten nebeneinander laufen`() {
        val state = BoosterState.EMPTY
            .withStarted(BoosterType.DOUBLE_CLICKS, NOW)
            .withStarted(BoosterType.DOUBLE_INCOME, NOW)

        assertThat(state.activeAt(NOW).map { it.type })
            .containsExactly(BoosterType.DOUBLE_CLICKS, BoosterType.DOUBLE_INCOME)
    }

    @Test
    fun `beginnt nach dem Ablauf von vorn statt zu verlaengern`() {
        val duration = BoosterType.GOLD_RUSH.durationMillis
        val expired = BoosterState.EMPTY.withStarted(BoosterType.GOLD_RUSH, NOW)

        val restarted = expired.withStarted(BoosterType.GOLD_RUSH, NOW + duration + 1L)

        assertThat(restarted[BoosterType.GOLD_RUSH]?.remainingAt(NOW + duration + 1L))
            .isEqualTo(duration)
    }

    @Test
    fun `entfernt abgelaufene Booster`() {
        val state = BoosterState.EMPTY
            .withStarted(BoosterType.GOLD_RUSH, NOW)
            .withStarted(BoosterType.LUCKY_HOUR, NOW)

        val pruned = state.pruned(NOW + BoosterType.GOLD_RUSH.durationMillis + 1L)

        assertThat(pruned.active.keys).containsExactly(BoosterType.LUCKY_HOUR)
    }

    @Test
    fun `liefert dasselbe Objekt, wenn nichts abgelaufen ist`() {
        // Der Vergleich im BoosterManager entscheidet daran, ob der Spielstand
        // als geaendert gilt. Ohne diese Zusicherung wuerde jede Sekunde ein
        // Schreibvorgang ausgeloest, obwohl sich nichts geaendert hat.
        val state = BoosterState.EMPTY.withStarted(BoosterType.LUCKY_HOUR, NOW)

        assertThat(state.pruned(NOW + 1_000L)).isSameInstanceAs(state)
    }

    @Test
    fun `verlaengert sich nicht durch Zurueckstellen der Uhr`() {
        // Der Kern der Absicherung: Sonst genuegte ein Zurueckstellen der Uhr
        // um ein Jahr, um ein Jahr lang doppeltes Einkommen zu haben.
        val state = BoosterState.EMPTY.withStarted(BoosterType.DOUBLE_INCOME, NOW)

        val remaining = state[BoosterType.DOUBLE_INCOME]
            ?.remainingAt(NOW - 365L * 24L * 60L * 60L * 1_000L)

        assertThat(remaining).isEqualTo(BoosterType.DOUBLE_INCOME.durationMillis)
    }

    @Test
    fun `weist verdrehte Zeitpunkte ab`() {
        assertFailsWith<IllegalArgumentException> {
            ActiveBooster(
                type = BoosterType.GOLD_RUSH,
                startedAtMillis = NOW,
                endsAtMillis = NOW - 1L,
            )
        }
    }
}

class BoosterCatalogTest {

    @Test
    fun `haelt die Schluessel eindeutig`() {
        assertThat(BoosterType.entries.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `gibt jedem Booster Laufzeit und Preis`() {
        BoosterType.entries.forEach { booster ->
            assertThat(booster.durationMillis).isGreaterThan(0L)
            assertThat(booster.price.isEmpty).isFalse()
        }
    }

    @Test
    fun `bepreist Booster in Diamanten`() {
        // In Muenzen bepreist waeren sie im spaeten Spiel kostenlos.
        BoosterType.entries.forEach { booster ->
            assertThat(booster.price.amounts.keys).containsExactly(ResourceType.DIAMONDS)
        }
    }

    @Test
    fun `stapelt hoechstens auf das Vielfache der Grundlaufzeit`() {
        BoosterType.entries.forEach { booster ->
            assertThat(booster.maxStackedDurationMillis)
                .isEqualTo(booster.durationMillis * GameConfig.BOOSTER_MAX_STACK_FACTOR)
        }
    }
}

class PurchaseBoosterUseCaseTest {

    private val purchase = PurchaseBoosterUseCase(StartBoosterUseCase())

    @Test
    fun `bucht ab und startet`() {
        val state = stateWithDiamonds(100L)

        val result = purchase(state, BoosterType.DOUBLE_CLICKS, NOW)

        assertThat(result).isInstanceOf(BoosterPurchaseResult.Success::class.java)
        val success = result as BoosterPurchaseResult.Success
        assertThat(success.state[ResourceType.DIAMONDS]).isEqualTo(BigNumber.of(80))
        assertThat(success.state.boosters[BoosterType.DOUBLE_CLICKS]).isNotNull()
    }

    @Test
    fun `verweigert den Kauf ohne ausreichenden Bestand`() {
        val state = stateWithDiamonds(1L)

        assertThat(purchase(state, BoosterType.GOLD_RUSH, NOW))
            .isEqualTo(BoosterPurchaseResult.NotAffordable)
    }

    @Test
    fun `laesst den Bestand bei gescheitertem Kauf unberuehrt`() {
        val state = stateWithDiamonds(1L)

        purchase(state, BoosterType.GOLD_RUSH, NOW)

        assertThat(state[ResourceType.DIAMONDS]).isEqualTo(BigNumber.of(1))
    }

    @Test
    fun `verweigert den Kauf bei ausgereizter Laufzeit`() {
        // Ohne diese Pruefung wuerde der Spieler zahlen, und die Laufzeit
        // bliebe wegen des Deckels unveraendert.
        var state = stateWithDiamonds(1_000L)
        repeat(GameConfig.BOOSTER_MAX_STACK_FACTOR) {
            state = (purchase(state, BoosterType.GOLD_RUSH, NOW) as BoosterPurchaseResult.Success)
                .state
        }

        val extra = purchase(state, BoosterType.GOLD_RUSH, NOW)

        assertThat(extra).isEqualTo(BoosterPurchaseResult.AlreadyAtMaximum)
    }

    @Test
    fun `ueberlebt den Prestige-Reset`() {
        // Einen bezahlten Booster beim Prestige zu loeschen waere eine
        // Enteignung mitten im laufenden Kauf.
        val bought = (
            purchase(stateWithDiamonds(100L), BoosterType.LUCKY_HOUR, NOW)
                as BoosterPurchaseResult.Success
            ).state

        val afterPrestige = bought.afterPrestige(BigNumber.of(5))

        assertThat(afterPrestige.boosters).isEqualTo(bought.boosters)
    }
}

class BoosterModifierTest {

    private val calculateModifiers = CalculateModifiersUseCase(CalculatePrestigeUseCase())
    private val startBooster = StartBoosterUseCase()

    @Test
    fun `wirkt auf den Klickertrag`() {
        val plain = GameState.newGame(0L)
        val boosted = startBooster(plain, BoosterType.DOUBLE_CLICKS, NOW)

        val before = calculateModifiers(plain).click.expectedValuePerClick()
        val after = calculateModifiers(boosted).click.expectedValuePerClick()

        assertThat(after).isEqualTo(before * 2.0)
    }

    @Test
    fun `wirkt auf das Einkommen`() {
        val boosted = startBooster(GameState.newGame(0L), BoosterType.DOUBLE_INCOME, NOW)

        assertThat(calculateModifiers(boosted).incomeMultiplier).isEqualTo(2.0)
    }

    @Test
    fun `stapelt mit Upgrades multiplikativ`() {
        // Booster und Upgrades laufen durch dieselbe Auswertung. Waeren es zwei
        // Kopien, liefen die Verrechnungsregeln frueher oder spaeter
        // auseinander.
        val withUpgrade = GameState.newGame(0L).copy(
            upgrades = UpgradeInventory.of(UpgradeType.PRODUCTION_LINE),
        )
        val boosted = startBooster(withUpgrade, BoosterType.DOUBLE_INCOME, NOW)

        assertThat(calculateModifiers(boosted).incomeMultiplier)
            .isEqualTo(calculateModifiers(withUpgrade).incomeMultiplier * 2.0)
    }

    @Test
    fun `bleibt beim Offline-Ertrag aussen vor`() {
        // Wuerden Booster auch bei geschlossener App zahlen, waere das
        // Schliessen der App die beste Art, einen Booster zu nutzen.
        val boosted = startBooster(GameState.newGame(0L), BoosterType.DOUBLE_INCOME, NOW)

        assertThat(calculateModifiers(boosted, includeBoosters = false).incomeMultiplier)
            .isEqualTo(1.0)
    }

    @Test
    fun `laesst Upgrades beim Offline-Ertrag unberuehrt`() {
        // Die Ausnahme gilt ausschliesslich Boostern. Ein Offline-Upgrade
        // auszulassen waere ein Fehler, denn genau dafuer wurde es gekauft.
        val withUpgrade = GameState.newGame(0L).copy(
            upgrades = UpgradeInventory.of(UpgradeType.NIGHT_SHIFT),
        )

        assertThat(calculateModifiers(withUpgrade, includeBoosters = false).offlineEfficiency)
            .isEqualTo(calculateModifiers(withUpgrade).offlineEfficiency)
    }

    @Test
    fun `wirkt auf das tatsaechliche Einkommen`() {
        // Die Probe aufs Exempel: nicht nur der Faktor, sondern der Betrag.
        val calculateIncome = CalculateIncomeUseCase()
        val buildings = BuildingInventory.of(BuildingType.CURSOR to 10)
        val state = GameState.newGame(0L).copy(buildings = buildings)
        val boosted = startBooster(state, BoosterType.DOUBLE_INCOME, NOW)

        val plainIncome = calculateIncome(buildings, calculateModifiers(state).incomeMultiplier)
        val boostedIncome =
            calculateIncome(buildings, calculateModifiers(boosted).incomeMultiplier)

        assertThat(boostedIncome).isEqualTo(plainIncome * 2.0)
    }

    @Test
    fun `deckt jede Wirkungsart des Katalogs ab`() {
        // Ein Booster mit einer Wirkungsart, die es in UpgradeEffect nicht
        // gibt, waere ein Kauf ohne Wirkung.
        val known = setOf(
            UpgradeEffect.ClickMultiplier::class,
            UpgradeEffect.IncomeMultiplier::class,
            UpgradeEffect.CriticalChanceBonus::class,
        )

        assertThat(known).containsAtLeastElementsIn(
            BoosterType.entries.map { it.effect::class }.toSet(),
        )
    }
}
