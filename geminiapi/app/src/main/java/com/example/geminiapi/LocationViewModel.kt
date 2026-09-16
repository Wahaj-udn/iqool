package com.example.geminiapi

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.*
import org.maplibre.android.geometry.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationState(
    val location: Location? = null,
    val pathPoints: List<LatLng> = emptyList(),
    val isTracking: Boolean = false,
    val error: String? = null
)

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation ?: return
            
            _locationState.value = _locationState.value.copy(location = lastLocation)

            // Logic for smooth path recording
            if (_locationState.value.isTracking) {
                // Filter 1: Accuracy (Ignore "jumpy" points)
                if (lastLocation.accuracy > 20) return

                val currentPoints = _locationState.value.pathPoints
                val newPoint = LatLng(lastLocation.latitude, lastLocation.longitude)

                if (currentPoints.isEmpty()) {
                    _locationState.value = _locationState.value.copy(
                        pathPoints = listOf(newPoint)
                    )
                } else {
                    val lastPoint = currentPoints.last()
                    val distance = FloatArray(1)
                    Location.distanceBetween(
                        lastPoint.latitude, lastPoint.longitude,
                        newPoint.latitude, newPoint.longitude,
                        distance
                    )

                    // Filter 2: Distance (Only record if moved > 2 meters to avoid "cluster" at stops)
                    if (distance[0] > 2.0) {
                        _locationState.value = _locationState.value.copy(
                            pathPoints = currentPoints + newPoint
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startRunning() {
        // High frequency request for "Smooth" tracking (every 1 second)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L) // Allow faster updates if available
            .setMaxUpdateDelayMillis(1000L) // Ensure UI updates smoothly
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            _locationState.value = _locationState.value.copy(
                isTracking = true, 
                pathPoints = emptyList(), // Clear previous path for a new run
                error = null
            )
        } catch (e: Exception) {
            _locationState.value = _locationState.value.copy(error = e.localizedMessage)
        }
    }

    fun stopRunning() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _locationState.value = _locationState.value.copy(isTracking = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopRunning()
    }
}
