package com.pokelike.idle.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.pokelike.idle.domain.model.AchievementInventory
import com.pokelike.idle.domain.model.AchievementType
import com.pokelike.idle.domain.model.BigNumber
import com.pokelike.idle.domain.model.BuildingInventory
import com.pokelike.idle.domain.model.BuildingType
import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.GameStatistics
import com.pokelike.idle.domain.model.QuestPeriod
import com.pokelike.idle.domain.model.QuestType
import com.pokelike.idle.domain.model.ResourceBundle
import com.pokelike.idle.domain.model.ResourceType
import org.junit.Test
import java.time.ZoneId

class CheckAchievementsUseCaseTest {

    private val checkAchievements = CheckAchievementsUseCase()

    @Test
    fun `schaltet erfuellte Achievements frei`() {
        val state = GameState.newGame(0L).copy(
            statistics = GameStatistics(totalClicks = 1L),
        )

        val result = checkAchievements(state)

        assertThat(result.newlyUnlocked).contains(AchievementType.FIRST_CLICK)
        assertThat(AchievementType.FIRST_CLICK in result.state.achievements).isTrue()
    }

    @Test
    fun `schreibt die Belohnung sofort gut`() {
        // Belohnung und Freischaltung in einem Schritt: Waeren sie getrennt,
        // ginge die Belohnung verloren, sobald die App dazwischen endet.
        val state = GameState.newGame(0L).copy(
            statistics = GameStatistics(totalClicks = 1L),
        )
        val before = state[ResourceType.DIAMONDS]

        val result = checkAchievements(state)

        assertThat(result.state[ResourceType.DIAMONDS])
            .isEqualTo(before + AchievementType.FIRST_CLICK.reward[ResourceType.DIAMONDS])
    }

    @Test
    fun `belohnt kein Achievement ein zweites Mal`() {
        // Die Pruefung laeuft im Sekundentakt. Ohne diese Absicherung wuerde
        // dieselbe Belohnung jede Sekunde erneut ausgeschuettet.
        val state = GameState.newGame(0L).copy(
            statistics = GameStatistics(totalClicks = 1L),
        )

        val first = checkAchievements(state)
        val second = checkAchievements(first.state)

        assertThat(second.newlyUnlocked).isEmpty()
        assertThat(second.state[ResourceType.DIAMONDS])
            .isEqualTo(first.state[ResourceType.DIAMONDS])
    }

    @Test
    fun `laesst den Zustand unveraendert, wenn nichts faellig ist`() {
        val state = GameState.newGame(0L)

        val result = checkAchievements(state)

        assertThat(result.newlyUnlocked).isEmpty()
        assertThat(result.state).isEqualTo(state)
    }

    @Test
    fun `schaltet mehrere Achievements in einem Durchgang frei`() {
        // Tritt nach einer laengeren Abwesenheit auf: Der Offline-Ertrag kann
        // gleich mehrere Schwellen ueberspringen.
        val state = GameState.newGame(0L).copy(
            statistics = GameStatistics(
                totalClicks = 20_000L,
                lifetimeEarned = ResourceBundle.single(
                    ResourceType.COINS,
                    BigNumber.of(1.0, 13),
                ),
            ),
        )

        val result = checkAchievements(state)

        assertThat(result.newlyUnlocked).containsAtLeast(
            AchievementType.FIRST_CLICK,
            AchievementType.HUNDRED_CLICKS,
            AchievementType.THOUSAND_CLICKS,
            AchievementType.TEN_THOUSAND_CLICKS,
            AchievementType.MILLION_COINS,
            AchievementType.TRILLION_COINS,
        )
    }

    @Test
    fun `erkennt das Achievement fuer alle Gebaeudearten`() {
        val allTypes = GameState.newGame(0L).copy(
            buildings = BuildingInventory.of(
                BuildingType.entries.associateWith { 1 },
            ),
        )

        assertThat(checkAchievements(allTypes).newlyUnlocked)
            .contains(AchievementType.ALL_BUILDING_TYPES)
        assertThat(checkAchievements(GameState.newGame(0L)).newlyUnlocked)
            .doesNotContain(AchievementType.ALL_BUILDING_TYPES)
    }

    @Test
    fun `ueberlebt den Prestige-Reset`() {
        val unlocked = GameState.newGame(0L).copy(
            achievements = AchievementInventory.of(AchievementType.FIRST_CLICK),
        )

        val afterPrestige = unlocked.afterPrestige(BigNumber.of(5))

        assertThat(AchievementType.FIRST_CLICK in afterPrestige.achievements).isTrue()
    }
}

class QuestUseCasesTest {

    private val rolloverQuests = RolloverQuestsUseCase()
    private val claimQuest = ClaimQuestUseCase()

    private val zone: ZoneId = ZoneId.of("UTC")

    /** 1. Januar 2024, 12:00 UTC. */
    private val day1Noon = 1_704_110_400_000L
    private val oneDay = 24L * 60L * 60L * 1_000L

    private fun startedState(nowMillis: Long = day1Noon): GameState =
        rolloverQuests(GameState.newGame(0L), nowMillis, zone)

    // --- Zuruecksetzen ----------------------------------------------------

    @Test
    fun `legt beim ersten Aufruf Ausgangswerte an`() {
        val state = startedState()

        assertThat(state.quests.baselines).containsKey(QuestPeriod.DAILY)
        assertThat(state.quests.baselines).containsKey(QuestPeriod.WEEKLY)
        // Lebenszeitquests brauchen keinen Ausgangswert.
        assertThat(state.quests.baselines).doesNotContainKey(QuestPeriod.LIFETIME)
    }

    @Test
    fun `setzt am naechsten Kalendertag zurueck`() {
        val started = startedState()
        val progressed = started.copy(statistics = GameStatistics(totalClicks = 250L))

        val nextDay = rolloverQuests(progressed, day1Noon + oneDay, zone)

        // Der Ausgangswert steht jetzt auf dem aktuellen Stand, der Fortschritt
        // beginnt wieder bei null.
        assertThat(nextDay.quests.progressOf(QuestType.DAILY_HUNDRED_CLICKS, nextDay))
            .isEqualTo(BigNumber.ZERO)
    }

    @Test
    fun `setzt innerhalb desselben Tages nicht zurueck`() {
        val started = startedState()
        val progressed = started.copy(statistics = GameStatistics(totalClicks = 50L))

        // Sechs Stunden spaeter, immer noch derselbe Kalendertag.
        val later = rolloverQuests(progressed, day1Noon + 6L * 60L * 60L * 1_000L, zone)

        assertThat(later.quests.progressOf(QuestType.DAILY_HUNDRED_CLICKS, later))
            .isEqualTo(BigNumber.of(50))
    }

    @Test
    fun `setzt bei zurueckgestellter Uhr nicht zurueck`() {
        // Ein Reset waere hier ein Geschenk fuer den Versuch, das System
        // auszutricksen.
        val started = startedState()
        val progressed = started.copy(statistics = GameStatistics(totalClicks = 50L))

        val backwards = rolloverQuests(progressed, day1Noon - 5L * oneDay, zone)

        assertThat(backwards.quests.baselines[QuestPeriod.DAILY]?.startedAtMillis)
            .isEqualTo(day1Noon)
    }

    @Test
    fun `laesst die Wochenquest beim Tageswechsel unberuehrt`() {
        val started = startedState()
        val progressed = started.copy(statistics = GameStatistics(totalClicks = 250L))

        // Ein Tag weiter, aber dieselbe Kalenderwoche.
        val nextDay = rolloverQuests(progressed, day1Noon + oneDay, zone)

        assertThat(nextDay.quests.progressOf(QuestType.WEEKLY_FIVE_THOUSAND_CLICKS, nextDay))
            .isEqualTo(BigNumber.of(250))
    }

    @Test
    fun `gibt abgeholte Quests des Zeitraums wieder frei`() {
        val started = startedState()
            .copy(statistics = GameStatistics(totalClicks = 250L))
        val claimed = (
            claimQuest(started, QuestType.DAILY_HUNDRED_CLICKS)
                as QuestClaimResult.Success
            ).state

        val nextDay = rolloverQuests(claimed, day1Noon + oneDay, zone)

        assertThat(nextDay.quests.isClaimed(QuestType.DAILY_HUNDRED_CLICKS)).isFalse()
    }

    // --- Fortschritt ------------------------------------------------------

    @Test
    fun `misst den Fortschritt ab dem Beginn des Zeitraums`() {
        // Der Kern des Ansatzes: Eine Tagesquest zaehlt die Klicks von heute,
        // nicht die des gesamten Spiels.
        val withHistory = GameState.newGame(0L)
            .copy(statistics = GameStatistics(totalClicks = 5_000L))
        val started = rolloverQuests(withHistory, day1Noon, zone)

        val progressed = started.copy(statistics = GameStatistics(totalClicks = 5_030L))

        assertThat(progressed.quests.progressOf(QuestType.DAILY_HUNDRED_CLICKS, progressed))
            .isEqualTo(BigNumber.of(30))
    }

    @Test
    fun `zaehlt bei Lebenszeitquests den Gesamtstand`() {
        val state = startedState().copy(statistics = GameStatistics(totalClicks = 5_000L))

        assertThat(state.quests.progressOf(QuestType.LIFETIME_TEN_THOUSAND_CLICKS, state))
            .isEqualTo(BigNumber.of(5_000))
    }

    // --- Abholen ----------------------------------------------------------

    @Test
    fun `holt eine abgeschlossene Quest ab`() {
        val state = startedState().copy(statistics = GameStatistics(totalClicks = 100L))

        val result = claimQuest(state, QuestType.DAILY_HUNDRED_CLICKS)

        assertThat(result).isInstanceOf(QuestClaimResult.Success::class.java)
        val success = result as QuestClaimResult.Success
        assertThat(success.state[ResourceType.DIAMONDS])
            .isEqualTo(
                state[ResourceType.DIAMONDS] +
                    QuestType.DAILY_HUNDRED_CLICKS.reward[ResourceType.DIAMONDS],
            )
        assertThat(success.state.quests.isClaimed(QuestType.DAILY_HUNDRED_CLICKS)).isTrue()
    }

    @Test
    fun `verweigert die Abholung vor Erreichen des Ziels`() {
        val state = startedState().copy(statistics = GameStatistics(totalClicks = 99L))

        assertThat(claimQuest(state, QuestType.DAILY_HUNDRED_CLICKS))
            .isEqualTo(QuestClaimResult.NotComplete)
    }

    @Test
    fun `verweigert die zweite Abholung`() {
        val state = startedState().copy(statistics = GameStatistics(totalClicks = 100L))
        val claimed = (
            claimQuest(state, QuestType.DAILY_HUNDRED_CLICKS)
                as QuestClaimResult.Success
            ).state

        assertThat(claimQuest(claimed, QuestType.DAILY_HUNDRED_CLICKS))
            .isEqualTo(QuestClaimResult.AlreadyClaimed)
    }

    @Test
    fun `haelt die Schluessel eindeutig`() {
        assertThat(QuestType.entries.map { it.id }).containsNoDuplicates()
        assertThat(AchievementType.entries.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `belohnt jede Quest und jedes Achievement`() {
        // Ein Ziel ohne Gegenwert ist im Genre ein Haken auf einer Liste.
        QuestType.entries.forEach { quest ->
            assertThat(quest.reward.isEmpty).isFalse()
            assertThat(quest.target.isPositive).isTrue()
        }
        AchievementType.entries.forEach { achievement ->
            assertThat(achievement.reward.isEmpty).isFalse()
        }
    }
}
