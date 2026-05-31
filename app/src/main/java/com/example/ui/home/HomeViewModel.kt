package com.example.ui.home

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeocodingResult
import com.example.data.NetworkModule
import com.example.data.WeatherRepository
import com.example.data.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val city: GeocodingResult, val weather: WeatherResponse, val currentModel: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository()
    private val prefs = application.getSharedPreferences("meteo_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow(prefs.getString("last_city", "București") ?: "București")
    val searchQuery = _searchQuery.asStateFlow()

    private val _currentModel = MutableStateFlow(
        prefs.getString("last_model", "icon_eu")?.let { savedModel ->
            val validKeys = listOf("icon_eu", "icon_seamless", "ecmwf_ifs025", "gfs_global", "gfs_seamless", "open_meteo")
            if (savedModel in validKeys) savedModel else "icon_eu"
        } ?: "icon_eu"
    )
    val currentModel = _currentModel.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val availableModels = listOf(
        "icon_seamless" to "ICON-EU Flash",
        "icon_eu" to "ICON-EU",
        "ecmwf_ifs025" to "ECMWF IFS HRES",
        "gfs_global" to "GFS 0.125°",
        "gfs_seamless" to "GFS",
        "open_meteo" to "Open-Meteo"
    )

    private fun saveToOfflineCache(city: GeocodingResult, weather: WeatherResponse, model: String) {
        try {
            val cityJson = NetworkModule.moshi.adapter(GeocodingResult::class.java).toJson(city)
            val weatherJson = NetworkModule.moshi.adapter(WeatherResponse::class.java).toJson(weather)
            prefs.edit()
                .putString("offline_cached_city", cityJson)
                .putString("offline_cached_weather", weatherJson)
                .putString("offline_cached_model", model)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFromOfflineCache(): HomeUiState.Success? {
        val cityJson = prefs.getString("offline_cached_city", null)
        val weatherJson = prefs.getString("offline_cached_weather", null)
        val model = prefs.getString("offline_cached_model", "icon_eu") ?: "icon_eu"
        if (cityJson != null && weatherJson != null) {
            try {
                val city = NetworkModule.moshi.adapter(GeocodingResult::class.java).fromJson(cityJson)
                val weather = NetworkModule.moshi.adapter(WeatherResponse::class.java).fromJson(weatherJson)
                if (city != null && weather != null) {
                    return HomeUiState.Success(city, weather, model)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    init {
        val cached = loadFromOfflineCache()
        if (cached != null) {
            _uiState.value = cached
            _searchQuery.value = cached.city.name
        }
        fetchWeather()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onModelChange(modelId: String) {
        _currentModel.value = modelId
        prefs.edit().putString("last_model", modelId).apply()
        fetchWeather()
    }

    fun fetchWeatherForCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            if (_uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Loading
            }
            _isRefreshing.value = true
            
            val cityNameResult = try {
                val reverseResult = com.example.data.NetworkModule.nominatimApi.reverseGeocode(lat, lon)
                val address = reverseResult.address
                val cityOrTown = address?.get("city") 
                    ?: address?.get("town") 
                    ?: address?.get("village") 
                    ?: address?.get("municipality") 
                    ?: address?.get("county") 
                    ?: "Locație GPS"
                cityOrTown
            } catch (e: Exception) {
                "Locație GPS"
            }
            
            val cityObj = GeocodingResult(
                id = 0,
                name = cityNameResult,
                latitude = lat,
                longitude = lon,
                country = "",
                admin1 = ""
            )
            
            _searchQuery.value = cityNameResult
            prefs.edit().putString("last_city", cityNameResult).apply()
            
            val result = repository.getWeatherWithCoordinates(cityObj, _currentModel.value)
            
            result.onSuccess {
                prefs.edit()
                    .putString("cached_city_name", cityNameResult)
                    .putFloat("cached_city_lat", lat.toFloat())
                    .putFloat("cached_city_lon", lon.toFloat())
                    .putString("cached_city_display_name", cityNameResult)
                    .apply()
                saveToOfflineCache(it.first, it.second, _currentModel.value)
                _uiState.value = HomeUiState.Success(it.first, it.second, _currentModel.value)
                _isRefreshing.value = false
            }.onFailure {
                val cached = loadFromOfflineCache()
                if (cached != null) {
                    _uiState.value = cached
                    _searchQuery.value = cached.city.name
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(
                            getApplication(),
                            "Eroare de conexiune. Se afișează datele salvate offline.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    _uiState.value = HomeUiState.Error(it.message ?: "Unknown error")
                }
                _isRefreshing.value = false
            }
        }
    }

    fun fetchWeather() {
        val query = _searchQuery.value.trim()
        val savedCity = prefs.getString("cached_city_name", "") ?: ""
        val savedLat = prefs.getFloat("cached_city_lat", 0f)
        val savedLon = prefs.getFloat("cached_city_lon", 0f)
        val savedDisplayName = prefs.getString("cached_city_display_name", "") ?: ""

        prefs.edit().putString("last_city", query).apply()

        viewModelScope.launch {
            if (_uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Loading
            }
            _isRefreshing.value = true
            
            val result = if (query.equals(savedCity, ignoreCase = true) && savedLat != 0f && savedLon != 0f) {
                val cityObj = GeocodingResult(
                    id = 0,
                    name = savedDisplayName.ifEmpty { query },
                    latitude = savedLat.toDouble(),
                    longitude = savedLon.toDouble(),
                    country = "",
                    admin1 = ""
                )
                repository.getWeatherWithCoordinates(cityObj, _currentModel.value)
            } else {
                repository.getCityAndWeather(query, _currentModel.value)
            }

            result.onSuccess {
                prefs.edit()
                    .putString("cached_city_name", query)
                    .putFloat("cached_city_lat", it.first.latitude.toFloat())
                    .putFloat("cached_city_lon", it.first.longitude.toFloat())
                    .putString("cached_city_display_name", it.first.name)
                    .apply()
                saveToOfflineCache(it.first, it.second, _currentModel.value)
                _uiState.value = HomeUiState.Success(it.first, it.second, _currentModel.value)
                _isRefreshing.value = false
            }.onFailure {
                val cached = loadFromOfflineCache()
                if (cached != null) {
                    _uiState.value = cached
                    _searchQuery.value = cached.city.name
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(
                            getApplication(),
                            "Eroare de conexiune. Se afișează datele salvate offline.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    _uiState.value = HomeUiState.Error(it.message ?: "Unknown error")
                }
                _isRefreshing.value = false
            }
        }
    }
}
