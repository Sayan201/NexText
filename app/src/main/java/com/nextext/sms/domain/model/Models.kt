package com.nextext.sms.domain.model

enum class MessageCategory(val label: String) {
    PERSONAL("Personal"),
    TRANSACTIONS("Transactions"),
    PROMOTIONS("Promotions"),
    SPAM("Spam")
}

enum class TransactionType(val label: String, val isOutflow: Boolean) {
    DEBIT("Debited", true),
    CREDIT("Credited", false),
    UPI("UPI", true),
    CARD_SPEND("Card Spend", true),
    ATM("ATM Withdrawal", true),
    BILL_PAID("Bill Paid", true),
    REFUND("Refund", false)
}

enum class SpendCategory(val label: String) {
    FOOD_DINING("Food & Dining"),
    SHOPPING("Shopping"),
    TRAVEL_FUEL("Travel & Fuel"),
    BILLS_UTILITIES("Bills & Utilities"),
    TRANSFERS_UPI("Transfers & UPI"),
    ENTERTAINMENT("Entertainment"),
    ATM_CASH("ATM & Cash"),
    OTHER("Other")
}

enum class ReminderKind(val label: String) {
    BILL("Bill"),
    EMI("EMI"),
    RECHARGE("Recharge"),
    FLIGHT("Flight"),
    TRAIN("Train"),
    CUSTOM("Reminder")
}

enum class TransitOrderKind(val label: String) {
    TRAIN("Train"),
    FLIGHT("Flight"),
    DELIVERY("Delivery")
}

data class ParsedOtp(
    val code: String,
    val service: String?
)

data class ParsedTransaction(
    val amount: Double,
    val currency: String,
    val type: TransactionType,
    val institution: String?,
    val accountLast4: String?,
    val merchant: String?,
    val balanceAfter: Double?,
    val spendCategory: SpendCategory
)

data class ParsedReminder(
    val title: String,
    val description: String,
    val amount: Double?,
    val currency: String?,
    val dueTimestamp: Long,
    val kind: ReminderKind
)

data class ParsedTransitOrder(
    val kind: TransitOrderKind,
    val referenceCode: String,
    val carrierOrOperator: String,
    val summary: String,
    val status: String,
    val eventTimestamp: Long?
)

data class ParsedSmsResult(
    val category: MessageCategory,
    val otp: ParsedOtp? = null,
    val transaction: ParsedTransaction? = null,
    val reminder: ParsedReminder? = null,
    val transitOrder: ParsedTransitOrder? = null
)

data class SimInfo(
    val subId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val number: String?
)

data class ContactOption(
    val name: String,
    val phoneNumber: String
)
