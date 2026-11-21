package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.BuildingFootprint
import com.example.engine3d.RoadSegment
import com.example.engine3d.WorldLandmark
import com.example.model.ControlInputState
import com.example.model.GameMode
import com.example.model.PlayerHudState
import com.example.ui.components.MinimapWidget
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltBorder
import com.example.ui.theme.AsphaltDark
import com.example.ui.theme.AsphaltElevated
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.SaffronBright
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameplayHudScreen(
    hud: PlayerHudState,
    gameMode: GameMode,
    minimapRoads: List<RoadSegment>,
    minimapLandmarks: List<WorldLandmark>,
    minimapBuildings: List<BuildingFootprint>,
    railwayZ: Float,
    isPaused: Boolean,
    onUpdateControls: ((ControlInputState) -> ControlInputState) -> Unit,
    onToggleEngine: () -> Unit,
    onCycleHeadlight: () -> Unit,
    onCycleCamera: () -> Unit,
    onPauseToggle: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    BackHandler {
        onPauseToggle(!isPaused)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(10.dp)
    ) {
        // 1. TOP HUD BAR: Zone/Objective + Minimap + Pause
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Surface(
                    color = AsphaltDark.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AsphaltBorder)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(hud.zoneName.uppercase(), style = MaterialTheme.typography.labelLarge, color = SaffronBright)
                            Text("• ${hud.timeLabel} • ${hud.weatherLabel}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text(hud.missionObjective, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (gameMode == GameMode.MISSION && hud.missionTimeLeftSec > 0f) {
                                Text(
                                    text = "TIME: ${hud.missionTimeLeftSec.toInt()}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (hud.missionTimeLeftSec < 12f) CrimsonStop else AmberGold
                                )
                            }
                            if (gameMode == GameMode.RACING) {
                                Text(
                                    text = "POS P${hud.racePosition}/${hud.raceTotalRacers}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NitroCyan
                                )
                            }
                            if (hud.stuntScore > 0 || gameMode == GameMode.STUNT) {
                                Text(
                                    text = "STUNT: ${hud.stuntScore} (x${hud.comboMultiplier})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldGo
                                )
                            }
                            if (hud.policeWantedLevel > 0) {
                                Text(
                                    text = "PCR ALERT ★${hud.policeWantedLevel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CrimsonStop
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                IconButton(
                    onClick = { onPauseToggle(true) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AsphaltDark.copy(alpha = 0.82f))
                        .border(1.dp, AsphaltBorder, CircleShape)
                        .testTag("btn_pause")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextPrimary)
                }

                // Corner Mini-Map Radar showing player position & nearby landmarks
                MinimapWidget(
                    hud = hud,
                    roads = minimapRoads,
                    landmarks = minimapLandmarks,
                    buildings = minimapBuildings,
                    railwayZ = railwayZ
                )
            }
        }

        // 2. CENTER BANNERS (Stunts, Engine off hint, Train warning, Petrol Refuel, Crash)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 86.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!hud.engineRunning) {
                StatusPill("ENGINE OFF — TAP START TO IGNITE", SaffronPrimary)
            }
            if (hud.nearTrainWarning) {
                StatusPill("RAILWAY PHATAK — EXPRESS TRAIN!", CrimsonStop, Icons.Default.Warning)
            }
            if (hud.refuelBanner) {
                StatusPill("REFUELING AT BHARAT PETROL PUMP", EmeraldGo, Icons.Default.LocalGasStation)
            }
            if (hud.lastStuntLabel.isNotEmpty()) {
                StatusPill(hud.lastStuntLabel, NitroCyan)
            }
            if (hud.crashWarning.isNotEmpty()) {
                StatusPill(hud.crashWarning, CrimsonStop)
            }
        }

        // 3. RIGHT-SIDE UTILITY BUTTONS (Start/Stop, Headlight, Camera, Horn)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UtilityToggleButton(
                    label = if (hud.engineRunning) "STOP" else "START",
                    icon = Icons.Default.PowerSettingsNew,
                    active = hud.engineRunning,
                    activeColor = EmeraldGo,
                    tag = "btn_engine_toggle",
                    onClick = onToggleEngine
                )
                UtilityToggleButton(
                    label = when {
                        hud.highBeam -> "HIGH"
                        hud.headlightOn -> "LOW"
                        else -> "LIGHT"
                    },
                    icon = Icons.Default.Highlight,
                    active = hud.headlightOn,
                    activeColor = AmberGold,
                    tag = "btn_headlight",
                    onClick = onCycleHeadlight
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UtilityToggleButton(
                    label = hud.cameraMode.label.uppercase(),
                    icon = Icons.Default.CameraAlt,
                    active = true,
                    activeColor = NitroCyan,
                    tag = "btn_camera",
                    onClick = onCycleCamera
                )
                HoldControlButton(
                    label = "HORN",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    accent = SaffronBright,
                    size = 56.dp,
                    tag = "btn_horn",
                    onHoldChanged = { held -> onUpdateControls { it.copy(horn = held) } }
                )
            }
        }

        // 4. BOTTOM CONTROLS & DIGITAL INSTRUMENT CLUSTER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left & Right Steering
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                HoldControlButton(
                    label = "LEFT",
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    accent = TextPrimary,
                    size = 70.dp,
                    tag = "btn_steer_left",
                    onHoldChanged = { held -> onUpdateControls { it.copy(steerLeft = held) } }
                )
                HoldControlButton(
                    label = "RIGHT",
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    accent = TextPrimary,
                    size = 70.dp,
                    tag = "btn_steer_right",
                    onHoldChanged = { held -> onUpdateControls { it.copy(steerRight = held) } }
                )
            }

            // Center Speedometer, Gear, Fuel & Nitro
            Surface(
                color = AsphaltDark.copy(alpha = 0.84f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AsphaltBorder),
                modifier = Modifier.width(136.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${hud.speedKmh}",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 30.sp, lineHeight = 32.sp),
                            color = TextPrimary
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("KM/H", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("G${hud.gear}", style = MaterialTheme.typography.labelLarge, color = SaffronBright)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { hud.rpmNormalized },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = if (hud.rpmNormalized > 0.82f) CrimsonStop else SaffronPrimary,
                        trackColor = AsphaltElevated
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { hud.nitroPercent },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = NitroCyan,
                            trackColor = AsphaltElevated
                        )
                        LinearProgressIndicator(
                            progress = { hud.fuelPercent },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = if (hud.fuelPercent < 0.2f) CrimsonStop else EmeraldGo,
                            trackColor = AsphaltElevated
                        )
                    }
                }
            }

            // Right Pedals: Wheelie/Nitro + Brake + Accelerate
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HoldControlButton(
                        label = "STUNT",
                        icon = Icons.Default.Speed,
                        accent = EmeraldGo,
                        size = 54.dp,
                        tag = "btn_stunt",
                        onHoldChanged = { held -> onUpdateControls { it.copy(wheelie = held) } }
                    )
                    HoldControlButton(
                        label = "NOS",
                        icon = Icons.Default.Bolt,
                        accent = NitroCyan,
                        size = 54.dp,
                        tag = "btn_nitro",
                        onHoldChanged = { held -> onUpdateControls { it.copy(nitro = held) } }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HoldControlButton(
                        label = "BRAKE",
                        icon = null,
                        accent = CrimsonStop,
                        size = 68.dp,
                        tag = "btn_brake",
                        onHoldChanged = { held -> onUpdateControls { it.copy(brake = held) } }
                    )
                    HoldControlButton(
                        label = "GAS",
                        icon = null,
                        accent = SaffronPrimary,
                        size = 76.dp,
                        tag = "btn_accelerate",
                        onHoldChanged = { held -> onUpdateControls { it.copy(throttle = held) } }
                    )
                }
            }
        }

        // 5. PAUSE MODAL
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = AsphaltSurface,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, AsphaltBorder),
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("GAME PAUSED", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text(
                            text = "${hud.zoneName} • Stunt Score: ${hud.stuntScore}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Button(
                            onClick = { onPauseToggle(false) },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_resume"),
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = AsphaltDark)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("RESUME RIDE", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_restart")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("RESTART", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = onExitToMenu,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_quit_menu")
                        ) {
                            Text("MAIN MENU", style = MaterialTheme.typography.labelLarge, color = CrimsonStop)
                        }
                    }
                }
            }
        }

        // 6. SESSION RESULT MODAL (Mission Complete / Race Finish / Failed)
        hud.resultState?.let { res ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB000000)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = AsphaltSurface,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, if (res.success) EmeraldGo else CrimsonStop),
                    modifier = Modifier.fillMaxWidth(0.86f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = res.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (res.success) EmeraldGo else CrimsonStop
                        )
                        Text(res.subtitle, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        if (res.coinsEarned > 0) {
                            Text(
                                text = "+${res.coinsEarned} COINS EARNED",
                                style = MaterialTheme.typography.titleLarge,
                                color = AmberGold
                            )
                        }
                        Button(
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary, contentColor = AsphaltDark)
                        ) {
                            Text("PLAY AGAIN", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onExitToMenu,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("MAIN MENU", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color, icon: ImageVector? = null) {
    Surface(
        color = AsphaltDark.copy(alpha = 0.85f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
private fun UtilityToggleButton(
    label: String,
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (active) activeColor.copy(alpha = 0.22f) else AsphaltDark.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, if (active) activeColor else AsphaltBorder),
        modifier = Modifier
            .size(width = 62.dp, height = 52.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (active) activeColor else TextSecondary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (active) activeColor else TextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun HoldControlButton(
    label: String,
    icon: ImageVector?,
    accent: Color,
    size: androidx.compose.ui.unit.Dp,
    tag: String,
    onHoldChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Surface(
        color = if (pressed) accent.copy(alpha = 0.42f) else AsphaltDark.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (pressed) 2.dp else 1.2.dp, if (pressed) accent else accent.copy(alpha = 0.6f)),
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(tag) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onHoldChanged(true)
                    waitForUpOrCancellation()
                    pressed = false
                    onHoldChanged(false)
                }
            }
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(28.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (pressed) TextPrimary else accent
            )
        }
    }
}

