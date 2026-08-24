package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos

class LocationTracker(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _accuracy = MutableStateFlow<Float?>(null)
    val accuracy: StateFlow<Float?> = _accuracy.asStateFlow()

    private val _bearing = MutableStateFlow<Float?>(null)
    val bearing: StateFlow<Float?> = _bearing.asStateFlow()

    private val _isSimulated = MutableStateFlow(false)
    val isSimulated: StateFlow<Boolean> = _isSimulated.asStateFlow()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startTracking(isSimulation: Boolean = false) {
        _isSimulated.value = isSimulation
        _isTracking.value = true

        if (isSimulation) {
            // Set initial simulated location if null
            if (_currentLocation.value == null) {
                val simLoc = Location("SimulationProvider").apply {
                    latitude = 19.432608  // Default reference coordinate
                    longitude = -99.133209
                    accuracy = 1.5f
                    bearing = 0f
                    time = System.currentTimeMillis()
                }
                _currentLocation.value = simLoc
                _accuracy.value = 1.5f
                _bearing.value = 0f
            }
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // 1 second interval
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setMinUpdateDistanceMeters(0.5f) // 0.5 meter threshold
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                    _accuracy.value = location.accuracy
                    if (location.hasBearing()) {
                        _bearing.value = location.bearing
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
            // Also fetch immediate last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && _currentLocation.value == null) {
                    _currentLocation.value = loc
                    _accuracy.value = loc.accuracy
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stopTracking() {
        _isTracking.value = false
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        locationCallback = null
    }

    /**
     * Moves simulated coordinate by dxMeters (East-West) and dyMeters (North-South).
     */
    fun simulateMove(dxMeters: Double, dyMeters: Double) {
        val current = _currentLocation.value ?: Location("SimulationProvider").apply {
            latitude = 19.432608
            longitude = -99.133209
            accuracy = 1.2f
            bearing = 0f
            time = System.currentTimeMillis()
        }

        val latRad = Math.toRadians(current.latitude)
        val metersPerDegreeLat = 111132.954
        val metersPerDegreeLon = 111412.84 * cos(latRad)

        val newLat = current.latitude + (dyMeters / metersPerDegreeLat)
        val newLon = current.longitude + (dxMeters / metersPerDegreeLon)

        // Calculate heading angle
        val heading = (Math.toDegrees(kotlin.math.atan2(dxMeters, dyMeters)) + 360.0) % 360.0

        val newLoc = Location("SimulationProvider").apply {
            latitude = newLat
            longitude = newLon
            accuracy = 1.0f
            bearing = heading.toFloat()
            time = System.currentTimeMillis()
        }

        _currentLocation.value = newLoc
        _bearing.value = heading.toFloat()
        _accuracy.value = 1.0f
    }

    fun setLocationManually(lat: Double, lon: Double) {
        val newLoc = Location("ManualProvider").apply {
            latitude = lat
            longitude = lon
            accuracy = 1.0f
            time = System.currentTimeMillis()
        }
        _currentLocation.value = newLoc
    }
}
