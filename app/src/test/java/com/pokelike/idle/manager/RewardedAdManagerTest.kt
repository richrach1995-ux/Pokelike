package com.pokelike.idle.manager

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.ads.AdResult
import com.pokelike.idle.domain.model.RewardedAdPlacement
import com.pokelike.idle.domain.usecases.GrantAdRewardUseCase
import com.pokelike.idle.domain.usecases.StartBoosterUseCase
import com.pokelike.idle.testing.FakeAdSource
import com.pokelike.idle.testing.FakeGameRepository
import com.pokelike.idle.testing.RecordingGameLogger
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
 * Wie beim [GameSessionManagerTest] wird bewusst mit `runCurrent` und
 * `advanceTimeBy` gearbeitet: Arbeit im `backgroundScope` zaehlt seit
 * kotlinx-coroutines 1.7 nicht als ausstehend, und `advanceUntilIdle` kehrte
 * zurueck, ohne sie ausgefuehrt zu haben.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RewardedAdManagerTest {

    private val placement = RewardedAdPlacement.BOOSTER_REWARD

    private class Fixture(
        val manager: RewardedAdManager,
        val repository: FakeGameRepository,
        val adSource: FakeAdSource,
        val clock: GameClock,
        val logger: RecordingGameLogger,
    )

    private fun TestScope.createFixture(
        scope: CoroutineScope,
        adSource: FakeAdSource = FakeAdSource(),
    ): Fixture {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        val timeSource = VirtualTimeSource(testScheduler, offsetMillis = WALL_CLOCK_BASE)
        val repository = FakeGameRepository()
        val clock = GameClock(scope, dispatchers, timeSource)
        val logger = RecordingGameLogger()

        return Fixture(
            manager = RewardedAdManager(
                scope = scope,
                dispatchers = dispatchers,
                gameClock = clock,
                repository = repository,
                timeSource = timeSource,
                adSource = adSource,
                grantAdReward = GrantAdRewardUseCase(StartBoosterUseCase()),
                logger = logger,
            ),
            repository = repository,
            adSource = adSource,
            clock = clock,
            logger = logger,
        )
    }

    // --- Laden ------------------------------------------------------------

    @Test
    fun `laedt beim Start ein Video vor`() = runTest {
        // Erst beim Antippen zu laden hiesse: Der Spieler tippt und sieht
        // sekundenlang nichts.
        val fixture = createFixture(backgroundScope)

        fixture.manager.start()
        runCurrent()

        assertThat(fixture.adSource.prepareCount).isEqualTo(1)
        assertThat(fixture.manager.status.value.isReady).isTrue()
    }

    @Test
    fun `bleibt gesperrt, solange kein Video geladen ist`() = runTest {
        val fixture = createFixture(backgroundScope, FakeAdSource(loadSucceeds = false))

        fixture.manager.start()
        runCurrent()

        assertThat(fixture.manager.status.value.isReady).isFalse()
    }

    // --- Belohnung --------------------------------------------------------

    @Test
    fun `schreibt nach einem gesehenen Video den Booster gut`() = runTest {
        val fixture = createFixture(backgroundScope)
        fixture.manager.start()
        runCurrent()

        fixture.manager.show()
        runCurrent()

        assertThat(fixture.repository.gameState.value.boosters[placement.rewardedBooster])
            .isNotNull()
    }

    @Test
    fun `startet die Wartezeit erst nach einem gesehenen Video`() = runTest {
        val fixture = createFixture(backgroundScope)
        fixture.manager.start()
        runCurrent()

        fixture.manager.show()
        runCurrent()

        assertThat(fixture.repository.gameState.value.ads.isReadyAt(placement, WALL_CLOCK_BASE))
            .isFalse()
        assertThat(fixture.manager.status.value.cooldownRemainingMillis)
            .isEqualTo(placement.cooldownMillis)
    }

    @Test
    fun `belohnt einen Abbruch nicht und sperrt auch nicht`() = runTest {
        // Der Abbruch ist ein regulaerer Ausgang. Die Wartezeit ist der Preis
        // der Belohnung, nicht die Strafe fuer einen Abbruch.
        val fixture = createFixture(backgroundScope, FakeAdSource(result = AdResult.Dismissed))
        fixture.manager.start()
        runCurrent()

        fixture.manager.show()
        runCurrent()

        assertThat(fixture.repository.gameState.value.boosters.isEmpty).isTrue()
        assertThat(fixture.repository.gameState.value.ads.isEmpty).isTrue()
    }

    @Test
    fun `meldet einen fehlgeschlagenen Versuch`() = runTest {
        val fixture = createFixture(
            backgroundScope,
            FakeAdSource(result = AdResult.Failed("kein Netz")),
        )
        fixture.manager.start()
        runCurrent()

        fixture.manager.show()
        runCurrent()

        assertThat(fixture.manager.status.value.lastFailed).isTrue()
        assertThat(fixture.repository.gameState.value.ads.isEmpty).isTrue()
    }

    @Test
    fun `vergisst den Fehlerhinweis auf Wunsch`() = runTest {
        val fixture = createFixture(
            backgroundScope,
            FakeAdSource(result = AdResult.Failed("kein Netz")),
        )
        fixture.manager.start()
        runCurrent()
        fixture.manager.show()
        runCurrent()

        fixture.manager.consumeFailure()

        assertThat(fixture.manager.status.value.lastFailed).isFalse()
    }

    @Test
    fun `zeigt waehrend der Wartezeit kein zweites Video`() = runTest {
        val fixture = createFixture(backgroundScope)
        fixture.manager.start()
        runCurrent()
        fixture.manager.show()
        runCurrent()
        val showsAfterFirst = fixture.adSource.showCount

        fixture.manager.show()
        runCurrent()

        assertThat(fixture.adSource.showCount).isEqualTo(showsAfterFirst)
    }

    @Test
    fun `laedt nach dem Ablauf der Wartezeit nach`() = runTest {
        val fixture = createFixture(backgroundScope)
        fixture.manager.start()
        runCurrent()
        fixture.manager.show()
        runCurrent()

        // Uhr laufen lassen, damit die Sekundenpruefung greift.
        fixture.clock.start()
        advanceTimeBy(placement.cooldownMillis + 2_000L)
        runCurrent()

        assertThat(fixture.manager.status.value.cooldownRemainingMillis).isEqualTo(0L)
        assertThat(fixture.adSource.prepareCount).isAtLeast(2)
    }

    @Test
    fun `raeumt eine abgelaufene Wartezeit beim Aktualisieren weg`() = runTest {
        val fixture = createFixture(backgroundScope)
        fixture.repository.update { state ->
            state.copy(
                ads = state.ads.withCooldownStarted(
                    placement,
                    WALL_CLOCK_BASE - placement.cooldownMillis - 1_000L,
                ),
            )
        }

        fixture.manager.refresh()
        runCurrent()

        assertThat(fixture.repository.gameState.value.ads.isEmpty).isTrue()
    }

    @Test
    fun `protokolliert eine verfallene Belohnung`() = runTest {
        // Der schwerwiegendste denkbare Fehler dieses Systems: Der Spieler hat
        // das Video zu Ende gesehen und geht leer aus. Er darf nicht
        // stillschweigend geschehen.
        val fixture = createFixture(backgroundScope)
        fixture.manager.start()
        runCurrent()

        // Wartezeit von aussen setzen, nachdem der Manager bereits bereit war.
        fixture.repository.update { state ->
            state.copy(ads = state.ads.withCooldownStarted(placement, WALL_CLOCK_BASE))
        }

        fixture.manager.show()
        runCurrent()

        // Auf den Inhalt geprueft und nicht nur auf "irgendeine Warnung":
        // Sonst genuegte kuenftig eine beliebige andere Meldung, um den Test
        // gruen zu halten.
        assertThat(fixture.logger.warnings.any { it.contains("verfaellt") }).isTrue()
        assertThat(fixture.repository.gameState.value.boosters.isEmpty).isTrue()
    }

    private companion object {
        const val WALL_CLOCK_BASE = 1_700_000_000_000L
    }
}
