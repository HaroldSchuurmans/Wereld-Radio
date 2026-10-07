package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "current") val current: CurrentWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature_2m") val temperature: Double?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "is_day") val isDay: Int?
)

interface OpenMeteoApiService {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double = 52.1326, // Central Netherlands (Utrecht)
        @Query("longitude") longitude: Double = 5.2913,
        @Query("current") current: String = "temperature_2m,weather_code,is_day",
        @Query("timezone") timezone: String = "auto"
    ): WeatherResponse

    companion object {
        fun create(): OpenMeteoApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(OpenMeteoApiService::class.java)
        }
    }
}
