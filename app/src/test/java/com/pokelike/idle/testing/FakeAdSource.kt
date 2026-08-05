package com.pokelike.idle.testing

import com.pokelike.idle.ads.AdResult
import com.pokelike.idle.ads.RewardedAdSource
import com.pokelike.idle.domain.model.RewardedAdPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Videoquelle mit vorgegebenem Ausgang.
 *
 * Anders als [com.pokelike.idle.ads.FakeRewardedAdSource] wuerfelt sie nicht:
 * Ein Test, der den Erfolgsfall prueft, darf nicht gelegentlich am simulierten
 * Netzfehler scheitern. Der Ausgang wird hier vorgegeben.
 *
 * @property result Ergebnis des naechsten [show].
 * @property loadSucceeds Ob [prepare] ein Video bereitstellt.
 */
class FakeAdSource(
    var result: AdResult = AdResult.EarnedReward,
    var loadSucceeds: Boolean = true,
) : RewardedAdSource {

    private val _readyPlacements = MutableStateFlow<Set<RewardedAdPlacement>>(emptySet())
    override val readyPlacements: StateFlow<Set<RewardedAdPlacement>> =
        _readyPlacements.asStateFlow()

    /** Zaehlt die Aufrufe, damit Tests das Nachladen pruefen koennen. */
    var prepareCount: Int = 0
        private set

    var showCount: Int = 0
        private set

    override suspend fun prepare(placement: RewardedAdPlacement) {
        prepareCount++
        if (loadSucceeds) _readyPlacements.value = _readyPlacements.value + placement
    }

    override suspend fun show(placement: RewardedAdPlacement): AdResult {
        showCount++
        if (placement !in _readyPlacements.value) return AdResult.NotReady

        // Ein gezeigtes Video ist verbraucht - wie beim echten SDK.
        _readyPlacements.value = _readyPlacements.value - placement
        return result
    }
}
