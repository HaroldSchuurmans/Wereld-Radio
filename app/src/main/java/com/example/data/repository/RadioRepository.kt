package com.example.data.repository

import com.example.data.CuratedStations
import com.example.data.local.FavoriteStationDao
import com.example.data.local.FavoriteStationEntity
import com.example.data.remote.RadioBrowserApiService
import com.example.data.remote.StationDto
import com.example.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RadioRepository(
    private val apiService: RadioBrowserApiService,
    private val favoriteDao: FavoriteStationDao
) {

    val favoriteStations: Flow<List<RadioStation>> = favoriteDao.getAllFavorites()
        .map { entities ->
            entities.map { entity ->
                RadioStation(
                    stationUuid = entity.stationUuid,
                    name = entity.name,
                    streamUrl = entity.streamUrl,
                    favicon = entity.favicon,
                    homepage = entity.homepage,
                    country = entity.country,
                    countryCode = entity.countryCode,
                    tags = entity.tags,
                    codec = entity.codec,
                    bitrate = entity.bitrate,
                    votes = entity.votes,
                    isFavorite = true,
                    lastPlayedTimestamp = entity.lastPlayedTimestamp
                )
            }
        }.flowOn(Dispatchers.IO)

    val recentStations: Flow<List<RadioStation>> = favoriteDao.getRecentHistory()
        .map { entities ->
            entities.map { entity ->
                RadioStation(
                    stationUuid = entity.stationUuid,
                    name = entity.name,
                    streamUrl = entity.streamUrl,
                    favicon = entity.favicon,
                    homepage = entity.homepage,
                    country = entity.country,
                    countryCode = entity.countryCode,
                    tags = entity.tags,
                    codec = entity.codec,
                    bitrate = entity.bitrate,
                    votes = entity.votes,
                    isFavorite = entity.isFavorite,
                    lastPlayedTimestamp = entity.lastPlayedTimestamp
                )
            }
        }.flowOn(Dispatchers.IO)

    suspend fun isFavorite(uuid: String): Boolean = withContext(Dispatchers.IO) {
        val entity = favoriteDao.getFavoriteById(uuid)
        entity?.isFavorite == true
    }

    suspend fun toggleFavorite(station: RadioStation): Boolean = withContext(Dispatchers.IO) {
        val existing = favoriteDao.getFavoriteById(station.stationUuid)
        if (existing != null) {
            val newFav = !existing.isFavorite
            favoriteDao.insertOrUpdate(
                existing.copy(
                    isFavorite = newFav,
                    addedTimestamp = if (newFav) System.currentTimeMillis() else 0L
                )
            )
            newFav
        } else {
            favoriteDao.insertOrUpdate(
                FavoriteStationEntity(
                    stationUuid = station.stationUuid,
                    name = station.name,
                    streamUrl = station.streamUrl,
                    favicon = station.favicon,
                    homepage = station.homepage,
                    country = station.country,
                    countryCode = station.countryCode,
                    tags = station.tags,
                    codec = station.codec,
                    bitrate = station.bitrate,
                    votes = station.votes,
                    isFavorite = true,
                    addedTimestamp = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun recordPlayed(station: RadioStation) = withContext(Dispatchers.IO) {
        val existing = favoriteDao.getFavoriteById(station.stationUuid)
        val now = System.currentTimeMillis()
        if (existing != null) {
            favoriteDao.updateLastPlayed(station.stationUuid, now)
        } else {
            // CRITICAL: isFavorite MUST be false so it only logs play history and NEVER becomes a favorite!
            favoriteDao.insertOrUpdate(
                FavoriteStationEntity(
                    stationUuid = station.stationUuid,
                    name = station.name,
                    streamUrl = station.streamUrl,
                    favicon = station.favicon,
                    homepage = station.homepage,
                    country = station.country,
                    countryCode = station.countryCode,
                    tags = station.tags,
                    codec = station.codec,
                    bitrate = station.bitrate,
                    votes = station.votes,
                    isFavorite = false,
                    addedTimestamp = 0L,
                    lastPlayedTimestamp = now
                )
            )
        }
    }

    suspend fun getTopStations(): List<RadioStation> = withContext(Dispatchers.IO) {
        val favIds = favoriteDao.getAllFavorites().first().map { it.stationUuid }.toSet()
        val curated = CuratedStations.defaultStations.map { it.copy(isFavorite = favIds.contains(it.stationUuid)) }
        try {
            val dtoList = apiService.getTopVotedStations(limit = 40)
            val remote = dtoList.mapNotNull { it.toDomain(favIds) }
            val merged = (curated + remote).distinctBy { it.name.trim().lowercase() }
            merged
        } catch (_: Exception) {
            curated
        }
    }

    suspend fun getDutchStations(): List<RadioStation> = withContext(Dispatchers.IO) {
        val favIds = favoriteDao.getAllFavorites().first().map { it.stationUuid }.toSet()
        val curatedDutch = CuratedStations.defaultStations
            .filter { it.countryCode == "NL" }
            .map { it.copy(isFavorite = favIds.contains(it.stationUuid)) }
        try {
            val dtoList = apiService.getStationsByCountry(countryCode = "NL", limit = 40)
            val remote = dtoList.mapNotNull { it.toDomain(favIds) }
            val merged = (curatedDutch + remote).distinctBy { it.name.trim().lowercase() }
            merged
        } catch (_: Exception) {
            curatedDutch
        }
    }

    suspend fun getStationsByCountry(countryCode: String): List<RadioStation> = withContext(Dispatchers.IO) {
        val favIds = favoriteDao.getAllFavorites().first().map { it.stationUuid }.toSet()
        val curatedMatching = CuratedStations.defaultStations
            .filter { it.countryCode.equals(countryCode, ignoreCase = true) }
            .map { it.copy(isFavorite = favIds.contains(it.stationUuid)) }
        try {
            val dtoList = apiService.getStationsByCountry(countryCode = countryCode, limit = 40)
            val remote = dtoList.mapNotNull { it.toDomain(favIds) }
            val merged = (curatedMatching + remote).distinctBy { it.name.trim().lowercase() }
            if (merged.isNotEmpty()) merged else curatedMatching
        } catch (_: Exception) {
            curatedMatching
        }
    }

    suspend fun getStationsByGenre(genre: String): List<RadioStation> = withContext(Dispatchers.IO) {
        val favIds = favoriteDao.getAllFavorites().first().map { it.stationUuid }.toSet()
        val curatedGenre = CuratedStations.defaultStations
            .filter { it.tags.contains(genre, ignoreCase = true) }
            .map { it.copy(isFavorite = favIds.contains(it.stationUuid)) }
        try {
            val dtoList = apiService.getStationsByTag(tag = genre.lowercase(), limit = 40)
            val remote = dtoList.mapNotNull { it.toDomain(favIds) }
            val merged = (curatedGenre + remote).distinctBy { it.name.trim().lowercase() }
            if (merged.isNotEmpty()) merged else curatedGenre
        } catch (_: Exception) {
            curatedGenre
        }
    }

    suspend fun searchStations(query: String, countryCode: String? = null, genre: String? = null): List<RadioStation> =
        withContext(Dispatchers.IO) {
            val favIds = favoriteDao.getAllFavorites().first().map { it.stationUuid }.toSet()
            val curatedMatches = CuratedStations.defaultStations.filter {
                (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.tags.contains(query, ignoreCase = true)) &&
                        (countryCode.isNullOrBlank() || it.countryCode.equals(countryCode, ignoreCase = true)) &&
                        (genre.isNullOrBlank() || it.tags.contains(genre, ignoreCase = true))
            }.map { it.copy(isFavorite = favIds.contains(it.stationUuid)) }

            try {
                val dtoList = apiService.searchStations(
                    name = query.ifBlank { null },
                    countryCode = countryCode?.ifBlank { null },
                    tag = genre?.ifBlank { null },
                    limit = 50
                )
                val remote = dtoList.mapNotNull { it.toDomain(favIds) }
                (curatedMatches + remote).distinctBy { it.name.trim().lowercase() }
            } catch (_: Exception) {
                curatedMatches
            }
        }

    private fun StationDto.toDomain(favoriteIds: Set<String>): RadioStation? {
        val uuid = stationuuid ?: return null
        val stream = urlResolved?.takeIf { it.isNotBlank() } ?: url?.takeIf { it.isNotBlank() } ?: return null
        
        // Filter out stations that fail recent health checks or have invalid stream schemes
        if (lastcheckok != null && lastcheckok == 0) return null
        if (!stream.startsWith("http://") && !stream.startsWith("https://")) return null

        val stationName = name?.trim()?.takeIf { it.isNotBlank() } ?: "Radiozender"
        return RadioStation(
            stationUuid = uuid,
            name = stationName,
            streamUrl = stream,
            favicon = favicon?.trim() ?: "",
            homepage = homepage?.trim() ?: "",
            country = country?.trim() ?: "",
            countryCode = countrycode?.trim() ?: "",
            tags = tags?.trim() ?: "",
            codec = codec?.trim() ?: "MP3",
            bitrate = bitrate ?: 128,
            votes = votes ?: 0,
            isFavorite = favoriteIds.contains(uuid)
        )
    }
}
