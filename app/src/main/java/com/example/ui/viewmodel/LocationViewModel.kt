package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.BookmarkedPlace
import com.example.data.db.UserRating
import com.example.data.location.LocationTracker
import com.example.data.location.LocationData
import com.example.data.model.Place
import com.example.data.model.NavStep
import com.example.data.repository.PlaceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.*

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "LocationViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val repository = PlaceRepository(database.locationDao())
    val locationTracker = LocationTracker(application)

    // Observe active GPS states
    val gpsLocation: StateFlow<LocationData?> = locationTracker.locationState
    val isGpsTracking: StateFlow<Boolean> = locationTracker.isTracking

    // Query states
    private val _selectedCategory = MutableStateFlow("gas_station")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Places list UI state
    private val _placesState = MutableStateFlow<UiState<List<Place>>>(UiState.Idle)
    val placesState: StateFlow<UiState<List<Place>>> = _placesState.asStateFlow()

    // Selected place details
    private val _selectedPlace = MutableStateFlow<Place?>(null)
    val selectedPlace: StateFlow<Place?> = _selectedPlace.asStateFlow()

    // Selected place local reviews
    val selectedPlaceReviews: Flow<List<UserRating>> = _selectedPlace.flatMapLatest { place ->
        place?.let { repository.getRatingsForPlace(it.id) } ?: flowOf(emptyList())
    }

    // Selected place bookmarked state
    val isSelectedPlaceBookmarked: Flow<Boolean> = _selectedPlace.flatMapLatest { place ->
        place?.let { repository.isBookmarked(it.id) } ?: flowOf(false)
    }

    // Bookmarks and general user reviews
    val bookmarkedPlaces: StateFlow<List<BookmarkedPlace>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSubmittedReviews: StateFlow<List<UserRating>> = repository.allUserRatings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- NAVIGATION STATES ---
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _navDestination = MutableStateFlow<Place?>(null)
    val navDestination: StateFlow<Place?> = _navDestination.asStateFlow()

    private val _navSteps = MutableStateFlow<List<NavStep>>(emptyList())
    val navSteps: StateFlow<List<NavStep>> = _navSteps.asStateFlow()

    private val _navDistanceRemaining = MutableStateFlow(0.0) // meters
    val navDistanceRemaining: StateFlow<Double> = _navDistanceRemaining.asStateFlow()

    private val _navBearing = MutableStateFlow(0.0) // degrees
    val navBearing: StateFlow<Double> = _navBearing.asStateFlow()

    private val _navEtaMinutes = MutableStateFlow(0)
    val navEtaMinutes: StateFlow<Int> = _navEtaMinutes.asStateFlow()

    private val _navSpeed = MutableStateFlow(0.0) // km/h
    val navSpeed: StateFlow<Double> = _navSpeed.asStateFlow()

    private val _isSimulationActive = MutableStateFlow(false)
    val isSimulationActive: StateFlow<Boolean> = _isSimulationActive.asStateFlow()

    private var simulationJob: Job? = null

    init {
        // Start tracking user location right away if possible
        locationTracker.startTracking()

        // Observe location changes to recalculate active navigation distances in real-time!
        viewModelScope.launch {
            gpsLocation.collect { location ->
                location?.let { loc ->
                    recalculateNavMetrics(loc)
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _searchQuery.value = ""
        searchNearby()
    }

    fun performCustomSearch(query: String) {
        _searchQuery.value = query
        _selectedCategory.value = "custom"
        searchNearby()
    }

    fun searchNearby() {
        val currentLoc = gpsLocation.value
        if (currentLoc == null) {
            _placesState.value = UiState.Error("Waiting for 100% accurate GPS signal...")
            return
        }

        _placesState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val results = repository.getNearbyPlaces(
                    latitude = currentLoc.latitude,
                    longitude = currentLoc.longitude,
                    category = selectedCategory.value,
                    query = searchQuery.value
                )
                // Append distance details to each place dynamically relative to user's real GPS coordinates
                val placesWithDistance = results.map { place ->
                    val distance = repository.calculateDistance(
                        currentLoc.latitude, currentLoc.longitude,
                        place.latitude, place.longitude
                    )
                    place.copy(estimatedDistance = distance)
                }.sortedBy { it.estimatedDistance }

                _placesState.value = UiState.Success(placesWithDistance)
            } catch (e: Exception) {
                Log.e(TAG, "Search failed: ${e.message}", e)
                _placesState.value = UiState.Error(e.message ?: "An unexpected error occurred during analysis.")
            }
        }
    }

    fun setPlaceDetail(place: Place) {
        _selectedPlace.value = place
    }

    fun clearPlaceDetail() {
        _selectedPlace.value = null
    }

    fun toggleBookmark(place: Place) {
        viewModelScope.launch {
            val bookmarked = repository.isBookmarked(place.id).first()
            if (bookmarked) {
                repository.removeBookmark(place.id)
            } else {
                repository.addBookmark(place)
            }
        }
    }

    fun submitRating(place: Place, rating: Float, comment: String, userName: String) {
        viewModelScope.launch {
            repository.submitRating(
                placeId = place.id,
                placeName = place.name,
                rating = rating,
                comment = comment,
                userName = userName
            )
            // Re-fetch places to update local ratings averages immediately!
            searchNearby()
            
            // Refresh local selected place to update detail view instantly!
            _selectedPlace.value?.let { current ->
                val reviews = repository.getRatingsForPlace(current.id).first()
                val sumLocal = reviews.sumOf { it.rating.toDouble() }
                val totalRatings = current.userRatingCount + 1
                val weightedAverage = ((current.rating * current.userRatingCount) + sumLocal) / totalRatings
                _selectedPlace.value = current.copy(
                    rating = ((weightedAverage * 10).toInt() / 10.0).toFloat()
                )
            }
        }
    }

    // --- NAVIGATION LOGIC ---
    fun startNavigation(place: Place) {
        stopNavigation() // reset previous
        _navDestination.value = place
        _isNavigating.value = true

        val loc = gpsLocation.value
        if (loc != null) {
            _navSteps.value = repository.generateTurnByTurnGuide(
                startLat = loc.latitude, startLng = loc.longitude,
                destLat = place.latitude, destLng = place.longitude
            )
            recalculateNavMetrics(loc)
        }
    }

    fun stopNavigation() {
        stopSimulation()
        _isNavigating.value = false
        _navDestination.value = null
        _navSteps.value = emptyList()
        _navDistanceRemaining.value = 0.0
        _navBearing.value = 0.0
        _navEtaMinutes.value = 0
        _navSpeed.value = 0.0
    }

    private fun recalculateNavMetrics(loc: LocationData) {
        val dest = navDestination.value ?: return
        
        // Calculate 100% accurate distance using Haversine formula
        val distMeters = repository.calculateDistance(
            loc.latitude, loc.longitude,
            dest.latitude, dest.longitude
        )
        _navDistanceRemaining.value = distMeters

        // Calculate bearing direction
        val bearing = repository.calculateBearing(
            loc.latitude, loc.longitude,
            dest.latitude, dest.longitude
        )
        _navBearing.value = bearing

        // Determine current speed (either from GPS hardware speed or estimated simulated speed)
        val speedMps = if (isSimulationActive.value) {
            _navSpeed.value // keep simulator speed
        } else {
            val s = loc.speed.toDouble() * 3.6 // convert m/s to km/h
            _navSpeed.value = if (s > 0.5) s else 0.0
            loc.speed.toDouble()
        }

        // Calculate ETA: assume walking speed (~1.4 m/s) if speed is zero, otherwise actual speed
        val currentSpeedMps = if (speedMps > 0.1) speedMps / 3.6 else 1.4
        val etaSeconds = distMeters / currentSpeedMps
        _navEtaMinutes.value = (etaSeconds / 60.0).toInt().coerceAtLeast(1)

        // Regenerate steps guide to reflect the latest updated starting coordinate
        _navSteps.value = repository.generateTurnByTurnGuide(
            startLat = loc.latitude, startLng = loc.longitude,
            destLat = dest.latitude, destLng = dest.longitude
        )
    }

    // --- NAVIGATION AUTO-SIMULATION ENGINE ---
    fun toggleSimulation() {
        if (isSimulationActive.value) {
            stopSimulation()
        } else {
            startSimulation()
        }
    }

    private fun startSimulation() {
        val dest = navDestination.value ?: return
        val loc = gpsLocation.value ?: return

        _isSimulationActive.value = true
        _navSpeed.value = 35.0 // Simulate driving speed: 35 km/h

        val speedMps = 35.0 / 3.6 // ~9.7 meters per second
        var currentLat = loc.latitude
        var currentLng = loc.longitude

        simulationJob = viewModelScope.launch {
            while (isSimulationActive.value) {
                delay(1000) // Update every 1 second

                val remainingDist = repository.calculateDistance(currentLat, currentLng, dest.latitude, dest.longitude)
                if (remainingDist <= 15.0) {
                    // Arrived!
                    locationTracker.simulateLocation(dest.latitude, dest.longitude, accuracy = 1f, speed = 0f)
                    stopSimulation()
                    break
                }

                // Interpolate coordinate step towards the destination
                val bearingRad = Math.toRadians(repository.calculateBearing(currentLat, currentLng, dest.latitude, dest.longitude))
                
                // Earth's radius in meters
                val earthRadius = 6371000.0
                // Angular distance
                val angularDist = speedMps / earthRadius

                val lat1 = Math.toRadians(currentLat)
                val lon1 = Math.toRadians(currentLng)

                val lat2 = asin(sin(lat1) * cos(angularDist) + cos(lat1) * sin(angularDist) * cos(bearingRad))
                val lon2 = lon1 + atan2(sin(bearingRad) * sin(angularDist) * cos(lat1), cos(angularDist) - sin(lat1) * sin(lat2))

                currentLat = Math.toDegrees(lat2)
                currentLng = Math.toDegrees(lon2)

                // Inject simulated GPS location
                locationTracker.simulateLocation(
                    latitude = currentLat,
                    longitude = currentLng,
                    accuracy = 2.5f,
                    speed = speedMps.toFloat()
                )
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _isSimulationActive.value = false
        _navSpeed.value = 0.0
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopTracking()
        stopSimulation()
    }
}
