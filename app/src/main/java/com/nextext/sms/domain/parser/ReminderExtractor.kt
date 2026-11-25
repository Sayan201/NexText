package com.nextext.sms.domain.parser

import com.nextext.sms.domain.model.ParsedReminder
import com.nextext.sms.domain.model.ReminderKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

object ReminderExtractor {

    private val dueTriggerRegex = Regex(
        pattern = """\b(due\s+(?:on|by|date)|last\s+date|pay\s+before|expires?\s+(?:on|today|tomorrow)|expiring\s+(?:on|soon)|emi\s+of|statement\s+generated|minimum\s+amount\s+due|bill\s+of|recharge\s+due)\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val paidAlreadyRegex = Regex(
        pattern = """\b(thank\s+you\s+for\s+paying|payment\s+received|bill\s+paid|successfully\s+recharged|debited|paid\s+to)\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val amountRegex = Regex(
        pattern = """(₹|rs\.?|inr|\$|usd|€|eur|£|gbp)\s*([0-9][0-9,]*(?:\.\d{1,2})?)""",
        option = RegexOption.IGNORE_CASE
    )

    private val numericDateRegex = Regex("""\b(\d{1,2})[/\-](\d{1,2})[/\-](\d{2,4})\b""")
    private val textDateRegex = Regex(
        """\b(\d{1,2})[\s\-]([A-Za-z]{3,9})(?:[\s,\-]+(\d{2,4}))?\b""",
        RegexOption.IGNORE_CASE
    )

    private val monthYearFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMM yyyy")
        .toFormatter(Locale.ENGLISH)

    private val fullMonthYearFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMMM yyyy")
        .toFormatter(Locale.ENGLISH)

    fun extract(address: String, body: String, messageTimestamp: Long): ParsedReminder? {
        if (!dueTriggerRegex.containsMatchIn(body)) return null
        if (paidAlreadyRegex.containsMatchIn(body) && !body.contains("due", ignoreCase = true)) {
            return null
        }

        val zone = ZoneId.systemDefault()
        val msgDate = Instant.ofEpochMilli(messageTimestamp).atZone(zone).toLocalDate()
        val dueDate = parseDueDate(body, msgDate) ?: return null

        val amountMatch = amountRegex.find(body)
        val currency = amountMatch?.groupValues?.getOrNull(1)?.let { TransactionExtractor.normalizeCurrency(it) }
        val amount = amountMatch?.groupValues?.getOrNull(2)?.replace(",", "")?.toDoubleOrNull()

        val institution = SenderIdUtils.inferInstitution(address, body) ?: "Bill"
        val lower = body.lowercase()
        val kind = when {
            lower.contains("emi") || lower.contains("loan") -> ReminderKind.EMI
            lower.contains("recharge") || lower.contains("plan") || lower.contains("validity") -> ReminderKind.RECHARGE
            lower.contains("flight") -> ReminderKind.FLIGHT
            lower.contains("train") || lower.contains("irctc") -> ReminderKind.TRAIN
            else -> ReminderKind.BILL
        }

        val title = when (kind) {
            ReminderKind.EMI -> "$institution EMI Due"
            ReminderKind.RECHARGE -> "$institution Recharge Due"
            ReminderKind.BILL -> "$institution Bill Due"
            ReminderKind.FLIGHT -> "$institution Flight"
            ReminderKind.TRAIN -> "$institution Train"
            ReminderKind.CUSTOM -> "$institution Reminder"
        }

        val dueMillis = dueDate.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        return ParsedReminder(
            title = title,
            description = body.trim().take(160),
            amount = amount,
            currency = currency,
            dueTimestamp = dueMillis,
            kind = kind
        )
    }

    private fun parseDueDate(body: String, referenceDate: LocalDate): LocalDate? {
        val lower = body.lowercase()
        if (lower.contains("tomorrow")) return referenceDate.plusDays(1)
        if (lower.contains("today")) return referenceDate

        numericDateRegex.find(body)?.let { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return@let
            val month = m.groupValues[2].toIntOrNull() ?: return@let
            val rawYear = m.groupValues[3].toIntOrNull() ?: return@let
            val year = if (rawYear < 100) 2000 + rawYear else rawYear
            runCatching { LocalDate.of(year, month, day) }.getOrNull()?.let { return it }
        }

        textDateRegex.find(body)?.let { m ->
            val day = m.groupValues[1]
            val month = m.groupValues[2]
            val rawYear = m.groupValues[3].toIntOrNull()
            val year = when {
                rawYear == null -> referenceDate.year
                rawYear < 100 -> 2000 + rawYear
                else -> rawYear
            }
            val candidateStr = "$day $month $year"
            val parsed = runCatching { LocalDate.parse(candidateStr, monthYearFormatter) }.getOrNull()
                ?: runCatching { LocalDate.parse(candidateStr, fullMonthYearFormatter) }.getOrNull()
            if (parsed != null) {
                return if (rawYear == null && parsed.isBefore(referenceDate.minusDays(7))) {
                    parsed.plusYears(1)
                } else {
                    parsed
                }
            }
        }
        return null
    }
}
