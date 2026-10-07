package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface RadioBrowserApiService {

    @GET("json/stations/topclick")
    suspend fun getTopClickStations(
        @Query("limit") limit: Int = 40,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<StationDto>

    @GET("json/stations/topvote")
    suspend fun getTopVotedStations(
        @Query("limit") limit: Int = 40,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<StationDto>

    @GET("json/stations/bycountrycodeexact/{countryCode}")
    suspend fun getStationsByCountry(
        @Path("countryCode") countryCode: String,
        @Query("limit") limit: Int = 40,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<StationDto>

    @GET("json/stations/bytag/{tag}")
    suspend fun getStationsByTag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int = 40,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<StationDto>

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("countrycode") countryCode: String? = null,
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hidebroken: Boolean = true
    ): List<StationDto>

    companion object {
        private const val BASE_URL = "https://de1.api.radio-browser.info/"

        fun create(): RadioBrowserApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "AutoRadioAndroid/1.0")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(RadioBrowserApiService::class.java)
        }
    }
}
