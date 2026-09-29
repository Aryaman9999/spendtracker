package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HdfcSmsParserTest {

    @Test
    fun testHdfcSenderVerification() {
        assertTrue(HdfcSmsParser.isHdfcSender("AD-HDFCT"))
        assertTrue(HdfcSmsParser.isHdfcSender("VM-HDFCS"))
        assertTrue(HdfcSmsParser.isHdfcSender("HDFCBK"))
        assertTrue(HdfcSmsParser.isHdfcSender("VK-HDFCBANK"))
    }

    @Test
    fun testPixelPlayCreditCardUpiSms() {
        val sms = "A transaction of Rs. 179.49 was made using your HDFC Bank Pixel Play Credit Card at zomato.eternaltsp.payu@hdfcbank via UPI 626963138975 on 26/09/26 at 19:44. Not you? Block your Card: https://1.hdfc.bank.in/HDFCBK/s/LEVx6R93 or SMS BLOCKPCC 6894 to 8433642286"

        val parsed = HdfcSmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(179.49, parsed!!.amount, 0.001)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals("zomato.eternaltsp.payu@hdfcbank", parsed.rawMerchant)
        assertEquals("Zomato", parsed.cleanMerchant)
        assertEquals("626963138975", parsed.referenceNumber)
        assertEquals("Pixel Play Credit Card", parsed.accountInfo)
    }

    @Test
    fun testAccountUpiDebitMultiline25Rupees() {
        val sms = """
            Sent Rs.25.00
            From HDFC Bank A/C *5164
            To ANUGRHA STORE
            On 26/09/26
            Ref 626954558817
            Not You?
            Call 18002586161/SMS BLOCK UPI to 7308080808
        """.trimIndent()

        val testCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 45)
        }

        val parsed = HdfcSmsParser.parse(sms, testCal.timeInMillis)
        assertNotNull(parsed)
        assertEquals(25.00, parsed!!.amount, 0.001)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals("ANUGRHA STORE", parsed.rawMerchant)
        assertEquals("Anugrha Store", parsed.cleanMerchant)
        assertEquals("HDFC *5164", parsed.accountInfo)
        assertEquals("626954558817", parsed.referenceNumber)

        val resultCal = java.util.Calendar.getInstance().apply { timeInMillis = parsed.timestampMillis }
        assertEquals(9, resultCal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(45, resultCal.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun testAccountUpiDebitMultiline600Rupees() {
        val sms = """
            Sent Rs.600.00
            From HDFC Bank A/C *5164
            To SURYAWANSHI ADHIKARAN GEN
            On 25/09/26
            Ref 626849833983
            Not You?
            Call 18002586161/SMS BLOCK UPI to 7308080808
        """.trimIndent()

        val parsed = HdfcSmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(600.00, parsed!!.amount, 0.001)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals("SURYAWANSHI ADHIKARAN GEN", parsed.rawMerchant)
        assertEquals("Suryawanshi Adhikaran Gen", parsed.cleanMerchant)
        assertEquals("HDFC *5164", parsed.accountInfo)
        assertEquals("626849833983", parsed.referenceNumber)
    }

    @Test
    fun testAccountUpiCreditMultiline() {
        val sms = """
            Received Rs.500.00 in HDFC Bank A/C *5164 From RAMESH On 25/09/26 Ref 626849833983
        """.trimIndent()

        val parsed = HdfcSmsParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(500.00, parsed!!.amount, 0.001)
        assertEquals(TransactionDirection.CREDIT, parsed.direction)
        assertEquals("RAMESH", parsed.rawMerchant)
        assertEquals("Ramesh", parsed.cleanMerchant)
        assertEquals("HDFC *5164", parsed.accountInfo)
        assertEquals("626849833983", parsed.referenceNumber)
    }
}
