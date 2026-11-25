package com.nextext.sms.data.backup

import android.content.Context
import android.net.Uri
import com.nextext.sms.data.local.BlockedSenderEntity
import com.nextext.sms.data.local.CategoryOverrideEntity
import com.nextext.sms.data.local.MessageEntity
import com.nextext.sms.data.local.NexTextDatabase
import com.nextext.sms.data.local.ReminderEntity
import com.nextext.sms.data.local.TransactionEntity
import com.nextext.sms.data.local.TransitOrderEntity
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.domain.model.ReminderKind
import com.nextext.sms.domain.parser.SmsParsingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Serializable
data class NexTextBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val messages: List<BackupMessage> = emptyList(),
    val reminders: List<BackupReminder> = emptyList(),
    val blockedSenders: List<BackupBlockedSender> = emptyList(),
    val categoryOverrides: List<BackupCategoryOverride> = emptyList()
)

@Serializable
data class BackupMessage(
    val systemSmsId: Long? = null,
    val threadId: Long,
    val address: String,
    val normalizedAddress: String,
    val contactName: String? = null,
    val body: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val isRead: Boolean,
    val subId: Int = -1,
    val category: String,
    val isStarred: Boolean = false,
    val isArchived: Boolean = false
)

@Serializable
data class BackupReminder(
    val title: String,
    val description: String,
    val amount: Double? = null,
    val currency: String? = null,
    val dueTimestamp: Long,
    val kind: String,
    val isCompleted: Boolean = false,
    val notifyEnabled: Boolean = true
)

@Serializable
data class BackupBlockedSender(
    val normalizedAddress: String,
    val displayAddress: String,
    val blockedAt: Long
)

@Serializable
data class BackupCategoryOverride(
    val normalizedAddress: String,
    val category: String
)

class BackupManager(
    private val context: Context,
    private val database: NexTextDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val messages = database.messageDao().getAllActiveMessages().map {
                BackupMessage(
                    systemSmsId = it.systemSmsId,
                    threadId = it.threadId,
                    address = it.address,
                    normalizedAddress = it.normalizedAddress,
                    contactName = it.contactName,
                    body = it.body,
                    timestamp = it.timestamp,
                    isIncoming = it.isIncoming,
                    isRead = it.isRead,
                    subId = it.subId,
                    category = it.category.name,
                    isStarred = it.isStarred,
                    isArchived = it.isArchived
                )
            }
            val reminders = database.reminderDao().getAllReminders().map {
                BackupReminder(
                    title = it.title,
                    description = it.description,
                    amount = it.amount,
                    currency = it.currency,
                    dueTimestamp = it.dueTimestamp,
                    kind = it.kind.name,
                    isCompleted = it.isCompleted,
                    notifyEnabled = it.notifyEnabled
                )
            }
            val blocked = database.senderRuleDao().getAllBlockedSenders()
            val overrides = database.senderRuleDao().getAllOverrides().map {
                BackupCategoryOverride(it.normalizedAddress, it.category.name)
            }

            val payload = NexTextBackupPayload(
                messages = messages,
                reminders = reminders,
                blockedSenders = blocked.map {
                    BackupBlockedSender(it.normalizedAddress, it.displayAddress, it.blockedAt)
                },
                categoryOverrides = overrides
            )
            val serialized = json.encodeToString(payload)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(serialized.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Unable to open destination file for backup")
            messages.size
        }
    }

    suspend fun importFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Unable to read backup file")

            val payload = json.decodeFromString<NexTextBackupPayload>(content)

            payload.blockedSenders.forEach {
                database.senderRuleDao().blockSender(
                    BlockedSenderEntity(it.normalizedAddress, it.displayAddress, it.blockedAt)
                )
            }
            payload.categoryOverrides.forEach {
                val cat = runCatching { MessageCategory.valueOf(it.category) }
                    .getOrDefault(MessageCategory.PERSONAL)
                database.senderRuleDao().setCategoryOverride(
                    CategoryOverrideEntity(it.normalizedAddress, cat)
                )
            }

            var importedCount = 0
            val messageDao = database.messageDao()
            val txDao = database.transactionDao()
            val transitDao = database.transitOrderDao()

            for (msg in payload.messages) {
                val existing = msg.systemSmsId?.let { messageDao.findBySystemSmsId(it) }
                    ?: messageDao.findDuplicate(msg.threadId, msg.body, msg.timestamp)
                if (existing != null) continue

                val category = runCatching { MessageCategory.valueOf(msg.category) }
                    .getOrDefault(MessageCategory.PERSONAL)
                val parsed = SmsParsingEngine.parse(
                    address = msg.address,
                    body = msg.body,
                    timestamp = msg.timestamp,
                    isKnownContact = !msg.contactName.isNullOrBlank(),
                    categoryOverride = category
                )

                val badge = buildSmartBadge(parsed)
                val rowId = messageDao.insertMessage(
                    MessageEntity(
                        systemSmsId = msg.systemSmsId,
                        threadId = msg.threadId,
                        address = msg.address,
                        normalizedAddress = msg.normalizedAddress,
                        contactName = msg.contactName,
                        body = msg.body,
                        timestamp = msg.timestamp,
                        isIncoming = msg.isIncoming,
                        isRead = msg.isRead,
                        subId = msg.subId,
                        category = category,
                        isStarred = msg.isStarred,
                        isArchived = msg.isArchived,
                        otpCode = parsed.otp?.code,
                        otpService = parsed.otp?.service,
                        smartBadge = badge
                    )
                )

                parsed.transaction?.let { tx ->
                    txDao.insert(
                        TransactionEntity(
                            messageId = rowId,
                            threadId = msg.threadId,
                            address = msg.address,
                            amount = tx.amount,
                            currency = tx.currency,
                            type = tx.type,
                            institution = tx.institution,
                            accountLast4 = tx.accountLast4,
                            merchant = tx.merchant,
                            balanceAfter = tx.balanceAfter,
                            spendCategory = tx.spendCategory,
                            timestamp = msg.timestamp
                        )
                    )
                }

                parsed.transitOrder?.let { tr ->
                    transitDao.insert(
                        TransitOrderEntity(
                            messageId = rowId,
                            threadId = msg.threadId,
                            kind = tr.kind,
                            referenceCode = tr.referenceCode,
                            carrierOrOperator = tr.carrierOrOperator,
                            summary = tr.summary,
                            status = tr.status,
                            eventTimestamp = tr.eventTimestamp ?: msg.timestamp
                        )
                    )
                }
                importedCount++
            }

            payload.reminders.forEach { rem ->
                val kind = runCatching { ReminderKind.valueOf(rem.kind) }.getOrDefault(ReminderKind.CUSTOM)
                database.reminderDao().insert(
                    ReminderEntity(
                        title = rem.title,
                        description = rem.description,
                        amount = rem.amount,
                        currency = rem.currency,
                        dueTimestamp = rem.dueTimestamp,
                        kind = kind,
                        isCompleted = rem.isCompleted,
                        notifyEnabled = rem.notifyEnabled
                    )
                )
            }

            importedCount
        }
    }
}

fun buildSmartBadge(parsed: com.nextext.sms.domain.model.ParsedSmsResult): String? {
    parsed.otp?.let { return "OTP: ${it.code}" }
    parsed.transaction?.let { tx ->
        val symbol = when (tx.currency) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "${tx.currency} "
        }
        val formatted = runCatching {
            NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
                maximumFractionDigits = if (tx.amount % 1.0 == 0.0) 0 else 2
            }.format(tx.amount)
        }.getOrDefault(tx.amount.toString())
        return "$symbol$formatted ${tx.type.label}"
    }
    parsed.transitOrder?.let { return "${it.kind.label}: ${it.status}" }
    parsed.reminder?.let { return "${it.kind.label} Due" }
    return null
}
