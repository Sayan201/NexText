package com.nextext.sms.domain.parser

import com.nextext.sms.domain.model.ParsedTransitOrder
import com.nextext.sms.domain.model.TransitOrderKind

object TransitOrderExtractor {

    private val pnrRegex = Regex("""\bPNR\s*(?:No\.?|:|-)?\s*([A-Z0-9]{6,10})\b""", RegexOption.IGNORE_CASE)
    private val trainRegex = Regex("""\b(?:Train|Trn)\s*(?:No\.?|:|-)?\s*(\d{4,5})\b""", RegexOption.IGNORE_CASE)
    private val flightCodeRegex = Regex("""\b(6E|AI|UK|SG|QP|IX|EK|QR|BA|LH|UA|DL)[\s-]?(\d{2,4})\b""")
    private val routeRegex = Regex("""\b([A-Z]{3})\s*(?:-|to|->)\s*([A-Z]{3})\b""")
    private val orderIdRegex = Regex(
        """\b(?:Order|AWB|Tracking|Shipment)\s*(?:ID|No\.?|#|:|-)?\s*([A-Z0-9-]{6,22})\b""",
        RegexOption.IGNORE_CASE
    )
    private val trainContextRegex = Regex("""\b(irctc|pnr|coach|berth|rac|cnf|doj|boarding)\b""", RegexOption.IGNORE_CASE)
    private val flightContextRegex = Regex("""\b(flight|boarding|departure|airline|indigo|air\s+india|vistara|akasa|spicejet)\b""", RegexOption.IGNORE_CASE)
    private val deliveryContextRegex = Regex(
        """\b(out\s+for\s+delivery|shipped|dispatched|arriving\s+today|delivered|package|parcel|courier|delhivery|bluedart|blue\s*dart|xpressbees|ekart|amazon|flipkart)\b""",
        RegexOption.IGNORE_CASE
    )

    fun extract(address: String, body: String, messageTimestamp: Long): ParsedTransitOrder? {
        val pnrMatch = pnrRegex.find(body)?.groupValues?.getOrNull(1)
        val trainNo = trainRegex.find(body)?.groupValues?.getOrNull(1)

        if (trainContextRegex.containsMatchIn(body) && (pnrMatch != null || trainNo != null)) {
            val ref = pnrMatch ?: trainNo!!
            val route = routeRegex.find(body)?.let { "${it.groupValues[1]} → ${it.groupValues[2]}" }
            val status = when {
                body.contains("CNF", ignoreCase = true) -> "Confirmed"
                body.contains("RAC", ignoreCase = true) -> "RAC"
                body.contains("WL", ignoreCase = true) -> "Waitlisted"
                body.contains("cancelled", ignoreCase = true) -> "Cancelled"
                else -> "Booked"
            }
            val summaryParts = listOfNotNull(
                trainNo?.let { "Train $it" },
                route,
                "PNR $ref"
            )
            return ParsedTransitOrder(
                kind = TransitOrderKind.TRAIN,
                referenceCode = ref,
                carrierOrOperator = "IRCTC / Indian Railways",
                summary = summaryParts.joinToString(" • "),
                status = status,
                eventTimestamp = messageTimestamp
            )
        }

        val flightMatch = flightCodeRegex.find(body)
        if (flightContextRegex.containsMatchIn(body) && (flightMatch != null || pnrMatch != null)) {
            val flightCode = flightMatch?.let { "${it.groupValues[1]}-${it.groupValues[2]}" }
            val ref = pnrMatch ?: flightCode ?: return null
            val route = routeRegex.find(body)?.let { "${it.groupValues[1]} → ${it.groupValues[2]}" }
            val operator = SenderIdUtils.inferInstitution(address, body) ?: "Airline"
            val status = when {
                body.contains("delayed", ignoreCase = true) -> "Delayed"
                body.contains("cancelled", ignoreCase = true) -> "Cancelled"
                body.contains("check-in", ignoreCase = true) || body.contains("web check", ignoreCase = true) -> "Check-in Open"
                else -> "Scheduled"
            }
            return ParsedTransitOrder(
                kind = TransitOrderKind.FLIGHT,
                referenceCode = ref,
                carrierOrOperator = operator,
                summary = listOfNotNull(flightCode, route).joinToString(" • ").ifBlank { "Flight $ref" },
                status = status,
                eventTimestamp = messageTimestamp
            )
        }

        if (deliveryContextRegex.containsMatchIn(body)) {
            val orderRef = orderIdRegex.find(body)?.groupValues?.getOrNull(1)
            val status = when {
                body.contains("out for delivery", ignoreCase = true) -> "Out for Delivery"
                body.contains("arriving today", ignoreCase = true) -> "Arriving Today"
                body.contains("delivered", ignoreCase = true) -> "Delivered"
                body.contains("shipped", ignoreCase = true) || body.contains("dispatched", ignoreCase = true) -> "Shipped"
                else -> "In Transit"
            }
            val operator = SenderIdUtils.inferInstitution(address, body) ?: "Courier"
            val ref = orderRef ?: operator
            return ParsedTransitOrder(
                kind = TransitOrderKind.DELIVERY,
                referenceCode = ref,
                carrierOrOperator = operator,
                summary = "$operator • $status" + (orderRef?.let { " ($it)" } ?: ""),
                status = status,
                eventTimestamp = messageTimestamp
            )
        }

        return null
    }
}
