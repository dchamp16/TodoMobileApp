package com.example.todomobileapp

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<Weather>
) {
    data class Main(
        @SerializedName("temp") val temp: Double
    )

    data class Weather(
        @SerializedName("description") val description: String
    )
}
