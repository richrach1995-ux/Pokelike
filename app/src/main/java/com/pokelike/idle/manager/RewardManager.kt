package com.pokelike.idle.manager

import com.pokelike.idle.domain.model.PendingReward
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sammelt Belohnungsmeldungen aus allen Quellen.
 *
 * Ohne diese Stelle braechte jede Quelle ihr eigenes Popup mit: Achievements,
 * Quests, Login-Bonus, Events und Battle Pass. Ein Spieler, der nach einer
 * laengeren Pause zurueckkehrt, muesste sich dann durch ein halbes Dutzend
 * Dialoge tippen, bevor er das Spiel sieht. Hier laufen alle zusammen und
 * werden als eine Meldung gezeigt.
 *
 * Die Belohnungen sind zum Zeitpunkt der Meldung bereits gutgeschrieben -
 * siehe [PendingReward]. Diese Klasse verwaltet ausschliesslich die Anzeige.
 *
 * Prozessweit und nicht im ViewModel, weil eine Belohnung waehrend eines
 * Bildschirmwechsels anfallen kann und dann verloren ginge.
 */
@Singleton
class RewardManager @Inject constructor() {

    private val _pending = MutableStateFlow<List<PendingReward>>(emptyList())

    /** Anliegende Meldungen in der Reihenfolge ihres Eintreffens. */
    val pending: StateFlow<List<PendingReward>> = _pending.asStateFlow()

    /**
     * Meldet eine Belohnung zur Anzeige an.
     *
     * Bereits angemeldete Belohnungen werden uebergangen. Die
     * Achievement-Pruefung laeuft im Sekundentakt; ohne diese Absicherung
     * stuende dieselbe Meldung nach wenigen Sekunden dutzendfach in der Liste.
     */
    fun offer(reward: PendingReward) {
        _pending.update { current ->
            if (current.any { it.id == reward.id }) current else current + reward
        }
    }

    /** Meldet mehrere Belohnungen auf einmal an. */
    fun offerAll(rewards: List<PendingReward>) {
        if (rewards.isEmpty()) return
        rewards.forEach(::offer)
    }

    /** Bestaetigt, dass alle Meldungen gezeigt wurden. */
    fun consumeAll() {
        _pending.value = emptyList()
    }
}
