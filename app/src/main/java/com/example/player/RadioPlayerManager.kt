package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.metadata.icy.IcyHeaders
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import com.example.model.RadioStation
import com.example.model.RdsInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

@OptIn(UnstableApi::class)
class RadioPlayerManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // High compatibility HTTP DataSource: supports redirects, Icecast, Shoutcast, HLS & standard ICY headers
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("AutoRadio/1.0 (Android; Linux; ExoPlayer)")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(20000)
        .setKeepPostFor302Redirects(true)

    // Extractors configured for internet radio formats (MP3, AAC, OGG/Opus, TS/HLS, FLAC)
    private val extractorsFactory = DefaultExtractorsFactory()
        .setConstantBitrateSeekingEnabled(true)
        .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)

    private val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory, extractorsFactory)

    // Optimized buffering for live mobile / car radio (quick initial start + generous max buffer)
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 3000,
            /* maxBufferMs = */ 30000,
            /* bufferForPlaybackMs = */ 1500,
            /* bufferForPlaybackAfterRebufferMs = */ 3000
        )
        .build()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(
        appContext,
        DefaultRenderersFactory(appContext).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    )
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _playbackStatus = MutableStateFlow(PlaybackStatus.IDLE)
    val playbackStatus: StateFlow<PlaybackStatus> = _playbackStatus.asStateFlow()

    private val _rdsInfo = MutableStateFlow(RdsInfo())
    val rdsInfo: StateFlow<RdsInfo> = _rdsInfo.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _playlist = MutableStateFlow<List<RadioStation>>(emptyList())
    val playlist: StateFlow<List<RadioStation>> = _playlist.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var previousVolume: Float = 1.0f
    private var retryAttempt = 0

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> _playbackStatus.value = PlaybackStatus.BUFFERING
                    Player.STATE_READY -> {
                        retryAttempt = 0
                        _playbackStatus.value = if (exoPlayer.playWhenReady) {
                            PlaybackStatus.PLAYING
                        } else {
                            PlaybackStatus.PAUSED
                        }
                    }
                    Player.STATE_ENDED -> _playbackStatus.value = PlaybackStatus.IDLE
                    Player.STATE_IDLE -> {
                        if (_playbackStatus.value != PlaybackStatus.ERROR) {
                            _playbackStatus.value = PlaybackStatus.IDLE
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _playbackStatus.value = PlaybackStatus.PLAYING
                    _errorMessage.value = null
                } else if (_playbackStatus.value != PlaybackStatus.BUFFERING && _playbackStatus.value != PlaybackStatus.ERROR) {
                    _playbackStatus.value = PlaybackStatus.PAUSED
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val current = _currentStation.value
                if (current != null && retryAttempt < 1) {
                    retryAttempt++
                    scope.launch {
                        delay(1200)
                        // If it failed on https or http redirect, retry with alternate scheme if applicable
                        val altUrl = when {
                            current.streamUrl.startsWith("https://") -> current.streamUrl.replaceFirst("https://", "http://")
                            current.streamUrl.startsWith("http://") -> current.streamUrl.replaceFirst("http://", "https://")
                            else -> current.streamUrl
                        }
                        tryPlayStream(altUrl, current)
                    }
                } else {
                    _playbackStatus.value = PlaybackStatus.ERROR
                    _errorMessage.value = "Kon live audio stream niet laden. Probeer een andere zender."
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                parseMediaMetadata(mediaMetadata)
            }

            override fun onMetadata(metadata: Metadata) {
                parseIcyMetadata(metadata)
            }
        })
    }

    fun setPlaylist(stations: List<RadioStation>) {
        _playlist.value = stations
    }

    fun playStation(station: RadioStation, newPlaylist: List<RadioStation>? = null) {
        if (newPlaylist != null) {
            _playlist.value = newPlaylist
        } else if (!_playlist.value.any { it.stationUuid == station.stationUuid }) {
            _playlist.value = _playlist.value + station
        }

        _currentStation.value = station
        _errorMessage.value = null
        _playbackStatus.value = PlaybackStatus.BUFFERING
        retryAttempt = 0

        // Initialize base RDS info from station
        _rdsInfo.value = RdsInfo(
            stationName = station.name,
            genre = station.genreDisplay,
            bitrate = station.bitrate,
            codec = station.codec,
            isLive = true
        )

        tryPlayStream(station.streamUrl, station)
    }

    private fun tryPlayStream(streamUrl: String, station: RadioStation) {
        try {
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(station.name)
                .setArtist("Live Radio • ${station.country}")
                .setArtworkUri(if (station.favicon.isNotBlank()) Uri.parse(station.favicon) else null)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(station.stationUuid)
                .setUri(streamUrl)
                .setMediaMetadata(mediaMetadata)
                .build()

            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            _playbackStatus.value = PlaybackStatus.ERROR
            _errorMessage.value = "Fout bij starten van stream: ${e.localizedMessage}"
        }
    }

    fun updateCurrentStationFavorite(isFavorite: Boolean) {
        val current = _currentStation.value ?: return
        _currentStation.value = current.copy(isFavorite = isFavorite)
        _playlist.value = _playlist.value.map {
            if (it.stationUuid == current.stationUuid) it.copy(isFavorite = isFavorite) else it
        }
    }

    fun togglePlayPause() {
        if (_playbackStatus.value == PlaybackStatus.PLAYING) {
            exoPlayer.pause()
            _playbackStatus.value = PlaybackStatus.PAUSED
        } else if (_playbackStatus.value == PlaybackStatus.PAUSED) {
            exoPlayer.play()
            _playbackStatus.value = PlaybackStatus.PLAYING
        } else {
            _currentStation.value?.let { playStation(it) }
        }
    }

    fun nextStation() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val current = _currentStation.value ?: return
        val currentIndex = list.indexOfFirst { it.stationUuid == current.stationUuid }
        val nextIndex = if (currentIndex != -1 && currentIndex < list.size - 1) {
            currentIndex + 1
        } else {
            0
        }
        playStation(list[nextIndex])
    }

    fun previousStation() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val current = _currentStation.value ?: return
        val currentIndex = list.indexOfFirst { it.stationUuid == current.stationUuid }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            list.size - 1
        }
        playStation(list[prevIndex])
    }

    fun toggleMute() {
        if (_isMuted.value) {
            exoPlayer.volume = previousVolume.coerceAtLeast(0.2f)
            _isMuted.value = false
        } else {
            previousVolume = exoPlayer.volume
            exoPlayer.volume = 0f
            _isMuted.value = true
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerSeconds.value = null
            return
        }

        val totalSeconds = minutes * 60
        _sleepTimerSeconds.value = totalSeconds

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerSeconds.value = remaining
            }
            if (isActive) {
                exoPlayer.pause()
                _playbackStatus.value = PlaybackStatus.PAUSED
                _sleepTimerSeconds.value = null
            }
        }
    }

    private fun parseMediaMetadata(mediaMetadata: MediaMetadata) {
        val title = mediaMetadata.title?.toString()?.trim() ?: ""
        val artist = mediaMetadata.artist?.toString()?.trim() ?: ""

        if (title.isNotBlank() || artist.isNotBlank()) {
            val current = _rdsInfo.value
            val full = if (artist.isNotBlank() && title.isNotBlank()) "$artist - $title" else title.ifBlank { artist }
            _rdsInfo.value = current.copy(
                trackTitle = title,
                artist = artist,
                fullTitle = full
            )
            updateLiveRdsMediaMetadata(full, artist, title)
        }
    }

    private fun parseIcyMetadata(metadata: Metadata) {
        for (i in 0 until metadata.length()) {
            when (val entry = metadata.get(i)) {
                is IcyInfo -> {
                    val streamTitle = entry.title?.trim() ?: ""
                    if (streamTitle.isNotBlank()) {
                        val parts = streamTitle.split(" - ", limit = 2)
                        val artist = if (parts.size > 1) parts[0].trim() else ""
                        val title = if (parts.size > 1) parts[1].trim() else parts[0].trim()

                        val current = _rdsInfo.value
                        _rdsInfo.value = current.copy(
                            fullTitle = streamTitle,
                            artist = artist,
                            trackTitle = title,
                            isLive = true
                        )
                        updateLiveRdsMediaMetadata(streamTitle, artist, title)
                    }
                }
                is IcyHeaders -> {
                    val current = _rdsInfo.value
                    _rdsInfo.value = current.copy(
                        bitrate = if (entry.bitrate > 0) entry.bitrate else current.bitrate,
                        genre = entry.genre?.trim()?.ifBlank { current.genre } ?: current.genre,
                        stationName = entry.name?.trim()?.ifBlank { current.stationName } ?: current.stationName
                    )
                }
            }
        }
    }

    private fun updateLiveRdsMediaMetadata(streamTitle: String, artist: String, trackTitle: String) {
        val current = _currentStation.value ?: return
        try {
            // Note: In ExoPlayer for live streams, calling replaceMediaItem during playback triggers 
            // a full stream re-buffer / stall. We update the UI StateFlow (_rdsInfo) directly, which
            // immediately reflects in Compose UI and MediaNotification without interrupting audio.
        } catch (_: Exception) {
        }
    }

    fun release() {
        sleepTimerJob?.cancel()
        exoPlayer.release()
    }

    companion object {
        @Volatile
        private var INSTANCE: RadioPlayerManager? = null

        fun getInstance(context: Context): RadioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RadioPlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
