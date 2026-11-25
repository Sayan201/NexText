package com.nextext.sms.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.domain.model.ReminderKind
import com.nextext.sms.domain.model.SpendCategory
import com.nextext.sms.domain.model.TransactionType
import com.nextext.sms.domain.model.TransitOrderKind

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["systemSmsId"]),
        Index(value = ["threadId"]),
        Index(value = ["normalizedAddress"]),
        Index(value = ["category", "timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val category: MessageCategory = MessageCategory.PERSONAL,
    val isStarred: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val otpCode: String? = null,
    val otpService: String? = null,
    val smartBadge: String? = null
)

@Fts4(contentEntity = MessageEntity::class)
@Entity(tableName = "messages_fts")
data class MessageFtsEntity(
    val address: String,
    val contactName: String?,
    val body: String
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["messageId"], unique = true),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val threadId: Long,
    val address: String,
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    val institution: String?,
    val accountLast4: String?,
    val merchant: String?,
    val balanceAfter: Double?,
    val spendCategory: SpendCategory,
    val timestamp: Long
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["messageId"], unique = true),
        Index(value = ["dueTimestamp"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long? = null,
    val threadId: Long? = null,
    val title: String,
    val description: String,
    val amount: Double? = null,
    val currency: String? = null,
    val dueTimestamp: Long,
    val kind: ReminderKind = ReminderKind.BILL,
    val isCompleted: Boolean = false,
    val notifyEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transit_orders",
    indices = [
        Index(value = ["messageId"], unique = true),
        Index(value = ["eventTimestamp"])
    ]
)
data class TransitOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val threadId: Long,
    val kind: TransitOrderKind,
    val referenceCode: String,
    val carrierOrOperator: String,
    val summary: String,
    val status: String,
    val eventTimestamp: Long
)

@Entity(tableName = "blocked_senders")
data class BlockedSenderEntity(
    @PrimaryKey val normalizedAddress: String,
    val displayAddress: String,
    val blockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "category_overrides")
data class CategoryOverrideEntity(
    @PrimaryKey val normalizedAddress: String,
    val category: MessageCategory
)

data class ThreadSummary(
    val threadId: Long,
    val address: String,
    val normalizedAddress: String,
    val contactName: String?,
    val lastBody: String,
    val lastTimestamp: Long,
    val category: MessageCategory,
    val unreadCount: Int,
    val isArchived: Boolean,
    val hasStarred: Boolean,
    val lastOtpCode: String?,
    val lastSmartBadge: String?
)
