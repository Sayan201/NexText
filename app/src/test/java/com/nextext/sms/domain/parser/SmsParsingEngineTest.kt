package com.nextext.sms.domain.parser

import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.domain.model.ReminderKind
import com.nextext.sms.domain.model.SpendCategory
import com.nextext.sms.domain.model.TransactionType
import com.nextext.sms.domain.model.TransitOrderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParsingEngineTest {

    private val refTimestamp = 1795610000000L // Nov 2026

    @Test
    fun parsesHdfcUpiDebitWithBalance() {
        val result = SmsParsingEngine.parse(
            address = "AD-HDFCBK",
            body = "Rs. 1,250.50 debited from A/c XX1234 on 25-11-26 to VPA swiggy@okicici via UPI. Avl Bal: Rs. 24,510.75",
            timestamp = refTimestamp
        )

        assertEquals(MessageCategory.TRANSACTIONS, result.category)
        assertNull(result.otp)
        val tx = result.transaction
        assertNotNull(tx)
        assertEquals(1250.50, tx!!.amount, 0.01)
        assertEquals("INR", tx.currency)
        assertEquals(TransactionType.UPI, tx.type)
        assertEquals("HDFC Bank", tx.institution)
        assertEquals("1234", tx.accountLast4)
        assertEquals(24510.75, tx.balanceAfter!!, 0.01)
        assertEquals(SpendCategory.FOOD_DINING, tx.spendCategory)
    }

    @Test
    fun parsesCreditCardSpend() {
        val result = SmsParsingEngine.parse(
            address = "VM-AXISBK",
            body = "Spent INR 4,999.00 on Axis Bank Credit Card no. XX9012 at AMAZON on 25-11-2026.",
            timestamp = refTimestamp
        )

        assertEquals(MessageCategory.TRANSACTIONS, result.category)
        val tx = result.transaction!!
        assertEquals(4999.0, tx.amount, 0.01)
        assertEquals(TransactionType.CARD_SPEND, tx.type)
        assertEquals("9012", tx.accountLast4)
        assertEquals(SpendCategory.SHOPPING, tx.spendCategory)
    }

    @Test
    fun parsesSalaryCredit() {
        val result = SmsParsingEngine.parse(
            address = "VK-SBIINB",
            body = "Dear Customer, INR 75,000.00 credited to your A/c **5678 on 25/11/2026 via NEFT. Available balance is Rs. 98,200.00",
            timestamp = refTimestamp
        )

        assertEquals(MessageCategory.TRANSACTIONS, result.category)
        val tx = result.transaction!!
        assertEquals(75000.0, tx.amount, 0.01)
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals("SBI", tx.institution)
        assertEquals("5678", tx.accountLast4)
        assertEquals(98200.0, tx.balanceAfter!!, 0.01)
    }

    @Test
    fun extractsOtpCorrectly() {
        val result = SmsParsingEngine.parse(
            address = "AX-HDFCBK",
            body = "482910 is your OTP for transaction of Rs. 1,499 at Flipkart. Do not share OTP with anyone.",
            timestamp = refTimestamp
        )

        assertEquals(MessageCategory.TRANSACTIONS, result.category)
        assertNotNull(result.otp)
        assertEquals("482910", result.otp!!.code)
        assertEquals("HDFC Bank", result.otp!!.service)
    }

    @Test
    fun extractsGoogleVerificationCode() {
        val result = SmsParsingEngine.parse(
            address = "GOOGLE",
            body = "G-839201 is your Google verification code.",
            timestamp = refTimestamp
        )

        assertEquals(MessageCategory.TRANSACTIONS, result.category)
        assertEquals("839201", result.otp?.code)
        assertEquals("Google", result.otp?.service)
    }

    @Test
    fun extractsBillDueReminder() {
        val result = SmsParsingEngine.parse(
            address = "JM-ICICIB",
            body = "Your ICICI Bank Credit Card bill of Rs. 14,520.00 is due on 05-Dec-2026. Pay before due date.",
            timestamp = refTimestamp
        )

        assertEquals(MessageCategory.TRANSACTIONS, result.category)
        val rem = result.reminder
        assertNotNull(rem)
        assertEquals(ReminderKind.BILL, rem!!.kind)
        assertEquals(14520.0, rem.amount!!, 0.01)
        assertEquals("INR", rem.currency)
    }

    @Test
    fun extractsIrctcTrainPnr() {
        val result = SmsParsingEngine.parse(
            address = "JD-IRCTCi",
            body = "PNR:4512908123,Train:12951,DOJ:28-11-26,NDLS to MMCT,CNF,B2-45.",
            timestamp = refTimestamp
        )

        assertEquals(MessageCategory.TRANSACTIONS, result.category)
        val tr = result.transitOrder
        assertNotNull(tr)
        assertEquals(TransitOrderKind.TRAIN, tr!!.kind)
        assertEquals("4512908123", tr.referenceCode)
        assertEquals("Confirmed", tr.status)
    }

    @Test
    fun extractsCourierDelivery() {
        val result = SmsParsingEngine.parse(
            address = "AX-DLHVRY",
            body = "Your Delhivery package AWB 7894561230 is out for delivery today.",
            timestamp = refTimestamp
        )

        assertEquals(MessageCategory.TRANSACTIONS, result.category)
        val tr = result.transitOrder
        assertNotNull(tr)
        assertEquals(TransitOrderKind.DELIVERY, tr!!.kind)
        assertEquals("7894561230", tr.referenceCode)
        assertEquals("Out for Delivery", tr.status)
    }

    @Test
    fun classifiesPromotionalDltAsPromotions() {
        val result = SmsParsingEngine.parse(
            address = "AD-MYNTRA-P",
            body = "End of season sale is live! Flat 50% off on top brands. Use code SAVE50. Shop now!",
            timestamp = refTimestamp
        )

        assertEquals(MessageCategory.PROMOTIONS, result.category)
    }

    @Test
    fun classifiesScamAsSpam() {
        val result = SmsParsingEngine.parse(
            address = "+919876500000",
            body = "Congratulations you won Rs 25 Lakh lottery! Claim your prize now by clicking link.",
            timestamp = refTimestamp,
            isKnownContact = false
        )

        assertEquals(MessageCategory.SPAM, result.category)
    }

    @Test
    fun classifiesPersonalContactMessage() {
        val result = SmsParsingEngine.parse(
            address = "+919876543210",
            body = "Hey, are we still meeting for dinner at 8?",
            timestamp = refTimestamp,
            isKnownContact = true
        )

        assertEquals(MessageCategory.PERSONAL, result.category)
    }
}
