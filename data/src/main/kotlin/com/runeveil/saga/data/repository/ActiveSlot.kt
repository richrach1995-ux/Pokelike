package com.runeveil.saga.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which save slot the game is currently playing.
 *
 * Every repository partitions its tables by this value, so switching slots is
 * a single assignment — no cache invalidation, no reloading, and the Flows
 * driven by [slot] re-query automatically.
 */
@Singleton
class ActiveSlot @Inject constructor() {

    private val _slot = MutableStateFlow(DEFAULT_SLOT)

    val slot: StateFlow<Int> = _slot.asStateFlow()

    val current: Int get() = _slot.value

    fun set(slotIndex: Int) {
        _slot.value = slotIndex.coerceIn(0, MAX_SLOT)
    }

    companion object {
        const val DEFAULT_SLOT = 0
        const val MAX_SLOT = 3
    }
}
