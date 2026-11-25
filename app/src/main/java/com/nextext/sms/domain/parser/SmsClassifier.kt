package com.nextext.sms.domain.parser

import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.domain.model.ParsedSmsResult

object SmsParsingEngine {

    private val spamKeywordsRegex = Regex(
        pattern = """\b(congratulations\s+you\s+won|lottery|claim\s+your\s+prize|kyc\s+(?:blocked|suspended|update\s+immediately)|click\s+link\s+to\s+avoid\s+block|instant\s+loan\s+without|rummy|teen\s*patti|betting|jackpot|win\s+cash\s+now|work\s+from\s+home\s+earn)\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val promoKeywordsRegex = Regex(
        pattern = """\b(flat\s+\d+%|up\s*to\s+\d+%\s*off|\buse\s+code\b|coupon|promo\s*code|sale\s+(?:is\s+)?live|limited\s+period\s+offer|shop\s+now|order\s+now|book\s+now|special\s+offer|festive\s+offer|cashback\s+upto|free\s+delivery|subscribe\s+now|unsubscribe|opt[\s-]?out|t&c\s+apply)\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val transactionalContextRegex = Regex(
        pattern = """\b(debited|credited|a/c|account|balance|avl\s*bal|upi|otp|verification\s+code|passcode|pnr|order|shipped|delivered|out\s+for\s+delivery|due\s+on|bill|emi|statement|recharge\s+successful|invoice)\b""",
        option = RegexOption.IGNORE_CASE
    )

    fun parse(
        address: String,
        body: String,
        timestamp: Long,
        isKnownContact: Boolean = false,
        isBlockedSender: Boolean = false,
        categoryOverride: MessageCategory? = null
    ): ParsedSmsResult {
        val otp = OtpExtractor.extract(address, body)
        val transaction = TransactionExtractor.extract(address, body)
        val reminder = ReminderExtractor.extract(address, body, timestamp)
        val transitOrder = TransitOrderExtractor.extract(address, body, timestamp)

        val category = when {
            isBlockedSender -> MessageCategory.SPAM
            categoryOverride != null -> categoryOverride
            else -> classify(
                address = address,
                body = body,
                isKnownContact = isKnownContact,
                hasOtp = otp != null,
                hasTransaction = transaction != null,
                hasReminder = reminder != null,
                hasTransitOrder = transitOrder != null
            )
        }

        return ParsedSmsResult(
            category = category,
            otp = otp,
            transaction = transaction,
            reminder = reminder,
            transitOrder = transitOrder
        )
    }

    private fun classify(
        address: String,
        body: String,
        isKnownContact: Boolean,
        hasOtp: Boolean,
        hasTransaction: Boolean,
        hasReminder: Boolean,
        hasTransitOrder: Boolean
    ): MessageCategory {
        // Spam signals (only when not a known contact)
        if (!isKnownContact && spamKeywordsRegex.containsMatchIn(body)) {
            return MessageCategory.SPAM
        }

        // High-confidence transactional signals (Banks, OTPs, Bills, Transit/Delivery)
        if (hasOtp || hasTransaction || hasReminder || hasTransitOrder) {
            return MessageCategory.TRANSACTIONS
        }

        // DLT explicit tag (-P = Promotional, -T / -S / -G = Service/Transactional)
        val dltTag = SenderIdUtils.extractDltTag(address)
        if (dltTag == "P") {
            return MessageCategory.PROMOTIONS
        }

        // Promotional keywords from non-contact senders
        if (!isKnownContact && promoKeywordsRegex.containsMatchIn(body)) {
            return MessageCategory.PROMOTIONS
        }

        // Known contact or personal phone number without commercial content
        if (isKnownContact || SenderIdUtils.isPersonalPhoneNumber(address)) {
            return MessageCategory.PERSONAL
        }

        // Short codes / alphanumeric headers
        if (SenderIdUtils.isShortCodeOrAlphanumericSender(address)) {
            return if (transactionalContextRegex.containsMatchIn(body)) {
                MessageCategory.TRANSACTIONS
            } else {
                MessageCategory.PROMOTIONS
            }
        }

        return MessageCategory.PERSONAL
    }
}
