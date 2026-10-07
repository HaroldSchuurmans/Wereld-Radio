package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.data.CuratedStations
import com.example.data.local.AppDatabase
import com.example.data.remote.RadioBrowserApiService
import com.example.data.repository.RadioRepository
import com.example.model.RadioStation
import com.example.player.RadioPlayerManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(UnstableApi::class)
class RadioMediaService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var playerManager: RadioPlayerManager
    private lateinit var repository: RadioRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val ROOT_ID = "root"
        const val CATEGORY_FAVORITES = "category_favorites"
        const val CATEGORY_DUTCH = "category_dutch"
        const val CATEGORY_TOP = "category_top"
        const val CATEGORY_GENRES = "category_genres"
        private const val NOTIFICATION_CHANNEL_ID = "autoradio_media_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()

        playerManager = RadioPlayerManager.getInstance(this)
        val database = AppDatabase.getDatabase(this)
        repository = RadioRepository(RadioBrowserApiService.create(), database.favoriteStationDao())

        // Eagerly preload cached icons for default curated stations so Android Auto always has instant artwork
        CuratedStations.defaultStations.forEach { station ->
            StationIconProvider.preloadIcon(this, station.stationUuid, station.name, station.favicon)
        }

        initializeSession()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "AutoRadio Afspelen",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Toont bediening voor de radio op de achtergrond en Android Auto"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun initializeSession() {
        if (mediaLibrarySession != null) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, playerManager.exoPlayer, MediaLibraryCallback())
            .setSessionActivity(pendingIntent)
            .setId("AutoRadioSession_${System.currentTimeMillis()}")
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initializeSession()
        super.onStartCommand(intent, flags, startId)
        // START_STICKY ensures Android OS restarts the service automatically if memory pressure drops it
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        initializeSession()
        return mediaLibrarySession
    }

    private inner class MediaLibraryCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Explicitly allow full connection and all commands for Android Auto, Bluetooth and Head Units
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .build()
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            // Android Auto root navigation item
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("AutoRadio")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

            serviceScope.launch {
                try {
                    when (parentId) {
                        ROOT_ID -> {
                            // Top-level categories optimized for car touchscreens with clear badges
                            val categories = ImmutableList.of(
                                createFolderItem(CATEGORY_DUTCH, "🇳🇱 Nederlandse Zenders", "538, Qmusic, NPO, Sky Radio, Veronica"),
                                createFolderItem(CATEGORY_FAVORITES, "⭐ Favoriete Zenders", "Jouw opgeslagen favorieten"),
                                createFolderItem(CATEGORY_TOP, "🌍 Wereld Top Zenders", "Best beoordeelde internationale stations"),
                                createFolderItem(CATEGORY_GENRES, "🎵 Genres & Sferen", "Dance, Rock, Pop, Nieuws, Jazz")
                            )
                            future.set(LibraryResult.ofItemList(categories, params))
                        }

                        CATEGORY_DUTCH -> {
                            val stations = withTimeoutOrNull(2000) { repository.getDutchStations() }
                                ?: CuratedStations.defaultStations
                            val items = stations.map { it.toMediaItem() }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                        }

                        CATEGORY_FAVORITES -> {
                            val favorites = withTimeoutOrNull(1500) { repository.favoriteStations.first() }
                            val items = if (!favorites.isNullOrEmpty()) {
                                favorites.map { it.toMediaItem() }
                            } else {
                                // If no favorites saved yet, show curated top stations as helpful suggestions
                                CuratedStations.defaultStations.take(6).map { it.toMediaItem() }
                            }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                        }

                        CATEGORY_TOP -> {
                            val stations = withTimeoutOrNull(2000) { repository.getTopStations() }
                                ?: CuratedStations.defaultStations
                            val items = stations.map { it.toMediaItem() }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                        }

                        CATEGORY_GENRES -> {
                            val genres = listOf(
                                Pair("Dance", "Club & Electronic"),
                                Pair("Rock", "Classic & Modern Rock"),
                                Pair("Pop", "Top 40 & Hits"),
                                Pair("News", "Nieuws & Actualiteiten"),
                                Pair("Jazz", "Smooth Jazz & Blues"),
                                Pair("Classical", "Klassieke Muziek")
                            )
                            val genreItems = genres.map { (genre, subtitle) ->
                                createFolderItem("genre_$genre", genre, subtitle)
                            }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(genreItems), params))
                        }

                        else -> {
                            if (parentId.startsWith("genre_")) {
                                val tag = parentId.removePrefix("genre_")
                                val stations = withTimeoutOrNull(2000) { repository.getStationsByGenre(tag) }
                                    ?: CuratedStations.defaultStations.filter { it.tags.contains(tag, ignoreCase = true) }
                                val items = stations.map { it.toMediaItem() }
                                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                            } else {
                                future.set(LibraryResult.ofItemList(ImmutableList.of(), params))
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Safe instant fallback to curated stations so Android Auto NEVER shows "geen inhoud kan laden"
                    val fallback = CuratedStations.defaultStations.map { it.toMediaItem() }
                    future.set(LibraryResult.ofItemList(ImmutableList.copyOf(fallback), params))
                }
            }

            return future
        }

        // Handle item selection from Android Auto UI
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val resolvedItems = mediaItems.map { item ->
                if (item.localConfiguration?.uri != null) {
                    item
                } else {
                    val station = CuratedStations.defaultStations.find { it.stationUuid == item.mediaId }
                    if (station != null) {
                        station.toMediaItem()
                    } else {
                        item
                    }
                }
            }

            // Also synchronize with playerManager's currentStation state
            resolvedItems.getOrNull(startIndex)?.let { chosen ->
                val matchedStation = CuratedStations.defaultStations.find { it.stationUuid == chosen.mediaId }
                    ?: RadioStation(
                        stationUuid = chosen.mediaId,
                        name = chosen.mediaMetadata.title?.toString() ?: "Radio",
                        streamUrl = chosen.localConfiguration?.uri?.toString() ?: "",
                        favicon = chosen.mediaMetadata.artworkUri?.toString() ?: "",
                        country = chosen.mediaMetadata.albumTitle?.toString() ?: "NL"
                    )
                serviceScope.launch {
                    playerManager.playStation(matchedStation)
                }
            }

            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(resolvedItems, startIndex, startPositionMs)
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        private fun createFolderItem(id: String, title: String, subtitle: String): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
        }

        private fun RadioStation.toMediaItem(): MediaItem {
            // Generate or fetch local content:// uri so Android Auto displays icons with 100% reliability
            val iconContentUri = StationIconProvider.getIconUri(
                context = this@RadioMediaService,
                stationUuid = stationUuid,
                stationName = name,
                remoteUrl = favicon
            )

            return MediaItem.Builder()
                .setMediaId(stationUuid)
                .setUri(streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(name)
                        .setArtist(genreDisplay)
                        .setAlbumTitle(country)
                        .setArtworkUri(iconContentUri)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                        .build()
                )
                .build()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaLibrarySession?.run {
            // Note: Do NOT release the shared playerManager.exoPlayer here, as that would invalidate
            // the singleton player if Android Auto disconnects and reconnects while app is still in memory.
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }
}
