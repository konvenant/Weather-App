package com.example.weatherapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapplication.R
import com.example.weatherapplication.repository.WeatherRepository
import com.example.weatherapplication.ui.viewmodel.WeatherViewModel
import com.example.weatherapplication.ui.viewmodel.WeatherViewModelProvider
import com.example.weatherapplication.util.Constants.Companion.API_KEY
import com.example.weatherapplication.util.Constants.Companion.UNIT
import com.example.weatherapplication.util.Resource
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: WeatherViewModel
    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    var lat by Delegates.notNull<Double>()
    var long by Delegates.notNull<Double>()

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val view = findViewById<RelativeLayout>(R.id.mainContainer)
        val weatherRepository = WeatherRepository()
        val viewModelProviderFactory = WeatherViewModelProvider(application, weatherRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(WeatherViewModel::class.java)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()

        viewModel.weather.observe(this ,Observer { response ->
            when(response) {
                is Resource.Success -> {
                    hideProgressBar()
                    findViewById<TextView>(R.id.errorText).visibility = View.GONE
                    response.data?.let {weather ->

                        val main = weather.main
                        val sys = weather.sys
                        val wind = weather.wind
                        val weathers = weather.weather

                        val updatedAt:Long = weather.dt.toLong()
                        val updatedAtText = "Updated at: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(
                            Date(updatedAt*1000)
                        )
                        val temp = ""+main.temp + "°C"
                        val tempMin = "Min Temp: " + main.temp_min+"°C"
                        val tempMax = "Max Temp: " + main.temp_max+"°C"
                        val pressure = main.pressure
                        val humidity = main.humidity

                        val sunrise:Long = sys.sunrise.toLong()
                        val sunset:Long = sys.sunset.toLong()
                        val windSpeed = wind.speed
                        val weatherDescription = weather.weather[0].description
                        val about = weather.weather[0].main
                        val feels = "Feels like: "+weather.main.feels_like
                        val address = weather.name+", "+sys.country

                        /* Populating extracted data into our views */
                        findViewById<TextView>(R.id.address).text = address
                        findViewById<TextView>(R.id.updatedAt).text =  updatedAtText
                        findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                        findViewById<TextView>(R.id.temp).text = temp
                        findViewById<TextView>(R.id.temp_min).text = tempMin
                        findViewById<TextView>(R.id.temp_max).text = tempMax
                        findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise*1000))
                        findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset*1000))
                        findViewById<TextView>(R.id.wind).text = windSpeed.toString()
                        findViewById<TextView>(R.id.pressure).text = pressure.toString()
                        findViewById<TextView>(R.id.humidity).text = humidity.toString()
                        findViewById<TextView>(R.id.about).text = about
                        findViewById<TextView>(R.id.feels_like).text = feels
                    }



                    /* Views populated, Hiding the loader, Showing the main design */
                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE

                }
                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        findViewById<TextView>(R.id.errorText).visibility = View.VISIBLE
                        findViewById<TextView>(R.id.errorText).text = message
                        Snackbar.make(this,view,"An error occurred: $message",Snackbar.LENGTH_LONG).setAction("Try again") {
                            viewModel.getWeather(lat, long,UNIT,API_KEY)
                        }
                            .show()
                    }
                    findViewById<TextView>(R.id.errorText).setOnClickListener {
                        viewModel.getWeather(lat, long,UNIT,API_KEY)
                    }
                }
                is Resource.Loading -> {
                    showProgressBar()
                }

            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                      val latitude = location.latitude
                        val longitude = location.longitude
                        lat = latitude
                        long = longitude
                        viewModel.getWeather(latitude, longitude,UNIT,API_KEY)
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location? = locationResult.lastLocation
           val latitude = mLastLocation!!.latitude
            val longitude = mLastLocation.longitude

            lat = latitude
            long = longitude
            viewModel.getWeather(latitude, longitude,UNIT,API_KEY)
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    private fun hideProgressBar() {
        findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
    }
    private fun showProgressBar() {
        findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
        findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
        findViewById<TextView>(R.id.errorText).visibility = View.GONE
    }
}