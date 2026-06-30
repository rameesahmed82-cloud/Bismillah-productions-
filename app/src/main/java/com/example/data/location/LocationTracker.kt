package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float, // in meters
    val altitude: Double, // in meters
    val speed: Float, // in meters/second
    val timestamp: Long = System.currentTimeMillis()
)

class LocationTracker(private val context: Context) {
    private val TAG = "LocationTracker"
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow<LocationData?>(null)
    val locationState: StateFlow<LocationData?> = _locationState.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private var locationCallback: LocationCallback? = null

    // Default starting fallback location: London, UK (or easily customizable by simulator)
    init {
        // Set a nice default start coordinates so that the user doesn't see empty screens before GPS starts
        _locationState.value = LocationData(
            latitude = 51.5074,
            longitude = -0.1278,
            accuracy = 10f,
            altitude = 15.0,
            speed = 0f
        )
    }

    @SuppressLint("MissingPermission")
    fun startTracking(intervalMs: Long = 2000L) {
        if (_isTracking.value) return

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .setWaitForAccurateLocation(true)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        altitude = location.altitude,
                        speed = location.speed,
                        timestamp = location.time
                    )
                    _locationState.value = locationData
                    Log.d(TAG, "New GPS location: Lat=${location.latitude}, Lng=${location.longitude}, Acc=${location.accuracy}m")
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            _isTracking.value = true
            Log.i(TAG, "GPS location updates started successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GPS tracking: ${e.message}", e)
        }
    }

    fun stopTracking() {
        if (!_isTracking.value) return

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        _isTracking.value = false
        Log.i(TAG, "GPS location updates stopped.")
    }

    // Allows manual injection of coordinates (perfect for simulation or testing inside emulators)
    fun simulateLocation(latitude: Double, longitude: Double, accuracy: Float = 3f, speed: Float = 5f) {
        _locationState.value = LocationData(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            altitude = 25.0,
            speed = speed
        )
    }
}
