package com.nextext.sms.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nextext.sms.domain.model.MessageCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query(
        """
        SELECT 
            m.threadId AS threadId,
            m.address AS address,
            m.normalizedAddress AS normalizedAddress,
            m.contactName AS contactName,
            m.body AS lastBody,
            m.timestamp AS lastTimestamp,
            m.category AS category,
            (SELECT COUNT(*) FROM messages u WHERE u.threadId = m.threadId AND u.isRead = 0 AND u.isDeleted = 0) AS unreadCount,
            m.isArchived AS isArchived,
            EXISTS(SELECT 1 FROM messages s WHERE s.threadId = m.threadId AND s.isStarred = 1 AND s.isDeleted = 0) AS hasStarred,
            m.otpCode AS lastOtpCode,
            m.smartBadge AS lastSmartBadge
        FROM messages m
        INNER JOIN (
            SELECT threadId, MAX(timestamp) AS maxTs
            FROM messages
            WHERE isDeleted = 0
            GROUP BY threadId
        ) latest ON m.threadId = latest.threadId AND m.timestamp = latest.maxTs
        WHERE m.isDeleted = 0
        GROUP BY m.threadId
        ORDER BY m.timestamp DESC
        """
    )
    fun observeAllThreads(): Flow<List<ThreadSummary>>

    @Query("SELECT * FROM messages WHERE threadId = :threadId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun observeThreadMessages(threadId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getAllActiveMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE systemSmsId = :systemSmsId LIMIT 1")
    suspend fun findBySystemSmsId(systemSmsId: Long): MessageEntity?

    @Query("SELECT * FROM messages WHERE threadId = :threadId AND body = :body AND ABS(timestamp - :timestamp) < 5000 LIMIT 1")
    suspend fun findDuplicate(threadId: Long, body: String, timestamp: Long): MessageEntity?

    @Query("SELECT DISTINCT threadId FROM messages WHERE normalizedAddress = :normalizedAddress AND isDeleted = 0 LIMIT 1")
    suspend fun findThreadIdByAddress(normalizedAddress: String): Long?

    @Query("SELECT COALESCE(MAX(threadId), 0) + 1 FROM messages")
    suspend fun nextThreadId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET isRead = 1 WHERE threadId = :threadId AND isRead = 0")
    suspend fun markThreadAsRead(threadId: Long)

    @Query("UPDATE messages SET isRead = 1 WHERE isRead = 0 AND isDeleted = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE messages SET isArchived = :archived WHERE threadId = :threadId")
    suspend fun setThreadArchived(threadId: Long, archived: Boolean)

    @Query("UPDATE messages SET isStarred = :starred WHERE id = :messageId")
    suspend fun setMessageStarred(messageId: Long, starred: Boolean)

    @Query("UPDATE messages SET isDeleted = 1 WHERE threadId = :threadId")
    suspend fun softDeleteThread(threadId: Long)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: Long)

    @Query("UPDATE messages SET category = :category WHERE normalizedAddress = :normalizedAddress")
    suspend fun updateCategoryForSender(normalizedAddress: String, category: MessageCategory)

    @Query("UPDATE messages SET isDeleted = 1 WHERE otpCode IS NOT NULL AND timestamp < :cutoffTimestamp AND isStarred = 0")
    suspend fun softDeleteOldOtps(cutoffTimestamp: Long): Int

    @Query(
        """
        SELECT * FROM messages
        WHERE isDeleted = 0
          AND (body LIKE '%' || :plainQuery || '%'
               OR address LIKE '%' || :plainQuery || '%'
               OR COALESCE(contactName, '') LIKE '%' || :plainQuery || '%')
        ORDER BY timestamp DESC
        LIMIT 150
        """
    )
    suspend fun searchFallback(plainQuery: String): List<MessageEntity>

    @Query(
        """
        SELECT messages.* FROM messages
        JOIN messages_fts ON messages.id = messages_fts.rowid
        WHERE messages_fts MATCH :ftsQuery AND messages.isDeleted = 0
        ORDER BY messages.timestamp DESC
        LIMIT 150
        """
    )
    suspend fun searchFts(ftsQuery: String): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0 AND isDeleted = 0 AND isArchived = 0 AND category = :category")
    fun observeUnreadCountByCategory(category: MessageCategory): Flow<Int>

    @Query("SELECT * FROM messages WHERE otpCode IS NOT NULL AND isDeleted = 0 ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestOtp(): Flow<MessageEntity?>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueTimestamp ASC")
    fun observeAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY dueTimestamp ASC")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("UPDATE reminders SET notifyEnabled = :enabled WHERE id = :id")
    suspend fun setNotifyEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND notifyEnabled = 1 AND dueTimestamp BETWEEN :fromTs AND :toTs")
    suspend fun getUpcomingDueReminders(fromTs: Long, toTs: Long): List<ReminderEntity>
}

@Dao
interface TransitOrderDao {
    @Query("SELECT * FROM transit_orders ORDER BY eventTimestamp DESC")
    fun observeAll(): Flow<List<TransitOrderEntity>>

    @Query("SELECT * FROM transit_orders ORDER BY eventTimestamp DESC")
    suspend fun getAll(): List<TransitOrderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: TransitOrderEntity): Long
}

@Dao
interface SenderRuleDao {
    @Query("SELECT * FROM blocked_senders ORDER BY blockedAt DESC")
    fun observeBlockedSenders(): Flow<List<BlockedSenderEntity>>

    @Query("SELECT * FROM blocked_senders ORDER BY blockedAt DESC")
    suspend fun getAllBlockedSenders(): List<BlockedSenderEntity>

    @Query("SELECT normalizedAddress FROM blocked_senders")
    suspend fun getBlockedAddresses(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_senders WHERE normalizedAddress = :normalizedAddress)")
    fun observeIsBlocked(normalizedAddress: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockSender(entity: BlockedSenderEntity)

    @Query("DELETE FROM blocked_senders WHERE normalizedAddress = :normalizedAddress")
    suspend fun unblockSender(normalizedAddress: String)

    @Query("SELECT * FROM category_overrides")
    suspend fun getAllOverrides(): List<CategoryOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCategoryOverride(entity: CategoryOverrideEntity)
}
