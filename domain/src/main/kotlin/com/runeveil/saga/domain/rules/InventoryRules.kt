package com.runeveil.saga.domain.rules

import com.runeveil.saga.domain.model.item.InventoryStack
import com.runeveil.saga.domain.model.item.Item
import com.runeveil.saga.domain.model.item.Recipe
import com.runeveil.saga.domain.util.Rng
import kotlin.math.roundToInt

/** Satchel arithmetic: stacking, capacity and shop prices. */
object InventoryRules {

    /** Distinct item kinds the satchel can hold. */
    const val MAX_DISTINCT_STACKS = 250

    /** Adds [amount] of [item], splitting across stacks when the cap is hit. */
    fun add(stacks: List<InventoryStack>, item: Item, amount: Int): List<InventoryStack> {
        if (amount <= 0) return stacks
        val result = stacks.toMutableList()
        var remaining = amount
        // Fill existing stacks first.
        for (index in result.indices) {
            if (remaining <= 0) break
            val stack = result[index]
            if (stack.itemId != item.id) continue
            val room = item.stackLimit - stack.quantity
            if (room <= 0) continue
            val applied = minOf(room, remaining)
            result[index] = stack.withDelta(applied)
            remaining -= applied
        }
        // Then open new ones.
        while (remaining > 0 && result.size < MAX_DISTINCT_STACKS) {
            val applied = minOf(item.stackLimit, remaining)
            result += InventoryStack(item.id, applied)
            remaining -= applied
        }
        return result
    }

    /** Removes [amount]; returns null when the satchel does not hold enough. */
    fun remove(stacks: List<InventoryStack>, itemId: String, amount: Int): List<InventoryStack>? {
        if (amount <= 0) return stacks
        if (countOf(stacks, itemId) < amount) return null
        val result = stacks.toMutableList()
        var remaining = amount
        for (index in result.indices) {
            if (remaining <= 0) break
            val stack = result[index]
            if (stack.itemId != itemId) continue
            val applied = minOf(stack.quantity, remaining)
            result[index] = stack.withDelta(-applied)
            remaining -= applied
        }
        return result.filterNot { it.isEmpty }
    }

    fun countOf(stacks: List<InventoryStack>, itemId: String): Int =
        stacks.filter { it.itemId == itemId }.sumOf { it.quantity }

    /** Purchase price including regional markup and faction discount. */
    fun buyPrice(item: Item, markup: Double, discountPercent: Int): Int {
        if (item.price <= 0) return 0
        val discounted = item.price * markup * (1.0 - discountPercent / 100.0)
        return discounted.roundToInt().coerceAtLeast(1)
    }

    /** Sale price. Selling is deliberately unprofitable versus crafting. */
    fun sellPrice(item: Item, hasMerchantTitle: Boolean): Int {
        val base = item.sellValue
        return if (hasMerchantTitle) (base * 1.25).roundToInt() else base
    }

    /** Whether the satchel holds every ingredient of [recipe]. */
    fun canCraft(stacks: List<InventoryStack>, recipe: Recipe, gold: Int): Boolean =
        gold >= recipe.goldCost &&
            recipe.ingredients.all { (itemId, amount) -> countOf(stacks, itemId) >= amount }

    /** Result of one crafting attempt. */
    data class CraftResult(
        val success: Boolean,
        val stacks: List<InventoryStack>,
        val producedItemId: String?,
        val producedAmount: Int,
        val byproductItemId: String? = null,
    )

    /**
     * Executes a craft. A failed legendary craft consumes half of every
     * ingredient (rounded up), which is the intended risk.
     */
    fun craft(
        stacks: List<InventoryStack>,
        recipe: Recipe,
        resultItem: Item,
        byproductItem: Item?,
        rng: Rng,
    ): CraftResult {
        var working = stacks
        val succeeded = !recipe.canFail || rng.chance(recipe.successChance)

        for ((itemId, amount) in recipe.ingredients) {
            val consumed = if (succeeded) amount else (amount + 1) / 2
            working = remove(working, itemId, consumed) ?: return CraftResult(false, stacks, null, 0)
        }
        if (!succeeded) {
            return CraftResult(false, working, null, 0)
        }
        working = add(working, resultItem, recipe.resultAmount)
        var byproduct: String? = null
        if (byproductItem != null && rng.chance(recipe.byproductChance)) {
            working = add(working, byproductItem, 1)
            byproduct = byproductItem.id
        }
        return CraftResult(true, working, resultItem.id, recipe.resultAmount, byproduct)
    }
}
