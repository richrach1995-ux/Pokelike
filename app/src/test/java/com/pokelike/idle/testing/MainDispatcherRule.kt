package com.pokelike.idle.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Ersetzt [Dispatchers.Main] fuer die Dauer eines Tests durch einen
 * Test-Dispatcher.
 *
 * Notwendig fuer alles, was `viewModelScope` benutzt: Dieser Scope laeuft auf
 * dem Main-Dispatcher, und der ist in einem reinen JVM-Test nicht vorhanden -
 * ohne diese Regel scheitert jeder ViewModel-Test mit
 * `Module with the Main dispatcher had failed to initialize`.
 *
 * `runTest` uebernimmt den Scheduler dieses Dispatchers automatisch, sodass
 * Test und ViewModel dieselbe virtuelle Uhr teilen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
