package com.runeveil.saga.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.runeveil.saga.data.di.ApplicationScope
import com.runeveil.saga.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All sound in Runeveil.
 *
 * Two independent paths, because they have opposite requirements:
 *
 *  * **Music** — one long, looping, cross-faded stream per context
 *    (title, region, dungeon, battle, boss). ExoPlayer handles it.
 *  * **Effects** — dozens of short, overlapping one-shots per second during a
 *    battle. [SoundPool] is used instead of ExoPlayer because it keeps decoded
 *    samples in memory and has no per-play allocation cost.
 *
 * Assets are addressed by *key* (`bgm_battle_boss`, `sfx_fire_3`) and resolved
 * to `assets/audio/<key>.ogg`. A missing file is a no-op rather than a crash —
 * the game must stay playable while the soundtrack is still being produced.
 */
@Singleton
class AudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /**
     * Every ExoPlayer call has to happen on the thread the player was built on.
     * The player is built on the main looper, but the injected application
     * scope runs on [kotlinx.coroutines.Dispatchers.Default] — collecting the
     * settings flow there and writing `player.volume` from it threw
     * `IllegalStateException: Player is accessed on the wrong thread` and killed
     * the process a moment after launch.
     *
     * All player work therefore runs in this scope: same lifetime as the
     * application scope (it is a child of its job, so it dies with it), but
     * pinned to the main thread. `Main.immediate` means a call that is already
     * on the main thread is executed inline rather than posted, so pausing the
     * music from the lifecycle observer stays synchronous.
     *
     * SoundPool has no such restriction and is used from any thread.
     */
    private val playerScope = CoroutineScope(
        SupervisorJob(scope.coroutineContext[Job]) + Dispatchers.Main.immediate,
    )

    private var musicPlayer: ExoPlayer? = null
    private var soundPool: SoundPool? = null

    /** Loaded sample ids, keyed by asset key. */
    private val samples = mutableMapOf<String, Int>()

    /** The music key currently playing; used to avoid restarting a track. */
    private val currentMusic = MutableStateFlow<String?>(null)

    private var musicVolume = 0.7f
    private var soundVolume = 0.85f

    /** Music that was interrupted by a battle, restored afterwards. */
    private var suspendedMusic: String? = null

    fun initialise() {
        if (musicPlayer == null) {
            // Pin the player to the main looper explicitly instead of inheriting
            // the looper of whatever thread happens to call initialise().
            musicPlayer = ExoPlayer.Builder(context)
                .setLooper(Looper.getMainLooper())
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = musicVolume
                }
        }
        if (soundPool == null) {
            soundPool = SoundPool.Builder()
                .setMaxStreams(MAX_PARALLEL_EFFECTS)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .build()
        }
        settings.observeSettings()
            .onEach { current ->
                musicVolume = current.musicVolume
                soundVolume = current.soundVolume
                musicPlayer?.volume = musicVolume
            }
            .launchIn(playerScope)
    }

    /**
     * Plays [musicKey], cross-fading out of whatever is playing.
     * Passing the key that is already playing does nothing.
     */
    fun playMusic(musicKey: String?, fadeMs: Int = DEFAULT_FADE_MS) {
        if (musicKey == currentMusic.value) return
        val player = musicPlayer ?: return
        currentMusic.value = musicKey

        if (musicKey == null) {
            playerScope.launch { fadeOutAndStop(player, fadeMs) }
            return
        }
        val path = "asset:///audio/$musicKey.ogg"
        playerScope.launch {
            if (player.isPlaying) fadeOutAndStop(player, fadeMs / 2)
            runCatching {
                player.setMediaItem(MediaItem.fromUri(path))
                player.prepare()
                player.volume = 0f
                player.play()
                fadeIn(player, fadeMs)
            }
        }
    }

    /** Ducks the overworld theme and starts a battle track. */
    fun pushBattleMusic(musicKey: String) {
        suspendedMusic = currentMusic.value
        playMusic(musicKey, fadeMs = 250)
    }

    /** Returns to whatever was playing before the battle. */
    fun popBattleMusic() {
        val previous = suspendedMusic ?: return
        suspendedMusic = null
        playMusic(previous, fadeMs = 600)
    }

    /**
     * Plays a one-shot effect. [pitch] varies the playback rate so repeated
     * hits do not sound mechanically identical.
     */
    fun playSound(soundKey: String, pitch: Float = 1f, volumeScale: Float = 1f) {
        val pool = soundPool ?: return
        val volume = (soundVolume * volumeScale).coerceIn(0f, 1f)
        if (volume <= 0f) return

        val existing = samples[soundKey]
        if (existing != null) {
            pool.play(existing, volume, volume, 1, 0, pitch.coerceIn(0.5f, 2f))
            return
        }
        runCatching {
            context.assets.openFd("audio/$soundKey.ogg").use { descriptor ->
                val id = pool.load(descriptor, 1)
                samples[soundKey] = id
                pool.setOnLoadCompleteListener { loadedPool, sampleId, status ->
                    if (status == 0 && sampleId == id) {
                        loadedPool.play(id, volume, volume, 1, 0, pitch.coerceIn(0.5f, 2f))
                    }
                }
            }
        }
    }

    /** Pre-loads the effects a screen is about to need (battle entry). */
    fun preload(soundKeys: Collection<String>) {
        val pool = soundPool ?: return
        for (key in soundKeys) {
            if (key in samples) continue
            runCatching {
                context.assets.openFd("audio/$key.ogg").use { descriptor ->
                    samples[key] = pool.load(descriptor, 1)
                }
            }
        }
    }

    fun pause() {
        soundPool?.autoPause()
        playerScope.launch { musicPlayer?.pause() }
    }

    fun resume() {
        soundPool?.autoResume()
        playerScope.launch { musicPlayer?.play() }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        samples.clear()
        currentMusic.value = null
        playerScope.launch {
            musicPlayer?.release()
            musicPlayer = null
        }
    }

    private suspend fun fadeIn(player: ExoPlayer, durationMs: Int) {
        val steps = (durationMs / FADE_STEP_MS).coerceAtLeast(1)
        repeat(steps) { step ->
            player.volume = musicVolume * (step + 1) / steps
            delay(FADE_STEP_MS.toLong())
        }
        player.volume = musicVolume
    }

    private suspend fun fadeOutAndStop(player: ExoPlayer, durationMs: Int) {
        val steps = (durationMs / FADE_STEP_MS).coerceAtLeast(1)
        val start = player.volume
        repeat(steps) { step ->
            player.volume = start * (steps - step - 1) / steps
            delay(FADE_STEP_MS.toLong())
        }
        player.stop()
        player.volume = musicVolume
    }

    private companion object {
        const val MAX_PARALLEL_EFFECTS = 12
        const val DEFAULT_FADE_MS = 900
        const val FADE_STEP_MS = 40
    }
}

/**
 * Maps game situations to audio keys, so screens never hard-code file names.
 * The keys line up with the `musicKey`/`soundKey` fields in the content JSON.
 */
object AudioKeys {
    const val TITLE = "bgm_title"
    const val CHARACTER_CREATION = "bgm_creation"
    const val ENDING = "bgm_ending"

    const val UI_SELECT = "sfx_ui_select"
    const val UI_CONFIRM = "sfx_ui_confirm"
    const val UI_CANCEL = "sfx_ui_cancel"
    const val UI_ERROR = "sfx_ui_error"

    const val STEP = "sfx_step"
    const val ENCOUNTER = "sfx_encounter"
    const val LEVEL_UP = "sfx_level_up"
    const val EVOLUTION = "sfx_evolution"
    const val EGG_HATCH = "sfx_egg_hatch"
    const val CRAFT = "sfx_craft"
    const val PURCHASE = "sfx_purchase"

    const val ORB_THROW = "sfx_orb_throw"
    const val ORB_SHAKE = "sfx_orb_shake"
    const val ORB_CAUGHT = "sfx_orb_caught"
    const val ORB_BREAK = "sfx_orb_break"

    const val HIT_NEUTRAL = "sfx_hit_neutral"
    const val HIT_SUPER = "sfx_hit_super"
    const val HIT_WEAK = "sfx_hit_weak"
    const val CRITICAL = "sfx_critical"
    const val FAINT = "sfx_faint"
    const val HEAL = "sfx_heal"
    const val SHIELD = "sfx_shield"
    const val STATUS = "sfx_status"

    /** Battle music for a fight type, falling back to the wild theme. */
    fun battleMusic(typeMusicKey: String?): String = typeMusicKey ?: "bgm_battle_wild"
}
