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
 * Kopfzeile ueber der Quest-Liste: Stand der Anmeldeserie.
 *
 * Hier und nicht im Dialog des Tagesbonus, weil der Dialog nur an Tagen
 * erscheint, an denen etwas abzuholen ist. Der Serienschutz muss aber auch
 * danach noch kaufbar sein - der Spieler weiss meist erst nach dem Abholen,
 * dass er morgen keine Zeit haben wird.
 *
 * @property streak Laufende Serie in Tagen.
 * @property longestStreak Bestwert.
 * @property protectionCharges Vorrat an Serienschutz.
 * @property protectionPrice Preis einer Ladung, bereits formatiert.
 * @property canBuyProtection Ob ein Kauf jetzt moeglich ist. Falsch bei
 *   vollem Vorrat und bei zu wenig Diamanten.
 * @property isProtectionFull Ob der Vorrat voll ist. Von [canBuyProtection]
 *   getrennt, damit die Oberflaeche den Grund benennen kann, statt nur einen
 *   ausgegrauten Knopf zu zeigen.
 */
@Immutable
data class StreakRow(
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val protectionCharges: Int = 0,
    val protectionPrice: String = "",
    val canBuyProtection: Boolean = false,
    val isProtectionFull: Boolean = false,
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
    val streak: StreakRow = StreakRow(),
    val questsByPeriod: Map<QuestPeriod, List<QuestRow>> = emptyMap(),
    val achievements: List<AchievementRow> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val claimableCount: Int = 0,
)
