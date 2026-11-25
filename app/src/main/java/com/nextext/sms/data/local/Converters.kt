package com.nextext.sms.data.local

import androidx.room.TypeConverter
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.domain.model.ReminderKind
import com.nextext.sms.domain.model.SpendCategory
import com.nextext.sms.domain.model.TransactionType
import com.nextext.sms.domain.model.TransitOrderKind

class Converters {
    @TypeConverter
    fun fromMessageCategory(value: MessageCategory): String = value.name

    @TypeConverter
    fun toMessageCategory(value: String): MessageCategory =
        runCatching { MessageCategory.valueOf(value) }.getOrDefault(MessageCategory.PERSONAL)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.DEBIT)

    @TypeConverter
    fun fromSpendCategory(value: SpendCategory): String = value.name

    @TypeConverter
    fun toSpendCategory(value: String): SpendCategory =
        runCatching { SpendCategory.valueOf(value) }.getOrDefault(SpendCategory.OTHER)

    @TypeConverter
    fun fromReminderKind(value: ReminderKind): String = value.name

    @TypeConverter
    fun toReminderKind(value: String): ReminderKind =
        runCatching { ReminderKind.valueOf(value) }.getOrDefault(ReminderKind.CUSTOM)

    @TypeConverter
    fun fromTransitOrderKind(value: TransitOrderKind): String = value.name

    @TypeConverter
    fun toTransitOrderKind(value: String): TransitOrderKind =
        runCatching { TransitOrderKind.valueOf(value) }.getOrDefault(TransitOrderKind.DELIVERY)
}
