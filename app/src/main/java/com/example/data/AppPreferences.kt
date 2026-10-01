package com.example.data

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREFS_NAME = "thief_hunter_prefs"
    private const val KEY_USER_EMAIL = "key_user_email"
    private const val KEY_USER_DISPLAY_NAME = "key_user_display_name"
    private const val KEY_USER_PHOTO_URL = "key_user_photo_url"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_ALARM_SOUND_TYPE = "key_alarm_sound_type"
    private const val KEY_CHARGER_GUARD_ENABLED = "key_charger_guard_enabled"
    private const val KEY_OWNER_FACE_ENROLLED = "key_owner_face_enrolled"
    private const val KEY_OWNER_FACE_COUNT = "key_owner_face_count"
    private const val KEY_OWNER_FACE_BASELINE = "key_owner_face_baseline"
    private const val KEY_BATTERY_MODE = "key_battery_mode"
    private const val KEY_PRIMARY_IMEI = "key_primary_imei"
    private const val KEY_PRIMARY_DEVICE_NAME = "key_primary_device_name"
    private const val KEY_PRIMARY_DEVICE_MODEL = "key_primary_device_model"
    private const val KEY_EMERGENCY_PHONE = "key_emergency_phone"
    private const val KEY_MASTER_PIN = "key_master_pin"
    private const val KEY_MOTION_SENSITIVITY = "key_motion_sensitivity"
    private const val KEY_ARMING_DELAY = "key_arming_delay"
    private const val KEY_IS_GUARD_SERVICE_ACTIVE = "key_is_guard_service_active"
    private const val KEY_IS_SYSTEM_ARMED = "key_is_system_armed"
    private const val KEY_OWNER_LIFT_SPEED = "key_owner_lift_speed"
    private const val KEY_OWNER_WALK_VARIANCE = "key_owner_walk_variance"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBatteryMode(context: Context): BatteryMode {
        val id = getPrefs(context).getString(KEY_BATTERY_MODE, BatteryMode.BALANCED.id)
        return BatteryMode.values().firstOrNull { it.id == id } ?: BatteryMode.BALANCED
    }

    fun setBatteryMode(context: Context, mode: BatteryMode) {
        getPrefs(context).edit().putString(KEY_BATTERY_MODE, mode.id).apply()
    }

    fun isOwnerFaceEnrolled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_OWNER_FACE_ENROLLED, false)
    }

    fun setOwnerFaceEnrolled(context: Context, enrolled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_OWNER_FACE_ENROLLED, enrolled).apply()
    }

    fun getOwnerFaceSamplesCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_OWNER_FACE_COUNT, 0)
    }

    fun setOwnerFaceSamplesCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_OWNER_FACE_COUNT, count).apply()
    }

    fun getOwnerFaceBaseline(context: Context): String? {
        return getPrefs(context).getString(KEY_OWNER_FACE_BASELINE, null)
    }

    fun setOwnerFaceBaseline(context: Context, baseline: String?) {
        getPrefs(context).edit().putString(KEY_OWNER_FACE_BASELINE, baseline).apply()
    }

    fun clearOwnerFace(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_OWNER_FACE_ENROLLED)
            .remove(KEY_OWNER_FACE_COUNT)
            .remove(KEY_OWNER_FACE_BASELINE)
            .apply()
    }

    fun getAlarmSoundType(context: Context): AlarmSoundType {
        val id = getPrefs(context).getString(KEY_ALARM_SOUND_TYPE, AlarmSoundType.POLICE_SIREN.id)
        return AlarmSoundType.values().firstOrNull { it.id == id } ?: AlarmSoundType.POLICE_SIREN
    }

    fun setAlarmSoundType(context: Context, soundType: AlarmSoundType) {
        getPrefs(context).edit().putString(KEY_ALARM_SOUND_TYPE, soundType.id).apply()
    }

    fun isChargerGuardEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CHARGER_GUARD_ENABLED, false)
    }

    fun setChargerGuardEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CHARGER_GUARD_ENABLED, enabled).apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val email = getUserEmail(context)
        return !email.isNullOrBlank()
    }

    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null)
    }

    fun setUserEmail(context: Context, email: String?) {
        getPrefs(context).edit().apply {
            if (email != null) {
                putString(KEY_USER_EMAIL, email)
                putBoolean(KEY_IS_LOGGED_IN, true)
            } else {
                remove(KEY_USER_EMAIL)
                putBoolean(KEY_IS_LOGGED_IN, false)
            }
            apply()
        }
    }

    fun getUserDisplayName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_DISPLAY_NAME, "User") ?: "User"
    }

    fun setUserDisplayName(context: Context, name: String?) {
        getPrefs(context).edit().putString(KEY_USER_DISPLAY_NAME, name ?: "User").apply()
    }

    fun getUserPhotoUrl(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_PHOTO_URL, null)
    }

    fun setUserPhotoUrl(context: Context, photoUrl: String?) {
        getPrefs(context).edit().putString(KEY_USER_PHOTO_URL, photoUrl).apply()
    }

    fun clearUserSession(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_DISPLAY_NAME)
            remove(KEY_USER_PHOTO_URL)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    fun getPrimaryImei(context: Context): String {
        return getPrefs(context).getString(KEY_PRIMARY_IMEI, "358941092837461") ?: "358941092837461"
    }

    fun setPrimaryImei(context: Context, imei: String) {
        getPrefs(context).edit().putString(KEY_PRIMARY_IMEI, imei).apply()
    }

    fun getPrimaryDeviceName(context: Context): String {
        return getPrefs(context).getString(KEY_PRIMARY_DEVICE_NAME, "My Primary Phone") ?: "My Primary Phone"
    }

    fun setPrimaryDeviceName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_PRIMARY_DEVICE_NAME, name).apply()
    }

    fun getPrimaryDeviceModel(context: Context): String {
        return getPrefs(context).getString(KEY_PRIMARY_DEVICE_MODEL, "Samsung Galaxy S24 Ultra") ?: "Samsung Galaxy S24 Ultra"
    }

    fun setPrimaryDeviceModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_PRIMARY_DEVICE_MODEL, model).apply()
    }

    fun getEmergencyPhone(context: Context): String {
        return getPrefs(context).getString(KEY_EMERGENCY_PHONE, "+92 300 4589211") ?: "+92 300 4589211"
    }

    fun setEmergencyPhone(context: Context, phone: String) {
        getPrefs(context).edit().putString(KEY_EMERGENCY_PHONE, phone).apply()
    }

    fun getMasterPin(context: Context): String {
        return getPrefs(context).getString(KEY_MASTER_PIN, "9051") ?: "9051"
    }

    fun setMasterPin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_MASTER_PIN, pin).apply()
    }

    fun getMotionSensitivity(context: Context): Float {
        return getPrefs(context).getFloat(KEY_MOTION_SENSITIVITY, 2.0f)
    }

    fun setMotionSensitivity(context: Context, sensitivity: Float) {
        getPrefs(context).edit().putFloat(KEY_MOTION_SENSITIVITY, sensitivity).apply()
    }

    fun getArmingDelaySeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_ARMING_DELAY, 30)
    }

    fun setArmingDelaySeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_ARMING_DELAY, seconds).apply()
    }

    fun isGuardServiceActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_GUARD_SERVICE_ACTIVE, true)
    }

    fun setGuardServiceActive(context: Context, active: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_GUARD_SERVICE_ACTIVE, active).apply()
    }

    fun isSystemArmed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_SYSTEM_ARMED, false)
    }

    fun setSystemArmed(context: Context, armed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_SYSTEM_ARMED, armed).apply()
    }

    fun getOwnerLiftSpeed(context: Context): Float {
        return getPrefs(context).getFloat(KEY_OWNER_LIFT_SPEED, 0.45f)
    }

    fun setOwnerLiftSpeed(context: Context, speed: Float) {
        getPrefs(context).edit().putFloat(KEY_OWNER_LIFT_SPEED, speed).apply()
    }

    fun getOwnerWalkVariance(context: Context): Float {
        return getPrefs(context).getFloat(KEY_OWNER_WALK_VARIANCE, 1.8f)
    }

    fun setOwnerWalkVariance(context: Context, variance: Float) {
        getPrefs(context).edit().putFloat(KEY_OWNER_WALK_VARIANCE, variance).apply()
    }
}
