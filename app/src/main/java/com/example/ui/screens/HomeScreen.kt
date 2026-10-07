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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.model.WeatherInfo
import com.example.ui.components.RdsDisplay
import com.example.ui.components.StationLogoImage
import com.example.ui.components.WeatherWidget
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyCardHighlight
import com.example.ui.theme.SpotifyDarkBackground
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary
import java.util.Calendar

@Composable
fun HomeScreen(
    topStations: List<RadioStation>,
    dutchStations: List<RadioStation>,
    danceStations: List<RadioStation>,
    rockStations: List<RadioStation>,
    newsStations: List<RadioStation>,
    currentStation: RadioStation?,
    weatherInfo: WeatherInfo?,
    rdsInfo: RdsInfo? = null,
    isLoading: Boolean,
    onSelectStation: (RadioStation, List<RadioStation>) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onOpenCarMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = rememberGreeting()

    if (isLoading && topStations.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SpotifyDarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SpotifyGreen)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // GREETING, WEATHER & CAR MODE HEADER
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = greeting,
                            color = SpotifyTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Veilig luisteren onderweg",
                            color = SpotifyTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    // Dedicated Driving Mode Toggle Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SpotifyGreen)
                            .clickable(onClick = onOpenCarMode)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("home_open_car_mode_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Open Rijmodus",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rijmodus",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Weather Indicator Widget directly below greeting
                if (weatherInfo != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    WeatherWidget(weatherInfo = weatherInfo)
                }

                // Active Station RDS Live Display Banner on Home Screen
                if (currentStation != null && rdsInfo != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    RdsDisplay(
                        rdsInfo = rdsInfo,
                        isCarMode = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // SPOTIFY TOP 6 TILES GRID (Quick Access)
        item {
            val quickStations = (dutchStations.take(4) + topStations.take(2)).take(6)
            if (quickStations.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in quickStations.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val left = quickStations[i]
                            SpotifyQuickTile(
                                station = left,
                                isPlaying = currentStation?.stationUuid == left.stationUuid,
                                onClick = { onSelectStation(left, quickStations) },
                                modifier = Modifier.weight(1f)
                            )
                            if (i + 1 < quickStations.size) {
                                val right = quickStations[i + 1]
                                SpotifyQuickTile(
                                    station = right,
                                    isPlaying = currentStation?.stationUuid == right.stationUuid,
                                    onClick = { onSelectStation(right, quickStations) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // SECTION: NEDERLANDSE TOP ZENDERS
        if (dutchStations.isNotEmpty()) {
            item {
                StationSection(
                    title = "Nederlandse Top Zenders",
                    subtitle = "Direct afspelen in de auto",
                    stations = dutchStations,
                    currentStation = currentStation,
                    onSelectStation = { station -> onSelectStation(station, dutchStations) },
                    onToggleFavorite = onToggleFavorite
                )
            }
        }

        // SECTION: WERELDWIJD POPULAIR
        if (topStations.isNotEmpty()) {
            item {
                StationSection(
                    title = "Wereldwijd Populair",
                    subtitle = "Best beluisterde radiostations ter wereld",
                    stations = topStations,
                    currentStation = currentStation,
                    onSelectStation = { station -> onSelectStation(station, topStations) },
                    onToggleFavorite = onToggleFavorite
                )
            }
        }

        // SECTION: DANCE & ELECTRONIC
        if (danceStations.isNotEmpty()) {
            item {
                StationSection(
                    title = "Dance & Electronic",
                    subtitle = "Energieke beats voor onderweg",
                    stations = danceStations,
                    currentStation = currentStation,
                    onSelectStation = { station -> onSelectStation(station, danceStations) },
                    onToggleFavorite = onToggleFavorite
                )
            }
        }

        // SECTION: ROCK & CLASSIC HITS
        if (rockStations.isNotEmpty()) {
            item {
                StationSection(
                    title = "Rock & Classic Hits",
                    subtitle = "De grootste gitaar- en tijdloze klassiekers",
                    stations = rockStations,
                    currentStation = currentStation,
                    onSelectStation = { station -> onSelectStation(station, rockStations) },
                    onToggleFavorite = onToggleFavorite
                )
            }
        }

        // SECTION: NIEUWS & TALK
        if (newsStations.isNotEmpty()) {
            item {
                StationSection(
                    title = "Nieuws & Actualiteiten",
                    subtitle = "Blijf op de hoogte tijdens het rijden",
                    stations = newsStations,
                    currentStation = currentStation,
                    onSelectStation = { station -> onSelectStation(station, newsStations) },
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

@Composable
fun SpotifyQuickTile(
    station: RadioStation,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPlaying) SpotifyCardHighlight else SpotifyCardElevated)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StationLogoImage(
            imageUrl = station.favicon,
            stationName = station.name,
            size = 56.dp,
            cornerRadius = 0.dp
        )
        Text(
            text = station.name,
            color = if (isPlaying) SpotifyGreen else SpotifyTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StationSection(
    title: String,
    subtitle: String,
    stations: List<RadioStation>,
    currentStation: RadioStation?,
    onSelectStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = SpotifyTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = SpotifyTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(stations, key = { it.stationUuid }) { station ->
                SpotifyStationCard(
                    station = station,
                    isPlaying = station.stationUuid == currentStation?.stationUuid,
                    onClick = { onSelectStation(station) },
                    onToggleFavorite = { onToggleFavorite(station) }
                )
            }
        }
    }
}

@Composable
fun SpotifyStationCard(
    station: RadioStation,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SpotifyCardElevated)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            StationLogoImage(
                imageUrl = station.favicon,
                stationName = station.name,
                size = 120.dp,
                cornerRadius = 8.dp
            )

            // Heart / Favorite button in top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favoriet schakelen",
                    tint = if (station.isFavorite) SpotifyGreen else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Play icon badge in bottom-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) SpotifyGreen else Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Speel ${station.name}",
                    tint = if (isPlaying) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = station.name,
            color = if (isPlaying) SpotifyGreen else SpotifyTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${station.displayCountryFlag} ${station.genreDisplay}",
            color = SpotifyTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Goedemorgen"
        in 12..17 -> "Goedemiddag"
        in 18..22 -> "Goedenavond"
        else -> "Goedenacht"
    }
}
