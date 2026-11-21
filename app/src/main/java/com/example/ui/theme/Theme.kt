package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GameColorScheme = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = AsphaltDark,
    primaryContainer = AsphaltElevated,
    onPrimaryContainer = SaffronBright,
    secondary = NitroCyan,
    onSecondary = AsphaltDark,
    secondaryContainer = AsphaltElevated,
    onSecondaryContainer = NitroCyan,
    tertiary = EmeraldGo,
    onTertiary = AsphaltDark,
    background = AsphaltDark,
    onBackground = TextPrimary,
    surface = AsphaltSurface,
    onSurface = TextPrimary,
    surfaceVariant = AsphaltElevated,
    onSurfaceVariant = TextSecondary,
    outline = AsphaltBorder,
    error = CrimsonStop,
    onError = TextPrimary
)

val GameShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun IndianBikeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GameColorScheme,
        typography = Typography,
        shapes = GameShapes,
        content = content
    )
}
