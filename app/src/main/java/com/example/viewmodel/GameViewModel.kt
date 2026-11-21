package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.GamePreferencesRepository
import com.example.data.PersistedGameState
import com.example.engine3d.IndianWorldMap
import com.example.engine3d.SimulationEngine
import com.example.engine3d.WorldRenderer
import com.example.model.AppScreen
import com.example.model.BikeCustomization
import com.example.model.BikeId
import com.example.model.ControlInputState
import com.example.model.GameMode
import com.example.model.GameSettingsState
import com.example.model.MissionSpec
import com.example.model.PlayerHudState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GamePreferencesRepository(application.applicationContext)
    val soundEngine = SoundEngine()
    val worldMap = IndianWorldMap()
    val simulation = SimulationEngine(worldMap, soundEngine)
    val renderer = WorldRenderer(simulation, worldMap)

    val persistedState: StateFlow<PersistedGameState> = repository.gameStateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PersistedGameState())

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _hudState = MutableStateFlow(PlayerHudState())
    val hudState: StateFlow<PlayerHudState> = _hudState.asStateFlow()

    private val _previewBike = MutableStateFlow(BikeId.ROYAL_ENFIELD_BULLET)
    val previewBike: StateFlow<BikeId> = _previewBike.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    private var lastLaunchMode: GameMode = GameMode.FREE_RIDE
    private var lastLaunchMission: MissionSpec? = null

    init {
        soundEngine.start()

        simulation.onHudUpdate = { hud ->
            _hudState.value = hud
        }
        simulation.onSessionCompleted = { result, mission ->
            viewModelScope.launch {
                if (result.success) {
                    if (mission != null) {
                        repository.markMissionCompleted(mission.id, result.coinsEarned)
                    } else if (result.coinsEarned > 0) {
                        repository.addCoins(result.coinsEarned)
                    }
                }
                if (result.scoreAchieved > 0) {
                    repository.updateBestStuntScore(result.scoreAchieved)
                }
            }
        }

        viewModelScope.launch {
            persistedState.collect { state ->
                simulation.updateSettings(state.settings)
                if (_currentScreen.value == AppScreen.MAIN_MENU) {
                    _previewBike.value = state.selectedBike
                }
                syncShowroomBike(state)
            }
        }
    }

    private fun syncShowroomBike(state: PersistedGameState) {
        val bike = if (_currentScreen.value == AppScreen.SELECT_BIKE) _previewBike.value else state.selectedBike
        val custom = state.customizations[bike] ?: BikeCustomization(bike.defaultTankColorHex)
        renderer.showroomBikeId = bike
        renderer.showroomCustomization = custom
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.GAMEPLAY) {
            renderer.showroomMode = false
        } else {
            simulation.isPlaying = false
            simulation.isPaused = false
            _isPaused.value = false
            renderer.showroomMode = true
            renderer.showroomAutoRotate = screen != AppScreen.GARAGE
            if (screen == AppScreen.SELECT_BIKE) {
                _previewBike.value = persistedState.value.selectedBike
            }
            syncShowroomBike(persistedState.value)
        }
    }

    fun setPreviewBike(bikeId: BikeId) {
        _previewBike.value = bikeId
        val state = persistedState.value
        renderer.showroomBikeId = bikeId
        renderer.showroomCustomization = state.customizations[bikeId] ?: BikeCustomization(bikeId.defaultTankColorHex)
    }

    fun previewEngineSound(bikeId: BikeId) {
        soundEngine.triggerEngineRevPreview(bikeId)
    }

    fun selectOrUnlockBike(bikeId: BikeId) {
        val state = persistedState.value
        viewModelScope.launch {
            if (bikeId in state.unlockedBikes) {
                repository.selectBike(bikeId)
                _statusToast.value = "${bikeId.displayName} selected!"
            } else {
                val ok = repository.unlockAndSelectBike(bikeId, bikeId.unlockCost)
                if (ok) {
                    _statusToast.value = "${bikeId.displayName} unlocked!"
                    soundEngine.triggerEngineRevPreview(bikeId)
                } else {
                    _statusToast.value = "Need ${bikeId.unlockCost} Coins to unlock!"
                }
            }
        }
    }

    fun updateBikeColors(bikeId: BikeId, tankColorHex: Long, rimColorHex: Long) {
        viewModelScope.launch {
            repository.updateBikeColors(bikeId, tankColorHex, rimColorHex)
        }
    }

    fun upgradeBikePart(bikeId: BikeId, partType: String, cost: Int) {
        viewModelScope.launch {
            val ok = repository.upgradeBikePart(bikeId, partType, cost)
            _statusToast.value = if (ok) {
                "$partType upgraded!"
            } else {
                "Not enough coins ($cost needed)"
            }
        }
    }

    fun updateSettings(settings: GameSettingsState) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun launchGame(mode: GameMode, mission: MissionSpec? = null) {
        val state = persistedState.value
        val bike = state.selectedBike
        val custom = state.customizations[bike] ?: BikeCustomization(bike.defaultTankColorHex)
        lastLaunchMode = mode
        lastLaunchMission = mission
        _isPaused.value = false
        simulation.startSession(mode, bike, custom, state.settings, mission)
        navigateTo(AppScreen.GAMEPLAY)
    }

    fun restartSession() {
        launchGame(lastLaunchMode, lastLaunchMission)
    }

    fun togglePause(paused: Boolean) {
        _isPaused.value = paused
        simulation.isPaused = paused
    }

    fun updateControls(transform: (ControlInputState) -> ControlInputState) {
        simulation.input = transform(simulation.input)
    }

    fun toggleEngine() = simulation.toggleEngine()
    fun cycleHeadlight() = simulation.cycleHeadlight()
    fun cycleCamera() = simulation.cycleCamera()

    fun endFreeRideOrStuntAndCollect() {
        val pts = simulation.stuntScore
        if (pts > 0) {
            val bonusCoins = (pts / 10).coerceIn(10, 1500)
            viewModelScope.launch {
                repository.addCoins(bonusCoins)
                repository.updateBestStuntScore(pts)
            }
        }
        navigateTo(AppScreen.MAIN_MENU)
    }

    fun rotateShowroom(deltaDeg: Float) {
        renderer.showroomAutoRotate = false
        renderer.showroomOrbitDeg = (renderer.showroomOrbitDeg + deltaDeg + 360f) % 360f
    }

    fun clearToast() {
        _statusToast.value = null
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.stop()
    }
}
