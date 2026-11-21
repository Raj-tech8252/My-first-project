package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PersistedGameState
import com.example.model.BikeCustomization
import com.example.model.BikeId
import com.example.model.BikePaintPalette
import com.example.model.RimAccentPalette
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
fun GarageScreen(
    gameState: PersistedGameState,
    onUpdateColors: (BikeId, Long, Long) -> Unit,
    onUpgradePart: (BikeId, String, Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val bike = gameState.selectedBike
    val custom = gameState.customizations[bike] ?: BikeCustomization(bike.defaultTankColorHex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xBA0D1117), Color(0x160D1117), Color(0xF20D1117))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
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
                        .testTag("garage_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("CUSTOM GARAGE", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("${bike.displayName} • Drag to rotate", style = MaterialTheme.typography.bodyMedium, color = SaffronBright)
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

        // Customization & Upgrade Controls
        Surface(
            color = AsphaltSurface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, AsphaltBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("BODY PAINT COLOR", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(BikePaintPalette) { opt ->
                        val isSelected = custom.tankColorHex == opt.colorHex
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(opt.composeColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) SaffronBright else AsphaltBorder,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateColors(bike, opt.colorHex, custom.rimColorHex) }
                                .testTag("paint_${opt.name}")
                        )
                    }
                }

                Text("ALLOY RIM ACCENT", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(RimAccentPalette) { opt ->
                        val isSelected = custom.rimColorHex == opt.colorHex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(opt.composeColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) NitroCyan else AsphaltBorder,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateColors(bike, custom.tankColorHex, opt.colorHex) }
                                .testTag("rim_${opt.name}")
                        )
                    }
                }

                Text("PERFORMANCE UPGRADES", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UpgradeCard(
                        title = "ENGINE",
                        level = custom.engineLevel,
                        cost = custom.engineLevel * 450,
                        icon = Icons.Default.Speed,
                        accent = SaffronPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onUpgradePart(bike, "ENGINE", custom.engineLevel * 450) }
                    )
                    UpgradeCard(
                        title = "NITRO",
                        level = custom.nitroLevel,
                        cost = custom.nitroLevel * 400,
                        icon = Icons.Default.Bolt,
                        accent = NitroCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onUpgradePart(bike, "NITRO", custom.nitroLevel * 400) }
                    )
                    UpgradeCard(
                        title = "SUSPENSION",
                        level = custom.suspensionLevel,
                        cost = custom.suspensionLevel * 350,
                        icon = Icons.Default.Build,
                        accent = EmeraldGo,
                        modifier = Modifier.weight(1f),
                        onClick = { onUpgradePart(bike, "SUSPENSION", custom.suspensionLevel * 350) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    title: String,
    level: Int,
    cost: Int,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val maxed = level >= 3
    Surface(
        modifier = modifier,
        color = AsphaltElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
            Text("LVL $level/3", style = MaterialTheme.typography.labelLarge, color = accent)
            Button(
                onClick = onClick,
                enabled = !maxed,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = AsphaltDark,
                    disabledContainerColor = AsphaltSurface,
                    disabledContentColor = TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("upgrade_$title")
            ) {
                Text(
                    text = if (maxed) "MAX" else "$cost",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
