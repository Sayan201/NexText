package com.nextext.sms.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nextext_prefs")

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val autoDeleteOldOtps: Boolean = false,
    val mutePromotionsNotifications: Boolean = true,
    val showOtpCopyNotification: Boolean = true,
    val lastSyncTimestamp: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AUTO_DELETE_OTPS = booleanPreferencesKey("auto_delete_otps")
        val MUTE_PROMOS = booleanPreferencesKey("mute_promos")
        val OTP_NOTIFICATION = booleanPreferencesKey("otp_notification")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            autoDeleteOldOtps = prefs[Keys.AUTO_DELETE_OTPS] ?: false,
            mutePromotionsNotifications = prefs[Keys.MUTE_PROMOS] ?: true,
            showOtpCopyNotification = prefs[Keys.OTP_NOTIFICATION] ?: true,
            lastSyncTimestamp = prefs[Keys.LAST_SYNC] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAutoDeleteOldOtps(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DELETE_OTPS] = enabled }
    }

    suspend fun setMutePromotionsNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MUTE_PROMOS] = enabled }
    }

    suspend fun setShowOtpCopyNotification(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OTP_NOTIFICATION] = enabled }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC] = timestamp }
    }
}
