package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.GameState
import com.pokelike.idle.domain.model.QuestType
import com.pokelike.idle.domain.model.ResourceBundle
import javax.inject.Inject

/** Ergebnis einer Quest-Abholung. */
sealed interface QuestClaimResult {

    data class Success(
        val state: GameState,
        val quest: QuestType,
        val reward: ResourceBundle,
    ) : QuestClaimResult

    /** Das Ziel ist noch nicht erreicht. */
    data object NotComplete : QuestClaimResult

    /**
     * Bereits abgeholt.
     *
     * Von [NotComplete] getrennt, weil beides auf unterschiedliche Fehler
     * hindeutet: "nicht fertig" kann der Spieler ausloesen, "schon abgeholt"
     * deutet auf einen Fehler in der Oberflaeche hin.
     */
    data object AlreadyClaimed : QuestClaimResult
}

/**
 * Holt die Belohnung einer abgeschlossenen Quest ab.
 *
 * Anders als Achievements werden Quests bewusst nicht automatisch
 * gutgeschrieben: Das Abholen ist im Genre ein eigener, bewusst gesetzter
 * Moment, und der Spieler soll ihn selbst ausloesen.
 *
 * Pruefung und Gutschrift liegen in einer Operation, damit eine Quest nicht
 * zweimal abgeholt werden kann.
 */
class ClaimQuestUseCase @Inject constructor() {

    operator fun invoke(state: GameState, quest: QuestType): QuestClaimResult {
        if (state.quests.isClaimed(quest)) return QuestClaimResult.AlreadyClaimed
        if (!state.quests.isComplete(quest, state)) return QuestClaimResult.NotComplete

        val rewarded = state.grant(quest.reward)

        return QuestClaimResult.Success(
            state = rewarded.copy(quests = rewarded.quests.withClaimed(quest)),
            quest = quest,
            reward = quest.reward,
        )
    }
}
