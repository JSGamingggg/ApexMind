package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ApexDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF002026),
    primaryContainer = CyanContainer,
    onPrimaryContainer = OnCyanContainer,
    secondary = EmeraldTelemetry,
    onSecondary = Color(0xFF00210D),
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = PurpleNeural,
    onTertiary = Color(0xFF1F004A),
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = CoralMistake,
    onError = Color.White,
    outline = BorderSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ApexDarkColorScheme,
        typography = Typography,
        content = content
    )
}
