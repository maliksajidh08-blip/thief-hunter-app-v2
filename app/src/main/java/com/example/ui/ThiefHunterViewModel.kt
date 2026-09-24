package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmSoundType
import com.example.data.AppDatabase
import com.example.data.AppLanguage
import com.example.data.AppPreferences
import com.example.data.CommunityAlert
import com.example.data.FamilyAlert
import com.example.data.FamilyDeviceNode
import com.example.data.FamilyDeviceRole
import com.example.data.FamilyNetworkState
import com.example.data.IntruderCapture
import com.example.data.LiveTrackerState
import com.example.data.Localization
import com.example.data.LocationHistoryEntity
import com.example.data.MobileDevice
import com.example.data.ReportStatus
import com.example.data.Screen
import com.example.data.StolenReport
import com.example.security.AlarmSirenEngine
import com.example.security.CameraCaptureHelper
import com.example.security.ChargerBreachType
import com.example.security.ChargerGuard
import com.example.security.ChargerGuardEnvironment
import com.example.security.ChargerGuardState
import com.example.security.GuardConfig
import com.example.security.AutoSleepDetectorEngine
import com.example.security.AutoSleepState
import com.example.security.OfflineLocationTrackerEngine
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
    val userEmail: String? = null,
    val userDisplayName: String = "User",
    val userPhotoUrl: String? = null,
    val isLoggedIn: Boolean = false,
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
    val activeSnackbarMessage: String? = null,
    val familyNetwork: FamilyNetworkState = FamilyNetworkState(
        accountEmail = "sajidhr905@gmail.com",
        familyName = "Sajid Family Guard Group",
        masterDeviceId = "fam_1",
        currentActiveDeviceId = "fam_1",
        isFailoverActive = false,
        failoverGuardianDeviceId = null,
        devices = listOf(
            FamilyDeviceNode(
                id = "fam_1",
                name = "Dad's Galaxy S24 Ultra",
                ownerName = "Sajid (Dad)",
                model = "Samsung Galaxy S24 Ultra",
                phoneNumber = "+92-300-4589211",
                imei = "358912048210341",
                role = FamilyDeviceRole.MASTER,
                isOnline = true,
                batteryPct = 88,
                isArmed = true,
                isStolen = false,
                isLocked = false,
                isSirenActive = false,
                latitude = 31.5204,
                longitude = 74.3587,
                address = "Liberty Market, Gulberg III",
                lastSeenTime = "Active now",
                simNumber = "+92 300 4589211",
                emergencyPhone = "+92 300 4589211",
                capturedPhotoCount = 1
            ),
            FamilyDeviceNode(
                id = "fam_2",
                name = "Mom's Galaxy A54",
                ownerName = "Mom",
                model = "Samsung Galaxy A54 5G",
                phoneNumber = "+92-321-8892104",
                imei = "354728109384721",
                role = FamilyDeviceRole.MEMBER,
                isOnline = true,
                batteryPct = 76,
                isArmed = true,
                isStolen = false,
                isLocked = false,
                isSirenActive = false,
                latitude = 31.5280,
                longitude = 74.3610,
                address = "Home - Block H, Gulberg",
                lastSeenTime = "Active now",
                simNumber = "+92 321 8892104",
                emergencyPhone = "+92 321 8892104",
                capturedPhotoCount = 0
            ),
            FamilyDeviceNode(
                id = "fam_3",
                name = "Son's Pixel 8",
                ownerName = "Ali (Son)",
                model = "Google Pixel 8",
                phoneNumber = "+92-300-1234567",
                imei = "357891043218765",
                role = FamilyDeviceRole.MEMBER,
                isOnline = true,
                batteryPct = 92,
                isArmed = true,
                isStolen = false,
                isLocked = false,
                isSirenActive = false,
                latitude = 31.5122,
                longitude = 74.3489,
                address = "University Campus, Garden Town",
                lastSeenTime = "5 mins ago",
                simNumber = "+92 333 7712345",
                emergencyPhone = "+92 300 1234567",
                capturedPhotoCount = 0
            ),
            FamilyDeviceNode(
                id = "fam_4",
                name = "Daughter's Redmi Note 13",
                ownerName = "Fatima (Daughter)",
                model = "Xiaomi Redmi Note 13",
                phoneNumber = "+92-345-9901234",
                imei = "869402019485720",
                role = FamilyDeviceRole.MEMBER,
                isOnline = true,
                batteryPct = 68,
                isArmed = true,
                isStolen = false,
                isLocked = false,
                isSirenActive = false,
                latitude = 31.5390,
                longitude = 74.3720,
                address = "College Road, Lahore Cantt",
                lastSeenTime = "12 mins ago",
                simNumber = "+92 345 9901234",
                emergencyPhone = "+92 345 9901234",
                capturedPhotoCount = 0
            ),
            FamilyDeviceNode(
                id = "fam_5",
                name = "Home Backup Galaxy S21",
                ownerName = "Home Safe Node",
                model = "Samsung Galaxy S21 FE",
                phoneNumber = "+92-302-1122334",
                imei = "359182736452819",
                role = FamilyDeviceRole.MEMBER,
                isOnline = true,
                batteryPct = 99,
                isArmed = true,
                isStolen = false,
                isLocked = false,
                isSirenActive = false,
                latitude = 31.5217,
                longitude = 74.4036,
                address = "Home Base Desk, Living Room",
                lastSeenTime = "Active (Charging)",
                simNumber = "+92 302 1122334",
                emergencyPhone = "+92 302 1122334",
                capturedPhotoCount = 0
            )
        ),
        alerts = listOf(
            FamilyAlert(
                id = "alt_init",
                timestamp = "Today, 09:00 AM",
                originDeviceId = "fam_1",
                originDeviceName = "Dad's Galaxy S24 Ultra",
                title = "Family Network Online",
                description = "All 5 devices securely synchronized under sajidhr905@gmail.com",
                isUrgent = false
            )
        )
    ),
    val locationHistory: List<LocationHistoryEntity> = emptyList(),
    val lastKnownLocation: LocationHistoryEntity? = null,
    val isDeviceFrozen: Boolean = false,
    val isLocationTracking30sActive: Boolean = true,
    val lastSilentSmsTimestamp: Long? = null,
    val autoSleepState: AutoSleepState = AutoSleepState(),
    val selectedAlarmSound: AlarmSoundType = AlarmSoundType.POLICE_SIREN,
    val isChargerGuardActive: Boolean = false,
    val chargerGuardState: ChargerGuardState = ChargerGuardState()
)

class ThiefHunterViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Local fallback managers in case foreground service is binding
    private val localSensorManager = SensorSecurityManager(application)
    private val localSirenEngine = AlarmSirenEngine(application)
    private val localNotificationHelper = SecurityNotificationHelper(application)
    val offlineLocationTracker = OfflineLocationTrackerEngine(application, viewModelScope)
    val autoSleepDetector = AutoSleepDetectorEngine(application, viewModelScope)
    val chargerGuard = ChargerGuard(application, viewModelScope)
    private val db = AppDatabase.getDatabase(application)
    private val locationDao = db.locationHistoryDao()

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
        val app = getApplication<Application>()
        val savedEmail = AppPreferences.getUserEmail(app)
        val isUserLogged = AppPreferences.isLoggedIn(app)
        val displayName = AppPreferences.getUserDisplayName(app)
        val photoUrl = AppPreferences.getUserPhotoUrl(app)
        val savedSound = AppPreferences.getAlarmSoundType(app)
        val isChargerGuardSaved = AppPreferences.isChargerGuardEnabled(app)

        if (isChargerGuardSaved) {
            chargerGuard.activate()
        }

        _uiState.update {
            it.copy(
                userEmail = savedEmail,
                isLoggedIn = isUserLogged,
                userDisplayName = displayName,
                userPhotoUrl = photoUrl,
                selectedAlarmSound = savedSound,
                isChargerGuardActive = chargerGuard.guardState.value.isActive,
                chargerGuardState = chargerGuard.guardState.value,
                familyNetwork = it.familyNetwork.copy(
                    accountEmail = savedEmail ?: it.familyNetwork.accountEmail
                )
            )
        }

        viewModelScope.launch {
            chargerGuard.guardState.collect { cgState ->
                _uiState.update { it.copy(isChargerGuardActive = cgState.isActive, chargerGuardState = cgState) }
            }
        }

        chargerGuard.onBreachDetected = { breach ->
            onChargerGuardBreached(breach)
        }

        // Collect offline Room location history
        viewModelScope.launch {
            locationDao.getAllLocations().collect { list ->
                _uiState.update { it.copy(locationHistory = list) }
            }
        }

        // Collect latest location fixes from OfflineLocationTrackerEngine
        viewModelScope.launch {
            offlineLocationTracker.lastFix.collect { fix ->
                _uiState.update { state ->
                    state.copy(
                        lastKnownLocation = fix,
                        liveTrackerState = if (fix != null) {
                            state.liveTrackerState.copy(
                                latitude = fix.latitude,
                                longitude = fix.longitude,
                                accuracyMeters = fix.accuracy
                            )
                        } else state.liveTrackerState
                    )
                }
            }
        }

        // Collect device frozen state
        viewModelScope.launch {
            offlineLocationTracker.isDeviceFrozen.collect { frozen ->
                _uiState.update { it.copy(isDeviceFrozen = frozen) }
            }
        }

        // Collect 30s tracking active state
        viewModelScope.launch {
            offlineLocationTracker.isTrackingActive.collect { active ->
                _uiState.update { it.copy(isLocationTracking30sActive = active) }
            }
        }

        // Collect last silent SMS dispatch timestamp
        viewModelScope.launch {
            offlineLocationTracker.lastSmsDispatchedTime.collect { time ->
                _uiState.update { it.copy(lastSilentSmsTimestamp = time) }
            }
        }

        // Collect telemetry from local sensor manager
        viewModelScope.launch {
            localSensorManager.telemetry.collect { tele ->
                autoSleepDetector.onSensorTelemetryUpdate(tele.totalAcceleration, _uiState.value.isSystemArmed)
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

        // Collect Auto-Sleep AI State
        viewModelScope.launch {
            autoSleepDetector.sleepState.collect { sleepState ->
                _uiState.update { it.copy(autoSleepState = sleepState) }
            }
        }

        autoSleepDetector.onAutoArmTriggered = {
            if (!_uiState.value.isSystemArmed) {
                armSystem()
                showMessage("AI Auto-Sleep Guard: Inactivity reached 30s. Armed all sensors automatically.")
            }
        }

        autoSleepDetector.onStrangerTouchBreach = { detail ->
            localSensorManager.fireTrigger(TriggerReason.SLEEP_TOUCH_BREACH)
        }

        autoSleepDetector.onOwnerWakeUpVerified = {
            if (_uiState.value.isSystemArmed || _uiState.value.isSirenPlaying) {
                disarmSystem()
                showMessage("✓ Owner wake-up recognized! Sensors disarmed peacefully.")
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
                TriggerReason.POCKET_REMOVAL,
                TriggerReason.HAND_GRAB_DETECTED -> config.pocketDetectionEnabled
                TriggerReason.MOTION_DETECTED -> config.motionDetectionEnabled
                TriggerReason.CHARGER_UNPLUGGED -> config.chargerUnplugEnabled
                TriggerReason.USB_CONNECTED -> config.usbConnectionEnabled
                TriggerReason.INTRUDER_FAILED_PIN -> config.intruderSelfieEnabled
                TriggerReason.SLEEP_TOUCH_BREACH -> true
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
                if (reason == TriggerReason.SLEEP_TOUCH_BREACH) {
                    dispatchSleepBreachSmsAlert("Stranger touched phone during sleep")
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

    fun toggle30sLocationTracking() {
        if (_uiState.value.isLocationTracking30sActive) {
            offlineLocationTracker.stopTracking()
            showMessage("30s Location Tracking paused.")
        } else {
            offlineLocationTracker.startTracking()
            showMessage("30s Location Tracking activated (No battery drain).")
        }
    }

    fun toggleDeviceFrozen(frozen: Boolean? = null) {
        val target = frozen ?: !_uiState.value.isDeviceFrozen
        offlineLocationTracker.setDeviceFrozen(target)
        if (target) {
            showMessage("DEVICE FROZEN: Emergency Silent SMS dispatched to emergency contact.")
        } else {
            showMessage("Device unfrozen. Normal guard resumed.")
        }
    }

    fun dispatchSilentLocationSmsManual() {
        val fix = _uiState.value.lastKnownLocation
        val lat = fix?.latitude ?: _uiState.value.liveTrackerState.latitude
        val lng = fix?.longitude ?: _uiState.value.liveTrackerState.longitude
        val latStr = String.format(java.util.Locale.US, "%.6f", lat)
        val lngStr = String.format(java.util.Locale.US, "%.6f", lng)
        val sent = offlineLocationTracker.sendSilentSms(lat, lng)
        if (sent) {
            showMessage("Silent SMS sent: 'THIEF HUNTER: $latStr, $lngStr'")
        } else {
            showMessage("Failed to dispatch Silent SMS.")
        }
    }

    fun markDeviceStolenWithSms() {
        offlineLocationTracker.setDeviceStolen(true)
        triggerGlobalSiren()
        showMessage("Device flagged STOLEN! Silent location SMS dispatched.")
    }

    fun clearLocationHistory() {
        viewModelScope.launch {
            locationDao.clearHistory()
            showMessage("Location History cleared.")
        }
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
                showMessage("3rd Wrong PIN! Front camera capturing intruder photo...")
            } else {
                val remaining = 3 - newAttempts
                recordIntruderCapture("Wrong Master PIN entered: $pin (Attempt $newAttempts/3)")
                showMessage("Incorrect PIN! ($newAttempts/3). $remaining attempt(s) until photo capture.")
            }
            return false
        }
    }

    fun onPhotoCaptured(filePath: String, isManualTest: Boolean = false) {
        val reason = if (isManualTest) "Manual Camera Test Snapshot" else "3rd Wrong Master PIN Intruder Selfie"
        recordIntruderCapture(reason, filePath)
        _uiState.update {
            it.copy(
                isCameraCapturing = false,
                lastCapturedPhotoPath = filePath,
                failedPinAttempts = 0
            )
        }
        showMessage(if (isManualTest) "Test photo captured and stored in Vault!" else "Intruder photo saved to Vault!")
    }

    fun onPhotoCaptureFailed(errorMsg: String) {
        _uiState.update { it.copy(isCameraCapturing = false) }
        recordIntruderCapture("3rd Wrong PIN Attempt (Camera Note: $errorMsg)")
        showMessage("Camera note: $errorMsg")
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
            it.copy(
                intruderCaptures = listOf(newCapture) + it.intruderCaptures,
                lastCapturedPhotoPath = photoUri ?: it.lastCapturedPhotoPath
            )
        }
    }

    fun deleteIntruderCapture(id: String) {
        val target = _uiState.value.intruderCaptures.find { it.id == id }
        target?.photoUri?.let { path ->
            try {
                val file = java.io.File(path)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        _uiState.update { state ->
            val updated = state.intruderCaptures.filterNot { it.id == id }
            val newLatest = updated.firstOrNull { it.photoUri != null }?.photoUri
            state.copy(
                intruderCaptures = updated,
                lastCapturedPhotoPath = newLatest
            )
        }
        showMessage("Photo entry deleted from Vault")
    }

    fun updateFamilyDevicePhone(deviceId: String, newPhone: String) {
        _uiState.update { state ->
            val updatedList = state.familyNetwork.devices.map { dev ->
                if (dev.id == deviceId) dev.copy(phoneNumber = newPhone, emergencyPhone = newPhone)
                else dev
            }
            state.copy(familyNetwork = state.familyNetwork.copy(devices = updatedList))
        }
        showMessage("Device phone number updated to $newPhone")
    }

    fun addFamilyDevice(name: String, ownerName: String, model: String, phoneNumber: String, imei: String) {
        val newId = "fam_${System.currentTimeMillis() % 10000}"
        val newDevice = FamilyDeviceNode(
            id = newId,
            name = name,
            ownerName = ownerName,
            model = model,
            phoneNumber = phoneNumber,
            imei = imei,
            role = FamilyDeviceRole.MEMBER,
            isOnline = true,
            batteryPct = 95,
            isArmed = true,
            isStolen = false,
            isLocked = false,
            isSirenActive = false,
            latitude = 31.5204 + (Math.random() - 0.5) * 0.02,
            longitude = 74.3587 + (Math.random() - 0.5) * 0.02,
            address = "Registered Device Location",
            lastSeenTime = "Active now",
            simNumber = phoneNumber,
            emergencyPhone = phoneNumber,
            capturedPhotoCount = 0
        )
        _uiState.update { state ->
            val updatedDevices = state.familyNetwork.devices + newDevice
            state.copy(familyNetwork = state.familyNetwork.copy(devices = updatedDevices))
        }
        showMessage("Device '$name' registered into Family Network!")
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

    fun isUserLoggedIn(): Boolean {
        return AppPreferences.isLoggedIn(getApplication())
    }

    fun loginWithGoogle(email: String, displayName: String? = null, photoUrl: String? = null) {
        val app = getApplication<Application>()
        AppPreferences.setUserEmail(app, email)
        if (displayName != null) AppPreferences.setUserDisplayName(app, displayName)
        if (photoUrl != null) AppPreferences.setUserPhotoUrl(app, photoUrl)

        _uiState.update {
            it.copy(
                userEmail = email,
                userDisplayName = displayName ?: it.userDisplayName,
                userPhotoUrl = photoUrl ?: it.userPhotoUrl,
                isLoggedIn = true,
                familyNetwork = it.familyNetwork.copy(accountEmail = email),
                currentScreen = Screen.HOME
            )
        }
        showMessage("Signed in successfully as $email")
    }

    fun logout() {
        val app = getApplication<Application>()
        AppPreferences.clearUserSession(app)
        _uiState.update {
            it.copy(
                userEmail = null,
                userDisplayName = "User",
                userPhotoUrl = null,
                isLoggedIn = false,
                currentScreen = Screen.LOGIN
            )
        }
        showMessage("Logged out successfully.")
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

    fun setAlarmSound(sound: AlarmSoundType) {
        AppPreferences.setAlarmSoundType(getApplication(), sound)
        _uiState.update { it.copy(selectedAlarmSound = sound) }
        showMessage("Alarm Tone set to: ${sound.displayName}")
    }

    fun previewAlarmSound(sound: AlarmSoundType) {
        localSirenEngine.previewSound(sound, 3000L)
        _uiState.update { it.copy(isSirenPlaying = true) }
        viewModelScope.launch {
            delay(3000L)
            _uiState.update { it.copy(isSirenPlaying = false) }
        }
    }

    fun toggleChargerGuard(environment: ChargerGuardEnvironment = ChargerGuardEnvironment.AIRPORT) {
        val app = getApplication<Application>()
        if (chargerGuard.guardState.value.isActive) {
            chargerGuard.deactivate()
            AppPreferences.setChargerGuardEnabled(app, false)
            showMessage("Charger Guard Disarmed.")
        } else {
            chargerGuard.activate(environment)
            AppPreferences.setChargerGuardEnabled(app, true)
            showMessage("Charger Guard Armed (${environment.label}). Unplugging will trigger loud alarm!")
        }
    }

    fun setChargerGuardEnvironment(env: ChargerGuardEnvironment) {
        chargerGuard.setEnvironment(env)
    }

    fun onChargerGuardBreached(breach: ChargerBreachType) {
        localSirenEngine.startSiren("CHARGER_UNPLUG", _uiState.value.selectedAlarmSound)
        _uiState.update {
            it.copy(
                isSirenPlaying = true,
                activeBreachTrigger = TriggerReason.CHARGER_UNPLUGGED,
                triggerPhotoCaptureEvent = System.currentTimeMillis()
            )
        }
        localNotificationHelper.showTriggerNotification(
            breach.title,
            breach.message
        )
        recordIntruderCapture("Charger Guard Breach: ${breach.title}")
        val loc = _uiState.value.lastKnownLocation
        val lat = loc?.latitude ?: 31.5204
        val lng = loc?.longitude ?: 74.3587
        val emergencyPhone = _uiState.value.myDevices.firstOrNull()?.emergencyContactPhone ?: "+923004589211"
        val smsMessage = "THIEF HUNTER GUARD: Public Charger Guard breached! ${breach.title}. Location: https://maps.google.com/?q=$lat,$lng"
        SmsAlertHelper.sendSms(
            context = getApplication(),
            phoneNumber = emergencyPhone,
            message = smsMessage
        )
    }

    fun startSiren(vibrationPattern: String = "EMERGENCY_ALARM") {
        localSirenEngine.startSiren(vibrationPattern, _uiState.value.selectedAlarmSound)
        _uiState.update {
            it.copy(
                isSirenPlaying = true,
                activeBreachTrigger = TriggerReason.MANUAL_PANIC
            )
        }
    }

    fun stopSiren() {
        localSirenEngine.stopSiren()
        localNotificationHelper.cancelTriggerNotification()
        _uiState.update {
            it.copy(
                isSirenPlaying = false,
                activeBreachTrigger = null
            )
        }
    }

    fun toggleSystemArm() {
        if (_uiState.value.isSystemArmed) {
            disarmSystem()
        } else {
            armSystem()
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

    fun stopGlobalSiren() {
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

    fun triggerAlarmWithReason(reason: TriggerReason) {
        localSensorManager.fireTrigger(reason)
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
            offlineLocationTracker.setDeviceStolen(true)
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
            offlineLocationTracker.setDeviceStolen(false)
            stolenTrackingJob?.cancel()
            stolenTrackingJob = null
        }
        showMessage("Device recovered! Stolen mode deactivated.")
    }

    fun updateDeviceEmergencyPhone(deviceId: String, newPhone: String) {
        offlineLocationTracker.setEmergencyPhone(newPhone)
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
        val lat = _uiState.value.lastKnownLocation?.latitude ?: _uiState.value.sensorTelemetry.latitude
        val lng = _uiState.value.lastKnownLocation?.longitude ?: _uiState.value.sensorTelemetry.longitude
        val latStr = String.format(Locale.US, "%.6f", lat)
        val lngStr = String.format(Locale.US, "%.6f", lng)
        val mapsUrl = "https://maps.google.com/?q=$latStr,$lngStr"
        // Required format: "THIEF HUNTER: [lat], [lng]"
        val message = "THIEF HUNTER: $latStr, $lngStr\nDevice '${device.name}' marked STOLEN!\n$mapsUrl"

        SmsAlertHelper.sendSms(
            context = app,
            phoneNumber = device.emergencyContactPhone,
            message = message,
            onSuccess = {
                showMessage("SMS alert sent to ${device.emergencyContactPhone}: 'THIEF HUNTER: $latStr, $lngStr'")
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

    // ==========================================
    // FAMILY SECURITY NETWORK OPERATIONS
    // ==========================================

    fun switchActivePerspectiveDevice(deviceId: String) {
        val device = _uiState.value.familyNetwork.devices.find { it.id == deviceId } ?: return
        _uiState.update { state ->
            state.copy(
                familyNetwork = state.familyNetwork.copy(currentActiveDeviceId = deviceId)
            )
        }
        showMessage("Switched perspective to: ${device.name}")
    }

    fun updateFamilyGmail(newEmail: String) {
        if (newEmail.contains("@") && newEmail.contains(".")) {
            _uiState.update { state ->
                val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                val newAlert = FamilyAlert(
                    id = "alt_${System.currentTimeMillis()}",
                    timestamp = now,
                    originDeviceId = state.familyNetwork.currentActiveDeviceId,
                    originDeviceName = "Account Manager",
                    title = "Gmail Linked",
                    description = "Family network synchronized under $newEmail",
                    isUrgent = false
                )
                state.copy(
                    familyNetwork = state.familyNetwork.copy(
                        accountEmail = newEmail,
                        alerts = listOf(newAlert) + state.familyNetwork.alerts
                    )
                )
            }
            showMessage("Family Security Network linked with $newEmail")
        }
    }

    fun remoteLockFamilyDevice(targetDeviceId: String) {
        val state = _uiState.value
        val target = state.familyNetwork.devices.find { it.id == targetDeviceId } ?: return
        val current = state.familyNetwork.devices.find { it.id == state.familyNetwork.currentActiveDeviceId }
        val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val updatedDevices = state.familyNetwork.devices.map {
            if (it.id == targetDeviceId) it.copy(isLocked = true) else it
        }

        val alert = FamilyAlert(
            id = "alt_${System.currentTimeMillis()}",
            timestamp = now,
            originDeviceId = current?.id ?: "fam_1",
            originDeviceName = current?.name ?: "Family Node",
            title = "Remote Lock Applied",
            description = "${current?.name ?: "Controller"} remotely locked ${target.name}",
            isUrgent = true
        )

        _uiState.update {
            it.copy(
                familyNetwork = it.familyNetwork.copy(
                    devices = updatedDevices,
                    alerts = listOf(alert) + it.familyNetwork.alerts
                )
            )
        }

        // If target is current active device, activate local lock
        if (targetDeviceId == state.familyNetwork.currentActiveDeviceId) {
            _uiState.update { it.copy(isSystemArmed = true) }
        }

        showMessage("Remote LOCK dispatched to ${target.name} via Family Network!")
    }

    fun remoteUnlockFamilyDevice(targetDeviceId: String) {
        val state = _uiState.value
        val target = state.familyNetwork.devices.find { it.id == targetDeviceId } ?: return
        val updatedDevices = state.familyNetwork.devices.map {
            if (it.id == targetDeviceId) it.copy(isLocked = false) else it
        }

        _uiState.update {
            it.copy(
                familyNetwork = it.familyNetwork.copy(devices = updatedDevices)
            )
        }
        showMessage("${target.name} unlocked successfully")
    }

    fun remoteTriggerFamilySiren(targetDeviceId: String) {
        val state = _uiState.value
        val target = state.familyNetwork.devices.find { it.id == targetDeviceId } ?: return
        val current = state.familyNetwork.devices.find { it.id == state.familyNetwork.currentActiveDeviceId }
        val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val updatedDevices = state.familyNetwork.devices.map {
            if (it.id == targetDeviceId) it.copy(isSirenActive = true) else it
        }

        val alert = FamilyAlert(
            id = "alt_${System.currentTimeMillis()}",
            timestamp = now,
            originDeviceId = current?.id ?: "fam_1",
            originDeviceName = current?.name ?: "Family Node",
            title = "SIREN TRIGGERED",
            description = "High-decibel emergency siren triggered remotely on ${target.name}",
            isUrgent = true
        )

        _uiState.update {
            it.copy(
                familyNetwork = it.familyNetwork.copy(
                    devices = updatedDevices,
                    alerts = listOf(alert) + it.familyNetwork.alerts
                )
            )
        }

        // If current active device is target, sound siren
        if (targetDeviceId == state.familyNetwork.currentActiveDeviceId) {
            triggerGlobalSiren()
        }

        showMessage("SIREN command broadcasted to ${target.name}!")
    }

    fun remoteStopFamilySiren(targetDeviceId: String) {
        val state = _uiState.value
        val target = state.familyNetwork.devices.find { it.id == targetDeviceId } ?: return
        val updatedDevices = state.familyNetwork.devices.map {
            if (it.id == targetDeviceId) it.copy(isSirenActive = false) else it
        }

        _uiState.update {
            it.copy(
                familyNetwork = it.familyNetwork.copy(devices = updatedDevices)
            )
        }

        if (targetDeviceId == state.familyNetwork.currentActiveDeviceId) {
            stopGlobalSiren()
        }

        showMessage("Siren silenced on ${target.name}")
    }

    fun markFamilyDeviceStolen(targetDeviceId: String) {
        val state = _uiState.value
        val target = state.familyNetwork.devices.find { it.id == targetDeviceId } ?: return
        val isTargetMaster = (targetDeviceId == state.familyNetwork.masterDeviceId)
        val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        // If target is Master phone, activate Failover:
        // Next available online member becomes FAILOVER_GUARDIAN
        var electedGuardianId: String? = null
        val updatedDevices = state.familyNetwork.devices.map { dev ->
            if (dev.id == targetDeviceId) {
                dev.copy(isStolen = true, isLocked = true, isSirenActive = true)
            } else if (isTargetMaster && dev.role == FamilyDeviceRole.MEMBER && electedGuardianId == null && dev.isOnline) {
                electedGuardianId = dev.id
                dev.copy(role = FamilyDeviceRole.FAILOVER_GUARDIAN)
            } else dev
        }

        val guardianName = updatedDevices.find { it.id == electedGuardianId }?.name ?: "Surviving Family Phones"

        val alertTitle = if (isTargetMaster) "🚨 MASTER FAILOVER ACTIVATED" else "🚨 FAMILY PHONE STOLEN"
        val alertDesc = if (isTargetMaster) {
            "Master phone (${target.name}) was stolen! $guardianName has automatically taken over as Guardian Controller. All 4 phones can track and lock Master."
        } else {
            "${target.name} has been marked STOLEN! Live GPS coordinates shared across all 4 family phones."
        }

        val alert = FamilyAlert(
            id = "alt_${System.currentTimeMillis()}",
            timestamp = now,
            originDeviceId = targetDeviceId,
            originDeviceName = target.name,
            title = alertTitle,
            description = alertDesc,
            isUrgent = true
        )

        _uiState.update {
            it.copy(
                familyNetwork = it.familyNetwork.copy(
                    isFailoverActive = isTargetMaster,
                    failoverGuardianDeviceId = electedGuardianId,
                    devices = updatedDevices,
                    alerts = listOf(alert) + it.familyNetwork.alerts
                )
            )
        }

        // Send SMS to emergency number
        val smsMessage = "🚨 FAMILY NETWORK ALERT: ${target.name} marked STOLEN! Live GPS: https://maps.google.com/?q=${target.latitude},${target.longitude}. Master Failover active."
        SmsAlertHelper.sendSms(
            context = getApplication(),
            phoneNumber = target.emergencyPhone,
            message = smsMessage,
            onSuccess = {},
            onError = {}
        )

        showMessage(if (isTargetMaster) "MASTER STOLEN! Failover transferred to $guardianName" else "${target.name} marked STOLEN!")
    }

    fun unmarkFamilyDeviceStolen(targetDeviceId: String) {
        val state = _uiState.value
        val target = state.familyNetwork.devices.find { it.id == targetDeviceId } ?: return
        val wasMaster = (targetDeviceId == state.familyNetwork.masterDeviceId)
        val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val updatedDevices = state.familyNetwork.devices.map { dev ->
            if (dev.id == targetDeviceId) {
                dev.copy(
                    isStolen = false,
                    isLocked = false,
                    isSirenActive = false,
                    role = if (wasMaster) FamilyDeviceRole.MASTER else FamilyDeviceRole.MEMBER
                )
            } else if (dev.role == FamilyDeviceRole.FAILOVER_GUARDIAN) {
                dev.copy(role = FamilyDeviceRole.MEMBER)
            } else dev
        }

        val alert = FamilyAlert(
            id = "alt_${System.currentTimeMillis()}",
            timestamp = now,
            originDeviceId = targetDeviceId,
            originDeviceName = target.name,
            title = "Device Recovered",
            description = "${target.name} has been marked RECOVERED. Normal security restored.",
            isUrgent = false
        )

        _uiState.update {
            it.copy(
                familyNetwork = it.familyNetwork.copy(
                    isFailoverActive = false,
                    failoverGuardianDeviceId = null,
                    devices = updatedDevices,
                    alerts = listOf(alert) + it.familyNetwork.alerts
                )
            )
        }

        showMessage("${target.name} recovered! Family network restored.")
    }

    fun broadcastFamilyEmergency(title: String, description: String) {
        val state = _uiState.value
        val current = state.familyNetwork.devices.find { it.id == state.familyNetwork.currentActiveDeviceId }
        val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val alert = FamilyAlert(
            id = "alt_${System.currentTimeMillis()}",
            timestamp = now,
            originDeviceId = current?.id ?: "fam_1",
            originDeviceName = current?.name ?: "Family Phone",
            title = title,
            description = description,
            isUrgent = true
        )

        _uiState.update {
            it.copy(
                familyNetwork = it.familyNetwork.copy(
                    alerts = listOf(alert) + it.familyNetwork.alerts
                )
            )
        }
        showMessage("Emergency broadcasted to all family phones!")
    }

    // ═══════════════════════════════════════════════════════════
    // AI FEATURE 1: AUTO-SLEEP DETECTION METHODS
    // ═══════════════════════════════════════════════════════════

    fun toggleAutoSleepDetection() {
        val current = _uiState.value.autoSleepState.isEnabled
        autoSleepDetector.setEnabled(!current)
        showMessage(if (!current) "AI Auto-Sleep Detection Enabled (30s inactivity auto-arms)" else "AI Auto-Sleep Detection Disabled")
    }

    fun setSleepInactivityThreshold(seconds: Int) {
        autoSleepDetector.setSleepThresholdSeconds(seconds)
        showMessage("Sleep inactivity threshold set to ${seconds}s")
    }

    fun simulateSleepFastForward() {
        autoSleepDetector.fastForwardSleepArming()
        showMessage("⚡ Fast-forward: 30s stillness reached. Auto-arming all sensors!")
    }

    fun simulateSleepTouch(isOwner: Boolean) {
        val isArmed = _uiState.value.isSystemArmed || _uiState.value.autoSleepState.isSleepArmed
        if (!isArmed) {
            showMessage("Please allow sleep auto-arm or tap 'ARM' first to test sleep touch.")
            return
        }

        if (isOwner) {
            // Owner wake up: Face recognition passes -> disarms with no alarm!
            autoSleepDetector.confirmOwnerWakeUp("Owner face recognized via front camera AI.")
        } else {
            // Stranger midnight touch: Face unrecognized -> Alarm rings immediately + SMS dispatched!
            autoSleepDetector.confirmStrangerBreach("Stranger face / unauthorized touch while sleeping")
        }
    }

    fun resetSleepLearning() {
        autoSleepDetector.resetSleepState()
        showMessage("Sleep pattern tracking reset.")
    }

    private fun dispatchSleepBreachSmsAlert(detail: String) {
        val app = getApplication<Application>()
        val currentDevice = _uiState.value.myDevices.firstOrNull()
        val emergencyPhone = currentDevice?.emergencyContactPhone ?: "+92 300 1234567"
        val lat = _uiState.value.lastKnownLocation?.latitude ?: _uiState.value.sensorTelemetry.latitude
        val lng = _uiState.value.lastKnownLocation?.longitude ?: _uiState.value.sensorTelemetry.longitude
        val latStr = String.format(Locale.US, "%.6f", lat)
        val lngStr = String.format(Locale.US, "%.6f", lng)
        val mapsUrl = "https://maps.google.com/?q=$latStr,$lngStr"
        val message = "🚨 THIEF HUNTER: Sleep Alert! Stranger touch while owner sleeping!\nGPS: $latStr, $lngStr\nMap: $mapsUrl"

        SmsAlertHelper.sendSms(
            context = app,
            phoneNumber = emergencyPhone,
            message = message,
            onSuccess = {
                showMessage("Emergency SMS dispatched to $emergencyPhone: Stranger sleep touch!")
            },
            onError = { err ->
                showMessage("SMS alert notice: $err")
            }
        )
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
