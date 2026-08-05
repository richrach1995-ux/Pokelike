package com.pokelike.idle.ui.screens.goals

import androidx.compose.runtime.Immutable
import com.pokelike.idle.domain.model.AchievementType
import com.pokelike.idle.domain.model.QuestPeriod
import com.pokelike.idle.domain.model.QuestType
import com.pokelike.idle.ui.components.RewardPart

/** Welche Liste gerade sichtbar ist. */
enum class GoalsTab {
    QUESTS,
    ACHIEVEMENTS,
}

/**
 * Eine Quest-Zeile.
 *
 * @property progressText Stand als Text, etwa "37 / 100".
 * @property progressFraction Stand als Anteil fuer den Balken, 0 bis 1.
 * @property reward Bestandteile der Belohnung.
 * @property isComplete Ob das Ziel erreicht ist.
 * @property isClaimed Ob die Belohnung bereits abgeholt wurde.
 */
@Immutable
data class QuestRow(
    val type: QuestType,
    val progressText: String,
    val progressFraction: Float,
    val reward: List<RewardPart>,
    val isComplete: Boolean,
    val isClaimed: Boolean,
)

/**
 * Eine Achievement-Zeile.
 *
 * Nicht erreichte Achievements bleiben sichtbar. Sie zu verbergen wuerde dem
 * Spieler die Ziele nehmen, auf die er zusteuern kann - und genau das ist ihr
 * Zweck.
 */
@Immutable
data class AchievementRow(
    val type: AchievementType,
    val reward: List<RewardPart>,
    val isUnlocked: Boolean,
)

/**
 * Anzeigezustand des Ziele-Bildschirms.
 *
 * @property questsByPeriod Quests nach Zeitraum gegliedert.
 * @property achievements Alle Achievements, erreichte zuletzt.
 * @property unlockedCount Erreichte Achievements.
 * @property totalCount Achievements insgesamt.
 * @property claimableCount Abholbereite Quests. Grundlage des Hinweispunkts an
 *   der unteren Leiste.
 */
@Immutable
data class GoalsUiState(
    val selectedTab: GoalsTab = GoalsTab.QUESTS,
    val questsByPeriod: Map<QuestPeriod, List<QuestRow>> = emptyMap(),
    val achievements: List<AchievementRow> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val claimableCount: Int = 0,
)
