package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.BuildingFootprint
import com.example.engine3d.LandmarkKind
import com.example.engine3d.RoadSegment
import com.example.engine3d.WorldLandmark
import com.example.model.PlayerHudState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltBorder
import com.example.ui.theme.AsphaltDark
import com.example.ui.theme.AsphaltElevated
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.SaffronBright
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val ZoomRangesMeters = listOf(300f, 540f, 920f)

@Composable
fun MinimapWidget(
    hud: PlayerHudState,
    roads: List<RoadSegment>,
    landmarks: List<WorldLandmark>,
    buildings: List<BuildingFootprint>,
    railwayZ: Float,
    modifier: Modifier = Modifier
) {
    var zoomIndex by remember { mutableIntStateOf(1) } // Default 540m district view
    val rangeMeters = ZoomRangesMeters[zoomIndex]
    val textMeasurer = rememberTextMeasurer()

    val nearestLandmark = remember(hud.playerX, hud.playerZ, landmarks) {
        landmarks.minByOrNull { lm ->
            hypot(lm.x - hud.playerX, lm.z - hud.playerZ)
        }
    }
    val nearestDistMeters = nearestLandmark?.let {
        hypot(it.x - hud.playerX, it.z - hud.playerZ).toInt()
    } ?: 0

    Column(
        modifier = modifier.width(128.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = AsphaltDark.copy(alpha = 0.88f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.2.dp, AsphaltBorder),
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { zoomIndex = (zoomIndex + 1) % ZoomRangesMeters.size }
                .testTag("minimap_radar")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w * 0.5f
                    val cy = h * 0.5f
                    val scale = w / rangeMeters

                    fun mapX(wx: Float) = cx + (wx - hud.playerX) * scale
                    fun mapY(wz: Float) = cy + (wz - hud.playerZ) * scale

                    clipRect {
                        // Subtle range rings & crosshair grid
                        drawCircle(
                            color = Color(0x22FFFFFF),
                            radius = w * 0.25f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1f)
                        )
                        drawCircle(
                            color = Color(0x33FFFFFF),
                            radius = w * 0.46f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1f)
                        )
                        drawLine(
                            color = Color(0x16FFFFFF),
                            start = Offset(cx, 0f),
                            end = Offset(cx, h),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0x16FFFFFF),
                            start = Offset(0f, cy),
                            end = Offset(w, cy),
                            strokeWidth = 1f
                        )

                        // Building & Hut Footprints
                        for (b in buildings) {
                            val bx = mapX(b.cx)
                            val by = mapY(b.cz)
                            val bw = (b.width * scale).coerceAtLeast(2.5f)
                            val bh = (b.depth * scale).coerceAtLeast(2.5f)
                            if (bx + bw * 0.5f >= 0f && bx - bw * 0.5f <= w &&
                                by + bh * 0.5f >= 0f && by - bh * 0.5f <= h
                            ) {
                                drawRect(
                                    color = if (b.isVillageHut) Color(0x66B07B4A) else Color(0x6649566B),
                                    topLeft = Offset(bx - bw * 0.5f, by - bh * 0.5f),
                                    size = Size(bw, bh)
                                )
                            }
                        }

                        // Roads
                        for (r in roads) {
                            val x0 = mapX(r.x0)
                            val y0 = mapY(r.z0)
                            val x1 = mapX(r.x1)
                            val y1 = mapY(r.z1)
                            val roadColor = when {
                                r.isHighway -> Color(0xFF9AA7B8)
                                r.isDirt -> Color(0xFFA6784B)
                                else -> Color(0xFF5D6A7E)
                            }
                            drawLine(
                                color = roadColor,
                                start = Offset(x0, y0),
                                end = Offset(x1, y1),
                                strokeWidth = if (r.isHighway) 5f else if (r.isDirt) 2.5f else 3.2f,
                                cap = StrokeCap.Round
                            )
                            if (r.isHighway) {
                                drawLine(
                                    color = Color(0x88FFC400),
                                    start = Offset(x0, y0),
                                    end = Offset(x1, y1),
                                    strokeWidth = 1.1f
                                )
                            }
                        }

                        // Railway Track Line (East-West at railwayZ)
                        val railY = mapY(railwayZ)
                        if (railY in -6f..(h + 6f)) {
                            val railX0 = mapX(-540f)
                            val railX1 = mapX(540f)
                            drawLine(
                                color = Color(0xFFCFD8DC),
                                start = Offset(railX0, railY),
                                end = Offset(railX1, railY),
                                strokeWidth = 2.4f
                            )
                            var tx = railX0.coerceAtLeast(0f)
                            val maxTx = railX1.coerceAtMost(w)
                            while (tx <= maxTx) {
                                drawLine(
                                    color = Color(0xFF795548),
                                    start = Offset(tx, railY - 3f),
                                    end = Offset(tx, railY + 3f),
                                    strokeWidth = 1.6f
                                )
                                tx += 8f
                            }
                        }

                        // Traffic, Police & Rival Dots
                        for (d in hud.minimapDots) {
                            val dx = mapX(d.x)
                            val dy = mapY(d.z)
                            if (dx in 2f..(w - 2f) && dy in 2f..(h - 2f)) {
                                val col = when (d.type) {
                                    1 -> CrimsonStop
                                    2 -> NitroCyan
                                    else -> Color(0xFFB0BEC5)
                                }
                                drawCircle(
                                    color = col,
                                    radius = if (d.type == 1) 3.8f else 2.3f,
                                    center = Offset(dx, dy)
                                )
                            }
                        }

                        // Landmarks inside or on edge of radar
                        for (lm in landmarks) {
                            val lx = mapX(lm.x)
                            val ly = mapY(lm.z)
                            val lmColor = landmarkColor(lm.kind)
                            val inside = lx in 8f..(w - 8f) && ly in 10f..(h - 8f)
                            if (inside) {
                                drawLandmarkBadge(
                                    cx = lx,
                                    cy = ly,
                                    color = lmColor,
                                    kind = lm.kind,
                                    label = lm.shortLabel,
                                    textMeasurer = textMeasurer,
                                    canvasWidth = w
                                )
                            } else if (lm.kind != LandmarkKind.HIGHWAY) {
                                // Subtle edge pip pointing toward off-radar major landmarks
                                val clampedX = lx.coerceIn(5f, w - 5f)
                                val clampedY = ly.coerceIn(5f, h - 5f)
                                drawCircle(
                                    color = lmColor.copy(alpha = 0.72f),
                                    radius = 2.5f,
                                    center = Offset(clampedX, clampedY)
                                )
                            }
                        }

                        // Active Mission / Race Target Beacon
                        val tx = hud.targetX
                        val tz = hud.targetZ
                        if (tx != null && tz != null) {
                            val mx = mapX(tx).coerceIn(7f, w - 7f)
                            val my = mapY(tz).coerceIn(7f, h - 7f)
                            drawCircle(
                                color = AmberGold.copy(alpha = 0.35f),
                                radius = 9f,
                                center = Offset(mx, my)
                            )
                            drawCircle(
                                color = AmberGold,
                                radius = 4.6f,
                                center = Offset(mx, my)
                            )
                        }

                        // Player Forward View Cone + Position Arrow
                        rotate(degrees = -hud.playerHeadingDeg + 180f, pivot = Offset(cx, cy)) {
                            val coneAngle = 32f * PI.toFloat() / 180f
                            val coneLen = 28f
                            val fovPath = Path().apply {
                                moveTo(cx, cy)
                                lineTo(cx - sin(coneAngle) * coneLen, cy - cos(coneAngle) * coneLen)
                                lineTo(cx + sin(coneAngle) * coneLen, cy - cos(coneAngle) * coneLen)
                                close()
                            }
                            drawPath(fovPath, color = SaffronPrimary.copy(alpha = 0.22f))

                            val arrow = Path().apply {
                                moveTo(cx, cy - 8f)
                                lineTo(cx - 5.5f, cy + 6f)
                                lineTo(cx, cy + 2.5f)
                                lineTo(cx + 5.5f, cy + 6f)
                                close()
                            }
                            drawPath(arrow, color = SaffronPrimary)
                            drawPath(arrow, color = TextPrimary, style = Stroke(width = 1.2f))
                        }
                    }
                }

                // North Compass Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 3.dp)
                        .background(AsphaltElevated.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = "N",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SaffronBright
                    )
                }

                // Zoom Scale Badge (tap minimap to toggle)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(AsphaltElevated.copy(alpha = 0.88f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${rangeMeters.toInt()}m",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = TextSecondary
                    )
                }
            }
        }

        // Nearest Landmark & Player Coordinate Pill below mini-map
        Surface(
            color = AsphaltDark.copy(alpha = 0.84f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, AsphaltBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("minimap_landmark_readout")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nearestLandmark?.shortLabel ?: "OPEN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = nearestLandmark?.let { landmarkColor(it.kind) } ?: SaffronBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${nearestDistMeters}m • ${hud.playerX.toInt()},${(-hud.playerZ).toInt()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

private fun landmarkColor(kind: LandmarkKind): Color = when (kind) {
    LandmarkKind.PETROL_PUMP -> EmeraldGo
    LandmarkKind.TEMPLE -> SaffronPrimary
    LandmarkKind.SCHOOL -> AmberGold
    LandmarkKind.VILLAGE -> Color(0xFFD99B66)
    LandmarkKind.BAZAAR -> NitroCyan
    LandmarkKind.RAILWAY -> CrimsonStop
    LandmarkKind.STUNT_ARENA -> Color(0xFF76FF03)
    LandmarkKind.HIGHWAY -> Color(0xFF4CAF50)
}

private fun DrawScope.drawLandmarkBadge(
    cx: Float,
    cy: Float,
    color: Color,
    kind: LandmarkKind,
    label: String,
    textMeasurer: TextMeasurer,
    canvasWidth: Float
) {
    // Halo
    drawCircle(color = color.copy(alpha = 0.28f), radius = 6.5f, center = Offset(cx, cy))

    // Distinct shape per landmark kind
    when (kind) {
        LandmarkKind.TEMPLE -> {
            val tri = Path().apply {
                moveTo(cx, cy - 4.5f)
                lineTo(cx - 4.2f, cy + 3.5f)
                lineTo(cx + 4.2f, cy + 3.5f)
                close()
            }
            drawPath(tri, color = color)
        }
        LandmarkKind.PETROL_PUMP, LandmarkKind.SCHOOL -> {
            drawRect(
                color = color,
                topLeft = Offset(cx - 3.6f, cy - 3.6f),
                size = Size(7.2f, 7.2f)
            )
        }
        LandmarkKind.RAILWAY -> {
            drawLine(color, Offset(cx - 3.8f, cy - 3.8f), Offset(cx + 3.8f, cy + 3.8f), strokeWidth = 2.2f)
            drawLine(color, Offset(cx + 3.8f, cy - 3.8f), Offset(cx - 3.8f, cy + 3.8f), strokeWidth = 2.2f)
        }
        else -> {
            drawCircle(color = color, radius = 3.6f, center = Offset(cx, cy))
        }
    }

    // Crisp compact label beside marker (avoid drawing right at center over player arrow)
    if (kind != LandmarkKind.HIGHWAY) {
        val style = TextStyle(
            fontFamily = PlusJakartaSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 7.sp,
            color = TextPrimary
        )
        val measured = textMeasurer.measure(label, style)
        val labelX = (cx - measured.size.width * 0.5f).coerceIn(1f, (canvasWidth - measured.size.width - 1f).coerceAtLeast(1f))
        val labelY = (cy + 5f).coerceAtMost(size.height - measured.size.height - 1f)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(labelX, labelY)
        )
    }
}
