package com.pokelike.idle.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.util.NumberFormatter

/**
 * Ein Bestandteil einer Belohnung, fertig zur Anzeige.
 *
 * Die Beschriftung steht als Ressourcen-ID hier und nicht als fertiger Text:
 * Ein ViewModel hat keinen Context und darf keine Zeichenketten aufloesen. Das
 * uebernimmt der Composable.
 *
 * @property labelRes Name der Ressource.
 * @property amount Betrag, bereits formatiert.
 */
@Immutable
data class RewardPart(
    @StringRes val labelRes: Int,
    val amount: String,
)

/**
 * Beschriftung einer Ressource.
 *
 * Bewusst in der UI-Schicht: [ResourceType] bleibt dadurch frei von
 * Android-Typen und in einer reinen JVM-Umgebung uebersetzbar.
 *
 * Als `when` ohne `else` - eine neue Ressource meldet der Compiler, statt im
 * Spiel ohne Namen zu erscheinen.
 */
@get:StringRes
val ResourceType.labelRes: Int
    get() = when (this) {
        ResourceType.COINS -> R.string.resource_coins
        ResourceType.DIAMONDS -> R.string.resource_diamonds
        ResourceType.TICKETS -> R.string.resource_tickets
        ResourceType.EVENT_TOKENS -> R.string.resource_event_tokens
        ResourceType.PRESTIGE_POINTS -> R.string.resource_prestige_points
    }

/**
 * Zerlegt eine Belohnung in anzeigbare Bestandteile.
 *
 * Genau eine Stelle fuer diese Umwandlung: Quests, Achievements und spaeter
 * Login-Bonus, Events und Battle Pass sollen ihre Belohnungen identisch
 * darstellen.
 */
fun ResourceBundle.toRewardParts(formatter: NumberFormatter): List<RewardPart> =
    amounts.map { (type, amount) ->
        RewardPart(labelRes = type.labelRes, amount = formatter.format(amount))
    }
