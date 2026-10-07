package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_stations")
data class FavoriteStationEntity(
    @PrimaryKey
    val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val favicon: String,
    val homepage: String,
    val country: String,
    val countryCode: String,
    val tags: String,
    val codec: String,
    val bitrate: Int,
    val votes: Int,
    val isFavorite: Boolean = false,
    val addedTimestamp: Long = 0L,
    val lastPlayedTimestamp: Long = 0L
)

@Dao
interface FavoriteStationDao {
    @Query("SELECT * FROM favorite_stations WHERE isFavorite = 1 ORDER BY addedTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteStationEntity>>

    @Query("SELECT * FROM favorite_stations WHERE stationUuid = :uuid LIMIT 1")
    suspend fun getFavoriteById(uuid: String): FavoriteStationEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stations WHERE stationUuid = :uuid AND isFavorite = 1)")
    fun isFavorite(uuid: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(station: FavoriteStationEntity)

    @Query("UPDATE favorite_stations SET isFavorite = 0, addedTimestamp = 0 WHERE stationUuid = :uuid")
    suspend fun removeFavorite(uuid: String)

    @Query("SELECT * FROM favorite_stations WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 15")
    fun getRecentHistory(): Flow<List<FavoriteStationEntity>>

    @Query("UPDATE favorite_stations SET lastPlayedTimestamp = :timestamp WHERE stationUuid = :uuid")
    suspend fun updateLastPlayed(uuid: String, timestamp: Long)
}
