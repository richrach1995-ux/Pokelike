package com.pokelike.idle.ui.screens.upgrades

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.UpgradeCategory
import com.pokelike.idle.domain.model.UpgradeType

/**
 * Verbindet Upgrades mit ihrer Darstellung.
 *
 * Wie bei den Gebaeuden bewusst in der UI-Schicht, damit [UpgradeType] frei von
 * Android-Typen bleibt und in einer reinen JVM-Umgebung uebersetzbar ist.
 *
 * Als `when` ohne `else`: Kommt ein Upgrade hinzu, meldet der Compiler den
 * fehlenden Zweig - andernfalls erschiene es im Spiel ohne Namen.
 */
@get:StringRes
val UpgradeType.nameRes: Int
    get() = when (this) {
        UpgradeType.STRONGER_FINGERS -> R.string.upgrade_stronger_fingers
        UpgradeType.IRON_FINGERS -> R.string.upgrade_iron_fingers
        UpgradeType.GOLDEN_TOUCH -> R.string.upgrade_golden_touch
        UpgradeType.HEAVY_HANDS -> R.string.upgrade_heavy_hands
        UpgradeType.THUNDER_STRIKE -> R.string.upgrade_thunder_strike
        UpgradeType.LUCKY_CHARM -> R.string.upgrade_lucky_charm
        UpgradeType.FOUR_LEAF_CLOVER -> R.string.upgrade_four_leaf_clover
        UpgradeType.CRITICAL_MASS -> R.string.upgrade_critical_mass
        UpgradeType.PRODUCTION_LINE -> R.string.upgrade_production_line
        UpgradeType.AUTOMATION -> R.string.upgrade_automation
        UpgradeType.GLOBAL_LOGISTICS -> R.string.upgrade_global_logistics
        UpgradeType.NIMBLE_FINGERS -> R.string.upgrade_nimble_fingers
        UpgradeType.ERGONOMIC_CURSOR -> R.string.upgrade_ergonomic_cursor
        UpgradeType.DEEP_DRILLING -> R.string.upgrade_deep_drilling
        UpgradeType.FERTILIZER -> R.string.upgrade_fertilizer
        UpgradeType.BULK_DISCOUNT -> R.string.upgrade_bulk_discount
        UpgradeType.WHOLESALE -> R.string.upgrade_wholesale
        UpgradeType.NIGHT_SHIFT -> R.string.upgrade_night_shift
        UpgradeType.DREAM_FACTORY -> R.string.upgrade_dream_factory
    }

@get:StringRes
val UpgradeType.descriptionRes: Int
    get() = when (this) {
        UpgradeType.STRONGER_FINGERS -> R.string.upgrade_stronger_fingers_description
        UpgradeType.IRON_FINGERS -> R.string.upgrade_iron_fingers_description
        UpgradeType.GOLDEN_TOUCH -> R.string.upgrade_golden_touch_description
        UpgradeType.HEAVY_HANDS -> R.string.upgrade_heavy_hands_description
        UpgradeType.THUNDER_STRIKE -> R.string.upgrade_thunder_strike_description
        UpgradeType.LUCKY_CHARM -> R.string.upgrade_lucky_charm_description
        UpgradeType.FOUR_LEAF_CLOVER -> R.string.upgrade_four_leaf_clover_description
        UpgradeType.CRITICAL_MASS -> R.string.upgrade_critical_mass_description
        UpgradeType.PRODUCTION_LINE -> R.string.upgrade_production_line_description
        UpgradeType.AUTOMATION -> R.string.upgrade_automation_description
        UpgradeType.GLOBAL_LOGISTICS -> R.string.upgrade_global_logistics_description
        UpgradeType.NIMBLE_FINGERS -> R.string.upgrade_nimble_fingers_description
        UpgradeType.ERGONOMIC_CURSOR -> R.string.upgrade_ergonomic_cursor_description
        UpgradeType.DEEP_DRILLING -> R.string.upgrade_deep_drilling_description
        UpgradeType.FERTILIZER -> R.string.upgrade_fertilizer_description
        UpgradeType.BULK_DISCOUNT -> R.string.upgrade_bulk_discount_description
        UpgradeType.WHOLESALE -> R.string.upgrade_wholesale_description
        UpgradeType.NIGHT_SHIFT -> R.string.upgrade_night_shift_description
        UpgradeType.DREAM_FACTORY -> R.string.upgrade_dream_factory_description
    }

/**
 * Symbol der Kategorie.
 *
 * Bewusst je Kategorie und nicht je Upgrade: Zwanzig unterschiedliche Symbole
 * traegen keine zusaetzliche Information, waehrend ein gemeinsames Symbol je
 * Kategorie die Liste auf einen Blick gliedert.
 */
val UpgradeCategory.icon: ImageVector
    get() = when (this) {
        UpgradeCategory.CLICK -> Icons.Filled.Bolt
        UpgradeCategory.CRITICAL -> Icons.Filled.Star
        UpgradeCategory.INCOME -> Icons.Filled.Factory
        UpgradeCategory.BUILDING -> Icons.Filled.Storefront
        UpgradeCategory.OFFLINE -> Icons.Filled.Bedtime
    }
