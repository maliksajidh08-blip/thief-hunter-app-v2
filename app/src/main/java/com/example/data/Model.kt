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
    val lastSeenTime: String = "Just now",
    val isStolen: Boolean = false,
    val stolenTimestamp: String? = null,
    val emergencyContactPhone: String = "+92 300 1234567",
    val lastKnownLatitude: Double = 31.5204,
    val lastKnownLongitude: Double = 74.3587
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

enum class FamilyDeviceRole {
    MASTER,
    MEMBER,
    FAILOVER_GUARDIAN
}

data class FamilyDeviceNode(
    val id: String,
    val name: String,
    val ownerName: String,
    val model: String,
    val phoneNumber: String = "+92-300-1234567",
    val imei: String = "357891043218765",
    val role: FamilyDeviceRole = FamilyDeviceRole.MEMBER,
    val isOnline: Boolean = true,
    val batteryPct: Int = 85,
    val isArmed: Boolean = true,
    val isStolen: Boolean = false,
    val isLocked: Boolean = false,
    val isSirenActive: Boolean = false,
    val latitude: Double = 31.5204,
    val longitude: Double = 74.3587,
    val address: String = "Liberty Market, Lahore",
    val lastSeenTime: String = "Active now",
    val simNumber: String = "+92 300 ••••111",
    val emergencyPhone: String = "+92 300 4589211",
    val capturedPhotoCount: Int = 0,
    val lastPhotoUri: String? = null
)

data class FamilyAlert(
    val id: String,
    val timestamp: String,
    val originDeviceId: String,
    val originDeviceName: String,
    val title: String,
    val description: String,
    val isUrgent: Boolean = true
)

data class FamilyNetworkState(
    val accountEmail: String = "sajidhr905@gmail.com",
    val familyName: String = "Sajid Family Guard Group",
    val masterDeviceId: String = "fam_1",
    val currentActiveDeviceId: String = "fam_1",
    val isFailoverActive: Boolean = false,
    val failoverGuardianDeviceId: String? = null,
    val devices: List<FamilyDeviceNode> = emptyList(),
    val alerts: List<FamilyAlert> = emptyList()
)

enum class AlarmSoundType(val id: String, val displayName: String, val icon: String, val description: String) {
    POLICE_SIREN("police_siren", "Police Siren", "🔵", "High-pitch alternating emergency police siren (Default)"),
    DOG_BARKING("dog_barking", "Dog Barking", "🔴", "Aggressive watchdog guarding staccato bark & growl"),
    GUN_SHOT("gun_shot", "Gun Shot", "🟡", "Rapid tactical gunshot detonations with echo reverberation"),
    STANDARD_BEEP("standard_beep", "Standard Beep", "🟢", "Piercing 2.8kHz dual-tone electronic alarm pulses"),
    CUSTOM_SOUND("custom_sound", "Custom Sound", "🔵", "High-intensity oscillating tactical cyber warble")
}

enum class Screen(val titleKey: String) {
    SPLASH("app_name"),
    LOGIN("login"),
    HOME("home_title"),
    FAMILY_NETWORK("family_network"),
    SENSORS_HUB("sensors_hub"),
    MY_MOBILES("my_mobiles"),
    STOLEN_DEVICES("stolen_devices"),
    LIVE_TRACKING("live_tracking"),
    COMMUNITY("community"),
    REPORT_THEFT("report_theft"),
    INTRUDER_SELFIE("intruder_selfie"),
    DEVICE_REGISTRATION("register_device"),
    SETTINGS("settings"),
    ABOUT("about_app")
}

