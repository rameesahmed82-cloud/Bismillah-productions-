package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Place(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String, // "gas_station", "hotel", "mosque", "custom"
    val description: String,
    val rating: Float,
    val userRatingCount: Int,
    val phone: String = "",
    val website: String = "",
    val iconName: String = "",
    val estimatedDistance: Double = 0.0
)

data class NavStep(
    val instruction: String,
    val distanceMeters: Double,
    val icon: String // "straight", "left", "right", "arrive"
)
