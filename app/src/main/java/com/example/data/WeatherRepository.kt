package com.example.data

class WeatherRepository {
    private val weatherApi = NetworkModule.weatherApi
    private val geocodingApi = NetworkModule.geocodingApi

    suspend fun getWeatherWithCoordinates(
        city: GeocodingResult,
        model: String
    ): Result<Pair<GeocodingResult, WeatherResponse>> {
        return try {
            val queryModel = when (model) {
                "open_meteo" -> null
                else -> model
            }
            val forecastDays = when (model) {
                "icon_seamless" -> 2
                "icon_eu" -> 5
                "ecmwf_ifs025" -> 10
                "gfs_global" -> 6
                "gfs_seamless" -> 14
                else -> null
            }
            val weather = weatherApi.getWeather(
                latitude = city.latitude,
                longitude = city.longitude,
                models = queryModel,
                forecastDays = forecastDays
            )
            Result.success(Pair(city, weather))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCityAndWeather(cityName: String, model: String): Result<Pair<GeocodingResult, WeatherResponse>> {
        return try {
            val geoResponse = geocodingApi.searchCity(cityName)
            val first = geoResponse.results?.firstOrNull()
                ?: return Result.failure(Exception("Locația nu a fost găsită (Nicio potrivire)"))
            
            getWeatherWithCoordinates(first, model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
