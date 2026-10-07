package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.local.AppDatabase
import com.example.data.remote.RadioBrowserApiService
import com.example.data.repository.RadioRepository
import com.example.player.RadioPlayerManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AutoRadioApplication : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: RadioRepository
        private set

    lateinit var playerManager: RadioPlayerManager
        private set

    lateinit var weatherApiService: com.example.data.remote.OpenMeteoApiService
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = RadioRepository(
            apiService = RadioBrowserApiService.create(),
            favoriteDao = database.favoriteStationDao()
        )
        playerManager = RadioPlayerManager.getInstance(this)
        weatherApiService = com.example.data.remote.OpenMeteoApiService.create()
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 AutoRadio/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
