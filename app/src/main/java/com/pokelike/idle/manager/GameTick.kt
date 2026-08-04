package com.pokelike.idle.manager

/**
 * Ein einzelner Takt der Spiel-Engine.
 *
 * @property index Fortlaufende Nummer seit Start der Uhr. Nuetzlich fuer
 *   Systeme, die nur jeden n-ten Takt arbeiten muessen (z. B. Autosave oder
 *   die Event-Wuerfelprobe), damit nicht jedes System im 100-ms-Raster laeuft.
 * @property deltaMillis Vergangene Zeit seit dem vorherigen Takt, bereits auf
 *   [com.pokelike.idle.config.GameConfig.MAX_TICK_DELTA_MS] begrenzt. Alle
 *   Ertragsberechnungen muessen mit diesem Wert rechnen und nicht mit dem
 *   Sollintervall - sonst laeuft das Spiel auf gedrosselten Geraeten langsamer.
 * @property elapsedMillis Summe aller bisher verrechneten Deltas. Entspricht
 *   der aktiven Spielzeit dieser Sitzung, ohne Pausen im Hintergrund.
 */
data class GameTick(
    val index: Long,
    val deltaMillis: Long,
    val elapsedMillis: Long,
) {
    companion object {
        /** Ausgangszustand vor dem ersten Takt. */
        val ZERO = GameTick(index = 0L, deltaMillis = 0L, elapsedMillis = 0L)
    }
}
