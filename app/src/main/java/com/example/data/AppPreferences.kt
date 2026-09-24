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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
}
