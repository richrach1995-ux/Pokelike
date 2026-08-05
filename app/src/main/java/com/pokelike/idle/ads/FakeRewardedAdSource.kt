package com.pokelike.idle.ads

import com.pokelike.idle.domain.model.RewardedAdPlacement
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.GameLogger
import com.pokelike.idle.util.RandomProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Belohnungsvideos ohne Werbe-SDK.
 *
 * Wird im Debug-Build verwendet und ist damit die einzige Umsetzung, mit der
 * das Spiel bis zur Anbindung des echten SDK ueberhaupt laeuft. Sie ist kein
 * Platzhalter: Sie bildet die Eigenschaften nach, an denen die Anzeige
 * tatsaechlich haengt.
 *
 * - **Laden dauert.** Ein Fake, der sofort bereit ist, wuerde den Zustand
 *   "laedt noch" nie zeigen, und der zugehoerige Anzeigepfad bliebe
 *   ungetestet, bis er beim echten SDK erstmals auftritt.
 * - **Laden schlaegt gelegentlich fehl.** Kein Netz und leere Werbebestaende
 *   sind der Normalfall, nicht die Ausnahme; die Oberflaeche muss ihn
 *   beherrschen.
 * - **Der Spieler kann abbrechen.** Auch das ist ein regulaerer Ausgang.
 *
 * Die Anteile stehen als Konstanten und sind bewusst niedrig gewaehlt: Beim
 * Entwickeln soll der uebliche Weg funktionieren, der seltene Fall aber
 * regelmaessig genug auftreten, um ihn zu bemerken.
 */
@Singleton
class FakeRewardedAdSource @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val random: RandomProvider,
    private val logger: GameLogger,
) : RewardedAdSource {

    private val _readyPlacements = MutableStateFlow<Set<RewardedAdPlacement>>(emptySet())
    override val readyPlacements: StateFlow<Set<RewardedAdPlacement>> =
        _readyPlacements.asStateFlow()

    override suspend fun prepare(placement: RewardedAdPlacement) {
        if (placement in _readyPlacements.value) return

        withContext(dispatchers.io) {
            delay(LOAD_DURATION_MS)

            if (random.rollChance(LOAD_FAILURE_CHANCE)) {
                logger.warn(TAG, "Laden fehlgeschlagen (simuliert): ${placement.id}")
                return@withContext
            }

            _readyPlacements.update { it + placement }
        }
    }

    override suspend fun show(placement: RewardedAdPlacement): AdResult =
        withContext(dispatchers.io) {
            if (placement !in _readyPlacements.value) return@withContext AdResult.NotReady

            delay(PLAYBACK_DURATION_MS)

            // Ein gezeigtes Video ist verbraucht - unabhaengig davon, wie es
            // ausgegangen ist. Genau so verhaelt sich auch das echte SDK, und
            // ein Fake, der das Video liegen liesse, wuerde den Nachladepfad
            // verdecken.
            _readyPlacements.update { it - placement }

            if (random.rollChance(DISMISS_CHANCE)) {
                AdResult.Dismissed
            } else {
                AdResult.EarnedReward
            }
        }

    private companion object {
        const val TAG = "FakeRewardedAds"

        /** Ladedauer eines Videos. Entspricht ungefaehr einem echten Abruf. */
        const val LOAD_DURATION_MS = 1_500L

        /** Spieldauer eines Videos. */
        const val PLAYBACK_DURATION_MS = 3_000L

        const val LOAD_FAILURE_CHANCE = 0.1
        const val DISMISS_CHANCE = 0.1
    }
}
