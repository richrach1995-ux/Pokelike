package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.DailyRewardType
import com.pokelike.idle.domain.model.GameModifiers
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.LoginState
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
import org.junit.Test
import java.time.ZoneId

/**
 * Gemeinsame Grundlage der Tests rund um den Tagesbonus.
 *
 * Der Zeitpunkt ist fest gewaehlt und die Zone ausdruecklich UTC, damit die
 * Ergebnisse nicht davon abhaengen, wo der Test laeuft.
 */
private val zone: ZoneId = ZoneId.of("UTC")

/** 1. Januar 2024, 12:00 UTC. */
private const val DAY1_NOON = 1_704_110_400_000L
private const val ONE_DAY = 24L * 60L * 60L * 1_000L

private val baseModifiers = GameModifiers.base()

class EvaluateDailyRewardUseCaseTest {

    private val evaluate = EvaluateDailyRewardUseCase(CalculateIncomeUseCase())

    private fun evaluateAt(state: GameState, nowMillis: Long) =
        evaluate(state, baseModifiers, nowMillis, zone)

    // --- Anspruch ---------------------------------------------------------

    @Test
    fun `bietet dem neuen Spieler den ersten Tag an`() {
        val status = evaluateAt(GameState.newGame(0L), DAY1_NOON)

        assertThat(status).isNotNull()
        assertThat(status?.day).isEqualTo(DailyRewardType.DAY_1)
        assertThat(status?.streakAfterClaim).isEqualTo(1)
    }

    @Test
    fun `bietet am selben Tag nichts ein zweites Mal an`() {
        val claimed = GameState.newGame(0L).copy(
            login = LoginState(streak = 1, longestStreak = 1, lastClaimedAtMillis = DAY1_NOON),
        )

        // Acht Stunden spaeter, immer noch derselbe Kalendertag.
        val later = evaluateAt(claimed, DAY1_NOON + 8L * 60L * 60L * 1_000L)

        assertThat(later).isNull()
    }

    @Test
    fun `bietet am naechsten Kalendertag wieder an`() {
        val claimed = GameState.newGame(0L).copy(
            login = LoginState(streak = 1, longestStreak = 1, lastClaimedAtMillis = DAY1_NOON),
        )

        val nextDay = evaluateAt(claimed, DAY1_NOON + ONE_DAY)

        assertThat(nextDay?.streakAfterClaim).isEqualTo(2)
        assertThat(nextDay?.day).isEqualTo(DailyRewardType.DAY_2)
    }

    @Test
    fun `bietet bei zurueckgestellter Uhr nichts an`() {
        // Ein Anspruch waere hier ein Geschenk fuer den Versuch, das System
        // auszutricksen: Zuruecksetzen der Uhr, zweimal abholen.
        val claimed = GameState.newGame(0L).copy(
            login = LoginState(streak = 3, longestStreak = 3, lastClaimedAtMillis = DAY1_NOON),
        )

        assertThat(evaluateAt(claimed, DAY1_NOON - 5L * ONE_DAY)).isNull()
    }

    // --- Serie ------------------------------------------------------------

    @Test
    fun `laesst die Serie nach einem ausgelassenen Tag neu beginnen`() {
        val claimed = GameState.newGame(0L).copy(
            login = LoginState(streak = 6, longestStreak = 6, lastClaimedAtMillis = DAY1_NOON),
        )

        val status = evaluateAt(claimed, DAY1_NOON + 2L * ONE_DAY)

        assertThat(status?.streakAfterClaim).isEqualTo(1)
        assertThat(status?.streakBroken).isTrue()
        assertThat(status?.day).isEqualTo(DailyRewardType.DAY_1)
    }

    @Test
    fun `beginnt den Zyklus nach dem letzten Tag von vorn`() {
        // Die Serie laeuft weiter, der Zyklus nicht. Wuerde die Serie mit dem
        // Zyklus enden, verloere der Bestwert seine Aussage.
        val claimed = GameState.newGame(0L).copy(
            login = LoginState(streak = 7, longestStreak = 7, lastClaimedAtMillis = DAY1_NOON),
        )

        val status = evaluateAt(claimed, DAY1_NOON + ONE_DAY)

        assertThat(status?.streakAfterClaim).isEqualTo(8)
        assertThat(status?.day).isEqualTo(DailyRewardType.DAY_1)
    }

    @Test
    fun `ueberbrueckt einen ausgelassenen Tag mit Serienschutz`() {
        val claimed = GameState.newGame(0L).copy(
            login = LoginState(
                streak = 6,
                longestStreak = 6,
                lastClaimedAtMillis = DAY1_NOON,
                protectionCharges = 1,
            ),
        )

        val status = evaluateAt(claimed, DAY1_NOON + 2L * ONE_DAY)

        assertThat(status?.streakAfterClaim).isEqualTo(7)
        assertThat(status?.streakBroken).isFalse()
        assertThat(status?.protectionUsed).isEqualTo(1)
    }

    @Test
    fun `verbraucht je ausgelassenem Tag eine Ladung`() {
        val claimed = GameState.newGame(0L).copy(
            login = LoginState(
                streak = 4,
                longestStreak = 4,
                lastClaimedAtMillis = DAY1_NOON,
                protectionCharges = 3,
            ),
        )

        val status = evaluateAt(claimed, DAY1_NOON + 3L * ONE_DAY)

        assertThat(status?.protectionUsed).isEqualTo(2)
        assertThat(status?.streakAfterClaim).isEqualTo(5)
    }

    @Test
    fun `verbraucht nichts, wenn der Schutz die Luecke nicht deckt`() {
        // Eine halb ueberbrueckte Luecke haette dem Spieler nichts gebracht.
        // Die Ladungen dennoch einzuziehen waere die schlechteste Antwort:
        // Serie weg und Vorrat weg.
        val claimed = GameState.newGame(0L).copy(
            login = LoginState(
                streak = 9,
                longestStreak = 9,
                lastClaimedAtMillis = DAY1_NOON,
                protectionCharges = 1,
            ),
        )

        val status = evaluateAt(claimed, DAY1_NOON + 4L * ONE_DAY)

        assertThat(status?.streakBroken).isTrue()
        assertThat(status?.protectionUsed).isEqualTo(0)
    }

    // --- Belohnung --------------------------------------------------------

    @Test
    fun `bemisst die Muenzen am Einkommen des Spielers`() {
        // Zehn Cursor. Der genaue Betrag ergibt sich aus der Einkommensformel;
        // geprueft wird, dass der Bonus daran haengt und nicht an einer festen
        // Zahl - ein fester Betrag waere nach wenigen Stunden bedeutungslos.
        val buildings = BuildingInventory.of(BuildingType.CURSOR to 10)
        val state = GameState.newGame(0L).copy(buildings = buildings)
        val perSecond = CalculateIncomeUseCase()(buildings)

        val status = evaluateAt(state, DAY1_NOON)

        val expected = perSecond * BigNumber.of(DailyRewardType.DAY_1.idleMinutes * 60L)
        assertThat(status?.reward?.get(ResourceType.COINS)).isEqualTo(expected)
    }

    @Test
    fun `gibt auch ohne Gebaeude Muenzen`() {
        // Der Spieler ohne Gebaeude ist genau der, dem der Bonus am meisten
        // hilft. Null Muenzen waeren hier das falsche Signal.
        val status = evaluateAt(GameState.newGame(0L), DAY1_NOON)

        assertThat(status?.reward?.get(ResourceType.COINS)?.isPositive).isTrue()
    }

    @Test
    fun `waechst mit dem Einkommen`() {
        val small = GameState.newGame(0L).copy(
            buildings = BuildingInventory.of(BuildingType.CURSOR to 5),
        )
        val large = GameState.newGame(0L).copy(
            buildings = BuildingInventory.of(BuildingType.CURSOR to 500),
        )

        val smallReward = evaluateAt(small, DAY1_NOON)?.reward?.get(ResourceType.COINS)
        val largeReward = evaluateAt(large, DAY1_NOON)?.reward?.get(ResourceType.COINS)

        assertThat(largeReward).isGreaterThan(smallReward)
    }

    @Test
    fun `reicht den festen Zuschlag durch`() {
        val claimed = GameState.newGame(0L).copy(
            login = LoginState(streak = 1, longestStreak = 1, lastClaimedAtMillis = DAY1_NOON),
        )

        val status = evaluateAt(claimed, DAY1_NOON + ONE_DAY)

        assertThat(status?.reward?.get(ResourceType.DIAMONDS))
            .isEqualTo(DailyRewardType.DAY_2.bonus[ResourceType.DIAMONDS])
    }
}

class ClaimDailyRewardUseCaseTest {

    private val evaluate = EvaluateDailyRewardUseCase(CalculateIncomeUseCase())
    private val claim = ClaimDailyRewardUseCase(evaluate)

    private fun claimAt(state: GameState, nowMillis: Long) =
        claim(state, baseModifiers, nowMillis, zone)

    @Test
    fun `schreibt die Belohnung gut und vermerkt die Abholung`() {
        val state = GameState.newGame(0L)
        val before = state[ResourceType.COINS]

        val result = claimAt(state, DAY1_NOON)

        assertThat(result).isInstanceOf(DailyRewardClaimResult.Success::class.java)
        val success = result as DailyRewardClaimResult.Success
        assertThat(success.state[ResourceType.COINS]).isGreaterThan(before)
        assertThat(success.state.login.streak).isEqualTo(1)
        assertThat(success.state.login.lastClaimedAtMillis).isEqualTo(DAY1_NOON)
    }

    @Test
    fun `verweigert die zweite Abholung am selben Tag`() {
        val first = claimAt(GameState.newGame(0L), DAY1_NOON) as DailyRewardClaimResult.Success

        val second = claimAt(first.state, DAY1_NOON + 60_000L)

        assertThat(second).isEqualTo(DailyRewardClaimResult.NotDue)
    }

    @Test
    fun `zieht den verbrauchten Serienschutz ab`() {
        val state = GameState.newGame(0L).copy(
            login = LoginState(
                streak = 2,
                longestStreak = 2,
                lastClaimedAtMillis = DAY1_NOON,
                protectionCharges = 2,
            ),
        )

        val result = claimAt(state, DAY1_NOON + 2L * ONE_DAY) as DailyRewardClaimResult.Success

        assertThat(result.state.login.protectionCharges).isEqualTo(1)
        assertThat(result.state.login.streak).isEqualTo(3)
    }

    @Test
    fun `schreibt den Bestwert fort`() {
        val state = GameState.newGame(0L).copy(
            login = LoginState(streak = 4, longestStreak = 4, lastClaimedAtMillis = DAY1_NOON),
        )

        val result = claimAt(state, DAY1_NOON + ONE_DAY) as DailyRewardClaimResult.Success

        assertThat(result.state.login.longestStreak).isEqualTo(5)
    }

    @Test
    fun `laesst den Bestwert bei gerissener Serie stehen`() {
        // Der Bestwert traegt die Achievements. Wuerde er mitfallen, verloere
        // der Spieler ein bereits erreichtes Achievement wieder.
        val state = GameState.newGame(0L).copy(
            login = LoginState(streak = 9, longestStreak = 9, lastClaimedAtMillis = DAY1_NOON),
        )

        val result = claimAt(state, DAY1_NOON + 5L * ONE_DAY) as DailyRewardClaimResult.Success

        assertThat(result.state.login.streak).isEqualTo(1)
        assertThat(result.state.login.longestStreak).isEqualTo(9)
    }

    @Test
    fun `ueberlebt den Prestige-Reset`() {
        val claimed = (
            claimAt(GameState.newGame(0L), DAY1_NOON) as DailyRewardClaimResult.Success
            ).state

        val afterPrestige = claimed.afterPrestige(BigNumber.of(5))

        assertThat(afterPrestige.login).isEqualTo(claimed.login)
    }
}

class PurchaseStreakProtectionUseCaseTest {

    private val purchase = PurchaseStreakProtectionUseCase()

    private fun withDiamonds(amount: Long, charges: Int = 0): GameState =
        GameState.newGame(0L).copy(
            resources = ResourcePool.of(ResourceType.DIAMONDS to BigNumber.of(amount)),
            login = LoginState(protectionCharges = charges),
        )

    @Test
    fun `legt eine Ladung in den Vorrat und bucht ab`() {
        val state = withDiamonds(GameConfig.STREAK_PROTECTION_PRICE_DIAMONDS)

        val result = purchase(state)

        assertThat(result).isInstanceOf(StreakProtectionPurchaseResult.Success::class.java)
        val success = result as StreakProtectionPurchaseResult.Success
        assertThat(success.state.login.protectionCharges).isEqualTo(1)
        assertThat(success.state[ResourceType.DIAMONDS]).isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `verweigert den Kauf ohne ausreichenden Bestand`() {
        val state = withDiamonds(GameConfig.STREAK_PROTECTION_PRICE_DIAMONDS - 1L)

        assertThat(purchase(state)).isEqualTo(StreakProtectionPurchaseResult.NotAffordable)
    }

    @Test
    fun `laesst den Bestand bei gescheitertem Kauf unberuehrt`() {
        val state = withDiamonds(1L)

        purchase(state)

        assertThat(state[ResourceType.DIAMONDS]).isEqualTo(BigNumber.of(1))
    }

    @Test
    fun `verweigert den Kauf bei vollem Vorrat`() {
        // Getrennt von der fehlenden Deckung: Der Spieler soll erfahren, dass
        // mehr Diamanten hier nichts aendern wuerden.
        val state = withDiamonds(
            amount = 10_000L,
            charges = GameConfig.STREAK_PROTECTION_MAX_CHARGES,
        )

        assertThat(purchase(state)).isEqualTo(StreakProtectionPurchaseResult.AtMaximum)
    }

    @Test
    fun `verlangt Diamanten und keine Muenzen`() {
        // Der erste Ausgabezweck der Premiumwaehrung. Waere er in Muenzen
        // bepreist, waere er im spaeten Spiel kostenlos.
        assertThat(purchase.price.amounts.keys).containsExactly(ResourceType.DIAMONDS)
    }
}

class DailyRewardCatalogTest {

    @Test
    fun `deckt den Zyklus lueckenlos ab`() {
        assertThat(DailyRewardType.entries.map { it.day })
            .containsExactlyElementsIn(1..DailyRewardType.cycleLength)
            .inOrder()
    }

    @Test
    fun `gibt an jedem Tag etwas`() {
        DailyRewardType.entries.forEach { day ->
            assertThat(day.idleMinutes).isGreaterThan(0)
        }
    }

    @Test
    fun `steigert den Muenzanteil bis zum letzten Tag`() {
        // Der Zugeffekt des Zyklus haengt daran: Wer am fuenften Tag ueberlegt
        // aufzuhoeren, muss ein groesseres Ziel vor sich sehen.
        val minutes = DailyRewardType.entries.map { it.idleMinutes }

        assertThat(minutes).isInOrder()
        assertThat(minutes.last()).isGreaterThan(minutes.first())
    }

    @Test
    fun `bezahlt mit einem vollen Zyklus genau eine Ladung Serienschutz`() {
        // Das gewaehlte Gleichgewicht: Eine lueckenlose Woche traegt genau
        // einen Serienschutz. Aendert sich einer der beiden Werte, faellt es
        // hier auf.
        val diamondsPerCycle = DailyRewardType.entries.fold(BigNumber.ZERO) { sum, day ->
            sum + day.bonus[ResourceType.DIAMONDS]
        }

        assertThat(diamondsPerCycle)
            .isEqualTo(BigNumber.of(GameConfig.STREAK_PROTECTION_PRICE_DIAMONDS))
    }

    @Test
    fun `ordnet jeder Serienlaenge einen Tag zu`() {
        assertThat(DailyRewardType.forStreak(1)).isEqualTo(DailyRewardType.DAY_1)
        assertThat(DailyRewardType.forStreak(7)).isEqualTo(DailyRewardType.DAY_7)
        assertThat(DailyRewardType.forStreak(8)).isEqualTo(DailyRewardType.DAY_1)
        assertThat(DailyRewardType.forStreak(15)).isEqualTo(DailyRewardType.DAY_1)
        // Serie null tritt auf, solange nie abgeholt wurde.
        assertThat(DailyRewardType.forStreak(0)).isEqualTo(DailyRewardType.DAY_1)
    }
}
