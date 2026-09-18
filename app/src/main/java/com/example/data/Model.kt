package com.example.data

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val isRtl: Boolean) {
    ENGLISH("en", "English", "English", false),
    URDU("ur", "Urdu", "اردو", true),
    HINDI("hi", "Hindi", "हिन्दी", false),
    ARABIC("ar", "Arabic", "العربية", true),
    SPANISH("es", "Spanish", "Español", false)
}

enum class DeviceStatus {
    SECURE,
    ALARM_ACTIVE,
    LOCKED,
    DISCONNECTED
}

data class MobileDevice(
    val id: String,
    val name: String,
    val model: String,
    val imei: String,
    val batteryPercent: Int,
    val isSecured: Boolean = true,
    val isLocked: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val simCardNumber: String = "+92 300 ••••123",
    val lastSeenAddress: String = "Gulberg III, Main Blvd",
    val lastSeenTime: String = "Just now"
)

enum class ReportStatus {
    ALERT_ACTIVE,
    INVESTIGATING,
    RECOVERED
}

data class StolenReport(
    val id: String,
    val imei: String,
    val brand: String,
    val modelName: String,
    val firNumber: String,
    val reportDate: String,
    val theftLocation: String,
    val ownerPhone: String,
    val rewardAmount: String = "$100",
    val status: ReportStatus = ReportStatus.ALERT_ACTIVE
)

data class CommunityAlert(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val timeAgo: String,
    val imeiMasked: String,
    val isUrgent: Boolean = false
)

data class LiveTrackerState(
    val latitude: Double = 31.5204,
    val longitude: Double = 74.3587,
    val locationName: String = "Canal Road, Tech District",
    val accuracyMeters: Float = 4.2f,
    val speedKmh: Float = 0.0f,
    val isGpsActive: Boolean = true,
    val isRadarScanning: Boolean = true,
    val isSirenRinging: Boolean = false,
    val isDeviceLocked: Boolean = false,
    val isTorchFlashing: Boolean = false
)

data class IntruderCapture(
    val id: String,
    val timestamp: String,
    val triggerReason: String,
    val photoUri: String? = null,
    val wasPinWrong: Boolean = true
)

enum class Screen(val titleKey: String) {
    SPLASH("app_name"),
    HOME("home_title"),
    SENSORS_HUB("sensors_hub"),
    MY_MOBILES("my_mobiles"),
    STOLEN_DEVICES("stolen_devices"),
    LIVE_TRACKING("live_tracking"),
    COMMUNITY("community"),
    REPORT_THEFT("report_theft"),
    INTRUDER_SELFIE("intruder_selfie"),
    SETTINGS("settings")
}
