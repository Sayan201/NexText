package com.nextext.sms.domain.parser

import com.nextext.sms.domain.model.ParsedTransaction
import com.nextext.sms.domain.model.SpendCategory
import com.nextext.sms.domain.model.TransactionType

object TransactionExtractor {

    private val amountRegex = Regex(
        pattern = """(₹|rs\.?|inr|\$|usd|€|eur|£|gbp)\s*([0-9][0-9,]*(?:\.\d{1,2})?)""",
        option = RegexOption.IGNORE_CASE
    )

    private val trailingAmountRegex = Regex(
        pattern = """([0-9][0-9,]*(?:\.\d{1,2})?)\s*(inr|rs\.?|usd|eur|gbp)\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val balanceRegex = Regex(
        pattern = """(?:avl\.?\s*bal(?:ance)?|available\s+bal(?:ance)?|bal(?:ance)?\s*(?:is|:)|clr\s*bal)\s*(?:is|:)?\s*(?:₹|rs\.?|inr|\$|€|£)?\s*([0-9][0-9,]*(?:\.\d{1,2})?)""",
        option = RegexOption.IGNORE_CASE
    )

    private val accountRegex = Regex(
        pattern = """(?:a/c|acct?|account|card)\s*(?:no\.?|ending(?:\s+with)?|bearing)?\s*(?:[xX*#]+)?(\d{4})\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val maskedAccountFallbackRegex = Regex(
        pattern = """\b(?:xx|\*\*|x+)(\d{4})\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val merchantRegex = Regex(
        pattern = """(?:\bat\s+|\bto\s+(?:vpa\s+)?|\bvia\s+upi\s+to\s+|\binfo[:\s]+(?:upi/\d+/)?|\bpaid\s+to\s+|\bmerchant[:\s]+)([A-Za-z0-9@._&\-\s]{2,36})(?=\s+(?:on|via|ref|upi|avl|bal|not|if|call|$)|\n|\.|,)""",
        option = RegexOption.IGNORE_CASE
    )

    private val debitKeywords = Regex(
        """\b(debited|spent|paid|withdrawn|deducted|sent|purchase|charged|txn\s+of|payment\s+of|transferred)\b""",
        RegexOption.IGNORE_CASE
    )
    private val creditKeywords = Regex(
        """\b(credited|received|deposited|salary|cashback|interest\s+credited)\b""",
        RegexOption.IGNORE_CASE
    )
    private val refundKeywords = Regex(
        """\b(refund|refunded|reversed|reversal)\b""",
        RegexOption.IGNORE_CASE
    )
    private val atmKeywords = Regex(
        """\b(atm|cash\s+withdrawal|withdrawn\s+from\s+atm)\b""",
        RegexOption.IGNORE_CASE
    )
    private val cardKeywords = Regex(
        """\b(credit\s+card|debit\s+card|card\s+ending|card\s+no|spent\s+on\s+card)\b""",
        RegexOption.IGNORE_CASE
    )
    private val upiKeywords = Regex(
        """\b(upi|vpa|gpay|phonepe|paytm|bhim)\b""",
        RegexOption.IGNORE_CASE
    )
    private val billPaidKeywords = Regex(
        """\b(bill\s+payment\s+(?:of|successful|received)|recharge\s+(?:of|successful))\b""",
        RegexOption.IGNORE_CASE
    )

    private val nonTransactionFilter = Regex(
        """\b(win|won|upto|up\s+to|cashback\s+offer|flat\s+\d+%|discount|get\s+loan|pre-approved|eligible\s+for|due\s+on|bill\s+is\s+generated|minimum\s+amount\s+due)\b""",
        RegexOption.IGNORE_CASE
    )

    fun extract(address: String, body: String): ParsedTransaction? {
        // Ignore pure promotional offers or unpaid bill alerts (handled by ReminderExtractor)
        if (nonTransactionFilter.containsMatchIn(body) &&
            !debitKeywords.containsMatchIn(body) &&
            !creditKeywords.containsMatchIn(body) &&
            !refundKeywords.containsMatchIn(body)
        ) {
            return null
        }

        val hasActionKeyword = debitKeywords.containsMatchIn(body) ||
            creditKeywords.containsMatchIn(body) ||
            refundKeywords.containsMatchIn(body) ||
            atmKeywords.containsMatchIn(body) ||
            billPaidKeywords.containsMatchIn(body)

        if (!hasActionKeyword) return null

        val (currency, amount) = extractAmountAndCurrency(body) ?: return null
        if (amount <= 0.0) return null

        val type = when {
            refundKeywords.containsMatchIn(body) -> TransactionType.REFUND
            atmKeywords.containsMatchIn(body) -> TransactionType.ATM
            creditKeywords.containsMatchIn(body) && !debitKeywords.containsMatchIn(body) -> TransactionType.CREDIT
            billPaidKeywords.containsMatchIn(body) -> TransactionType.BILL_PAID
            cardKeywords.containsMatchIn(body) -> TransactionType.CARD_SPEND
            upiKeywords.containsMatchIn(body) -> TransactionType.UPI
            debitKeywords.containsMatchIn(body) -> TransactionType.DEBIT
            else -> TransactionType.DEBIT
        }

        val balanceAfter = balanceRegex.find(body)
            ?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()

        val accountLast4 = accountRegex.find(body)?.groupValues?.getOrNull(1)
            ?: maskedAccountFallbackRegex.find(body)?.groupValues?.getOrNull(1)

        val merchant = merchantRegex.find(body)
            ?.groupValues?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.length >= 2 && !it.equals("your", ignoreCase = true) }

        val institution = SenderIdUtils.inferInstitution(address, body)
        val spendCategory = inferSpendCategory(body, merchant, type)

        return ParsedTransaction(
            amount = amount,
            currency = currency,
            type = type,
            institution = institution,
            accountLast4 = accountLast4,
            merchant = merchant,
            balanceAfter = balanceAfter,
            spendCategory = spendCategory
        )
    }

    private fun extractAmountAndCurrency(body: String): Pair<String, Double>? {
        val primary = amountRegex.find(body)
        if (primary != null) {
            val currency = normalizeCurrency(primary.groupValues[1])
            val value = primary.groupValues[2].replace(",", "").toDoubleOrNull()
            if (value != null) return currency to value
        }
        val trailing = trailingAmountRegex.find(body)
        if (trailing != null) {
            val value = trailing.groupValues[1].replace(",", "").toDoubleOrNull()
            val currency = normalizeCurrency(trailing.groupValues[2])
            if (value != null) return currency to value
        }
        return null
    }

    fun normalizeCurrency(raw: String): String {
        return when (raw.lowercase().replace(".", "").trim()) {
            "₹", "rs", "inr" -> "INR"
            "$", "usd" -> "USD"
            "€", "eur" -> "EUR"
            "£", "gbp" -> "GBP"
            else -> raw.uppercase()
        }
    }

    private fun inferSpendCategory(
        body: String,
        merchant: String?,
        type: TransactionType
    ): SpendCategory {
        val text = "${merchant.orEmpty()} $body".lowercase()
        return when {
            type == TransactionType.ATM -> SpendCategory.ATM_CASH
            listOf("swiggy", "zomato", "dominos", "mcdonald", "starbucks", "kfc", "cafe", "restaurant", "blinkit", "zepto", "instamart", "bigbasket").any { text.contains(it) } ->
                SpendCategory.FOOD_DINING
            listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "apple", "mall", "store", "mart").any { text.contains(it) } ->
                SpendCategory.SHOPPING
            listOf("uber", "ola", "rapido", "irctc", "indigo", "air india", "vistara", "makemytrip", "petrol", "fuel", "iocl", "bpcl", "hpcl", "metro", "fastag").any { text.contains(it) } ->
                SpendCategory.TRAVEL_FUEL
            listOf("airtel", "jio", "vi ", "bsnl", "electricity", "bescom", "tata power", "water", "gas", "broadband", "dth", "recharge", "bill").any { text.contains(it) } ->
                SpendCategory.BILLS_UTILITIES
            listOf("netflix", "spotify", "prime", "hotstar", "bookmyshow", "pvr", "inox", "youtube").any { text.contains(it) } ->
                SpendCategory.ENTERTAINMENT
            type == TransactionType.UPI || text.contains("@") ->
                SpendCategory.TRANSFERS_UPI
            else -> SpendCategory.OTHER
        }
    }
}
