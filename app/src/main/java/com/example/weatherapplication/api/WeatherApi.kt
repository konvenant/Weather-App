package com.example.weatherapplication.api

import com.example.weatherapplication.models.Weather
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

        @GET("data/2.5/weather")
        suspend fun getWeather(
            @Query("lat") lat: Double,
            @Query("lon") lon: Double,
            @Query("units") units: String,
            @Query("appid") apiKey: String
        ): Response<Weather>

}