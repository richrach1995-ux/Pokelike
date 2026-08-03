package com.runeveil.saga

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.runeveil.saga.data.i18n.LocalizationRepository
import com.runeveil.saga.domain.repository.ContentRepository
import com.runeveil.saga.domain.repository.GameSettings
import com.runeveil.saga.domain.repository.SettingsRepository
import com.runeveil.saga.navigation.RuneveilNavHost
import com.runeveil.saga.ui.components.LoadingScreen
import com.runeveil.saga.ui.components.ProvideContentStrings
import com.runeveil.saga.ui.theme.MotionSettings
import com.runeveil.saga.ui.theme.RuneveilTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The single activity of the game.
 *
 * It keeps the splash screen on screen until the content pack and the
 * localisation table are parsed, so the player never sees an empty frame.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: BootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { !viewModel.ready.value }

        setContent {
            val ready by viewModel.ready.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            RuneveilTheme(
                darkTheme = true,
                highContrast = settings.highContrast,
                motion = MotionSettings(
                    reducedMotion = settings.reducedMotion,
                    screenShake = settings.screenShake,
                    battleSpeed = settings.battleAnimationSpeed,
                ),
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val failure by viewModel.bootFailure.collectAsStateWithLifecycle()
                    when {
                        failure != null -> BootFailureScreen(
                            title = getString(R.string.boot_failed_title),
                            hint = getString(R.string.boot_failed_hint),
                            detail = failure.orEmpty(),
                        )

                        !ready -> LoadingScreen(message = getString(R.string.common_loading))

                        else -> ProvideContentStrings(resolver = viewModel::text) {
                            RuneveilNavHost(navController = rememberNavController())
                        }
                    }
                }
            }
        }
    }
}

/**
 * Boots the game: parses the content assets and the localisation table once,
 * and exposes the settings the theme needs.
 */
@HiltViewModel
class BootViewModel @Inject constructor(
    private val content: ContentRepository,
    private val localization: LocalizationRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _bootFailure = MutableStateFlow<String?>(null)

    /** Non-null when booting failed; carries the message shown on screen. */
    val bootFailure: StateFlow<String?> = _bootFailure.asStateFlow()

    val settings: StateFlow<GameSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    init {
        viewModelScope.launch {
            // An exception escaping here would be an unhandled coroutine failure
            // and would take the whole process down without a word — exactly the
            // "app closes immediately" symptom. A broken content pack must show
            // a readable message instead.
            try {
                val language = settingsRepository.settings().language
                localization.load(language)
                content.ensureLoaded()
            } catch (failure: Throwable) {
                if (failure is CancellationException) throw failure
                Log.e(TAG, "Start fehlgeschlagen", failure)
                _bootFailure.value = failure.toReadableText()
            }
            _ready.value = true
        }
    }

    /** Class name plus message plus the first frames — enough to act on. */
    private fun Throwable.toReadableText(): String = buildString {
        appendLine("${this@toReadableText::class.java.name}: ${message.orEmpty()}")
        stackTrace.take(STACK_FRAMES_SHOWN).forEach { frame -> appendLine("  at $frame") }
        cause?.let { appendLine("Ursache: ${it::class.java.name}: ${it.message.orEmpty()}") }
    }.trim()

    /** Resolver handed to the composition for content strings. */
    fun text(key: String): String = localization[key]

    private companion object {
        const val TAG = "Runeveil"
        const val STACK_FRAMES_SHOWN = 12
    }
}

/**
 * Shown when the game cannot boot. Its whole purpose is to replace a silent
 * process death with something the player can read out or send in, so it uses
 * no content strings — those may be exactly what failed to load.
 */
@Composable
private fun BootFailureScreen(title: String, hint: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
