package com.nextext.sms.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nextext.sms.BuildConfig
import com.nextext.sms.data.local.BlockedSenderEntity
import com.nextext.sms.data.preferences.AutoBackupFrequency
import com.nextext.sms.data.preferences.GroupMessagingMode
import com.nextext.sms.data.preferences.PitchBlackAccent
import com.nextext.sms.data.preferences.SwipeAction
import com.nextext.sms.data.preferences.ThemeMode
import com.nextext.sms.data.preferences.UserPreferences
import com.nextext.sms.ui.components.LiquidGlassNavBottomBar
import com.nextext.sms.ui.components.LiquidGlassTabItem
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    isDefaultSmsApp: Boolean,
    blockedSenders: List<BlockedSenderEntity>,
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetPitchBlackAccent: (PitchBlackAccent) -> Unit,
    onSetUseMaterial3Design: (Boolean) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAutoDeleteOtps: (Boolean) -> Unit,
    onSetMutePromotions: (Boolean) -> Unit,
    onSetShowOtpNotification: (Boolean) -> Unit,
    onSetShowQuickReplies: (Boolean) -> Unit = {},
    onSetSwapMenuBarPositions: (Boolean) -> Unit = {},
    onSetGlassNavStyle: (Int, Int, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onSetGroupMessagingMode: (GroupMessagingMode) -> Unit = {},
    onSetSwipeLeftAction: (SwipeAction) -> Unit = {},
    onSetSwipeRightAction: (SwipeAction) -> Unit = {},
    onSetAutoBackupFrequency: (AutoBackupFrequency) -> Unit = {},
    onCleanOldOtpsNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onBackupToGoogleDrive: () -> Unit = {},
    onRestoreFromGoogleDrive: () -> Unit = {},
    onLinkDriveFolder: () -> Unit = {},
    onUnlinkDriveFolder: () -> Unit = {},
    onUnblockSender: (String) -> Unit,
    isCheckingUpdates: Boolean = false,
    onCheckForUpdates: () -> Unit = {},
    onSetReceiveDevUpdates: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var expandedSection by remember { mutableStateOf<String?>("theme") }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showSwipeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showGlassStyleDialog by remember { mutableStateOf(false) }

    // Everything below follows the active scheme, so Light actually reaches
    // this screen and Dark stays AMOLED black through the theme itself.
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. General
            item {
                SettingsRowItem(
                    icon = Icons.Default.Settings,
                    title = "General",
                    trailingText = if (isDefaultSmsApp) "Active" else "Set Default",
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        expandedSection = if (expandedSection == "general") null else "general"
                    }
                )
                AnimatedVisibility(visible = expandedSection == "general") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "100% on-device SMS processing • Network only for update checks",
                                style = MaterialTheme.typography.bodySmall,
                                color = subTextColor
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Default SMS App", color = textColor, fontSize = 14.sp)
                                Text(
                                    if (isDefaultSmsApp) "NexText is your active Default SMS app"
                                    else "Required for full SMS send/receive/delete",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor
                                )
                            }
                            if (!isDefaultSmsApp) {
                                Button(onClick = onRequestDefaultSms) { Text("Enable") }
                            }
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            // 2. Swap menu bar positions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetSwapMenuBarPositions(!preferences.swapMenuBarPositions) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Swap menu bar positions",
                        color = textColor,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = preferences.swapMenuBarPositions,
                        onCheckedChange = onSetSwapMenuBarPositions
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            // 3. Backup & restore
            item {
                SettingsRowItem(
                    icon = Icons.Default.CloudUpload,
                    title = "Backup & restore",
                    trailingText = when {
                        preferences.autoBackupFrequency != AutoBackupFrequency.OFF ->
                            "Auto: ${preferences.autoBackupFrequency.label}"
                        preferences.driveBackupTreeUri != null -> "Drive linked"
                        else -> "Drive & Local"
                    },
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        expandedSection = if (expandedSection == "backup") null else "backup"
                    }
                )
                AnimatedVisibility(visible = expandedSection == "backup") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Auto Backup Schedule", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AutoBackupFrequency.entries.forEach { freq ->
                                FilterChip(
                                    selected = preferences.autoBackupFrequency == freq,
                                    onClick = { onSetAutoBackupFrequency(freq) },
                                    label = { Text(freq.label) }
                                )
                            }
                        }
                        Text(
                            "Writes one JSON file into the linked Google Drive folder on this schedule, overwriting the previous one. The Drive app performs the sync, so Android's device-backup toggle is not involved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                        if (preferences.autoBackupFrequency != AutoBackupFrequency.OFF &&
                            preferences.driveBackupTreeUri == null
                        ) {
                            Text(
                                "No Drive folder linked yet — the schedule has nowhere to write.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE0A030)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("Google Drive Backup", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (preferences.driveBackupTreeUri == null) {
                            Text(
                                "Pick a folder in your Google Drive once. NexText keeps a single file there and overwrites it every run, so backups never stack — not on the phone, not in Drive.",
                                style = MaterialTheme.typography.bodySmall,
                                color = subTextColor
                            )
                            Button(onClick = onLinkDriveFolder) { Text("Choose Drive folder") }
                        } else {
                            val lastUpload = if (preferences.lastDriveBackupAt > 0L) {
                                java.text.SimpleDateFormat(
                                    "d MMM yyyy, h:mm a",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date(preferences.lastDriveBackupAt))
                            } else null
                            Text(
                                "Folder: ${preferences.driveBackupFolderName ?: "Linked folder"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                            Text(
                                if (lastUpload != null) "Last upload: $lastUpload" else "No upload yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = subTextColor
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = onBackupToGoogleDrive) { Text("Back up now") }
                                OutlinedButton(onClick = onRestoreFromGoogleDrive) { Text("Restore") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = onLinkDriveFolder) { Text("Change folder") }
                                OutlinedButton(onClick = onUnlinkDriveFolder) { Text("Unlink") }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("Local JSON Backup", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onExportBackup) { Text("Export Local") }
                            OutlinedButton(onClick = onImportBackup) { Text("Restore Local") }
                        }
                    }
                }
                HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
            }

            // 4. Message rules
            item {
                SettingsRowItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Message rules",
                    trailingText = if (blockedSenders.isEmpty()) "Auto-OTP" else "${blockedSenders.size} blocked",
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        expandedSection = if (expandedSection == "rules") null else "rules"
                    }
                )
                AnimatedVisibility(visible = expandedSection == "rules") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToggleRow(
                            title = "Auto-Clean OTPs after 24 hours",
                            subtitle = "Removes unstarred OTP messages older than 24h",
                            checked = preferences.autoDeleteOldOtps,
                            onCheckedChange = onSetAutoDeleteOtps
                        )
                        OutlinedButton(onClick = onCleanOldOtpsNow) {
                            Text("Clean Old OTPs Now")
                        }
                        if (blockedSenders.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Blocked Senders (${blockedSenders.size})",
                                color = textColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            blockedSenders.forEach { sender ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sender.normalizedAddress, color = subTextColor, fontSize = 14.sp)
                                    TextButton(onClick = { onUnblockSender(sender.normalizedAddress) }) {
                                        Text("Unblock")
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            // 5. Notifications
            item {
                SettingsRowItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    trailingText = if (preferences.mutePromotionsNotifications) "Promos muted" else "All",
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        expandedSection = if (expandedSection == "notifications") null else "notifications"
                    }
                )
                AnimatedVisibility(visible = expandedSection == "notifications") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 14.dp)
                    ) {
                        ToggleRow(
                            title = "Show 1-Tap Copy OTP Notification",
                            subtitle = "Adds a Copy OTP action button on verification alerts",
                            checked = preferences.showOtpCopyNotification,
                            onCheckedChange = onSetShowOtpNotification
                        )
                        ToggleRow(
                            title = "Mute Promotions Notifications",
                            subtitle = "Silently organize promotional offers without buzzing",
                            checked = preferences.mutePromotionsNotifications,
                            onCheckedChange = onSetMutePromotions
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            // 6. Group Messaging
            item {
                SettingsRowItem(
                    icon = Icons.Default.Groups,
                    title = "Group Messaging",
                    trailingText = preferences.groupMessagingMode.label,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = { showGroupDialog = true }
                )
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            // 7. Swipe actions
            item {
                SettingsRowItem(
                    icon = Icons.Default.SwapHoriz,
                    title = "Swipe actions",
                    trailingText = "${preferences.swipeLeftAction.label} / ${preferences.swipeRightAction.label}",
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = { showSwipeDialog = true }
                )
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            // 8. Theme
            item {
                val themeSummary = if (preferences.useMaterial3Design) {
                    "Material 3"
                } else {
                    "${preferences.themeMode.label} · ${preferences.pitchBlackAccent.label}"
                }
                SettingsRowItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    trailingText = themeSummary,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        expandedSection = if (expandedSection == "theme") null else "theme"
                    }
                )
                AnimatedVisibility(visible = expandedSection == "theme") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = preferences.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        ToggleRow(
                            title = "Material 3 Design",
                            subtitle = "Use Material 3 design instead of the NexText AMOLED theme",
                            checked = preferences.useMaterial3Design,
                            onCheckedChange = onSetUseMaterial3Design
                        )
                        if (!preferences.useMaterial3Design) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Accent colour & top glow",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PitchBlackAccent.entries.forEach { accent ->
                                    val isSelected = preferences.pitchBlackAccent == accent
                                    val swatchColor = Color(accent.primaryHex)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSetPitchBlackAccent(accent) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(swatchColor)
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 0.9f else 0.3f),
                                                        shape = CircleShape
                                                    )
                                            )
                                        },
                                        label = { Text(accent.label) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .border(
                                        width = 0.6.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { showGlassStyleDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Glass navigation bar size and style",
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Height ${preferences.glassBarHeightPercent}% • Width ${preferences.glassBarMaxWidthPercent}% • Frost ${preferences.glassBarFrostPercent}% • Rim ${preferences.glassBarRimBrightnessPercent}%",
                                        color = subTextColor,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            ToggleRow(
                                title = "Material You Dynamic Color",
                                subtitle = "Use wallpaper colors on Android 12+",
                                checked = preferences.dynamicColor,
                                onCheckedChange = onSetDynamicColor
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
            }

            // 9. Show quick replies
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetShowQuickReplies(!preferences.showQuickReplies) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Quickreply,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Show quick replies",
                        color = textColor,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = preferences.showQuickReplies,
                        onCheckedChange = onSetShowQuickReplies
                    )
                }
                HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
            }

            // 10. Check for updates
            item {
                SettingsRowItem(
                    icon = Icons.Default.SystemUpdate,
                    title = "Check for updates",
                    trailingText = when {
                        !com.nextext.sms.update.AppUpdater.isSupported -> "F-Droid"
                        isCheckingUpdates -> "Checking..."
                        else -> "v${BuildConfig.VERSION_NAME}"
                    },
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = onCheckForUpdates
                )
                HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
            }

            if (com.nextext.sms.update.AppUpdater.isSupported) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetReceiveDevUpdates(!preferences.receiveDevUpdates) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Receive Dev updates",
                                color = textColor,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Include pre-release Dev builds in update checks",
                                color = subTextColor,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = preferences.receiveDevUpdates,
                            onCheckedChange = onSetReceiveDevUpdates
                        )
                    }
                    HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
                }
            }

            // 11. About
            item {
                SettingsRowItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = { showAboutDialog = true }
                )
                HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
            }

            // 12. Version
            item {
                SettingsRowItem(
                    icon = Icons.Default.Verified,
                    title = "Version",
                    trailingText = BuildConfig.VERSION_NAME,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {}
                )
                HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About NexText") },
            text = {
                Text(
                    "NexText is a 100% on-device smart SMS organizer with AMOLED & Liquid Glass styling, automatic OTP detection, Finance passbook tracking, and Reminders. Your messages are parsed on your phone. The only network request the app makes is an update check against GitHub Releases — it carries no message data, no identifiers, and no analytics."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }

    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text("Group Messaging") },
            text = {
                Column {
                    GroupMessagingMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetGroupMessagingMode(mode)
                                    showGroupDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.groupMessagingMode == mode,
                                onClick = {
                                    onSetGroupMessagingMode(mode)
                                    showGroupDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(mode.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSwipeDialog) {
        AlertDialog(
            onDismissRequest = { showSwipeDialog = false },
            title = { Text("Swipe Actions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Swipe Left", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SwipeAction.entries.forEach { action ->
                            FilterChip(
                                selected = preferences.swipeLeftAction == action,
                                onClick = { onSetSwipeLeftAction(action) },
                                label = { Text(action.label) }
                            )
                        }
                    }
                    Text("Swipe Right", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SwipeAction.entries.forEach { action ->
                            FilterChip(
                                selected = preferences.swipeRightAction == action,
                                onClick = { onSetSwipeRightAction(action) },
                                label = { Text(action.label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwipeDialog = false }) { Text("Done") }
            }
        )
    }

    if (showGlassStyleDialog) {
        GlassNavStyleDialog(
            initialHeightPercent = preferences.glassBarHeightPercent,
            initialMaxWidthPercent = preferences.glassBarMaxWidthPercent,
            initialButtonSpacingDp = preferences.glassBarButtonSpacingDp,
            initialFrostPercent = preferences.glassBarFrostPercent,
            initialRimBrightnessPercent = preferences.glassBarRimBrightnessPercent,
            accentColor = MaterialTheme.colorScheme.primary,
            onDismiss = { showGlassStyleDialog = false },
            onConfirm = { h, w, s, o, r ->
                onSetGlassNavStyle(h, w, s, o, r)
                showGlassStyleDialog = false
            }
        )
    }
}

@Composable
private fun GlassNavStyleDialog(
    initialHeightPercent: Int,
    initialMaxWidthPercent: Int,
    initialButtonSpacingDp: Int,
    initialFrostPercent: Int,
    initialRimBrightnessPercent: Int,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Int, Int) -> Unit
) {
    var heightPercent by remember { mutableIntStateOf(initialHeightPercent) }
    var maxWidthPercent by remember { mutableIntStateOf(initialMaxWidthPercent) }
    var buttonSpacingDp by remember { mutableIntStateOf(initialButtonSpacingDp) }
    var frostPercent by remember { mutableIntStateOf(initialFrostPercent) }
    var rimBrightnessPercent by remember { mutableIntStateOf(initialRimBrightnessPercent) }
    var previewTab by remember { mutableIntStateOf(0) }

    val previewHazeState = remember { HazeState() }
    val previewBackdropLayer = rememberGraphicsLayer()
    var previewOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    val previewTabs = remember {
        listOf(
            LiquidGlassTabItem("Messages", Icons.Default.ChatBubble),
            LiquidGlassTabItem("Reminders", Icons.Default.Notifications),
            LiquidGlassTabItem("Finance", Icons.Default.CreditCard)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Glass navigation bar size and style",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(14.dp))

                // Live Preview Container with Blue + Coral cards behind the Glass Nav Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                            .onGloballyPositioned { coords ->
                                previewOriginInRoot = coords.positionInRoot()
                            }
                            .drawWithContent {
                                previewBackdropLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(previewBackdropLayer)
                            }
                            .haze(state = previewHazeState),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF5A9BFF))
                        ) {
                            Text(
                                text = "NexText\nNexText\nNexText",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFE57373))
                        ) {
                            Text(
                                text = "Aa Bb Cc\n123 456 789\n!@# \$%^",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    LiquidGlassNavBottomBar(
                        selectedTab = previewTab,
                        tabs = previewTabs,
                        accentColor = accentColor,
                        hazeState = previewHazeState,
                        backdropLayer = previewBackdropLayer,
                        backdropOriginInRoot = previewOriginInRoot,
                        heightPercent = heightPercent,
                        maxWidthPercent = maxWidthPercent,
                        buttonSpacingDp = buttonSpacingDp,
                        frostPercent = frostPercent,
                        rimBrightnessPercent = rimBrightnessPercent,
                        showComposeButton = false,
                        onSelectTab = { previewTab = it },
                        onComposeClick = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Buttons shown: ${previewTabs.size} of ${previewTabs.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(14.dp))

                GlassStyleSliderRow(
                    label = "Height",
                    valueText = "$heightPercent%",
                    value = heightPercent.toFloat(),
                    valueRange = 80f..135f,
                    onValueChange = { heightPercent = it.roundToInt() }
                )
                GlassStyleSliderRow(
                    label = "Maximum width",
                    valueText = "$maxWidthPercent%",
                    value = maxWidthPercent.toFloat(),
                    valueRange = 65f..100f,
                    onValueChange = { maxWidthPercent = it.roundToInt() }
                )
                GlassStyleSliderRow(
                    label = "Button spacing",
                    valueText = "${buttonSpacingDp}dp",
                    value = buttonSpacingDp.toFloat(),
                    valueRange = 0f..24f,
                    onValueChange = { buttonSpacingDp = it.roundToInt() }
                )
                GlassStyleSliderRow(
                    label = "Frost",
                    valueText = if (frostPercent == 0) "Clear" else "$frostPercent%",
                    value = frostPercent.toFloat(),
                    valueRange = 0f..100f,
                    onValueChange = { frostPercent = it.roundToInt() }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FrostSwatch(frostPercent = frostPercent, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = when {
                            frostPercent <= 10 -> "Crystal clear — pure refraction, no blur"
                            frostPercent <= 40 -> "Light frost — keeps labels legible"
                            frostPercent <= 75 -> "Frosted — glass reads as glass"
                            else -> "Heavy frost — strong blur and lift"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                GlassStyleSliderRow(
                    label = "Rim brightness",
                    valueText = "$rimBrightnessPercent%",
                    value = rimBrightnessPercent.toFloat(),
                    valueRange = 0f..100f,
                    onValueChange = { rimBrightnessPercent = it.roundToInt() }
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            heightPercent = 100
                            maxWidthPercent = 100
                            buttonSpacingDp = 4
                            frostPercent = 35
                            rimBrightnessPercent = 3
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset", fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = {
                            onConfirm(
                                heightPercent,
                                maxWidthPercent,
                                buttonSpacingDp,
                                frostPercent,
                                rimBrightnessPercent
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OK", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostSwatch(frostPercent: Int, modifier: Modifier = Modifier) {
    val frost = (frostPercent.coerceIn(0, 100) / 100f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2E7BF6)),
        contentAlignment = Alignment.Center
    ) {
        // Sharp text behind the glass: blurring it is the clearest read of frost.
        Text(
            text = "Aa",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.blur((frost * 6f).dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.White.copy(alpha = frost * 0.42f))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f + 0.32f * frost),
                    shape = RoundedCornerShape(12.dp)
                )
        )
    }
}

@Composable
private fun GlassStyleSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                color = subTextColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
