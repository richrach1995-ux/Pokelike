package com.pokelike.idle.domain.repository

import com.pokelike.idle.domain.model.GameState
import kotlinx.coroutines.flow.StateFlow

/**
 * Einzige Zugriffsstelle auf den Spielstand.
 *
 * Das Interface liegt in der Domaenenschicht, seine Umsetzung in der
 * Datenschicht. Dadurch kennen Use Cases und ViewModels nur diesen Vertrag und
 * nichts von Room, DataStore oder einer spaeteren Cloud-Sicherung. Ein Wechsel
 * der Speichertechnik beruehrt sie nicht.
 *
 * Zum Zusammenspiel von Arbeitsspeicher und Datenbank:
 * Der Spielstand wird im Arbeitsspeicher gefuehrt und nur in Abstaenden
 * geschrieben. Bei zehn Aenderungen pro Sekunde waere ein Schreibvorgang je
 * Aenderung sinnlos - er wuerde den Speicher belasten und die Batterie
 * verbrauchen, ohne dem Spieler etwas zu bringen. [gameState] ist deshalb die
 * verbindliche Quelle waehrend der Laufzeit, die Datenbank die Sicherung
 * darueber hinaus.
 */
interface GameRepository {

    /**
     * Aktueller Spielstand.
     *
     * Vor dem ersten [load] steht hier ein leerer Ausgangszustand. Ob bereits
     * geladen wurde, sagt [isLoaded] - die UI kann dadurch warten, statt
     * kurzzeitig einen Kontostand von null anzuzeigen.
     */
    val gameState: StateFlow<GameState>

    /** Ob [load] abgeschlossen ist. */
    val isLoaded: StateFlow<Boolean>

    /**
     * Laedt den Spielstand oder legt einen neuen an.
     *
     * Schlaegt die Integritaetspruefung fehl, wird ebenfalls ein neuer
     * Spielstand angelegt. Ein manipulierter oder beschaedigter Stand darf
     * nicht teilweise uebernommen werden.
     *
     * @param nowMillis Systemzeit, aus der Zeitquelle hereingereicht statt hier
     *   abgefragt - so bleibt das Verhalten testbar.
     */
    suspend fun load(nowMillis: Long): GameState

    /**
     * Aendert den Spielstand im Arbeitsspeicher.
     *
     * Die Aenderung wird atomar angewendet: [transform] kann bei gleichzeitigem
     * Zugriff mehrfach aufgerufen werden und muss deshalb frei von
     * Nebenwirkungen sein.
     */
    fun update(transform: (GameState) -> GameState)

    /**
     * Schreibt den Spielstand, sofern es seit dem letzten Schreiben Aenderungen
     * gab.
     *
     * @return `true`, wenn tatsaechlich geschrieben wurde.
     */
    suspend fun save(): Boolean

    /**
     * Verwirft den Spielstand und beginnt von vorn.
     *
     * Fuer die Funktion "Spiel zuruecksetzen" in den Einstellungen.
     */
    suspend fun resetGame(nowMillis: Long): GameState
}
