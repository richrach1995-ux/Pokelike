package com.runeveil.saga.presentation.cutscene

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.runeveil.saga.R
import com.runeveil.saga.audio.AudioEngine
import com.runeveil.saga.domain.model.story.CutsceneBeat
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.PlayerRepository
import com.runeveil.saga.ui.components.contentText
import com.runeveil.saga.ui.theme.RuneGold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Cinematic playback. Beats advance on their own timer, and a tap skips ahead
 * — cutscenes should never hold the player hostage.
 */
@Composable
fun CutsceneScreen(
    cutsceneId: String,
    onFinished: () -> Unit,
    viewModel: CutsceneViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(cutsceneId) { viewModel.play(cutsceneId) }
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { viewModel.skipBeat() },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state.currentText,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
            label = "beat",
        ) { text ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.speaker?.let {
                    Text(contentText(it), style = MaterialTheme.typography.titleMedium, color = RuneGold)
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    // Beats carry content keys; resolve them for display.
                    text = if (text.isEmpty()) "" else contentText(text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = stringResource(R.string.common_continue),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(28.dp),
        )
    }
}

data class CutsceneUiState(
    val currentText: String = "",
    val speaker: String? = null,
    val finished: Boolean = false,
)

@HiltViewModel
class CutsceneViewModel @Inject constructor(
    private val content: ContentRepository,
    private val player: PlayerRepository,
    private val audio: AudioEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(CutsceneUiState())
    val state: StateFlow<CutsceneUiState> = _state.asStateFlow()

    private var skipRequested = false

    fun play(cutsceneId: String) {
        viewModelScope.launch {
            content.ensureLoaded()
            val cutscene = content.cutscene(cutsceneId) ?: run {
                _state.value = CutsceneUiState(finished = true)
                return@launch
            }
            cutscene.musicKey?.let { audio.playMusic(it) }

            for (beat in cutscene.beats) {
                skipRequested = false
                when (beat) {
                    is CutsceneBeat.Narration -> {
                        _state.value = CutsceneUiState(currentText = beat.textKey)
                        waitOrSkip(beat.durationMs)
                    }
                    is CutsceneBeat.Speech -> {
                        _state.value = CutsceneUiState(
                            currentText = beat.textKey,
                            speaker = beat.speakerNameKey,
                        )
                        waitOrSkip(SPEECH_DURATION_MS)
                    }
                    is CutsceneBeat.ShowImage -> waitOrSkip(beat.durationMs)
                    is CutsceneBeat.PlaySound -> audio.playSound(beat.soundKey)
                    is CutsceneBeat.ChangeMusic -> audio.playMusic(beat.musicKey, beat.fadeMs)
                    is CutsceneBeat.ScreenEffect -> waitOrSkip(beat.durationMs)
                    is CutsceneBeat.GrantFlag -> player.grantFlag(beat.flag)
                    is CutsceneBeat.StartBattle -> Unit // Triggered by the caller.
                    is CutsceneBeat.Choice -> Unit // Choice beats are handled by DialogueScreen.
                }
            }
            _state.value = _state.value.copy(finished = true)
        }
    }

    fun skipBeat() {
        skipRequested = true
    }

    private suspend fun waitOrSkip(durationMs: Int) {
        var elapsed = 0
        while (elapsed < durationMs && !skipRequested) {
            delay(TICK_MS)
            elapsed += TICK_MS
        }
    }

    private companion object {
        const val SPEECH_DURATION_MS = 3800
        const val TICK_MS = 60
    }
}
