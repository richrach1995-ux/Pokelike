package com.pokelike.idle.manager

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BoosterState
import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.usecases.CalculateIncomeUseCase
import com.pokelike.idle.domain.usecases.CalculateModifiersUseCase
import com.pokelike.idle.domain.usecases.CalculateOfflineProgressUseCase
import com.pokelike.idle.domain.usecases.CalculatePrestigeUseCase
import com.pokelike.idle.domain.usecases.CheckAchievementsUseCase
import com.pokelike.idle.domain.usecases.ClaimDailyRewardUseCase
import com.pokelike.idle.domain.usecases.EvaluateDailyRewardUseCase
import com.pokelike.idle.domain.usecases.PerformClickUseCase
import com.pokelike.idle.domain.usecases.PurchaseBoosterUseCase
import com.pokelike.idle.domain.usecases.RolloverQuestsUseCase
import com.pokelike.idle.domain.usecases.StartBoosterUseCase
import com.pokelike.idle.testing.FakeGameRepository
import com.pokelike.idle.testing.modifierManagerFor
import com.pokelike.idle.testing.FakeRandomProvider
import com.pokelike.idle.testing.TestDispatcherProvider
import com.pokelike.idle.testing.VirtualTimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Hinweis zur Testtechnik: Hier wird bewusst mit `runCurrent` und
 * `advanceTimeBy` gearbeitet und nicht mit `advanceUntilIdle`. Seit
 * kotlinx-coroutines 1.7 zaehlt Arbeit im `backgroundScope` nicht als
 * ausstehend - `advanceUntilIdle` kehrt sofort zurueck, ohne sie ausgefuehrt zu
 * haben, und die Zusicherungen liefen ins Leere. Ohne den `backgroundScope`
 * wiederum wuerde die endlose Tick-Schleife der Uhr den Test nie beenden.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameSessionManagerTest {

    private class Fixture(
        val session: GameSessionManager,
        val repository: FakeGameRepository,
        val clock: GameClock,
        val dailyReward: DailyRewardManager,
        val boosters: BoosterManager,
    )

    private fun TestScope.createFixture(
        scope: CoroutineScope,
        repository: FakeGameRepository = FakeGameRepository(),
    ): Fixture {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        // Versatz, damit die Systemzeit einen realistischen Wert hat: Ein
        // zurueckliegender Speicherzeitpunkt muesste sonst negativ sein.
        val timeSource = VirtualTimeSource(testScheduler, offsetMillis = WALL_CLOCK_BASE)
        val clock = GameClock(scope, dispatchers, timeSource)
        val autosave = AutosaveManager(scope, dispatchers, clock, repository)
        val modifierManager = modifierManagerFor(scope, repository)
        val clickManager = ClickManager(
            scope = scope,
            dispatchers = dispatchers,
            timeSource = timeSource,
            gameClock = clock,
            repository = repository,
            modifierManager = modifierManager,
            performClick = PerformClickUseCase(FakeRandomProvider.neverHitting()),
        )
        val incomeManager = IdleIncomeManager(
            scope = scope,
            dispatchers = dispatchers,
            gameClock = clock,
            repository = repository,
            modifierManager = modifierManager,
            calculateIncome = CalculateIncomeUseCase(),
        )
        val achievementManager = AchievementManager(
            scope = scope,
            dispatchers = dispatchers,
            gameClock = clock,
            repository = repository,
            rewardManager = RewardManager(),
            checkAchievements = CheckAchievementsUseCase(),
        )
        val boosterManager = BoosterManager(
            scope = scope,
            dispatchers = dispatchers,
            gameClock = clock,
            repository = repository,
            timeSource = timeSource,
            purchaseBooster = PurchaseBoosterUseCase(StartBoosterUseCase()),
        )
        val evaluateDailyReward = EvaluateDailyRewardUseCase(CalculateIncomeUseCase())
        val dailyRewardManager = DailyRewardManager(
            repository = repository,
            calculateModifiers = CalculateModifiersUseCase(CalculatePrestigeUseCase()),
            timeSource = timeSource,
            evaluate = evaluateDailyReward,
            claimDailyReward = ClaimDailyRewardUseCase(evaluateDailyReward),
        )

        return Fixture(
            session = GameSessionManager(
                scope = scope,
                dispatchers = dispatchers,
                timeSource = timeSource,
                repository = repository,
                gameClock = clock,
                autosaveManager = autosave,
                clickManager = clickManager,
                idleIncomeManager = incomeManager,
                calculateModifiers = CalculateModifiersUseCase(CalculatePrestigeUseCase()),
                calculateIncome = CalculateIncomeUseCase(),
                calculateOfflineProgress = CalculateOfflineProgressUseCase(),
                achievementManager = achievementManager,
                boosterManager = boosterManager,
                dailyRewardManager = dailyRewardManager,
                rolloverQuests = RolloverQuestsUseCase(),
            ),
            repository = repository,
            clock = clock,
            dailyReward = dailyRewardManager,
            boosters = boosterManager,
        )
    }

    @Test
    fun `laedt den Spielstand und startet die Uhr`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.session.onEnterForeground()
        runCurrent()

        assertThat(fixture.repository.loadCount).isEqualTo(1)
        assertThat(fixture.repository.isLoaded.value).isTrue()
        assertThat(fixture.clock.isRunning.value).isTrue()
    }

    @Test
    fun `laedt bei erneutem Wechsel in den Vordergrund nicht noch einmal`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.session.onEnterForeground()
        runCurrent()
        fixture.session.onEnterBackground()
        runCurrent()
        fixture.session.onEnterForeground()
        runCurrent()

        // Ein zweites Laden wuerde den Fortschritt der laufenden Sitzung
        // verwerfen und auf den zuletzt gespeicherten Stand zuruecksetzen.
        assertThat(fixture.repository.loadCount).isEqualTo(1)
        assertThat(fixture.clock.isRunning.value).isTrue()
    }

    @Test
    fun `haelt die Uhr an und speichert beim Wechsel in den Hintergrund`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.session.onEnterForeground()
        runCurrent()

        fixture.session.onEnterBackground()
        runCurrent()

        assertThat(fixture.clock.isRunning.value).isFalse()
        assertThat(fixture.repository.saveCount).isAtLeast(1)
    }

    @Test
    fun `schreibt den Zeitstempel fuer die Offline-Berechnung fort`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.session.onEnterForeground()
        runCurrent()
        val beforeBackground = fixture.repository.gameState.value.lastSeenAtMillis

        // Spielzeit vergehen lassen, damit sich der Zeitstempel messbar
        // veraendert. Die Zeitquelle haengt an der virtuellen Uhr des Tests.
        advanceTimeBy(60_000L)
        runCurrent()

        fixture.session.onEnterBackground()
        runCurrent()

        // Ohne diesen Zeitstempel gaebe es beim naechsten Start keinen
        // Bezugspunkt fuer den Offline-Fortschritt.
        assertThat(fixture.repository.gameState.value.lastSeenAtMillis)
            .isGreaterThan(beforeBackground)
    }

    // --- Offline-Fortschritt ---------------------------------------------

    @Test
    fun `schreibt den Offline-Ertrag beim Laden gut`() = runTest {
        // Ein gespeicherter Stand mit zehn Cursorn und einer Stunde
        // Abwesenheit. Zehn Muenzen pro Sekunde, halbe Offline-Effizienz.
        val saved = GameState.newGame(nowMillis = 0L).copy(
            buildings = BuildingInventory.of(BuildingType.CURSOR to 10),
            lastSeenAtMillis = WALL_CLOCK_BASE - ONE_HOUR,
        )
        val fixture = createFixture(
            backgroundScope,
            FakeGameRepository(stateToLoad = saved),
        )

        fixture.session.onEnterForeground()
        runCurrent()

        // 3600 Sekunden * 10 * 0.5 = 18.000
        assertThat(fixture.repository.gameState.value[ResourceType.COINS].toDouble())
            .isWithin(TOLERANCE).of(18_000.0)
    }

    @Test
    fun `bietet den Offline-Ertrag zur Anzeige an`() = runTest {
        val saved = GameState.newGame(nowMillis = 0L).copy(
            buildings = BuildingInventory.of(BuildingType.CURSOR to 10),
            lastSeenAtMillis = WALL_CLOCK_BASE - ONE_HOUR,
        )
        val fixture = createFixture(
            backgroundScope,
            FakeGameRepository(stateToLoad = saved),
        )

        fixture.session.onEnterForeground()
        runCurrent()

        val progress = fixture.session.offlineProgress.value
        assertThat(progress).isNotNull()
        assertThat(progress!!.isWorthShowing).isTrue()

        // Nach dem Bestaetigen darf der Dialog nicht erneut erscheinen.
        fixture.session.consumeOfflineProgress()
        assertThat(fixture.session.offlineProgress.value).isNull()
    }

    @Test
    fun `zeigt nichts an, wenn offline nichts angefallen ist`() = runTest {
        // Ohne Gebaeude gibt es keinen Ertrag, und ein Dialog darueber waere
        // reine Stoerung.
        val saved = GameState.newGame(nowMillis = 0L).copy(
            lastSeenAtMillis = WALL_CLOCK_BASE - ONE_HOUR,
        )
        val fixture = createFixture(
            backgroundScope,
            FakeGameRepository(stateToLoad = saved),
        )

        fixture.session.onEnterForeground()
        runCurrent()

        assertThat(fixture.session.offlineProgress.value).isNull()
    }

    @Test
    fun `verweigert den Offline-Ertrag bei vorgestellter Uhr`() = runTest {
        // Der gespeicherte Zeitpunkt liegt in der Zukunft: Die Uhr wurde
        // vorgestellt und danach zurueckgesetzt.
        val saved = GameState.newGame(nowMillis = 0L).copy(
            buildings = BuildingInventory.of(BuildingType.CURSOR to 10),
            lastSeenAtMillis = WALL_CLOCK_BASE + ONE_HOUR,
        )
        val fixture = createFixture(
            backgroundScope,
            FakeGameRepository(stateToLoad = saved),
        )

        fixture.session.onEnterForeground()
        runCurrent()

        assertThat(fixture.repository.gameState.value[ResourceType.COINS])
            .isEqualTo(BigNumber.ZERO)
        assertThat(fixture.session.offlineProgress.value).isNull()
    }

    // --- Booster ----------------------------------------------------------

    @Test
    fun `raeumt abgelaufene Booster beim Wechsel in den Vordergrund weg`() = runTest {
        val expiredAt = WALL_CLOCK_BASE - BoosterType.GOLD_RUSH.durationMillis - 1_000L
        val saved = GameState.newGame(nowMillis = 0L).copy(
            boosters = BoosterState.EMPTY.withStarted(BoosterType.GOLD_RUSH, expiredAt),
            lastSeenAtMillis = WALL_CLOCK_BASE,
        )
        val fixture = createFixture(
            backgroundScope,
            FakeGameRepository(stateToLoad = saved),
        )

        fixture.session.onEnterForeground()
        runCurrent()

        assertThat(fixture.repository.gameState.value.boosters.isEmpty).isTrue()
        assertThat(fixture.boosters.active.value).isEmpty()
    }

    @Test
    fun `behaelt einen noch laufenden Booster`() = runTest {
        val saved = GameState.newGame(nowMillis = 0L).copy(
            boosters = BoosterState.EMPTY.withStarted(
                BoosterType.LUCKY_HOUR,
                WALL_CLOCK_BASE - 60_000L,
            ),
            lastSeenAtMillis = WALL_CLOCK_BASE,
        )
        val fixture = createFixture(
            backgroundScope,
            FakeGameRepository(stateToLoad = saved),
        )

        fixture.session.onEnterForeground()
        runCurrent()

        assertThat(fixture.boosters.active.value.map { it.type })
            .containsExactly(BoosterType.LUCKY_HOUR)
    }

    @Test
    fun `laesst Booster den Offline-Ertrag unberuehrt`() = runTest {
        // Wuerden Booster auch bei geschlossener App zahlen, waere das
        // Schliessen der App die beste Art, einen Booster zu nutzen.
        val buildings = BuildingInventory.of(BuildingType.CURSOR to 10)
        val saved = GameState.newGame(nowMillis = 0L).copy(
            buildings = buildings,
            // Bewusst noch laufend: Ein bereits abgelaufener Booster wuerde
            // schon durch das Aufraeumen verschwinden, und der Test pruefte
            // dann etwas anderes als das, was er soll.
            boosters = BoosterState.EMPTY.withStarted(
                BoosterType.DOUBLE_INCOME,
                WALL_CLOCK_BASE - 10L * 60L * 1_000L,
            ),
            lastSeenAtMillis = WALL_CLOCK_BASE - ONE_HOUR,
        )
        val fixture = createFixture(
            backgroundScope,
            FakeGameRepository(stateToLoad = saved),
        )

        fixture.session.onEnterForeground()
        runCurrent()

        // Derselbe Betrag wie ohne Booster: 3600 Sekunden * 10 * 0.5 = 18.000
        assertThat(fixture.repository.gameState.value[ResourceType.COINS].toDouble())
            .isWithin(TOLERANCE).of(18_000.0)
    }

    // --- Taeglicher Bonus ------------------------------------------------

    @Test
    fun `bietet den Tagesbonus beim Wechsel in den Vordergrund an`() = runTest {
        val fixture = createFixture(backgroundScope)

        fixture.session.onEnterForeground()
        runCurrent()

        assertThat(fixture.dailyReward.pending.value).isNotNull()
    }

    @Test
    fun `bemisst den Tagesbonus am geladenen Gebaeudebestand`() = runTest {
        // Der Grund fuer diesen Test: Der Bonus haengt am Einkommen, und das
        // steht erst nach dem Laden fest. Wuerde er vor dem Laden bemessen,
        // bekaeme jeder Spieler den Betrag eines Anfaengers.
        val saved = GameState.newGame(nowMillis = 0L).copy(
            buildings = BuildingInventory.of(BuildingType.CURSOR to 100),
            lastSeenAtMillis = WALL_CLOCK_BASE,
        )
        val fixture = createFixture(
            backgroundScope,
            FakeGameRepository(stateToLoad = saved),
        )
        val withoutBuildings = createFixture(backgroundScope).also { plain ->
            plain.session.onEnterForeground()
        }
        runCurrent()

        fixture.session.onEnterForeground()
        runCurrent()

        val withBuildingsReward = fixture.dailyReward.pending.value
            ?.reward?.get(ResourceType.COINS)
        val plainReward = withoutBuildings.dailyReward.pending.value
            ?.reward?.get(ResourceType.COINS)

        assertThat(withBuildingsReward).isGreaterThan(plainReward)
    }

    @Test
    fun `speichert nicht, solange nichts geladen wurde`() = runTest {
        val fixture = createFixture(backgroundScope)

        // Kann eintreten, wenn der Prozess sofort wieder in den Hintergrund
        // geht. Ein Schreibvorgang wuerde hier den leeren Ausgangszustand
        // ueber den gespeicherten Spielstand legen.
        fixture.session.onEnterBackground()
        runCurrent()

        assertThat(fixture.repository.saveCount).isEqualTo(0)
    }

    private companion object {
        const val WALL_CLOCK_BASE = 1_700_000_000_000L
        const val ONE_HOUR = 3_600_000L
        const val TOLERANCE = 1e-6
    }
}
