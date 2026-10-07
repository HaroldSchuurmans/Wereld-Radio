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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RadioStation
import com.example.ui.components.StationLogoImage
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyCardHighlight
import com.example.ui.theme.SpotifyDarkBackground
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@Composable
fun SearchScreen(
    searchQuery: String,
    selectedCountry: String?,
    selectedGenre: String?,
    searchResults: List<RadioStation>,
    isSearching: Boolean,
    currentStation: RadioStation?,
    onQueryChange: (String) -> Unit,
    onCountrySelect: (String?) -> Unit,
    onGenreSelect: (String?) -> Unit,
    onSelectStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val countries = listOf(
        Pair("Alle", null),
        Pair("🇳🇱 Nederland", "NL"),
        Pair("🇧🇪 België", "BE"),
        Pair("🇩🇪 Duitsland", "DE"),
        Pair("🇬🇧 UK", "GB"),
        Pair("🇫🇷 Frankrijk", "FR"),
        Pair("🇪🇸 Spanje", "ES"),
        Pair("🇺🇸 USA", "US")
    )

    val genres = listOf(
        "Pop", "Dance", "Rock", "80s", "90s", "News", "Jazz", "Classical", "Hip Hop", "Electro"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyDarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // TITLE
        item {
            Text(
                text = "Wereldwijd Zoeken",
                color = SpotifyTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // SEARCH INPUT BAR (Spotify style)
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Zoek zenders, artiesten of frequenties...",
                        color = SpotifyTextSecondary,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Zoeken",
                        tint = SpotifyTextSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Wis invoer",
                                tint = SpotifyTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SpotifyCardElevated,
                    unfocusedContainerColor = SpotifyCardElevated,
                    disabledContainerColor = SpotifyCardElevated,
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = SpotifyTextPrimary,
                    unfocusedTextColor = SpotifyTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("search_input_field")
            )
        }

        // COUNTRY FILTER CHIPS
        item {
            Text(
                text = "Filter op land",
                color = SpotifyTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                items(countries) { (label, code) ->
                    val isSelected = selectedCountry == code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SpotifyGreen else SpotifyCardElevated)
                            .clickable { onCountrySelect(if (isSelected) null else code) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else SpotifyTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // GENRE FILTER CHIPS
        item {
            Text(
                text = "Filter op genre",
                color = SpotifyTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(genres) { genre ->
                    val isSelected = selectedGenre.equals(genre, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SpotifyGreen else SpotifyCardElevated)
                            .clickable { onGenreSelect(genre) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = genre,
                            color = if (isSelected) Color.Black else SpotifyTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // LOADING INDICATOR
        if (isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        } else if (searchResults.isNotEmpty()) {
            item {
                Text(
                    text = "${searchResults.size} zenders gevonden",
                    color = SpotifyTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            items(searchResults, key = { it.stationUuid }) { station ->
                StationListItem(
                    station = station,
                    isPlaying = station.stationUuid == currentStation?.stationUuid,
                    onClick = { onSelectStation(station) },
                    onToggleFavorite = { onToggleFavorite(station) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else if (searchQuery.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = SpotifyTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Geen radiozenders gevonden",
                        color = SpotifyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Probeer een andere zendernaam of kies een ander land.",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SpotifyTextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Vind je favoriete live streams",
                        color = SpotifyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Typ een zendernaam of kies een land hierboven om tienduizenden live internetradiostations met RDS-weergave te ontdekken.",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StationListItem(
    station: RadioStation,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPlaying) SpotifyCardHighlight else SpotifyCardElevated)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StationLogoImage(
            imageUrl = station.favicon,
            stationName = station.name,
            size = 48.dp,
            cornerRadius = 8.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                color = if (isPlaying) SpotifyGreen else SpotifyTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${station.displayCountryFlag} ${station.genreDisplay}",
                    color = SpotifyTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${station.displayBitrate}",
                    color = SpotifyGreen,
                    fontSize = 11.sp
                )
            }
        }

        // Heart Icon for Favorite
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.testTag("search_station_favorite_btn_${station.stationUuid}")
        ) {
            Icon(
                imageVector = if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (station.isFavorite) "Verwijder uit favorieten" else "Voeg toe aan favorieten",
                tint = if (station.isFavorite) SpotifyGreen else SpotifyTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        if (isPlaying) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Nu aan het spelen",
                tint = SpotifyGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
