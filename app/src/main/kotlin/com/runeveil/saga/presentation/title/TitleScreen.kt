package com.runeveil.saga.presentation.title

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.runeveil.saga.R
import com.runeveil.saga.audio.AudioEngine
import com.runeveil.saga.audio.AudioKeys
import com.runeveil.saga.domain.repository.SaveRepository
import com.runeveil.saga.ui.components.RunicButton
import com.runeveil.saga.ui.components.RunicOutlinedButton
import com.runeveil.saga.ui.theme.RuneGold
import com.runeveil.saga.ui.theme.RuneNight
import com.runeveil.saga.ui.theme.RuneNightSunken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The title screen: a slowly breathing rune over a night sky, the game's
 * wordmark, and the entry points into the game.
 */
@Composable
fun TitleScreen(
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    onLoad: () -> Unit,
    onSettings: () -> Unit,
    onBestiary: () -> Unit,
    viewModel: TitleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onEnter() }

    val transition = rememberInfiniteTransition(label = "titleGlow")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RuneNight, RuneNightSunken))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "ᚨ",
                    style = MaterialTheme.typography.displayLarge,
                    color = RuneGold,
                    modifier = Modifier.alpha(glow),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = RuneGold,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.hasSave) {
                    RunicButton(
                        text = stringResource(R.string.title_continue),
                        onClick = { viewModel.continueGame(onContinue) },
                        modifier = Modifier.fillMaxWidth(),
                        glyph = "ᚱ",
                    )
                }
                RunicButton(
                    text = stringResource(R.string.title_new_game),
                    onClick = onNewGame,
                    modifier = Modifier.fillMaxWidth(),
                    glyph = "ᚠ",
                )
                RunicOutlinedButton(
                    text = stringResource(R.string.title_load),
                    onClick = onLoad,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.hasSave,
                )
                RunicOutlinedButton(
                    text = stringResource(R.string.title_bestiary),
                    onClick = onBestiary,
                    modifier = Modifier.fillMaxWidth(),
                )
                RunicOutlinedButton(
                    text = stringResource(R.string.title_settings),
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "v${state.versionName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

data class TitleUiState(
    val hasSave: Boolean = false,
    val versionName: String = "0.1.0",
    val latestSlot: Int = 0,
)

@HiltViewModel
class TitleViewModel @Inject constructor(
    private val saves: SaveRepository,
    private val audio: AudioEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(TitleUiState())
    val state: StateFlow<TitleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val slots = saves.slots().filter { it.exists }
            val newest = slots.maxByOrNull { it.savedAtEpochMs }
            _state.value = TitleUiState(
                hasSave = newest != null,
                latestSlot = newest?.slotIndex ?: 0,
            )
        }
    }

    fun onEnter() {
        audio.playMusic(AudioKeys.TITLE)
    }

    /** Loads the most recently written slot. */
    fun continueGame(onLoaded: () -> Unit) {
        viewModelScope.launch {
            audio.playSound(AudioKeys.UI_CONFIRM)
            saves.load(_state.value.latestSlot).onSuccess { onLoaded() }
        }
    }
}
