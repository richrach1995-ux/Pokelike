package com.pokelike.idle.ui.screens.goals

import androidx.annotation.StringRes
import com.pokelike.idle.R
import com.pokelike.idle.domain.model.AchievementType
import com.pokelike.idle.domain.model.QuestPeriod
import com.pokelike.idle.domain.model.QuestType

/**
 * Verbindet Ziele mit ihrer Beschriftung.
 *
 * Wie bei Gebaeuden und Upgrades in der UI-Schicht, damit die Domaenentypen
 * frei von Android bleiben. Als `when` ohne `else`: Ein neues Ziel meldet der
 * Compiler, statt im Spiel ohne Namen zu erscheinen.
 */
@get:StringRes
val QuestType.nameRes: Int
    get() = when (this) {
        QuestType.DAILY_HUNDRED_CLICKS -> R.string.quest_daily_hundred_clicks
        QuestType.DAILY_FIVE_HUNDRED_CLICKS -> R.string.quest_daily_five_hundred_clicks
        QuestType.DAILY_TEN_BUILDINGS -> R.string.quest_daily_ten_buildings
        QuestType.DAILY_TWENTY_CRITICALS -> R.string.quest_daily_twenty_criticals
        QuestType.WEEKLY_FIVE_THOUSAND_CLICKS -> R.string.quest_weekly_five_thousand_clicks
        QuestType.WEEKLY_HUNDRED_BUILDINGS -> R.string.quest_weekly_hundred_buildings
        QuestType.WEEKLY_MILLION_COINS -> R.string.quest_weekly_million_coins
        QuestType.LIFETIME_TEN_THOUSAND_CLICKS -> R.string.quest_lifetime_ten_thousand_clicks
        QuestType.LIFETIME_FIVE_UPGRADES -> R.string.quest_lifetime_five_upgrades
        QuestType.LIFETIME_THOUSAND_BUILDINGS -> R.string.quest_lifetime_thousand_buildings
    }

@get:StringRes
val QuestPeriod.titleRes: Int
    get() = when (this) {
        QuestPeriod.DAILY -> R.string.quest_period_daily
        QuestPeriod.WEEKLY -> R.string.quest_period_weekly
        QuestPeriod.LIFETIME -> R.string.quest_period_lifetime
    }

@get:StringRes
val AchievementType.nameRes: Int
    get() = when (this) {
        AchievementType.FIRST_CLICK -> R.string.achievement_first_click
        AchievementType.HUNDRED_CLICKS -> R.string.achievement_hundred_clicks
        AchievementType.THOUSAND_CLICKS -> R.string.achievement_thousand_clicks
        AchievementType.TEN_THOUSAND_CLICKS -> R.string.achievement_ten_thousand_clicks
        AchievementType.FIRST_CRITICAL -> R.string.achievement_first_critical
        AchievementType.HUNDRED_CRITICALS -> R.string.achievement_hundred_criticals
        AchievementType.THOUSAND_COINS -> R.string.achievement_thousand_coins
        AchievementType.HUNDRED_THOUSAND_COINS -> R.string.achievement_hundred_thousand_coins
        AchievementType.MILLION_COINS -> R.string.achievement_million_coins
        AchievementType.BILLION_COINS -> R.string.achievement_billion_coins
        AchievementType.TRILLION_COINS -> R.string.achievement_trillion_coins
        AchievementType.FIRST_BUILDING -> R.string.achievement_first_building
        AchievementType.TEN_BUILDINGS -> R.string.achievement_ten_buildings
        AchievementType.HUNDRED_BUILDINGS -> R.string.achievement_hundred_buildings
        AchievementType.THOUSAND_BUILDINGS -> R.string.achievement_thousand_buildings
        AchievementType.ALL_BUILDING_TYPES -> R.string.achievement_all_building_types
        AchievementType.FIRST_UPGRADE -> R.string.achievement_first_upgrade
        AchievementType.FIVE_UPGRADES -> R.string.achievement_five_upgrades
        AchievementType.FIRST_PRESTIGE -> R.string.achievement_first_prestige
        AchievementType.FIFTH_PRESTIGE -> R.string.achievement_fifth_prestige
    }
