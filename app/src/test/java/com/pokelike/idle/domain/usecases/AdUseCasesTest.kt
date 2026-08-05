package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.domain.model.AdState
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.RewardedAdPlacement
import org.junit.Test

private const val NOW = 1_700_000_000_000L
private val placement = RewardedAdPlacement.BOOSTER_REWARD

class AdStateTest {

    @Test
    fun `ist ohne Eintrag sofort bereit`() {
        assertThat(AdState.EMPTY.isReadyAt(placement, NOW)).isTrue()
    }

    @Test
    fun `sperrt fuer die Dauer der Wartezeit`() {
        val state = AdState.EMPTY.withCooldownStarted(placement, NOW)

        assertThat(state.isReadyAt(placement, NOW)).isFalse()
        assertThat(state.remainingAt(placement, NOW)).isEqualTo(placement.cooldownMillis)
    }

    @Test
    fun `gibt nach Ablauf der Wartezeit wieder frei`() {
        val state = AdState.EMPTY.withCooldownStarted(placement, NOW)

        assertThat(state.isReadyAt(placement, NOW + placement.cooldownMillis)).isTrue()
    }

    @Test
    fun `sperrt nicht laenger als die Wartezeit`() {
        // Nach einem Zeitzonenwechsel oder einer Uhrkorrektur wuerde der
        // Spieler sonst ohne erkennbaren Grund ausgesperrt.
        val state = AdState.EMPTY.withCooldownStarted(placement, NOW)

        val remaining = state.remainingAt(placement, NOW - 365L * 24L * 60L * 60L * 1_000L)

        assertThat(remaining).isEqualTo(placement.cooldownMillis)
    }

    @Test
    fun `entfernt abgelaufene Wartezeiten`() {
        val state = AdState.EMPTY.withCooldownStarted(placement, NOW)

        val pruned = state.pruned(NOW + placement.cooldownMillis)

        assertThat(pruned.isEmpty).isTrue()
    }

    @Test
    fun `liefert dasselbe Objekt, wenn nichts abgelaufen ist`() {
        // Der Vergleich im RewardedAdManager entscheidet daran, ob der
        // Spielstand als geaendert gilt.
        val state = AdState.EMPTY.withCooldownStarted(placement, NOW)

        assertThat(state.pruned(NOW + 1_000L)).isSameInstanceAs(state)
    }
}

class GrantAdRewardUseCaseTest {

    private val grant = GrantAdRewardUseCase(StartBoosterUseCase())

    @Test
    fun `startet den Booster und die Wartezeit`() {
        val result = grant(GameState.newGame(0L), placement, NOW)

        assertThat(result).isInstanceOf(AdRewardResult.Success::class.java)
        val success = result as AdRewardResult.Success
        assertThat(success.state.boosters[placement.rewardedBooster]).isNotNull()
        assertThat(success.state.ads.isReadyAt(placement, NOW)).isFalse()
    }

    @Test
    fun `kostet nichts`() {
        // Der Unterschied zum Kauf: Das Video ist der Weg fuer Spieler, die
        // nicht zahlen. Ein Abzug waere ein Fehler mit Ansage.
        val state = GameState.newGame(0L)
        val before = state.resources

        val result = grant(state, placement, NOW) as AdRewardResult.Success

        assertThat(result.state.resources).isEqualTo(before)
    }

    @Test
    fun `verweigert die zweite Belohnung waehrend der Wartezeit`() {
        val first = grant(GameState.newGame(0L), placement, NOW) as AdRewardResult.Success

        val second = grant(first.state, placement, NOW + 1_000L)

        assertThat(second).isEqualTo(AdRewardResult.OnCooldown)
    }

    @Test
    fun `belohnt nach Ablauf der Wartezeit erneut`() {
        val first = grant(GameState.newGame(0L), placement, NOW) as AdRewardResult.Success

        val second = grant(first.state, placement, NOW + placement.cooldownMillis)

        assertThat(second).isInstanceOf(AdRewardResult.Success::class.java)
    }

    @Test
    fun `verlaengert einen bereits laufenden Booster`() {
        val duration = placement.rewardedBooster.durationMillis
        val first = grant(GameState.newGame(0L), placement, NOW) as AdRewardResult.Success

        val later = NOW + placement.cooldownMillis
        val second = grant(first.state, placement, later) as AdRewardResult.Success

        assertThat(second.state.boosters[placement.rewardedBooster]?.remainingAt(later))
            .isEqualTo(2L * duration - placement.cooldownMillis)
    }

    @Test
    fun `ueberlebt den Prestige-Reset`() {
        // Waere die Wartezeit nach einem Prestige weg, waere der Reset der Weg,
        // sie zu umgehen.
        val granted = (
            grant(GameState.newGame(0L), placement, NOW) as AdRewardResult.Success
            ).state

        val afterPrestige = granted.afterPrestige(BigNumber.of(5))

        assertThat(afterPrestige.ads).isEqualTo(granted.ads)
    }
}

class RewardedAdPlacementTest {

    @Test
    fun `haelt die Schluessel eindeutig`() {
        assertThat(RewardedAdPlacement.entries.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `gibt jeder Stelle eine Wartezeit`() {
        // Ohne Wartezeit koennte sich ein Spieler unbegrenzt Booster ansehen,
        // und weder Diamanten noch Kaeufe haetten noch einen Zweck.
        RewardedAdPlacement.entries.forEach { placement ->
            assertThat(placement.cooldownMillis).isGreaterThan(0L)
        }
    }

    @Test
    fun `belohnt nicht den staerksten Booster`() {
        // Waere das Video der beste Weg zum besten Booster, haette der
        // Diamantenpreis daneben keinen Sinn mehr.
        val strongest = BoosterType.entries
            .maxBy { it.price.amounts.values.first().toDouble() }

        RewardedAdPlacement.entries.forEach { placement ->
            assertThat(placement.rewardedBooster).isNotEqualTo(strongest)
        }
    }
}
