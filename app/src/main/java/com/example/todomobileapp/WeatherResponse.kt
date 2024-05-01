package com.example.todomobileapp

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("name") val name: String,
    @SerializedName("sys") val sys: SystemInfo?
) {
    data class Main(
        @SerializedName("temp") val temp: Double
    )

    data class Weather(
        @SerializedName("description") val description: String
    )

    data class SystemInfo(
        @SerializedName("country") val country: String
    )
}
