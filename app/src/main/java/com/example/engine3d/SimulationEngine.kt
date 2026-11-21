package com.example.engine3d

import com.example.audio.SoundEngine
import com.example.model.BikeCustomization
import com.example.model.BikeId
import com.example.model.CameraMode
import com.example.model.ControlInputState
import com.example.model.GameMode
import com.example.model.GameSettingsState
import com.example.model.GraphicsQuality
import com.example.model.MinimapDot
import com.example.model.MissionSpec
import com.example.model.PlayerHudState
import com.example.model.SessionResult
import com.example.model.TimeOfDayMode
import com.example.model.WeatherMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class TrafficAgent(
    var x: Float,
    var z: Float,
    var headingDeg: Float,
    var speedMs: Float,
    val baseSpeedMs: Float,
    val type: TrafficVehicleType,
    val bodyR: Float,
    val bodyG: Float,
    val bodyB: Float,
    val axisAlongZ: Boolean,
    val laneCoord: Float,
    val dirSign: Float,
    var wheelAngle: Float = 0f,
    var isPursuing: Boolean = false
) {
    val halfWidth: Float
        get() = when (type) {
            TrafficVehicleType.AUTO_RICKSHAW -> 0.72f
            TrafficVehicleType.TRACTOR -> 0.88f
            TrafficVehicleType.BUS -> 1.28f
            TrafficVehicleType.TRUCK -> 1.25f
            else -> 0.96f
        }
    val halfLength: Float
        get() = when (type) {
            TrafficVehicleType.AUTO_RICKSHAW -> 1.15f
            TrafficVehicleType.TRACTOR -> 1.55f
            TrafficVehicleType.BUS -> 4.10f
            TrafficVehicleType.TRUCK -> 3.50f
            else -> 2.15f
        }
}

data class PedestrianAgent(
    var x: Float,
    var z: Float,
    var headingDeg: Float,
    var speedMs: Float,
    var walkPhase: Float,
    val shirtR: Float,
    val shirtG: Float,
    val shirtB: Float,
    val minCoord: Float,
    val maxCoord: Float,
    val alongZ: Boolean
)

data class RivalBikerAgent(
    var x: Float,
    var z: Float,
    var headingDeg: Float,
    var speedMs: Float,
    var leanDeg: Float,
    var wheelAngle: Float,
    var checkpointIdx: Int,
    var lap: Int,
    val bikeId: BikeId,
    val r: Float,
    val g: Float,
    val b: Float
)

data class RainDrop(
    var x: Float,
    var y: Float,
    var z: Float,
    val speed: Float
)

class SimulationEngine(
    private val worldMap: IndianWorldMap,
    private val soundEngine: SoundEngine
) {
    @Volatile var input = ControlInputState()
    @Volatile var settings = GameSettingsState()
    @Volatile var activeBikeId = BikeId.ROYAL_ENFIELD_BULLET
    @Volatile var activeCustomization = BikeCustomization(BikeId.ROYAL_ENFIELD_BULLET.defaultTankColorHex)
    @Volatile var isPlaying = false
    @Volatile var isPaused = false

    // Player physics state
    var playerX = 0f
    var playerY = 0f
    var playerZ = 60f
    var playerVelY = 0f
    var playerSpeedMs = 0f
    var playerHeadingDeg = 180f // Facing South along NH-44 initially or North (180 = -Z toward City/Highway)
    var playerSteerAngleDeg = 0f
    var playerLeanDeg = 0f
    var playerPitchDeg = 0f
    var wheelSpinDeg = 0f
    var suspensionOffset = 0f
    var suspensionVelocity = 0f
    var prevRampHeight = 0f
    var airborne = false
    var airTimeSec = 0f
    var wheelieAngleDeg = 0f

    var engineRunning = true
    var headlightMode = 0 // 0=Off, 1=Low, 2=High
    var nitroAmount = 1.0f
    var fuelAmount = 1.0f
    var cameraMode = CameraMode.CHASE_NEAR
    var camPosX = 0f
    var camPosY = 3.5f
    var camPosZ = 70f
    var camShake = 0f

    // Environment state
    var dayClock = 0.34f // 0..1 (0.25=dawn, 0.45=day, 0.72=sunset, 0.90=night)
    var weatherCycleTimer = 0f
    var currentWeather = WeatherMode.CLEAR
    var rainAmount = 0f
    var fogAmount = 0f

    // Railway train state
    var trainX = -480f
    var trainTimer = 14f
    var trainActive = false
    var gateClosedFactor = 0f // 0=open, 1=closed

    // Mode & Mission state
    var gameMode = GameMode.FREE_RIDE
    var activeMission: MissionSpec? = null
    var missionTimeLeft = 0f
    var missionMaxSpeedReached = 0f
    var missionCrashed = false
    var targetX: Float? = null
    var targetZ: Float? = null
    var sessionResult: SessionResult? = null
    var sessionRewardClaimed = false

    // Stunt & Police state
    var stuntScore = 0
    var comboMultiplier = 1
    var comboTimer = 0f
    var lastStuntLabel = ""
    var lastStuntBannerTimer = 0f
    var crashBannerTimer = 0f
    var crashBannerText = ""
    var policeWantedLevel = 0
    var wantedDecayTimer = 0f
    var nearMissCooldown = 0f
    var redLightCooldown = 0f

    // Racing Mode
    private val raceCheckpoints = listOf(
        Pair(0f, -80f),
        Pair(0f, -350f),
        Pair(280f, -365f),
        Pair(280f, -120f),
        Pair(280f, 120f),
        Pair(120f, 120f),
        Pair(0f, 120f),
        Pair(0f, 20f)
    )
    var raceCheckpointIndex = 0
    val rivals = mutableListOf<RivalBikerAgent>()

    // Agents
    val traffic = mutableListOf<TrafficAgent>()
    val pedestrians = mutableListOf<PedestrianAgent>()
    val rainDrops = mutableListOf<RainDrop>()

    private var hudEmitTimer = 0f
    private val rng = Random(2026)

    var onHudUpdate: ((PlayerHudState) -> Unit)? = null
    var onSessionCompleted: ((SessionResult, MissionSpec?) -> Unit)? = null

    fun startSession(
        mode: GameMode,
        bikeId: BikeId,
        customization: BikeCustomization,
        gameSettings: GameSettingsState,
        mission: MissionSpec? = null
    ) {
        activeBikeId = bikeId
        activeCustomization = customization
        settings = gameSettings
        gameMode = mode
        activeMission = mission
        sessionResult = null
        sessionRewardClaimed = false
        isPaused = false
        isPlaying = true

        // Reset player vehicle
        playerVelY = 0f
        playerSpeedMs = 0f
        playerSteerAngleDeg = 0f
        playerLeanDeg = 0f
        playerPitchDeg = 0f
        suspensionOffset = 0f
        suspensionVelocity = 0f
        airborne = false
        airTimeSec = 0f
        wheelieAngleDeg = 0f
        engineRunning = true
        nitroAmount = 1.0f
        fuelAmount = 1.0f
        stuntScore = 0
        comboMultiplier = 1
        comboTimer = 0f
        lastStuntLabel = ""
        lastStuntBannerTimer = 0f
        crashBannerTimer = 0f
        policeWantedLevel = 0
        wantedDecayTimer = 0f
        missionMaxSpeedReached = 0f
        missionCrashed = false

        when (mode) {
            GameMode.FREE_RIDE -> {
                playerX = 0f; playerZ = 65f; playerHeadingDeg = 180f
                targetX = null; targetZ = null
            }
            GameMode.STUNT -> {
                // Spawn facing the Stunt Arena mega ramps
                playerX = 265f; playerZ = -245f; playerHeadingDeg = 180f
                targetX = 295f; targetZ = -325f
            }
            GameMode.RACING -> {
                playerX = -3f; playerZ = 35f; playerHeadingDeg = 180f
                raceCheckpointIndex = 0
                targetX = raceCheckpoints[0].first
                targetZ = raceCheckpoints[0].second
                spawnRivals()
            }
            GameMode.MISSION -> {
                val m = mission ?: com.example.model.AllMissions.first()
                activeMission = m
                playerX = m.startX
                playerZ = m.startZ
                playerHeadingDeg = m.startHeadingDeg
                targetX = m.targetX
                targetZ = m.targetZ
                missionTimeLeft = m.timeLimitSec
                if (m.startWanted) {
                    policeWantedLevel = 2
                    wantedDecayTimer = 25f
                }
            }
        }

        playerY = worldMap.getRampHeightAt(playerX, playerZ)
        prevRampHeight = playerY
        val rad = playerHeadingDeg * PI.toFloat() / 180f
        camPosX = playerX - sin(rad) * 7f
        camPosY = 3.2f
        camPosZ = playerZ - cos(rad) * 7f

        // Set initial time & weather
        dayClock = when (settings.timeOfDayMode) {
            TimeOfDayMode.DAY -> 0.48f
            TimeOfDayMode.SUNSET -> 0.72f
            TimeOfDayMode.NIGHT -> 0.92f
            TimeOfDayMode.AUTO -> 0.42f
        }
        currentWeather = when (settings.weatherMode) {
            WeatherMode.AUTO -> WeatherMode.CLEAR
            else -> settings.weatherMode
        }

        spawnTrafficAndPedestrians()
        spawnRainDrops()
        emitHud()
    }

    fun toggleEngine() {
        engineRunning = !engineRunning
        if (engineRunning) {
            soundEngine.triggerStarter()
        }
        emitHud()
    }

    fun cycleHeadlight() {
        headlightMode = (headlightMode + 1) % 3
        emitHud()
    }

    fun cycleCamera() {
        cameraMode = when (cameraMode) {
            CameraMode.CHASE_NEAR -> CameraMode.CHASE_FAR
            CameraMode.CHASE_FAR -> CameraMode.FIRST_PERSON
            CameraMode.FIRST_PERSON -> CameraMode.CHASE_NEAR
        }
        emitHud()
    }

    fun updateSettings(newSettings: GameSettingsState) {
        val qualityChanged = newSettings.graphicsQuality != settings.graphicsQuality
        settings = newSettings
        soundEngine.updateSettings(newSettings.soundEnabled, newSettings.soundVolume)
        if (qualityChanged && isPlaying) {
            spawnTrafficAndPedestrians()
            spawnRainDrops()
        }
    }

    private fun spawnRivals() {
        rivals.clear()
        val rivalBikes = listOf(
            Triple(BikeId.KTM_DUKE, 1.0f, 0.43f to 0.0f),
            Triple(BikeId.YAMAHA_R15, 0.0f, 0.35f to 0.95f),
            Triple(BikeId.TVS_APACHE, 0.85f, 0.05f to 0.1f)
        )
        rivalBikes.forEachIndexed { idx, (bike, r, gb) ->
            rivals.add(
                RivalBikerAgent(
                    x = if (idx % 2 == 0) 3.5f else -0.5f,
                    z = 35f + idx * 5f,
                    headingDeg = 180f,
                    speedMs = 0f,
                    leanDeg = 0f,
                    wheelAngle = 0f,
                    checkpointIdx = 0,
                    lap = 0,
                    bikeId = bike,
                    r = r,
                    g = gb.first,
                    b = gb.second
                )
            )
        }
    }

    private fun spawnTrafficAndPedestrians() {
        traffic.clear()
        pedestrians.clear()
        val quality = settings.graphicsQuality

        val roadDefs = listOf(
            // (axisAlongZ, laneCoord, dirSign, minRange, maxRange)
            Triple(true, 4.5f, 1f),
            Triple(true, -4.5f, -1f),
            Triple(true, 123.5f, 1f),
            Triple(true, 116.5f, -1f),
            Triple(true, -116.5f, 1f),
            Triple(true, -123.5f, -1f),
            Triple(true, 283.5f, 1f),
            Triple(false, 3.5f, 1f),
            Triple(false, -3.5f, -1f),
            Triple(false, 123.5f, 1f),
            Triple(false, -116.5f, -1f),
            Triple(true, -303f, 1f) // Village road for tractors & autos
        )

        val vehicleColors = listOf(
            Triple(0.92f, 0.93f, 0.95f),
            Triple(0.75f, 0.15f, 0.15f),
            Triple(0.15f, 0.35f, 0.72f),
            Triple(0.72f, 0.75f, 0.78f),
            Triple(0.18f, 0.52f, 0.32f),
            Triple(0.95f, 0.72f, 0.08f)
        )

        for (i in 0 until quality.trafficCount) {
            val road = roadDefs[i % roadDefs.size]
            val axisZ = road.first
            val lane = road.second
            val dir = road.third
            val posAlong = -340f + rng.nextFloat() * 680f

            val type = when {
                lane < -200f -> if (i % 2 == 0) TrafficVehicleType.TRACTOR else TrafficVehicleType.AUTO_RICKSHAW
                i % 9 == 0 -> TrafficVehicleType.POLICE_PCR
                i % 6 == 0 -> TrafficVehicleType.BUS
                i % 5 == 0 -> TrafficVehicleType.TRUCK
                i % 3 == 0 -> TrafficVehicleType.AUTO_RICKSHAW
                else -> TrafficVehicleType.CAR
            }

            val baseSpeed = when (type) {
                TrafficVehicleType.TRACTOR -> 7.5f
                TrafficVehicleType.AUTO_RICKSHAW -> 10.5f
                TrafficVehicleType.BUS -> 11.5f
                TrafficVehicleType.TRUCK -> 12.0f
                TrafficVehicleType.POLICE_PCR -> 16.5f
                TrafficVehicleType.CAR -> 14.5f
            }

            val col = when (type) {
                TrafficVehicleType.AUTO_RICKSHAW -> Triple(0.10f, 0.14f, 0.12f)
                TrafficVehicleType.BUS -> Triple(0.82f, 0.14f, 0.12f)
                TrafficVehicleType.TRUCK -> Triple(0.92f, 0.52f, 0.08f)
                TrafficVehicleType.TRACTOR -> Triple(0.12f, 0.56f, 0.22f)
                TrafficVehicleType.POLICE_PCR -> Triple(0.94f, 0.95f, 0.98f)
                TrafficVehicleType.CAR -> vehicleColors[i % vehicleColors.size]
            }

            val x = if (axisZ) lane else posAlong
            val z = if (axisZ) posAlong else lane
            val heading = if (axisZ) (if (dir > 0) 0f else 180f) else (if (dir > 0) 90f else 270f)

            // Keep spawn away from player start
            if (hypot(x - playerX, z - playerZ) < 28f) continue

            traffic.add(
                TrafficAgent(
                    x = x, z = z, headingDeg = heading,
                    speedMs = baseSpeed, baseSpeedMs = baseSpeed,
                    type = type, bodyR = col.first, bodyG = col.second, bodyB = col.third,
                    axisAlongZ = axisZ, laneCoord = lane, dirSign = dir
                )
            )
        }

        // Pedestrians along bazaar sidewalks & village paths
        val shirtColors = listOf(
            Triple(0.95f, 0.45f, 0.12f),
            Triple(0.18f, 0.55f, 0.85f),
            Triple(0.90f, 0.85f, 0.22f),
            Triple(0.82f, 0.20f, 0.45f),
            Triple(0.22f, 0.72f, 0.45f)
        )
        for (i in 0 until quality.pedestrianCount) {
            val alongZ = i % 2 == 0
            val sideX = if (alongZ) (if (i % 4 == 0) 9.5f else -9.5f) else (-160f + rng.nextFloat() * 320f)
            val sideZ = if (alongZ) (-160f + rng.nextFloat() * 320f) else (if (i % 4 == 1) 8.5f else -8.5f)
            val c = shirtColors[i % shirtColors.size]
            pedestrians.add(
                PedestrianAgent(
                    x = sideX, z = sideZ,
                    headingDeg = if (alongZ) 0f else 90f,
                    speedMs = 1.4f + rng.nextFloat() * 0.6f,
                    walkPhase = rng.nextFloat() * 6.28f,
                    shirtR = c.first, shirtG = c.second, shirtB = c.third,
                    minCoord = -180f, maxCoord = 180f, alongZ = alongZ
                )
            )
        }
    }

    private fun spawnRainDrops() {
        rainDrops.clear()
        val count = settings.graphicsQuality.rainParticles
        for (i in 0 until count) {
            rainDrops.add(
                RainDrop(
                    x = (rng.nextFloat() - 0.5f) * 55f,
                    y = rng.nextFloat() * 18f,
                    z = (rng.nextFloat() - 0.5f) * 55f,
                    speed = 18f + rng.nextFloat() * 8f
                )
            )
        }
    }

    fun step(rawDt: Float) {
        if (!isPlaying || isPaused || sessionResult != null) {
            soundEngine.updateTelemetry(
                activeBikeId, false, 0f, false, false, 0f, false, false, 0f, 0f, false, false
            )
            return
        }
        val dt = rawDt.coerceIn(0.001f, 0.05f)

        updateEnvironment(dt)
        updateRailwayAndTrain(dt)
        updatePlayerBike(dt)
        updateTraffic(dt)
        updatePedestrians(dt)
        updateModeAndMissions(dt)
        updateCamera(dt)
        updateRain(dt)

        hudEmitTimer += dt
        if (hudEmitTimer >= 0.05f) {
            hudEmitTimer = 0f
            emitHud()
        }
    }

    private fun updateEnvironment(dt: Float) {
        dayClock = when (settings.timeOfDayMode) {
            TimeOfDayMode.AUTO -> (dayClock + dt * 0.0038f) % 1.0f // ~4.3 min full day-night cycle
            TimeOfDayMode.DAY -> 0.48f
            TimeOfDayMode.SUNSET -> 0.72f
            TimeOfDayMode.NIGHT -> 0.92f
        }

        if (settings.weatherMode == WeatherMode.AUTO) {
            weatherCycleTimer += dt
            if (weatherCycleTimer > 55f) {
                weatherCycleTimer = 0f
                currentWeather = when (currentWeather) {
                    WeatherMode.CLEAR -> WeatherMode.RAIN
                    WeatherMode.RAIN -> WeatherMode.FOG
                    else -> WeatherMode.CLEAR
                }
            }
        } else {
            currentWeather = settings.weatherMode
        }

        val targetRain = if (currentWeather == WeatherMode.RAIN) 1f else 0f
        val targetFog = when (currentWeather) {
            WeatherMode.FOG -> 1f
            WeatherMode.RAIN -> 0.45f
            else -> 0f
        }
        rainAmount += (targetRain - rainAmount) * min(1f, dt * 1.2f)
        fogAmount += (targetFog - fogAmount) * min(1f, dt * 1.2f)
    }

    private fun updateRailwayAndTrain(dt: Float) {
        trainTimer -= dt
        if (!trainActive && trainTimer <= 0f) {
            trainActive = true
            trainX = -520f
        }
        if (trainActive) {
            trainX += 46f * dt // Fast Indian Express train!
            if (trainX > 520f) {
                trainActive = false
                trainTimer = 26f
            }
        }
        val shouldCloseGate = trainActive && trainX > -240f && trainX < 240f
        val targetGate = if (shouldCloseGate) 1f else 0f
        gateClosedFactor += (targetGate - gateClosedFactor) * min(1f, dt * 3.5f)
    }

    private fun updatePlayerBike(dt: Float) {
        val bike = activeBikeId
        val custom = activeCustomization
        val engMult = 1f + (custom.engineLevel - 1) * 0.12f
        val nosMult = 1f + (custom.nitroLevel - 1) * 0.25f
        val suspMult = 1f + (custom.suspensionLevel - 1) * 0.15f

        val maxSpeedMs = (bike.topSpeedKmh / 3.6f) * engMult
        val accelBase = bike.acceleration * engMult
        val ctrl = input

        // Nitro boost management
        val usingNitro = ctrl.nitro && engineRunning && nitroAmount > 0.02f && fuelAmount > 0f && playerSpeedMs > 2f
        if (usingNitro) {
            nitroAmount = (nitroAmount - dt * (0.24f / nosMult)).coerceAtLeast(0f)
        } else {
            nitroAmount = (nitroAmount + dt * 0.08f * nosMult).coerceAtMost(1f)
        }

        // Fuel consumption & Bharat Petrol Pump refuel
        if (worldMap.isNearPetrolPump(playerX, playerZ)) {
            fuelAmount = (fuelAmount + dt * 0.45f).coerceAtMost(1f)
        } else if (engineRunning && playerSpeedMs > 1f) {
            fuelAmount = (fuelAmount - dt * 0.0012f).coerceAtLeast(0f)
        }

        val hasFuel = fuelAmount > 0.005f
        val canDrive = engineRunning && hasFuel

        // Off-road surface drag (grass vs paved road)
        val onRoad = isOnPavedRoad(playerX, playerZ)
        val surfaceDrag = if (onRoad) 0.35f else 1.25f
        val offroadSpeedFactor = if (onRoad) 1.0f else 0.72f

        var targetAccel = 0f
        if (ctrl.throttle && canDrive) {
            val nitroBoost = if (usingNitro) 1.55f else 1.0f
            targetAccel = accelBase * nitroBoost
        } else if (ctrl.brake) {
            if (playerSpeedMs > 0.8f) {
                targetAccel = -bike.braking
            } else if (canDrive) {
                // Reverse walk speed
                targetAccel = -4.5f
            }
        }

        // Apply longitudinal dynamics
        if (!airborne) {
            playerSpeedMs += targetAccel * dt
            val drag = (surfaceDrag + 0.012f * playerSpeedMs * abs(playerSpeedMs)) * dt
            if (!ctrl.throttle && !ctrl.brake) {
                if (playerSpeedMs > 0f) playerSpeedMs = (playerSpeedMs - drag - 1.5f * dt).coerceAtLeast(0f)
                else if (playerSpeedMs < 0f) playerSpeedMs = (playerSpeedMs + 3.5f * dt).coerceAtMost(0f)
            }
            val topCap = maxSpeedMs * offroadSpeedFactor * (if (usingNitro) 1.24f else 1.0f)
            playerSpeedMs = playerSpeedMs.coerceIn(-6f, topCap)
        }

        // Steering & Lean Angle
        val steerInput = (if (ctrl.steerLeft) 1f else 0f) - (if (ctrl.steerRight) 1f else 0f)
        val targetSteer = steerInput * 26f * settings.steeringSensitivity.coerceIn(0.6f, 1.5f)
        playerSteerAngleDeg += (targetSteer - playerSteerAngleDeg) * min(1f, dt * 10f)

        val speedFactor = (abs(playerSpeedMs) / 14f).coerceIn(0f, 1f)
        val turnRateDeg = steerInput * 105f * bike.handling * settings.steeringSensitivity * (0.35f + 0.65f * speedFactor) *
            (if (playerSpeedMs >= -0.1f) 1f else -1f)
        if (abs(playerSpeedMs) > 0.3f) {
            playerHeadingDeg = (playerHeadingDeg + turnRateDeg * dt + 360f) % 360f
        }

        val targetLean = -steerInput * 28f * speedFactor
        playerLeanDeg += (targetLean - playerLeanDeg) * min(1f, dt * 8f)

        // Wheelie / Stunt pitch
        val targetWheelie = if (ctrl.wheelie && engineRunning && playerSpeedMs > 7f && !airborne) 22f else 0f
        wheelieAngleDeg += (targetWheelie - wheelieAngleDeg) * min(1f, dt * 7f)
        if (wheelieAngleDeg > 14f && !airborne) {
            awardStuntPoints((24f * dt).toInt(), "WHEELIE")
        }

        // Move bike in XZ
        val rad = playerHeadingDeg * PI.toFloat() / 180f
        val nextX = worldMap.clampToWorld(playerX + sin(rad) * playerSpeedMs * dt)
        val nextZ = worldMap.clampToWorld(playerZ + cos(rad) * playerSpeedMs * dt)

        // World & Gate Collision Check
        var collided = false
        val bikeRadius = 0.85f
        for (box in worldMap.colliders) {
            if (box.containsXZ(nextX, nextZ, bikeRadius)) {
                collided = true
                // Slide along non-blocking axis if possible
                val canMoveX = !box.containsXZ(nextX, playerZ, bikeRadius)
                val canMoveZ = !box.containsXZ(playerX, nextZ, bikeRadius)
                if (canMoveX) playerX = nextX
                if (canMoveZ) playerZ = nextZ
                break
            }
        }
        // Closed railway crossing barrier check
        if (!collided && gateClosedFactor > 0.65f && abs(nextX) < 12f && abs(nextZ - worldMap.railwayZ) < 5.5f && abs(playerZ - worldMap.railwayZ) >= 5.5f) {
            collided = true
        }
        if (!collided) {
            playerX = nextX
            playerZ = nextZ
        } else {
            onPlayerCollision(abs(playerSpeedMs))
        }

        // Ramp & Vertical Jump Physics
        val groundH = worldMap.getRampHeightAt(playerX, playerZ)
        val rampDelta = groundH - prevRampHeight
        if (!airborne && rampDelta > 0.04f && playerSpeedMs > 8f) {
            // Following ramp slope upward
            playerY = groundH
            playerVelY = (rampDelta / dt) * 0.92f
        } else if (!airborne && prevRampHeight > 0.8f && groundH < prevRampHeight - 0.5f && playerSpeedMs > 8f) {
            // Launched off top lip of ramp!
            airborne = true
            airTimeSec = 0f
            playerVelY = max(playerVelY, playerSpeedMs * 0.34f)
        } else if (airborne || playerY > groundH + 0.05f) {
            airborne = true
            airTimeSec += dt
            playerVelY -= 16.5f * dt
            playerY += playerVelY * dt
            if (playerY <= groundH) {
                // Touchdown! Compress suspension
                playerY = groundH
                suspensionVelocity = (playerVelY * 0.08f).coerceIn(-0.4f, -0.05f)
                if (airTimeSec > 0.45f) {
                    val pts = (airTimeSec * 280f).toInt()
                    awardStuntPoints(pts, "BIG AIR ${String.format("%.1f", airTimeSec)}s")
                }
                airborne = false
                playerVelY = 0f
                airTimeSec = 0f
            }
        } else {
            playerY = groundH
            playerVelY = 0f
        }
        prevRampHeight = groundH

        // Suspension spring-damper simulation (squat on accel, dive on brake, offroad bumps)
        val accelPitchForce = if (ctrl.throttle && canDrive) 0.06f else if (ctrl.brake && playerSpeedMs > 3f) -0.09f else 0f
        val offroadBump = if (!onRoad && !airborne && playerSpeedMs > 4f) {
            sin(playerX * 1.4f + playerZ * 1.4f) * 0.025f
        } else 0f
        val springK = 85f * bike.suspensionStiffness * suspMult
        val damping = 12f
        val springForce = -springK * (suspensionOffset - offroadBump) - damping * suspensionVelocity
        suspensionVelocity += springForce * dt
        suspensionOffset = (suspensionOffset + suspensionVelocity * dt).coerceIn(-0.14f, 0.14f)

        val targetPitch = if (airborne) (playerVelY * 1.4f).coerceIn(-25f, 25f) else (accelPitchForce * 45f + wheelieAngleDeg)
        playerPitchDeg += (targetPitch - playerPitchDeg) * min(1f, dt * 8f)

        // Spin wheels
        val circumference = 2f * PI.toFloat() * 0.32f
        wheelSpinDeg = (wheelSpinDeg + (playerSpeedMs * dt / circumference) * 360f) % 360f

        // Speedometer & RPM/Gear calculation
        val speedKmh = abs(playerSpeedMs) * 3.6f
        missionMaxSpeedReached = max(missionMaxSpeedReached, speedKmh)
        val topKmh = bike.topSpeedKmh * engMult
        val gear = ((speedKmh / topKmh) * 5f).toInt().coerceIn(0, 4) + 1
        val gearLow = ((gear - 1) / 5f) * topKmh
        val gearHigh = (gear / 5f) * topKmh
        val rpmNorm = if (!engineRunning) 0f else {
            val base = ((speedKmh - gearLow * 0.7f) / (gearHigh - gearLow * 0.7f).coerceAtLeast(15f)).coerceIn(0.12f, 0.98f)
            if (ctrl.throttle) max(base, 0.35f) else base * 0.75f
        }

        // Red-light violation check at city intersections
        redLightCooldown = (redLightCooldown - dt).coerceAtLeast(0f)
        if (speedKmh > 45f && redLightCooldown <= 0f) {
            for (sig in worldMap.trafficSignals) {
                if (hypot(playerX - sig.x, playerZ - sig.z) < 10f && isSignalRedForZ(sig)) {
                    redLightCooldown = 6f
                    policeWantedLevel = min(3, policeWantedLevel + 1)
                    wantedDecayTimer = 22f
                    crashBannerText = "RED LIGHT JUMPED! PCR ALERT"
                    crashBannerTimer = 2.4f
                }
            }
        }

        // Update Sound Engine
        val nearTrain = hypot(playerX - trainX, playerZ - worldMap.railwayZ) < 110f ||
            (gateClosedFactor > 0.3f && abs(playerZ - worldMap.railwayZ) < 55f)
        soundEngine.updateTelemetry(
            bike = bike,
            isEngineOn = engineRunning && hasFuel,
            rpmNormalized = rpmNorm,
            isThrottle = ctrl.throttle && canDrive,
            isBraking = ctrl.brake,
            currentSpeedKmh = speedKmh,
            isNitro = usingNitro,
            isHorn = ctrl.horn,
            rain = rainAmount,
            night = getNightFactor(),
            railwayBell = nearTrain,
            policeSiren = policeWantedLevel > 0
        )

        // Check collision with active Express Train!
        if (trainActive && abs(playerZ - worldMap.railwayZ) < 2.8f && abs(playerX - trainX) < 34f) {
            onPlayerCollision(35f)
            playerZ = if (playerZ >= worldMap.railwayZ) worldMap.railwayZ + 6f else worldMap.railwayZ - 6f
        }
    }

    private fun onPlayerCollision(impactSpeedMs: Float) {
        if (impactSpeedMs > 5f) {
            soundEngine.triggerCrash((impactSpeedMs / 35f).coerceIn(0.3f, 1.0f))
            camShake = (impactSpeedMs / 40f).coerceIn(0.15f, 0.65f)
            missionCrashed = true
            crashBannerText = "COLLISION!"
            crashBannerTimer = 1.6f
        }
        playerSpeedMs = -playerSpeedMs * 0.25f
    }

    private fun updateTraffic(dt: Float) {
        nearMissCooldown = (nearMissCooldown - dt).coerceAtLeast(0f)
        if (policeWantedLevel > 0 && gameMode != GameMode.MISSION) {
            wantedDecayTimer -= dt
            if (wantedDecayTimer <= 0f) {
                policeWantedLevel = 0
            }
        }

        for (agent in traffic) {
            var desiredSpeed = agent.baseSpeedMs

            // Police PCR pursuit mode!
            if (agent.type == TrafficVehicleType.POLICE_PCR && policeWantedLevel > 0) {
                val dist = hypot(playerX - agent.x, playerZ - agent.z)
                if (dist < 180f) {
                    agent.isPursuing = true
                    val angleToPlayer = atan2(playerX - agent.x, playerZ - agent.z) * 180f / PI.toFloat()
                    agent.headingDeg = angleToPlayer
                    desiredSpeed = 24f + policeWantedLevel * 3f
                    val rad = agent.headingDeg * PI.toFloat() / 180f
                    agent.x = worldMap.clampToWorld(agent.x + sin(rad) * desiredSpeed * dt)
                    agent.z = worldMap.clampToWorld(agent.z + cos(rad) * desiredSpeed * dt)
                    agent.speedMs = desiredSpeed
                    agent.wheelAngle = (agent.wheelAngle + desiredSpeed * dt * 120f) % 360f
                } else {
                    agent.isPursuing = false
                }
            } else {
                agent.isPursuing = false
            }

            if (!agent.isPursuing) {
                // Stop for red traffic signals
                for (sig in worldMap.trafficSignals) {
                    val distSig = hypot(agent.x - sig.x, agent.z - sig.z)
                    if (distSig < 20f) {
                        val isRed = if (agent.axisAlongZ) isSignalRedForZ(sig) else !isSignalRedForZ(sig)
                        if (isRed) desiredSpeed = 0f
                    }
                }
                // Stop for railway crossing gate
                if (agent.axisAlongZ && gateClosedFactor > 0.4f && abs(agent.z - worldMap.railwayZ) in 7f..22f) {
                    desiredSpeed = 0f
                }
                // Slow down if close to another traffic vehicle ahead
                for (other in traffic) {
                    if (other === agent) continue
                    if (agent.axisAlongZ == other.axisAlongZ && abs(agent.laneCoord - other.laneCoord) < 2.2f) {
                        val gap = if (agent.axisAlongZ) (other.z - agent.z) * agent.dirSign else (other.x - agent.x) * agent.dirSign
                        if (gap in 0.5f..11f) {
                            desiredSpeed = min(desiredSpeed, other.speedMs * 0.7f)
                        }
                    }
                }

                agent.speedMs += (desiredSpeed - agent.speedMs) * min(1f, dt * 4f)
                val stepDist = agent.speedMs * dt * agent.dirSign
                if (agent.axisAlongZ) {
                    agent.z += stepDist
                    if (agent.z > 420f) agent.z = -420f
                    if (agent.z < -420f) agent.z = 420f
                } else {
                    agent.x += stepDist
                    if (agent.x > 420f) agent.x = -420f
                    if (agent.x < -420f) agent.x = 420f
                }
                agent.wheelAngle = (agent.wheelAngle + agent.speedMs * dt * 110f) % 360f
            }

            // Player vs Traffic vehicle collision + Near-Miss Stunt bonus!
            val dx = playerX - agent.x
            val dz = playerZ - agent.z
            val dist = hypot(dx, dz)
            val hitDist = agent.halfWidth + 0.9f
            if (dist < hitDist && playerY < 1.6f) {
                val pushX = if (dist > 0.01f) dx / dist else 1f
                val pushZ = if (dist > 0.01f) dz / dist else 0f
                playerX += pushX * (hitDist - dist + 0.25f)
                playerZ += pushZ * (hitDist - dist + 0.25f)
                onPlayerCollision(abs(playerSpeedMs - agent.speedMs) + 6f)
                if (agent.type == TrafficVehicleType.POLICE_PCR) {
                    policeWantedLevel = min(3, policeWantedLevel + 1)
                    wantedDecayTimer = 25f
                }
            } else if (dist in (hitDist + 0.2f)..(hitDist + 2.3f) && abs(playerSpeedMs) > 16f && nearMissCooldown <= 0f) {
                nearMissCooldown = 1.4f
                awardStuntPoints(120, "NEAR MISS!")
            }
        }
    }

    private fun updatePedestrians(dt: Float) {
        for (ped in pedestrians) {
            val distToPlayer = hypot(playerX - ped.x, playerZ - ped.z)
            // Pedestrians scurry aside if player honks nearby
            if (distToPlayer < 14f && input.horn) {
                val awayX = ped.x - playerX
                val awayZ = ped.z - playerZ
                val len = hypot(awayX, awayZ).coerceAtLeast(0.1f)
                ped.x += (awayX / len) * 3.8f * dt
                ped.z += (awayZ / len) * 3.8f * dt
            } else {
                val dir = if (ped.headingDeg < 90f || ped.headingDeg > 270f) 1f else -1f
                if (ped.alongZ) {
                    ped.z += ped.speedMs * dir * dt
                    if (ped.z > ped.maxCoord) ped.headingDeg = 180f
                    if (ped.z < ped.minCoord) ped.headingDeg = 0f
                } else {
                    ped.x += ped.speedMs * dir * dt
                    if (ped.x > ped.maxCoord) ped.headingDeg = 270f
                    if (ped.x < ped.minCoord) ped.headingDeg = 90f
                }
            }
            ped.walkPhase += ped.speedMs * dt * 4f

            if (distToPlayer < 1.0f && playerY < 1.2f) {
                val pushX = (playerX - ped.x)
                val pushZ = (playerZ - ped.z)
                val len = hypot(pushX, pushZ).coerceAtLeast(0.05f)
                ped.x -= (pushX / len) * 1.4f
                ped.z -= (pushZ / len) * 1.4f
                onPlayerCollision(6f)
            }
        }
    }

    private fun updateModeAndMissions(dt: Float) {
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) comboMultiplier = 1
        }
        lastStuntBannerTimer = (lastStuntBannerTimer - dt).coerceAtLeast(0f)
        crashBannerTimer = (crashBannerTimer - dt).coerceAtLeast(0f)

        when (gameMode) {
            GameMode.RACING -> {
                val cp = raceCheckpoints[raceCheckpointIndex]
                if (hypot(playerX - cp.first, playerZ - cp.second) < 12f) {
                    raceCheckpointIndex++
                    if (raceCheckpointIndex >= raceCheckpoints.size) {
                        val pos = computeRacePosition()
                        val reward = when (pos) {
                            1 -> 800
                            2 -> 450
                            3 -> 250
                            else -> 120
                        }
                        finishSession(
                            SessionResult(
                                success = true,
                                title = "RACE FINISHED — P$pos",
                                subtitle = if (pos == 1) "Champion of the Bharat Highway Circuit!" else "Strong finish! Upgrade your bike to take P1.",
                                coinsEarned = reward,
                                scoreAchieved = (5 - pos) * 1000
                            )
                        )
                    } else {
                        targetX = raceCheckpoints[raceCheckpointIndex].first
                        targetZ = raceCheckpoints[raceCheckpointIndex].second
                        soundEngine.triggerStarter()
                    }
                }

                // Update AI rival bikers
                for (rival in rivals) {
                    val rCp = raceCheckpoints[rival.checkpointIdx.coerceAtMost(raceCheckpoints.lastIndex)]
                    val dx = rCp.first - rival.x
                    val dz = rCp.second - rival.z
                    val dist = hypot(dx, dz)
                    val targetAngle = atan2(dx, dz) * 180f / PI.toFloat()
                    var diff = ((targetAngle - rival.headingDeg + 540f) % 360f) - 180f
                    diff = diff.coerceIn(-90f * dt, 90f * dt)
                    rival.headingDeg = (rival.headingDeg + diff + 360f) % 360f
                    rival.leanDeg = (-diff * 6f).coerceIn(-25f, 25f)

                    val targetSpd = (rival.bikeId.topSpeedKmh / 3.6f) * 0.78f
                    rival.speedMs += (targetSpd - rival.speedMs) * min(1f, dt * 1.8f)
                    val rad = rival.headingDeg * PI.toFloat() / 180f
                    rival.x += sin(rad) * rival.speedMs * dt
                    rival.z += cos(rad) * rival.speedMs * dt
                    rival.wheelAngle = (rival.wheelAngle + rival.speedMs * dt * 130f) % 360f

                    if (dist < 12f && rival.checkpointIdx < raceCheckpoints.size - 1) {
                        rival.checkpointIdx++
                    }
                }
            }
            GameMode.MISSION -> {
                val m = activeMission ?: return
                missionTimeLeft -= dt
                if (m.noCrashRequired && missionCrashed) {
                    finishSession(
                        SessionResult(
                            success = false,
                            title = "MISSION FAILED",
                            subtitle = "You crashed on the pilgrimage route! Ride smoothly.",
                            coinsEarned = 0,
                            scoreAchieved = stuntScore
                        )
                    )
                    return
                }
                if (missionTimeLeft <= 0f) {
                    finishSession(
                        SessionResult(
                            success = false,
                            title = "TIME EXPIRED",
                            subtitle = "You ran out of time for ${m.title}.",
                            coinsEarned = 0,
                            scoreAchieved = stuntScore
                        )
                    )
                    return
                }

                val distToTarget = hypot(playerX - m.targetX, playerZ - m.targetZ)
                val speedOk = m.targetSpeedKmh <= 0f || missionMaxSpeedReached >= m.targetSpeedKmh
                val stuntOk = m.targetStuntPoints <= 0 || stuntScore >= m.targetStuntPoints

                if (m.targetStuntPoints > 0) {
                    if (stuntOk) {
                        finishSession(
                            SessionResult(
                                success = true,
                                title = "MISSION COMPLETE!",
                                subtitle = "${m.title} mastered with $stuntScore stunt points!",
                                coinsEarned = m.rewardCoins,
                                scoreAchieved = stuntScore
                            )
                        )
                    }
                } else if (distToTarget < 14f && speedOk) {
                    finishSession(
                        SessionResult(
                            success = true,
                            title = "MISSION COMPLETE!",
                            subtitle = "${m.title} cleared with ${missionTimeLeft.toInt()}s remaining!",
                            coinsEarned = m.rewardCoins,
                            scoreAchieved = (missionTimeLeft * 40f).toInt() + stuntScore
                        )
                    )
                }
            }
            else -> {}
        }
    }

    private fun finishSession(result: SessionResult) {
        if (sessionResult != null) return
        sessionResult = result
        if (!sessionRewardClaimed) {
            sessionRewardClaimed = true
            onSessionCompleted?.invoke(result, activeMission)
        }
        emitHud()
    }

    private fun computeRacePosition(): Int {
        var rank = 1
        val playerDist = targetX?.let { tx ->
            targetZ?.let { tz -> hypot(playerX - tx, playerZ - tz) }
        } ?: 0f
        for (r in rivals) {
            if (r.checkpointIdx > raceCheckpointIndex) {
                rank++
            } else if (r.checkpointIdx == raceCheckpointIndex) {
                val cp = raceCheckpoints[r.checkpointIdx.coerceAtMost(raceCheckpoints.lastIndex)]
                val rDist = hypot(r.x - cp.first, r.z - cp.second)
                if (rDist < playerDist) rank++
            }
        }
        return rank
    }

    private fun awardStuntPoints(basePts: Int, label: String) {
        val pts = basePts * comboMultiplier
        stuntScore += pts
        comboTimer = 4.5f
        comboMultiplier = min(5, comboMultiplier + 1)
        lastStuntLabel = "$label +$pts"
        lastStuntBannerTimer = 2.0f
    }

    private fun updateCamera(dt: Float) {
        camShake = (camShake - dt * 1.8f).coerceAtLeast(0f)
        val rad = playerHeadingDeg * PI.toFloat() / 180f
        val (distBack, heightUp) = when (cameraMode) {
            CameraMode.CHASE_NEAR -> Pair(5.8f, 2.55f)
            CameraMode.CHASE_FAR -> Pair(9.2f, 4.10f)
            CameraMode.FIRST_PERSON -> Pair(-0.18f, 1.38f)
        }

        if (cameraMode == CameraMode.FIRST_PERSON) {
            camPosX = playerX + sin(rad) * 0.25f
            camPosY = playerY + heightUp + suspensionOffset * 0.5f
            camPosZ = playerZ + cos(rad) * 0.25f
        } else {
            val desiredX = playerX - sin(rad) * distBack
            val desiredY = playerY + heightUp
            val desiredZ = playerZ - cos(rad) * distBack
            val smooth = min(1f, dt * 8.5f)
            camPosX += (desiredX - camPosX) * smooth
            camPosY += (desiredY - camPosY) * smooth
            camPosZ += (desiredZ - camPosZ) * smooth
        }
    }

    private fun updateRain(dt: Float) {
        if (rainAmount < 0.05f) return
        for (drop in rainDrops) {
            drop.y -= drop.speed * dt
            if (drop.y < 0f) {
                drop.y = 17f + rng.nextFloat() * 3f
                drop.x = (rng.nextFloat() - 0.5f) * 55f
                drop.z = (rng.nextFloat() - 0.5f) * 55f
            }
        }
    }

    fun isSignalRedForZ(sig: TrafficSignalPole): Boolean {
        val cycle = (System.currentTimeMillis() / 1000.0 + sig.phaseOffset) % 14.0
        return cycle > 7.5
    }

    fun isSignalYellow(sig: TrafficSignalPole): Boolean {
        val cycle = (System.currentTimeMillis() / 1000.0 + sig.phaseOffset) % 14.0
        return (cycle in 6.0..7.5) || (cycle > 12.5)
    }

    fun getNightFactor(): Float {
        // 0.25 dawn, 0.5 day, 0.72 sunset, 0.90 night
        return when {
            dayClock < 0.20f -> 1.0f
            dayClock < 0.30f -> 1f - (dayClock - 0.20f) / 0.10f
            dayClock < 0.68f -> 0.0f
            dayClock < 0.80f -> (dayClock - 0.68f) / 0.12f
            else -> 1.0f
        }.coerceIn(0f, 1f)
    }

    private fun isOnPavedRoad(x: Float, z: Float): Boolean {
        if (abs(x) < 11f) return true
        if (abs(z - 380f) < 10f || abs(z + 380f) < 10f) return true
        if (abs(z) < 8f && x in -220f..360f) return true
        if ((abs(z - 120f) < 8f || abs(z + 120f) < 8f) && x in -220f..360f) return true
        if (abs(x - 120f) < 8f || abs(x + 120f) < 8f || abs(x - 280f) < 8f || abs(x - 410f) < 8f) return true
        if (worldMap.isNearPetrolPump(x, z)) return true
        return false
    }

    private fun getTimeLabel(): String = when {
        dayClock < 0.24f || dayClock > 0.82f -> "Night"
        dayClock < 0.34f -> "Dawn"
        dayClock < 0.65f -> "Day"
        else -> "Sunset"
    }

    private fun emitHud() {
        val speedKmh = (abs(playerSpeedMs) * 3.6f).toInt()
        val engMult = 1f + (activeCustomization.engineLevel - 1) * 0.12f
        val topKmh = activeBikeId.topSpeedKmh * engMult
        val gear = ((speedKmh / topKmh) * 5f).toInt().coerceIn(0, 4) + 1
        val rpmNorm = if (!engineRunning) 0f else (0.18f + (speedKmh / topKmh) * 0.80f).coerceIn(0.15f, 1f)

        val dots = mutableListOf<MinimapDot>()
        traffic.forEach { t ->
            if (hypot(t.x - playerX, t.z - playerZ) < 260f) {
                dots.add(MinimapDot(t.x, t.z, if (t.type == TrafficVehicleType.POLICE_PCR) 1 else 0))
            }
        }
        rivals.forEach { r ->
            dots.add(MinimapDot(r.x, r.z, 2))
        }
        val tx = targetX
        val tz = targetZ
        val distTarget = if (tx != null && tz != null) hypot(tx - playerX, tz - playerZ).toInt() else 0

        val objectiveText = when (gameMode) {
            GameMode.FREE_RIDE -> "Explore India — Petrol Pump refills fuel"
            GameMode.STUNT -> "Hit ramps & weave traffic for combos!"
            GameMode.RACING -> "Checkpoint ${raceCheckpointIndex + 1} / ${raceCheckpoints.size}"
            GameMode.MISSION -> {
                val m = activeMission
                when {
                    m == null -> ""
                    m.targetStuntPoints > 0 -> "Stunts: $stuntScore / ${m.targetStuntPoints}"
                    m.targetSpeedKmh > 0f && missionMaxSpeedReached < m.targetSpeedKmh ->
                        "Hit ${m.targetSpeedKmh.toInt()} km/h (${missionMaxSpeedReached.toInt()}) & reach target"
                    else -> "${m.subtitle} (${distTarget}m)"
                }
            }
        }

        val nearTrain = (trainActive && abs(playerZ - worldMap.railwayZ) < 60f) ||
            (gateClosedFactor > 0.4f && abs(playerZ - worldMap.railwayZ) < 55f)

        onHudUpdate?.invoke(
            PlayerHudState(
                speedKmh = speedKmh,
                rpmNormalized = rpmNorm,
                gear = gear,
                engineRunning = engineRunning,
                headlightOn = headlightMode > 0,
                highBeam = headlightMode == 2,
                nitroPercent = nitroAmount,
                fuelPercent = fuelAmount,
                cameraMode = cameraMode,
                zoneName = worldMap.getZoneName(playerX, playerZ),
                timeLabel = getTimeLabel(),
                weatherLabel = currentWeather.label,
                stuntScore = stuntScore,
                lastStuntLabel = if (lastStuntBannerTimer > 0f) lastStuntLabel else "",
                comboMultiplier = comboMultiplier,
                airTimeSec = airTimeSec,
                policeWantedLevel = policeWantedLevel,
                nearTrainWarning = nearTrain,
                refuelBanner = worldMap.isNearPetrolPump(playerX, playerZ) && fuelAmount < 0.99f,
                crashWarning = if (crashBannerTimer > 0f) crashBannerText else "",
                missionTitle = activeMission?.title ?: gameMode.label,
                missionObjective = objectiveText,
                missionTimeLeftSec = missionTimeLeft,
                missionDistanceMeters = distTarget,
                raceCheckpointIndex = raceCheckpointIndex,
                raceTotalCheckpoints = raceCheckpoints.size,
                racePosition = if (gameMode == GameMode.RACING) computeRacePosition() else 1,
                raceTotalRacers = rivals.size + 1,
                resultState = sessionResult,
                playerX = playerX,
                playerZ = playerZ,
                playerHeadingDeg = playerHeadingDeg,
                targetX = tx,
                targetZ = tz,
                minimapDots = dots
            )
        )
    }
}
