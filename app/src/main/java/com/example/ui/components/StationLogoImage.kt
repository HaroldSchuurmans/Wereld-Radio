package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.SpotifyCardHighlight

@Composable
fun StationLogoImage(
    imageUrl: String?,
    stationName: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 10.dp
) {
    // Sanitize image URL if present: convert HTTP images to secure HTTPS or fix common radio-browser issues
    val sanitizedUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() }?.let { url ->
        when {
            // Wikipedia thumbnail URLs frequently return HTTP 400 when missing correct parameters
            url.contains("upload.wikimedia.org") && url.contains("/thumb/") -> null
            url.startsWith("http://") -> url.replaceFirst("http://", "https://")
            else -> url
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(SpotifyCardHighlight),
        contentAlignment = Alignment.Center
    ) {
        if (!sanitizedUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(sanitizedUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$stationName logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    StationLogoFallback(stationName = stationName, size = size)
                },
                error = {
                    StationLogoFallback(stationName = stationName, size = size)
                }
            )
        } else {
            StationLogoFallback(stationName = stationName, size = size)
        }
    }
}

@Composable
fun StationLogoFallback(
    stationName: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val cleanName = stationName.trim()
    val hash = cleanName.hashCode()
    val gradients = listOf(
        listOf(Color(0xFF1DB954), Color(0xFF0D5325)), // Spotify Green
        listOf(Color(0xFF2E77D0), Color(0xFF103366)), // Electric Blue
        listOf(Color(0xFFE91429), Color(0xFF6B0913)), // Vibrant Red
        listOf(Color(0xFFE8630A), Color(0xFF752F02)), // Bright Amber Orange
        listOf(Color(0xFF7B2CBF), Color(0xFF3C096C)), // Purple
        listOf(Color(0xFF009688), Color(0xFF004D40)), // Deep Teal
        listOf(Color(0xFFFF007F), Color(0xFF800040))  // Hot Pink
    )
    val chosenGradient = gradients[(kotlin.math.abs(hash) % gradients.size)]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(chosenGradient)),
        contentAlignment = Alignment.Center
    ) {
        // Show 2-letter abbreviation for 3FM, 538, Qmusic, etc. or first initial
        val shortLabel = when {
            cleanName.startsWith("NPO", ignoreCase = true) -> cleanName.substringAfter("NPO").trim().take(3).ifBlank { "NPO" }
            cleanName.contains("538") -> "538"
            cleanName.contains("3FM", ignoreCase = true) -> "3FM"
            cleanName.contains("10", ignoreCase = true) -> "R10"
            cleanName.contains("JOE", ignoreCase = true) -> "JOE"
            cleanName.contains("SLAM", ignoreCase = true) -> "SLAM"
            else -> cleanName.take(2).uppercase()
        }

        if (size >= 36.dp && shortLabel.isNotBlank()) {
            Text(
                text = shortLabel,
                color = Color.White,
                fontSize = if (shortLabel.length > 2) (size.value * 0.28f).sp else (size.value * 0.38f).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Radio,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
