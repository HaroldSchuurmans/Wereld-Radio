package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AutoRadioApplication
import com.example.model.RadioStation
import com.example.model.RdsInfo
import com.example.player.PlaybackStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoRadioApplication
    private val repository = app.repository
    private val playerManager = app.playerManager

    val currentStation: StateFlow<RadioStation?> = playerManager.currentStation
    val playbackStatus: StateFlow<PlaybackStatus> = playerManager.playbackStatus
    val rdsInfo: StateFlow<RdsInfo> = playerManager.rdsInfo
    val errorMessage: StateFlow<String?> = playerManager.errorMessage
    val sleepTimerRemaining: StateFlow<Int?> = playerManager.sleepTimerSeconds
    val isMuted: StateFlow<Boolean> = playerManager.isMuted

    val favoriteStations: StateFlow<List<RadioStation>> = repository.favoriteStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStations: StateFlow<List<RadioStation>> = repository.recentStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _topStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val topStations: StateFlow<List<RadioStation>> = _topStations.asStateFlow()

    private val _dutchStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val dutchStations: StateFlow<List<RadioStation>> = _dutchStations.asStateFlow()

    private val _danceStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val danceStations: StateFlow<List<RadioStation>> = _danceStations.asStateFlow()

    private val _rockStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val rockStations: StateFlow<List<RadioStation>> = _rockStations.asStateFlow()

    private val _newsStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val newsStations: StateFlow<List<RadioStation>> = _newsStations.asStateFlow()

    private val _searchResults = MutableStateFlow<List<RadioStation>>(emptyList())
    val searchResults: StateFlow<List<RadioStation>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCountryFilter = MutableStateFlow<String?>(null)
    val selectedCountryFilter: StateFlow<String?> = _selectedCountryFilter.asStateFlow()

    private val _selectedGenreFilter = MutableStateFlow<String?>(null)
    val selectedGenreFilter: StateFlow<String?> = _selectedGenreFilter.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(false)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    private val _isCarMode = MutableStateFlow(false)
    val isCarMode: StateFlow<Boolean> = _isCarMode.asStateFlow()

    private val _showFullPlayer = MutableStateFlow(false)
    val showFullPlayer: StateFlow<Boolean> = _showFullPlayer.asStateFlow()

    private val _weatherInfo = MutableStateFlow<com.example.model.WeatherInfo?>(null)
    val weatherInfo: StateFlow<com.example.model.WeatherInfo?> = _weatherInfo.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadHomeData()
        loadWeatherData()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            try {
                val resp = app.weatherApiService.getCurrentWeather()
                val current = resp.current
                if (current != null) {
                    val info = com.example.model.WeatherInfo.fromWmoCode(
                        code = current.weatherCode,
                        temp = current.temperature,
                        isDayTime = current.isDay != 0
                    )
                    _weatherInfo.value = info
                }
            } catch (_: Exception) {
                // Default fallback nice weather
                _weatherInfo.value = com.example.model.WeatherInfo(
                    temperatureCelsius = 19,
                    condition = com.example.model.WeatherCondition.PARTLY_CLOUDY,
                    conditionDescription = "Licht bewolkt",
                    isDay = true
                )
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isLoadingHome.value = true
            try {
                _dutchStations.value = repository.getDutchStations()
                _topStations.value = repository.getTopStations()
                _danceStations.value = repository.getStationsByGenre("dance")
                _rockStations.value = repository.getStationsByGenre("rock")
                _newsStations.value = repository.getStationsByGenre("news")
            } catch (_: Exception) {
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    fun playStation(station: RadioStation, contextPlaylist: List<RadioStation>? = null) {
        viewModelScope.launch {
            repository.recordPlayed(station)
            val isFav = repository.isFavorite(station.stationUuid)
            val updated = station.copy(isFavorite = isFav)
            val playlist = contextPlaylist ?: listOf(updated)
            playerManager.playStation(updated, playlist)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun nextStation() {
        playerManager.nextStation()
    }

    fun previousStation() {
        playerManager.previousStation()
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavorite(station)
            
            // Sync current station if playing
            val current = currentStation.value
            if (current != null && current.stationUuid == station.stationUuid) {
                // Trigger updated currentStation inside playerManager
                playerManager.updateCurrentStationFavorite(isNowFav)
            }
            
            // Sync all in-memory lists
            updateListsFavoriteState(station.stationUuid, isNowFav)
        }
    }

    private fun updateListsFavoriteState(uuid: String, isFav: Boolean) {
        fun update(list: List<RadioStation>): List<RadioStation> {
            return list.map { if (it.stationUuid == uuid) it.copy(isFavorite = isFav) else it }
        }
        _topStations.value = update(_topStations.value)
        _dutchStations.value = update(_dutchStations.value)
        _danceStations.value = update(_danceStations.value)
        _rockStations.value = update(_rockStations.value)
        _newsStations.value = update(_newsStations.value)
        _searchResults.value = update(_searchResults.value)
    }

    fun setCarMode(enabled: Boolean) {
        _isCarMode.value = enabled
    }

    fun setShowFullPlayer(show: Boolean) {
        _showFullPlayer.value = show
    }

    fun setSleepTimer(minutes: Int?) {
        playerManager.setSleepTimer(minutes)
    }

    fun toggleMute() {
        playerManager.toggleMute()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        triggerDebouncedSearch()
    }

    fun selectCountryFilter(countryCode: String?) {
        _selectedCountryFilter.value = countryCode
        triggerDebouncedSearch()
    }

    fun selectGenreFilter(genre: String?) {
        _selectedGenreFilter.value = if (_selectedGenreFilter.value == genre) null else genre
        triggerDebouncedSearch()
    }

    private fun triggerDebouncedSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            val query = _searchQuery.value.trim()
            val country = _selectedCountryFilter.value
            val genre = _selectedGenreFilter.value

            if (query.isEmpty() && country == null && genre == null) {
                _searchResults.value = emptyList()
                return@launch
            }

            _isSearching.value = true
            try {
                val results = repository.searchStations(
                    query = query,
                    countryCode = country,
                    genre = genre
                )
                _searchResults.value = results
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}
