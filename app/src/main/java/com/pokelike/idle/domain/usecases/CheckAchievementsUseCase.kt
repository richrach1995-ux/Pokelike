package com.pokelike.idle.domain.usecases

import com.pokelike.idle.domain.model.AchievementType
import com.pokelike.idle.domain.model.GameState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ergebnis einer Achievement-Pruefung.
 *
 * @property state Spielstand mit gutgeschriebenen Belohnungen.
 * @property newlyUnlocked Was in diesem Durchgang dazugekommen ist. Leer, wenn
 *   sich nichts geaendert hat - der Regelfall, denn die Pruefung laeuft im
 *   Sekundentakt.
 */
data class AchievementCheckResult(
    val state: GameState,
    val newlyUnlocked: List<AchievementType>,
)

/**
 * Prueft alle Achievements und schreibt faellige Belohnungen gut.
 *
 * Belohnung und Freischaltung passieren in einem Schritt. Waeren sie getrennt -
 * etwa Freischaltung jetzt, Belohnung beim Antippen eines Popups -, ginge die
 * Belohnung verloren, sobald die App dazwischen beendet wird. Die Oberflaeche
 * bekommt deshalb nur noch eine Benachrichtigung ueber etwas, das bereits
 * geschehen ist.
 *
 * Die Pruefung ist bewusst zustandslos und vollstaendig: Sie geht jedes Mal
 * alle Achievements durch, statt sich zu merken, welche noch offen sind. Bei
 * zwanzig Vergleichen im Sekundentakt ist das belanglos, und es schliesst eine
 * ganze Fehlerklasse aus - naemlich Achievements, die nie geprueft werden, weil
 * ein Merkzustand nicht aktualisiert wurde.
 */
@Singleton
class CheckAchievementsUseCase @Inject constructor() {

    operator fun invoke(state: GameState): AchievementCheckResult {
        val newlyUnlocked = AchievementType.entries.filter { achievement ->
            achievement !in state.achievements && achievement.condition.isMet(state)
        }

        if (newlyUnlocked.isEmpty()) return AchievementCheckResult(state, emptyList())

        var updated = state
        newlyUnlocked.forEach { achievement ->
            updated = updated
                .grant(achievement.reward)
                .copy(achievements = updated.achievements.plus(achievement))
        }

        return AchievementCheckResult(state = updated, newlyUnlocked = newlyUnlocked)
    }
}
