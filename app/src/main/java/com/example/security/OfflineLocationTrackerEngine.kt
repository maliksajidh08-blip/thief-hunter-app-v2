package com.example.security

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.LocationHistoryEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Robust Offline Location Tracker Engine:
 * - Records location every 30 seconds without draining battery.
 * - Stores all points locally in Room database for offline persistence.
 * - Automatically dispatches silent SMS ("THIEF HUNTER: [lat], [lng]") when:
 *     1. No Internet connection
 *     2. Device marked stolen
 *     3. Device is frozen / locked down
 * - Works across all cellular networks (GSM, LTE, 5G) without internet.
 */
class OfflineLocationTrackerEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        const val TAG = "OfflineLocationTracker"
        const val TRACKING_INTERVAL_BALANCED_MS = 600_000L // 10 minutes (Battery Saver)
        const val TRACKING_INTERVAL_FAST_MS = 30_000L      // 30 seconds (Performance or Stolen)
        const val TRACKING_INTERVAL_STOLEN_MS = 15_000L    // 15 seconds (Stolen emergency)
    }

    private val db = AppDatabase.getDatabase(context)
    private val locationDao = db.locationHistoryDao()
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    val batteryOptimizer = BatteryOptimizer.getInstance(context)

    private var trackingJob: Job? = null
    private var locationCallback: LocationCallback? = null
    private var fallbackListener: LocationListener? = null

    private val _isTrackingActive = MutableStateFlow(true)
    val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

    private val _isDeviceFrozen = MutableStateFlow(false)
    val isDeviceFrozen: StateFlow<Boolean> = _isDeviceFrozen.asStateFlow()

    private val _isDeviceStolen = MutableStateFlow(false)
    val isDeviceStolen: StateFlow<Boolean> = _isDeviceStolen.asStateFlow()

    private val _lastFix = MutableStateFlow<LocationHistoryEntity?>(null)
    val lastFix: StateFlow<LocationHistoryEntity?> = _lastFix.asStateFlow()

    private val _lastSmsDispatchedTime = MutableStateFlow<Long?>(null)
    val lastSmsDispatchedTime: StateFlow<Long?> = _lastSmsDispatchedTime.asStateFlow()

    // Default emergency contact phone (can be updated via Settings)
    var emergencyPhoneNumber: String = "+92 300 4589211"

    init {
        // Load last known location from database on startup
        scope.launch {
            val last = locationDao.getLastKnownLocationOnce()
            if (last != null) {
                _lastFix.value = last
            }
        }
        startTracking()
    }

    fun setEmergencyPhone(phone: String) {
        if (phone.isNotBlank()) {
            emergencyPhoneNumber = phone.trim()
        }
    }

    fun setDeviceFrozen(frozen: Boolean) {
        _isDeviceFrozen.value = frozen
        if (frozen) {
            // Immediately force a silent SMS dispatch with current or fresh location
            scope.launch {
                captureAndProcessLocation(triggerReason = "FROZEN_DEVICE", forceSms = true)
            }
        }
    }

    fun setDeviceStolen(stolen: Boolean) {
        _isDeviceStolen.value = stolen
        if (stolen) {
            refreshTrackingConfig()
            scope.launch {
                captureAndProcessLocation(triggerReason = "STOLEN_ALARM", forceSms = true)
            }
        } else {
            refreshTrackingConfig()
        }
    }

    fun refreshTrackingConfig() {
        stopTracking()
        startTracking()
    }

    fun hasLocationPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!hasLocationPermission()) {
            Log.i(TAG, "Location permissions not yet granted; tracking will resume when granted.")
            return
        }
        if (trackingJob?.isActive == true) return

        val isStolen = _isDeviceStolen.value || _isDeviceFrozen.value
        val mode = batteryOptimizer.currentMode.value

        // In Ultra Low mode: ONLY GPS WHEN STOLEN or FROZEN
        if (mode == com.example.data.BatteryMode.ULTRA_LOW && !isStolen) {
            _isTrackingActive.value = false
            Log.i(TAG, "Ultra Low Power Mode: GPS tracking paused to save battery (activates on theft only).")
            return
        }

        _isTrackingActive.value = true

        val intervalMs = when {
            isStolen -> TRACKING_INTERVAL_STOLEN_MS
            mode == com.example.data.BatteryMode.BALANCED -> TRACKING_INTERVAL_BALANCED_MS // 10 minutes
            else -> TRACKING_INTERVAL_FAST_MS // 30 seconds
        }

        val priority = if (isStolen) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        // 1. Configure Fused Location with Balanced Power (No battery drain)
        try {
            val request = LocationRequest.Builder(priority, intervalMs)
                .setMinUpdateIntervalMillis((intervalMs / 2).coerceAtLeast(5000L))
                .setMaxUpdateDelayMillis(intervalMs * 2)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        scope.launch {
                            handleNewLocation(loc, triggerReason = if (isStolen) "STOLEN_TRACKING" else "PERIODIC_GPS")
                        }
                    }
                }
            }

            fusedClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Fused location unavailable: ${e.message}")
        }

        // 2. Hardware LocationManager fallback for offline GPS chips
        try {
            fallbackListener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    scope.launch {
                        handleNewLocation(loc, triggerReason = "GPS_HARDWARE")
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            locationManager?.let { lm ->
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        intervalMs,
                        10f,
                        fallbackListener!!,
                        Looper.getMainLooper()
                    )
                } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        intervalMs,
                        15f,
                        fallbackListener!!,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "LocationManager fallback error: ${e.message}")
        }

        // 3. Periodic Coroutine check (only active when stolen or performance mode)
        if (isStolen || mode == com.example.data.BatteryMode.PERFORMANCE) {
            trackingJob = scope.launch {
                while (isActive) {
                    delay(intervalMs)
                    captureAndProcessLocation(triggerReason = "PERIODIC_CHECK", forceSms = false)
                }
            }
        }
    }

    fun stopTracking() {
        _isTrackingActive.value = false
        trackingJob?.cancel()
        trackingJob = null

        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
        }
        fallbackListener?.let {
            locationManager?.removeUpdates(it)
            fallbackListener = null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun captureAndProcessLocation(triggerReason: String, forceSms: Boolean = false) {
        // Use WakeLock briefly to guarantee execution during deep freeze / sleep
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ThiefHunter:LocationFix")
        wakeLock?.acquire(3000L)

        try {
            // Attempt Fused Location lastLocation first
            var location: Location? = null
            try {
                location = fusedClient.lastLocation.result
            } catch (_: Exception) {}

            // Fallback to LocationManager if Fused is null
            if (location == null && locationManager != null) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } catch (_: Exception) {}
            }

            if (location != null) {
                handleNewLocation(location, triggerReason = triggerReason, forceSms = forceSms)
            } else {
                // If hardware sensor has no instant fix, retrieve last saved database location or default
                val lastKnown = _lastFix.value ?: locationDao.getLastKnownLocationOnce()
                if (lastKnown != null) {
                    evaluateAndDispatchSms(
                        lat = lastKnown.latitude,
                        lng = lastKnown.longitude,
                        triggerReason = triggerReason,
                        forceSms = forceSms
                    )
                }
            }
        } finally {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
        }
    }

    private suspend fun handleNewLocation(
        loc: Location,
        triggerReason: String,
        forceSms: Boolean = false
    ) {
        val lat = loc.latitude
        val lng = loc.longitude
        val accuracy = loc.accuracy
        val now = System.currentTimeMillis()
        val timeFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))
        val isOffline = !isInternetConnected()

        // 1. Check if SMS should be sent:
        // Conditions: No internet OR Device marked stolen OR Device frozen OR Force trigger
        val shouldSendSms = forceSms ||
                _isDeviceFrozen.value ||
                _isDeviceStolen.value ||
                isOffline

        var smsSent = false
        if (shouldSendSms) {
            smsSent = sendSilentSms(lat, lng)
        }

        // 2. Save locally in Room Database (Guaranteed offline persistence)
        val entity = LocationHistoryEntity(
            latitude = lat,
            longitude = lng,
            accuracy = accuracy,
            timestamp = now,
            formattedTime = timeFormatted,
            triggerSource = triggerReason,
            isSentViaSms = smsSent,
            smsRecipient = if (smsSent) emergencyPhoneNumber else null,
            isOffline = isOffline,
            googleMapsUrl = "https://maps.google.com/?q=$lat,$lng"
        )

        locationDao.insertLocation(entity)
        _lastFix.value = entity
    }

    private fun evaluateAndDispatchSms(
        lat: Double,
        lng: Double,
        triggerReason: String,
        forceSms: Boolean
    ) {
        val isOffline = !isInternetConnected()
        val shouldSend = forceSms || _isDeviceFrozen.value || _isDeviceStolen.value || isOffline
        if (shouldSend) {
            sendSilentSms(lat, lng)
        }
    }

    /**
     * Silent SMS (no notification):
     * Format: "THIEF HUNTER: [lat], [lng]"
     * Works on all cellular networks without internet.
     */
    fun sendSilentSms(lat: Double, lng: Double): Boolean {
        return try {
            val latStr = String.format(Locale.US, "%.6f", lat)
            val lngStr = String.format(Locale.US, "%.6f", lng)
            // Required Format: "THIEF HUNTER: [lat], [lng]"
            val message = "THIEF HUNTER: $latStr, $lngStr\nhttps://maps.google.com/?q=$latStr,$lngStr"

            SmsAlertHelper.sendSms(
                context = context,
                phoneNumber = emergencyPhoneNumber,
                message = message,
                onSuccess = {
                    Log.d(TAG, "Silent location SMS sent successfully to $emergencyPhoneNumber")
                    _lastSmsDispatchedTime.value = System.currentTimeMillis()
                },
                onError = { err ->
                    Log.e(TAG, "Silent SMS dispatch failed: $err")
                }
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering silent SMS", e)
            false
        }
    }

    fun isInternetConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun clearHistory() {
        locationDao.clearHistory()
    }
}
