package com.example.model

enum class WeatherCondition {
    SUNNY,              // Zonnetje
    PARTLY_CLOUDY,      // Wolkje met zonnetje er iets achter
    CLOUDY,             // Bewolkt
    RAINY,              // Wolkje met regen
    THUNDERSTORM,       // Onweer
    SNOWY               // Sneeuw
}

data class WeatherInfo(
    val temperatureCelsius: Int,
    val condition: WeatherCondition,
    val conditionDescription: String,
    val isDay: Boolean = true
) {
    companion object {
        fun fromWmoCode(code: Int?, temp: Double?, isDayTime: Boolean = true): WeatherInfo {
            val roundedTemp = temp?.let { kotlin.math.round(it).toInt() } ?: 18
            val c = code ?: 0

            val condition = when (c) {
                0, 1 -> WeatherCondition.SUNNY
                2 -> WeatherCondition.PARTLY_CLOUDY
                3, 45, 48 -> WeatherCondition.CLOUDY
                51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherCondition.RAINY
                95, 96, 99 -> WeatherCondition.THUNDERSTORM
                71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOWY
                else -> WeatherCondition.PARTLY_CLOUDY
            }

            val desc = when (condition) {
                WeatherCondition.SUNNY -> if (isDayTime) "Zonnig" else "Helder"
                WeatherCondition.PARTLY_CLOUDY -> "Licht bewolkt"
                WeatherCondition.CLOUDY -> "Bewolkt"
                WeatherCondition.RAINY -> "Regen"
                WeatherCondition.THUNDERSTORM -> "Onweer"
                WeatherCondition.SNOWY -> "Sneeuw"
            }

            return WeatherInfo(
                temperatureCelsius = roundedTemp,
                condition = condition,
                conditionDescription = desc,
                isDay = isDayTime
            )
        }
    }
}
