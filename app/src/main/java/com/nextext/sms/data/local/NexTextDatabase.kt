package com.nextext.sms.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MessageEntity::class,
        MessageFtsEntity::class,
        TransactionEntity::class,
        ReminderEntity::class,
        TransitOrderEntity::class,
        BlockedSenderEntity::class,
        CategoryOverrideEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NexTextDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun transactionDao(): TransactionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun transitOrderDao(): TransitOrderDao
    abstract fun senderRuleDao(): SenderRuleDao

    companion object {
        @Volatile
        private var instance: NexTextDatabase? = null

        fun getInstance(context: Context): NexTextDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NexTextDatabase::class.java,
                    "nextext.db"
                ).build().also { instance = it }
            }
        }
    }
}
