package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.CommunityAlert
import com.example.data.IntruderCapture
import com.example.data.LiveTrackerState
import com.example.data.Localization
import com.example.data.MobileDevice
import com.example.data.ReportStatus
import com.example.data.Screen
import com.example.data.StolenReport
import com.example.security.AlarmSirenEngine
import com.example.security.CameraCaptureHelper
import com.example.security.GuardConfig
import com.example.security.SecurityNotificationHelper
import com.example.security.SensorSecurityManager
import com.example.security.SensorTelemetry
import com.example.security.SmsAlertHelper
import com.example.security.ThiefGuardService
import com.example.security.TriggerReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiState(
    val currentScreen: Screen = Screen.SPLASH,
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    val guardConfig: GuardConfig = GuardConfig(),
    val sensorTelemetry: SensorTelemetry = SensorTelemetry(),
    val isSystemArmed: Boolean = false,
    val isArmingCountdown: Boolean = false,
    val countdownRemaining: Int = 0,
    val activeBreachTrigger: TriggerReason? = null,
    val isServiceRunning: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val masterPin: String = "9051",
    val enteredPinAttempt: String = "",
    val pinError: Boolean = false,
    val failedPinAttempts: Int = 0,
    val isCameraCapturing: Boolean = false,
    val triggerPhotoCaptureEvent: Long = 0L,
    val lastCapturedPhotoPath: String? = null,
    val intruderCaptures: List<IntruderCapture> = listOf(
        IntruderCapture(
            id = "cap_1",
            timestamp = "Today, 02:15 PM",
            triggerReason = "Wrong PIN entered (3 attempts)",
            photoUri = null,
            wasPinWrong = true
        )
    ),
    val myDevices: List<MobileDevice> = listOf(
        MobileDevice(
            id = "dev_1",
            name = "My Primary Phone",
            model = "Samsung Galaxy S24 Ultra",
            imei = "358941092837461",
            batteryPercent = 88,
            isSecured = true,
            isLocked = false,
            isSirenPlaying = false,
            simCardNumber = "+92 300 4589211",
            lastSeenAddress = "Liberty Market, Lahore",
            lastSeenTime = "Active now",
            isStolen = false,
            stolenTimestamp = null,
            emergencyContactPhone = "+92 300 4589211",
            lastKnownLatitude = 31.5204,
            lastKnownLongitude = 74.3587
        ),
        MobileDevice(
            id = "dev_2",
            name = "Office Work Tablet",
            model = "Google Pixel Tablet",
            imei = "864209051837492",
            batteryPercent = 64,
            isSecured = true,
            isLocked = false,
            isSirenPlaying = false,
            simCardNumber = "+92 321 8892104",
            lastSeenAddress = "Tech Hub, Block H",
            lastSeenTime = "12 mins ago",
            isStolen = false,
            stolenTimestamp = null,
            emergencyContactPhone = "+92 321 8892104",
            lastKnownLatitude = 31.5280,
            lastKnownLongitude = 74.3610
        ),
        MobileDevice(
            id = "dev_3",
            name = "Personal iPhone 15 Pro",
            model = "Apple iPhone 15 Pro Max",
            imei = "354129087654321",
            batteryPercent = 75,
            isSecured = true,
            isLocked = false,
            isSirenPlaying = false,
            simCardNumber = "+92 333 7712345",
            lastSeenAddress = "Gulberg III, Main Blvd",
            lastSeenTime = "25 mins ago",
            isStolen = false,
            stolenTimestamp = null,
            emergencyContactPhone = "+92 333 7712345",
            lastKnownLatitude = 31.5122,
            lastKnownLongitude = 74.3489
        ),
        MobileDevice(
            id = "dev_4",
            name = "Family Backup Mobile",
            model = "Xiaomi Redmi Note 13 Pro",
            imei = "869402061234567",
            batteryPercent = 91,
            isSecured = true,
            isLocked = false,
            isSirenPlaying = false,
            simCardNumber = "+92 345 9901234",
            lastSeenAddress = "Cantt Saddar Bazaar",
            lastSeenTime = "1 hour ago",
            isStolen = false,
            stolenTimestamp = null,
            emergencyContactPhone = "+92 345 9901234",
            lastKnownLatitude = 31.5390,
            lastKnownLongitude = 74.3720
        ),
        MobileDevice(
            id = "dev_5",
            name = "Travel OnePlus 12",
            model = "OnePlus 12 5G Flagship",
            imei = "357891043218765",
            batteryPercent = 43,
            isSecured = true,
            isLocked = false,
            isSirenPlaying = false,
            simCardNumber = "+92 302 1122334",
            lastSeenAddress = "Allama Iqbal Airport Terminal",
            lastSeenTime = "2 hours ago",
            isStolen = false,
            stolenTimestamp = null,
            emergencyContactPhone = "+92 302 1122334",
            lastKnownLatitude = 31.5217,
            lastKnownLongitude = 74.4036
        )
    ),
    val stolenReports: List<StolenReport> = listOf(
        StolenReport(
            id = "rep_1",
            imei = "354129087654321",
            brand = "Apple",
            modelName = "iPhone 15 Pro Max (Titanium Blue)",
            firNumber = "FIR-2026/894-Gulberg",
            reportDate = "2026-09-12",
            theftLocation = "Anarkali Bazaar, Lahore",
            ownerPhone = "+92 333 7712345",
            rewardAmount = "$200",
            status = ReportStatus.ALERT_ACTIVE
        ),
        StolenReport(
            id = "rep_2",
            imei = "869402061234567",
            brand = "Xiaomi",
            modelName = "Redmi Note 13 Pro+",
            firNumber = "FIR-2026/512-Cantt",
            reportDate = "2026-09-10",
            theftLocation = "Mall Road Junction",
            ownerPhone = "+92 345 9901234",
            rewardAmount = "$80",
            status = ReportStatus.INVESTIGATING
        ),
        StolenReport(
            id = "rep_3",
            imei = "357891043218765",
            brand = "OnePlus",
            modelName = "OnePlus 12 5G",
            firNumber = "FIR-2026/301-ModelTown",
            reportDate = "2026-09-08",
            theftLocation = "Metro Bus Terminal",
            ownerPhone = "+92 302 1122334",
            rewardAmount = "$150",
            status = ReportStatus.RECOVERED
        )
    ),
    val communityAlerts: List<CommunityAlert> = listOf(
        CommunityAlert(
            id = "com_1",
            title = "iPhone 15 Snatching Reported",
            description = "Black motorbike spotted near Hafeez Center. Phone serial flagged across all local shops.",
            location = "Gulberg III",
            timeAgo = "18 mins ago",
            imeiMasked = "354129••••••321",
            isUrgent = true
        ),
        CommunityAlert(
            id = "com_2",
            title = "Device Recovered at Repair Market",
            description = "Shopkeeper scanned IMEI on Thief Hunter app and handed device to local precinct.",
            location = "Hall Road Electronics",
            timeAgo = "2 hours ago",
            imeiMasked = "869402••••••567",
            isUrgent = false
        ),
        CommunityAlert(
            id = "com_3",
            title = "Warning: SIM Swap Activity in Area",
            description = "Make sure SIM Lock PIN is turned ON in settings to prevent unauthorized carrier access.",
            location = "Defense Phase 5",
            timeAgo = "Yesterday",
            imeiMasked = "General Advisory",
            isUrgent = false
        )
    ),
    val searchImeiQuery: String = "",
    val searchResultReport: StolenReport? = null,
    val hasSearched: Boolean = false,
    val liveTrackerState: LiveTrackerState = LiveTrackerState(),
    val activeSnackbarMessage: String? = null
)

class ThiefHunterViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Local fallback managers in case foreground service is binding
    private val localSensorManager = SensorSecurityManager(application)
    private val localSirenEngine = AlarmSirenEngine(application)
    private val localNotificationHelper = SecurityNotificationHelper(application)

    private var boundService: ThiefGuardService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? ThiefGuardService.LocalBinder
            boundService = binder?.getService()
            isBound = true
            _uiState.update { it.copy(isServiceRunning = true) }
            observeServiceData()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            isBound = false
            _uiState.update { it.copy(isServiceRunning = false) }
        }
    }

    init {
        // Collect telemetry from local sensor manager
        viewModelScope.launch {
            localSensorManager.telemetry.collect { tele ->
                _uiState.update {
                    it.copy(
                        sensorTelemetry = tele,
                        liveTrackerState = it.liveTrackerState.copy(
                            latitude = tele.latitude,
                            longitude = tele.longitude,
                            accuracyMeters = tele.accuracy,
                            speedKmh = tele.speed
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            localSensorManager.isArmed.collect { armed ->
                _uiState.update { it.copy(isSystemArmed = armed) }
            }
        }

        viewModelScope.launch {
            localSensorManager.isArmingCountdown.collect { countdown ->
                _uiState.update { it.copy(isArmingCountdown = countdown) }
            }
        }

        viewModelScope.launch {
            localSensorManager.countdownRemaining.collect { remaining ->
                _uiState.update { it.copy(countdownRemaining = remaining) }
            }
        }

        viewModelScope.launch {
            localSensorManager.activeTrigger.collect { trigger ->
                _uiState.update {
                    it.copy(
                        activeBreachTrigger = trigger,
                        isSirenPlaying = trigger != null
                    )
                }
            }
        }

        localSensorManager.onTriggerAlarm = { reason ->
            val config = _uiState.value.guardConfig
            val shouldTrigger = when (reason) {
                TriggerReason.POCKET_REMOVAL -> config.pocketDetectionEnabled
                TriggerReason.MOTION_DETECTED -> config.motionDetectionEnabled
                TriggerReason.CHARGER_UNPLUGGED -> config.chargerUnplugEnabled
                TriggerReason.USB_CONNECTED -> config.usbConnectionEnabled
                TriggerReason.INTRUDER_FAILED_PIN -> config.intruderSelfieEnabled
                TriggerReason.MANUAL_PANIC -> true
            }

            if (shouldTrigger) {
                localSirenEngine.startSiren(reason.patternKey)
                localNotificationHelper.showTriggerNotification(
                    reason.title,
                    "Security breach detected: ${reason.title}! Enter PIN to disarm."
                )
                _uiState.update {
                    it.copy(
                        activeBreachTrigger = reason,
                        isSirenPlaying = true
                    )
                }
            } else {
                localSensorManager.clearTrigger()
            }
        }

        // Start Foreground Service automatically
        startForegroundGuardService()
    }

    fun startForegroundGuardService() {
        val app = getApplication<Application>()
        val intent = Intent(app, ThiefGuardService::class.java)
        try {
            app.startService(intent)
            app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (_: Exception) {}
    }

    fun stopForegroundGuardService() {
        val app = getApplication<Application>()
        if (isBound) {
            try {
                app.unbindService(serviceConnection)
            } catch (_: Exception) {}
            isBound = false
        }
        val intent = Intent(app, ThiefGuardService::class.java).apply {
            action = ThiefGuardService.ACTION_STOP_SERVICE
        }
        app.startService(intent)
        _uiState.update { it.copy(isServiceRunning = false) }
        disarmSystem()
    }

    private fun observeServiceData() {
        val service = boundService ?: return
        viewModelScope.launch {
            service.sensorManager.telemetry.collect { tele ->
                _uiState.update { it.copy(sensorTelemetry = tele) }
            }
        }
    }

    fun armSystem() {
        val config = _uiState.value.guardConfig
        localSensorManager.startArmingSequence(config.armingDelaySeconds, config)
        boundService?.sensorManager?.startArmingSequence(config.armingDelaySeconds, config)
        showMessage("Arming countdown started: Keep phone secure!")
    }

    fun disarmSystem() {
        localSensorManager.disarmSystem()
        localSirenEngine.stopSiren()
        localNotificationHelper.cancelTriggerNotification()
        boundService?.disarmAndStopAlarm()
        _uiState.update {
            it.copy(
                isSystemArmed = false,
                isArmingCountdown = false,
                activeBreachTrigger = null,
                isSirenPlaying = false,
                enteredPinAttempt = "",
                pinError = false
            )
        }
        showMessage("Anti-Theft Guard Disarmed.")
    }

    private var stolenTrackingJob: Job? = null

    fun triggerManualBreach(reason: TriggerReason) {
        localSensorManager.fireTrigger(reason)
    }

    fun verifyAndDisarmPin(pin: String): Boolean {
        if (pin == _uiState.value.masterPin) {
            disarmSystem()
            _uiState.update {
                it.copy(
                    enteredPinAttempt = "",
                    pinError = false,
                    failedPinAttempts = 0
                )
            }
            showMessage("Master PIN verified. Guard disarmed.")
            return true
        } else {
            val newAttempts = _uiState.value.failedPinAttempts + 1
            _uiState.update {
                it.copy(
                    pinError = true,
                    failedPinAttempts = newAttempts
                )
            }

            if (newAttempts >= 3) {
                // Request automatic front camera capture via UI event
                _uiState.update {
                    it.copy(
                        triggerPhotoCaptureEvent = System.currentTimeMillis(),
                        isCameraCapturing = true
                    )
                }
                recordIntruderCapture("3rd Wrong PIN attempt: '$pin' - Front Camera Triggered")
                showMessage("3rd Wrong PIN! Taking silent front camera intruder selfie...")
            } else {
                val remaining = 3 - newAttempts
                recordIntruderCapture("Wrong Master PIN entered: $pin (Attempt $newAttempts/3)")
                showMessage("Incorrect PIN! ($newAttempts/3). $remaining attempt(s) until photo capture.")
            }
            return false
        }
    }

    fun onPhotoCaptured(filePath: String, isManualTest: Boolean = false) {
        val reason = if (isManualTest) "Manual Camera Test Snapshot" else "3rd Wrong PIN Intruder Selfie"
        recordIntruderCapture(reason, filePath)
        _uiState.update {
            it.copy(
                isCameraCapturing = false,
                lastCapturedPhotoPath = filePath
            )
        }
        showMessage(if (isManualTest) "Test photo captured and stored in Vault!" else "Intruder photo saved to Vault!")
    }

    fun onPhotoCaptureFailed(errorMsg: String) {
        _uiState.update { it.copy(isCameraCapturing = false) }
        showMessage("Camera capture note: $errorMsg")
    }

    fun recordIntruderCapture(reasonText: String, photoUri: String? = null) {
        val formatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        val newCapture = IntruderCapture(
            id = "cap_${System.currentTimeMillis()}",
            timestamp = formatter.format(Date()),
            triggerReason = reasonText,
            photoUri = photoUri,
            wasPinWrong = true
        )
        _uiState.update {
            it.copy(intruderCaptures = listOf(newCapture) + it.intruderCaptures)
        }
    }

    fun clearIntruderCaptures() {
        _uiState.update {
            it.copy(
                intruderCaptures = emptyList(),
                lastCapturedPhotoPath = null,
                failedPinAttempts = 0
            )
        }
        showMessage("Intruder log cleared.")
    }

    fun updateConfig(update: (GuardConfig) -> GuardConfig) {
        _uiState.update { it.copy(guardConfig = update(it.guardConfig)) }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun tr(key: String): String {
        return Localization.getString(_uiState.value.selectedLanguage, key)
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(activeSnackbarMessage = message) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(activeSnackbarMessage = null) }
    }

    fun updateImeiSearchQuery(query: String) {
        _uiState.update { it.copy(searchImeiQuery = query) }
    }

    fun performImeiCheck() {
        val query = _uiState.value.searchImeiQuery.trim()
        if (query.isEmpty()) return

        val found = _uiState.value.stolenReports.find {
            it.imei.contains(query, ignoreCase = true)
        }

        _uiState.update {
            it.copy(
                hasSearched = true,
                searchResultReport = found
            )
        }
    }

    fun resetImeiSearch() {
        _uiState.update {
            it.copy(searchImeiQuery = "", searchResultReport = null, hasSearched = false)
        }
    }

    fun addMobile(name: String, model: String, imei: String) {
        val newDev = MobileDevice(
            id = "dev_${System.currentTimeMillis()}",
            name = if (name.isBlank()) "My Device" else name,
            model = if (model.isBlank()) "Generic Android" else model,
            imei = if (imei.isBlank()) "35" + (1000000000000L..9999999999999L).random() else imei,
            batteryPercent = 95,
            isSecured = true,
            isLocked = false,
            isSirenPlaying = false,
            simCardNumber = "+92 300 0000000",
            lastSeenAddress = "Current Location",
            lastSeenTime = "Just now"
        )
        _uiState.update { it.copy(myDevices = it.myDevices + newDev) }
        showMessage("Device ${newDev.name} registered and protected!")
    }

    fun toggleDeviceLock(deviceId: String) {
        _uiState.update { state ->
            val updated = state.myDevices.map { dev ->
                if (dev.id == deviceId) {
                    val newLocked = !dev.isLocked
                    dev.copy(isLocked = newLocked, isSecured = newLocked || dev.isSecured)
                } else dev
            }
            state.copy(myDevices = updated)
        }
    }

    fun toggleDeviceSiren(deviceId: String) {
        _uiState.update { state ->
            val updated = state.myDevices.map { dev ->
                if (dev.id == deviceId) {
                    val newRinging = !dev.isSirenPlaying
                    dev.copy(isSirenPlaying = newRinging)
                } else dev
            }
            state.copy(myDevices = updated)
        }
    }

    fun triggerGlobalSiren() {
        val currentlyRinging = _uiState.value.isSirenPlaying
        if (!currentlyRinging) {
            localSirenEngine.startSiren("EMERGENCY_ALARM")
            _uiState.update {
                it.copy(
                    isSirenPlaying = true,
                    activeBreachTrigger = TriggerReason.MANUAL_PANIC
                )
            }
            showMessage(tr("simulated_alert"))
        } else {
            localSirenEngine.stopSiren()
            localNotificationHelper.cancelTriggerNotification()
            _uiState.update {
                it.copy(
                    isSirenPlaying = false,
                    activeBreachTrigger = null
                )
            }
            showMessage(tr("quick_stop"))
        }
    }

    fun toggleTrackerLock() {
        val isLocked = _uiState.value.liveTrackerState.isDeviceLocked
        _uiState.update { state ->
            state.copy(
                liveTrackerState = state.liveTrackerState.copy(isDeviceLocked = !isLocked)
            )
        }
        showMessage(if (!isLocked) "Device lock signal transmitted!" else "Device unlocked with Master PIN.")
    }

    fun toggleTorchStrobe() {
        val isFlashing = _uiState.value.liveTrackerState.isTorchFlashing
        _uiState.update { state ->
            state.copy(
                liveTrackerState = state.liveTrackerState.copy(isTorchFlashing = !isFlashing)
            )
        }
        showMessage(if (!isFlashing) "Strobe LED beacon activated!" else "Strobe deactivated.")
    }

    fun submitTheftReport(
        imei: String,
        brand: String,
        modelName: String,
        firNumber: String,
        theftLocation: String,
        ownerPhone: String,
        rewardAmount: String
    ) {
        val newReport = StolenReport(
            id = "rep_${System.currentTimeMillis()}",
            imei = imei,
            brand = brand,
            modelName = modelName,
            firNumber = if (firNumber.isBlank()) "PENDING-POLICE-VERIFICATION" else firNumber,
            reportDate = "Today",
            theftLocation = theftLocation,
            ownerPhone = ownerPhone,
            rewardAmount = if (rewardAmount.isBlank()) "$50" else rewardAmount,
            status = ReportStatus.ALERT_ACTIVE
        )

        val newCommunityPost = CommunityAlert(
            id = "com_${System.currentTimeMillis()}",
            title = "Stolen Alert: $brand $modelName",
            description = "Lost/stolen near $theftLocation. FIR: ${newReport.firNumber}. Reward offered: ${newReport.rewardAmount}",
            location = theftLocation,
            timeAgo = "Just now",
            imeiMasked = if (imei.length >= 6) imei.take(4) + "••••••" + imei.takeLast(3) else imei,
            isUrgent = true
        )

        _uiState.update {
            it.copy(
                stolenReports = listOf(newReport) + it.stolenReports,
                communityAlerts = listOf(newCommunityPost) + it.communityAlerts
            )
        }
        showMessage(tr("report_success"))
    }

    fun markDeviceAsStolen(deviceId: String) {
        val now = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())
        var targetDevice: MobileDevice? = null

        _uiState.update { state ->
            val updated = state.myDevices.map { dev ->
                if (dev.id == deviceId) {
                    val lat = state.sensorTelemetry.latitude
                    val lng = state.sensorTelemetry.longitude
                    val stolenDev = dev.copy(
                        isStolen = true,
                        stolenTimestamp = now,
                        isLocked = true,
                        isSecured = false,
                        lastSeenTime = "Stolen mode active",
                        lastKnownLatitude = lat,
                        lastKnownLongitude = lng
                    )
                    targetDevice = stolenDev
                    stolenDev
                } else dev
            }
            state.copy(myDevices = updated)
        }

        targetDevice?.let { dev ->
            // Send initial emergency SMS alert with coordinates
            sendSmsAlertForDevice(dev)
            startStolenDeviceTrackingLoop()
            showMessage("⚠️ ${dev.name} marked as STOLEN! Red badge active, 30s location tracking & SMS alerts engaged.")
        }
    }

    fun unmarkDeviceStolen(deviceId: String) {
        _uiState.update { state ->
            val updated = state.myDevices.map { dev ->
                if (dev.id == deviceId) {
                    dev.copy(
                        isStolen = false,
                        stolenTimestamp = null,
                        isSecured = true,
                        lastSeenTime = "Recovered just now"
                    )
                } else dev
            }
            state.copy(myDevices = updated)
        }

        val anyStillStolen = _uiState.value.myDevices.any { it.isStolen }
        if (!anyStillStolen) {
            stolenTrackingJob?.cancel()
            stolenTrackingJob = null
        }
        showMessage("Device recovered! Stolen mode deactivated.")
    }

    fun updateDeviceEmergencyPhone(deviceId: String, newPhone: String) {
        _uiState.update { state ->
            val updated = state.myDevices.map { dev ->
                if (dev.id == deviceId) dev.copy(emergencyContactPhone = newPhone) else dev
            }
            state.copy(myDevices = updated)
        }
        showMessage("Emergency alert phone updated.")
    }

    fun sendManualSmsAlert(deviceId: String) {
        val dev = _uiState.value.myDevices.find { it.id == deviceId } ?: return
        sendSmsAlertForDevice(dev)
    }

    private fun sendSmsAlertForDevice(device: MobileDevice) {
        val app = getApplication<Application>()
        val lat = _uiState.value.sensorTelemetry.latitude
        val lng = _uiState.value.sensorTelemetry.longitude
        val mapsUrl = "https://maps.google.com/?q=$lat,$lng"
        val message = "🚨 THIEF HUNTER EMERGENCY ALERT: Device '${device.name}' (${device.model}, IMEI: ${device.imei}) marked STOLEN! Live GPS: $mapsUrl (Lat: $lat, Lng: $lng). Battery: ${_uiState.value.sensorTelemetry.batteryPct}%. Time: ${device.stolenTimestamp ?: "Now"}"

        SmsAlertHelper.sendSms(
            context = app,
            phoneNumber = device.emergencyContactPhone,
            message = message,
            onSuccess = {
                showMessage("SMS alert sent to ${device.emergencyContactPhone} with live GPS location!")
            },
            onError = { err ->
                showMessage("SMS alert dispatched: $err")
            }
        )
    }

    private fun startStolenDeviceTrackingLoop() {
        if (stolenTrackingJob?.isActive == true) return
        stolenTrackingJob = viewModelScope.launch {
            var cycleCount = 0
            while (true) {
                delay(30_000L) // Every 30 seconds update location
                cycleCount++

                val currentLat = _uiState.value.sensorTelemetry.latitude
                val currentLng = _uiState.value.sensorTelemetry.longitude
                val timeNow = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                _uiState.update { state ->
                    val updated = state.myDevices.map { dev ->
                        if (dev.isStolen) {
                            dev.copy(
                                lastKnownLatitude = currentLat,
                                lastKnownLongitude = currentLng,
                                lastSeenAddress = "Tracked via GPS ($currentLat, $currentLng)",
                                lastSeenTime = "30s sync at $timeNow"
                            )
                        } else dev
                    }
                    state.copy(myDevices = updated)
                }

                // Every 15 minutes (30 updates * 30 seconds = 900s = 15 minutes), send periodic SMS alert
                if (cycleCount % 30 == 0) {
                    val stolenDevs = _uiState.value.myDevices.filter { it.isStolen }
                    stolenDevs.forEach { dev ->
                        sendSmsAlertForDevice(dev)
                    }
                }
            }
        }
    }

    fun updateMasterPin(newPin: String) {
        if (newPin.length in 4..6) {
            _uiState.update { it.copy(masterPin = newPin) }
            showMessage("Master Security PIN updated!")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stolenTrackingJob?.cancel()
        localSensorManager.onDestroy()
        localSirenEngine.stopSiren()
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (_: Exception) {}
        }
    }
}
