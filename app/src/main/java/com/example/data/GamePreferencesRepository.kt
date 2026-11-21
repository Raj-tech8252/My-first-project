package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.BikeCustomization
import com.example.model.BikeId
import com.example.model.GameSettingsState
import com.example.model.GraphicsQuality
import com.example.model.TimeOfDayMode
import com.example.model.WeatherMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "indian_bike_3d_prefs")

data class PersistedGameState(
    val coins: Int = 3500,
    val selectedBike: BikeId = BikeId.ROYAL_ENFIELD_BULLET,
    val unlockedBikes: Set<BikeId> = BikeId.entries.filter { it.defaultUnlocked }.toSet(),
    val customizations: Map<BikeId, BikeCustomization> = BikeId.entries.associateWith {
        BikeCustomization(tankColorHex = it.defaultTankColorHex)
    },
    val completedMissions: Set<Int> = emptySet(),
    val bestStuntScore: Int = 0,
    val settings: GameSettingsState = GameSettingsState()
)

class GamePreferencesRepository(private val context: Context) {

    private object Keys {
        val COINS = intPreferencesKey("coins")
        val SELECTED_BIKE = stringPreferencesKey("selected_bike")
        val UNLOCKED_BIKES = stringSetPreferencesKey("unlocked_bikes")
        val COMPLETED_MISSIONS = stringSetPreferencesKey("completed_missions")
        val BEST_STUNT_SCORE = intPreferencesKey("best_stunt_score")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val STEERING_SENSITIVITY = floatPreferencesKey("steering_sensitivity")
        val TIME_OF_DAY = stringPreferencesKey("time_of_day")
        val WEATHER_MODE = stringPreferencesKey("weather_mode")
    }

    private fun bikeTankKey(bikeId: BikeId) = stringPreferencesKey("bike_${bikeId.name}_tank")
    private fun bikeRimKey(bikeId: BikeId) = stringPreferencesKey("bike_${bikeId.name}_rim")
    private fun bikeEngineKey(bikeId: BikeId) = intPreferencesKey("bike_${bikeId.name}_eng")
    private fun bikeNitroKey(bikeId: BikeId) = intPreferencesKey("bike_${bikeId.name}_nos")
    private fun bikeSuspKey(bikeId: BikeId) = intPreferencesKey("bike_${bikeId.name}_susp")

    val gameStateFlow: Flow<PersistedGameState> = context.gameDataStore.data.map { prefs ->
        val defaultUnlocked = BikeId.entries.filter { it.defaultUnlocked }.toSet()
        val unlockedNames = prefs[Keys.UNLOCKED_BIKES]
        val unlockedBikes = if (unlockedNames == null) {
            defaultUnlocked
        } else {
            unlockedNames.mapNotNull { name -> runCatching { BikeId.valueOf(name) }.getOrNull() }.toSet() + defaultUnlocked
        }

        val selectedBike = prefs[Keys.SELECTED_BIKE]?.let { name ->
            runCatching { BikeId.valueOf(name) }.getOrNull()
        } ?: BikeId.ROYAL_ENFIELD_BULLET

        val customizations = BikeId.entries.associateWith { bike ->
            val tank = prefs[bikeTankKey(bike)]?.toLongOrNull() ?: bike.defaultTankColorHex
            val rim = prefs[bikeRimKey(bike)]?.toLongOrNull() ?: 0xFFD8DEE9
            val eng = (prefs[bikeEngineKey(bike)] ?: 1).coerceIn(1, 3)
            val nos = (prefs[bikeNitroKey(bike)] ?: 1).coerceIn(1, 3)
            val susp = (prefs[bikeSuspKey(bike)] ?: 1).coerceIn(1, 3)
            BikeCustomization(tank, rim, eng, nos, susp)
        }

        val completedMissions = prefs[Keys.COMPLETED_MISSIONS]
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()

        val graphics = prefs[Keys.GRAPHICS_QUALITY]?.let {
            runCatching { GraphicsQuality.valueOf(it) }.getOrNull()
        } ?: GraphicsQuality.MEDIUM

        val timeMode = prefs[Keys.TIME_OF_DAY]?.let {
            runCatching { TimeOfDayMode.valueOf(it) }.getOrNull()
        } ?: TimeOfDayMode.AUTO

        val weatherMode = prefs[Keys.WEATHER_MODE]?.let {
            runCatching { WeatherMode.valueOf(it) }.getOrNull()
        } ?: WeatherMode.AUTO

        PersistedGameState(
            coins = prefs[Keys.COINS] ?: 3500,
            selectedBike = selectedBike,
            unlockedBikes = unlockedBikes,
            customizations = customizations,
            completedMissions = completedMissions,
            bestStuntScore = prefs[Keys.BEST_STUNT_SCORE] ?: 0,
            settings = GameSettingsState(
                graphicsQuality = graphics,
                soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
                soundVolume = prefs[Keys.SOUND_VOLUME] ?: 0.85f,
                steeringSensitivity = prefs[Keys.STEERING_SENSITIVITY] ?: 1.0f,
                timeOfDayMode = timeMode,
                weatherMode = weatherMode
            )
        )
    }

    suspend fun selectBike(bikeId: BikeId) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.SELECTED_BIKE] = bikeId.name
        }
    }

    suspend fun unlockAndSelectBike(bikeId: BikeId, cost: Int): Boolean {
        var success = false
        context.gameDataStore.edit { prefs ->
            val currentCoins = prefs[Keys.COINS] ?: 3500
            if (currentCoins >= cost) {
                prefs[Keys.COINS] = currentCoins - cost
                val existing = prefs[Keys.UNLOCKED_BIKES] ?: emptySet()
                prefs[Keys.UNLOCKED_BIKES] = existing + bikeId.name
                prefs[Keys.SELECTED_BIKE] = bikeId.name
                success = true
            }
        }
        return success
    }

    suspend fun updateBikeColors(bikeId: BikeId, tankColorHex: Long, rimColorHex: Long) {
        context.gameDataStore.edit { prefs ->
            prefs[bikeTankKey(bikeId)] = tankColorHex.toString()
            prefs[bikeRimKey(bikeId)] = rimColorHex.toString()
        }
    }

    suspend fun upgradeBikePart(bikeId: BikeId, partType: String, cost: Int): Boolean {
        var upgraded = false
        context.gameDataStore.edit { prefs ->
            val coins = prefs[Keys.COINS] ?: 3500
            val key = when (partType) {
                "ENGINE" -> bikeEngineKey(bikeId)
                "NITRO" -> bikeNitroKey(bikeId)
                else -> bikeSuspKey(bikeId)
            }
            val currentLevel = (prefs[key] ?: 1).coerceIn(1, 3)
            if (currentLevel < 3 && coins >= cost) {
                prefs[Keys.COINS] = coins - cost
                prefs[key] = currentLevel + 1
                upgraded = true
            }
        }
        return upgraded
    }

    suspend fun addCoins(amount: Int) {
        if (amount <= 0) return
        context.gameDataStore.edit { prefs ->
            val current = prefs[Keys.COINS] ?: 3500
            prefs[Keys.COINS] = current + amount
        }
    }

    suspend fun markMissionCompleted(missionId: Int, rewardCoins: Int) {
        context.gameDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_MISSIONS] ?: emptySet()
            prefs[Keys.COMPLETED_MISSIONS] = current + missionId.toString()
            val coins = prefs[Keys.COINS] ?: 3500
            prefs[Keys.COINS] = coins + rewardCoins
        }
    }

    suspend fun updateBestStuntScore(score: Int) {
        context.gameDataStore.edit { prefs ->
            val prev = prefs[Keys.BEST_STUNT_SCORE] ?: 0
            if (score > prev) {
                prefs[Keys.BEST_STUNT_SCORE] = score
            }
        }
    }

    suspend fun updateSettings(settings: GameSettingsState) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.GRAPHICS_QUALITY] = settings.graphicsQuality.name
            prefs[Keys.SOUND_ENABLED] = settings.soundEnabled
            prefs[Keys.SOUND_VOLUME] = settings.soundVolume
            prefs[Keys.STEERING_SENSITIVITY] = settings.steeringSensitivity
            prefs[Keys.TIME_OF_DAY] = settings.timeOfDayMode.name
            prefs[Keys.WEATHER_MODE] = settings.weatherMode.name
        }
    }
}
