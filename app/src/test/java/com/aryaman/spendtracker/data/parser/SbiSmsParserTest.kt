package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SbiSmsParserTest {

    @Test
    fun testSbiSenderVerification() {
        assertTrue(SbiSmsParser.isSbiSender("JM-SBIUPI-S"))
        assertTrue(SbiSmsParser.isSbiSender("VK-SBIUPI"))
        assertTrue(SbiSmsParser.isSbiSender("SBIINB"))
        assertTrue(SbiSmsParser.isSbiSender("SBISMS"))
        assertTrue(SbiSmsParser.isSbiSender("AD-SBIBNK"))
    }

    @Test
    fun testSbiUpiDebitHungerBox() {
        val sms = "Dear UPI user A/C X1618 debited by 25.00 on date 28Sep26 trf to HungerBox Refno 125802739536 If not u? call-1800111109 for other services-18001234-SBI"

        // Simulated SMS receipt time: 12:58 PM (12:58:30)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 58)
            set(Calendar.SECOND, 30)
        }
        val smsTimestamp = cal.timeInMillis

        val parsed = SbiSmsParser.parse(sms, smsTimestamp)
        assertNotNull(parsed)
        assertEquals(25.00, parsed!!.amount, 0.001)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals("HungerBox", parsed.rawMerchant)
        assertEquals("HungerBox", parsed.cleanMerchant)
        assertEquals("SBI *1618", parsed.accountInfo)
        assertEquals("125802739536", parsed.referenceNumber)

        // Verify time is NOT 12:00 AM (midnight)
        val parsedCal = Calendar.getInstance().apply { timeInMillis = parsed.timestampMillis }
        assertEquals(12, parsedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(58, parsedCal.get(Calendar.MINUTE))
        assertNotEquals(0, parsedCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testSbiUpiCreditArushi() {
        val sms = "Dear UPI User, your A/c XXXXXX1618-credited by Rs.533.00 on 29-05-26 transfer from ARUSHI D/O RAJENDRA SINGH BAGRI Ref No 614972592157 -SBI"

        // Simulated SMS receipt time: 9:24 PM (21:24)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 24)
        }
        val smsTimestamp = cal.timeInMillis

        val parsed = SbiSmsParser.parse(sms, smsTimestamp)
        assertNotNull(parsed)
        assertEquals(533.00, parsed!!.amount, 0.001)
        assertEquals(TransactionDirection.CREDIT, parsed.direction)
        assertEquals("ARUSHI D/O RAJENDRA SINGH BAGRI", parsed.rawMerchant)
        assertEquals("Arushi", parsed.cleanMerchant)
        assertEquals("SBI *1618", parsed.accountInfo)
        assertEquals("614972592157", parsed.referenceNumber)

        val parsedCal = Calendar.getInstance().apply { timeInMillis = parsed.timestampMillis }
        assertEquals(21, parsedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(24, parsedCal.get(Calendar.MINUTE))
    }
}
