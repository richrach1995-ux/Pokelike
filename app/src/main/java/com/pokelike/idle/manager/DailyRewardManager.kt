package com.pokelike.idle.manager

import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.CalculateModifiersUseCase
import com.pokelike.idle.domain.usecases.ClaimDailyRewardUseCase
import com.pokelike.idle.domain.usecases.DailyRewardClaimResult
import com.pokelike.idle.domain.usecases.DailyRewardStatus
import com.pokelike.idle.domain.usecases.EvaluateDailyRewardUseCase
import com.pokelike.idle.util.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Haelt den anstehenden Tagesbonus bereit.
 *
 * **Warum die Pruefung nur beim Wechsel in den Vordergrund laeuft und nicht
 * fortlaufend.** Der Tageswechsel faellt in aller Regel in eine Zeit, in der
 * die App nicht offen ist. Eine Pruefung in jedem Takt wuerde also fast immer
 * dasselbe Ergebnis liefern; der einzige Fall, den sie zusaetzlich abdeckt,
 * ist ein Spieler, der die App ueber Mitternacht geoeffnet liegen laesst. Der
 * bekommt seinen Bonus beim naechsten Hinsehen - und ein Dialog, der von
 * selbst ueber einem laufenden Spiel aufgeht, waere ohnehin die schlechtere
 * Loesung.
 *
 * Prozessweit und nicht im ViewModel, weil der Bonus unabhaengig davon
 * anliegt, welcher Bildschirm gerade sichtbar ist, und einen Bildschirmwechsel
 * ueberdauern muss.
 */
@Singleton
class DailyRewardManager @Inject constructor(
    private val repository: GameRepository,
    private val calculateModifiers: CalculateModifiersUseCase,
    private val timeSource: TimeSource,
    private val evaluate: EvaluateDailyRewardUseCase,
    private val claimDailyReward: ClaimDailyRewardUseCase,
) {

    private val _pending = MutableStateFlow<DailyRewardStatus?>(null)

    /**
     * Der anstehende Bonus, oder `null`, wenn heute nichts abzuholen ist.
     *
     * Als Zustand und nicht als Ereignis, damit eine Bildschirmdrehung
     * waehrend des Dialogs ihn nicht verschwinden laesst - dieselbe
     * Ueberlegung wie beim Offline-Fortschritt.
     */
    val pending: StateFlow<DailyRewardStatus?> = _pending.asStateFlow()

    /**
     * Prueft, ob ein Bonus ansteht. Wird beim Wechsel in den Vordergrund
     * gerufen.
     *
     * Die Modifikatoren werden hier aus dem Spielstand berechnet und nicht bei
     * [ModifierManager] abgefragt. Dessen Wert wird nebenlaeufig fortgeschrieben
     * und kann in genau diesem Moment noch der Stand von vor dem Laden sein -
     * die Muenzen des Tagesbonus fielen dann zu niedrig aus. Zustand und
     * Modifikatoren stammen so aus derselben Momentaufnahme.
     */
    fun refresh() {
        val state = repository.gameState.value

        _pending.value = evaluate(
            state = state,
            modifiers = calculateModifiers(state),
            nowMillis = timeSource.wallClock(),
        )
    }

    /**
     * Holt den anstehenden Bonus ab.
     *
     * Der Zeitpunkt wird hier erneut abgefragt und nicht der aus [refresh]
     * wiederverwendet: Zwischen Anzeige und Antippen koennen Stunden liegen,
     * wenn die App im Hintergrund lag. Massgeblich ist der Moment der
     * Abholung.
     *
     * @return `true`, wenn gutgeschrieben wurde.
     */
    fun claim(): Boolean {
        var claimed = false

        repository.update { state ->
            when (
                val result = claimDailyReward(
                    state = state,
                    modifiers = calculateModifiers(state),
                    nowMillis = timeSource.wallClock(),
                )
            ) {
                is DailyRewardClaimResult.Success -> {
                    claimed = true
                    result.state
                }

                DailyRewardClaimResult.NotDue -> {
                    claimed = false
                    state
                }
            }
        }

        _pending.value = null
        return claimed
    }

    /**
     * Blendet den Hinweis aus, ohne abzuholen.
     *
     * Der Anspruch bleibt bestehen: Beim naechsten Wechsel in den Vordergrund
     * steht derselbe Tag wieder an. Ein Wegwischen, das den Bonus verfallen
     * laesst, waere eine Falle.
     */
    fun dismiss() {
        _pending.value = null
    }
}
