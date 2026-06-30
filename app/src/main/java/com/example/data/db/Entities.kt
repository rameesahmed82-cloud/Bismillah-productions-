package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_ratings")
data class UserRating(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val placeId: String,
    val placeName: String,
    val rating: Float, // 1.0 to 5.0
    val comment: String,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarked_places")
data class BookmarkedPlace(
    @PrimaryKey val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val description: String,
    val savedAt: Long = System.currentTimeMillis()
)
