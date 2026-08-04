package com.pokelike.idle.domain.model

/**
 * Stand des Prestige-Systems.
 *
 * Enthaelt alles, was der Spieler vor der Entscheidung wissen muss. Ein
 * Prestige-Reset ist unumkehrbar und loescht den gesamten Ausbau - er darf
 * deshalb nie ohne vollstaendige Angaben ausloesbar sein.
 *
 * @property currentPoints Bereits vorhandene Prestige-Punkte.
 * @property pointsOnReset Punkte, die ein Reset jetzt einbringen wuerde.
 * @property currentBonus Wirksamer Faktor aus den vorhandenen Punkten.
 * @property bonusAfterReset Faktor nach dem Reset.
 * @property coinsForNextPoint Lebenssumme, die fuer einen weiteren Punkt noetig
 *   ist. Ohne diese Angabe waere fuer den Spieler nicht erkennbar, ob sich
 *   Weiterspielen lohnt oder ob er schon am Ende der Kurve steht.
 * @property progressToNextPoint Fortschritt zum naechsten Punkt, 0 bis 1.
 * @property canPrestige Ob ein Reset zulaessig ist.
 */
data class PrestigeInfo(
    val currentPoints: BigNumber,
    val pointsOnReset: BigNumber,
    val currentBonus: Double,
    val bonusAfterReset: Double,
    val coinsForNextPoint: BigNumber,
    val progressToNextPoint: Float,
    val canPrestige: Boolean,
) {
    companion object {
        /** Ausgangszustand, bevor etwas verdient wurde. */
        val NONE = PrestigeInfo(
            currentPoints = BigNumber.ZERO,
            pointsOnReset = BigNumber.ZERO,
            currentBonus = 1.0,
            bonusAfterReset = 1.0,
            coinsForNextPoint = BigNumber.ZERO,
            progressToNextPoint = 0f,
            canPrestige = false,
        )
    }
}
