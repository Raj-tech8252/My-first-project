package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PersistedGameState
import com.example.model.BikeId
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltBorder
import com.example.ui.theme.AsphaltDark
import com.example.ui.theme.AsphaltElevated
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.EmeraldGo
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.SaffronBright
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BikeSelectorScreen(
    gameState: PersistedGameState,
    previewBike: BikeId,
    onPreviewBike: (BikeId) -> Unit,
    onSelectOrUnlock: (BikeId) -> Unit,
    onRevSound: (BikeId) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val isUnlocked = previewBike in gameState.unlockedBikes
    val isEquipped = previewBike == gameState.selectedBike

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xBB0D1117), Color(0x180D1117), Color(0xF00D1117))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AsphaltElevated)
                        .testTag("bike_select_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("BHARAT SHOWROOM", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("Swipe 3D bike to inspect", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            Surface(
                color = AsphaltElevated,
                shape = CircleShape,
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = AmberGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${gameState.coins}", style = MaterialTheme.typography.labelLarge, color = AmberGold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Specs Card + Horizontal Bike List
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = AsphaltSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AsphaltBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(previewBike.displayName, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                            Text("${previewBike.ccLabel} • ${previewBike.subtitle}", style = MaterialTheme.typography.bodyMedium, color = SaffronBright)
                        }
                        OutlinedButton(
                            onClick = { onRevSound(previewBike) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NitroCyan),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_rev_sound")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Rev engine", tint = NitroCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("REV", style = MaterialTheme.typography.labelLarge, color = NitroCyan)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBar("TOP SPEED", "${previewBike.topSpeedKmh.toInt()} km/h", previewBike.speedRating(), SaffronPrimary, Modifier.weight(1f))
                        StatBar("ACCEL", "${previewBike.acceleration.toInt()} m/s²", previewBike.accelRating(), NitroCyan, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBar("HANDLING", "${(previewBike.handling * 100).toInt()}%", previewBike.handlingRating(), EmeraldGo, Modifier.weight(1f))
                        StatBar("BRAKING", "${previewBike.braking.toInt()}", previewBike.brakingRating(), AmberGold, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = { onSelectOrUnlock(previewBike) },
                        enabled = !isEquipped,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_equip_or_unlock"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUnlocked) EmeraldGo else SaffronPrimary,
                            disabledContainerColor = AsphaltElevated,
                            contentColor = AsphaltDark,
                            disabledContentColor = EmeraldGo
                        )
                    ) {
                        Icon(
                            imageVector = when {
                                isEquipped -> Icons.Default.CheckCircle
                                isUnlocked -> Icons.Default.CheckCircle
                                else -> Icons.Default.Lock
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                isEquipped -> "EQUIPPED"
                                isUnlocked -> "SELECT MOTORCYCLE"
                                else -> "UNLOCK FOR ${previewBike.unlockCost} COINS"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(BikeId.entries) { bike ->
                    val selected = bike == previewBike
                    val unlocked = bike in gameState.unlockedBikes
                    val equipped = bike == gameState.selectedBike
                    Surface(
                        modifier = Modifier
                            .width(146.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPreviewBike(bike) }
                            .testTag("bike_card_${bike.name}"),
                        color = if (selected) AsphaltElevated else AsphaltSurface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) SaffronPrimary else AsphaltBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(bike.ccLabel, style = MaterialTheme.typography.labelSmall, color = SaffronBright)
                                if (equipped) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Equipped", tint = EmeraldGo, modifier = Modifier.size(16.dp))
                                } else if (!unlocked) {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = AmberGold, modifier = Modifier.size(15.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = bike.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = if (unlocked) "${bike.topSpeedKmh.toInt()} km/h" else "${bike.unlockCost} Coins",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (unlocked) TextSecondary else AmberGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(
    label: String,
    valueText: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = AsphaltElevated
        )
    }
}
