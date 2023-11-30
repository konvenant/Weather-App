package com.example.weatherapplication.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_ETHERNET
import android.net.ConnectivityManager.TYPE_MOBILE
import android.net.ConnectivityManager.TYPE_WIFI
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapplication.WeatherApplication
import com.example.weatherapplication.models.Weather
import com.example.weatherapplication.repository.WeatherRepository
import com.example.weatherapplication.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class WeatherViewModel(
    app: Application,
    val weatherRepository: WeatherRepository
) : AndroidViewModel(app){

    val weather: MutableLiveData<Resource<Weather>> = MutableLiveData()
    var weatherResponse: Weather? = null

    @SuppressLint("SuspiciousIndentation")
    fun getWeather(
        latitude: Double, longitude: Double,unit:String, apiKey:String
    ){
        viewModelScope.launch {
            weather.postValue(Resource.Loading())
            try {
               if (hasInternetConnection()){
                   val response = weatherRepository.getWeather(latitude, longitude, unit, apiKey)
                     weather.postValue(handleWeatherResponse(response))
               } else{
                weather.postValue(Resource.Error("No internet connection"))
               }
            } catch (t: Throwable){
                when(t) {
                    is IOException -> weather.postValue(Resource.Error("Network Failure"))

                    else -> weather.postValue(Resource.Error("Conversion Error $t"))
                }
            }

        }
    }

    private fun handleWeatherResponse(response: Response<Weather>): Resource<Weather> {
        if (response.isSuccessful){
           response.body()?.let {
              weatherResponse = it
               return Resource.Success(weatherResponse ?: it)
           }
        }
        return Resource.Error(response.message())
    }

    private fun hasInternetConnection(): Boolean {

        val connectivityManager = getApplication<WeatherApplication>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return when {
                capabilities.hasTransport(TRANSPORT_WIFI) -> true
                capabilities.hasTransport(TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.activeNetworkInfo?.run {
                return when(type) {
                    TYPE_WIFI -> true
                    TYPE_MOBILE -> true
                    TYPE_ETHERNET -> true
                    else -> false
                }
            }
        }
        return false
    }
}