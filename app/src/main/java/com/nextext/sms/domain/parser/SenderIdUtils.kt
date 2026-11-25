package com.nextext.sms.domain.parser

object SenderIdUtils {
    private val dltHeaderRegex = Regex("""^[A-Z]{2}-([A-Z0-9]{3,12})(?:-([STPG]))?$""", RegexOption.IGNORE_CASE)

    private val knownInstitutions = listOf(
        "HDFCBK" to "HDFC Bank",
        "HDFC" to "HDFC Bank",
        "SBIINB" to "SBI",
        "SBIPSG" to "SBI",
        "SBICRD" to "SBI Card",
        "SBI" to "SBI",
        "ICICIB" to "ICICI Bank",
        "ICICI" to "ICICI Bank",
        "AXISBK" to "Axis Bank",
        "AXIS" to "Axis Bank",
        "KOTAKB" to "Kotak Bank",
        "KOTAK" to "Kotak Bank",
        "PNBSMS" to "PNB",
        "BOBTXN" to "Bank of Baroda",
        "CANBNK" to "Canara Bank",
        "IDFCFB" to "IDFC FIRST",
        "INDUSB" to "IndusInd Bank",
        "YESBNK" to "Yes Bank",
        "FEDBNK" to "Federal Bank",
        "AMEX" to "American Express",
        "CITIBK" to "Citibank",
        "HSBC" to "HSBC",
        "CHASE" to "Chase",
        "PAYTM" to "Paytm",
        "PHONEPE" to "PhonePe",
        "GPAY" to "Google Pay",
        "AIRTEL" to "Airtel",
        "JIO" to "Jio",
        "BESCOM" to "BESCOM",
        "IRCTC" to "IRCTC",
        "INDIGO" to "IndiGo",
        "AIRIND" to "Air India",
        "VISTARA" to "Vistara",
        "AKASA" to "Akasa Air",
        "AMAZON" to "Amazon",
        "FLIPKART" to "Flipkart",
        "FLPKRT" to "Flipkart",
        "BLUDRT" to "Blue Dart",
        "BLUEDART" to "Blue Dart",
        "DLHVRY" to "Delhivery",
        "DELHIVERY" to "Delhivery"
    )

    fun extractHeaderToken(address: String): String {
        val trimmed = address.trim()
        val match = dltHeaderRegex.matchEntire(trimmed)
        if (match != null) return match.groupValues[1].uppercase()
        return trimmed.uppercase()
    }

    fun extractDltTag(address: String): String? {
        val match = dltHeaderRegex.matchEntire(address.trim()) ?: return null
        return match.groupValues.getOrNull(2)?.uppercase()?.takeIf { it.isNotEmpty() }
    }

    fun isShortCodeOrAlphanumericSender(address: String): Boolean {
        val trimmed = address.trim()
        if (dltHeaderRegex.matches(trimmed)) return true
        if (trimmed.any { it.isLetter() }) return true
        val digitsOnly = trimmed.filter { it.isDigit() }
        return digitsOnly.length in 3..6
    }

    fun isPersonalPhoneNumber(address: String): Boolean {
        val trimmed = address.trim()
        if (trimmed.any { it.isLetter() }) return false
        val digitsOnly = trimmed.filter { it.isDigit() }
        return digitsOnly.length >= 10
    }

    fun inferInstitution(address: String, body: String): String? {
        val header = extractHeaderToken(address)
        val combined = "$header ${body.uppercase()}"
        for ((token, name) in knownInstitutions) {
            if (combined.contains(token)) return name
        }
        return header.takeIf { it.any(Char::isLetter) && it.length in 3..12 }
    }
}
