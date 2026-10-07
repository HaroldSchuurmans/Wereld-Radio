package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RdsInfo
import com.example.ui.theme.RdsBadgeLive
import com.example.ui.theme.RdsStereoGreen
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun RdsDisplay(
    rdsInfo: RdsInfo,
    modifier: Modifier = Modifier,
    isCarMode: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rds_blink")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rds_live_blink"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpotifyCardElevated)
            .padding(if (isCarMode) 16.dp else 12.dp)
    ) {
        // Top status row: RDS indicator, Stereo badge, Bitrate
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulsing LIVE dot
                Box(
                    modifier = Modifier
                        .size(if (isCarMode) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(RdsBadgeLive.copy(alpha = alphaAnim))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RDS LIVE",
                    color = RdsBadgeLive,
                    fontSize = if (isCarMode) 13.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Stereo / Quality Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E2F23))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = RdsStereoGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STEREO",
                            color = RdsStereoGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${rdsInfo.bitrate} kbps ${rdsInfo.codec}",
                    color = SpotifyTextSecondary,
                    fontSize = if (isCarMode) 12.sp else 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live Track Marquee / Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isCarMode) 10.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier
                    .size(if (isCarMode) 22.dp else 16.dp)
                    .alpha(0.85f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = rdsInfo.displayTrack,
                color = SpotifyTextPrimary,
                fontSize = if (isCarMode) 18.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}
