package com.nextext.sms.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextext.sms.data.local.MessageEntity
import com.nextext.sms.data.local.ThreadSummary
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.ui.util.Formatters
import com.nextext.sms.ui.viewmodel.InboxFilter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    threads: List<ThreadSummary>,
    selectedFilter: InboxFilter,
    unreadCounts: Map<MessageCategory, Int>,
    recentOtp: MessageEntity?,
    isSyncing: Boolean,
    isDefaultSmsApp: Boolean,
    hasPermissions: Boolean,
    unreadOnly: Boolean,
    useMaterial3Design: Boolean,
    onSelectFilter: (InboxFilter) -> Unit,
    onOpenThread: (ThreadSummary) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCompose: () -> Unit,
    onSyncClick: () -> Unit,
    onToggleUnreadOnly: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    onRequestPermissions: () -> Unit,
    onArchiveThread: (Long, Boolean) -> Unit,
    onDeleteThread: (Long) -> Unit,
    onMarkRead: (Long) -> Unit,
    onCopyOtp: (String) -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToFinance: () -> Unit
) {
    val context = LocalContext.current
    var showTopOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = if (!useMaterial3Design) Color.Black else MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (!useMaterial3Design) Color.Black else MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (!useMaterial3Design) Color.Black else MaterialTheme.colorScheme.surface,
                        titleContentColor = if (!useMaterial3Design) Color.White else MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = if (!useMaterial3Design) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Text(
                            text = "NexText",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        Box {
                            IconButton(onClick = { showTopOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }

                            // 3-dot overflow menu with all 8 options from Photo 5
                            Photo5TopOverflowMenu(
                                expanded = showTopOverflowMenu,
                                onDismissRequest = { showTopOverflowMenu = false },
                                unreadOnly = unreadOnly,
                                onToggleUnreadOnly = {
                                    onToggleUnreadOnly()
                                    showTopOverflowMenu = false
                                },
                                onMarkAllAsRead = {
                                    showTopOverflowMenu = false
                                    onMarkAllAsRead()
                                },
                                onRefreshMessages = {
                                    showTopOverflowMenu = false
                                    onSyncClick()
                                },
                                onArchive = {
                                    showTopOverflowMenu = false
                                    onSelectFilter(InboxFilter.ARCHIVED)
                                },
                                onBlocked = {
                                    showTopOverflowMenu = false
                                    onOpenSettings()
                                },
                                onInviteFriends = {
                                    showTopOverflowMenu = false
                                    shareInvite(context)
                                },
                                onFeedback = {
                                    showTopOverflowMenu = false
                                    openFeedback(context)
                                },
                                onSettings = {
                                    showTopOverflowMenu = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                )

                // Photo 4 Top Tabs: Messages | Reminders | Finance
                if (!useMaterial3Design) {
                    SmsOrganizerTopTabs(
                        selectedTab = 0, // 0 = Messages
                        onSelectTab = { tab ->
                            when (tab) {
                                1 -> onNavigateToReminders()
                                2 -> onNavigateToFinance()
                            }
                        },
                        unreadMessagesCount = unreadCounts.values.sum()
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isSyncing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF5B86E5)
                    )
                }

                if (!hasPermissions) {
                    OnboardingBanner(
                        title = "SMS & Contacts Permission Needed",
                        subtitle = "Grant SMS permissions to organize messages 100% on-device.",
                        actionLabel = "Grant Access",
                        onClick = onRequestPermissions
                    )
                } else if (!isDefaultSmsApp) {
                    OnboardingBanner(
                        title = "Set NexText as Default SMS App",
                        subtitle = "Required to receive, delete, and manage SMS directly.",
                        actionLabel = "Set Default",
                        onClick = onRequestDefaultSms
                    )
                }

                if (recentOtp != null && !recentOtp.otpCode.isNullOrBlank()) {
                    RecentOtpCard(
                        message = recentOtp,
                        onCopy = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("OTP", recentOtp.otpCode))
                            onCopyOtp(recentOtp.otpCode)
                        }
                    )
                }

                if (useMaterial3Design) {
                    CategoryFilterStripM3(
                        selectedFilter = selectedFilter,
                        unreadCounts = unreadCounts,
                        onSelectFilter = onSelectFilter
                    )
                }

                if (threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF5B86E5).copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (unreadOnly) {
                                    "No unread messages"
                                } else {
                                    "No messages in ${selectedFilter.name.lowercase().replaceFirstChar { it.uppercase() }}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = if (!useMaterial3Design) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Everything is organized locally on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9E9E)
                            )
                            if (hasPermissions) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = onSyncClick) {
                                    Text("Sync Messages")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(threads, key = { it.threadId }) { thread ->
                            Photo4ThreadListRow(
                                thread = thread,
                                useMaterial3 = useMaterial3Design,
                                onClick = { onOpenThread(thread) },
                                onArchive = { onArchiveThread(thread.threadId, !thread.isArchived) },
                                onDelete = { onDeleteThread(thread.threadId) },
                                onMarkRead = { onMarkRead(thread.threadId) }
                            )
                            if (!useMaterial3Design) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = Color(0xFF141416)
                                )
                            }
                        }
                    }
                }
            }

            // Photo 4 Floating Action Button (FAB) at bottom-right above the floating capsule
            FloatingActionButton(
                onClick = onOpenCompose,
                shape = CircleShape,
                containerColor = Color(0xFF5B86E5),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = if (!useMaterial3Design) 80.dp else 16.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "New message")
            }

            // WhatsApp flowering (floating capsule) + Twitter liquid glass bottom bar
            if (!useMaterial3Design) {
                LiquidGlassFloatingBottomBar(
                    selectedFilter = selectedFilter,
                    unreadCounts = unreadCounts,
                    onSelectFilter = onSelectFilter,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }
        }
    }
}

/**
 * Top 3-dot overflow menu containing all 8 options from Photo 5:
 * 1. Unread only [Checkbox]
 * 2. Mark All As Read
 * 3. Refresh Messages
 * 4. Archive
 * 5. Blocked
 * 6. Invite friends
 * 7. Feedback
 * 8. Settings
 */
@Composable
private fun Photo5TopOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    unreadOnly: Boolean,
    onToggleUnreadOnly: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onRefreshMessages: () -> Unit,
    onArchive: () -> Unit,
    onBlocked: () -> Unit,
    onInviteFriends: () -> Unit,
    onFeedback: () -> Unit,
    onSettings: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(225.dp)
            .background(Color(0xFF242426))
            .border(0.5.dp, Color(0xFF333336), RoundedCornerShape(12.dp))
    ) {
        // 1. Unread only
        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Unread only", color = Color.White, fontSize = 15.sp)
                    Checkbox(
                        checked = unreadOnly,
                        onCheckedChange = { onToggleUnreadOnly() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF5B86E5),
                            uncheckedColor = Color(0xFF9E9E9E),
                            checkmarkColor = Color.White
                        )
                    )
                }
            },
            leadingIcon = {
                Icon(Icons.Default.MailOutline, contentDescription = null, tint = Color.White)
            },
            onClick = onToggleUnreadOnly
        )

        // 2. Mark All As Read
        DropdownMenuItem(
            text = { Text("Mark All As Read", color = Color.White, fontSize = 15.sp) },
            leadingIcon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
            },
            onClick = onMarkAllAsRead
        )

        // 3. Refresh Messages
        DropdownMenuItem(
            text = { Text("Refresh Messages", color = Color.White, fontSize = 15.sp) },
            leadingIcon = {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            },
            onClick = onRefreshMessages
        )

        // 4. Archive
        DropdownMenuItem(
            text = { Text("Archive", color = Color.White, fontSize = 15.sp) },
            leadingIcon = {
                Icon(Icons.Default.Archive, contentDescription = null, tint = Color.White)
            },
            onClick = onArchive
        )

        // 5. Blocked
        DropdownMenuItem(
            text = { Text("Blocked", color = Color.White, fontSize = 15.sp) },
            leadingIcon = {
                Icon(Icons.Default.Block, contentDescription = null, tint = Color.White)
            },
            onClick = onBlocked
        )

        // 6. Invite friends
        DropdownMenuItem(
            text = { Text("Invite friends", color = Color.White, fontSize = 15.sp) },
            leadingIcon = {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
            },
            onClick = onInviteFriends
        )

        // 7. Feedback
        DropdownMenuItem(
            text = { Text("Feedback", color = Color.White, fontSize = 15.sp) },
            leadingIcon = {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = Color.White)
            },
            onClick = onFeedback
        )

        // 8. Settings
        DropdownMenuItem(
            text = { Text("Settings", color = Color.White, fontSize = 15.sp) },
            leadingIcon = {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
            },
            onClick = onSettings
        )
    }
}

/**
 * Photo 4 SMS Organizer Top Tabs: Messages | Reminders | Finance
 */
@Composable
fun SmsOrganizerTopTabs(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    unreadMessagesCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val tabs = listOf(
            Triple(0, "Messages", Icons.Default.ChatBubble),
            Triple(1, "Reminders", Icons.Default.Notifications),
            Triple(2, "Finance", Icons.Default.CreditCard)
        )

        tabs.forEach { (index, label, icon) ->
            val isSelected = selectedTab == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectTab(index) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color(0xFF5B86E5) else Color(0xFF888888),
                        modifier = Modifier.size(20.dp)
                    )
                    if (index == 0 && unreadMessagesCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53E3E))
                                .align(Alignment.TopEnd)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF5B86E5) else Color(0xFF888888)
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(2.5.dp)
                        .background(if (isSelected) Color(0xFF5B86E5) else Color.Transparent)
                )
            }
        }
    }
}

/**
 * WhatsApp flowering (floating capsule) + Twitter liquid glass bottom bar
 * Translucent frosted glassmorphism with specular border highlight sheen and pill indicators.
 */
@Composable
fun LiquidGlassFloatingBottomBar(
    selectedFilter: InboxFilter,
    unreadCounts: Map<MessageCategory, Int>,
    onSelectFilter: (InboxFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(InboxFilter.PERSONAL, "Personal", unreadCounts[MessageCategory.PERSONAL] ?: 0),
        Triple(InboxFilter.TRANSACTIONS, "Transactions", unreadCounts[MessageCategory.TRANSACTIONS] ?: 0),
        Triple(InboxFilter.PROMOTIONS, "Promotions", unreadCounts[MessageCategory.PROMOTIONS] ?: 0),
        Triple(InboxFilter.STARRED, "Starred", 0)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xCC11141B),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x66FFFFFF), // Specular rim highlight (Twitter liquid glass)
                        Color(0x15FFFFFF),
                        Color(0x05FFFFFF)
                    )
                )
            ),
            shadowElevation = 14.dp,
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xD9181D26),
                                Color(0xF20B0E14)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (filter, label, unread) ->
                    val isSelected = selectedFilter == filter

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (isSelected) {
                                    // WhatsApp active pill container highlight
                                    Color(0xFF2C3E55)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { onSelectFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFB0B3B8)
                            )
                            if (unread > 0) {
                                Spacer(Modifier.width(6.dp))
                                // WhatsApp / SMS Organizer red unread dot (Photo 4)
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53E3E))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thread row matching Photo 4:
 * Deep muted avatar circles with bold initials, crisp white sender title, 12h/date time,
 * light gray preview, and blue pill unread count badge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Photo4ThreadListRow(
    thread: ThreadSummary,
    useMaterial3: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onMarkRead: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val displayName = thread.contactName?.takeIf { it.isNotBlank() } ?: thread.address
    val isUnread = thread.unreadCount > 0

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .background(if (!useMaterial3) Color.Black else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo 4 circular avatar with initial
            Photo4Avatar(name = displayName, seed = thread.threadId)

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = Formatters.formatRelativeTime(thread.lastTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnread) Color(0xFF5B86E5) else Color(0xFF888888)
                    )
                }

                Spacer(Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.lastBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUnread) Color(0xFFDDDDDD) else Color(0xFF8E8E93),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnread) {
                        Spacer(Modifier.width(8.dp))
                        // Photo 4 blue pill unread badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF3B5998))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = thread.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (isUnread) {
                DropdownMenuItem(
                    text = { Text("Mark as Read") },
                    leadingIcon = { Icon(Icons.Default.MarkEmailRead, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onMarkRead()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(if (thread.isArchived) "Unarchive" else "Archive") },
                leadingIcon = {
                    Icon(
                        if (thread.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = null
                    )
                },
                onClick = {
                    showMenu = false
                    onArchive()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete Conversation") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun Photo4Avatar(name: String, seed: Long) {
    // Deep rich palette seen in SMS Organizer (Photo 4)
    val palette = listOf(
        Color(0xFF162D4A), // Deep Navy
        Color(0xFF2E1A3E), // Deep Violet
        Color(0xFF123438), // Deep Teal
        Color(0xFF382618), // Deep Bronze
        Color(0xFF3A1822), // Deep Wine
        Color(0xFF1B3D2B)  // Deep Forest
    )
    val bg = palette[(abs(seed) % palette.size).toInt()]
    val initial = Formatters.initialsFor(name).take(1)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE5E7EB)
        )
    }
}

@Composable
private fun CategoryFilterStripM3(
    selectedFilter: InboxFilter,
    unreadCounts: Map<MessageCategory, Int>,
    onSelectFilter: (InboxFilter) -> Unit
) {
    val chips = listOf(
        Triple(InboxFilter.PERSONAL, "Personal", unreadCounts[MessageCategory.PERSONAL] ?: 0),
        Triple(InboxFilter.TRANSACTIONS, "Transactions", unreadCounts[MessageCategory.TRANSACTIONS] ?: 0),
        Triple(InboxFilter.PROMOTIONS, "Promotions", unreadCounts[MessageCategory.PROMOTIONS] ?: 0),
        Triple(InboxFilter.SPAM, "Spam", unreadCounts[MessageCategory.SPAM] ?: 0),
        Triple(InboxFilter.STARRED, "Starred", 0),
        Triple(InboxFilter.ARCHIVED, "Archived", 0)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (filter, label, count) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onSelectFilter(filter) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label)
                        if (count > 0) {
                            Spacer(Modifier.width(6.dp))
                            Badge { Text(if (count > 99) "99+" else count.toString()) }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun OnboardingBanner(
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onClick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF5B86E5))
            ) {
                Text(actionLabel, color = Color.White)
            }
        }
    }
}

@Composable
private fun RecentOtpCard(
    message: MessageEntity,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162534))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.otpService?.let { "Recent OTP • $it" } ?: "Recent Verification Code",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8BB8FF)
                )
                Text(
                    text = message.otpCode.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Button(
                onClick = onCopy,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF5B86E5))
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy OTP")
            }
        }
    }
}

private fun shareInvite(context: Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out NexText - Privacy-focused, 100% on-device SMS Organizer: https://github.com/Sayan201/NexText"
            )
        }
        context.startActivity(Intent.createChooser(intent, "Invite friends to NexText"))
    }
}

private fun openFeedback(context: Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Sayan201/NexText/issues"))
        context.startActivity(intent)
    }
}
