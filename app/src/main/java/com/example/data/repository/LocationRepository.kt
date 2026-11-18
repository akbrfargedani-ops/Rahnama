package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.domain.model.GeoPointModel
import com.example.domain.model.LocationState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocationRepository(private val context: Context) : SensorEventListener {

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var isTracking = false
    private var privacyEnabled = true
    private var lastGpsBearing: Float? = null

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { handleLocation(it) }
        }
    }

    private val fallbackListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleLocation(location)
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun refreshPermissionState() {
        val perm = hasLocationPermission()
        _locationState.update { it.copy(hasPermission = perm) }
        if (perm && privacyEnabled) {
            startTracking()
        } else {
            stopTracking()
        }
    }

    fun setPrivacyLocationEnabled(enabled: Boolean) {
        privacyEnabled = enabled
        if (!enabled) {
            stopTracking()
            _locationState.update {
                it.copy(
                    isGpsAvailable = false,
                    currentPoint = null,
                    speedMetersPerSec = 0f,
                    accuracyMeters = null
                )
            }
        } else if (hasLocationPermission()) {
            startTracking()
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!hasLocationPermission() || !privacyEnabled) return
        if (isTracking) return
        isTracking = true

        val gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        _locationState.update { it.copy(hasPermission = true, isGpsAvailable = gpsEnabled) }

        // Last known location first for instant fix
        runCatching {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && privacyEnabled) handleLocation(loc)
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
            .setMinUpdateIntervalMillis(1200L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        runCatching {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        }.onFailure {
            // Fallback to framework LocationManager on devices without Play Services
            runCatching {
                if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2500L,
                        3f,
                        fallbackListener,
                        Looper.getMainLooper()
                    )
                }
                if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        4000L,
                        5f,
                        fallbackListener,
                        Looper.getMainLooper()
                    )
                }
            }
        }

        // Start compass sensor for smooth heading when stationary
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        runCatching { fusedClient.removeLocationUpdates(fusedCallback) }
        runCatching { locationManager?.removeUpdates(fallbackListener) }
        runCatching { sensorManager?.unregisterListener(this) }
    }

    private fun handleLocation(location: Location) {
        if (!privacyEnabled) return
        if (location.hasBearing() && location.speed > 1.2f) {
            lastGpsBearing = location.bearing
        }
        _locationState.update { state ->
            state.copy(
                hasPermission = true,
                isGpsAvailable = true,
                currentPoint = GeoPointModel(location.latitude, location.longitude),
                speedMetersPerSec = if (location.hasSpeed()) location.speed else 0f,
                headingDegrees = lastGpsBearing ?: state.headingDegrees,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else null
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthDeg = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360f) % 360f
            // Only update from compass when not moving fast with GPS bearing
            if (_locationState.value.speedMetersPerSec < 1.2f) {
                _locationState.update { it.copy(headingDegrees = azimuthDeg) }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
