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
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextext.sms.BuildConfig
import com.nextext.sms.data.local.BlockedSenderEntity
import com.nextext.sms.data.preferences.AutoBackupFrequency
import com.nextext.sms.data.preferences.GroupMessagingMode
import com.nextext.sms.data.preferences.PitchBlackAccent
import com.nextext.sms.data.preferences.SwipeAction
import com.nextext.sms.data.preferences.ThemeMode
import com.nextext.sms.data.preferences.UserPreferences

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
    onSetGroupMessagingMode: (GroupMessagingMode) -> Unit = {},
    onSetSwipeLeftAction: (SwipeAction) -> Unit = {},
    onSetSwipeRightAction: (SwipeAction) -> Unit = {},
    onSetAutoBackupFrequency: (AutoBackupFrequency) -> Unit = {},
    onCleanOldOtpsNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onBackupToGoogleDrive: () -> Unit = {},
    onRestoreFromGoogleDrive: () -> Unit = {},
    onUnblockSender: (String) -> Unit
) {
    val context = LocalContext.current
    var expandedSection by remember { mutableStateOf<String?>("theme") }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showSwipeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val isPitchBlack = !preferences.useMaterial3Design && preferences.themeMode == ThemeMode.PITCH_BLACK
    val bgColor = if (isPitchBlack) Color.Black else MaterialTheme.colorScheme.background
    val textColor = if (isPitchBlack) Color.White else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isPitchBlack) Color(0xFF8E8E93) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (isPitchBlack) Color.White.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outlineVariant

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
                                "100% On-Device Privacy • No INTERNET permission",
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
                    trailingText = if (preferences.autoBackupFrequency == AutoBackupFrequency.OFF) "Drive & Local" else "Auto: ${preferences.autoBackupFrequency.label}",
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
                            "Saves a periodic backup locally (and syncs via Android's Google Drive device backup when enabled).",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Google Drive Backup", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onBackupToGoogleDrive) { Text("Back up to Drive") }
                            OutlinedButton(onClick = onRestoreFromGoogleDrive) { Text("Restore Drive") }
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
                val themeSummary = when {
                    preferences.useMaterial3Design -> "Material 3"
                    preferences.themeMode == ThemeMode.PITCH_BLACK -> "Midnight (${preferences.pitchBlackAccent.label})"
                    else -> preferences.themeMode.label
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
                            subtitle = "Use Material 3 design instead of Pitch Black AMOLED",
                            checked = preferences.useMaterial3Design,
                            onCheckedChange = onSetUseMaterial3Design
                        )
                        if (!preferences.useMaterial3Design) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Pitch Black Accent & Top Glow",
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
                                                        color = Color.White.copy(alpha = if (isSelected) 0.9f else 0.3f),
                                                        shape = CircleShape
                                                    )
                                            )
                                        },
                                        label = { Text(accent.label) }
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
                    trailingText = "GitHub",
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/Sayan201/NexText/releases")
                        )
                        runCatching { context.startActivity(intent) }
                    }
                )
                HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
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
                    "NexText is a 100% on-device smart SMS organizer with Pitch Black AMOLED & Liquid Glass styling, automatic OTP detection, Finance passbook tracking, and Reminders—with zero INTERNET permission."
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
