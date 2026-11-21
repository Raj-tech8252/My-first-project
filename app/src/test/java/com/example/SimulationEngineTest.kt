package com.example

import com.example.audio.SoundEngine
import com.example.engine3d.IndianWorldMap
import com.example.engine3d.SimulationEngine
import com.example.model.AllMissions
import com.example.model.BikeCustomization
import com.example.model.BikeId
import com.example.model.CameraMode
import com.example.model.ControlInputState
import com.example.model.GameMode
import com.example.model.GameSettingsState
import com.example.model.GraphicsQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationEngineTest {

    @Test
    fun bikeAcceleratesSteersAndCyclesControls() {
        val map = IndianWorldMap().apply { buildWorld() }
        val sound = SoundEngine()
        val sim = SimulationEngine(map, sound)

        sim.startSession(
            mode = GameMode.FREE_RIDE,
            bikeId = BikeId.KTM_DUKE,
            customization = BikeCustomization(BikeId.KTM_DUKE.defaultTankColorHex),
            gameSettings = GameSettingsState(graphicsQuality = GraphicsQuality.MEDIUM)
        )

        val startZ = sim.playerZ
        sim.input = ControlInputState(throttle = true)
        repeat(40) { sim.step(0.03f) }

        assertTrue("Bike should accelerate forward", sim.playerSpeedMs > 5f)
        assertTrue("Bike should move along heading", sim.playerZ != startZ)

        val headingBefore = sim.playerHeadingDeg
        sim.input = ControlInputState(throttle = true, steerLeft = true)
        repeat(20) { sim.step(0.03f) }
        assertTrue("Bike heading should change when steering left", sim.playerHeadingDeg != headingBefore)
        assertTrue("Bike should lean into turn", sim.playerLeanDeg != 0f)

        // Camera cycle
        assertEquals(CameraMode.CHASE_NEAR, sim.cameraMode)
        sim.cycleCamera()
        assertEquals(CameraMode.CHASE_FAR, sim.cameraMode)
        sim.cycleCamera()
        assertEquals(CameraMode.FIRST_PERSON, sim.cameraMode)

        // Headlight cycle
        assertEquals(0, sim.headlightMode)
        sim.cycleHeadlight()
        assertEquals(1, sim.headlightMode)
        sim.cycleHeadlight()
        assertEquals(2, sim.headlightMode)

        // Engine toggle
        assertTrue(sim.engineRunning)
        sim.toggleEngine()
        assertFalse(sim.engineRunning)
    }

    @Test
    fun trafficAndPedestriansPopulateAccordingToQuality() {
        val map = IndianWorldMap().apply { buildWorld() }
        val sim = SimulationEngine(map, SoundEngine())

        sim.startSession(
            mode = GameMode.FREE_RIDE,
            bikeId = BikeId.ROYAL_ENFIELD_BULLET,
            customization = BikeCustomization(BikeId.ROYAL_ENFIELD_BULLET.defaultTankColorHex),
            gameSettings = GameSettingsState(graphicsQuality = GraphicsQuality.HIGH)
        )
        assertTrue("Traffic agents should be spawned", sim.traffic.isNotEmpty())
        assertTrue("Pedestrians should be spawned", sim.pedestrians.isNotEmpty())
    }

    @Test
    fun missionCompletesWhenReachingTarget() {
        val map = IndianWorldMap().apply { buildWorld() }
        val sim = SimulationEngine(map, SoundEngine())
        val mission = AllMissions.first()

        sim.startSession(
            mode = GameMode.MISSION,
            bikeId = BikeId.YAMAHA_R15,
            customization = BikeCustomization(BikeId.YAMAHA_R15.defaultTankColorHex),
            gameSettings = GameSettingsState(),
            mission = mission
        )

        sim.playerX = mission.targetX
        sim.playerZ = mission.targetZ
        sim.step(0.03f)

        assertNotNull("Mission result should trigger at target", sim.sessionResult)
        assertTrue("Mission should succeed", sim.sessionResult!!.success)
        assertEquals(mission.rewardCoins, sim.sessionResult!!.coinsEarned)
    }

    @Test
    fun worldMapPopulatesLandmarksAndBuildingsForMinimap() {
        val map = IndianWorldMap().apply { buildWorld() }
        assertTrue("Minimap landmarks should include key Indian landmarks", map.minimapLandmarks.size >= 7)
        assertTrue("Minimap buildings should include city/village footprints", map.minimapBuildings.isNotEmpty())
        assertTrue(map.minimapLandmarks.any { it.shortLabel == "FUEL" })
        assertTrue(map.minimapLandmarks.any { it.shortLabel == "TEMPLE" })
        assertTrue(map.minimapLandmarks.any { it.shortLabel == "PHATAK" })
    }
}
