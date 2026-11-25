package com.nextext.sms.data.repository

import android.Manifest
import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.nextext.sms.data.backup.buildSmartBadge
import com.nextext.sms.data.local.BlockedSenderEntity
import com.nextext.sms.data.local.CategoryOverrideEntity
import com.nextext.sms.data.local.MessageEntity
import com.nextext.sms.data.local.NexTextDatabase
import com.nextext.sms.data.local.ReminderEntity
import com.nextext.sms.data.local.ThreadSummary
import com.nextext.sms.data.local.TransactionEntity
import com.nextext.sms.data.local.TransitOrderEntity
import com.nextext.sms.data.preferences.UserPreferencesRepository
import com.nextext.sms.domain.model.ContactOption
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.domain.model.ReminderKind
import com.nextext.sms.domain.model.SimInfo
import com.nextext.sms.domain.parser.SenderIdUtils
import com.nextext.sms.domain.parser.SmsParsingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SmsRepository(
    private val context: Context,
    private val database: NexTextDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val messageDao = database.messageDao()
    private val transactionDao = database.transactionDao()
    private val reminderDao = database.reminderDao()
    private val transitOrderDao = database.transitOrderDao()
    private val senderRuleDao = database.senderRuleDao()

    fun observeAllThreads(): Flow<List<ThreadSummary>> = messageDao.observeAllThreads()

    fun observeThreadMessages(threadId: Long): Flow<List<MessageEntity>> =
        messageDao.observeThreadMessages(threadId)

    fun observeUnreadCount(category: MessageCategory): Flow<Int> =
        messageDao.observeUnreadCountByCategory(category)

    fun observeLatestOtp(): Flow<MessageEntity?> = messageDao.observeLatestOtp()

    fun observeAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.observeAllTransactions()

    fun observeAllReminders(): Flow<List<ReminderEntity>> = reminderDao.observeAllReminders()

    fun observeAllTransitOrders(): Flow<List<TransitOrderEntity>> = transitOrderDao.observeAll()

    fun observeBlockedSenders(): Flow<List<BlockedSenderEntity>> =
        senderRuleDao.observeBlockedSenders()

    fun observeIsBlocked(address: String): Flow<Boolean> =
        senderRuleDao.observeIsBlocked(normalizeAddress(address))

    fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    fun hasSmsPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun syncSystemSms(): Int = withContext(Dispatchers.IO) {
        if (!hasSmsPermissions()) return@withContext 0

        val blockedSet = senderRuleDao.getBlockedAddresses().toSet()
        val overrides = senderRuleDao.getAllOverrides()
            .associate { it.normalizedAddress to it.category }

        val contactCache = mutableMapOf<String, String?>()
        var syncedCount = 0

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.SUBSCRIPTION_ID
        )

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC LIMIT 1500"
        )?.use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val idxThread = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val idxAddr = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val idxBody = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val idxDate = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val idxType = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val idxRead = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            val idxSubId = cursor.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

            while (cursor.moveToNext()) {
                val systemId = cursor.getLong(idxId)
                if (messageDao.findBySystemSmsId(systemId) != null) continue

                val threadId = cursor.getLong(idxThread)
                val address = cursor.getString(idxAddr)?.takeIf { it.isNotBlank() } ?: "Unknown"
                val body = cursor.getString(idxBody) ?: ""
                val timestamp = cursor.getLong(idxDate)
                val type = cursor.getInt(idxType)
                val isIncoming = type == Telephony.Sms.MESSAGE_TYPE_INBOX
                val isRead = cursor.getInt(idxRead) == 1 || !isIncoming
                val subId = if (idxSubId >= 0) cursor.getInt(idxSubId) else -1

                val normalized = normalizeAddress(address)
                val contactName = contactCache.getOrPut(normalized) { lookupContactName(address) }

                persistParsedMessage(
                    systemSmsId = systemId,
                    threadId = threadId,
                    address = address,
                    normalizedAddress = normalized,
                    contactName = contactName,
                    body = body,
                    timestamp = timestamp,
                    isIncoming = isIncoming,
                    isRead = isRead,
                    subId = subId,
                    isBlocked = blockedSet.contains(normalized),
                    categoryOverride = overrides[normalized]
                )
                syncedCount++
            }
        }

        preferencesRepository.setLastSyncTimestamp(System.currentTimeMillis())
        syncedCount
    }

    suspend fun ingestIncomingSms(
        address: String,
        body: String,
        timestamp: Long,
        subId: Int
    ): MessageEntity = withContext(Dispatchers.IO) {
        val normalized = normalizeAddress(address)
        val blockedSet = senderRuleDao.getBlockedAddresses().toSet()
        val overrides = senderRuleDao.getAllOverrides()
            .associate { it.normalizedAddress to it.category }

        val existingThreadId = messageDao.findThreadIdByAddress(normalized)
        val threadId = existingThreadId ?: resolveOrCreateSystemThreadId(address) ?: messageDao.nextThreadId()

        var systemSmsId: Long? = null
        if (isDefaultSmsApp()) {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
                put(Telephony.Sms.THREAD_ID, threadId)
                if (subId != -1) put(Telephony.Sms.SUBSCRIPTION_ID, subId)
            }
            val insertedUri = runCatching {
                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            }.getOrNull()
            systemSmsId = insertedUri?.lastPathSegment?.toLongOrNull()
        }

        val contactName = lookupContactName(address)
        persistParsedMessage(
            systemSmsId = systemSmsId,
            threadId = threadId,
            address = address,
            normalizedAddress = normalized,
            contactName = contactName,
            body = body,
            timestamp = timestamp,
            isIncoming = true,
            isRead = false,
            subId = subId,
            isBlocked = blockedSet.contains(normalized),
            categoryOverride = overrides[normalized]
        )
    }

    suspend fun sendSms(
        address: String,
        body: String,
        subId: Int = -1,
        existingThreadId: Long? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanAddress = address.trim()
            require(cleanAddress.isNotEmpty()) { "Recipient address cannot be empty" }
            require(body.isNotBlank()) { "Message body cannot be empty" }

            val smsManager = getSmsManager(subId)
            val parts = smsManager.divideMessage(body)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(cleanAddress, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(cleanAddress, null, body, null, null)
            }

            val now = System.currentTimeMillis()
            val normalized = normalizeAddress(cleanAddress)
            val threadId = existingThreadId
                ?: messageDao.findThreadIdByAddress(normalized)
                ?: resolveOrCreateSystemThreadId(cleanAddress)
                ?: messageDao.nextThreadId()

            var systemSmsId: Long? = null
            if (isDefaultSmsApp()) {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, cleanAddress)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, now)
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.SEEN, 1)
                    put(Telephony.Sms.THREAD_ID, threadId)
                    if (subId != -1) put(Telephony.Sms.SUBSCRIPTION_ID, subId)
                }
                val uri = runCatching {
                    context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
                }.getOrNull()
                systemSmsId = uri?.lastPathSegment?.toLongOrNull()
            }

            val contactName = lookupContactName(cleanAddress)
            val overrides = senderRuleDao.getAllOverrides()
                .associate { it.normalizedAddress to it.category }

            persistParsedMessage(
                systemSmsId = systemSmsId,
                threadId = threadId,
                address = cleanAddress,
                normalizedAddress = normalized,
                contactName = contactName,
                body = body,
                timestamp = now,
                isIncoming = false,
                isRead = true,
                subId = subId,
                isBlocked = false,
                categoryOverride = overrides[normalized] ?: MessageCategory.PERSONAL
            )
            threadId
        }
    }

    private suspend fun persistParsedMessage(
        systemSmsId: Long?,
        threadId: Long,
        address: String,
        normalizedAddress: String,
        contactName: String?,
        body: String,
        timestamp: Long,
        isIncoming: Boolean,
        isRead: Boolean,
        subId: Int,
        isBlocked: Boolean,
        categoryOverride: MessageCategory?
    ): MessageEntity {
        val existingDup = messageDao.findDuplicate(threadId, body, timestamp)
        if (existingDup != null) return existingDup

        val parsed = SmsParsingEngine.parse(
            address = address,
            body = body,
            timestamp = timestamp,
            isKnownContact = !contactName.isNullOrBlank(),
            isBlockedSender = isBlocked,
            categoryOverride = categoryOverride
        )

        val smartBadge = buildSmartBadge(parsed)
        val entity = MessageEntity(
            systemSmsId = systemSmsId,
            threadId = threadId,
            address = address,
            normalizedAddress = normalizedAddress,
            contactName = contactName,
            body = body,
            timestamp = timestamp,
            isIncoming = isIncoming,
            isRead = isRead,
            subId = subId,
            category = parsed.category,
            otpCode = parsed.otp?.code,
            otpService = parsed.otp?.service,
            smartBadge = smartBadge
        )
        val id = messageDao.insertMessage(entity)

        parsed.transaction?.let { tx ->
            transactionDao.insert(
                TransactionEntity(
                    messageId = id,
                    threadId = threadId,
                    address = address,
                    amount = tx.amount,
                    currency = tx.currency,
                    type = tx.type,
                    institution = tx.institution,
                    accountLast4 = tx.accountLast4,
                    merchant = tx.merchant,
                    balanceAfter = tx.balanceAfter,
                    spendCategory = tx.spendCategory,
                    timestamp = timestamp
                )
            )
        }

        parsed.reminder?.let { rem ->
            reminderDao.insert(
                ReminderEntity(
                    messageId = id,
                    threadId = threadId,
                    title = rem.title,
                    description = rem.description,
                    amount = rem.amount,
                    currency = rem.currency,
                    dueTimestamp = rem.dueTimestamp,
                    kind = rem.kind
                )
            )
        }

        parsed.transitOrder?.let { tr ->
            transitOrderDao.insert(
                TransitOrderEntity(
                    messageId = id,
                    threadId = threadId,
                    kind = tr.kind,
                    referenceCode = tr.referenceCode,
                    carrierOrOperator = tr.carrierOrOperator,
                    summary = tr.summary,
                    status = tr.status,
                    eventTimestamp = tr.eventTimestamp ?: timestamp
                )
            )
        }

        return entity.copy(id = id)
    }

    suspend fun markThreadRead(threadId: Long) = withContext(Dispatchers.IO) {
        messageDao.markThreadAsRead(threadId)
        if (isDefaultSmsApp()) {
            runCatching {
                val values = ContentValues().apply {
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.SEEN, 1)
                }
                context.contentResolver.update(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    values,
                    "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                    arrayOf(threadId.toString())
                )
            }
        }
    }

    suspend fun setThreadArchived(threadId: Long, archived: Boolean) = withContext(Dispatchers.IO) {
        messageDao.setThreadArchived(threadId, archived)
    }

    suspend fun toggleMessageStarred(messageId: Long, starred: Boolean) = withContext(Dispatchers.IO) {
        messageDao.setMessageStarred(messageId, starred)
    }

    suspend fun deleteThread(threadId: Long) = withContext(Dispatchers.IO) {
        messageDao.softDeleteThread(threadId)
        if (isDefaultSmsApp()) {
            runCatching {
                context.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString())
                )
            }
        }
    }

    suspend fun deleteMessage(messageId: Long, systemSmsId: Long?) = withContext(Dispatchers.IO) {
        messageDao.softDeleteMessage(messageId)
        if (systemSmsId != null && isDefaultSmsApp()) {
            runCatching {
                context.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms._ID} = ?",
                    arrayOf(systemSmsId.toString())
                )
            }
        }
    }

    suspend fun blockSender(address: String) = withContext(Dispatchers.IO) {
        val normalized = normalizeAddress(address)
        senderRuleDao.blockSender(BlockedSenderEntity(normalized, address))
        messageDao.updateCategoryForSender(normalized, MessageCategory.SPAM)
    }

    suspend fun unblockSender(normalizedAddress: String) = withContext(Dispatchers.IO) {
        senderRuleDao.unblockSender(normalizedAddress)
        messageDao.updateCategoryForSender(normalizedAddress, MessageCategory.PERSONAL)
    }

    suspend fun overrideSenderCategory(address: String, category: MessageCategory) =
        withContext(Dispatchers.IO) {
            val normalized = normalizeAddress(address)
            senderRuleDao.setCategoryOverride(CategoryOverrideEntity(normalized, category))
            messageDao.updateCategoryForSender(normalized, category)
        }

    suspend fun cleanOldOtps(maxAgeHours: Long = 24L): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - maxAgeHours * 3600_000L
        messageDao.softDeleteOldOtps(cutoff)
    }

    suspend fun searchMessages(query: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val ftsQuery = trimmed
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                val clean = token.replace(Regex("[^A-Za-z0-9]"), "")
                if (clean.isNotEmpty()) "$clean*" else ""
            }
            .trim()
        if (ftsQuery.isNotEmpty()) {
            runCatching { messageDao.searchFts(ftsQuery) }.getOrNull()?.takeIf { it.isNotEmpty() }
                ?: messageDao.searchFallback(trimmed)
        } else {
            messageDao.searchFallback(trimmed)
        }
    }

    suspend fun addCustomReminder(
        title: String,
        description: String,
        amount: Double?,
        dueTimestamp: Long,
        kind: ReminderKind
    ) = withContext(Dispatchers.IO) {
        reminderDao.insert(
            ReminderEntity(
                title = title,
                description = description,
                amount = amount,
                currency = if (amount != null) "INR" else null,
                dueTimestamp = dueTimestamp,
                kind = kind
            )
        )
    }

    suspend fun setReminderCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        reminderDao.setCompleted(id, completed)
    }

    suspend fun setReminderNotify(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        reminderDao.setNotifyEnabled(id, enabled)
    }

    suspend fun deleteReminder(id: Long) = withContext(Dispatchers.IO) {
        reminderDao.deleteById(id)
    }

    fun getActiveSims(): List<SimInfo> {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val subManager = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        return runCatching {
            subManager.activeSubscriptionInfoList.orEmpty().map { info ->
                SimInfo(
                    subId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    displayName = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                        ?: "SIM ${info.simSlotIndex + 1}",
                    carrierName = info.carrierName?.toString() ?: "",
                    number = info.number
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun searchContacts(query: String): List<ContactOption> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext emptyList()
        }
        val results = mutableListOf<ContactOption>()
        val seen = mutableSetOf<String>()
        val selection = if (query.isNotBlank()) {
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        } else {
            null
        }
        val args = if (query.isNotBlank()) arrayOf("%$query%", "%$query%") else null

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                selection,
                args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && results.size < 40) {
                    val name = cursor.getString(nameIdx) ?: continue
                    val number = cursor.getString(numIdx)?.trim() ?: continue
                    val key = normalizeAddress(number)
                    if (seen.add(key)) {
                        results.add(ContactOption(name = name, phoneNumber = number))
                    }
                }
            }
        }
        results
    }

    private fun lookupContactName(address: String): String? {
        if (SenderIdUtils.isShortCodeOrAlphanumericSender(address)) return null
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return runCatching {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(address)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun resolveOrCreateSystemThreadId(address: String): Long? {
        return runCatching {
            Telephony.Threads.getOrCreateThreadId(context, address)
        }.getOrNull()
    }

    private fun getSmsManager(subId: Int): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = context.getSystemService(SmsManager::class.java)
            if (subId != -1) base.createForSubscriptionId(subId) else base
        } else {
            @Suppress("DEPRECATION")
            if (subId != -1) SmsManager.getSmsManagerForSubscriptionId(subId) else SmsManager.getDefault()
        }
    }

    companion object {
        fun normalizeAddress(address: String): String {
            val trimmed = address.trim()
            if (trimmed.any { it.isLetter() }) {
                return SenderIdUtils.extractHeaderToken(trimmed)
            }
            val digits = trimmed.filter { it.isDigit() }
            return if (digits.length >= 10) digits.takeLast(10) else trimmed.uppercase()
        }
    }
}
