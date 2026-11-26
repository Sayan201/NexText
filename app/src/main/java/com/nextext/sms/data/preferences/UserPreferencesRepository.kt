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
    PITCH_BLACK("Pitch Black (AMOLED)"),
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System Default")
}

enum class PitchBlackAccent(
    val label: String,
    val primaryHex: Long,
    val containerHex: Long,
    val onContainerHex: Long
) {
    PURPLE("Purple", 0xFFA855F7, 0xFF2B1644, 0xFFE9D5FF),
    BLUE("Blue", 0xFF5B86E5, 0xFF1A2942, 0xFFD6E4FF),
    EMERALD("Green", 0xFF25D366, 0xFF102E1C, 0xFFA7F3D0),
    ROSE("Rose", 0xFFF43F5E, 0xFF3A121D, 0xFFFECDD3),
    CYAN("Cyan", 0xFF06B6D4, 0xFF0E2F38, 0xFFA5F3FC),
    AMBER("Amber", 0xFFF59E0B, 0xFF362409, 0xFFFDE68A)
}

enum class GroupMessagingMode(val label: String) {
    BULK("Bulk"),
    INDIVIDUAL("Individual"),
    MMS_GROUP("MMS Group")
}

enum class SwipeAction(val label: String) {
    ARCHIVE("Archive"),
    DELETE("Delete"),
    MARK_READ("Mark as Read"),
    NONE("None")
}

enum class AutoBackupFrequency(val label: String, val intervalDays: Long) {
    OFF("Off", 0L),
    DAILY("Daily", 1L),
    WEEKLY("Weekly", 7L),
    MONTHLY("Monthly", 30L)
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.PITCH_BLACK,
    val pitchBlackAccent: PitchBlackAccent = PitchBlackAccent.CYAN,
    val useMaterial3Design: Boolean = false,
    val dynamicColor: Boolean = false,
    val autoDeleteOldOtps: Boolean = false,
    val mutePromotionsNotifications: Boolean = true,
    val showOtpCopyNotification: Boolean = true,
    val showQuickReplies: Boolean = true,
    val swapMenuBarPositions: Boolean = false,
    val groupMessagingMode: GroupMessagingMode = GroupMessagingMode.BULK,
    val swipeLeftAction: SwipeAction = SwipeAction.ARCHIVE,
    val swipeRightAction: SwipeAction = SwipeAction.DELETE,
    val autoBackupFrequency: AutoBackupFrequency = AutoBackupFrequency.OFF,
    val lastSyncTimestamp: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PITCH_BLACK_ACCENT = stringPreferencesKey("pitch_black_accent")
        val USE_MATERIAL3 = booleanPreferencesKey("use_material3")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AUTO_DELETE_OTPS = booleanPreferencesKey("auto_delete_otps")
        val MUTE_PROMOS = booleanPreferencesKey("mute_promos")
        val OTP_NOTIFICATION = booleanPreferencesKey("otp_notification")
        val SHOW_QUICK_REPLIES = booleanPreferencesKey("show_quick_replies")
        val SWAP_MENU_BAR = booleanPreferencesKey("swap_menu_bar")
        val GROUP_MESSAGING = stringPreferencesKey("group_messaging")
        val SWIPE_LEFT = stringPreferencesKey("swipe_left")
        val SWIPE_RIGHT = stringPreferencesKey("swipe_right")
        val AUTO_BACKUP_FREQ = stringPreferencesKey("auto_backup_freq")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.PITCH_BLACK)
            } ?: ThemeMode.PITCH_BLACK,
            pitchBlackAccent = prefs[Keys.PITCH_BLACK_ACCENT]?.let {
                runCatching { PitchBlackAccent.valueOf(it) }.getOrDefault(PitchBlackAccent.CYAN)
            } ?: PitchBlackAccent.CYAN,
            useMaterial3Design = prefs[Keys.USE_MATERIAL3] ?: false,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            autoDeleteOldOtps = prefs[Keys.AUTO_DELETE_OTPS] ?: false,
            mutePromotionsNotifications = prefs[Keys.MUTE_PROMOS] ?: true,
            showOtpCopyNotification = prefs[Keys.OTP_NOTIFICATION] ?: true,
            showQuickReplies = prefs[Keys.SHOW_QUICK_REPLIES] ?: true,
            swapMenuBarPositions = prefs[Keys.SWAP_MENU_BAR] ?: false,
            groupMessagingMode = prefs[Keys.GROUP_MESSAGING]?.let {
                runCatching { GroupMessagingMode.valueOf(it) }.getOrDefault(GroupMessagingMode.BULK)
            } ?: GroupMessagingMode.BULK,
            swipeLeftAction = prefs[Keys.SWIPE_LEFT]?.let {
                runCatching { SwipeAction.valueOf(it) }.getOrDefault(SwipeAction.ARCHIVE)
            } ?: SwipeAction.ARCHIVE,
            swipeRightAction = prefs[Keys.SWIPE_RIGHT]?.let {
                runCatching { SwipeAction.valueOf(it) }.getOrDefault(SwipeAction.DELETE)
            } ?: SwipeAction.DELETE,
            autoBackupFrequency = prefs[Keys.AUTO_BACKUP_FREQ]?.let {
                runCatching { AutoBackupFrequency.valueOf(it) }.getOrDefault(AutoBackupFrequency.OFF)
            } ?: AutoBackupFrequency.OFF,
            lastSyncTimestamp = prefs[Keys.LAST_SYNC] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setPitchBlackAccent(accent: PitchBlackAccent) {
        context.dataStore.edit { it[Keys.PITCH_BLACK_ACCENT] = accent.name }
    }

    suspend fun setUseMaterial3Design(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_MATERIAL3] = enabled }
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

    suspend fun setShowQuickReplies(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_QUICK_REPLIES] = enabled }
    }

    suspend fun setSwapMenuBarPositions(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SWAP_MENU_BAR] = enabled }
    }

    suspend fun setGroupMessagingMode(mode: GroupMessagingMode) {
        context.dataStore.edit { it[Keys.GROUP_MESSAGING] = mode.name }
    }

    suspend fun setSwipeLeftAction(action: SwipeAction) {
        context.dataStore.edit { it[Keys.SWIPE_LEFT] = action.name }
    }

    suspend fun setSwipeRightAction(action: SwipeAction) {
        context.dataStore.edit { it[Keys.SWIPE_RIGHT] = action.name }
    }

    suspend fun setAutoBackupFrequency(freq: AutoBackupFrequency) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_FREQ] = freq.name }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC] = timestamp }
    }
}
