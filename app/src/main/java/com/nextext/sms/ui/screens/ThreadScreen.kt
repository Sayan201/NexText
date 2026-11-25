package com.nextext.sms.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nextext.sms.data.local.MessageEntity
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.domain.model.SimInfo
import com.nextext.sms.domain.parser.SenderIdUtils
import com.nextext.sms.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    threadId: Long,
    title: String,
    address: String,
    messages: List<MessageEntity>,
    isBlocked: Boolean,
    activeSims: List<SimInfo>,
    selectedSubId: Int,
    onSelectSim: (Int) -> Unit,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onMarkRead: () -> Unit,
    onToggleStar: (Long, Boolean) -> Unit,
    onDeleteMessage: (Long, Long?) -> Unit,
    onOverrideCategory: (MessageCategory) -> Unit,
    onToggleBlock: () -> Unit,
    onArchiveThread: () -> Unit,
    onDeleteThread: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var showTopMenu by remember { mutableStateOf(false) }
    var showSimMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(threadId, messages.size) {
        onMarkRead()
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    val effectiveAddress = messages.lastOrNull()?.address ?: address
    val effectiveTitle = messages.lastOrNull()?.contactName?.takeIf { it.isNotBlank() } ?: title
    val canReply = !SenderIdUtils.isShortCodeOrAlphanumericSender(effectiveAddress) ||
        effectiveAddress.any(Char::isDigit)

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = effectiveTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (effectiveTitle != effectiveAddress) {
                            Text(
                                text = effectiveAddress,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Conversation options")
                        }
                        DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                            MessageCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("Move to ${cat.label}") },
                                    onClick = {
                                        showTopMenu = false
                                        onOverrideCategory(cat)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Archive Conversation") },
                                onClick = {
                                    showTopMenu = false
                                    onArchiveThread()
                                    onBack()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBlocked) "Unblock Sender" else "Block Sender") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onToggleBlock()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Conversation") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    onDeleteThread()
                                    onBack()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onCopyOtp = { otp ->
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("OTP", otp))
                            onShowMessage("OTP $otp copied")
                        },
                        onCopyBody = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("SMS", msg.body))
                            onShowMessage("Message copied")
                        },
                        onToggleStar = { onToggleStar(msg.id, !msg.isStarred) },
                        onDelete = { onDeleteMessage(msg.id, msg.systemSmsId) }
                    )
                }
            }

            if (canReply) {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (activeSims.size > 1) {
                            val currentSim = activeSims.firstOrNull { it.subId == selectedSubId }
                                ?: activeSims.first()
                            Box {
                                AssistChip(
                                    onClick = { showSimMenu = true },
                                    label = { Text("SIM ${currentSim.slotIndex + 1}: ${currentSim.displayName}") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.SimCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                                DropdownMenu(
                                    expanded = showSimMenu,
                                    onDismissRequest = { showSimMenu = false }
                                ) {
                                    activeSims.forEach { sim ->
                                        DropdownMenuItem(
                                            text = { Text("SIM ${sim.slotIndex + 1} (${sim.displayName})") },
                                            onClick = {
                                                onSelectSim(sim.subId)
                                                showSimMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                placeholder = { Text("Text message") },
                                modifier = Modifier.weight(1f),
                                maxLines = 4
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = {
                                    val text = draft.trim()
                                    if (text.isNotEmpty()) {
                                        onSend(text)
                                        draft = ""
                                    }
                                },
                                enabled = draft.isNotBlank()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            } else {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Replies are unavailable for alphanumeric sender IDs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageEntity,
    onCopyOtp: (String) -> Unit,
    onCopyBody: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val alignment = if (message.isIncoming) Alignment.Start else Alignment.End
    val containerColor = if (message.isIncoming) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = if (message.isIncoming) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!message.smartBadge.isNullOrBlank()) {
                        Text(
                            text = message.smartBadge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )

                    if (!message.otpCode.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { onCopyOtp(message.otpCode) }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Copy OTP ${message.otpCode}")
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = Formatters.formatDateTime(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        if (message.isStarred) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Starred",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Copy Text") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onCopyBody()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (message.isStarred) "Unstar" else "Star Message") },
                    leadingIcon = {
                        Icon(
                            if (message.isStarred) Icons.Default.StarBorder else Icons.Default.Star,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showMenu = false
                        onToggleStar()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
