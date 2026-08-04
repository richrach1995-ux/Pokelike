package com.pokelike.idle.config

/**
 * Zentrale Sammlung aller Balancing- und Laufzeitkonstanten.
 *
 * Warum ein eigenes Objekt statt verstreuter Konstanten:
 * Ein Idle-Spiel wird ueber seine Zahlen gesteuert, nicht ueber seinen Code.
 * Sobald Werte in Screens, ViewModels oder Use Cases verteilt liegen, ist
 * Balancing eine Suchaktion durch die halbe Codebasis. Alles, was ein Designer
 * jemals drehen wollen wird, steht deshalb hier.
 *
 * Ab dem Remote-Config-Schritt wird dieses Objekt zur Fallback-Ebene: Firebase
 * liefert dann die aktiven Werte, und die Konstanten hier greifen, solange kein
 * Remote-Wert vorliegt (Erststart, kein Netz, Fetch fehlgeschlagen). Deshalb
 * sind die Werte bewusst so gewaehlt, dass das Spiel auch komplett offline
 * sinnvoll spielbar bleibt.
 */
object GameConfig {

    /**
     * Taktrate der Spiel-Engine.
     *
     * 100 ms sind ein bewusster Kompromiss: fein genug, damit Einkommen und
     * Booster-Restlaufzeiten fluessig wirken, aber grob genug, dass die Engine
     * unabhaengig von der Bildwiederholrate laeuft. Die Engine wird bewusst
     * NICHT an die Compose-Frameclock gekoppelt: bei 120 Hz wuerde das Spiel
     * sonst doppelt so schnell ticken wie bei 60 Hz.
     */
    const val TICK_INTERVAL_MS: Long = 100L

    /**
     * Obergrenze fuer die Zeitspanne, die ein einzelner Tick verrechnen darf.
     *
     * Wird der Prozess gedrosselt oder haelt das System die Coroutine an, kann
     * zwischen zwei Ticks deutlich mehr Zeit vergehen als geplant. Ohne Deckel
     * wuerde ein einziger Tick dann eine riesige Menge Ertrag gutschreiben.
     * Alles oberhalb dieser Grenze ist per Definition Offline-Zeit und wird vom
     * Offline-Progress-Pfad behandelt, nicht vom Live-Tick.
     */
    const val MAX_TICK_DELTA_MS: Long = 1_000L

    /**
     * Abstand zwischen zwei automatischen Speichervorgaengen.
     *
     * 30 Sekunden begrenzen den maximalen Fortschrittsverlust bei einem Crash
     * auf ein Mass, das Spieler erfahrungsgemaess noch akzeptieren, ohne die
     * Datenbank unnoetig oft zu beschreiben.
     */
    const val AUTOSAVE_INTERVAL_MS: Long = 30_000L

    /**
     * Preissteigerung pro gekauftem Gebaeude: `preis = basispreis * 1.15^anzahl`.
     *
     * 1.15 ist der etablierte Standardfaktor des Genres. Er erzeugt eine
     * Kaufkurve, bei der jedes Gebaeude sich zunaechst lohnt und spaeter
     * natuerlich von der naechsten Stufe abgeloest wird.
     */
    const val BUILDING_COST_GROWTH: Double = 1.15
}
