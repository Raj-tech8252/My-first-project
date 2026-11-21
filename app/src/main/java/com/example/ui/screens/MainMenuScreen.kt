package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PersistedGameState
import com.example.model.AppScreen
import com.example.model.GameMode
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    gameState: PersistedGameState,
    onNavigate: (AppScreen) -> Unit,
    onPlayMode: (GameMode) -> Unit,
    onExitApp: () -> Unit
) {
    var showModeSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xB80D1117),
                        Color(0x440D1117),
                        Color(0xE60D1117)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val isWide = maxWidth > 640.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    MenuHeaderBlock(gameState)
                    ActiveBikeBadge(gameState)
                }
                Spacer(Modifier.width(24.dp))
                MenuActionPanel(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    onPlayClick = { showModeSheet = true },
                    onNavigate = onNavigate,
                    onExitClick = { showExitDialog = true }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                MenuHeaderBlock(gameState)
                Spacer(Modifier.weight(1f))
                ActiveBikeBadge(gameState)
                Spacer(Modifier.height(12.dp))
                MenuActionPanel(
                    modifier = Modifier.fillMaxWidth(),
                    onPlayClick = { showModeSheet = true },
                    onNavigate = onNavigate,
                    onExitClick = { showExitDialog = true }
                )
            }
        }
    }

    if (showModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModeSheet = false },
            containerColor = AsphaltSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CHOOSE DRIVING MODE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SaffronBright
                )
                ModeOptionCard(
                    title = stringResource(R.string.mode_free_ride),
                    subtitle = GameMode.FREE_RIDE.description,
                    icon = Icons.Default.TwoWheeler,
                    accent = EmeraldGo,
                    tag = "mode_free_ride",
                    onClick = {
                        showModeSheet = false
                        onPlayMode(GameMode.FREE_RIDE)
                    }
                )
                ModeOptionCard(
                    title = stringResource(R.string.mode_racing),
                    subtitle = GameMode.RACING.description,
                    icon = Icons.Default.Flag,
                    accent = NitroCyan,
                    tag = "mode_racing",
                    onClick = {
                        showModeSheet = false
                        onPlayMode(GameMode.RACING)
                    }
                )
                ModeOptionCard(
                    title = stringResource(R.string.mode_stunt),
                    subtitle = GameMode.STUNT.description,
                    icon = Icons.Default.Speed,
                    accent = SaffronPrimary,
                    tag = "mode_stunt",
                    onClick = {
                        showModeSheet = false
                        onPlayMode(GameMode.STUNT)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = AsphaltSurface,
            title = {
                Text(
                    stringResource(R.string.exit_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    stringResource(R.string.exit_confirm_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = onExitApp,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonStop)
                ) {
                    Text(stringResource(R.string.action_exit), color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MenuHeaderBlock(gameState: PersistedGameState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.img_menu_hero),
                contentDescription = "Indian Bike 3D highway artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, SaffronPrimary, RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "INDIAN BIKE 3D",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                    color = TextPrimary
                )
                Text(
                    text = "BHARAT OPEN-WORLD SIMULATOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = SaffronBright
                )
            }
        }

        Surface(
            color = AsphaltElevated.copy(alpha = 0.92f),
            shape = CircleShape,
            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Coins",
                    tint = AmberGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "${gameState.coins}",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberGold
                )
            }
        }
    }
}

@Composable
private fun ActiveBikeBadge(gameState: PersistedGameState) {
    Surface(
        color = AsphaltSurface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AsphaltBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsBike,
                contentDescription = null,
                tint = SaffronBright,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = gameState.selectedBike.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                    color = TextPrimary
                )
                Text(
                    text = "${gameState.selectedBike.ccLabel} • Top ${gameState.selectedBike.topSpeedKmh.toInt()} km/h • ${gameState.settings.graphicsQuality.label} GFX",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun MenuActionPanel(
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onExitClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onPlayClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("menu_play_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SaffronPrimary,
                contentColor = AsphaltDark
            ),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.menu_play).uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MenuTileButton(
                label = stringResource(R.string.menu_select_bike),
                icon = Icons.Default.TwoWheeler,
                accent = NitroCyan,
                modifier = Modifier
                    .weight(1f)
                    .testTag("menu_select_bike_button"),
                onClick = { onNavigate(AppScreen.SELECT_BIKE) }
            )
            MenuTileButton(
                label = stringResource(R.string.menu_garage),
                icon = Icons.Default.Build,
                accent = SaffronBright,
                modifier = Modifier
                    .weight(1f)
                    .testTag("menu_garage_button"),
                onClick = { onNavigate(AppScreen.GARAGE) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MenuTileButton(
                label = stringResource(R.string.menu_missions),
                icon = Icons.Default.EmojiEvents,
                accent = AmberGold,
                modifier = Modifier
                    .weight(1f)
                    .testTag("menu_missions_button"),
                onClick = { onNavigate(AppScreen.MISSIONS) }
            )
            MenuTileButton(
                label = stringResource(R.string.menu_settings),
                icon = Icons.Default.Settings,
                accent = TextSecondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("menu_settings_button"),
                onClick = { onNavigate(AppScreen.SETTINGS) }
            )
        }

        OutlinedButton(
            onClick = onExitClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("menu_exit_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AsphaltBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AsphaltSurface.copy(alpha = 0.85f),
                contentColor = TextSecondary
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.menu_exit).uppercase(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MenuTileButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = AsphaltSurface.copy(alpha = 0.90f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AsphaltBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        color = AsphaltElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accent)
        }
    }
}
