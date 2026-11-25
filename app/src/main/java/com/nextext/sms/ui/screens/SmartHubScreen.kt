package com.nextext.sms.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.nextext.sms.data.local.ReminderEntity
import com.nextext.sms.data.local.TransitOrderEntity
import com.nextext.sms.domain.model.ReminderKind
import com.nextext.sms.domain.model.TransitOrderKind
import com.nextext.sms.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHubScreen(
    reminders: List<ReminderEntity>,
    transitOrders: List<TransitOrderEntity>,
    useMaterial3Design: Boolean = false,
    onNavigateToMessages: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onToggleReminderDone: (Long, Boolean) -> Unit,
    onToggleReminderNotify: (Long, Boolean) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onAddReminder: (String, String, Double?, Long, ReminderKind) -> Unit,
    onOpenThread: (Long, String) -> Unit,
    onCopyCode: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = if (!useMaterial3Design) Color.Black else MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (!useMaterial3Design) Color.Black else MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = if (!useMaterial3Design) Color.Black else MaterialTheme.colorScheme.surface,
                        titleContentColor = if (!useMaterial3Design) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    title = { Text("Reminders & Trips", fontWeight = FontWeight.Bold) }
                )
                if (!useMaterial3Design) {
                    SmsOrganizerTopTabs(
                        selectedTab = 1,
                        onSelectTab = { tab ->
                            if (tab == 0) onNavigateToMessages()
                            else if (tab == 2) onNavigateToFinance()
                        },
                        unreadMessagesCount = 0
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add reminder")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) {
                    Text("Bills & Reminders (${reminders.count { !it.isCompleted }})")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) {
                    Text("Trips & Orders (${transitOrders.size})")
                }
            }

            if (selectedTab == 0) {
                if (reminders.isEmpty()) {
                    EmptySmartState(
                        title = "No bills or reminders",
                        subtitle = "Credit card bills, EMIs, recharges, and custom reminders appear here."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reminders, key = { it.id }) { item ->
                            ReminderCard(
                                item = item,
                                onToggleDone = { onToggleReminderDone(item.id, !item.isCompleted) },
                                onToggleNotify = { onToggleReminderNotify(item.id, !item.notifyEnabled) },
                                onDelete = { onDeleteReminder(item.id) },
                                onOpenSource = {
                                    item.threadId?.let { tid -> onOpenThread(tid, item.title) }
                                }
                            )
                        }
                    }
                }
            } else {
                if (transitOrders.isEmpty()) {
                    EmptySmartState(
                        title = "No trips or deliveries tracked",
                        subtitle = "IRCTC train PNRs, flight tickets, and courier tracking alerts are extracted automatically."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transitOrders, key = { it.id }) { item ->
                            TransitOrderCard(
                                item = item,
                                onCopy = {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Reference", item.referenceCode))
                                    onCopyCode(item.referenceCode)
                                },
                                onOpenSource = { onOpenThread(item.threadId, item.carrierOrOperator) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, notes, amount, daysFromNow ->
                val dueTs = System.currentTimeMillis() + daysFromNow * 86_400_000L
                onAddReminder(title, notes, amount, dueTs, ReminderKind.CUSTOM)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EmptySmartState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReminderCard(
    item: ReminderEntity,
    onToggleDone: () -> Unit,
    onToggleNotify: () -> Unit,
    onDelete: () -> Unit,
    onOpenSource: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.threadId != null, onClick = onOpenSource)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleDone) {
                Icon(
                    imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Mark complete",
                    tint = if (item.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.amount != null) {
                    Text(
                        text = Formatters.formatCurrency(item.amount, item.currency ?: "INR"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Due ${Formatters.formatDate(item.dueTimestamp)} • ${item.kind.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = onToggleNotify) {
                Icon(
                    imageVector = if (item.notifyEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = "Toggle notification"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete reminder")
            }
        }
    }
}

@Composable
private fun TransitOrderCard(
    item: TransitOrderEntity,
    onCopy: () -> Unit,
    onOpenSource: () -> Unit
) {
    val icon = when (item.kind) {
        TransitOrderKind.TRAIN -> Icons.Default.Train
        TransitOrderKind.FLIGHT -> Icons.Default.Flight
        TransitOrderKind.DELIVERY -> Icons.Default.LocalShipping
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSource)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.carrierOrOperator,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: ${item.status} • ${Formatters.formatRelativeTime(item.eventTimestamp)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy ${item.referenceCode}")
                }
            }
        }
    }
}

@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double?, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var daysText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Electricity Bill)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (optional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it.filter(Char::isDigit) },
                    label = { Text("Due in days") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isNotEmpty()) {
                        onSave(
                            cleanTitle,
                            notes.trim(),
                            amountText.toDoubleOrNull(),
                            (daysText.toLongOrNull() ?: 1L).coerceAtLeast(0L)
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
