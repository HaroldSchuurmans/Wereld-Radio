package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WeatherCondition
import com.example.model.WeatherInfo
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun WeatherWidget(
    weatherInfo: WeatherInfo?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp,
    showLabel: Boolean = true
) {
    if (weatherInfo == null) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SpotifyCardElevated)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeatherCanvasIcon(
            condition = weatherInfo.condition,
            isDay = weatherInfo.isDay,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${weatherInfo.temperatureCelsius}°C",
            color = SpotifyTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "• ${weatherInfo.conditionDescription}",
                color = SpotifyTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun WeatherCanvasIcon(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        when (condition) {
            WeatherCondition.SUNNY -> {
                drawSun(center = Offset(width / 2f, height / 2f), radius = width * 0.32f, color = Color(0xFFFFB300))
            }
            WeatherCondition.PARTLY_CLOUDY -> {
                // Sun slightly behind in top-right
                drawSun(center = Offset(width * 0.68f, height * 0.35f), radius = width * 0.22f, color = Color(0xFFFFB300))
                // Soft cloud in foreground
                drawCloud(center = Offset(width * 0.42f, height * 0.62f), cloudWidth = width * 0.65f, cloudColor = Color(0xFFE0E0E0))
            }
            WeatherCondition.CLOUDY -> {
                drawCloud(center = Offset(width * 0.5f, height * 0.52f), cloudWidth = width * 0.78f, cloudColor = Color(0xFFB0BEC5))
            }
            WeatherCondition.RAINY -> {
                // Cloud
                drawCloud(center = Offset(width * 0.5f, height * 0.42f), cloudWidth = width * 0.72f, cloudColor = Color(0xFF90A4AE))
                // Rain drops
                val dropColor = Color(0xFF4FC3F7)
                drawRainDrop(start = Offset(width * 0.35f, height * 0.68f), length = height * 0.22f, color = dropColor)
                drawRainDrop(start = Offset(width * 0.50f, height * 0.72f), length = height * 0.22f, color = dropColor)
                drawRainDrop(start = Offset(width * 0.65f, height * 0.68f), length = height * 0.22f, color = dropColor)
            }
            WeatherCondition.THUNDERSTORM -> {
                drawCloud(center = Offset(width * 0.5f, height * 0.42f), cloudWidth = width * 0.75f, cloudColor = Color(0xFF546E7A))
                // Lightning bolt
                val path = Path().apply {
                    moveTo(width * 0.52f, height * 0.55f)
                    lineTo(width * 0.42f, height * 0.75f)
                    lineTo(width * 0.52f, height * 0.75f)
                    lineTo(width * 0.45f, height * 0.95f)
                }
                drawPath(path = path, color = Color(0xFFFFD54F))
            }
            WeatherCondition.SNOWY -> {
                drawCloud(center = Offset(width * 0.5f, height * 0.45f), cloudWidth = width * 0.75f, cloudColor = Color(0xFFCFD8DC))
                drawCircle(color = Color.White, radius = width * 0.05f, center = Offset(width * 0.38f, height * 0.75f))
                drawCircle(color = Color.White, radius = width * 0.05f, center = Offset(width * 0.52f, height * 0.82f))
                drawCircle(color = Color.White, radius = width * 0.05f, center = Offset(width * 0.66f, height * 0.75f))
            }
        }
    }
}

private fun DrawScope.drawSun(center: Offset, radius: Float, color: Color) {
    drawCircle(color = color, radius = radius, center = center)
    // Subtle sun rays
    val rayLength = radius * 0.35f
    val rayColor = color.copy(alpha = 0.8f)
    for (i in 0 until 8) {
        val angle = (i * (kotlin.math.PI / 4)).toFloat()
        val startX = center.x + (radius + 2f) * kotlin.math.cos(angle)
        val startY = center.y + (radius + 2f) * kotlin.math.sin(angle)
        val endX = center.x + (radius + 2f + rayLength) * kotlin.math.cos(angle)
        val endY = center.y + (radius + 2f + rayLength) * kotlin.math.sin(angle)
        drawLine(
            color = rayColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCloud(center: Offset, cloudWidth: Float, cloudColor: Color) {
    val r = cloudWidth * 0.26f
    // Base fluffy circles
    drawCircle(color = cloudColor, radius = r * 0.8f, center = Offset(center.x - r * 0.9f, center.y + r * 0.1f))
    drawCircle(color = cloudColor, radius = r * 1.15f, center = Offset(center.x - r * 0.2f, center.y - r * 0.25f))
    drawCircle(color = cloudColor, radius = r * 0.85f, center = Offset(center.x + r * 0.8f, center.y + r * 0.15f))
    // Soft base flat pill
    drawRoundRect(
        color = cloudColor,
        topLeft = Offset(center.x - r * 1.2f, center.y),
        size = androidx.compose.ui.geometry.Size(r * 2.5f, r * 0.8f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.4f, r * 0.4f)
    )
}

private fun DrawScope.drawRainDrop(start: Offset, length: Float, color: Color) {
    drawLine(
        color = color,
        start = start,
        end = Offset(start.x - 3f, start.y + length),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}
