package com.example.ui.home

import com.example.R

fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "Cer senin"
        1 -> "Mai mult senin"
        2 -> "Parțial senin"
        3 -> "Cer acoperit"
        45, 48 -> "Ceață"
        51, 53, 55 -> "Burniță"
        56, 57 -> "Burniță înghețată"
        61 -> "Ploaie slabă"
        63 -> "Ploaie moderată"
        65 -> "Ploaie puternică"
        66, 67 -> "Ploaie înghețată"
        71 -> "Ninsoare slabă"
        73 -> "Ninsoare moderată"
        75 -> "Ninsoare puternică"
        77 -> "Grindină măruntă"
        80, 81, 82 -> "Averse de ploaie"
        85, 86 -> "Averse de ninsoare"
        95 -> "Furtună"
        96, 99 -> "Furtună cu grindină"
        else -> "Necunoscut"
    }
}

// In a real app we'd map these to nice Lottie files or standard Android icons.
// I'll provide an extension function that maps to Material Icons.
// But returning strings for now is fine since we can map to compose Icons or emojis.
fun getWeatherIconEmoji(code: Int): String {
    return when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "❓"
    }
}
