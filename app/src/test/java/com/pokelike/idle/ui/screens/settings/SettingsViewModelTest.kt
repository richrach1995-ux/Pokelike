package com.pokelike.idle.ui.screens.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.GameSettings
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.ThemeMode
import com.pokelike.idle.testing.FakeGameRepository
import com.pokelike.idle.testing.FakeSettingsRepository
import com.pokelike.idle.testing.MainDispatcherRule
import com.pokelike.idle.testing.VirtualTimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class Fixture(
        val viewModel: SettingsViewModel,
        val settings: FakeSettingsRepository,
        val game: FakeGameRepository,
    )

    private fun createFixture(
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        game: FakeGameRepository = FakeGameRepository(),
        timeSource: VirtualTimeSource,
    ): Fixture = Fixture(
        viewModel = SettingsViewModel(
            settingsRepository = settings,
            gameRepository = game,
            timeSource = timeSource,
        ),
        settings = settings,
        game = game,
    )

    @Test
    fun `zeigt die gespeicherten Einstellungen`() = runTest {
        val fixture = createFixture(
            settings = FakeSettingsRepository(
                GameSettings(themeMode = ThemeMode.DARK, vibrationEnabled = false),
            ),
            timeSource = VirtualTimeSource(testScheduler),
        )

        fixture.viewModel.uiState.test {
            // Der erste Wert ist der Ausgangswert des StateFlow; erst nach
            // einem Durchlauf des Dispatchers steht der gespeicherte Stand.
            awaitItem()
            runCurrent()

            val state = expectMostRecentItem()
            assertThat(state.themeMode).isEqualTo(ThemeMode.DARK)
            assertThat(state.vibrationEnabled).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `schreibt eine Aenderung sofort`() = runTest {
        // Es gibt bewusst kein "Speichern": Wer den Bildschirm ueber die
        // Zurueck-Taste verlaesst, wuerde seine Auswahl sonst verlieren.
        val fixture = createFixture(timeSource = VirtualTimeSource(testScheduler))

        fixture.viewModel.onThemeModeSelected(ThemeMode.LIGHT)
        runCurrent()

        assertThat(fixture.settings.current.themeMode).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `schaltet die Vibration um`() = runTest {
        val fixture = createFixture(timeSource = VirtualTimeSource(testScheduler))

        fixture.viewModel.onVibrationChanged(false)
        runCurrent()

        assertThat(fixture.settings.current.vibrationEnabled).isFalse()
    }

    @Test
    fun `loescht den Spielstand`() = runTest {
        val played = GameState.newGame(nowMillis = 0L).copy(
            resources = ResourcePool.of(ResourceType.COINS to BigNumber.of(1.0, 20)),
        )
        val fixture = createFixture(
            game = FakeGameRepository(initialState = played),
            timeSource = VirtualTimeSource(testScheduler),
        )

        fixture.viewModel.onResetGame()
        runCurrent()

        assertThat(fixture.game.gameState.value[ResourceType.COINS]).isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `nennt die Version des Spielstandformats`() = runTest {
        // Fuer Fehlerberichte: Sie sagt mehr ueber einen kaputten Spielstand
        // aus als die App-Version.
        val fixture = createFixture(timeSource = VirtualTimeSource(testScheduler))

        assertThat(fixture.viewModel.uiState.value.saveVersion)
            .isEqualTo(GameState.CURRENT_SCHEMA_VERSION)
    }
}
