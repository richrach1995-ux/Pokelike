package com.runeveil.saga.domain.battle

import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.monster.Ability
import com.runeveil.saga.domain.model.battle.Move

/**
 * Read-only content lookup the battle engine needs.
 *
 * Implemented in :data by the in-memory content cache. Keeping it as a narrow
 * interface (three lookups) means the engine can be tested with a handful of
 * literal moves and never touches a database.
 */
interface BattleContent {
    fun move(id: String): Move?
    fun ability(id: String): Ability?
    fun item(id: String): Item?

    /** Convenience: resolves the effect id of a monster's ability. */
    fun abilityEffectId(abilityId: String?): String? =
        abilityId?.let { ability(it)?.effectId }
}

/** Simple map-backed implementation, used by tests and by the tutorial fight. */
class MapBattleContent(
    private val moves: Map<String, Move> = emptyMap(),
    private val abilities: Map<String, Ability> = emptyMap(),
    private val items: Map<String, Item> = emptyMap(),
) : BattleContent {
    override fun move(id: String): Move? = moves[id]
    override fun ability(id: String): Ability? = abilities[id]
    override fun item(id: String): Item? = items[id]
}
