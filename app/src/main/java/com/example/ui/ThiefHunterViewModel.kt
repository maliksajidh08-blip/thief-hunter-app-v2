package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.CommunityAlert
import com.example.data.LiveTrackerState
import com.example.data.Localization
import com.example.data.MobileDevice
import com.example.data.ReportStatus
import com.example.data.Screen
import com.example.data.StolenReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val currentScreen: Screen = Screen.SPLASH,
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
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
            lastSeenTime = "Active now"
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
            lastSeenTime = "12 mins ago"
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
    val isPocketAlarmEnabled: Boolean = true,
    val isSimChangeAlertEnabled: Boolean = true,
    val motionSensitivity: Float = 3f,
    val masterPin: String = "9051",
    val activeSnackbarMessage: String? = null
)

class ThiefHunterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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
        val currentlyRinging = _uiState.value.liveTrackerState.isSirenRinging
        _uiState.update { state ->
            state.copy(
                liveTrackerState = state.liveTrackerState.copy(isSirenRinging = !currentlyRinging)
            )
        }
        if (!currentlyRinging) {
            showMessage(tr("simulated_alert"))
        } else {
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

    fun togglePocketAlarm(enabled: Boolean) {
        _uiState.update { it.copy(isPocketAlarmEnabled = enabled) }
    }

    fun toggleSimChangeAlert(enabled: Boolean) {
        _uiState.update { it.copy(isSimChangeAlertEnabled = enabled) }
    }

    fun setMotionSensitivity(value: Float) {
        _uiState.update { it.copy(motionSensitivity = value) }
    }

    fun updateMasterPin(newPin: String) {
        if (newPin.length <= 6) {
            _uiState.update { it.copy(masterPin = newPin) }
        }
    }
}
