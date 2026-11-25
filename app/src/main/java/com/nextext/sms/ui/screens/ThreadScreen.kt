package com.nextext.sms.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = Color.Black,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        if (effectiveTitle != effectiveAddress && effectiveAddress.isNotBlank()) {
                            Text(
                                text = effectiveAddress,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Conversation options")
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                            modifier = Modifier.background(Color(0xFF242426))
                        ) {
                            MessageCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("Move to ${cat.label}", color = Color.White) },
                                    onClick = {
                                        showTopMenu = false
                                        onOverrideCategory(cat)
                                    }
                                )
                            }
                            HorizontalDivider(color = Color(0xFF333336))
                            DropdownMenuItem(
                                text = { Text("Archive Conversation", color = Color.White) },
                                onClick = {
                                    showTopMenu = false
                                    onArchiveThread()
                                    onBack()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBlocked) "Unblock Sender" else "Block Sender", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color.White) },
                                onClick = {
                                    showTopMenu = false
                                    onToggleBlock()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Conversation", color = Color(0xFFFF5252)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
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
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Black),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    Photo4MessageBubble(
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
                // Photo 3: Send message bar with Message... prompt, attachment, schedule/sim, and send arrow
                Photo3SendBar(
                    draft = draft,
                    onDraftChange = { draft = it },
                    activeSims = activeSims,
                    selectedSubId = selectedSubId,
                    showSimMenu = showSimMenu,
                    onShowSimMenu = { showSimMenu = it },
                    onSelectSim = onSelectSim,
                    onAttachClick = { onShowMessage("Attachments coming soon") },
                    onSend = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            onSend(text)
                            draft = ""
                        }
                    }
                )
            } else {
                Surface(
                    color = Color(0xFF101216),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Replies are unavailable for alphanumeric sender IDs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Message input bar matching Photo 3:
 * Top: "Message..."
 * Bottom: Paperclip attachment (left), Calendar/Clock schedule & SIM (left), Send paper airplane (right).
 */
@Composable
private fun Photo3SendBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    activeSims: List<SimInfo>,
    selectedSubId: Int,
    showSimMenu: Boolean,
    onShowSimMenu: (Boolean) -> Unit,
    onSelectSim: (Int) -> Unit,
    onAttachClick: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color(0xFF101216),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Row 1: "Message..." text field
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 5,
                decorationBox = { innerTextField ->
                    if (draft.isEmpty()) {
                        Text(
                            text = "Message...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF757575),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )

            // Row 2: Attachment (left), Schedule/SIM (left), Send Arrow (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onAttachClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach",
                            tint = Color(0xFF7B9FE8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { onShowSimMenu(true) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Schedule / SIM",
                                tint = Color(0xFF7B9FE8),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (activeSims.isNotEmpty()) {
                            DropdownMenu(
                                expanded = showSimMenu,
                                onDismissRequest = { onShowSimMenu(false) },
                                modifier = Modifier.background(Color(0xFF242426))
                            ) {
                                activeSims.forEach { sim ->
                                    val isSelected = sim.subId == selectedSubId
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "SIM ${sim.slotIndex + 1} (${sim.displayName})" + (if (isSelected) " ✓" else ""),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                            )
                                        },
                                        onClick = {
                                            onSelectSim(sim.subId)
                                            onShowSimMenu(false)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (activeSims.size > 1) {
                        val currentSim = activeSims.firstOrNull { it.subId == selectedSubId } ?: activeSims.first()
                        Text(
                            text = "SIM ${currentSim.slotIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7B9FE8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onSend,
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (draft.isNotBlank()) Color.White else Color(0xFF555555),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Photo4MessageBubble(
    message: MessageEntity,
    onCopyOtp: (String) -> Unit,
    onCopyBody: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val alignment = if (message.isIncoming) Alignment.Start else Alignment.End
    val containerColor = if (message.isIncoming) {
        Color(0xFF16181D)
    } else {
        Color(0xFF223555) // Deep signature blue for sent messages
    }
    val textColor = Color.White

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
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Copy OTP ${message.otpCode}", color = Color.White)
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
                            color = Color(0xFF8E8E93)
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

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF242426))
            ) {
                DropdownMenuItem(
                    text = { Text("Copy Text", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White) },
                    onClick = {
                        showMenu = false
                        onCopyBody()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (message.isStarred) "Unstar" else "Star Message", color = Color.White) },
                    leadingIcon = {
                        Icon(
                            if (message.isStarred) Icons.Default.StarBorder else Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    onClick = {
                        showMenu = false
                        onToggleStar()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFFF5252)) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
