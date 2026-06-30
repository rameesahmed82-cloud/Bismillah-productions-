package com.example.data.repository

import com.example.data.api.GeminiPlaceService
import com.example.data.db.BookmarkedPlace
import com.example.data.db.LocationDao
import com.example.data.db.UserRating
import com.example.data.model.Place
import com.example.data.model.NavStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.*

class PlaceRepository(private val locationDao: LocationDao) {

    // Fetch user reviews from database
    val allUserRatings: Flow<List<UserRating>> = locationDao.getAllUserRatings()
    val allBookmarks: Flow<List<BookmarkedPlace>> = locationDao.getAllBookmarks()

    fun getRatingsForPlace(placeId: String): Flow<List<UserRating>> =
        locationDao.getRatingsForPlace(placeId)

    fun isBookmarked(placeId: String): Flow<Boolean> =
        locationDao.isBookmarked(placeId)

    suspend fun addBookmark(place: Place) {
        val bookmark = BookmarkedPlace(
            placeId = place.id,
            name = place.name,
            address = place.address,
            latitude = place.latitude,
            longitude = place.longitude,
            category = place.category,
            description = place.description
        )
        locationDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(placeId: String) {
        locationDao.deleteBookmark(placeId)
    }

    suspend fun submitRating(placeId: String, placeName: String, rating: Float, comment: String, userName: String) {
        val userRating = UserRating(
            placeId = placeId,
            placeName = placeName,
            rating = rating,
            comment = comment,
            userName = userName
        )
        locationDao.insertUserRating(userRating)
    }

    // High accuracy spherical distance using Haversine formula
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c // distance in meters
    }

    // Computes compass bearing in degrees (0 to 360)
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) -
                sin(phi1) * cos(phi2) * cos(deltaLambda)
        var theta = atan2(y, x)
        theta = Math.toDegrees(theta)
        return (theta + 360) % 360
    }

    fun getCompassDirection(bearingDegrees: Double): String {
        return when (bearingDegrees) {
            in 337.5..360.0, in 0.0..22.5 -> "North"
            in 22.5..67.5 -> "North-East"
            in 67.5..112.5 -> "East"
            in 112.5..157.5 -> "South-East"
            in 157.5..202.5 -> "South"
            in 202.5..247.5 -> "South-West"
            in 247.5..292.5 -> "West"
            else -> "North-West"
        }
    }

    // Generate turn-by-turn visual guide steps based on starting coordinates and target coordinates
    fun generateTurnByTurnGuide(startLat: Double, startLng: Double, destLat: Double, destLng: Double): List<NavStep> {
        val totalDistance = calculateDistance(startLat, startLng, destLat, destLng)
        val initialBearing = calculateBearing(startLat, startLng, destLat, destLng)
        val directionStr = getCompassDirection(initialBearing)

        if (totalDistance < 30.0) {
            return listOf(NavStep("You have arrived at your destination!", 0.0, "arrive"))
        }

        val steps = mutableListOf<NavStep>()
        
        // Let's create realistic dynamic turn-by-turn guidance based on real geometry!
        steps.add(NavStep("Head $directionStr on the main road", totalDistance * 0.35, "straight"))
        
        val intermediateBearing = (initialBearing + 45) % 360
        val intermediateDirection = getCompassDirection(intermediateBearing)
        steps.add(NavStep("Turn slightly right toward $intermediateDirection and continue", totalDistance * 0.40, "right"))
        
        steps.add(NavStep("Turn left onto destination street", totalDistance * 0.20, "left"))
        steps.add(NavStep("Arrive at your destination on your right", totalDistance * 0.05, "arrive"))

        return steps
    }

    // Dynamic search that integrates local Room ratings on top of the search results!
    suspend fun getNearbyPlaces(
        latitude: Double,
        longitude: Double,
        category: String,
        query: String = ""
    ): List<Place> {
        val fetchedPlaces = GeminiPlaceService.findPlacesNear(latitude, longitude, category, query)

        // Read all local user ratings to see if we should override/inject newly added local ratings and recalculate the average!
        val localRatings = locationDao.getAllUserRatings().first()

        return fetchedPlaces.map { place ->
            val placeLocalReviews = localRatings.filter { it.placeId == place.id }
            if (placeLocalReviews.isNotEmpty()) {
                val sumLocal = placeLocalReviews.sumOf { it.rating.toDouble() }
                val totalRatings = place.userRatingCount + placeLocalReviews.size
                val weightedAverage = ((place.rating * place.userRatingCount) + sumLocal) / totalRatings
                place.copy(
                    rating = ((weightedAverage * 10).roundToInt() / 10.0).toFloat(),
                    userRatingCount = totalRatings
                )
            } else {
                place
            }
        }.sortedBy { place ->
            calculateDistance(latitude, longitude, place.latitude, place.longitude)
        }
    }
}
