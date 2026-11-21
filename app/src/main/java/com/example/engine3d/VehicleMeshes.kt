package com.example.engine3d

import com.example.model.BikeId
import com.example.model.BikeStyle

enum class TrafficVehicleType {
    CAR,
    BUS,
    TRUCK,
    AUTO_RICKSHAW,
    TRACTOR,
    POLICE_PCR
}

data class BikeModelMeshes(
    val chassisMesh: Mesh,       // frame, engine block, seat, mudguards, tires
    val paintedMesh: Mesh,       // fuel tank, side panels, fairing (tinted by player paint)
    val chromeMesh: Mesh,        // exhaust, forks, mirrors, engine fins
    val frontWheelRimMesh: Mesh, // front rim + spokes (steers & spins)
    val rearWheelRimMesh: Mesh,  // rear rim + spokes (spins)
    val frontTireMesh: Mesh,     // front tire (steers & spins)
    val rearTireMesh: Mesh,      // rear tire (spins)
    val forkSteerMesh: Mesh,     // steering fork, handlebars, headlight housing
    val headlightLensMesh: Mesh, // glowing lens at front
    val taillightLensMesh: Mesh, // red brake light at rear
    val riderMesh: Mesh,         // seated rider body + helmet + limbs
    val frontWheelZ: Float,
    val rearWheelZ: Float,
    val wheelRadius: Float
)

data class TrafficModelMeshes(
    val bodyMesh: Mesh,
    val trimMesh: Mesh,
    val glassMesh: Mesh,
    val wheelMesh: Mesh,
    val decorMesh: Mesh? = null
)

class VehicleMeshLibrary {
    private val bikeMeshes = mutableMapOf<BikeId, BikeModelMeshes>()
    private val trafficMeshes = mutableMapOf<TrafficVehicleType, TrafficModelMeshes>()
    lateinit var pedestrianBodyMesh: Mesh
    lateinit var pedestrianHeadMesh: Mesh
    lateinit var shadowQuadMesh: Mesh
    lateinit var trainEngineMesh: Mesh
    lateinit var trainCoachMesh: Mesh
    lateinit var nitroFlameMesh: Mesh

    fun buildAll() {
        BikeId.entries.forEach { id ->
            bikeMeshes[id] = buildBikeModel(id)
        }
        TrafficVehicleType.entries.forEach { type ->
            trafficMeshes[type] = buildTrafficModel(type)
        }
        buildPedestrianMeshes()
        buildTrainMeshes()
        buildShadowAndFlame()
    }

    fun getBike(id: BikeId): BikeModelMeshes = bikeMeshes[id]!!
    fun getTraffic(type: TrafficVehicleType): TrafficModelMeshes = trafficMeshes[type]!!

    private fun buildBikeModel(id: BikeId): BikeModelMeshes {
        val chassis = MeshBuilder()
        val painted = MeshBuilder()
        val chrome = MeshBuilder()
        val frontRim = MeshBuilder()
        val rearRim = MeshBuilder()
        val frontTire = MeshBuilder()
        val rearTire = MeshBuilder()
        val fork = MeshBuilder()
        val headLens = MeshBuilder()
        val tailLens = MeshBuilder()
        val rider = MeshBuilder()

        val wheelR = when (id.style) {
            BikeStyle.CRUISER -> 0.34f
            BikeStyle.SUPERSPORT_FAIRING, BikeStyle.STREET_NAKED -> 0.32f
            else -> 0.31f
        }
        val frontZ = if (id.style == BikeStyle.CRUISER) 0.76f else 0.70f
        val rearZ = -0.68f
        val tireWidth = if (id.style == BikeStyle.SUPERSPORT_FAIRING || id.style == BikeStyle.STREET_NAKED) 0.14f else 0.11f

        // Wheels centered at origin for clean local rotation (placed at wheelZ, wheelR during draw)
        frontTire.addWheelX(0f, 0f, 0f, wheelR, tireWidth, 18)
        frontRim.addWheelX(0f, 0f, 0f, wheelR * 0.64f, tireWidth + 0.015f, 14)
        rearTire.addWheelX(0f, 0f, 0f, wheelR, tireWidth * 1.18f, 18)
        rearRim.addWheelX(0f, 0f, 0f, wheelR * 0.64f, tireWidth * 1.18f + 0.015f, 14)

        // Swingarm & frame backbone
        chassis.addBox(0f, wheelR + 0.03f, -0.34f, 0.16f, 0.07f, 0.64f)
        chassis.addBox(0f, 0.56f, 0.02f, 0.20f, 0.24f, 0.44f) // Engine crankcase
        chrome.addBox(0f, 0.68f, 0.05f, 0.19f, 0.20f, 0.24f)  // Cylinder cooling fins
        // Seat
        val seatY = if (id.style == BikeStyle.CRUISER) 0.78f else 0.84f
        chassis.addBox(0f, seatY, -0.34f, 0.26f, 0.10f, 0.54f)
        if (id.style == BikeStyle.SUPERSPORT_FAIRING || id.style == BikeStyle.STREET_NAKED || id.style == BikeStyle.TRACK_NAKED) {
            // Sport split pillion seat
            chassis.addRotatedBoxX(0f, seatY + 0.08f, -0.62f, 0.22f, 0.08f, 0.28f, -0.22f)
        }

        // Rear shocks & mudguard
        chrome.addCylinderY(-0.11f, wheelR + 0.05f, -0.52f, 0.025f, 0.025f, 0.34f, 8)
        chrome.addCylinderY(0.11f, wheelR + 0.05f, -0.52f, 0.025f, 0.025f, 0.34f, 8)
        painted.addRotatedBoxX(0f, wheelR + 0.28f, -0.70f, 0.17f, 0.05f, 0.36f, 0.38f)

        // Exhaust muffler (iconic long chrome pipe on Bullet, stubby sport pipe on Duke/R15)
        if (id == BikeId.ROYAL_ENFIELD_BULLET || id == BikeId.HERO_SPLENDOR || id == BikeId.HONDA_SHINE) {
            chrome.addCylinderZ(-0.19f, 0.32f, -0.86f, 0.08f, 0.052f, 10)
            chrome.addCylinderZ(-0.17f, 0.44f, 0.02f, 0.18f, 0.026f, 8)
        } else {
            chassis.addCylinderZ(-0.18f, 0.36f, -0.62f, -0.05f, 0.055f, 10)
            chrome.addCylinderZ(-0.19f, 0.38f, -0.68f, -0.58f, 0.048f, 10)
        }

        // Fuel Tank & Bodywork specific to each bike style
        when (id.style) {
            BikeStyle.CRUISER -> {
                // Bullet teardrop tank + chrome side badge
                painted.addBox(0f, 0.90f, 0.18f, 0.34f, 0.22f, 0.46f)
                painted.addBox(0f, 0.72f, -0.08f, 0.26f, 0.18f, 0.22f) // Side tool box
                chrome.addBox(0f, 0.92f, 0.18f, 0.355f, 0.05f, 0.26f)
            }
            BikeStyle.SUPERSPORT_FAIRING -> {
                // Yamaha R15 full aerodynamic front fairing & windshield
                painted.addBox(0f, 0.92f, 0.14f, 0.36f, 0.20f, 0.44f)
                painted.addRotatedBoxX(0f, 0.88f, 0.48f, 0.40f, 0.36f, 0.44f, 0.24f)
                painted.addBox(0f, 0.58f, 0.32f, 0.34f, 0.30f, 0.40f) // lower side fairing
                chassis.addRotatedBoxX(0f, 1.13f, 0.46f, 0.28f, 0.18f, 0.20f, 0.65f) // smoked windscreen
            }
            BikeStyle.STREET_NAKED, BikeStyle.TRACK_NAKED -> {
                // KTM Duke / TVS Apache angular tank shrouds & belly pan
                painted.addBox(0f, 0.92f, 0.18f, 0.36f, 0.22f, 0.42f)
                painted.addRotatedBoxX(-0.18f, 0.86f, 0.34f, 0.08f, 0.22f, 0.28f, 0.25f)
                painted.addRotatedBoxX(0.18f, 0.86f, 0.34f, 0.08f, 0.22f, 0.28f, 0.25f)
                painted.addBox(0f, 0.42f, 0.16f, 0.22f, 0.14f, 0.36f) // belly pan
            }
            BikeStyle.SPORT_COMMUTER -> {
                // Bajaj Pulsar muscular tank + wolf semi-fairing
                painted.addBox(0f, 0.91f, 0.16f, 0.36f, 0.22f, 0.46f)
                painted.addRotatedBoxX(0f, 0.94f, 0.46f, 0.32f, 0.26f, 0.28f, 0.22f)
                chassis.addBox(0f, 0.72f, -0.06f, 0.25f, 0.18f, 0.26f)
            }
            BikeStyle.CLASSIC_COMMUTER, BikeStyle.REFINED_COMMUTER -> {
                // Hero Splendor / Honda Shine commuter tank + rear grab rail
                painted.addBox(0f, 0.88f, 0.16f, 0.30f, 0.19f, 0.44f)
                painted.addBox(0f, 0.74f, -0.10f, 0.24f, 0.16f, 0.24f)
                chrome.addBox(0f, 0.83f, -0.68f, 0.26f, 0.04f, 0.18f) // rear luggage carrier
            }
        }

        // Steering Fork Assembly (placed relative to steering pivot at (0, 0, frontZ * 0.72f) or wheel center)
        // We pivot steering around (0, 0, frontZ - 0.12f)
        val forkLen = 0.68f
        chrome.let {} // fork tubes go in forkSteerMesh
        fork.addRotatedBoxX(-0.10f, wheelR + 0.30f, 0.02f, 0.045f, forkLen, 0.045f, -0.28f)
        fork.addRotatedBoxX(0.10f, wheelR + 0.30f, 0.02f, 0.045f, forkLen, 0.045f, -0.28f)
        // Front mudguard on fork
        fork.addBox(0f, wheelR + 0.10f, 0.08f, 0.16f, 0.05f, 0.34f)
        // Handlebars
        val barY = if (id.style == BikeStyle.SUPERSPORT_FAIRING) 0.98f else 1.08f
        fork.addBox(0f, barY, -0.08f, 0.66f, 0.04f, 0.05f)
        // Grips & mirrors
        fork.addBox(-0.33f, barY, -0.08f, 0.12f, 0.05f, 0.055f)
        fork.addBox(0.33f, barY, -0.08f, 0.12f, 0.05f, 0.055f)
        fork.addBox(-0.24f, barY + 0.14f, -0.05f, 0.09f, 0.06f, 0.03f)
        fork.addBox(0.24f, barY + 0.14f, -0.05f, 0.09f, 0.06f, 0.03f)
        // Speedometer cluster
        fork.addBox(0f, barY + 0.03f, 0.02f, 0.16f, 0.07f, 0.09f)
        // Headlight casing
        if (id.style == BikeStyle.CRUISER) {
            fork.addCylinderZ(0f, 0.94f, 0.04f, 0.20f, 0.11f, 12)
            headLens.addCylinderZ(0f, 0.94f, 0.19f, 0.22f, 0.095f, 12)
        } else {
            fork.addBox(0f, 0.93f, 0.12f, 0.24f, 0.16f, 0.16f)
            headLens.addBox(0f, 0.93f, 0.21f, 0.20f, 0.11f, 0.03f)
        }

        // Taillight
        tailLens.addBox(0f, 0.80f, -0.84f, 0.14f, 0.07f, 0.05f)

        // Seated Rider Character (torso, helmet, arms, legs)
        val forwardLean = if (id.style == BikeStyle.SUPERSPORT_FAIRING) 0.42f else 0.18f
        // Pelvis & torso
        rider.addRotatedBoxX(0f, seatY + 0.28f, -0.24f, 0.34f, 0.48f, 0.22f, forwardLean)
        // Helmet
        val headZ = -0.24f + if (id.style == BikeStyle.SUPERSPORT_FAIRING) 0.18f else 0.08f
        val headY = seatY + 0.64f
        rider.addBox(0f, headY, headZ, 0.24f, 0.25f, 0.27f)
        // Visor strip
        rider.addBox(0f, headY, headZ + 0.14f, 0.20f, 0.09f, 0.03f)
        // Left & right arms reaching toward handlebars
        rider.addRotatedBoxX(-0.22f, seatY + 0.34f, 0.06f, 0.09f, 0.09f, 0.44f, 0.25f)
        rider.addRotatedBoxX(0.22f, seatY + 0.34f, 0.06f, 0.09f, 0.09f, 0.44f, 0.25f)
        // Left & right legs resting on footpegs
        rider.addRotatedBoxX(-0.18f, seatY - 0.16f, -0.14f, 0.10f, 0.42f, 0.12f, -0.35f)
        rider.addRotatedBoxX(0.18f, seatY - 0.16f, -0.14f, 0.10f, 0.42f, 0.12f, -0.35f)

        return BikeModelMeshes(
            chassisMesh = chassis.build(),
            paintedMesh = painted.build(),
            chromeMesh = chrome.build(),
            frontWheelRimMesh = frontRim.build(),
            rearWheelRimMesh = rearRim.build(),
            frontTireMesh = frontTire.build(),
            rearTireMesh = rearTire.build(),
            forkSteerMesh = fork.build(),
            headlightLensMesh = headLens.build(),
            taillightLensMesh = tailLens.build(),
            riderMesh = rider.build(),
            frontWheelZ = frontZ,
            rearWheelZ = rearZ,
            wheelRadius = wheelR
        )
    }

    private fun buildTrafficModel(type: TrafficVehicleType): TrafficModelMeshes {
        val body = MeshBuilder()
        val trim = MeshBuilder()
        val glass = MeshBuilder()
        val wheels = MeshBuilder()
        var decor: MeshBuilder? = null

        when (type) {
            TrafficVehicleType.CAR -> {
                body.addBox(0f, 0.56f, 0f, 1.82f, 0.64f, 4.10f)
                body.addBox(0f, 1.12f, -0.15f, 1.56f, 0.52f, 2.05f)
                glass.addBox(0f, 1.12f, -0.15f, 1.59f, 0.38f, 1.82f)
                trim.addBox(0f, 0.36f, 2.06f, 1.76f, 0.20f, 0.08f)
                trim.addBox(0f, 0.36f, -2.06f, 1.76f, 0.20f, 0.08f)
                for (wx in listOf(-0.88f, 0.88f)) {
                    for (wz in listOf(1.26f, -1.26f)) {
                        wheels.addWheelX(wx, 0.34f, wz, 0.34f, 0.22f, 12)
                    }
                }
            }
            TrafficVehicleType.POLICE_PCR -> {
                // Indian Police Scorpio/Innova SUV + lightbar
                body.addBox(0f, 0.68f, 0f, 1.94f, 0.76f, 4.45f)
                body.addBox(0f, 1.32f, -0.20f, 1.78f, 0.60f, 2.85f)
                glass.addBox(0f, 1.32f, -0.20f, 1.81f, 0.42f, 2.55f)
                trim.addBox(0f, 0.62f, 0f, 1.97f, 0.18f, 4.48f) // Blue police stripe
                // Roof siren bar
                decor = MeshBuilder().apply {
                    addBox(-0.34f, 1.69f, 0.20f, 0.52f, 0.14f, 0.24f)
                    addBox(0.34f, 1.69f, 0.20f, 0.52f, 0.14f, 0.24f)
                }
                for (wx in listOf(-0.92f, 0.92f)) {
                    for (wz in listOf(1.40f, -1.40f)) {
                        wheels.addWheelX(wx, 0.38f, wz, 0.38f, 0.24f, 12)
                    }
                }
            }
            TrafficVehicleType.BUS -> {
                body.addBox(0f, 1.55f, 0f, 2.48f, 2.30f, 8.20f)
                glass.addBox(0f, 1.92f, 0.15f, 2.51f, 0.72f, 7.60f)
                trim.addBox(0f, 0.65f, 0f, 2.52f, 0.35f, 8.24f)
                trim.addBox(0f, 2.75f, -0.3f, 2.10f, 0.12f, 5.40f) // roof rack
                for (wx in listOf(-1.18f, 1.18f)) {
                    for (wz in listOf(2.55f, -2.55f)) {
                        wheels.addWheelX(wx, 0.50f, wz, 0.50f, 0.32f, 12)
                    }
                }
            }
            TrafficVehicleType.TRUCK -> {
                // Indian Highway Tata/Ashok Leyland Cargo Truck
                body.addBox(0f, 1.25f, 2.25f, 2.26f, 1.68f, 2.15f) // Cab
                glass.addBox(0f, 1.56f, 2.70f, 2.08f, 0.58f, 1.30f)
                trim.addBox(0f, 0.45f, 0.0f, 2.18f, 0.24f, 6.90f) // Chassis
                decor = MeshBuilder().apply {
                    addBox(0f, 1.72f, -1.25f, 2.38f, 2.15f, 4.75f) // Decorated cargo box
                }
                for (wx in listOf(-1.08f, 1.08f)) {
                    for (wz in listOf(2.25f, -0.95f, -2.55f)) {
                        wheels.addWheelX(wx, 0.50f, wz, 0.50f, 0.32f, 12)
                    }
                }
            }
            TrafficVehicleType.AUTO_RICKSHAW -> {
                // Iconic Indian 3-wheeler auto-rickshaw
                body.addBox(0f, 0.58f, -0.15f, 1.32f, 0.68f, 2.20f)
                body.addBox(0f, 0.76f, 0.88f, 0.84f, 0.76f, 0.62f) // Front cowl
                trim.addBox(0f, 1.30f, -0.15f, 1.34f, 0.72f, 2.10f) // Yellow/black canopy top
                glass.addBox(0f, 1.22f, 0.86f, 0.82f, 0.36f, 0.10f) // Windshield
                wheels.addWheelX(0f, 0.25f, 1.00f, 0.25f, 0.16f, 12) // Single front wheel
                wheels.addWheelX(-0.62f, 0.25f, -0.82f, 0.25f, 0.16f, 12)
                wheels.addWheelX(0.62f, 0.25f, -0.82f, 0.25f, 0.16f, 12)
            }
            TrafficVehicleType.TRACTOR -> {
                // Indian village tractor
                body.addBox(0f, 0.92f, 0.45f, 1.04f, 0.78f, 1.85f) // Hood & engine
                trim.addBox(0f, 1.28f, -0.85f, 1.50f, 0.24f, 0.80f) // Rear mudguards & seat
                trim.addCylinderY(0.32f, 1.28f, 0.85f, 0.06f, 0.06f, 0.72f, 8) // Exhaust stack
                wheels.addWheelX(-0.66f, 0.34f, 1.15f, 0.34f, 0.20f, 12)
                wheels.addWheelX(0.66f, 0.34f, 1.15f, 0.34f, 0.20f, 12)
                wheels.addWheelX(-0.84f, 0.68f, -0.82f, 0.68f, 0.34f, 14) // Huge rear wheels
                wheels.addWheelX(0.84f, 0.68f, -0.82f, 0.68f, 0.34f, 14)
            }
        }

        return TrafficModelMeshes(
            bodyMesh = body.build(),
            trimMesh = trim.build(),
            glassMesh = glass.build(),
            wheelMesh = wheels.build(),
            decorMesh = decor?.build()
        )
    }

    private fun buildPedestrianMeshes() {
        val body = MeshBuilder()
        body.addBox(0f, 0.98f, 0f, 0.44f, 0.68f, 0.24f) // Torso
        body.addBox(-0.12f, 0.35f, 0f, 0.15f, 0.68f, 0.16f) // Left leg
        body.addBox(0.12f, 0.35f, 0f, 0.15f, 0.68f, 0.16f) // Right leg
        pedestrianBodyMesh = body.build()

        val head = MeshBuilder()
        head.addBox(0f, 1.48f, 0f, 0.24f, 0.26f, 0.24f)
        pedestrianHeadMesh = head.build()
    }

    private fun buildTrainMeshes() {
        val loco = MeshBuilder()
        loco.addBox(0f, 2.05f, 0f, 3.1f, 3.3f, 15.0f)
        loco.addBox(0f, 0.45f, 0f, 2.7f, 0.7f, 15.4f)
        loco.addBox(0f, 3.85f, -4.0f, 1.2f, 0.3f, 2.2f)
        trainEngineMesh = loco.build()

        val coach = MeshBuilder()
        coach.addBox(0f, 2.00f, 0f, 3.0f, 3.2f, 14.5f)
        coach.addBox(0f, 0.40f, 0f, 2.6f, 0.6f, 14.6f)
        trainCoachMesh = coach.build()
    }

    private fun buildShadowAndFlame() {
        val shadow = MeshBuilder()
        shadow.addGroundQuad(0f, 0f, 0f, 1.0f, 1.0f)
        shadowQuadMesh = shadow.build()

        val flame = MeshBuilder()
        flame.addCylinderZ(0f, 0f, -0.65f, 0f, 0.08f, 8)
        nitroFlameMesh = flame.build()
    }
}
