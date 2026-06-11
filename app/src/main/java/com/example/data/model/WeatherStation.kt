package com.example.data.model

data class WeatherStation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val tideState: String, // 漲潮, 滿潮, 退潮, 乾潮
    val tideHeight: Double, // meters
    val waveHeight: Double, // meters
    val waveDirection: String,
    val windSpeed: Double, // km/h
    val windDirection: String,
    val waterTemperature: Double, // °C
    val airTemperature: Double, // °C
    val weatherStatus: String, // 晴朗, 陰天, 多雲, 局部陣雨
    val safetyStatus: String, // 安全, 注意 (風大浪高), 危險 (警報管制)
    val fishingSuitability: Int // Score from 0 to 100
)

data class FishingSpot(
    val name: String,
    val description: String,
    val locationName: String,
    val targetFish: String,
    val safetyRatable: String, // "安全", "中度", "危險"
    val majorBiteTimes: String, // e.g., "05:00 - 07:00, 17:30 - 19:30"
    val latitude: Double,
    val longitude: Double,
    val stars: Float,
    val imageDescription: String
)
