package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.monster.MonsterInstance

/**
 * Answers one question before anything is navigated to, allocated or played:
 * *may this battle start at all?*
 *
 * A battle that cannot be won, cannot be fought, or has no opponent used to be
 * entered anyway and then resolved into an instant defeat. That is the worst of
 * both worlds — the player loses progress to a situation the game could see
 * coming. The check therefore runs at the call site (the world screen, the
 * trainer, the story trigger), and the battle screen is never opened when it
 * comes back [Blocked].
 *
 * The rule is pure and lives in :domain so every entry point shares one
 * definition of "ready" and it can be tested without a UI.
 */
sealed interface BattleReadiness {

    /** Every precondition holds; the battle may be opened. */
    data object Ready : BattleReadiness

    /** The battle must not start. [reason] is mapped to a message by the UI. */
    data class Blocked(val reason: Reason) : BattleReadiness

    /**
     * Why a battle cannot start. Ordered from "the player's fault and fixable"
     * to "the content is wrong", because the first blocked reason found is the
     * one shown and the fixable ones are the more useful message.
     */
    enum class Reason {
        /** Nothing in the party at all — before the starter is chosen. */
        NO_MONSTERS,

        /** Everything carried is still an egg. */
        ONLY_EGGS,

        /** The whole party is knocked out; the player must heal first. */
        ALL_FAINTED,

        /** The encounter or trainer id does not resolve to any content. */
        UNKNOWN_OPPONENT,

        /** The opponent resolved, but its team is empty. */
        OPPONENT_HAS_NO_MONSTERS,
    }

    val isReady: Boolean get() = this is Ready
}

object BattleReadinessRules {

    /**
     * Checks the player's side only. Used where the opponent is not resolved
     * yet — a random encounter roll, for instance, is skipped entirely when the
     * party cannot fight, so no monster is even generated.
     */
    fun forParty(party: List<MonsterInstance>): BattleReadiness {
        if (party.isEmpty()) return BattleReadiness.Blocked(BattleReadiness.Reason.NO_MONSTERS)

        val fighters = party.filter { !it.isEgg }
        if (fighters.isEmpty()) return BattleReadiness.Blocked(BattleReadiness.Reason.ONLY_EGGS)

        if (fighters.none { !it.isFainted }) {
            return BattleReadiness.Blocked(BattleReadiness.Reason.ALL_FAINTED)
        }
        return BattleReadiness.Ready
    }

    /**
     * The full check.
     *
     * @param opponents the resolved enemy team, or `null` when the encounter id
     *   could not be resolved to content at all.
     */
    fun check(party: List<MonsterInstance>, opponents: List<MonsterInstance>?): BattleReadiness {
        val playerSide = forParty(party)
        if (playerSide is BattleReadiness.Blocked) return playerSide

        if (opponents == null) {
            return BattleReadiness.Blocked(BattleReadiness.Reason.UNKNOWN_OPPONENT)
        }
        if (opponents.isEmpty()) {
            return BattleReadiness.Blocked(BattleReadiness.Reason.OPPONENT_HAS_NO_MONSTERS)
        }
        return BattleReadiness.Ready
    }

    /** Convenience for call sites that only need a yes/no. */
    fun canFight(party: List<MonsterInstance>): Boolean = forParty(party).isReady
}
