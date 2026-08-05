package com.pokelike.idle.ui.screens.home

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.BoosterType

/**
 * Verbindet Booster mit Beschriftung und Sinnbild.
 *
 * Wie bei Gebaeuden, Upgrades und Zielen in der UI-Schicht, damit
 * [BoosterType] frei von Android bleibt. Als `when` ohne `else`: Ein neuer
 * Booster meldet der Compiler, statt im Spiel ohne Namen zu erscheinen.
 */
@get:StringRes
val BoosterType.nameRes: Int
    get() = when (this) {
        BoosterType.DOUBLE_CLICKS -> R.string.booster_double_clicks
        BoosterType.DOUBLE_INCOME -> R.string.booster_double_income
        BoosterType.LUCKY_HOUR -> R.string.booster_lucky_hour
        BoosterType.GOLD_RUSH -> R.string.booster_gold_rush
    }

@get:StringRes
val BoosterType.descriptionRes: Int
    get() = when (this) {
        BoosterType.DOUBLE_CLICKS -> R.string.booster_double_clicks_description
        BoosterType.DOUBLE_INCOME -> R.string.booster_double_income_description
        BoosterType.LUCKY_HOUR -> R.string.booster_lucky_hour_description
        BoosterType.GOLD_RUSH -> R.string.booster_gold_rush_description
    }

/**
 * Sinnbild des Boosters.
 *
 * In der laufenden Anzeige steht nur das Sinnbild, nicht der Name: Bei vier
 * gleichzeitig laufenden Boostern waere eine Zeile mit vier Namen breiter als
 * der Bildschirm.
 */
val BoosterType.icon: ImageVector
    get() = when (this) {
        BoosterType.DOUBLE_CLICKS -> Icons.Filled.TouchApp
        BoosterType.DOUBLE_INCOME -> Icons.Filled.Bolt
        BoosterType.LUCKY_HOUR -> Icons.Filled.Casino
        BoosterType.GOLD_RUSH -> Icons.Filled.RocketLaunch
    }
