package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.UpgradeType
import javax.inject.Inject

/**
 * Ergebnis eines Upgrade-Kaufs.
 *
 * Die Fehlerfaelle sind getrennt, weil sie unterschiedliche Rueckmeldungen
 * verlangen: "zu teuer" heisst weitersparen, "bereits gekauft" deutet auf einen
 * Fehler in der Oberflaeche hin, und "noch nicht freigeschaltet" bedeutet, dass
 * der Eintrag gar nicht haette sichtbar sein duerfen.
 */
sealed interface UpgradePurchaseResult {

    data class Success(
        val state: GameState,
        val upgrade: UpgradeType,
    ) : UpgradePurchaseResult

    data object NotAffordable : UpgradePurchaseResult

    data object AlreadyOwned : UpgradePurchaseResult

    data object NotUnlocked : UpgradePurchaseResult
}

/**
 * Kauft ein Upgrade.
 *
 * Prueft Besitz, Freischaltung und Deckung in dieser Reihenfolge. Die
 * Reihenfolge ist bewusst gewaehlt: Ein bereits gekauftes Upgrade darf nie
 * erneut abgebucht werden, auch wenn die Freischaltbedingung inzwischen nicht
 * mehr erfuellt waere.
 *
 * Die Freischaltung wird hier erneut geprueft und nicht nur in der Oberflaeche.
 * Andernfalls koennte ein Fehler bei der Filterung der Liste zu einem Kauf
 * fuehren, den das Spiel nie erlauben wollte.
 */
class PurchaseUpgradeUseCase @Inject constructor() {

    operator fun invoke(state: GameState, upgrade: UpgradeType): UpgradePurchaseResult {
        if (upgrade in state.upgrades) return UpgradePurchaseResult.AlreadyOwned
        if (!upgrade.unlockCondition.isMet(state)) return UpgradePurchaseResult.NotUnlocked

        val price = ResourceBundle.single(ResourceType.COINS, upgrade.priceAsBigNumber)
        val paidState = state.spend(price) ?: return UpgradePurchaseResult.NotAffordable

        return UpgradePurchaseResult.Success(
            state = paidState.copy(upgrades = paidState.upgrades.plus(upgrade)),
            upgrade = upgrade,
        )
    }
}
