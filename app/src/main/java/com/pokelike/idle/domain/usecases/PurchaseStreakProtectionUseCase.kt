package com.pokelike.idle.domain.usecases

import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import javax.inject.Inject
import javax.inject.Singleton

/** Ergebnis eines Kaufs von Serienschutz. */
sealed interface StreakProtectionPurchaseResult {

    data class Success(val state: GameState) : StreakProtectionPurchaseResult

    /** Der Diamantenstand reicht nicht. */
    data object NotAffordable : StreakProtectionPurchaseResult

    /**
     * Der Vorrat ist voll.
     *
     * Von [NotAffordable] getrennt, weil die Oberflaeche beides
     * unterschiedlich erklaeren muss: Im einen Fall soll der Spieler
     * Diamanten beschaffen, im anderen waere das vergeblich.
     */
    data object AtMaximum : StreakProtectionPurchaseResult
}

/**
 * Kauft eine Ladung Serienschutz.
 *
 * Der erste Ausgabezweck fuer Diamanten und damit die erste Stelle, an der die
 * Premiumwaehrung einen Gegenwert bekommt. Bewusst ein Verbrauchsgut und kein
 * dauerhafter Schutz: Ein einmal gekaufter Dauerschutz waere ein einziger
 * Kauf, danach nie wieder - und die Serie haette ihre Bedeutung verloren, weil
 * sie nicht mehr reissen kann.
 *
 * Preis und Deckel stehen in [GameConfig] und nicht hier. Beides sind Werte,
 * die sich mit jeder Balancing-Runde aendern koennen, und der Ablauf des Kaufs
 * bleibt davon unberuehrt.
 */
@Singleton
class PurchaseStreakProtectionUseCase @Inject constructor() {

    /** Preis einer Ladung. Die Oberflaeche zeigt ihn vor dem Kauf an. */
    val price: ResourceBundle = ResourceBundle.single(
        type = ResourceType.DIAMONDS,
        amount = BigNumber.of(GameConfig.STREAK_PROTECTION_PRICE_DIAMONDS),
    )

    operator fun invoke(state: GameState): StreakProtectionPurchaseResult {
        if (state.login.protectionCharges >= GameConfig.STREAK_PROTECTION_MAX_CHARGES) {
            return StreakProtectionPurchaseResult.AtMaximum
        }

        // spend prueft den Bestand und bucht in einem Schritt ab. Getrennt
        // waere zwischen Pruefung und Buchung ein Zustand moeglich, in dem der
        // Betrag bereits anderweitig ausgegeben wurde.
        val paid = state.spend(price) ?: return StreakProtectionPurchaseResult.NotAffordable

        return StreakProtectionPurchaseResult.Success(
            state = paid.copy(login = paid.login.withProtectionCharge()),
        )
    }
}
