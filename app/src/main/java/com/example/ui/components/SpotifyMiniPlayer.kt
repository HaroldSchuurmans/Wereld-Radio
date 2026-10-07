package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RadioStation
import com.example.model.RdsInfo
import com.example.player.PlaybackStatus
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun SpotifyMiniPlayer(
    station: RadioStation,
    rdsInfo: RdsInfo,
    status: PlaybackStatus,
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SpotifyCardElevated)
            .clickable(onClick = onOpenFullPlayer)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("spotify_mini_player")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station Logo
            StationLogoImage(
                imageUrl = station.favicon,
                stationName = station.name,
                size = 46.dp,
                cornerRadius = 8.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info (Station Name & RDS track ticker)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = station.name,
                    color = SpotifyTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (rdsInfo.displayTrack.isNotBlank()) rdsInfo.displayTrack else "${station.displayCountryFlag} ${station.genreDisplay}",
                    color = if (rdsInfo.trackTitle.isNotBlank()) SpotifyGreen else SpotifyTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("mini_player_favorite_btn")
            ) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (station.isFavorite) "Verwijder uit favorieten" else "Voeg toe aan favorieten",
                    tint = if (station.isFavorite) SpotifyGreen else SpotifyTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Big Play/Pause Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen)
                    .clickable(onClick = onTogglePlay)
                    .testTag("mini_player_play_pause_btn"),
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    PlaybackStatus.BUFFERING -> {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    PlaybackStatus.PLAYING -> {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pauzeer radio",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Speel radio",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
