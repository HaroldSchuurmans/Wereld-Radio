package com.example.model

/**
 * Domain model representing a global radio station.
 */
data class RadioStation(
    val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val favicon: String = "",
    val homepage: String = "",
    val country: String = "",
    val countryCode: String = "",
    val tags: String = "",
    val codec: String = "MP3",
    val bitrate: Int = 128,
    val votes: Int = 0,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L
) {
    val genreDisplay: String
        get() {
            if (tags.isBlank()) return "Diverse / Pop"
            return tags.split(",")
                .take(3)
                .joinToString(" • ") { it.trim().replaceFirstChar { char -> char.uppercase() } }
        }

    val displayBitrate: String
        get() = if (bitrate > 0) "$bitrate kbps" else "HQ Stream"

    val displayCountryFlag: String
        get() {
            val code = countryCode.trim().uppercase()
            if (code.length != 2) return "📻"
            val firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
}

/**
 * Real-time RDS (Radio Data System) broadcast info parsed from the live audio stream.
 */
data class RdsInfo(
    val stationName: String = "",
    val trackTitle: String = "",
    val artist: String = "",
    val fullTitle: String = "",
    val genre: String = "",
    val bitrate: Int = 128,
    val codec: String = "MP3",
    val isLive: Boolean = false,
    val signalStrengthPercent: Int = 99
) {
    val displayTrack: String
        get() = when {
            fullTitle.isNotBlank() -> fullTitle
            trackTitle.isNotBlank() && artist.isNotBlank() -> "$artist - $trackTitle"
            trackTitle.isNotBlank() -> trackTitle
            stationName.isNotBlank() -> "Live uitzending • $stationName"
            else -> "Live Radio Stream"
        }
}
