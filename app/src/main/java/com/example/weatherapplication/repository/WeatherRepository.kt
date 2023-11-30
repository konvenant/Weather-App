package com.example.weatherapplication.repository

import com.example.weatherapplication.api.RetrofitInstance

class WeatherRepository {
  suspend fun getWeather(latitude: Double, longitude: Double,unit:String, apiKey:String) =
        RetrofitInstance.api.getWeather(latitude,longitude,unit,apiKey)
}