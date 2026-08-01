package com.runeveil.saga.domain.model.item

import com.runeveil.saga.domain.model.world.UnlockRequirement

/**
 * A crafting recipe used at a Zwergenschmiede (dwarven forge) or a Seidr Loom.
 *
 * @property station which workstation is required.
 * @property ingredients item id → amount.
 * @property successChance 1.0 for ordinary recipes; legendary recipes can fail
 *   and consume half the ingredients (the risk is what makes the Svartalfheim
 *   endgame loop tense).
 */
data class Recipe(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val station: CraftingStation,
    val ingredients: Map<String, Int>,
    val goldCost: Int = 0,
    val resultItemId: String,
    val resultAmount: Int = 1,
    val successChance: Double = 1.0,
    val unlockRequirement: UnlockRequirement = UnlockRequirement.None,
    val tier: Int = 1,
    val craftTimeSeconds: Int = 0,
    val byproductItemId: String? = null,
    val byproductChance: Double = 0.0,
) {
    val canFail: Boolean get() = successChance < 1.0
}

enum class CraftingStation(val displayKey: String, val iconKey: String) {
    FORGE("station_forge", "ic_station_forge"),
    ALCHEMY_TABLE("station_alchemy", "ic_station_alchemy"),
    RUNE_LOOM("station_loom", "ic_station_loom"),
    ALTAR("station_altar", "ic_station_altar"),
}

/**
 * One stack in the player's satchel.
 *
 * The inventory itself is a plain list of these; quantity caps are enforced by
 * `InventoryRules`.
 */
data class InventoryStack(
    val itemId: String,
    val quantity: Int,
) {
    fun withDelta(delta: Int): InventoryStack = copy(quantity = (quantity + delta).coerceAtLeast(0))
    val isEmpty: Boolean get() = quantity <= 0
}
