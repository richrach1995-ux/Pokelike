package com.pokelike.idle.manager

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.testing.FakeGameRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class AutosaveManagerTest {

    private fun TestScope.createAutosave(
        scope: CoroutineScope,
        repository: FakeGameRepository,
    ): Pair<AutosaveManager, GameClock> {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        val clock = GameClock(
            scope = scope,
            dispatchers = dispatchers,
            timeSource = VirtualTimeSource(testScheduler),
        )
        val autosave = AutosaveManager(
            scope = scope,
            dispatchers = dispatchers,
            gameClock = clock,
            repository = repository,
        )
        return autosave to clock
    }

    @Test
    fun `speichert im konfigurierten Abstand`() = runTest {
        val repository = FakeGameRepository()
        val (autosave, clock) = createAutosave(backgroundScope, repository)

        autosave.start()
        clock.start()
        runCurrent()

        // Zwei volle Autosave-Abstaende.
        advanceTimeBy(2 * GameConfig.AUTOSAVE_INTERVAL_MS)
        runCurrent()

        assertThat(repository.saveCount).isEqualTo(2)
    }

    @Test
    fun `speichert vor Ablauf des Abstands nicht`() = runTest {
        val repository = FakeGameRepository()
        val (autosave, clock) = createAutosave(backgroundScope, repository)

        autosave.start()
        clock.start()
        runCurrent()

        advanceTimeBy(GameConfig.AUTOSAVE_INTERVAL_MS / 2)
        runCurrent()

        assertThat(repository.saveCount).isEqualTo(0)
    }

    @Test
    fun `pausiert mit der angehaltenen Uhr`() = runTest {
        val repository = FakeGameRepository()
        val (autosave, clock) = createAutosave(backgroundScope, repository)

        autosave.start()
        clock.start()
        runCurrent()
        advanceTimeBy(GameConfig.AUTOSAVE_INTERVAL_MS)
        runCurrent()
        assertThat(repository.saveCount).isEqualTo(1)

        // Die Uhr steht - der Autosave darf im Hintergrund nicht weiterlaufen
        // und wiederholt denselben unveraenderten Stand schreiben.
        clock.stop()
        runCurrent()
        advanceTimeBy(5 * GameConfig.AUTOSAVE_INTERVAL_MS)
        runCurrent()

        assertThat(repository.saveCount).isEqualTo(1)
    }

    @Test
    fun `mehrfaches Starten erzeugt keine zweite Schleife`() = runTest {
        val repository = FakeGameRepository()
        val (autosave, clock) = createAutosave(backgroundScope, repository)

        autosave.start()
        autosave.start()
        clock.start()
        runCurrent()

        advanceTimeBy(GameConfig.AUTOSAVE_INTERVAL_MS)
        runCurrent()

        // Zwei Schleifen wuerden doppelt so oft schreiben wie beabsichtigt.
        assertThat(repository.saveCount).isEqualTo(1)
    }

    @Test
    fun `schreibt nach dem Anhalten nicht mehr`() = runTest {
        val repository = FakeGameRepository()
        val (autosave, clock) = createAutosave(backgroundScope, repository)

        autosave.start()
        clock.start()
        runCurrent()

        autosave.stop()
        runCurrent()
        advanceTimeBy(5 * GameConfig.AUTOSAVE_INTERVAL_MS)
        runCurrent()

        assertThat(repository.saveCount).isEqualTo(0)
    }
}
