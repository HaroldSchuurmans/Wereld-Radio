package com.example.service

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.ParcelFileDescriptor
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.MessageDigest

/**
 * Serves locally cached station icons to Android Auto and system UI via content:// URIs.
 * This guarantees Android Auto can load artwork without remote network security blocks.
 */
class StationIconProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.aistudio.autoradio.kxrv.icons"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

        private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        fun getIconUri(context: Context, stationUuid: String, stationName: String, remoteUrl: String?): Uri {
            val key = generateKey(stationUuid)
            val file = File(context.cacheDir, "auto_icons/$key.png")

            // If not cached yet, trigger background download or generate fallback bitmap
            if (!file.exists()) {
                cacheScope.launch {
                    cacheIcon(context, key, stationName, remoteUrl)
                }
            }

            return Uri.parse("content://$AUTHORITY/$key.png")
        }

        fun preloadIcon(context: Context, stationUuid: String, stationName: String, remoteUrl: String?) {
            val key = generateKey(stationUuid)
            val file = File(context.cacheDir, "auto_icons/$key.png")
            if (!file.exists()) {
                cacheScope.launch {
                    cacheIcon(context, key, stationName, remoteUrl)
                }
            }
        }

        private suspend fun cacheIcon(context: Context, key: String, stationName: String, remoteUrl: String?) {
            withContext(Dispatchers.IO) {
                try {
                    val dir = File(context.cacheDir, "auto_icons")
                    if (!dir.exists()) dir.mkdirs()
                    val targetFile = File(dir, "$key.png")
                    if (targetFile.exists() && targetFile.length() > 0) return@withContext

                    var bitmap: Bitmap? = null

                    // Attempt downloading via Coil if URL is valid
                    if (!remoteUrl.isNullOrBlank()) {
                        val sanitized = when {
                            remoteUrl.contains("upload.wikimedia.org") && remoteUrl.contains("/thumb/") -> null
                            remoteUrl.startsWith("http://") -> remoteUrl.replaceFirst("http://", "https://")
                            else -> remoteUrl
                        }

                        if (!sanitized.isNullOrBlank()) {
                            try {
                                val imageLoader = coil.Coil.imageLoader(context)
                                val request = ImageRequest.Builder(context)
                                    .data(sanitized)
                                    .allowHardware(false)
                                    .size(256, 256)
                                    .build()
                                val result = imageLoader.execute(request)
                                if (result is SuccessResult) {
                                    val drawable = result.drawable
                                    bitmap = Bitmap.createBitmap(
                                        256, 256,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = Canvas(bitmap)
                                    drawable.setBounds(0, 0, 256, 256)
                                    drawable.draw(canvas)
                                }
                            } catch (_: Exception) {
                                bitmap = null
                            }
                        }
                    }

                    // Fallback to high quality generated badge
                    if (bitmap == null) {
                        bitmap = generateFallbackBitmap(stationName)
                    }

                    // Save to local cache file
                    FileOutputStream(targetFile).use { out: OutputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                } catch (_: Exception) {
                }
            }
        }

        private fun generateFallbackBitmap(stationName: String): Bitmap {
            val size = 256
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val cleanName = stationName.trim()
            val hash = kotlin.math.abs(cleanName.hashCode())

            val colors = listOf(
                Pair(0xFF1DB954.toInt(), 0xFF0D5325.toInt()), // Green
                Pair(0xFF2E77D0.toInt(), 0xFF103366.toInt()), // Blue
                Pair(0xFFE91429.toInt(), 0xFF6B0913.toInt()), // Red
                Pair(0xFFE8630A.toInt(), 0xFF752F02.toInt()), // Orange
                Pair(0xFF7B2CBF.toInt(), 0xFF3C096C.toInt()), // Purple
                Pair(0xFF009688.toInt(), 0xFF004D40.toInt())  // Teal
            )
            val (c1, c2) = colors[hash % colors.size]

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = c1
            }
            canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 32f, 32f, paint)

            val shortLabel = when {
                cleanName.startsWith("NPO", ignoreCase = true) -> cleanName.substringAfter("NPO").trim().take(3).ifBlank { "NPO" }
                cleanName.contains("538") -> "538"
                cleanName.contains("3FM", ignoreCase = true) -> "3FM"
                cleanName.contains("10", ignoreCase = true) -> "R10"
                cleanName.contains("JOE", ignoreCase = true) -> "JOE"
                cleanName.contains("SLAM", ignoreCase = true) -> "SLAM"
                else -> cleanName.take(2).uppercase()
            }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = if (shortLabel.length > 2) 72f else 96f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val textY = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(shortLabel, size / 2f, textY, textPaint)

            return bitmap
        }

        private fun generateKey(input: String): String {
            return try {
                val md = MessageDigest.getInstance("MD5")
                val bytes = md.digest(input.toByteArray())
                bytes.joinToString("") { "%02x".format(it) }
            } catch (_: Exception) {
                input.replace(Regex("[^a-zA-Z0-9]"), "_")
            }
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null

    override fun getType(uri: Uri): String = "image/png"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val fileName = uri.lastPathSegment ?: return null
        val context = context ?: return null
        val file = File(context.cacheDir, "auto_icons/$fileName")

        // If file doesn't exist yet, generate standard placeholder immediately so Android Auto never hangs
        if (!file.exists()) {
            val dir = File(context.cacheDir, "auto_icons")
            if (!dir.exists()) dir.mkdirs()
            val placeholder = generateFallbackBitmap("Radio")
            FileOutputStream(file).use { out ->
                placeholder.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
        }

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
