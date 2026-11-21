package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.GameSettingsState
import com.example.model.GraphicsQuality
import com.example.model.TimeOfDayMode
import com.example.model.WeatherMode
import com.example.ui.theme.AsphaltBorder
import com.example.ui.theme.AsphaltDark
import com.example.ui.theme.AsphaltElevated
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.SaffronBright
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: GameSettingsState,
    onUpdateSettings: (GameSettingsState) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEB0D1117))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AsphaltElevated)
                    .testTag("settings_back")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Graphics optimization, environment & audio", style = MaterialTheme.typography.bodyMedium, color = SaffronBright)
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Graphics Quality
            SettingsCard(title = "GRAPHICS QUALITY", subtitle = "Tune draw distance, reflections & traffic density for smooth FPS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GraphicsQuality.entries.forEach { quality ->
                        val selected = settings.graphicsQuality == quality
                        OptionChip(
                            label = quality.label,
                            selected = selected,
                            accent = SaffronPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gfx_${quality.name}"),
                            onClick = { onUpdateSettings(settings.copy(graphicsQuality = quality)) }
                        )
                    }
                }
                val desc = when (settings.graphicsQuality) {
                    GraphicsQuality.LOW -> "Best for low-end phones: reduced draw distance & simplified lighting."
                    GraphicsQuality.MEDIUM -> "Balanced performance: dynamic headlights, wet reflections & standard traffic."
                    GraphicsQuality.HIGH -> "Maximum detail: long draw distance, full rain particles & dense Indian traffic."
                }
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            // Time of Day
            SettingsCard(title = "DAY & NIGHT SYSTEM", subtitle = "Choose dynamic cycle or lock time of day") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeOfDayMode.entries.forEach { mode ->
                        OptionChip(
                            label = mode.label.split(" ").first(),
                            selected = settings.timeOfDayMode == mode,
                            accent = NitroCyan,
                            modifier = Modifier.weight(1f),
                            onClick = { onUpdateSettings(settings.copy(timeOfDayMode = mode)) }
                        )
                    }
                }
            }

            // Weather
            SettingsCard(title = "WEATHER CONDITIONS", subtitle = "Clear sunshine, Monsoon rain, or Morning mist") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherMode.entries.forEach { mode ->
                        OptionChip(
                            label = mode.label.split(" ").last(),
                            selected = settings.weatherMode == mode,
                            accent = SaffronBright,
                            modifier = Modifier.weight(1f),
                            onClick = { onUpdateSettings(settings.copy(weatherMode = mode)) }
                        )
                    }
                }
            }

            // Audio
            SettingsCard(title = "SOUND & ENGINE AUDIO", subtitle = "Synthesized bike exhaust, horn, rain & railway chimes") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Game Audio Enabled", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(soundEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AsphaltDark,
                            checkedTrackColor = SaffronPrimary
                        ),
                        modifier = Modifier.testTag("switch_sound")
                    )
                }
                Text("Master Volume: ${(settings.soundVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Slider(
                    value = settings.soundVolume,
                    onValueChange = { onUpdateSettings(settings.copy(soundVolume = it)) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = SaffronPrimary, activeTrackColor = SaffronPrimary)
                )
            }

            // Controls Sensitivity
            SettingsCard(title = "STEERING SENSITIVITY", subtitle = "Adjust turn responsiveness") {
                Text("Sensitivity: ${String.format("%.1f", settings.steeringSensitivity)}x", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Slider(
                    value = settings.steeringSensitivity,
                    onValueChange = { onUpdateSettings(settings.copy(steeringSensitivity = it)) },
                    valueRange = 0.6f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = NitroCyan, activeTrackColor = NitroCyan)
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = AsphaltSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AsphaltBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            content()
        }
    }
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) accent else AsphaltElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) accent else AsphaltBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) AsphaltDark else TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
