package com.example.weatherapplication.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapplication.repository.WeatherRepository

class WeatherViewModelProvider(
    val app : Application,
    val weatherRepository: WeatherRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WeatherViewModel(app,weatherRepository) as T
    }
}