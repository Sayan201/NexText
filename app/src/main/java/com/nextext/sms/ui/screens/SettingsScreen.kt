package com.nextext.sms.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextext.sms.data.local.BlockedSenderEntity
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
    onSetUseMaterial3Design: (Boolean) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAutoDeleteOtps: (Boolean) -> Unit,
    onSetMutePromotions: (Boolean) -> Unit,
    onSetShowOtpNotification: (Boolean) -> Unit,
    onCleanOldOtpsNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onUnblockSender: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings & Backup", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "100% On-Device Privacy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "NexText does not request INTERNET permission. Your SMS, OTPs, and finances never leave your phone.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default SMS App", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (isDefaultSmsApp) "NexText is your active Default SMS app" else "Not yet set as Default SMS app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isDefaultSmsApp) {
                            Button(onClick = onRequestDefaultSms) { Text("Enable") }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
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
                        Spacer(Modifier.height(8.dp))
                        ToggleRow(
                            title = "Material 3 Design",
                            subtitle = "Enable Google Material 3 design and dynamic theme instead of SMS Organizer Pitch Black AMOLED style",
                            checked = preferences.useMaterial3Design,
                            onCheckedChange = onSetUseMaterial3Design
                        )
                        if (preferences.useMaterial3Design) {
                            ToggleRow(
                                title = "Material You Dynamic Color",
                                subtitle = "Use wallpaper colors on Android 12+",
                                checked = preferences.dynamicColor,
                                onCheckedChange = onSetDynamicColor
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("OTP & Notifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
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
                        ToggleRow(
                            title = "Auto-Clean OTPs after 24 hours",
                            subtitle = "Removes unstarred OTP messages older than 24h",
                            checked = preferences.autoDeleteOldOtps,
                            onCheckedChange = onSetAutoDeleteOtps
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = onCleanOldOtpsNow) {
                            Text("Clean Old OTPs Now")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Local Backup & Restore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Export or restore all organized messages, reminders, and sender rules to a local JSON file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onExportBackup) { Text("Export Backup") }
                            OutlinedButton(onClick = onImportBackup) { Text("Restore Backup") }
                        }
                    }
                }
            }

            if (blockedSenders.isNotEmpty()) {
                item {
                    Text(
                        "Blocked Senders (${blockedSenders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(blockedSenders, key = { it.normalizedAddress }) { blocked ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(blocked.displayAddress, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onUnblockSender(blocked.normalizedAddress) }) {
                                Text("Unblock")
                            }
                        }
                    }
                }
            }
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
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
