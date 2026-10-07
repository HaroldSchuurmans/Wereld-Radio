package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RadioStation
import com.example.model.RdsInfo
import com.example.model.WeatherInfo
import com.example.player.PlaybackStatus
import com.example.ui.components.RdsDisplay
import com.example.ui.components.StationLogoImage
import com.example.ui.components.WeatherWidget
import com.example.ui.theme.CarModeGold
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyCardHighlight
import com.example.ui.theme.SpotifyDarkBackground
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun CarModePlayerScreen(
    station: RadioStation?,
    rdsInfo: RdsInfo,
    status: PlaybackStatus,
    favorites: List<RadioStation>,
    isMuted: Boolean,
    sleepTimerSeconds: Int?,
    weatherInfo: WeatherInfo? = null,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleMute: () -> Unit,
    onSelectSleepTimer: (Int?) -> Unit,
    onSelectStation: (RadioStation) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSleepMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = SpotifyDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER: Car Mode Status & Weather & Quick Safety Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Car Mode Active Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SpotifyCardElevated)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RIJMODUS",
                        color = SpotifyGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Weather Widget in center of Car Mode header
                if (weatherInfo != null) {
                    WeatherWidget(
                        weatherInfo = weatherInfo,
                        iconSize = 24.dp,
                        showLabel = false
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sleep Timer
                    Box {
                        IconButton(
                            onClick = { showSleepMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (sleepTimerSeconds != null) SpotifyCardHighlight else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = "Slaaptimer",
                                tint = if (sleepTimerSeconds != null) CarModeGold else SpotifyTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSleepMenu,
                            onDismissRequest = { showSleepMenu = false },
                            modifier = Modifier.background(SpotifyCardElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Uit", color = SpotifyTextPrimary) },
                                onClick = { onSelectSleepTimer(null); showSleepMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("15 minuten", color = SpotifyTextPrimary) },
                                onClick = { onSelectSleepTimer(15); showSleepMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("30 minuten", color = SpotifyTextPrimary) },
                                onClick = { onSelectSleepTimer(30); showSleepMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("45 minuten", color = SpotifyTextPrimary) },
                                onClick = { onSelectSleepTimer(45); showSleepMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("60 minuten", color = SpotifyTextPrimary) },
                                onClick = { onSelectSleepTimer(60); showSleepMenu = false }
                            )
                        }
                    }

                    // Quick Mute (for traffic sirens or conversation)
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("car_mode_mute_btn")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Geluid aanzetten" else "Dempen",
                            tint = if (isMuted) CarModeGold else SpotifyTextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Exit / Minimize button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("car_mode_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Sluit car player",
                            tint = SpotifyTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CENTER: Big Station Artwork & RDS Info
            if (station != null) {
                // Large Station Artwork
                StationLogoImage(
                    imageUrl = station.favicon,
                    stationName = station.name,
                    size = 190.dp,
                    cornerRadius = 24.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Station Title & Country Flag
                Text(
                    text = station.name,
                    color = SpotifyTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${station.displayCountryFlag} ${station.country.ifBlank { "Wereldwijd" }}",
                        color = SpotifyTextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = " • ",
                        color = SpotifyTextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = station.genreDisplay,
                        color = SpotifyGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live RDS Data Display
                RdsDisplay(
                    rdsInfo = rdsInfo,
                    isCarMode = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Geen zender geselecteerd",
                    color = SpotifyTextSecondary,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // GIANT DRIVING TRANSPORT CONTROLS (Safe & Accessible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Heart Favorite Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SpotifyCardElevated)
                        .clickable(onClick = onToggleFavorite)
                        .testTag("car_mode_favorite_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (station?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoriet schakelen",
                        tint = if (station?.isFavorite == true) SpotifyGreen else SpotifyTextSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Previous Station (Giant button)
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(SpotifyCardElevated)
                        .clickable(onClick = onPrevious)
                        .testTag("car_mode_previous_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Vorige zender",
                        tint = SpotifyTextPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Huge Play/Pause FAB
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen)
                        .clickable(onClick = onTogglePlay)
                        .testTag("car_mode_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    when (status) {
                        PlaybackStatus.BUFFERING -> {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        PlaybackStatus.PLAYING -> {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pauzeer",
                                tint = Color.Black,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Afspelen",
                                tint = Color.Black,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }

                // Next Station (Giant button)
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(SpotifyCardElevated)
                        .clickable(onClick = onNext)
                        .testTag("car_mode_next_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Volgende zender",
                        tint = SpotifyTextPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // QUICK FAVORITES PRESETS BAR (1-Tap while driving)
            if (favorites.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SNELLE VOORKEUZES",
                        color = SpotifyTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favorites, key = { it.stationUuid }) { favStation ->
                            val isSelected = favStation.stationUuid == station?.stationUuid
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SpotifyGreen else SpotifyCardElevated)
                                    .clickable { onSelectStation(favStation) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StationLogoImage(
                                    imageUrl = favStation.favicon,
                                    stationName = favStation.name,
                                    size = 28.dp,
                                    cornerRadius = 6.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = favStation.name,
                                    color = if (isSelected) Color.Black else SpotifyTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
