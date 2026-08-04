package com.pokelike.idle.domain.model

/**
 * Combo-Stand in der Form, die die Oberflaeche braucht.
 *
 * [ComboState] traegt einen Zeitstempel der monotonen Uhr. Die Oberflaeche kann
 * damit nichts anfangen - sie muesste selbst die Zeit abfragen, um den
 * Restanteil auszurechnen, und haette damit eine Abhaengigkeit zur Zeitquelle.
 * Dieser Schnappschuss wird stattdessen einmal je Takt gebildet und enthaelt
 * nur noch fertige Werte.
 *
 * @property count Anzahl der Klicks in Folge.
 * @property multiplier Wirksamer Ertragsfaktor.
 * @property remainingFraction Verbleibender Anteil des Zeitfensters, 1.0 bis 0.0.
 */
data class ComboSnapshot(
    val count: Int,
    val multiplier: Double,
    val remainingFraction: Float,
) {

    val isActive: Boolean get() = count > 0

    companion object {
        /** Keine laufende Combo. */
        val NONE = ComboSnapshot(count = 0, multiplier = 1.0, remainingFraction = 0f)

        /** Bildet den Schnappschuss zu einem Zeitpunkt der monotonen Uhr. */
        fun from(state: ComboState, nowMillis: Long): ComboSnapshot =
            if (!state.isActive || state.isExpired(nowMillis)) {
                NONE
            } else {
                ComboSnapshot(
                    count = state.count,
                    multiplier = state.multiplier(),
                    remainingFraction = state.remainingFraction(nowMillis),
                )
            }
    }
}
