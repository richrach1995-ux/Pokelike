package com.pokelike.idle.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokelike.idle.domain.model.BoosterType
import com.pokelike.idle.domain.model.ClickOutcome
import com.pokelike.idle.domain.model.ResourceType
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.repository.SettingsRepository
import com.pokelike.idle.domain.usecases.BoosterPurchaseResult
import com.pokelike.idle.manager.BoosterManager
import com.pokelike.idle.manager.ClickManager
import com.pokelike.idle.manager.RewardedAdManager
import com.pokelike.idle.manager.IdleIncomeManager
import com.pokelike.idle.manager.ModifierManager
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.DurationFormatter
import com.pokelike.idle.util.NumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel des Hauptbildschirms.
 *
 * Fuehrt Spielstand, Combo und Klickwerte zu genau einem [HomeUiState]
 * zusammen. Der Screen bekommt dadurch einen einzigen Zustand statt mehrerer
 * Fluesse, die er selbst kombinieren muesste - was bei mehreren Quellen
 * unweigerlich zu kurzzeitig widerspruechlichen Anzeigen fuehrt.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clickManager: ClickManager,
    private val boosterManager: BoosterManager,
    private val rewardedAdManager: RewardedAdManager,
    idleIncomeManager: IdleIncomeManager,
    modifierManager: ModifierManager,
    gameRepository: GameRepository,
    settingsRepository: SettingsRepository,
    private val numberFormatter: NumberFormatter,
    private val durationFormatter: DurationFormatter,
    dispatchers: DispatcherProvider,
) : ViewModel() {

    /**
     * Die haptische Einstellung als eigener Fluss.
     *
     * Nicht Teil des grossen `combine`: Dessen typsichere Ueberladungen enden
     * bei fuenf Fluessen, und ein sechster zwaenge zur Variante mit
     * `Array<Any?>`. Zusammengefuehrt wird erst im Anzeigezustand.
     */
    private val vibrationEnabled: Flow<Boolean> = settingsRepository.settings
        .map { it.vibrationEnabled }
        .distinctUntilChanged()

    private val gameUiState: Flow<HomeUiState> = combine(
        gameRepository.gameState,
        gameRepository.isLoaded,
        clickManager.combo,
        modifierManager.modifiers,
        idleIncomeManager.incomePerSecond,
    ) { gameState, isLoaded, combo, modifiers, incomePerSecond ->
        HomeUiState(
            isReady = isLoaded,
            coins = numberFormatter.format(gameState[ResourceType.COINS]),
            diamonds = numberFormatter.format(gameState[ResourceType.DIAMONDS]),
            coinsPerClick = numberFormatter.format(
                modifiers.click.expectedValuePerClick(combo.multiplier),
            ),
            comboCount = combo.count,
            comboMultiplier = formatMultiplier(combo.multiplier),
            comboRemaining = combo.remainingFraction,
            coinsPerSecond = numberFormatter.format(incomePerSecond),
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        gameUiState,
        vibrationEnabled,
    ) { state, isVibrationEnabled ->
        state.copy(vibrationEnabled = isVibrationEnabled)
    }
        // Formatierung gehoert nicht auf den UI-Thread: Bei laufender Combo
        // aendert sich der Zustand zehnmal pro Sekunde.
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(),
        )

    /**
     * Laufende Booster.
     *
     * Bewusst ein eigener Fluss und nicht Teil von [uiState]. Zwei Gruende:
     * [uiState] aendert sich bei laufender Combo zehnmal pro Sekunde, die
     * Restlaufzeiten dagegen einmal pro Sekunde - zusammengefuehrt wuerde die
     * Zeitformatierung zehnmal so oft laufen wie noetig. Und `combine` mit
     * typsicheren Ueberladungen endet bei fuenf Fluessen; ein sechster zwaenge
     * zur Variante mit `Array<Any?>` und verloere jede Typpruefung.
     */
    val boosters: StateFlow<List<BoosterRow>> = boosterManager.active
        .map { snapshots ->
            snapshots.map { snapshot ->
                BoosterRow(
                    type = snapshot.type,
                    remainingText = durationFormatter.formatCompact(snapshot.remainingMillis),
                    progress = snapshot.progress,
                )
            }
        }
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    /**
     * Das Booster-Angebot.
     *
     * Haengt am Spielstand, weil sich die Kaufbarkeit mit jedem Diamanten
     * aendert. [distinctUntilChanged] filtert die grosse Mehrheit der
     * Aenderungen weg: Der Muenzstand steigt zehnmal pro Sekunde, an den
     * Angeboten aendert das nichts.
     */
    val boosterOffers: StateFlow<List<BoosterOffer>> = combine(
        gameRepository.gameState,
        boosterManager.active,
    ) { gameState, active ->
        val running = active.associateBy { it.type }

        BoosterType.entries.map { type ->
            val remaining = running[type]?.remainingMillis
            val isAtMaximum = (remaining ?: 0L) >= type.maxStackedDurationMillis

            BoosterOffer(
                type = type,
                priceText = numberFormatter.format(type.price[ResourceType.DIAMONDS]),
                durationText = durationFormatter.formatCompact(type.durationMillis),
                canBuy = !isAtMaximum && gameState.resources.canAfford(type.price),
                isAtMaximum = isAtMaximum,
                remainingText = remaining?.let { durationFormatter.formatCompact(it) },
            )
        }
    }
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    /**
     * Das Angebot des Belohnungsvideos.
     *
     * Eigener Fluss wie die Booster: Er haengt am Ladezustand des Werbe-SDK
     * und nicht am Spielstand, und beides zusammenzufuehren hiesse, ihn
     * zehnmal pro Sekunde neu zu bilden.
     */
    val adOffer: StateFlow<AdOffer> = rewardedAdManager.status
        .map { status ->
            AdOffer(
                isReady = status.isReady,
                isLoading = status.isLoading,
                isShowing = status.isShowing,
                cooldownText = status.cooldownRemainingMillis
                    .takeIf { it > 0L }
                    ?.let(durationFormatter::formatCompact),
                lastFailed = status.lastFailed,
            )
        }
        .flowOn(dispatchers.default)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = AdOffer(),
        )

    /** Startet ein Belohnungsvideo. */
    fun onWatchAd() {
        rewardedAdManager.show()
    }

    /** Bestaetigt, dass der Fehlerhinweis gezeigt wurde. */
    fun onAdFailureShown() {
        rewardedAdManager.consumeFailure()
    }

    /**
     * Kauft einen Booster.
     *
     * @return `true`, wenn der Kauf gelungen ist.
     */
    fun onBuyBooster(type: BoosterType): Boolean =
        boosterManager.purchase(type) is BoosterPurchaseResult.Success

    /**
     * Verarbeitet einen Klick und liefert das Ergebnis fuer die Rueckmeldung.
     *
     * Der Rueckgabewert ist Absicht. Der uebliche Weg waere ein Ereignisfluss,
     * den der Bildschirm beobachtet - hier aber der falsche: Ein Klick muss
     * noch im selben Frame sichtbar werden. Der Umweg ueber einen Fluss
     * verschoebe die Rueckmeldung um mindestens einen Frame und braechte bei
     * schnellem Tippen zusaetzlich Pufferfragen mit sich.
     *
     * Der Aufruf ist nicht `suspend`, weil alle Schritte im Arbeitsspeicher
     * ablaufen; geschrieben wird ueber den Autosave.
     */
    fun onClick(): ClickOutcome = clickManager.click()

    /** Formatiert einen Klickertrag fuer den schwebenden Hinweis. */
    fun formatEarned(outcome: ClickOutcome): String =
        "+${numberFormatter.format(outcome.earned)}"

    private fun formatMultiplier(multiplier: Double): String =
        String.format(Locale.US, "%.2fx", multiplier)

    private companion object {
        /**
         * Wartezeit, bevor der Flow nach dem letzten Beobachter stoppt.
         *
         * Fuenf Sekunden sind die uebliche Wahl: lang genug fuer eine
         * Konfigurationsaenderung, kurz genug, um im Hintergrund nicht
         * unnoetig zu rechnen.
         */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
