package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NominatimResult(
    val lat: String,
    val lon: String,
    val display_name: String
)

@JsonClass(generateAdapter = true)
data class NominatimReverseResponse(
    val display_name: String?,
    val address: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    @Json(name = "admin1") val admin1: String?
)

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val hourly: Map<String, Any>,
    val daily: Map<String, Any>
) {
    val hourlyData: HourlyData
        get() = HourlyData(
            time = hourly.getAsStringList("time"),
            temperature2m = hourly.getAsDoubleList("temperature_2m"),
            precipitationProbability = hourly.getAsIntList("precipitation_probability"),
            windSpeed10m = hourly.getAsDoubleList("wind_speed_10m"),
            weatherCode = hourly.getAsIntList("weather_code")
        )

    val dailyData: DailyData
        get() = DailyData(
            time = daily.getAsStringList("time"),
            weatherCode = daily.getAsIntList("weather_code"),
            temperature2mMax = hourly.getAsDoubleList("temperature_2m_max").takeIf { it.isNotEmpty() } ?: daily.getAsDoubleList("temperature_2m_max"),
            temperature2mMin = hourly.getAsDoubleList("temperature_2m_min").takeIf { it.isNotEmpty() } ?: daily.getAsDoubleList("temperature_2m_min"),
            sunrise = daily.getAsStringList("sunrise"),
            sunset = daily.getAsStringList("sunset")
        )
}

data class HourlyData(
    val time: List<String>,
    val temperature2m: List<Double?>,
    val precipitationProbability: List<Int?>,
    val windSpeed10m: List<Double?>,
    val weatherCode: List<Int?>
)

data class DailyData(
    val time: List<String>,
    val weatherCode: List<Int?>,
    val temperature2mMax: List<Double?>,
    val temperature2mMin: List<Double?>,
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList()
)

private fun Map<String, Any>.findKeyFor(keyPart: String): String? {
    val exact = keys.find { it.equals(keyPart, ignoreCase = true) }
    if (exact != null) return exact
    return keys.find { 
        it.startsWith("${keyPart}_", ignoreCase = true) || 
        it.endsWith("_$keyPart", ignoreCase = true) || 
        it.contains("_${keyPart}_", ignoreCase = true)
    }
}

private fun Map<String, Any>.getAsStringList(keyPart: String): List<String> {
    val key = findKeyFor(keyPart) ?: return emptyList()
    val list = this[key] as? List<*> ?: return emptyList()
    return list.map { it?.toString() ?: "" }
}

private fun Map<String, Any>.getAsDoubleList(keyPart: String): List<Double?> {
    val key = findKeyFor(keyPart) ?: return emptyList()
    val list = this[key] as? List<*> ?: return emptyList()
    return list.map {
        when (it) {
            is Number -> it.toDouble()
            is String -> it.toDoubleOrNull()
            else -> null
        }
    }
}

private fun Map<String, Any>.getAsIntList(keyPart: String): List<Int?> {
    val key = findKeyFor(keyPart) ?: return emptyList()
    val list = this[key] as? List<*> ?: return emptyList()
    return list.map {
        when (it) {
            is Number -> it.toInt()
            is String -> it.toIntOrNull()
            else -> null
        }
    }
}
