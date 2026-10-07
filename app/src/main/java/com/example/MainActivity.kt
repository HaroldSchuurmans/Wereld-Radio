package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.SpotifyMiniPlayer
import com.example.ui.screens.CarModePlayerScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.AutoRadioTheme
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyDarkBackground
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // We do NOT manually call startForegroundService here; MediaSessionService/MediaLibraryService
        // is automatically bound and managed by Media3/Android Auto when needed, avoiding
        // ForegroundServiceDidNotStartInTimeException ANRs/crashes.
        setContent {
            AutoRadioTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MainViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentStation by viewModel.currentStation.collectAsStateWithLifecycle()
    val playbackStatus by viewModel.playbackStatus.collectAsStateWithLifecycle()
    val rdsInfo by viewModel.rdsInfo.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteStations.collectAsStateWithLifecycle()
    val isCarMode by viewModel.isCarMode.collectAsStateWithLifecycle()
    val showFullPlayer by viewModel.showFullPlayer.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val topStations by viewModel.topStations.collectAsStateWithLifecycle()
    val dutchStations by viewModel.dutchStations.collectAsStateWithLifecycle()
    val danceStations by viewModel.danceStations.collectAsStateWithLifecycle()
    val rockStations by viewModel.rockStations.collectAsStateWithLifecycle()
    val newsStations by viewModel.newsStations.collectAsStateWithLifecycle()
    val isLoadingHome by viewModel.isLoadingHome.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountryFilter.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenreFilter.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val weatherInfo by viewModel.weatherInfo.collectAsStateWithLifecycle()

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val isDrivingModeActive = isCarMode || showFullPlayer

    BackHandler(enabled = isDrivingModeActive) {
        viewModel.setCarMode(false)
        viewModel.setShowFullPlayer(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpotifyDarkBackground)
                ) {
                    // Spotify-style Mini Player docked right above bottom nav
                    if (currentStation != null && !isDrivingModeActive) {
                        SpotifyMiniPlayer(
                            station = currentStation!!,
                            rdsInfo = rdsInfo,
                            status = playbackStatus,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onToggleFavorite = { viewModel.toggleFavorite(currentStation!!) },
                            onOpenFullPlayer = { viewModel.setShowFullPlayer(true) }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = SpotifyDarkBackground,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("spotify_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotifyGreen,
                                selectedTextColor = SpotifyGreen,
                                unselectedIconColor = SpotifyTextSecondary,
                                unselectedTextColor = SpotifyTextSecondary,
                                indicatorColor = SpotifyCardElevated
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Zoeken") },
                            label = { Text("Zoeken") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotifyGreen,
                                selectedTextColor = SpotifyGreen,
                                unselectedIconColor = SpotifyTextSecondary,
                                unselectedTextColor = SpotifyTextSecondary,
                                indicatorColor = SpotifyCardElevated
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorieten") },
                            label = { Text("Favorieten") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotifyGreen,
                                selectedTextColor = SpotifyGreen,
                                unselectedIconColor = SpotifyTextSecondary,
                                unselectedTextColor = SpotifyTextSecondary,
                                indicatorColor = SpotifyCardElevated
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = {
                                viewModel.setCarMode(true)
                            },
                            icon = {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = "Rijmodus",
                                    tint = SpotifyGreen
                                )
                            },
                            label = { Text("Rijmodus", color = SpotifyGreen) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotifyGreen,
                                selectedTextColor = SpotifyGreen,
                                unselectedIconColor = SpotifyGreen,
                                unselectedTextColor = SpotifyGreen,
                                indicatorColor = SpotifyCardElevated
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        topStations = topStations,
                        dutchStations = dutchStations,
                        danceStations = danceStations,
                        rockStations = rockStations,
                        newsStations = newsStations,
                        currentStation = currentStation,
                        weatherInfo = weatherInfo,
                        rdsInfo = rdsInfo,
                        isLoading = isLoadingHome,
                        onSelectStation = { station, list ->
                            viewModel.playStation(station, list)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onOpenCarMode = { viewModel.setCarMode(true) }
                    )
                    1 -> SearchScreen(
                        searchQuery = searchQuery,
                        selectedCountry = selectedCountry,
                        selectedGenre = selectedGenre,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        currentStation = currentStation,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        onCountrySelect = { viewModel.selectCountryFilter(it) },
                        onGenreSelect = { viewModel.selectGenreFilter(it) },
                        onSelectStation = { viewModel.playStation(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                    2 -> FavoritesScreen(
                        favorites = favorites,
                        currentStation = currentStation,
                        onSelectStation = { station, list ->
                            viewModel.playStation(station, list)
                        },
                        onRemoveFavorite = { viewModel.toggleFavorite(it) },
                        onOpenCarMode = { viewModel.setCarMode(true) },
                        onGoToDiscover = { selectedTab = 0 }
                    )
                }
            }
        }

        // FULLSCREEN SPOTIFY CAR DRIVING MODE OVERLAY
        AnimatedVisibility(
            visible = isDrivingModeActive,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            CarModePlayerScreen(
                station = currentStation ?: topStations.firstOrNull(),
                rdsInfo = rdsInfo,
                status = playbackStatus,
                favorites = favorites,
                isMuted = isMuted,
                sleepTimerSeconds = sleepTimer,
                weatherInfo = weatherInfo,
                onTogglePlay = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.previousStation() },
                onNext = { viewModel.nextStation() },
                onToggleFavorite = {
                    val station = currentStation ?: topStations.firstOrNull()
                    station?.let { viewModel.toggleFavorite(it) }
                },
                onToggleMute = { viewModel.toggleMute() },
                onSelectSleepTimer = { viewModel.setSleepTimer(it) },
                onSelectStation = { viewModel.playStation(it) },
                onClose = {
                    viewModel.setCarMode(false)
                    viewModel.setShowFullPlayer(false)
                }
            )
        }
    }
}
