package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.ResourceType
import org.junit.Test
import kotlin.test.assertFailsWith

class CalculateOfflineProgressUseCaseTest {

    private val calculateOfflineProgress = CalculateOfflineProgressUseCase()

    @Test
    fun `rechnet Einkommen mal Zeit mal Effizienz`() {
        val result = calculateOfflineProgress(
            lastSeenAtMillis = NOW - ONE_HOUR,
            nowMillis = NOW,
            incomePerSecond = BigNumber.of(100),
            efficiency = 0.5,
        )

        // 3600 Sekunden * 100 pro Sekunde * 0.5 = 180.000
        assertThat(result.earned[ResourceType.COINS]).isEqualTo(BigNumber.of(180_000))
        assertThat(result.creditedMillis).isEqualTo(ONE_HOUR)
        assertThat(result.wasCapped).isFalse()
    }

    @Test
    fun `deckelt lange Abwesenheit`() {
        val threeDays = 72L * ONE_HOUR

        val result = calculateOfflineProgress(
            lastSeenAtMillis = NOW - threeDays,
            nowMillis = NOW,
            incomePerSecond = BigNumber.of(100),
            efficiency = 1.0,
            maxOfflineMillis = GameConfig.MAX_OFFLINE_MILLIS,
        )

        assertThat(result.creditedMillis).isEqualTo(GameConfig.MAX_OFFLINE_MILLIS)
        assertThat(result.elapsedMillis).isEqualTo(threeDays)
        assertThat(result.wasCapped).isTrue()

        // Acht Stunden zu 100 pro Sekunde bei voller Effizienz.
        assertThat(result.earned[ResourceType.COINS]).isEqualTo(BigNumber.of(2_880_000))
    }

    @Test
    fun `verweigert die Gutschrift bei vorgestellter Uhr`() {
        // Der Spielstand wurde bei vorgestellter Uhr gesichert; danach wurde
        // die Uhr zurueckgesetzt. Ohne diese Pruefung liesse sich der Ablauf
        // beliebig oft wiederholen, um Ertrag zu erzeugen.
        val result = calculateOfflineProgress(
            lastSeenAtMillis = NOW + 24L * ONE_HOUR,
            nowMillis = NOW,
            incomePerSecond = BigNumber.of(100),
        )

        assertThat(result.clockTamperingDetected).isTrue()
        assertThat(result.earned.isEmpty).isTrue()
        assertThat(result.isWorthShowing).isFalse()
    }

    @Test
    fun `toleriert kleine Zeitspruenge`() {
        // Zeitzonenwechsel, Sommerzeit und die Korrektur durch einen
        // Zeitserver bewegen die Uhr um Sekunden bis Minuten. Das darf nicht
        // als Manipulation gelten.
        val result = calculateOfflineProgress(
            lastSeenAtMillis = NOW + 60_000L,
            nowMillis = NOW,
            incomePerSecond = BigNumber.of(100),
        )

        assertThat(result.clockTamperingDetected).isFalse()
        assertThat(result.earned.isEmpty).isTrue()
    }

    @Test
    fun `uebergeht sehr kurze Unterbrechungen`() {
        val result = calculateOfflineProgress(
            lastSeenAtMillis = NOW - 30_000L,
            nowMillis = NOW,
            incomePerSecond = BigNumber.of(100),
        )

        // Ein Dialog fuer eine halbe Minute Abwesenheit waere reine Stoerung.
        assertThat(result).isEqualTo(
            com.pokelike.idle.domain.model.OfflineProgress.NONE,
        )
    }

    @Test
    fun `rechnet ohne Einkommen die Zeit trotzdem an`() {
        // Die Zeit wird gebraucht, auch wenn nichts anfaellt: Solange keine
        // Gebaeude gekauft sind, gibt es keinen Ertrag, aber der Spieler war
        // trotzdem weg.
        val result = calculateOfflineProgress(
            lastSeenAtMillis = NOW - ONE_HOUR,
            nowMillis = NOW,
            incomePerSecond = BigNumber.ZERO,
        )

        assertThat(result.creditedMillis).isEqualTo(ONE_HOUR)
        assertThat(result.earned.isEmpty).isTrue()
        assertThat(result.isWorthShowing).isFalse()
    }

    @Test
    fun `beruecksichtigt eine angehobene Effizienz`() {
        // Genau der Hebel, an dem ein Offline-Booster ansetzt.
        val normal = calculateOfflineProgress(
            lastSeenAtMillis = NOW - ONE_HOUR,
            nowMillis = NOW,
            incomePerSecond = BigNumber.of(100),
            efficiency = 0.5,
        )
        val boosted = calculateOfflineProgress(
            lastSeenAtMillis = NOW - ONE_HOUR,
            nowMillis = NOW,
            incomePerSecond = BigNumber.of(100),
            efficiency = 1.0,
        )

        assertThat(boosted.earned[ResourceType.COINS])
            .isEqualTo(normal.earned[ResourceType.COINS] * 2.0)
    }

    @Test
    fun `rechnet auch mit Ertraegen jenseits des Double-Bereichs`() {
        val result = calculateOfflineProgress(
            lastSeenAtMillis = NOW - ONE_HOUR,
            nowMillis = NOW,
            incomePerSecond = BigNumber.of(1.0, 400),
            efficiency = 1.0,
        )

        // 1e400 * 3600 = 3.6e403. Mit Double waere hier bereits Infinity.
        assertThat(result.earned[ResourceType.COINS].exponent).isEqualTo(403)
    }

    @Test
    fun `weist unsinnige Parameter ab`() {
        assertFailsWith<IllegalArgumentException> {
            calculateOfflineProgress(
                lastSeenAtMillis = NOW - ONE_HOUR,
                nowMillis = NOW,
                incomePerSecond = BigNumber.of(100),
                efficiency = -1.0,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            calculateOfflineProgress(
                lastSeenAtMillis = NOW - ONE_HOUR,
                nowMillis = NOW,
                incomePerSecond = BigNumber.of(100),
                maxOfflineMillis = -1L,
            )
        }
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val ONE_HOUR = 3_600_000L
    }
}
