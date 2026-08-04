package com.pokelike.idle.util

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Formatiert Zeitspannen fuer die Anzeige.
 *
 * Wird an vielen Stellen gebraucht - Sitzungsdauer, Booster-Restlaufzeit,
 * Offline-Zeit, Cooldowns -, deshalb steht die Regel genau einmal hier statt
 * in jedem Screen erneut.
 */
@Singleton
class DurationFormatter @Inject constructor() {

    /**
     * Formatiert [millis] als `mm:ss`, ab einer Stunde als `h:mm:ss`.
     *
     * Negative Eingaben werden auf 0 geklemmt: Sie entstehen, wenn zwei
     * Zeitstempel aus unterschiedlichen Quellen verrechnet werden, und sollen
     * in der UI nie als negative Dauer erscheinen.
     */
    fun formatCompact(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0L)) / MILLIS_PER_SECOND
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val SECONDS_PER_MINUTE = 60L
        const val SECONDS_PER_HOUR = 3_600L
    }
}
