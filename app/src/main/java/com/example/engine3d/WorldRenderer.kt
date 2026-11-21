package com.example.engine3d

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.BikeCustomization
import com.example.model.BikeId
import com.example.model.CameraMode
import com.example.model.GameMode
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class WorldRenderer(
    private val simulation: SimulationEngine,
    private val worldMap: IndianWorldMap
) : GLSurfaceView.Renderer {

    @Volatile var showroomMode = true
    @Volatile var showroomBikeId = BikeId.ROYAL_ENFIELD_BULLET
    @Volatile var showroomCustomization = BikeCustomization(BikeId.ROYAL_ENFIELD_BULLET.defaultTankColorHex)
    @Volatile var showroomOrbitDeg = 35f
    @Volatile var showroomAutoRotate = true

    private val textures = TextureFactory()
    private val vehicleMeshes = VehicleMeshLibrary()

    private lateinit var rainDropMesh: Mesh
    private lateinit var showroomFloorMesh: Mesh
    private lateinit var sunDiscMesh: Mesh

    private var programId = 0
    private var aPos = 0
    private var aNorm = 0
    private var aUv = 0
    private var uMvp = 0
    private var uModel = 0
    private var uColor = 0
    private var uAlpha = 0
    private var uLightDir = 0
    private var uLightColor = 0
    private var uAmbientColor = 0
    private var uCamPos = 0
    private var uFogColor = 0
    private var uFogDensity = 0
    private var uShininess = 0
    private var uEmissive = 0
    private var uHeadlightPos = 0
    private var uHeadlightDir = 0
    private var uHeadlightIntensity = 0
    private var uTex = 0

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private var lastFrameNs = System.nanoTime()
    private var viewportWidth = 1
    private var viewportHeight = 1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        programId = compileShaderProgram()
        aPos = GLES20.glGetAttribLocation(programId, "aPos")
        aNorm = GLES20.glGetAttribLocation(programId, "aNorm")
        aUv = GLES20.glGetAttribLocation(programId, "aUv")
        uMvp = GLES20.glGetUniformLocation(programId, "uMvp")
        uModel = GLES20.glGetUniformLocation(programId, "uModel")
        uColor = GLES20.glGetUniformLocation(programId, "uColor")
        uAlpha = GLES20.glGetUniformLocation(programId, "uAlpha")
        uLightDir = GLES20.glGetUniformLocation(programId, "uLightDir")
        uLightColor = GLES20.glGetUniformLocation(programId, "uLightColor")
        uAmbientColor = GLES20.glGetUniformLocation(programId, "uAmbientColor")
        uCamPos = GLES20.glGetUniformLocation(programId, "uCamPos")
        uFogColor = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogDensity = GLES20.glGetUniformLocation(programId, "uFogDensity")
        uShininess = GLES20.glGetUniformLocation(programId, "uShininess")
        uEmissive = GLES20.glGetUniformLocation(programId, "uEmissive")
        uHeadlightPos = GLES20.glGetUniformLocation(programId, "uHeadlightPos")
        uHeadlightDir = GLES20.glGetUniformLocation(programId, "uHeadlightDir")
        uHeadlightIntensity = GLES20.glGetUniformLocation(programId, "uHeadlightIntensity")
        uTex = GLES20.glGetUniformLocation(programId, "uTex")

        textures.initAll()
        worldMap.buildWorld()
        vehicleMeshes.buildAll()

        rainDropMesh = MeshBuilder().apply {
            addBox(0f, 0f, 0f, 0.03f, 0.85f, 0.03f)
        }.build()
        showroomFloorMesh = MeshBuilder().apply {
            addCylinderY(0f, -0.14f, 0f, 2.3f, 2.4f, 0.14f, 28)
        }.build()
        sunDiscMesh = MeshBuilder().apply {
            addBox(0f, 0f, 0f, 16f, 16f, 2f)
        }.build()

        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs

        if (showroomMode) {
            drawShowroomFrame(dt)
        } else {
            simulation.step(dt)
            drawWorldFrame()
        }
    }

    private fun drawShowroomFrame(dt: Float) {
        if (showroomAutoRotate) {
            showroomOrbitDeg = (showroomOrbitDeg + 22f * dt) % 360f
        }

        GLES20.glClearColor(0.055f, 0.07f, 0.10f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 44f, aspect, 0.2f, 80f)

        val orbitRad = showroomOrbitDeg * PI.toFloat() / 180f
        val camDist = if (aspect < 1f) 3.7f else 3.1f
        val camX = sin(orbitRad) * camDist
        val camZ = cos(orbitRad) * camDist
        val camY = 1.45f
        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, 0f, 0.68f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(programId)
        GLES20.glUniform3f(uLightDir, 0.45f, 0.82f, 0.35f)
        GLES20.glUniform3f(uLightColor, 1.0f, 0.94f, 0.88f)
        GLES20.glUniform3f(uAmbientColor, 0.34f, 0.38f, 0.46f)
        GLES20.glUniform3f(uCamPos, camX, camY, camZ)
        GLES20.glUniform3f(uFogColor, 0.055f, 0.07f, 0.10f)
        GLES20.glUniform1f(uFogDensity, 0.01f)
        GLES20.glUniform3f(uHeadlightPos, 0f, 0f, 0f)
        GLES20.glUniform3f(uHeadlightDir, 0f, 0f, 1f)
        GLES20.glUniform1f(uHeadlightIntensity, 0f)

        // Turntable platform
        Matrix.setIdentityM(modelMatrix, 0)
        drawMesh(showroomFloorMesh, TexId.HIGHWAY_ROAD, 0.25f, 0.28f, 0.34f, 1f, 0.45f, 0f)

        // Showroom glow ring under bike
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0.01f, 0f)
        Matrix.scaleM(modelMatrix, 0, 1.2f, 1f, 2.3f)
        drawMesh(vehicleMeshes.shadowQuadMesh, TexId.WHITE, 0.0f, 0.0f, 0.0f, 0.55f, 0f, 0f)

        // Draw showroom bike without rider
        drawBike(
            bikeId = showroomBikeId,
            custom = showroomCustomization,
            x = 0f, y = 0f, z = 0f,
            headingDeg = 0f,
            leanDeg = 0f,
            pitchDeg = 0f,
            steerDeg = 8f,
            wheelSpinDeg = 0f,
            headlightLevel = 1,
            braking = false,
            nitroActive = false,
            drawRider = false
        )
    }

    private fun drawWorldFrame() {
        val night = simulation.getNightFactor()
        val rain = simulation.rainAmount
        val fog = simulation.fogAmount
        val clock = simulation.dayClock

        // Dynamic sky color across Dawn / Day / Sunset / Night / Rain
        val sunsetFactor = if (clock in 0.60f..0.80f) {
            1f - (kotlin.math.abs(clock - 0.70f) / 0.10f)
        } else 0f

        var skyR = (0.42f * (1f - night) + 0.04f * night) + sunsetFactor * 0.45f
        var skyG = (0.72f * (1f - night) + 0.06f * night) + sunsetFactor * 0.12f
        var skyB = (0.96f * (1f - night) + 0.13f * night) - sunsetFactor * 0.22f
        if (rain > 0.05f) {
            skyR = skyR * (1f - rain * 0.55f) + 0.25f * rain * 0.55f
            skyG = skyG * (1f - rain * 0.55f) + 0.28f * rain * 0.55f
            skyB = skyB * (1f - rain * 0.55f) + 0.34f * rain * 0.55f
        }
        skyR = skyR.coerceIn(0.03f, 0.96f)
        skyG = skyG.coerceIn(0.04f, 0.85f)
        skyB = skyB.coerceIn(0.09f, 0.98f)

        GLES20.glClearColor(skyR, skyG, skyB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val quality = simulation.settings.graphicsQuality
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        val fov = if (simulation.input.nitro && simulation.engineRunning && simulation.playerSpeedMs > 8f) 66f else 56f
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.25f, quality.drawDistance)

        val rad = simulation.playerHeadingDeg * PI.toFloat() / 180f
        val shakeX = if (simulation.camShake > 0.01f) (sin(System.nanoTime() * 0.0001f) * simulation.camShake * 0.3f) else 0f
        val shakeY = if (simulation.camShake > 0.01f) (cos(System.nanoTime() * 0.00013f) * simulation.camShake * 0.25f) else 0f

        val lookTargetX = simulation.playerX + sin(rad) * (if (simulation.cameraMode == CameraMode.FIRST_PERSON) 14f else 3.2f)
        val lookTargetY = simulation.playerY + (if (simulation.cameraMode == CameraMode.FIRST_PERSON) 1.25f else 1.05f)
        val lookTargetZ = simulation.playerZ + cos(rad) * (if (simulation.cameraMode == CameraMode.FIRST_PERSON) 14f else 3.2f)

        Matrix.setLookAtM(
            viewMatrix, 0,
            simulation.camPosX + shakeX, simulation.camPosY + shakeY, simulation.camPosZ,
            lookTargetX, lookTargetY, lookTargetZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(programId)

        // Sun/Moon directional light
        val sunAngle = (clock - 0.25f) * 2f * PI.toFloat()
        val sunY = sin(sunAngle).coerceAtLeast(0.22f)
        val sunX = cos(sunAngle) * 0.7f
        val lightLen = kotlin.math.sqrt(sunX * sunX + sunY * sunY + 0.16f)
        GLES20.glUniform3f(uLightDir, sunX / lightLen, sunY / lightLen, 0.4f / lightLen)

        val lR = (1.0f * (1f - night) + 0.28f * night) + sunsetFactor * 0.3f
        val lG = (0.96f * (1f - night) + 0.34f * night) - sunsetFactor * 0.1f
        val lB = (0.90f * (1f - night) + 0.56f * night) - sunsetFactor * 0.3f
        GLES20.glUniform3f(uLightColor, lR.coerceIn(0.2f, 1.15f), lG.coerceIn(0.2f, 1.0f), lB.coerceIn(0.2f, 1.0f))
        GLES20.glUniform3f(
            uAmbientColor,
            0.38f * (1f - night) + 0.14f * night,
            0.40f * (1f - night) + 0.16f * night,
            0.42f * (1f - night) + 0.24f * night
        )
        GLES20.glUniform3f(uCamPos, simulation.camPosX, simulation.camPosY, simulation.camPosZ)
        GLES20.glUniform3f(uFogColor, skyR, skyG, skyB)
        val fogDens = (0.0022f + fog * 0.0075f + (if (quality.drawDistance < 220f) 0.0018f else 0f))
        GLES20.glUniform1f(uFogDensity, fogDens)

        // Dynamic headlight spot cone
        val headActive = quality.enableDynamicHeadlight && simulation.headlightMode > 0 && simulation.engineRunning
        val hlX = simulation.playerX + sin(rad) * 0.85f
        val hlY = simulation.playerY + 0.92f
        val hlZ = simulation.playerZ + cos(rad) * 0.85f
        GLES20.glUniform3f(uHeadlightPos, hlX, hlY, hlZ)
        GLES20.glUniform3f(uHeadlightDir, sin(rad), -0.14f, cos(rad))
        GLES20.glUniform1f(uHeadlightIntensity, if (!headActive) 0f else if (simulation.headlightMode == 2) 1.55f else 0.95f)

        // 1. Static World Geometry (roads, buildings, temple, village, school, petrol pump, trees, ramps)
        val wetRoadShininess = if (quality.enableReflections && rain > 0.2f) 0.65f * rain else 0f
        for (batch in worldMap.staticBatches) {
            Matrix.setIdentityM(modelMatrix, 0)
            val shine = if (batch.texId == TexId.HIGHWAY_ROAD || batch.texId == TexId.CITY_ROAD) {
                batch.shininess + wetRoadShininess
            } else batch.shininess
            val em = if (night > 0.4f && batch.emissive > 0f) batch.emissive * 1.4f else batch.emissive
            drawMesh(batch.mesh, batch.texId, batch.r, batch.g, batch.b, 1f, shine, em)
        }

        // 2. Streetlights glowing bulbs at night
        if (night > 0.25f) {
            for (lamp in worldMap.streetLights) {
                if (hypot(lamp.x - simulation.playerX, lamp.z - simulation.playerZ) > quality.drawDistance) continue
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, lamp.x, 5.85f, lamp.z)
                Matrix.scaleM(modelMatrix, 0, 1.3f, 0.5f, 1.3f)
                drawMesh(worldMap.lampSphereMesh, TexId.WHITE, 1.0f, 0.82f, 0.42f, 1f, 0f, 0.95f * night)
            }
        }

        // 3. Animated Traffic Signals
        for (sig in worldMap.trafficSignals) {
            if (hypot(sig.x - simulation.playerX, sig.z - simulation.playerZ) > quality.drawDistance) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, sig.x, 4.85f, sig.z)
            drawMesh(worldMap.trafficLightHousingMesh, TexId.WHITE, 0.14f, 0.15f, 0.18f, 1f, 0.1f, 0f)

            val isRed = simulation.isSignalRedForZ(sig)
            val isYellow = simulation.isSignalYellow(sig)
            val (lr, lg, lb) = when {
                isYellow -> Triple(1.0f, 0.82f, 0.05f)
                isRed -> Triple(1.0f, 0.10f, 0.10f)
                else -> Triple(0.08f, 0.95f, 0.35f)
            }
            Matrix.setIdentityM(modelMatrix, 0)
            val bulbY = if (isRed) 5.28f else if (isYellow) 4.85f else 4.42f
            Matrix.translateM(modelMatrix, 0, sig.x, bulbY, sig.z)
            drawMesh(worldMap.lampSphereMesh, TexId.WHITE, lr, lg, lb, 1f, 0f, 0.95f)
        }

        // 4. Railway Crossing Barriers & Indian Express Train
        drawRailwayBarriersAndTrain()

        // 5. Mode Checkpoints (Racing or Mission Target Beacon)
        drawCheckpointsAndBeacons()

        // 6. Traffic Vehicles + Shadows
        for (agent in simulation.traffic) {
            if (hypot(agent.x - simulation.playerX, agent.z - simulation.playerZ) > quality.drawDistance) continue
            drawTrafficAgent(agent, night)
        }

        // 7. Rival Racers (in Racing Mode)
        if (simulation.gameMode == GameMode.RACING) {
            for (r in simulation.rivals) {
                drawBike(
                    bikeId = r.bikeId,
                    custom = BikeCustomization(
                        tankColorHex = ((0xFFL shl 24) or ((r.r * 255).toLong() shl 16) or ((r.g * 255).toLong() shl 8) or (r.b * 255).toLong())
                    ),
                    x = r.x, y = 0f, z = r.z,
                    headingDeg = r.headingDeg,
                    leanDeg = r.leanDeg,
                    pitchDeg = 0f,
                    steerDeg = 0f,
                    wheelSpinDeg = r.wheelAngle,
                    headlightLevel = if (night > 0.3f) 1 else 0,
                    braking = false,
                    nitroActive = false,
                    drawRider = true
                )
            }
        }

        // 8. Pedestrians
        for (ped in simulation.pedestrians) {
            if (hypot(ped.x - simulation.playerX, ped.z - simulation.playerZ) > quality.drawDistance * 0.75f) continue
            drawPedestrian(ped)
        }

        // 9. Player Bike (hide rider helmet/torso in First-Person POV so view over handlebars is clean!)
        val showRider = simulation.cameraMode != CameraMode.FIRST_PERSON
        drawBike(
            bikeId = simulation.activeBikeId,
            custom = simulation.activeCustomization,
            x = simulation.playerX,
            y = simulation.playerY + simulation.suspensionOffset,
            z = simulation.playerZ,
            headingDeg = simulation.playerHeadingDeg,
            leanDeg = simulation.playerLeanDeg,
            pitchDeg = simulation.playerPitchDeg,
            steerDeg = simulation.playerSteerAngleDeg,
            wheelSpinDeg = simulation.wheelSpinDeg,
            headlightLevel = if (simulation.engineRunning) simulation.headlightMode else 0,
            braking = simulation.input.brake,
            nitroActive = simulation.input.nitro && simulation.engineRunning && simulation.nitroAmount > 0.02f && simulation.playerSpeedMs > 2f,
            drawRider = showRider
        )

        // 10. Weather Rain Streaks
        if (rain > 0.08f) {
            for (drop in simulation.rainDrops) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, simulation.playerX + drop.x, drop.y, simulation.playerZ + drop.z)
                drawMesh(rainDropMesh, TexId.WHITE, 0.75f, 0.85f, 0.98f, 0.45f * rain, 0f, 0.2f)
            }
        }
    }

    private fun drawRailwayBarriersAndTrain() {
        val gateAngle = (1f - simulation.gateClosedFactor) * 78f
        for (side in listOf(-1f, 1f)) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, side * 12.2f, 1.3f, worldMap.railwayZ + side * 7.5f)
            Matrix.rotateM(modelMatrix, 0, if (side < 0) -gateAngle else gateAngle, 0f, 0f, 1f)
            Matrix.translateM(modelMatrix, 0, -side * 5.8f, 0f, 0f)
            drawMesh(worldMap.gateArmMesh, TexId.SHOP_AWNING, 1f, 1f, 1f, 1f, 0f, 0.1f)
        }

        if (simulation.trainActive) {
            // Locomotive + 4 Coaches
            for (i in 0..4) {
                val cx = simulation.trainX - i * 16.0f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, cx, 0f, worldMap.railwayZ)
                Matrix.rotateM(modelMatrix, 0, 90f, 0f, 1f, 0f)
                if (i == 0) {
                    drawMesh(vehicleMeshes.trainEngineMesh, TexId.WHITE, 0.82f, 0.14f, 0.12f, 1f, 0.35f, 0.05f)
                } else {
                    drawMesh(vehicleMeshes.trainCoachMesh, TexId.WHITE, 0.08f, 0.36f, 0.76f, 1f, 0.25f, 0f)
                }
            }
        }
    }

    private fun drawCheckpointsAndBeacons() {
        val tx = simulation.targetX ?: return
        val tz = simulation.targetZ ?: return
        val pulse = 0.85f + 0.15f * sin(System.nanoTime() * 0.000000006f)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, 0f, tz)
        Matrix.scaleM(modelMatrix, 0, pulse, 1f, pulse)
        val (r, g, b) = if (simulation.gameMode == GameMode.RACING) Triple(0f, 0.90f, 1f) else Triple(1f, 0.50f, 0f)
        drawMesh(worldMap.checkpointRingMesh, TexId.WHITE, r, g, b, 0.72f, 0.2f, 0.8f)
    }

    private fun drawTrafficAgent(agent: TrafficAgent, night: Float) {
        val m = vehicleMeshes.getTraffic(agent.type)

        // Contact ground shadow
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, agent.x, 0.04f, agent.z)
        Matrix.rotateM(modelMatrix, 0, agent.headingDeg, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, agent.halfWidth * 2.1f, 1f, agent.halfLength * 2.0f)
        drawMesh(vehicleMeshes.shadowQuadMesh, TexId.WHITE, 0f, 0f, 0f, 0.42f, 0f, 0f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, agent.x, 0f, agent.z)
        Matrix.rotateM(modelMatrix, 0, agent.headingDeg, 0f, 1f, 0f)

        drawMesh(m.bodyMesh, TexId.WHITE, agent.bodyR, agent.bodyG, agent.bodyB, 1f, 0.45f, 0f)
        drawMesh(m.glassMesh, TexId.WHITE, 0.14f, 0.20f, 0.28f, 1f, 0.85f, if (night > 0.5f) 0.12f else 0f)
        drawMesh(m.wheelMesh, TexId.WHITE, 0.12f, 0.13f, 0.15f, 1f, 0.1f, 0f)

        when (agent.type) {
            TrafficVehicleType.AUTO_RICKSHAW -> {
                drawMesh(m.trimMesh, TexId.WHITE, 0.98f, 0.78f, 0.05f, 1f, 0.25f, 0f)
            }
            TrafficVehicleType.POLICE_PCR -> {
                drawMesh(m.trimMesh, TexId.WHITE, 0.08f, 0.24f, 0.78f, 1f, 0.4f, 0f)
                m.decorMesh?.let { decor ->
                    val flash = ((System.currentTimeMillis() / 160L) % 2L) == 0L
                    drawMesh(decor, TexId.WHITE, if (flash) 1f else 0.1f, 0.12f, if (flash) 0.1f else 1f, 1f, 0.5f, 0.95f)
                }
            }
            TrafficVehicleType.TRUCK -> {
                drawMesh(m.trimMesh, TexId.WHITE, 0.22f, 0.22f, 0.24f, 1f, 0.15f, 0f)
                m.decorMesh?.let { decor ->
                    drawMesh(decor, TexId.TRUCK_DECOR, 1f, 1f, 1f, 1f, 0.2f, 0f)
                }
            }
            else -> {
                drawMesh(m.trimMesh, TexId.WHITE, 0.18f, 0.20f, 0.24f, 1f, 0.2f, 0f)
            }
        }
    }

    private fun drawPedestrian(ped: PedestrianAgent) {
        val swing = sin(ped.walkPhase) * 0.06f
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, ped.x, abs(swing), ped.z)
        Matrix.rotateM(modelMatrix, 0, ped.headingDeg, 0f, 1f, 0f)
        drawMesh(vehicleMeshes.pedestrianBodyMesh, TexId.WHITE, ped.shirtR, ped.shirtG, ped.shirtB, 1f, 0.05f, 0f)
        drawMesh(vehicleMeshes.pedestrianHeadMesh, TexId.WHITE, 0.68f, 0.48f, 0.34f, 1f, 0.05f, 0f)
    }

    private fun drawBike(
        bikeId: BikeId,
        custom: BikeCustomization,
        x: Float, y: Float, z: Float,
        headingDeg: Float,
        leanDeg: Float,
        pitchDeg: Float,
        steerDeg: Float,
        wheelSpinDeg: Float,
        headlightLevel: Int,
        braking: Boolean,
        nitroActive: Boolean,
        drawRider: Boolean
    ) {
        val bike = vehicleMeshes.getBike(bikeId)

        // Projected Ground Shadow
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 0.04f, z)
        Matrix.rotateM(modelMatrix, 0, headingDeg, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.95f, 1f, 2.15f)
        drawMesh(vehicleMeshes.shadowQuadMesh, TexId.WHITE, 0f, 0f, 0f, 0.48f, 0f, 0f)

        val tankR = ((custom.tankColorHex shr 16) and 0xFF) / 255f
        val tankG = ((custom.tankColorHex shr 8) and 0xFF) / 255f
        val tankB = (custom.tankColorHex and 0xFF) / 255f

        val rimR = ((custom.rimColorHex shr 16) and 0xFF) / 255f
        val rimG = ((custom.rimColorHex shr 8) and 0xFF) / 255f
        val rimB = (custom.rimColorHex and 0xFF) / 255f

        // Base bike body transform
        val baseBikeMatrix = FloatArray(16)
        Matrix.setIdentityM(baseBikeMatrix, 0)
        Matrix.translateM(baseBikeMatrix, 0, x, y, z)
        Matrix.rotateM(baseBikeMatrix, 0, headingDeg, 0f, 1f, 0f)
        Matrix.rotateM(baseBikeMatrix, 0, leanDeg, 0f, 0f, 1f)
        Matrix.rotateM(baseBikeMatrix, 0, pitchDeg, -1f, 0f, 0f)

        // Chassis, Painted Bodywork, Chrome, Taillight, Rider
        System.arraycopy(baseBikeMatrix, 0, modelMatrix, 0, 16)
        drawMesh(bike.chassisMesh, TexId.WHITE, 0.14f, 0.15f, 0.18f, 1f, 0.30f, 0f)
        drawMesh(bike.paintedMesh, TexId.WHITE, tankR, tankG, tankB, 1f, 0.78f, 0f)
        drawMesh(bike.chromeMesh, TexId.WHITE, 0.85f, 0.88f, 0.92f, 1f, 0.95f, 0f)
        drawMesh(bike.taillightLensMesh, TexId.WHITE, 1f, 0.08f, 0.08f, 1f, 0.2f, if (braking) 0.98f else 0.35f)
        if (drawRider) {
            drawMesh(bike.riderMesh, TexId.WHITE, 0.18f, 0.22f, 0.30f, 1f, 0.25f, 0f)
        }

        // Nitro exhaust flame
        if (nitroActive) {
            System.arraycopy(baseBikeMatrix, 0, tempMatrix, 0, 16)
            Matrix.translateM(tempMatrix, 0, -0.19f, 0.36f, -0.85f)
            val flicker = 0.8f + 0.4f * sin(System.nanoTime() * 0.00000004f)
            Matrix.scaleM(tempMatrix, 0, flicker, flicker, 1.1f * flicker)
            System.arraycopy(tempMatrix, 0, modelMatrix, 0, 16)
            drawMesh(vehicleMeshes.nitroFlameMesh, TexId.WHITE, 0.0f, 0.88f, 1.0f, 0.85f, 0f, 1.0f)
        }

        // Rear wheel (spins around X)
        System.arraycopy(baseBikeMatrix, 0, tempMatrix, 0, 16)
        Matrix.translateM(tempMatrix, 0, 0f, bike.wheelRadius, bike.rearWheelZ)
        Matrix.rotateM(tempMatrix, 0, wheelSpinDeg, 1f, 0f, 0f)
        System.arraycopy(tempMatrix, 0, modelMatrix, 0, 16)
        drawMesh(bike.rearTireMesh, TexId.WHITE, 0.10f, 0.10f, 0.11f, 1f, 0.08f, 0f)
        drawMesh(bike.rearWheelRimMesh, TexId.WHITE, rimR, rimG, rimB, 1f, 0.85f, 0f)

        // Front Steering Fork + Handlebars + Headlight
        System.arraycopy(baseBikeMatrix, 0, tempMatrix, 0, 16)
        Matrix.translateM(tempMatrix, 0, 0f, 0f, bike.frontWheelZ - 0.12f)
        Matrix.rotateM(tempMatrix, 0, steerDeg, 0f, 1f, 0f)
        System.arraycopy(tempMatrix, 0, modelMatrix, 0, 16)
        drawMesh(bike.forkSteerMesh, TexId.WHITE, 0.72f, 0.75f, 0.80f, 1f, 0.75f, 0f)
        val hlGlow = when (headlightLevel) {
            2 -> 1.0f
            1 -> 0.75f
            else -> 0.10f
        }
        drawMesh(bike.headlightLensMesh, TexId.WHITE, 1.0f, 0.96f, 0.78f, 1f, 0.3f, hlGlow)

        // Front Wheel (steers with fork AND spins around X)
        Matrix.translateM(tempMatrix, 0, 0f, bike.wheelRadius, 0.12f)
        Matrix.rotateM(tempMatrix, 0, wheelSpinDeg, 1f, 0f, 0f)
        System.arraycopy(tempMatrix, 0, modelMatrix, 0, 16)
        drawMesh(bike.frontTireMesh, TexId.WHITE, 0.10f, 0.10f, 0.11f, 1f, 0.08f, 0f)
        drawMesh(bike.frontWheelRimMesh, TexId.WHITE, rimR, rimG, rimB, 1f, 0.85f, 0f)
    }

    private fun drawMesh(
        mesh: Mesh,
        texId: TexId,
        r: Float, g: Float, b: Float,
        alpha: Float,
        shininess: Float,
        emissive: Float
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(uColor, r, g, b)
        GLES20.glUniform1f(uAlpha, alpha)
        GLES20.glUniform1f(uShininess, shininess)
        GLES20.glUniform1f(uEmissive, emissive)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures.get(texId))
        GLES20.glUniform1i(uTex, 0)

        mesh.draw(aPos, aNorm, aUv)
    }

    private fun compileShaderProgram(): Int {
        val vertexSrc = """
            attribute vec3 aPos;
            attribute vec3 aNorm;
            attribute vec2 aUv;
            uniform mat4 uMvp;
            uniform mat4 uModel;
            varying vec3 vWorldPos;
            varying vec3 vWorldNorm;
            varying vec2 vUv;
            void main() {
                vec4 worldPos = uModel * vec4(aPos, 1.0);
                vWorldPos = worldPos.xyz;
                vWorldNorm = normalize((uModel * vec4(aNorm, 0.0)).xyz);
                vUv = aUv;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """.trimIndent()

        val fragSrc = """
            precision mediump float;
            varying vec3 vWorldPos;
            varying vec3 vWorldNorm;
            varying vec2 vUv;
            uniform sampler2D uTex;
            uniform vec3 uColor;
            uniform float uAlpha;
            uniform vec3 uLightDir;
            uniform vec3 uLightColor;
            uniform vec3 uAmbientColor;
            uniform vec3 uCamPos;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform float uShininess;
            uniform float uEmissive;
            uniform vec3 uHeadlightPos;
            uniform vec3 uHeadlightDir;
            uniform float uHeadlightIntensity;

            void main() {
                vec4 texSample = texture2D(uTex, vUv);
                vec3 baseColor = texSample.rgb * uColor;
                vec3 N = normalize(vWorldNorm);
                vec3 L = normalize(uLightDir);
                float ndotl = max(dot(N, L), 0.0);

                // Hemisphere ambient + sun diffuse
                float hemi = N.y * 0.5 + 0.5;
                vec3 ambient = uAmbientColor * (0.75 + 0.25 * hemi);
                vec3 lit = baseColor * (ambient + uLightColor * ndotl * 0.85);

                // Specular reflection for metallic bodywork and wet roads
                if (uShininess > 0.05) {
                    vec3 V = normalize(uCamPos - vWorldPos);
                    vec3 H = normalize(L + V);
                    float spec = pow(max(dot(N, H), 0.0), 28.0);
                    lit += uLightColor * spec * uShininess * 0.65;
                }

                // Dynamic Bike Headlight Cone
                if (uHeadlightIntensity > 0.01) {
                    vec3 toFrag = vWorldPos - uHeadlightPos;
                    float dist = length(toFrag);
                    if (dist < 42.0) {
                        vec3 dir = toFrag / max(dist, 0.001);
                        float cone = dot(dir, normalize(uHeadlightDir));
                        float spot = smoothstep(0.76, 0.94, cone);
                        float atten = 1.0 - smoothstep(4.0, 42.0, dist);
                        lit += baseColor * vec3(1.0, 0.94, 0.78) * spot * atten * uHeadlightIntensity;
                    }
                }

                // Emissive override for headlights, taillights, traffic signals, temple lamps
                lit += baseColor * uEmissive;

                // Distance Fog
                float camDist = length(uCamPos - vWorldPos);
                float fogFactor = clamp(exp(-pow(camDist * uFogDensity, 1.4)), 0.0, 1.0);
                vec3 finalRgb = mix(uFogColor, lit, fogFactor);

                gl_FragColor = vec4(finalRgb, texSample.a * uAlpha);
            }
        """.trimIndent()

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }
}
