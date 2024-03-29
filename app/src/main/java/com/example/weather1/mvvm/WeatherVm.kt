package com.example.weather1.mvvm

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather1.MainActivity
import com.example.weather1.SplashScreen
import com.example.weather1.service.RetrofitInstance
import com.example.weather1.WeatherList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class WeatherVm : ViewModel() {

    val todayWeatherLiveData = MutableLiveData<List<WeatherList>>()
    val forecastWeatherLiveData = MutableLiveData<List<WeatherList>>()

    val closetorexactlysameweatherdata = MutableLiveData<WeatherList?>()
    val cityName = MutableLiveData<String?>()

    fun   getWeather(city: String? = null, lati: String?=null, longi:String?=null) = viewModelScope.launch(Dispatchers.IO) {
        val todayWeatherList = mutableListOf<WeatherList>()
        val currentDateTime = LocalDateTime.now()
        val currentDateO = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        Log.d("Get Weather","$lati $longi")

        val call = if (city != null) {
            Log.d("Get Weather city not null","$city")
            RetrofitInstance.api.getWeatherByCity(city)
        } else {
            Log.d("Get Weather city null","")
            RetrofitInstance.api.getCurrentWeather(lati!!, longi!!)
        }

        val response = call.execute()

        if (response.isSuccessful) {
            val weatherList = response.body()?.weatherList
            Log.d("Response Successful",response.body().toString())
            cityName.postValue(response.body()?.city!!.name)
            val currentDate = currentDateO

            weatherList?.forEach { weather ->
                if (weather.dtTxt!!.split("\\s".toRegex()).contains(currentDate)) {
                    todayWeatherList.add(weather)
                }
            }

            val closestWeather = findClosestWeather(todayWeatherList)
            closetorexactlysameweatherdata.postValue(closestWeather)
            todayWeatherLiveData.postValue(todayWeatherList)
        } else {
            val errorMessage = response.message()
            Log.e("CurrentWeatherError", "Error: $errorMessage")
        }
    }

    fun getForecastUpcoming(city: String? = null, lati:String? = null, longi:String? = null) = viewModelScope.launch(Dispatchers.IO) {
        val forecastWeatherList = mutableListOf<WeatherList>()
        val currentDateTime = LocalDateTime.now()
        val currentDateO = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val call = if (city != null) {
            RetrofitInstance.api.getWeatherByCity(city)
        } else {
            RetrofitInstance.api.getCurrentWeather(lati!!, longi!!)
        }
        val response = call.execute()
        if (response.isSuccessful) {
            val weatherList = response.body()?.weatherList
            val currentDate = currentDateO
            weatherList?.forEach { weather ->
                if (!weather.dtTxt!!.split("\\s".toRegex()).contains(currentDate)) {
                    if (weather.dtTxt!!.substring(11, 16) == "12:00") {
                        forecastWeatherList.add(weather)
                    }
                }
            }
            forecastWeatherLiveData.postValue(forecastWeatherList)
            Log.d("Forecast LiveData", forecastWeatherLiveData.value.toString())
        } else {
            val errorMessage = response.message()
            Log.e("CurrentWeatherError", "Error: $errorMessage")
        }
    }

    private fun findClosestWeather(weatherList: List<WeatherList>): WeatherList? {
        val systemTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        var closestWeather: WeatherList? = null
        var minTimeDifference = Int.MAX_VALUE
        for (weather in weatherList) {
            val weatherTime = weather.dtTxt!!.substring(11, 16)
            val timeDifference = Math.abs(timeToMinutes(weatherTime) - timeToMinutes(systemTime))
            if (timeDifference < minTimeDifference) {
                minTimeDifference = timeDifference
                closestWeather = weather
            }
        }
        return closestWeather
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

}