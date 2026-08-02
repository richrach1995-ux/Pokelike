package com.runeveil.saga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    private val viewModel: BootViewModel by androidx.activity.viewModels()

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
                    if (!ready) {
                        LoadingScreen(message = getString(R.string.common_loading))
                    } else {
                        ProvideContentStrings(resolver = viewModel::text) {
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

    val settings: StateFlow<GameSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    init {
        viewModelScope.launch {
            val language = settingsRepository.settings().language
            localization.load(language)
            content.ensureLoaded()
            _ready.value = true
        }
    }

    /** Resolver handed to the composition for content strings. */
    fun text(key: String): String = localization[key]
}
