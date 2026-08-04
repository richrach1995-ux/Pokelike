package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.config.GameConfig
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.GameStatistics
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourcePool
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.model.UpgradeInventory
import com.pokelike.idle.domain.model.UpgradeType
import org.junit.Test

class CalculatePrestigeUseCaseTest {

    private val calculatePrestige = CalculatePrestigeUseCase()

    /** Spielstand mit einer bestimmten Lebenssumme an Muenzen. */
    private fun stateWithLifetime(coins: BigNumber): GameState =
        GameState.newGame(nowMillis = 0L).copy(
            statistics = GameStatistics(
                lifetimeEarned = ResourceBundle.single(ResourceType.COINS, coins),
            ),
        )

    @Test
    fun `liefert ohne Fortschritt keine Punkte`() {
        val info = calculatePrestige(GameState.newGame(0L))

        assertThat(info.pointsOnReset).isEqualTo(BigNumber.ZERO)
        assertThat(info.canPrestige).isFalse()
    }

    @Test
    fun `gibt den ersten Punkt bei Erreichen der Basis`() {
        val justBelow = calculatePrestige(stateWithLifetime(BigNumber.of(9.9e9)))
        val atBase = calculatePrestige(
            stateWithLifetime(BigNumber.of(GameConfig.PRESTIGE_BASE_COINS)),
        )

        assertThat(justBelow.pointsOnReset).isEqualTo(BigNumber.ZERO)
        assertThat(atBase.pointsOnReset).isEqualTo(BigNumber.ONE)
        assertThat(atBase.canPrestige).isTrue()
    }

    @Test
    fun `folgt der Wurzelformel`() {
        // Der vierfache Ertrag bringt genau doppelt so viele Punkte, der
        // hundertfache genau zehnmal so viele. Genau diese Daempfung soll
        // verhindern, dass ein einziger langer Durchlauf alles dominiert.
        val base = GameConfig.PRESTIGE_BASE_COINS

        assertThat(calculatePrestige(stateWithLifetime(BigNumber.of(base * 4))).pointsOnReset)
            .isEqualTo(BigNumber.of(2))
        assertThat(calculatePrestige(stateWithLifetime(BigNumber.of(base * 100))).pointsOnReset)
            .isEqualTo(BigNumber.of(10))
        assertThat(calculatePrestige(stateWithLifetime(BigNumber.of(base * 10_000))).pointsOnReset)
            .isEqualTo(BigNumber.of(100))
    }

    @Test
    fun `rundet Bruchteile ab`() {
        // Wurzel aus 90 ist rund 9,49 - das ergibt neun Punkte, nicht zehn.
        val info = calculatePrestige(
            stateWithLifetime(BigNumber.of(GameConfig.PRESTIGE_BASE_COINS * 90)),
        )

        assertThat(info.pointsOnReset).isEqualTo(BigNumber.of(9))
    }

    @Test
    fun `zieht bereits erhaltene Punkte ab`() {
        // Massgeblich ist die Lebenssumme der Punkte, nicht der Kontostand.
        // Sobald sich Punkte ausgeben lassen, wuerde der Kontostand sinken und
        // der Spieler bekaeme dieselben Punkte ein zweites Mal.
        val state = GameState.newGame(0L).copy(
            resources = ResourcePool.of(ResourceType.PRESTIGE_POINTS to BigNumber.of(3)),
            statistics = GameStatistics(
                lifetimeEarned = ResourceBundle.of(
                    ResourceType.COINS to BigNumber.of(GameConfig.PRESTIGE_BASE_COINS * 100),
                    ResourceType.PRESTIGE_POINTS to BigNumber.of(6),
                ),
            ),
        )

        val info = calculatePrestige(state)

        // Zehn Punkte insgesamt, sechs davon bereits erhalten.
        assertThat(info.pointsOnReset).isEqualTo(BigNumber.of(4))
        assertThat(info.currentPoints).isEqualTo(BigNumber.of(3))
    }

    @Test
    fun `liefert nie negative Punkte`() {
        // Kann auftreten, wenn Balancing-Werte nachtraeglich angehoben werden:
        // Der Spieler hat dann mehr Punkte, als seine Lebenssumme heute wert
        // waere. Ein negativer Wert wuerde die Anzeige unbrauchbar machen.
        val state = GameState.newGame(0L).copy(
            statistics = GameStatistics(
                lifetimeEarned = ResourceBundle.of(
                    ResourceType.COINS to BigNumber.of(GameConfig.PRESTIGE_BASE_COINS),
                    ResourceType.PRESTIGE_POINTS to BigNumber.of(50),
                ),
            ),
        )

        assertThat(calculatePrestige(state).pointsOnReset).isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `berechnet den Bonus additiv`() {
        // Hundert Punkte ergeben den dreifachen Ertrag, nicht das
        // Zweihoch-Hundertfache.
        assertThat(calculatePrestige.bonusFor(BigNumber.ZERO)).isEqualTo(1.0)
        assertThat(calculatePrestige.bonusFor(BigNumber.of(50)))
            .isWithin(TOLERANCE).of(2.0)
        assertThat(calculatePrestige.bonusFor(BigNumber.of(100)))
            .isWithin(TOLERANCE).of(3.0)
    }

    @Test
    fun `haelt den Bonus auch bei unsinnig hohen Punktzahlen endlich`() {
        // Ein Infinity an dieser Stelle wuerde jede nachfolgende Rechnung
        // unbrauchbar machen.
        val bonus = calculatePrestige.bonusFor(BigNumber.of(1.0, 400))

        assertThat(bonus).isFinite()
        assertThat(bonus).isGreaterThan(1.0)
    }

    @Test
    fun `zeigt den Bonus nach dem Reset an`() {
        val info = calculatePrestige(
            stateWithLifetime(BigNumber.of(GameConfig.PRESTIGE_BASE_COINS * 100)),
        )

        // Ohne Punkte jetzt, zehn danach: aus +0 % werden +20 %.
        assertThat(info.currentBonus).isWithin(TOLERANCE).of(1.0)
        assertThat(info.bonusAfterReset).isWithin(TOLERANCE).of(1.20)
    }

    @Test
    fun `nennt die Schwelle fuer den naechsten Punkt`() {
        val base = GameConfig.PRESTIGE_BASE_COINS
        val info = calculatePrestige(stateWithLifetime(BigNumber.of(base * 4)))

        // Zwei Punkte erreicht, der dritte liegt bei 9 * Basis.
        assertThat(info.coinsForNextPoint).isEqualTo(BigNumber.of(base * 9))
    }

    @Test
    fun `zeigt den Fortschritt logarithmisch`() {
        // Linear stuende der Balken im spaeten Spiel ueber Stunden bei nahezu
        // null: Zwischen zwei Punktschwellen liegen dort Groessenordnungen.
        val base = GameConfig.PRESTIGE_BASE_COINS

        val atThreshold = calculatePrestige(stateWithLifetime(BigNumber.of(base)))
        val midway = calculatePrestige(stateWithLifetime(BigNumber.of(base * 2)))

        assertThat(atThreshold.progressToNextPoint).isWithin(FLOAT_TOLERANCE).of(0f)
        // Geometrische Mitte zwischen 1x und 4x der Basis.
        assertThat(midway.progressToNextPoint).isWithin(FLOAT_TOLERANCE).of(0.5f)
    }

    @Test
    fun `rechnet auch mit Lebenssummen jenseits des Double-Bereichs`() {
        val info = calculatePrestige(stateWithLifetime(BigNumber.of(1.0, 400)))

        // Wurzel aus 1e390 ist 1e195.
        assertThat(info.pointsOnReset.exponent).isEqualTo(195)
    }

    private companion object {
        const val TOLERANCE = 1e-9
        const val FLOAT_TOLERANCE = 1e-4f
    }
}

class PerformPrestigeUseCaseTest {

    private val calculatePrestige = CalculatePrestigeUseCase()
    private val performPrestige = PerformPrestigeUseCase(calculatePrestige)

    private fun readyState(): GameState = GameState.newGame(nowMillis = 0L).copy(
        resources = ResourcePool.of(
            ResourceType.COINS to BigNumber.of(1.0, 12),
            ResourceType.DIAMONDS to BigNumber.of(500),
        ),
        buildings = BuildingInventory.of(
            BuildingType.FINGER to 100,
            BuildingType.MINE to 50,
        ),
        upgrades = UpgradeInventory.of(UpgradeType.STRONGER_FINGERS),
        statistics = GameStatistics(
            totalClicks = 50_000L,
            lifetimeEarned = ResourceBundle.single(
                ResourceType.COINS,
                BigNumber.of(GameConfig.PRESTIGE_BASE_COINS * 100),
            ),
        ),
    )

    @Test
    fun `schreibt die Punkte gut und leert den Ausbau`() {
        val result = performPrestige(readyState())

        assertThat(result).isInstanceOf(PrestigeResult.Success::class.java)
        val success = result as PrestigeResult.Success

        assertThat(success.pointsEarned).isEqualTo(BigNumber.of(10))
        assertThat(success.state[ResourceType.PRESTIGE_POINTS]).isEqualTo(BigNumber.of(10))
        assertThat(success.state.buildings.isEmpty).isTrue()
        assertThat(success.state[ResourceType.COINS]).isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `behaelt Upgrades, Diamanten und Statistik`() {
        val success = performPrestige(readyState()) as PrestigeResult.Success

        // Upgrades sind der Grund, warum ein Neuanfang schneller laeuft.
        assertThat(UpgradeType.STRONGER_FINGERS in success.state.upgrades).isTrue()
        // Gekaufte Premiumwaehrung zurueckzusetzen waere ein Erstattungsfall.
        assertThat(success.state[ResourceType.DIAMONDS]).isEqualTo(BigNumber.of(500))
        // Ohne die Statistik verloere der Spieler seine Achievements.
        assertThat(success.state.statistics.totalClicks).isEqualTo(50_000L)
        assertThat(success.state.statistics.prestigeCount).isEqualTo(1)
    }

    @Test
    fun `verweigert den Reset unterhalb der Mindestpunktzahl`() {
        val notReady = GameState.newGame(0L).copy(
            buildings = BuildingInventory.of(BuildingType.FINGER to 100),
            statistics = GameStatistics(
                lifetimeEarned = ResourceBundle.single(
                    ResourceType.COINS,
                    BigNumber.of(GameConfig.PRESTIGE_BASE_COINS / 2),
                ),
            ),
        )

        assertThat(performPrestige(notReady)).isEqualTo(PrestigeResult.NotEnoughPoints)
    }

    @Test
    fun `bringt beim zweiten Reset ohne neuen Fortschritt nichts`() {
        // Die Lebenssumme bleibt nach dem Reset erhalten. Ohne den Abzug der
        // bereits erhaltenen Punkte koennte der Spieler beliebig oft
        // zuruecksetzen und jedes Mal dieselben Punkte kassieren.
        val afterFirst = (performPrestige(readyState()) as PrestigeResult.Success).state

        assertThat(performPrestige(afterFirst)).isEqualTo(PrestigeResult.NotEnoughPoints)
    }

    @Test
    fun `sammelt Punkte ueber mehrere Durchlaeufe an`() {
        val afterFirst = (performPrestige(readyState()) as PrestigeResult.Success).state

        // Weiterer Fortschritt: insgesamt das Vierhundertfache der Basis,
        // also zwanzig Punkte - zehn davon bereits erhalten.
        val grown = afterFirst.copy(
            statistics = afterFirst.statistics.copy(
                lifetimeEarned = afterFirst.statistics.lifetimeEarned + ResourceBundle.single(
                    ResourceType.COINS,
                    BigNumber.of(GameConfig.PRESTIGE_BASE_COINS * 300),
                ),
            ),
        )

        val success = performPrestige(grown) as PrestigeResult.Success

        assertThat(success.pointsEarned).isEqualTo(BigNumber.of(10))
        assertThat(success.state[ResourceType.PRESTIGE_POINTS]).isEqualTo(BigNumber.of(20))
        assertThat(success.state.statistics.prestigeCount).isEqualTo(2)
    }
}
