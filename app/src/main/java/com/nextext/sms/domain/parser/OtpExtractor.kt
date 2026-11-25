package com.nextext.sms.domain.parser

import com.nextext.sms.domain.model.ParsedOtp

object OtpExtractor {
    private val otpTriggerRegex = Regex(
        pattern = """\b(otp|one[\s-]?time\s+password|verification\s+code|passcode|security\s+code|auth(?:entication)?\s+code|login\s+code|confirmation\s+code|2fa\s+code|pin\s+is)\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val explicitCodeAfterTrigger = Regex(
        pattern = """(?:otp|verification\s+code|passcode|security\s+code|auth(?:entication)?\s+code|login\s+code|confirmation\s+code|2fa\s+code|code|pin)\s*(?:is|:|=|-|as)?\s*(?:G-)?(\d{4,8})\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val codeBeforeTrigger = Regex(
        pattern = """\b(?:G-)?(\d{4,8})\s+(?:is\s+(?:the\s+|your\s+)?)?(?:one[\s-]?time\s+password|otp|verification\s+code|passcode|security\s+code|login\s+code)\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val genericOtpFallback = Regex("""\b(\d{4,8})\b""")

    private val falsePositiveContext = Regex(
        pattern = """(?:rs\.?|inr|₹|\$|€|£)\s*\d+|\b(?:a/c|acct?|card|pnr|order)\s*(?:no\.?|#|xx|\*)*\d+""",
        option = RegexOption.IGNORE_CASE
    )

    private val deliveryPinRegex = Regex(
        pattern = """\bdelivery\s+(?:pin|otp|code)\b""",
        option = RegexOption.IGNORE_CASE
    )

    fun extract(address: String, body: String): ParsedOtp? {
        if (!otpTriggerRegex.containsMatchIn(body) && !body.startsWith("G-", ignoreCase = false)) {
            return null
        }

        val candidate = explicitCodeAfterTrigger.find(body)?.groupValues?.getOrNull(1)
            ?: codeBeforeTrigger.find(body)?.groupValues?.getOrNull(1)
            ?: findSafeFallbackCode(body)
            ?: return null

        if (isLikelyYearOrInvalid(candidate)) return null

        val service = inferService(address, body)
        return ParsedOtp(code = candidate, service = service)
    }

    private fun findSafeFallbackCode(body: String): String? {
        val masked = falsePositiveContext.replace(body, " ")
        val matches = genericOtpFallback.findAll(masked).map { it.groupValues[1] }.toList()
        return matches.firstOrNull { candidate ->
            if (candidate.length == 4) {
                val num = candidate.toIntOrNull() ?: return@firstOrNull false
                num !in 1990..2035 || deliveryPinRegex.containsMatchIn(body)
            } else {
                candidate.length in 4..8
            }
        }
    }

    private fun isLikelyYearOrInvalid(code: String): Boolean {
        return code.length !in 4..8 || code.any { !it.isDigit() }
    }

    private fun inferService(address: String, body: String): String? {
        val knownBrands = listOf(
            "HDFC" to "HDFC Bank",
            "SBI" to "SBI",
            "ICICI" to "ICICI Bank",
            "AXIS" to "Axis Bank",
            "KOTAK" to "Kotak Bank",
            "PAYTM" to "Paytm",
            "PHONEPE" to "PhonePe",
            "GPAY" to "Google Pay",
            "GOOGLE" to "Google",
            "AMAZON" to "Amazon",
            "FLIPKART" to "Flipkart",
            "SWIGGY" to "Swiggy",
            "ZOMATO" to "Zomato",
            "IRCTC" to "IRCTC",
            "UIDAI" to "Aadhaar",
            "AADHAAR" to "Aadhaar",
            "AIRTEL" to "Airtel",
            "JIO" to "Jio",
            "WHATSAPP" to "WhatsApp",
            "UBER" to "Uber",
            "OLA" to "Ola"
        )
        val combined = "$address $body".uppercase()
        for ((token, label) in knownBrands) {
            if (combined.contains(token)) return label
        }
        val cleanSender = SenderIdUtils.extractHeaderToken(address)
        return cleanSender.takeIf { it.isNotBlank() && it.any(Char::isLetter) }
    }
}
