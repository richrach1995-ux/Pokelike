package com.runeveil.saga.ui.components

import androidx.annotation.StringRes
import com.runeveil.saga.R
import com.runeveil.saga.domain.rules.BattleReadiness

/**
 * Turns a refusal from :domain into something the player can read and act on.
 *
 * The mapping lives here rather than in the rule so that :domain stays free of
 * Android resources, and it is a `when` over the enum so that adding a reason
 * without adding a message will not compile.
 */
@get:StringRes
val BattleReadiness.Reason.messageRes: Int
    get() = when (this) {
        BattleReadiness.Reason.NO_MONSTERS -> R.string.battle_blocked_no_monsters
        BattleReadiness.Reason.ONLY_EGGS -> R.string.battle_blocked_only_eggs
        BattleReadiness.Reason.ALL_FAINTED -> R.string.battle_blocked_all_fainted
        BattleReadiness.Reason.UNKNOWN_OPPONENT -> R.string.battle_blocked_unknown_opponent
        BattleReadiness.Reason.OPPONENT_HAS_NO_MONSTERS -> R.string.battle_blocked_opponent_empty
    }
