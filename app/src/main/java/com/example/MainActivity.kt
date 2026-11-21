package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppScreen
import com.example.model.GameMode
import com.example.ui.components.GameViewport
import com.example.ui.screens.BikeSelectorScreen
import com.example.ui.screens.GameplayHudScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.IndianBikeTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndianBikeTheme {
                IndianBikeApp(
                    viewModel = viewModel,
                    onExitApp = { finishAffinity() }
                )
            }
        }
    }
}

@Composable
private fun IndianBikeApp(
    viewModel: GameViewModel,
    onExitApp: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val persistedState by viewModel.persistedState.collectAsStateWithLifecycle()
    val previewBike by viewModel.previewBike.collectAsStateWithLifecycle()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val statusToast by viewModel.statusToast.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusToast) {
        statusToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Persistent OpenGL ES 3D Viewport (Showroom turntable OR Open-World Simulation)
        GameViewport(
            renderer = viewModel.renderer,
            interactiveOrbit = currentScreen == AppScreen.SELECT_BIKE || currentScreen == AppScreen.GARAGE,
            onOrbitDelta = { delta -> viewModel.rotateShowroom(delta) }
        )

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.MAIN_MENU -> MainMenuScreen(
                    gameState = persistedState,
                    onNavigate = { viewModel.navigateTo(it) },
                    onPlayMode = { mode -> viewModel.launchGame(mode) },
                    onExitApp = onExitApp
                )
                AppScreen.SELECT_BIKE -> BikeSelectorScreen(
                    gameState = persistedState,
                    previewBike = previewBike,
                    onPreviewBike = { viewModel.setPreviewBike(it) },
                    onSelectOrUnlock = { viewModel.selectOrUnlockBike(it) },
                    onRevSound = { viewModel.previewEngineSound(it) },
                    onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                )
                AppScreen.GARAGE -> GarageScreen(
                    gameState = persistedState,
                    onUpdateColors = { bike, tank, rim -> viewModel.updateBikeColors(bike, tank, rim) },
                    onUpgradePart = { bike, part, cost -> viewModel.upgradeBikePart(bike, part, cost) },
                    onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                )
                AppScreen.MISSIONS -> MissionsScreen(
                    gameState = persistedState,
                    onStartMission = { mission -> viewModel.launchGame(GameMode.MISSION, mission) },
                    onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                )
                AppScreen.SETTINGS -> SettingsScreen(
                    settings = persistedState.settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                )
                AppScreen.GAMEPLAY -> GameplayHudScreen(
                    hud = hudState,
                    gameMode = viewModel.simulation.gameMode,
                    minimapRoads = viewModel.worldMap.minimapRoads,
                    minimapLandmarks = viewModel.worldMap.minimapLandmarks,
                    minimapBuildings = viewModel.worldMap.minimapBuildings,
                    railwayZ = viewModel.worldMap.railwayZ,
                    isPaused = isPaused,
                    onUpdateControls = { viewModel.updateControls(it) },
                    onToggleEngine = { viewModel.toggleEngine() },
                    onCycleHeadlight = { viewModel.cycleHeadlight() },
                    onCycleCamera = { viewModel.cycleCamera() },
                    onPauseToggle = { viewModel.togglePause(it) },
                    onRestart = { viewModel.restartSession() },
                    onExitToMenu = { viewModel.endFreeRideOrStuntAndCollect() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        )
    }
}
