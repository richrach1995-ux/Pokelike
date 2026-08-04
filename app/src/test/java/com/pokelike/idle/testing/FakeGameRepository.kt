package com.pokelike.idle.testing

import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Spielstand-Repository fuer Tests.
 *
 * Haelt den Zustand im Arbeitsspeicher und zaehlt die Schreibvorgaenge. Der
 * Zaehler ist der Kern: Am Autosave interessiert nicht, *was* geschrieben wird,
 * sondern *wann* und *wie oft* - genau dort sitzen die Fehler (doppelte
 * Schleifen, Schreiben im Hintergrund, ausbleibende Sicherung).
 *
 * @property saveCount Anzahl erfolgreicher Schreibvorgaenge.
 * @property failSave Laesst [save] fehlschlagen, um den Fehlerpfad zu pruefen.
 * @property stateToLoad Spielstand, den [load] liefert. Ohne Angabe wird ein
 *   neuer Stand angelegt. Wird gebraucht, um einen gespeicherten Stand mit
 *   zurueckliegendem Zeitstempel nachzustellen - die Voraussetzung fuer jeden
 *   Test des Offline-Fortschritts.
 */
class FakeGameRepository(
    initialState: GameState = GameState(),
    var failSave: Boolean = false,
    var stateToLoad: GameState? = null,
) : GameRepository {

    private val _gameState = MutableStateFlow(initialState)
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    var saveCount: Int = 0
        private set

    var loadCount: Int = 0
        private set

    var resetCount: Int = 0
        private set

    override suspend fun load(nowMillis: Long): GameState {
        loadCount++
        val state = stateToLoad ?: GameState.newGame(nowMillis)
        _gameState.value = state
        _isLoaded.value = true
        return state
    }

    override fun update(transform: (GameState) -> GameState) {
        _gameState.update(transform)
    }

    override suspend fun save(): Boolean {
        if (failSave) return false
        saveCount++
        return true
    }

    override suspend fun resetGame(nowMillis: Long): GameState {
        resetCount++
        val state = GameState.newGame(nowMillis)
        _gameState.value = state
        _isLoaded.value = true
        return state
    }
}

/**
 * Baut einen [ModifierManager] fuer Tests.
 *
 * Er leitet die wirksamen Werte aus dem Spielstand ab und wird von
 * ClickManager, IdleIncomeManager und GameSessionManager benoetigt.
 */
internal fun modifierManagerFor(
    scope: kotlinx.coroutines.CoroutineScope,
    repository: com.pokelike.idle.domain.repository.GameRepository,
): com.pokelike.idle.manager.ModifierManager = com.pokelike.idle.manager.ModifierManager(
    scope = scope,
    repository = repository,
    calculateModifiers = com.pokelike.idle.domain.usecases.CalculateModifiersUseCase(
        com.pokelike.idle.domain.usecases.CalculatePrestigeUseCase(),
    ),
)
