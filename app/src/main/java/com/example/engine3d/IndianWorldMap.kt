package com.example.engine3d

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class RenderBatch(
    val mesh: Mesh,
    val texId: TexId,
    val r: Float = 1f,
    val g: Float = 1f,
    val b: Float = 1f,
    val shininess: Float = 0.1f,
    val emissive: Float = 0f
)

data class BoxCollider(
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float,
    val label: String = ""
) {
    fun containsXZ(x: Float, z: Float, margin: Float = 0f): Boolean {
        return x >= minX - margin && x <= maxX + margin && z >= minZ - margin && z <= maxZ + margin
    }
}

data class StuntRamp(
    val cx: Float,
    val cz: Float,
    val width: Float,
    val length: Float,
    val height: Float,
    val slopeTowardPositiveZ: Boolean
) {
    fun getSurfaceHeight(x: Float, z: Float): Float {
        val hw = width * 0.5f
        val hl = length * 0.5f
        if (abs(x - cx) > hw || abs(z - cz) > hl) return 0f
        val t = if (slopeTowardPositiveZ) {
            (z - (cz - hl)) / length
        } else {
            ((cz + hl) - z) / length
        }
        return (t.coerceIn(0f, 1f)) * height
    }
}

data class TrafficSignalPole(
    val x: Float,
    val z: Float,
    val axisAlongZ: Boolean, // true = controls Z traffic, false = controls X traffic
    val phaseOffset: Float = 0f
)

data class StreetLightPos(
    val x: Float,
    val z: Float
)

data class RoadSegment(
    val x0: Float,
    val z0: Float,
    val x1: Float,
    val z1: Float,
    val isHighway: Boolean = false,
    val isDirt: Boolean = false
)

enum class LandmarkKind {
    PETROL_PUMP,
    TEMPLE,
    SCHOOL,
    VILLAGE,
    BAZAAR,
    RAILWAY,
    STUNT_ARENA,
    HIGHWAY
}

data class WorldLandmark(
    val id: String,
    val name: String,
    val shortLabel: String,
    val x: Float,
    val z: Float,
    val kind: LandmarkKind
)

data class BuildingFootprint(
    val cx: Float,
    val cz: Float,
    val width: Float,
    val depth: Float,
    val isVillageHut: Boolean = false
)

class IndianWorldMap {
    val staticBatches = mutableListOf<RenderBatch>()
    val colliders = mutableListOf<BoxCollider>()
    val ramps = mutableListOf<StuntRamp>()
    val trafficSignals = mutableListOf<TrafficSignalPole>()
    val streetLights = mutableListOf<StreetLightPos>()
    val minimapRoads = mutableListOf<RoadSegment>()
    val minimapLandmarks = mutableListOf<WorldLandmark>()
    val minimapBuildings = mutableListOf<BuildingFootprint>()

    // Dynamic mesh parts updated/drawn separately
    lateinit var trafficLightHousingMesh: Mesh
    lateinit var lampSphereMesh: Mesh
    lateinit var checkpointRingMesh: Mesh
    lateinit var gateArmMesh: Mesh

    val worldBound = 540f
    val railwayZ = -240f
    val petrolPumpX = 115f
    val petrolPumpZ = -105f

    fun buildWorld() {
        staticBatches.clear()
        colliders.clear()
        ramps.clear()
        trafficSignals.clear()
        streetLights.clear()
        minimapRoads.clear()
        minimapLandmarks.clear()
        minimapBuildings.clear()

        buildGroundAndRoads()
        buildCityBuildingsAndMarket()
        buildIndianVillage()
        buildTempleAndSchool()
        buildPetrolPump()
        buildRailwayCrossing()
        buildHighwayLandmarksAndRamps()
        buildTreesAndFoliage()
        buildDynamicHelperMeshes()
        populateMinimapLandmarks()
    }

    private fun buildGroundAndRoads() {
        // Expansive countryside grass floor
        val grass = MeshBuilder()
        grass.addGroundQuad(0f, -0.02f, 0f, 1200f, 1200f, 95f, 95f)
        staticBatches.add(RenderBatch(grass.build(), TexId.GRASS_FIELD, 0.92f, 0.95f, 0.88f))

        // National Highway NH-44 (North-South at x=0, width 20m, from z=-520..520)
        val highway = MeshBuilder()
        highway.addGroundQuad(0f, 0.01f, 0f, 20f, 1040f, 1f, 52f)
        // East-West Highway Loop Connector at z=-380 & z=380
        highway.addGroundQuad(0f, 0.01f, -380f, 840f, 18f, 1f, 42f) // note: rotated UV visual okay
        highway.addGroundQuad(0f, 0.01f, 380f, 840f, 18f, 1f, 42f)
        staticBatches.add(RenderBatch(highway.build(), TexId.HIGHWAY_ROAD, 1f, 1f, 1f, shininess = 0.25f))

        // City Roads Grid
        val cityRoads = MeshBuilder()
        // Main East-West City Bazaar Road (z = 0, x = -220..360)
        cityRoads.addGroundQuad(70f, 0.008f, 0f, 580f, 14f, 1f, 36f)
        // South City Road (z = 120, x = -220..360)
        cityRoads.addGroundQuad(70f, 0.008f, 120f, 580f, 14f, 1f, 36f)
        // North City / Petrol Pump Road (z = -120, x = -220..360)
        cityRoads.addGroundQuad(70f, 0.008f, -120f, 580f, 14f, 1f, 36f)
        // Parallel North-South Avenues (x = -120, x = 120, x = 280, x = 410)
        cityRoads.addGroundQuad(-120f, 0.008f, 0f, 14f, 770f, 1f, 48f)
        cityRoads.addGroundQuad(120f, 0.008f, 0f, 14f, 770f, 1f, 48f)
        cityRoads.addGroundQuad(280f, 0.008f, 0f, 14f, 770f, 1f, 48f)
        cityRoads.addGroundQuad(410f, 0.008f, 0f, 14f, 770f, 1f, 48f)
        staticBatches.add(RenderBatch(cityRoads.build(), TexId.CITY_ROAD, 1f, 1f, 1f, shininess = 0.20f))

        // Indian Village Dirt Roads (west side x = -430..-210)
        val dirt = MeshBuilder()
        dirt.addGroundQuad(-310f, 0.006f, 0f, 220f, 10f, 1f, 18f)
        dirt.addGroundQuad(-310f, 0.006f, -120f, 220f, 10f, 1f, 18f)
        dirt.addGroundQuad(-310f, 0.006f, 120f, 220f, 10f, 1f, 18f)
        dirt.addGroundQuad(-300f, 0.006f, 0f, 10f, 760f, 1f, 42f)
        dirt.addGroundQuad(-410f, 0.006f, 0f, 10f, 760f, 1f, 42f)
        staticBatches.add(RenderBatch(dirt.build(), TexId.VILLAGE_DIRT, 1f, 1f, 1f))

        // Record roads for Minimap
        minimapRoads.add(RoadSegment(0f, -520f, 0f, 520f, isHighway = true))
        minimapRoads.add(RoadSegment(-420f, -380f, 420f, -380f, isHighway = true))
        minimapRoads.add(RoadSegment(-420f, 380f, 420f, 380f, isHighway = true))
        minimapRoads.add(RoadSegment(-220f, 0f, 360f, 0f))
        minimapRoads.add(RoadSegment(-220f, 120f, 360f, 120f))
        minimapRoads.add(RoadSegment(-220f, -120f, 360f, -120f))
        minimapRoads.add(RoadSegment(-120f, -380f, -120f, 380f))
        minimapRoads.add(RoadSegment(120f, -380f, 120f, 380f))
        minimapRoads.add(RoadSegment(280f, -380f, 280f, 380f))
        minimapRoads.add(RoadSegment(-420f, 0f, -220f, 0f, isDirt = true))
        minimapRoads.add(RoadSegment(-300f, -380f, -300f, 380f, isDirt = true))

        // Traffic Signal Poles at key city intersections
        val intersections = listOf(
            Pair(0f, 0f),
            Pair(120f, 0f),
            Pair(-120f, 0f),
            Pair(0f, 120f),
            Pair(0f, -120f)
        )
        val poles = MeshBuilder()
        intersections.forEachIndexed { idx, (ix, iz) ->
            poles.addCylinderY(ix + 11f, 0f, iz + 11f, 0.16f, 0.12f, 5.2f, 8)
            poles.addBox(ix + 9.2f, 4.9f, iz + 11f, 3.6f, 0.18f, 0.18f)
            trafficSignals.add(TrafficSignalPole(ix + 8.5f, iz + 11f, axisAlongZ = true, phaseOffset = idx * 3.5f))
        }
        staticBatches.add(RenderBatch(poles.build(), TexId.WHITE, 0.22f, 0.24f, 0.28f))

        // Streetlights along NH-44 & City Avenue
        val lampPosts = MeshBuilder()
        for (z in -440..440 step 55) {
            if (abs(z - railwayZ) < 16f || abs(z) < 15f) continue
            lampPosts.addCylinderY(-11.5f, 0f, z.toFloat(), 0.12f, 0.08f, 6.2f, 6)
            lampPosts.addBox(-9.8f, 6.0f, z.toFloat(), 3.2f, 0.14f, 0.16f)
            lampPosts.addCylinderY(11.5f, 0f, z.toFloat(), 0.12f, 0.08f, 6.2f, 6)
            lampPosts.addBox(9.8f, 6.0f, z.toFloat(), 3.2f, 0.14f, 0.16f)
            streetLights.add(StreetLightPos(-8.5f, z.toFloat()))
            streetLights.add(StreetLightPos(8.5f, z.toFloat()))
        }
        staticBatches.add(RenderBatch(lampPosts.build(), TexId.WHITE, 0.35f, 0.38f, 0.42f))
    }

    private fun buildCityBuildingsAndMarket() {
        val buildings = MeshBuilder()
        val awnings = MeshBuilder()
        val rng = Random(101)

        // City blocks between avenues (carefully avoiding roads at x=-120, 0, 120, 280 and z=-120, 0, 120)
        val blockCenters = listOf(
            // Bazaar blocks around City Center
            Triple(-60f, -60f, 22f), Triple(-60f, 60f, 18f),
            Triple(60f, 60f, 24f), Triple(60f, -60f, 16f),
            Triple(195f, -60f, 26f), Triple(195f, 60f, 20f),
            Triple(-180f, -60f, 14f), Triple(-180f, 60f, 14f),
            Triple(60f, -180f, 22f), Triple(-60f, -180f, 19f),
            Triple(60f, 200f, 25f), Triple(-60f, 200f, 20f),
            Triple(195f, 200f, 28f), Triple(195f, -180f, 18f)
        )

        for ((bx, bz, h) in blockCenters) {
            val w = 26f + rng.nextFloat() * 14f
            val d = 24f + rng.nextFloat() * 14f
            buildings.addBox(bx, h * 0.5f, bz, w, h, d, 2f, max(1f, h / 8f))
            addCollider(bx, bz, w, d, "Building")
            minimapBuildings.add(BuildingFootprint(bx, bz, w, d, isVillageHut = false))

            // Market stalls / awnings flanking the building edge
            awnings.addBox(bx, 2.9f, bz + d * 0.5f + 2.2f, w * 0.72f, 0.35f, 3.6f, 4f, 1f)
            awnings.addBox(bx, 2.9f, bz - d * 0.5f - 2.2f, w * 0.72f, 0.35f, 3.6f, 4f, 1f)
        }

        // Smaller Bazaar Chai & Dhaba shops along z=0
        val shopSpots = listOf(
            Pair(38f, 18f), Pair(82f, 18f), Pair(158f, -18f),
            Pair(225f, 18f), Pair(-42f, -18f), Pair(-82f, 18f)
        )
        for ((sx, sz) in shopSpots) {
            buildings.addBox(sx, 2.6f, sz, 11f, 5.2f, 8f, 1f, 1f)
            addCollider(sx, sz, 11f, 8f, "Market Shop")
            minimapBuildings.add(BuildingFootprint(sx, sz, 11f, 8f, isVillageHut = false))
            val awningZ = if (sz > 0) sz - 5.2f else sz + 5.2f
            awnings.addBox(sx, 2.8f, awningZ, 10.5f, 0.25f, 2.8f, 3f, 1f)
        }

        staticBatches.add(RenderBatch(buildings.build(), TexId.BUILDING_CITY, 1f, 1f, 1f))
        staticBatches.add(RenderBatch(awnings.build(), TexId.SHOP_AWNING, 1f, 1f, 1f))
    }

    private fun buildIndianVillage() {
        val mudWalls = MeshBuilder()
        val thatchRoofs = MeshBuilder()
        val hutPositions = listOf(
            Pair(-255f, -35f), Pair(-255f, 45f), Pair(-345f, -40f), Pair(-345f, 50f),
            Pair(-260f, -165f), Pair(-355f, -165f), Pair(-265f, 165f), Pair(-355f, 165f),
            Pair(-455f, -60f), Pair(-455f, 70f)
        )

        for ((hx, hz) in hutPositions) {
            mudWalls.addBox(hx, 2.1f, hz, 10f, 4.2f, 9f)
            thatchRoofs.addPyramidRoof(hx, 4.2f, hz, 12.2f, 11.2f, 3.4f)
            addCollider(hx, hz, 10f, 9f, "Village Hut")
            minimapBuildings.add(BuildingFootprint(hx, hz, 10f, 9f, isVillageHut = true))
        }

        // Village stone well landmark
        mudWalls.addCylinderY(-320f, 0f, 18f, 2.6f, 2.6f, 1.4f, 12)
        addCollider(-320f, 18f, 5.2f, 5.2f, "Village Well")

        staticBatches.add(RenderBatch(mudWalls.build(), TexId.TEMPLE_STONE, 0.94f, 0.85f, 0.72f))
        staticBatches.add(RenderBatch(thatchRoofs.build(), TexId.VILLAGE_DIRT, 0.88f, 0.72f, 0.42f))
    }

    private fun buildTempleAndSchool() {
        // 1. Indian Sandstone Temple (East hill plateau around x=340, z=250)
        val temple = MeshBuilder()
        val tx = 340f; val tz = 250f
        // Stepped plinth
        temple.addBox(tx, 0.9f, tz, 34f, 1.8f, 34f, 3f, 1f)
        // Main mandapa hall
        temple.addBox(tx, 5.2f, tz, 20f, 7.0f, 22f, 2f, 1f)
        // Towering Shikhara stepped spire
        temple.addBox(tx, 11.0f, tz - 2f, 12f, 5.0f, 12f, 2f, 1f)
        temple.addBox(tx, 15.5f, tz - 2f, 8.2f, 4.2f, 8.2f, 1f, 1f)
        temple.addPyramidRoof(tx, 17.6f, tz - 2f, 7.8f, 7.8f, 7.2f)
        addCollider(tx, tz, 22f, 24f, "Temple")
        staticBatches.add(RenderBatch(temple.build(), TexId.TEMPLE_STONE, 1f, 0.96f, 0.88f))

        // Saffron Temple Flag
        val flag = MeshBuilder()
        flag.addCylinderY(tx, 24.5f, tz - 2f, 0.12f, 0.08f, 4.5f, 6)
        flag.addBox(tx + 1.5f, 28.0f, tz - 2f, 3.0f, 1.4f, 0.12f)
        staticBatches.add(RenderBatch(flag.build(), TexId.WHITE, 1.0f, 0.43f, 0.0f, emissive = 0.2f))

        // 2. Government School (x=-155, z=155)
        val school = MeshBuilder()
        val sx = -155f; val sz = 165f
        school.addBox(sx, 3.4f, sz, 34f, 6.8f, 16f, 2f, 1f)
        school.addBox(sx - 12f, 3.4f, sz + 14f, 10f, 6.8f, 14f, 1f, 1f)
        addCollider(sx, sz, 34f, 16f, "School")
        addCollider(sx - 12f, sz + 14f, 10f, 14f, "School Wing")
        staticBatches.add(RenderBatch(school.build(), TexId.BUILDING_CITY, 0.98f, 0.90f, 0.65f))
    }

    private fun buildPetrolPump() {
        val px = petrolPumpX
        val pz = petrolPumpZ
        val canopy = MeshBuilder()
        val pillars = MeshBuilder()
        // Canopy roof
        canopy.addBox(px, 6.2f, pz, 24f, 1.4f, 16f, 2f, 1f)
        // Concrete forecourt pad
        pillars.addGroundQuad(px, 0.03f, pz, 28f, 22f)
        // 4 canopy pillars
        for (dx in listOf(-9f, 9f)) {
            for (dz in listOf(-5.5f, 5.5f)) {
                pillars.addBox(px + dx, 3.0f, pz + dz, 0.8f, 6.0f, 0.8f)
            }
        }
        // Fuel dispenser islands (ride between them to refuel!)
        val pumps = MeshBuilder()
        pumps.addBox(px - 3.5f, 1.1f, pz, 1.6f, 2.2f, 1.2f)
        pumps.addBox(px + 3.5f, 1.1f, pz, 1.6f, 2.2f, 1.2f)
        addCollider(px - 3.5f, pz, 1.6f, 1.2f, "Fuel Dispenser")
        addCollider(px + 3.5f, pz, 1.6f, 1.2f, "Fuel Dispenser")

        staticBatches.add(RenderBatch(canopy.build(), TexId.PETROL_CANOPY, 1f, 1f, 1f, emissive = 0.15f))
        staticBatches.add(RenderBatch(pillars.build(), TexId.WHITE, 0.84f, 0.86f, 0.90f))
        staticBatches.add(RenderBatch(pumps.build(), TexId.WHITE, 0.95f, 0.35f, 0.05f))
    }

    private fun buildRailwayCrossing() {
        val rail = MeshBuilder()
        rail.addGroundQuad(0f, 0.025f, railwayZ, 1100f, 6.5f, 140f, 1f)
        staticBatches.add(RenderBatch(rail.build(), TexId.RAILWAY_TRACK, 1f, 1f, 1f))

        // Crossing signal posts on NH-44 (x = -13, +13)
        val posts = MeshBuilder()
        posts.addBox(-12.5f, 1.5f, railwayZ - 7.5f, 0.7f, 3.0f, 0.7f)
        posts.addBox(12.5f, 1.5f, railwayZ + 7.5f, 0.7f, 3.0f, 0.7f)
        staticBatches.add(RenderBatch(posts.build(), TexId.WHITE, 0.95f, 0.75f, 0.08f))
    }

    private fun buildHighwayLandmarksAndRamps() {
        val rampsMesh = MeshBuilder()
        val rampDefs = listOf(
            // Highway jump ramps
            StuntRamp(cx = 5.5f, cz = -165f, width = 7.5f, length = 14f, height = 2.6f, slopeTowardPositiveZ = false),
            StuntRamp(cx = -5.5f, cz = 175f, width = 7.5f, length = 14f, height = 2.6f, slopeTowardPositiveZ = true),
            // Stunt Arena mega ramps (around x=280, z=-300)
            StuntRamp(cx = 265f, cz = -290f, width = 10f, length = 16f, height = 3.8f, slopeTowardPositiveZ = false),
            StuntRamp(cx = 295f, cz = -325f, width = 10f, length = 16f, height = 4.2f, slopeTowardPositiveZ = true),
            StuntRamp(cx = 325f, cz = -290f, width = 10f, length = 18f, height = 4.8f, slopeTowardPositiveZ = false)
        )
        for (r in rampDefs) {
            ramps.add(r)
            rampsMesh.addRamp(r.cx, r.cz, r.width, r.length, r.height, r.slopeTowardPositiveZ)
        }
        staticBatches.add(RenderBatch(rampsMesh.build(), TexId.RAMP_CHEVRON, 1f, 1f, 1f))

        // Highway Overhead Gantries (NH-44 signs)
        val gantry = MeshBuilder()
        val board = MeshBuilder()
        for (gz in listOf(-310f, 280f)) {
            gantry.addCylinderY(-12.5f, 0f, gz, 0.3f, 0.25f, 7.5f, 8)
            gantry.addCylinderY(12.5f, 0f, gz, 0.3f, 0.25f, 7.5f, 8)
            gantry.addBox(0f, 7.2f, gz, 26f, 0.45f, 0.45f)
            board.addBox(0f, 7.2f, gz, 13f, 2.2f, 0.6f)
        }
        staticBatches.add(RenderBatch(gantry.build(), TexId.WHITE, 0.65f, 0.68f, 0.72f))
        staticBatches.add(RenderBatch(board.build(), TexId.WHITE, 0.05f, 0.46f, 0.22f)) // Indian NH Green Sign
    }

    private fun buildTreesAndFoliage() {
        val trunks = MeshBuilder()
        val crowns = MeshBuilder()
        val rng = Random(777)

        val treeSpots = mutableListOf<Pair<Float, Float>>()
        // Village & Countryside trees
        for (i in 0 until 75) {
            val tx = -490f + rng.nextFloat() * 980f
            val tz = -490f + rng.nextFloat() * 980f
            // Keep clear of roads, railway, petrol pump, and stunt park
            if (abs(tx) < 18f || abs(tz) < 16f) continue
            if (abs(tz - 120f) < 16f || abs(tz + 120f) < 16f) continue
            if (abs(tz - 380f) < 18f || abs(tz + 380f) < 18f) continue
            if (abs(tz - railwayZ) < 16f) continue
            if (abs(tx - 120f) < 16f || abs(tx + 120f) < 16f || abs(tx - 280f) < 16f) continue
            if (abs(tx + 300f) < 14f || abs(tx + 410f) < 14f) continue
            if (tx in 240f..350f && tz in -360f..-250f) continue
            if (colliders.any { it.containsXZ(tx, tz, 4.5f) }) continue
            treeSpots.add(Pair(tx, tz))
        }

        for ((tx, tz) in treeSpots) {
            val h = 4.2f + rng.nextFloat() * 3.8f
            trunks.addCylinderY(tx, 0f, tz, 0.36f, 0.22f, h, 6)
            crowns.addPyramidRoof(tx, h - 0.5f, tz, 4.6f, 4.6f, 3.6f)
            crowns.addBox(tx, h - 0.4f, tz, 3.6f, 1.8f, 3.6f)
            addCollider(tx, tz, 0.9f, 0.9f, "Tree")
        }

        staticBatches.add(RenderBatch(trunks.build(), TexId.VILLAGE_DIRT, 0.55f, 0.36f, 0.20f))
        staticBatches.add(RenderBatch(crowns.build(), TexId.GRASS_FIELD, 0.35f, 0.72f, 0.28f))
    }

    private fun buildDynamicHelperMeshes() {
        val sig = MeshBuilder()
        sig.addBox(0f, 0f, 0f, 0.7f, 1.6f, 0.5f)
        trafficLightHousingMesh = sig.build()

        val bulb = MeshBuilder()
        bulb.addBox(0f, 0f, 0f, 0.42f, 0.38f, 0.54f)
        lampSphereMesh = bulb.build()

        val ring = MeshBuilder()
        ring.addCylinderY(-3.5f, 0f, 0f, 0.22f, 0.22f, 6.2f, 8)
        ring.addCylinderY(3.5f, 0f, 0f, 0.22f, 0.22f, 6.2f, 8)
        ring.addBox(0f, 6.2f, 0f, 7.6f, 0.8f, 0.5f)
        checkpointRingMesh = ring.build()

        val arm = MeshBuilder()
        arm.addBox(0f, 0f, 0f, 12.2f, 0.28f, 0.28f)
        gateArmMesh = arm.build()
    }

    private fun populateMinimapLandmarks() {
        minimapBuildings.add(BuildingFootprint(340f, 250f, 34f, 34f, isVillageHut = false)) // Temple
        minimapBuildings.add(BuildingFootprint(-155f, 165f, 34f, 16f, isVillageHut = false)) // School
        minimapBuildings.add(BuildingFootprint(petrolPumpX, petrolPumpZ, 24f, 16f, isVillageHut = false)) // Petrol Pump

        minimapLandmarks.addAll(
            listOf(
                WorldLandmark("bazaar", "City Bazaar", "BAZAAR", 0f, 0f, LandmarkKind.BAZAAR),
                WorldLandmark("petrol", "Bharat Petrol Pump", "FUEL", petrolPumpX, petrolPumpZ, LandmarkKind.PETROL_PUMP),
                WorldLandmark("phatak", "Railway Phatak", "PHATAK", 0f, railwayZ, LandmarkKind.RAILWAY),
                WorldLandmark("temple", "Hilltop Temple", "TEMPLE", 340f, 250f, LandmarkKind.TEMPLE),
                WorldLandmark("school", "Govt. School", "SCHOOL", -155f, 165f, LandmarkKind.SCHOOL),
                WorldLandmark("village", "Bharat Village", "VILLAGE", -320f, 18f, LandmarkKind.VILLAGE),
                WorldLandmark("stunt", "Stunt Arena", "STUNT", 295f, -305f, LandmarkKind.STUNT_ARENA),
                WorldLandmark("nh_north", "NH-44 North Gate", "NH-44", 0f, -310f, LandmarkKind.HIGHWAY),
                WorldLandmark("nh_south", "NH-44 South Gate", "NH-44", 0f, 280f, LandmarkKind.HIGHWAY)
            )
        )
    }

    private fun addCollider(cx: Float, cz: Float, sx: Float, sz: Float, label: String) {
        colliders.add(
            BoxCollider(
                minX = cx - sx * 0.5f,
                maxX = cx + sx * 0.5f,
                minZ = cz - sz * 0.5f,
                maxZ = cz + sz * 0.5f,
                label = label
            )
        )
    }

    fun getZoneName(x: Float, z: Float): String {
        return when {
            abs(x) < 22f -> "NH-44 National Highway"
            x < -210f -> "Bharat Village"
            abs(z - railwayZ) < 25f -> "Railway Phatak"
            x in 90f..145f && z in -130f..-80f -> "Bharat Petrol Pump"
            x in 290f..390f && z in 200f..300f -> "Hilltop Temple"
            x in -190f..-110f && z in 135f..200f -> "Govt. School"
            x in 240f..355f && z in -355f..-265f -> "Stunt Arena"
            abs(x) < 160f && abs(z) < 140f -> "City Bazaar"
            else -> "Open Countryside"
        }
    }

    fun getRampHeightAt(x: Float, z: Float): Float {
        var h = 0f
        for (r in ramps) {
            h = max(h, r.getSurfaceHeight(x, z))
        }
        return h
    }

    fun isNearPetrolPump(x: Float, z: Float): Boolean {
        return abs(x - petrolPumpX) < 11f && abs(z - petrolPumpZ) < 7.5f
    }

    fun clampToWorld(x: Float): Float = min(worldBound, max(-worldBound, x))
}
