package com.pokelike.idle.data.repository

import com.pokelike.idle.data.database.dao.GameStateDao
import com.pokelike.idle.data.database.mapper.GameStateMapper
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.GameLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuehrt den Spielstand im Arbeitsspeicher und sichert ihn in der Datenbank.
 *
 * Aufbau in Kurzform:
 *
 * - [gameState] ist waehrend der Laufzeit die verbindliche Quelle. Alle
 *   Aenderungen laufen ueber [update] und wirken sofort.
 * - Geschrieben wird nur ueber [save] - vom Autosave in Abstaenden und beim
 *   Wechsel in den Hintergrund. Bei zehn Aenderungen pro Sekunde je Aenderung
 *   zu schreiben wuerde nichts verbessern und Speicher wie Batterie belasten.
 * - Eine Aenderungsmarkierung verhindert wirkungslose Schreibvorgaenge: Wer
 *   die App offen liegen laesst, ohne zu spielen, schreibt nichts.
 */
@Singleton
class GameRepositoryImpl @Inject constructor(
    private val dao: GameStateDao,
    private val mapper: GameStateMapper,
    private val dispatchers: DispatcherProvider,
    private val logger: GameLogger,
) : GameRepository {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    /**
     * Ob sich seit dem letzten Schreiben etwas geaendert hat.
     *
     * [AtomicBoolean], weil [update] von der Tick-Schleife und [save] vom
     * Autosave aufgerufen wird - beide auf unterschiedlichen Threads.
     */
    private val isDirty = AtomicBoolean(false)

    /**
     * Serialisiert Datenbankzugriffe.
     *
     * Ohne ihn koennten ein regulaerer Autosave und ein Speichern beim Wechsel
     * in den Hintergrund gleichzeitig laufen. Beide schreiben dieselben zwei
     * Tabellen, und der langsamere wuerde den neueren Stand ueberschreiben.
     */
    private val persistenceLock = Mutex()

    override suspend fun load(nowMillis: Long): GameState = persistenceLock.withLock {
        val loaded = withContext(dispatchers.io) {
            try {
                val entity = dao.findState()
                if (entity == null) {
                    null
                } else {
                    // Liefert null, wenn die Pruefsumme nicht passt.
                    mapper.toDomain(
                        entity = entity,
                        resources = dao.findResources(),
                        buildings = dao.findBuildings(),
                        upgrades = dao.findUpgrades(),
                        achievements = dao.findAchievements(),
                        questBaselines = dao.findQuestBaselines(),
                        questBaselineValues = dao.findQuestBaselineValues(),
                        questClaims = dao.findQuestClaims(),
                        dailyLogin = dao.findDailyLogin(),
                    ).also { state ->
                        if (state == null) {
                            logger.warn(TAG, "Spielstand hat die Integritaetspruefung nicht bestanden")
                        }
                    }
                }
            } catch (throwable: Throwable) {
                // Eine beschaedigte Datenbankdatei darf den Start nicht
                // verhindern. Die App faengt dann mit einem neuen Spielstand an
                // - unschoen, aber immer noch besser als eine Anwendung, die
                // sich nicht mehr oeffnen laesst und deren Splashscreen haengt.
                logger.error(TAG, "Spielstand konnte nicht gelesen werden", throwable)
                null
            }
        }

        val state = loaded ?: GameState.newGame(nowMillis)

        _gameState.value = state
        _isLoaded.value = true

        // Ein frisch angelegter Spielstand ist noch nicht gesichert. Ohne diese
        // Markierung ginge er verloren, falls die App vor der ersten Aenderung
        // beendet wird - der Spieler stuende beim naechsten Start erneut am
        // Anfang und haette seine Startdiamanten wieder.
        if (loaded == null) isDirty.set(true)

        state
    }

    override fun update(transform: (GameState) -> GameState) {
        _gameState.update(transform)
        isDirty.set(true)
    }

    override suspend fun save(): Boolean {
        // Die Markierung wird vor dem Schreiben zurueckgesetzt. Eine Aenderung,
        // die waehrend des Schreibens eintrifft, setzt sie erneut und wird beim
        // naechsten Durchgang beruecksichtigt. Andersherum - erst schreiben,
        // dann zuruecksetzen - ginge genau diese Aenderung verloren.
        if (!isDirty.getAndSet(false)) return false

        val snapshot = _gameState.value

        return persistenceLock.withLock {
            withContext(dispatchers.io) {
                try {
                    val persisted = mapper.toPersisted(snapshot)
                    dao.saveState(
                        state = persisted.state,
                        resources = persisted.resources,
                        buildings = persisted.buildings,
                        upgrades = persisted.upgrades,
                        achievements = persisted.achievements,
                        questBaselines = persisted.questBaselines,
                        questBaselineValues = persisted.questBaselineValues,
                        questClaims = persisted.questClaims,
                        dailyLogin = persisted.dailyLogin,
                    )
                    true
                } catch (throwable: Throwable) {
                    // Der Fortschritt bleibt im Arbeitsspeicher erhalten, und
                    // die Markierung wird neu gesetzt, damit der naechste
                    // Autosave es erneut versucht. Ein Absturz waere hier die
                    // schlechteste Antwort: Er wuerde genau den Fortschritt
                    // vernichten, der noch nicht geschrieben werden konnte.
                    logger.error(TAG, "Spielstand konnte nicht geschrieben werden", throwable)
                    isDirty.set(true)
                    false
                }
            }
        }
    }

    override suspend fun resetGame(nowMillis: Long): GameState = persistenceLock.withLock {
        val fresh = GameState.newGame(nowMillis)

        withContext(dispatchers.io) {
            dao.clearAll()
            val persisted = mapper.toPersisted(fresh)
            dao.saveState(
                state = persisted.state,
                resources = persisted.resources,
                buildings = persisted.buildings,
                upgrades = persisted.upgrades,
                achievements = persisted.achievements,
                questBaselines = persisted.questBaselines,
                questBaselineValues = persisted.questBaselineValues,
                questClaims = persisted.questClaims,
                dailyLogin = persisted.dailyLogin,
            )
        }

        _gameState.value = fresh
        _isLoaded.value = true
        isDirty.set(false)

        fresh
    }

    private companion object {
        const val TAG = "GameRepository"
    }
}
