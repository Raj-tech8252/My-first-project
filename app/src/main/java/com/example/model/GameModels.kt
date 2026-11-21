package com.example.model

import androidx.compose.ui.graphics.Color

enum class BikeStyle {
    CRUISER,
    STREET_NAKED,
    SPORT_COMMUTER,
    TRACK_NAKED,
    CLASSIC_COMMUTER,
    SUPERSPORT_FAIRING,
    REFINED_COMMUTER
}

enum class BikeId(
    val displayName: String,
    val subtitle: String,
    val ccLabel: String,
    val style: BikeStyle,
    val topSpeedKmh: Float,
    val acceleration: Float,      // m/s^2 base
    val handling: Float,          // turn responsiveness
    val braking: Float,           // deceleration m/s^2
    val suspensionStiffness: Float,
    val baseFreqHz: Float,        // engine idle fundamental Hz
    val maxFreqHz: Float,         // redline fundamental Hz
    val thumpFactor: Float,       // 0f..1f (Bullet thump vs R15 whine)
    val defaultTankColorHex: Long,
    val unlockCost: Int,
    val defaultUnlocked: Boolean
) {
    ROYAL_ENFIELD_BULLET(
        displayName = "Royal Enfield Bullet",
        subtitle = "350cc Classic Thumper",
        ccLabel = "349 CC",
        style = BikeStyle.CRUISER,
        topSpeedKmh = 136f,
        acceleration = 11.5f,
        handling = 0.78f,
        braking = 19.0f,
        suspensionStiffness = 0.85f,
        baseFreqHz = 28f,
        maxFreqHz = 112f,
        thumpFactor = 0.95f,
        defaultTankColorHex = 0xFF1A1D24,
        unlockCost = 0,
        defaultUnlocked = true
    ),
    HERO_SPLENDOR(
        displayName = "Hero Splendor",
        subtitle = "Bharat Mileage Legend",
        ccLabel = "97 CC",
        style = BikeStyle.CLASSIC_COMMUTER,
        topSpeedKmh = 108f,
        acceleration = 10.2f,
        handling = 0.96f,
        braking = 18.0f,
        suspensionStiffness = 0.80f,
        baseFreqHz = 36f,
        maxFreqHz = 135f,
        thumpFactor = 0.35f,
        defaultTankColorHex = 0xFF1C3D8C,
        unlockCost = 0,
        defaultUnlocked = true
    ),
    HONDA_SHINE(
        displayName = "Honda Shine",
        subtitle = "Refined City Commuter",
        ccLabel = "124 CC",
        style = BikeStyle.REFINED_COMMUTER,
        topSpeedKmh = 116f,
        acceleration = 11.0f,
        handling = 0.94f,
        braking = 19.5f,
        suspensionStiffness = 0.84f,
        baseFreqHz = 38f,
        maxFreqHz = 142f,
        thumpFactor = 0.25f,
        defaultTankColorHex = 0xFFB71C1C,
        unlockCost = 0,
        defaultUnlocked = true
    ),
    BAJAJ_PULSAR(
        displayName = "Bajaj Pulsar",
        subtitle = "220F Wolf-Eye Sport",
        ccLabel = "220 CC",
        style = BikeStyle.SPORT_COMMUTER,
        topSpeedKmh = 148f,
        acceleration = 13.8f,
        handling = 0.88f,
        braking = 22.0f,
        suspensionStiffness = 0.92f,
        baseFreqHz = 42f,
        maxFreqHz = 168f,
        thumpFactor = 0.45f,
        defaultTankColorHex = 0xFF1565C0,
        unlockCost = 0,
        defaultUnlocked = true
    ),
    TVS_APACHE(
        displayName = "TVS Apache",
        subtitle = "RTR Track Naked",
        ccLabel = "200 CC",
        style = BikeStyle.TRACK_NAKED,
        topSpeedKmh = 158f,
        acceleration = 15.0f,
        handling = 0.95f,
        braking = 24.0f,
        suspensionStiffness = 0.98f,
        baseFreqHz = 46f,
        maxFreqHz = 184f,
        thumpFactor = 0.32f,
        defaultTankColorHex = 0xFFD50000,
        unlockCost = 1400,
        defaultUnlocked = false
    ),
    KTM_DUKE(
        displayName = "KTM Duke",
        subtitle = "390 Streetfighter",
        ccLabel = "373 CC",
        style = BikeStyle.STREET_NAKED,
        topSpeedKmh = 172f,
        acceleration = 17.6f,
        handling = 1.00f,
        braking = 26.5f,
        suspensionStiffness = 1.05f,
        baseFreqHz = 48f,
        maxFreqHz = 196f,
        thumpFactor = 0.50f,
        defaultTankColorHex = 0xFFFF6D00,
        unlockCost = 1800,
        defaultUnlocked = false
    ),
    YAMAHA_R15(
        displayName = "Yamaha R15",
        subtitle = "V4 Supersport Fairing",
        ccLabel = "155 CC",
        style = BikeStyle.SUPERSPORT_FAIRING,
        topSpeedKmh = 180f,
        acceleration = 16.5f,
        handling = 0.93f,
        braking = 27.0f,
        suspensionStiffness = 1.08f,
        baseFreqHz = 54f,
        maxFreqHz = 228f,
        thumpFactor = 0.14f,
        defaultTankColorHex = 0xFF0040CC,
        unlockCost = 2400,
        defaultUnlocked = false
    );

    fun speedRating(): Float = (topSpeedKmh / 185f).coerceIn(0.2f, 1f)
    fun accelRating(): Float = (acceleration / 18.5f).coerceIn(0.2f, 1f)
    fun handlingRating(): Float = handling.coerceIn(0.2f, 1f)
    fun brakingRating(): Float = (braking / 28f).coerceIn(0.2f, 1f)
}

data class PaintOption(
    val name: String,
    val colorHex: Long
) {
    val composeColor: Color get() = Color(colorHex)
    fun r(): Float = ((colorHex shr 16) and 0xFF) / 255f
    fun g(): Float = ((colorHex shr 8) and 0xFF) / 255f
    fun b(): Float = (colorHex and 0xFF) / 255f
}

val BikePaintPalette = listOf(
    PaintOption("Saffron Blaze", 0xFFFF6D00),
    PaintOption("Royal Jet Black", 0xFF1A1D24),
    PaintOption("Crimson Fury", 0xFFD50000),
    PaintOption("Bharat Blue", 0xFF0052CC),
    PaintOption("Pearl White", 0xFFF2F4F8),
    PaintOption("Emerald Green", 0xFF00894B),
    PaintOption("Golden Taxi", 0xFFFFB300),
    PaintOption("Matte Titanium", 0xFF546E7A)
)

val RimAccentPalette = listOf(
    PaintOption("Chrome Silver", 0xFFD8DEE9),
    PaintOption("KTM Orange", 0xFFFF6D00),
    PaintOption("Gold Anodized", 0xFFFFC107),
    PaintOption("Electric Cyan", 0xFF00E5FF),
    PaintOption("Racing Red", 0xFFFF1744),
    PaintOption("Stealth Black", 0xFF20252F)
)

data class BikeCustomization(
    val tankColorHex: Long,
    val rimColorHex: Long = 0xFFD8DEE9,
    val engineLevel: Int = 1,      // 1..3
    val nitroLevel: Int = 1,       // 1..3
    val suspensionLevel: Int = 1   // 1..3
)

enum class GraphicsQuality(
    val label: String,
    val drawDistance: Float,
    val trafficCount: Int,
    val pedestrianCount: Int,
    val rainParticles: Int,
    val enableReflections: Boolean,
    val enableDynamicHeadlight: Boolean
) {
    LOW("Low", 190f, 12, 8, 90, false, false),
    MEDIUM("Medium", 290f, 20, 14, 180, true, true),
    HIGH("High", 420f, 30, 22, 300, true, true)
}

enum class TimeOfDayMode(val label: String) {
    AUTO("Auto Cycle"),
    DAY("Midday Sun"),
    SUNSET("Golden Sunset"),
    NIGHT("City Night")
}

enum class WeatherMode(val label: String) {
    AUTO("Dynamic"),
    CLEAR("Clear Sky"),
    RAIN("Monsoon Rain"),
    FOG("Morning Mist")
}

enum class GameMode(val label: String, val description: String) {
    FREE_RIDE("Free Ride", "Explore Indian highways, city markets, village trails, temple & railway crossing."),
    RACING("Racing Mode", "Compete against rival riders through the NH-44 & City Bazaar circuit."),
    STUNT("Bike Stunt Mode", "Hit mega ramps, hold wheelies, and pull traffic near-misses for combos."),
    MISSION("Mission Mode", "Complete timed objectives across Bharat landmarks for coin rewards.")
}

enum class CameraMode(val label: String) {
    CHASE_NEAR("Chase"),
    CHASE_FAR("Wide"),
    FIRST_PERSON("Rider POV")
}

data class MissionSpec(
    val id: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val startX: Float,
    val startZ: Float,
    val startHeadingDeg: Float,
    val targetX: Float,
    val targetZ: Float,
    val timeLimitSec: Float,
    val rewardCoins: Int,
    val targetSpeedKmh: Float = 0f,
    val targetStuntPoints: Int = 0,
    val noCrashRequired: Boolean = false,
    val startWanted: Boolean = false
)

val AllMissions = listOf(
    MissionSpec(
        id = 1,
        title = "Village Milk & Chai Run",
        subtitle = "Village -> Market Bazaar",
        description = "Deliver fresh canisters from the Indian Village well to the City Bazaar before the timer ends.",
        startX = -290f, startZ = 40f, startHeadingDeg = 90f,
        targetX = 45f, targetZ = 20f,
        timeLimitSec = 65f,
        rewardCoins = 450
    ),
    MissionSpec(
        id = 2,
        title = "Beat the Express Phatak",
        subtitle = "Highway -> Petrol Pump",
        description = "Cross the Railway Crossing before the express train arrives and refuel at Bharat Petrol Pump.",
        startX = -40f, startZ = -340f, startHeadingDeg = 0f,
        targetX = 115f, targetZ = -105f,
        timeLimitSec = 55f,
        rewardCoins = 600
    ),
    MissionSpec(
        id = 3,
        title = "NH-44 Highway Blitz",
        subtitle = "Top Speed Challenge",
        description = "Hit at least 120 km/h on the National Highway and reach the Northern Highway Gantry.",
        startX = 0f, startZ = 180f, startHeadingDeg = 180f,
        targetX = 0f, targetZ = -380f,
        timeLimitSec = 60f,
        rewardCoins = 750,
        targetSpeedKmh = 120f
    ),
    MissionSpec(
        id = 4,
        title = "Temple Sunset Pilgrimage",
        subtitle = "School -> Hilltop Temple",
        description = "Ride carefully from the Government School to the Sandstone Temple without crashing into traffic.",
        startX = -125f, startZ = 115f, startHeadingDeg = 90f,
        targetX = 330f, targetZ = 240f,
        timeLimitSec = 75f,
        rewardCoins = 900,
        noCrashRequired = true
    ),
    MissionSpec(
        id = 5,
        title = "Stunt King of Bharat",
        subtitle = "Stunt Park Mastery",
        description = "Score 1,200 stunt points using ramps, wheelies, and traffic near-misses around the Stunt Arena.",
        startX = 220f, startZ = -260f, startHeadingDeg = 90f,
        targetX = 300f, targetZ = -320f,
        timeLimitSec = 85f,
        rewardCoins = 1100,
        targetStuntPoints = 1200
    ),
    MissionSpec(
        id = 6,
        title = "PCR Pursuit Escape",
        subtitle = "City -> Village Safe House",
        description = "Outrun the Indian Police PCR units from City Center back to the Village Safe House!",
        startX = 10f, startZ = -10f, startHeadingDeg = 270f,
        targetX = -320f, targetZ = -120f,
        timeLimitSec = 70f,
        rewardCoins = 1400,
        startWanted = true
    )
)

enum class AppScreen {
    MAIN_MENU,
    SELECT_BIKE,
    GARAGE,
    MISSIONS,
    SETTINGS,
    GAMEPLAY
}

data class ControlInputState(
    val throttle: Boolean = false,
    val brake: Boolean = false,
    val steerLeft: Boolean = false,
    val steerRight: Boolean = false,
    val nitro: Boolean = false,
    val horn: Boolean = false,
    val wheelie: Boolean = false
)

data class MinimapDot(
    val x: Float,
    val z: Float,
    val type: Int // 0=traffic, 1=police, 2=rival, 3=target
)

data class PlayerHudState(
    val speedKmh: Int = 0,
    val rpmNormalized: Float = 0f,
    val gear: Int = 1,
    val engineRunning: Boolean = true,
    val headlightOn: Boolean = false,
    val highBeam: Boolean = false,
    val nitroPercent: Float = 1f,
    val fuelPercent: Float = 1f,
    val cameraMode: CameraMode = CameraMode.CHASE_NEAR,
    val zoneName: String = "City Center",
    val timeLabel: String = "Day",
    val weatherLabel: String = "Clear",
    val stuntScore: Int = 0,
    val lastStuntLabel: String = "",
    val comboMultiplier: Int = 1,
    val airTimeSec: Float = 0f,
    val policeWantedLevel: Int = 0,
    val nearTrainWarning: Boolean = false,
    val refuelBanner: Boolean = false,
    val crashWarning: String = "",
    val missionTitle: String = "",
    val missionObjective: String = "",
    val missionTimeLeftSec: Float = 0f,
    val missionDistanceMeters: Int = 0,
    val raceCheckpointIndex: Int = 0,
    val raceTotalCheckpoints: Int = 8,
    val racePosition: Int = 1,
    val raceTotalRacers: Int = 4,
    val resultState: SessionResult? = null,
    val playerX: Float = 0f,
    val playerZ: Float = 0f,
    val playerHeadingDeg: Float = 0f,
    val targetX: Float? = null,
    val targetZ: Float? = null,
    val minimapDots: List<MinimapDot> = emptyList()
)

data class SessionResult(
    val success: Boolean,
    val title: String,
    val subtitle: String,
    val coinsEarned: Int,
    val scoreAchieved: Int
)

data class GameSettingsState(
    val graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.85f,
    val steeringSensitivity: Float = 1.0f,
    val timeOfDayMode: TimeOfDayMode = TimeOfDayMode.AUTO,
    val weatherMode: WeatherMode = WeatherMode.AUTO
)
