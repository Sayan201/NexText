package com.nextext.sms.ui.components

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextext.sms.data.local.MessageEntity
import com.nextext.sms.ui.util.Formatters
import java.util.Locale

sealed interface SmartSmsCard {
    data class TicketCard(
        val kind: TicketKind,
        val headerTitle: String,
        val primaryLabel: String,
        val primaryValue: String,
        val dateText: String?,
        val timeText: String?,
        val fromCode: String?,
        val toCode: String?,
        val venueText: String?,
        val pnrOrBooking: String?,
        val seatOrClass: String?,
        val fareText: String?
    ) : SmartSmsCard

    data class OtpCard(
        val otpCode: String,
        val summary: String
    ) : SmartSmsCard

    data class BankingCard(
        val kind: BankingKind,
        val title: String,
        val subtitle: String,
        val amountText: String,
        val summary: String
    ) : SmartSmsCard

    data class BillingCard(
        val title: String,
        val subtitle: String,
        val amountText: String?,
        val summary: String
    ) : SmartSmsCard

    data class DeliveryCard(
        val statusHeadline: String,
        val summary: String
    ) : SmartSmsCard
}

enum class TicketKind { TRAIN, FLIGHT, MOVIE }
enum class BankingKind { CREDIT, DEBIT, FAILED }

object SmartSmsCardParser {
    private val AMOUNT_REGEX = Regex(
        """(?:INR|Rs\.?|₹|USD|\$|EUR|€|GBP|£)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    private val PNR_REGEX = Regex("""\bPNR\s*[:#-]?\s*([A-Z0-9]{6,12})\b""", RegexOption.IGNORE_CASE)
    private val BOOKING_REGEX = Regex(
        """\b(?:Booking\s*ID|Ref(?:erence)?|Ticket\s*ID|Order\s*ID)\s*[:#-]?\s*([A-Z0-9-]{5,16})\b""",
        RegexOption.IGNORE_CASE
    )
    private val TRAIN_NO_REGEX = Regex("""\b(?:Train(?:\s*No\.?)?|Trn)\s*[:#-]?\s*(\d{4,5})\b""", RegexOption.IGNORE_CASE)
    private val FLIGHT_NO_REGEX = Regex("""\b([A-Z0-9]{2}\s*-?\s*\d{2,4})\b""")
    private val ROUTE_REGEX = Regex("""\b([A-Z]{2,4})\s*(?:to|->|-|–|—)\s*([A-Z]{2,4})\b""")
    private val DATE_REGEX = Regex(
        """\b(\d{1,2}[-/]\d{1,2}[-/]\d{2,4}|\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*(?:\s+\d{2,4})?)\b""",
        RegexOption.IGNORE_CASE
    )
    private val TIME_REGEX = Regex("""\b(\d{1,2}:\d{2}(?:\s*[AP]M)?)\b""", RegexOption.IGNORE_CASE)
    private val SEAT_REGEX = Regex(
        """\b(?:Seat(?:s)?|Coach|Berth)\s*[:#-]?\s*([A-Z0-9]{1,4}(?:[-/]\d{1,3})?(?:,\s*[A-Z0-9]{1,4})*)""",
        RegexOption.IGNORE_CASE
    )
    private val COACH_SEAT_COMPACT_REGEX = Regex("""\b([A-Z]{1,2}\d{1,2}[-/]\d{1,3})\b""")

    fun parse(message: MessageEntity): SmartSmsCard? {
        if (!message.isIncoming) return null
        val body = message.body.trim()
        if (body.isEmpty()) return null
        val lower = body.lowercase(Locale.getDefault())

        // 1. OTP Card (highest priority when OTP code is extracted and not a ticket)
        val otp = message.otpCode?.takeIf { it.isNotBlank() }
        if (otp != null) {
            return SmartSmsCard.OtpCard(
                otpCode = otp,
                summary = body
            )
        }

        // 2. Ticket Cards (Train / Flight / Movie)
        parseTicket(body, lower)?.let { return it }

        // 3. Banking Cards (Credit / Debit / Failed)
        parseBanking(body, lower)?.let { return it }

        // 4. Billing / Mobile Recharge Cards
        parseBilling(body, lower)?.let { return it }

        // 5. Delivery Status Cards
        parseDelivery(body, lower)?.let { return it }

        return null
    }

    private fun parseTicket(body: String, lower: String): SmartSmsCard.TicketCard? {
        val isTrain = lower.contains("irctc") || lower.contains("train") ||
            (lower.contains("pnr") && (lower.contains("coach") || lower.contains("berth") || lower.contains("sleeper") || lower.contains("wl") || lower.contains("rac")))
        val isFlight = !isTrain && (lower.contains("flight") || lower.contains("boarding") ||
            lower.contains("indigo") || lower.contains("air india") || lower.contains("vistara") ||
            lower.contains("spicejet") || lower.contains("akasa") || lower.contains("airlines"))
        val isMovie = !isTrain && !isFlight && (lower.contains("bookmyshow") || lower.contains("pvr") ||
            lower.contains("inox") || lower.contains("cinepolis") || lower.contains("cinema") ||
            lower.contains("movie") || lower.contains("showtime") || lower.contains("audi") ||
            lower.contains("e-ticket"))

        if (!isTrain && !isFlight && !isMovie) return null

        val dateText = DATE_REGEX.find(body)?.groupValues?.getOrNull(1)
        val timeText = TIME_REGEX.find(body)?.groupValues?.getOrNull(1)
        val pnr = PNR_REGEX.find(body)?.groupValues?.getOrNull(1)
            ?: BOOKING_REGEX.find(body)?.groupValues?.getOrNull(1)
        val fareMatch = AMOUNT_REGEX.find(body)?.value
        val routeMatch = ROUTE_REGEX.find(body)
        val fromCode = routeMatch?.groupValues?.getOrNull(1)
        val toCode = routeMatch?.groupValues?.getOrNull(2)
        val seat = COACH_SEAT_COMPACT_REGEX.find(body)?.groupValues?.getOrNull(1)
            ?: SEAT_REGEX.find(body)?.groupValues?.getOrNull(1)

        return when {
            isTrain -> {
                val trainNo = TRAIN_NO_REGEX.find(body)?.groupValues?.getOrNull(1) ?: "Train"
                SmartSmsCard.TicketCard(
                    kind = TicketKind.TRAIN,
                    headerTitle = "TRAIN TICKET",
                    primaryLabel = "Train No.",
                    primaryValue = trainNo,
                    dateText = dateText,
                    timeText = timeText,
                    fromCode = fromCode,
                    toCode = toCode,
                    venueText = null,
                    pnrOrBooking = pnr,
                    seatOrClass = seat,
                    fareText = fareMatch
                )
            }
            isFlight -> {
                val flightNo = FLIGHT_NO_REGEX.find(body)?.groupValues?.getOrNull(1) ?: "Flight"
                SmartSmsCard.TicketCard(
                    kind = TicketKind.FLIGHT,
                    headerTitle = "FLIGHT TICKET",
                    primaryLabel = "Flight No.",
                    primaryValue = flightNo,
                    dateText = dateText,
                    timeText = timeText,
                    fromCode = fromCode,
                    toCode = toCode,
                    venueText = null,
                    pnrOrBooking = pnr,
                    seatOrClass = seat,
                    fareText = fareMatch
                )
            }
            else -> {
                val movieTitle = Regex("""(?:for|movie|show)\s*[:\-]?\s*([A-Za-z0-9 :'&!-]{3,32})""", RegexOption.IGNORE_CASE)
                    .find(body)?.groupValues?.getOrNull(1)?.trim()
                    ?: "Movie Ticket"
                val venue = Regex("""(?:at|venue|cinema)\s*[:\-]?\s*([A-Za-z0-9 ,.&-]{3,36})""", RegexOption.IGNORE_CASE)
                    .find(body)?.groupValues?.getOrNull(1)?.trim()
                SmartSmsCard.TicketCard(
                    kind = TicketKind.MOVIE,
                    headerTitle = "MOVIE TICKET",
                    primaryLabel = "Show",
                    primaryValue = movieTitle,
                    dateText = dateText,
                    timeText = timeText,
                    fromCode = null,
                    toCode = null,
                    venueText = venue,
                    pnrOrBooking = pnr,
                    seatOrClass = seat,
                    fareText = fareMatch
                )
            }
        }
    }

    private fun parseBanking(body: String, lower: String): SmartSmsCard.BankingCard? {
        val isFailed = lower.contains("failed") || lower.contains("declined") || lower.contains("unsuccessful")
        val isCredit = !isFailed && (lower.contains("credited") || lower.contains("received") || lower.contains("deposited"))
        val isDebit = !isFailed && !isCredit && (lower.contains("debited") || lower.contains("spent") ||
            lower.contains("withdrawn") || lower.contains("paid") || lower.contains("deducted"))

        if (!isFailed && !isCredit && !isDebit) return null
        val isBankContext = lower.contains("a/c") || lower.contains("acct") || lower.contains("account") ||
            lower.contains("card") || lower.contains("upi") || lower.contains("bank") || lower.contains("vpa")
        if (!isBankContext) return null

        val amount = AMOUNT_REGEX.find(body)?.value
        return when {
            isFailed -> SmartSmsCard.BankingCard(
                kind = BankingKind.FAILED,
                title = "Banking",
                subtitle = "Payment failed",
                amountText = "Failed",
                summary = body
            )
            isCredit && amount != null -> SmartSmsCard.BankingCard(
                kind = BankingKind.CREDIT,
                title = "Banking",
                subtitle = "Credit amount",
                amountText = amount,
                summary = body
            )
            isDebit && amount != null -> SmartSmsCard.BankingCard(
                kind = BankingKind.DEBIT,
                title = "Banking",
                subtitle = "Debit amount",
                amountText = amount,
                summary = body
            )
            else -> null
        }
    }

    private fun parseBilling(body: String, lower: String): SmartSmsCard.BillingCard? {
        val isRecharge = lower.contains("recharge") || (lower.contains("prepaid") && lower.contains("plan")) ||
            lower.contains("validity") || lower.contains("expir")
        val isBill = lower.contains("bill") || lower.contains("total due") || lower.contains("amount due") ||
            lower.contains("min due") || lower.contains("emi")
        if (!isRecharge && !isBill) return null

        val amount = AMOUNT_REGEX.find(body)?.value
        val dateText = DATE_REGEX.find(body)?.groupValues?.getOrNull(1)
        return if (isRecharge) {
            SmartSmsCard.BillingCard(
                title = "Mobile Recharge",
                subtitle = dateText?.let { "Expiry $it" } ?: "Plan update",
                amountText = amount,
                summary = body
            )
        } else {
            SmartSmsCard.BillingCard(
                title = "Billing",
                subtitle = dateText?.let { "Due $it" } ?: "Total due",
                amountText = amount,
                summary = body
            )
        }
    }

    private fun parseDelivery(body: String, lower: String): SmartSmsCard.DeliveryCard? {
        val headline = when {
            lower.contains("out for delivery") -> "Out for delivery"
            lower.contains("arriving today") -> "Arriving today"
            lower.contains("arriving") -> "Arriving soon"
            lower.contains("delivered") -> "Delivered"
            lower.contains("shipped") || lower.contains("dispatched") -> "Shipped"
            else -> return null
        }
        return SmartSmsCard.DeliveryCard(
            statusHeadline = headline,
            summary = body
        )
    }
}

@Composable
fun HyperOsSmartSmsCard(
    message: MessageEntity,
    card: SmartSmsCard,
    onCopyOtp: (String) -> Unit,
    onCopyCode: (String, String) -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var showRawSms by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .widthIn(max = 330.dp)
                .border(
                    width = 0.7.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (card) {
                    is SmartSmsCard.TicketCard -> TicketCardBody(
                        card = card,
                        onCopyPnr = { pnr -> onCopyCode(pnr, "PNR / Booking ID") },
                        onAddToCalendar = { openCalendarForTicket(context, card) }
                    )
                    is SmartSmsCard.OtpCard -> OtpCardBody(
                        card = card,
                        onCopyOtp = onCopyOtp
                    )
                    is SmartSmsCard.BankingCard -> BankingCardBody(card = card)
                    is SmartSmsCard.BillingCard -> BillingCardBody(
                        card = card,
                        onAddToCalendar = { openCalendarForBilling(context, card) }
                    )
                    is SmartSmsCard.DeliveryCard -> DeliveryCardBody(card = card)
                }

                AnimatedVisibility(visible = showRawSms) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = message.body,
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // Footer row: timestamp + star + "Full SMS" toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = Formatters.formatDateTime(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8E8E93)
                        )
                        if (message.isStarred) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Starred",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = if (showRawSms) "Hide SMS ▴" else "Full SMS ▾",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRawSms = !showRawSms }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketCardBody(
    card: SmartSmsCard.TicketCard,
    onCopyPnr: (String) -> Unit,
    onAddToCalendar: () -> Unit
) {
    val headerColor = when (card.kind) {
        TicketKind.TRAIN -> Color(0xFF26A69A)
        TicketKind.FLIGHT -> Color(0xFF0284C7)
        TicketKind.MOVIE -> Color(0xFFE11D48)
    }
    val headerIcon = when (card.kind) {
        TicketKind.TRAIN -> Icons.Default.DirectionsTransit
        TicketKind.FLIGHT -> Icons.Default.Flight
        TicketKind.MOVIE -> Icons.Default.Movie
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Colored Ticket Header Bar (like Photo 2: TRAIN TICKET)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = headerIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = card.headerTitle,
                // On the coloured header bar in both themes.
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        // Top section: Train/Flight/Show + Date & Time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.primaryLabel,
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
                Text(
                    text = card.primaryValue,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (card.dateText != null || card.timeText != null) {
                Column(horizontalAlignment = Alignment.End) {
                    if (card.dateText != null) {
                        Text(
                            text = card.dateText,
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    }
                    if (card.timeText != null) {
                        Text(
                            text = card.timeText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Route (HYB -> LUR) or Venue
        if (card.fromCode != null && card.toCode != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.fromCode,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(14.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = headerColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = card.toCode,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (!card.venueText.isNullOrBlank()) {
            Text(
                text = card.venueText,
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(6.dp))

        // Perforated Boarding-Pass Divider with Semicircle Notches.
        // Colours are resolved here, not inside the draw scope: the Canvas
        // lambda is not @Composable.
        val dividerDashColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
        val dividerNotchColor = MaterialTheme.colorScheme.background
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val midY = size.height / 2f
            val notchRadius = 8.dp.toPx()
            drawLine(
                color = dividerDashColor,
                start = Offset(notchRadius + 4.dp.toPx(), midY),
                end = Offset(size.width - notchRadius - 4.dp.toPx(), midY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
            drawCircle(
                color = dividerNotchColor,
                radius = notchRadius,
                center = Offset(0f, midY)
            )
            drawCircle(
                color = dividerNotchColor,
                radius = notchRadius,
                center = Offset(size.width, midY)
            )
        }

        // PNR & Seat/Fare section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!card.pnrOrBooking.isNullOrBlank()) {
                Text(
                    text = if (card.kind == TicketKind.MOVIE) "BOOKING: ${card.pnrOrBooking}" else "PNR: ${card.pnrOrBooking}",
                    color = headerColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (!card.seatOrClass.isNullOrBlank() || !card.fareText.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (!card.seatOrClass.isNullOrBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Seat / Coach", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            Text(
                                text = card.seatOrClass,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (!card.fareText.isNullOrBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Fare", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            Text(
                                text = card.fareText,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Bottom 2-action row: Copy PNR | Add to Calendar
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!card.pnrOrBooking.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCopyPnr(card.pnrOrBooking) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = headerColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (card.kind == TicketKind.MOVIE) "Copy Booking" else "Copy PNR",
                        color = headerColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAddToCalendar)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = headerColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Add to Calendar",
                    color = headerColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OtpCardBody(
    card: SmartSmsCard.OtpCard,
    onCopyOtp: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Password,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OTP",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = card.otpCode,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    .clickable { onCopyOtp(card.otpCode) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "COPY",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
        Spacer(Modifier.height(8.dp))
        Text(
            text = card.summary,
            color = Color(0xFF9CA3AF),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun BankingCardBody(card: SmartSmsCard.BankingCard) {
    val accentBar = when (card.kind) {
        BankingKind.CREDIT -> Color(0xFF10B981)
        BankingKind.DEBIT -> Color(0xFFF59E0B)
        BankingKind.FAILED -> Color(0xFFEF4444)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentBar)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = card.subtitle,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }
            Text(
                text = card.amountText,
                color = if (card.kind == BankingKind.FAILED) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
        Spacer(Modifier.height(8.dp))
        Text(
            text = card.summary,
            color = Color(0xFF9CA3AF),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun BillingCardBody(
    card: SmartSmsCard.BillingCard,
    onAddToCalendar: () -> Unit
) {
    val accentBar = Color(0xFFF59E0B)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentBar)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = card.subtitle,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }
            if (!card.amountText.isNullOrBlank()) {
                Text(
                    text = card.amountText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
        Spacer(Modifier.height(8.dp))
        Text(
            text = card.summary,
            color = Color(0xFF9CA3AF),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                .clickable(onClick = onAddToCalendar)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add Reminder to Calendar",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DeliveryCardBody(card: SmartSmsCard.DeliveryCard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Delivery Status",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
                Text(
                    text = card.statusHeadline,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
        Spacer(Modifier.height(8.dp))
        Text(
            text = card.summary,
            color = Color(0xFF9CA3AF),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}

private fun openCalendarForTicket(context: Context, card: SmartSmsCard.TicketCard) {
    val routePart = if (card.fromCode != null && card.toCode != null) {
        " (${card.fromCode} → ${card.toCode})"
    } else ""
    val title = "${card.headerTitle}: ${card.primaryValue}$routePart"
    val desc = buildString {
        card.pnrOrBooking?.let { append("PNR / Booking: $it\n") }
        card.seatOrClass?.let { append("Seat / Coach: $it\n") }
        card.dateText?.let { append("Date: $it ") }
        card.timeText?.let { append("Time: $it") }
    }.trim()

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, desc)
        card.venueText?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
    }
    runCatching { context.startActivity(intent) }
}

private fun openCalendarForBilling(context: Context, card: SmartSmsCard.BillingCard) {
    val title = buildString {
        append(card.title)
        card.amountText?.let { append(" • $it") }
    }
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, card.summary)
    }
    runCatching { context.startActivity(intent) }
}
