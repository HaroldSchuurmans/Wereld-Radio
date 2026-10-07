package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpotifyDarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E3A27),
    onPrimaryContainer = SpotifyGreenLight,
    secondary = SpotifyGreenLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF242424),
    onSecondaryContainer = SpotifyTextPrimary,
    background = SpotifyDarkBackground,
    onBackground = SpotifyTextPrimary,
    surface = SpotifyDarkSurface,
    onSurface = SpotifyTextPrimary,
    surfaceVariant = SpotifyCardElevated,
    onSurfaceVariant = SpotifyTextSecondary,
    outline = SpotifyDivider,
    error = RdsBadgeLive,
    onError = Color.White
)

@Composable
fun AutoRadioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpotifyDarkColorScheme,
        typography = Typography,
        content = content
    )
}
