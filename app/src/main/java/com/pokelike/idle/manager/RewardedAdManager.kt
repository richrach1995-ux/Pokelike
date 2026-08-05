package com.pokelike.idle.manager

import com.pokelike.idle.ads.AdResult
import com.pokelike.idle.ads.RewardedAdSource
import com.pokelike.idle.di.ApplicationScope
import com.pokelike.idle.domain.model.RewardedAdPlacement
import com.pokelike.idle.domain.repository.GameRepository
import com.pokelike.idle.domain.usecases.AdRewardResult
import com.pokelike.idle.domain.usecases.GrantAdRewardUseCase
import com.pokelike.idle.util.DispatcherProvider
import com.pokelike.idle.util.GameLogger
import com.pokelike.idle.util.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zustand eines Videoangebots aus Sicht der Oberflaeche.
 *
 * @property isReady Ob jetzt getippt werden kann.
 * @property isLoading Ob gerade ein Video geladen wird.
 * @property isShowing Ob gerade eines laeuft.
 * @property cooldownRemainingMillis Verbleibende Wartezeit, null wenn keine
 *   laeuft.
 * @property lastFailed Ob der letzte Versuch gescheitert ist. Grundlage eines
 *   Hinweises - der Spieler soll erfahren, dass kein Netz da war, und nicht
 *   glauben, das Angebot sei verschwunden.
 */
data class RewardedAdStatus(
    val isReady: Boolean = false,
    val isLoading: Boolean = false,
    val isShowing: Boolean = false,
    val cooldownRemainingMillis: Long = 0L,
    val lastFailed: Boolean = false,
)

/**
 * Bringt Videoangebot, Wartezeit und Gutschrift zusammen.
 *
 * Die Aufteilung gegenueber [RewardedAdSource] ist Absicht: Dort steht
 * ausschliesslich, was ein Werbe-SDK kann - laden und zeigen. Hier steht,
 * was das Spiel daraus macht - wann angeboten wird, was es einbringt und was
 * bei Abbruch oder Fehler geschieht. Ein Wechsel des Werbevermittlers
 * beruehrt diese Klasse nicht.
 *
 * **Wartezeit und Gutschrift liegen im Spielstand**, nicht hier. Eine
 * Wartezeit im Arbeitsspeicher waere durch einen Neustart der App zu umgehen,
 * und bei einem Spiel, das ohnehin staendig in den Hintergrund geht, ist das
 * kein Umweg, sondern der Normalfall.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val gameClock: GameClock,
    private val repository: GameRepository,
    private val timeSource: TimeSource,
    private val adSource: RewardedAdSource,
    private val grantAdReward: GrantAdRewardUseCase,
    private val logger: GameLogger,
) {

    private val _status = MutableStateFlow(RewardedAdStatus())

    /** Zustand der einzigen Stelle, an der es bisher Videos gibt. */
    val status: StateFlow<RewardedAdStatus> = _status.asStateFlow()

    private var tickJob: Job? = null
    private var showJob: Job? = null
    private var prepareJob: Job? = null

    /**
     * Startet die laufende Aktualisierung und laedt ein Video vor.
     *
     * Vorladen beim Start der Sitzung und nicht erst beim Antippen: Ein Video
     * braucht Sekunden zum Laden, und der Spieler wuerde tippen und nichts
     * sehen.
     */
    fun start() {
        prepare()

        if (tickJob?.isActive == true) return

        tickJob = scope.launch(dispatchers.default) {
            gameClock.tick.collect { tick ->
                if (tick.index % TICKS_PER_CHECK != 0L) return@collect
                refresh()
            }
        }
    }

    /** Haelt die laufende Aktualisierung an. Ein laufendes Video bleibt unberuehrt. */
    fun stop() {
        tickJob?.cancel()
        tickJob = null
    }

    /**
     * Entfernt abgelaufene Wartezeiten und schreibt den Anzeigezustand fort.
     *
     * Wird zusaetzlich beim Start der Sitzung aufgerufen, damit eine waehrend
     * der Abwesenheit abgelaufene Wartezeit sofort verschwindet.
     */
    fun refresh() {
        val now = timeSource.wallClock()

        repository.update { state ->
            val pruned = state.ads.pruned(now)
            if (pruned == state.ads) state else state.copy(ads = pruned)
        }

        val remaining = repository.gameState.value.ads.remainingAt(PLACEMENT, now)
        val isLoaded = PLACEMENT in adSource.readyPlacements.value

        _status.update { current ->
            current.copy(
                isReady = isLoaded && remaining == 0L && !current.isShowing,
                isLoading = prepareJob?.isActive == true,
                cooldownRemainingMillis = remaining,
            )
        }

        // Nachladen, sobald die Wartezeit vorbei ist und kein Video bereitliegt
        // - etwa nachdem das vorige verbraucht wurde.
        if (!isLoaded && remaining == 0L) prepare()
    }

    /**
     * Zeigt ein Video und schreibt bei Erfolg die Belohnung gut.
     *
     * Kehrt sofort zurueck; der Ablauf laeuft im prozessweiten Scope weiter.
     * Waere er an den Bildschirm gebunden, ginge die Belohnung verloren, sobald
     * das Video die Activity in den Hintergrund schiebt - und genau das tut ein
     * Belohnungsvideo.
     */
    fun show() {
        if (showJob?.isActive == true) return
        if (!_status.value.isReady) return

        showJob = scope.launch(dispatchers.default) {
            _status.update { it.copy(isShowing = true, isReady = false, lastFailed = false) }

            val result = adSource.show(PLACEMENT)

            when (result) {
                AdResult.EarnedReward -> grantReward()

                // Abbruch ist kein Fehler und keine Meldung wert. Die
                // Wartezeit beginnt nicht - sie ist der Preis der Belohnung,
                // nicht die Strafe fuer einen Abbruch.
                AdResult.Dismissed -> Unit

                AdResult.NotReady ->
                    logger.warn(TAG, "Video war doch nicht bereit: ${PLACEMENT.id}")

                is AdResult.Failed -> {
                    logger.warn(TAG, "Video fehlgeschlagen: ${result.reason}")
                    _status.update { it.copy(lastFailed = true) }
                }
            }

            _status.update { it.copy(isShowing = false) }
            refresh()
        }
    }

    /** Bestaetigt, dass der Fehlerhinweis gezeigt wurde. */
    fun consumeFailure() {
        _status.update { it.copy(lastFailed = false) }
    }

    private fun grantReward() {
        val now = timeSource.wallClock()
        lateinit var result: AdRewardResult

        repository.update { state ->
            result = grantAdReward(state, PLACEMENT, now)
            when (val outcome = result) {
                is AdRewardResult.Success -> outcome.state
                AdRewardResult.OnCooldown -> state
            }
        }

        if (result is AdRewardResult.OnCooldown) {
            // Der Spieler hat das Video zu Ende gesehen und geht dennoch leer
            // aus. Das darf nicht stillschweigend geschehen - es waere der
            // schwerwiegendste denkbare Fehler dieses Systems.
            logger.warn(TAG, "Belohnung verfaellt, Wartezeit laeuft noch: ${PLACEMENT.id}")
        }
    }

    private fun prepare() {
        if (prepareJob?.isActive == true) return

        prepareJob = scope.launch(dispatchers.default) {
            _status.update { it.copy(isLoading = true) }
            adSource.prepare(PLACEMENT)
            _status.update {
                it.copy(
                    isLoading = false,
                    isReady = PLACEMENT in adSource.readyPlacements.value &&
                        repository.gameState.value.ads
                            .isReadyAt(PLACEMENT, timeSource.wallClock()),
                )
            }
        }
    }

    private companion object {
        const val TAG = "RewardedAds"

        /**
         * Die einzige Stelle mit Videos.
         *
         * Als Konstante und nicht als Parameter: Solange es genau eine gibt,
         * waere ein Parameter eine Auswahl ohne Alternative. Kommt eine zweite
         * hinzu, wird daraus eine Zuordnung von Stelle zu Zustand - und genau
         * dann ist der richtige Zeitpunkt dafuer.
         */
        val PLACEMENT = RewardedAdPlacement.BOOSTER_REWARD

        /**
         * Takte zwischen zwei Aktualisierungen.
         *
         * Bei 100 ms Taktrate entspricht das einer Sekunde - die Wartezeit
         * wird sekundengenau angezeigt.
         */
        const val TICKS_PER_CHECK = 10L
    }
}
