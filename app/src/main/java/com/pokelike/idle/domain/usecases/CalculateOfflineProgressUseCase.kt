package com.pokelike.idle.domain.usecases

import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.OfflineProgress
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import javax.inject.Inject

/**
 * Berechnet, was ein Spieler waehrend seiner Abwesenheit verdient hat.
 *
 * Bewusst eine reine Funktion ohne Zustand und ohne Zugriff auf Uhr,
 * Datenbank oder Einstellungen: Alle Eingaben werden hereingereicht. Dadurch
 * laesst sich das Verhalten fuer beliebige Zeitspannen ohne Emulator pruefen -
 * und genau hier sitzen die Regeln, an denen ein Spiel wirtschaftlich haengt.
 *
 * Die Berechnung stuetzt sich zwangslaeufig auf die Systemzeit, denn nur sie
 * laeuft weiter, waehrend der Prozess beendet ist. Die monotone Uhr steht dafuer
 * nicht zur Verfuegung. Deshalb ist die Manipulationspruefung hier kein
 * Beiwerk, sondern der Kern: Ohne sie genuegt ein Vorstellen der Geraeteuhr,
 * um beliebig viel Ertrag zu erzeugen.
 */
class CalculateOfflineProgressUseCase @Inject constructor() {

    /**
     * @param lastSeenAtMillis Systemzeit beim letzten Speichern.
     * @param nowMillis Aktuelle Systemzeit.
     * @param incomePerSecond Einkommen pro Sekunde zum Zeitpunkt des Verlassens.
     * @param efficiency Anteil des Einkommens, der offline anfaellt. Als
     *   Parameter statt als Konstante, weil Booster und Upgrades ihn anheben.
     * @param maxOfflineMillis Obergrenze der anrechenbaren Zeit. Ebenfalls
     *   veraenderbar, da Premiumangebote sie verlaengern.
     */
    operator fun invoke(
        lastSeenAtMillis: Long,
        nowMillis: Long,
        incomePerSecond: BigNumber,
        efficiency: Double = GameConfig.OFFLINE_EFFICIENCY,
        maxOfflineMillis: Long = GameConfig.MAX_OFFLINE_MILLIS,
    ): OfflineProgress {
        require(efficiency >= 0.0) { "Negative Offline-Effizienz: $efficiency" }
        require(maxOfflineMillis >= 0L) { "Negative Obergrenze: $maxOfflineMillis" }

        val elapsed = nowMillis - lastSeenAtMillis

        // Der Speicherzeitpunkt liegt in der Zukunft. Entweder wurde die Uhr
        // vorgestellt und danach zurueckgesetzt, oder es wird gerade versucht.
        // Die Toleranz deckt legitime Spruenge ab: Zeitzonenwechsel,
        // Sommerzeit und die Korrektur durch einen Zeitserver.
        if (elapsed < -GameConfig.CLOCK_TOLERANCE_MILLIS) {
            return OfflineProgress.NONE.copy(clockTamperingDetected = true)
        }

        // Kurze Unterbrechungen erzeugen keinen nennenswerten Ertrag, aber
        // einen stoerenden Dialog.
        if (elapsed < GameConfig.MIN_OFFLINE_MILLIS) return OfflineProgress.NONE

        val credited = elapsed.coerceAtMost(maxOfflineMillis)
        val wasCapped = elapsed > maxOfflineMillis

        if (incomePerSecond.isZero || efficiency == 0.0) {
            return OfflineProgress(
                creditedMillis = credited,
                elapsedMillis = elapsed,
                earned = ResourceBundle.EMPTY,
                wasCapped = wasCapped,
                clockTamperingDetected = false,
            )
        }

        val seconds = credited.toDouble() / MILLIS_PER_SECOND
        val earned = incomePerSecond * seconds * efficiency

        return OfflineProgress(
            creditedMillis = credited,
            elapsedMillis = elapsed,
            earned = ResourceBundle.single(ResourceType.COINS, earned),
            wasCapped = wasCapped,
            clockTamperingDetected = false,
        )
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000.0
    }
}
