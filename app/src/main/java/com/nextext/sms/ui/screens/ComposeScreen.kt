package com.nextext.sms.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextext.sms.domain.model.ContactOption
import com.nextext.sms.domain.model.SimInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    initialAddress: String = "",
    initialBody: String = "",
    contacts: List<ContactOption>,
    activeSims: List<SimInfo>,
    selectedSubId: Int,
    onSelectSim: (Int) -> Unit,
    onSearchContacts: (String) -> Unit,
    onBack: () -> Unit,
    onSend: (String, String) -> Unit
) {
    var recipient by remember { mutableStateOf(initialAddress) }
    var message by remember { mutableStateOf(initialBody) }
    var showSimMenu by remember { mutableStateOf(false) }

    LaunchedEffect(recipient) {
        if (recipient.isEmpty()) {
            kotlinx.coroutines.delay(160)
        }
        onSearchContacts(recipient)
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("New Message", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("To (Name or Phone Number)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(contacts, key = { "${it.name}-${it.phoneNumber}" }) { contact ->
                    ListItem(
                        headlineContent = { Text(contact.name) },
                        supportingContent = { Text(contact.phoneNumber) },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.clickable {
                            recipient = contact.phoneNumber
                        }
                    )
                    HorizontalDivider()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (activeSims.size > 1) {
                    val currentSim = activeSims.firstOrNull { it.subId == selectedSubId } ?: activeSims.first()
                    Box {
                        AssistChip(
                            onClick = { showSimMenu = true },
                            label = { Text("SIM ${currentSim.slotIndex + 1}: ${currentSim.displayName}") },
                            leadingIcon = {
                                Icon(Icons.Default.SimCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        DropdownMenu(expanded = showSimMenu, onDismissRequest = { showSimMenu = false }) {
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
                    Spacer(Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("Write your message...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (recipient.isNotBlank() && message.isNotBlank()) {
                                onSend(recipient.trim(), message.trim())
                            }
                        },
                        enabled = recipient.isNotBlank() && message.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
