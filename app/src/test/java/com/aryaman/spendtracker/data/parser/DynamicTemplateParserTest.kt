package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.local.entity.SmsTemplateEntity
import com.aryaman.spendtracker.data.model.TransactionDirection
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class DynamicTemplateParserTest {

    @Test
    fun testHdfcCreditCardSmsSynthesisAndExecution() {
        val sample = "A transaction of Rs. 179.49 was made using your HDFC Bank Pixel Play Credit Card at zomato.eternaltsp.payu@hdfcbank via UPI 626963138975 on 26/09/26 at 19:44. Not you? Block your Card: https://1.hdfc.bank.in"

        // 1. Test auto detection
        val detected = TemplateSynthesizer.autoDetectSlots(sample)
        assertEquals("179.49", detected.amount)
        assertEquals("26/09/26", detected.date)
        assertEquals("19:44", detected.time)
        assertEquals("626963138975", detected.ref)
        assertEquals(TransactionDirection.DEBIT, detected.direction)

        // 2. Synthesize
        val result = TemplateSynthesizer.synthesize(
            sampleText = sample,
            amountSnippet = detected.amount!!,
            merchantSnippet = "zomato.eternaltsp.payu@hdfcbank",
            dateSnippet = detected.date,
            timeSnippet = detected.time,
            accountSnippet = "Pixel Play Credit Card",
            refSnippet = detected.ref,
            direction = detected.direction,
            bankName = "HDFC Pixel Play",
            senderFilter = "HDFC"
        )

        if (result is SynthesisResult.Error) {
            fail(result.message)
        }
        val success = result as SynthesisResult.Success
        assertEquals(179.49, success.preview.amount, 0.001)
        assertEquals("Zomato", success.preview.cleanMerchant)
        assertEquals("Pixel Play Credit Card", success.preview.accountInfo)
        assertEquals("626963138975", success.preview.referenceNumber)

        // 3. Test on a DIFFERENT message from the same bank format
        val secondSms = "A transaction of Rs. 840.50 was made using your HDFC Bank Pixel Play Credit Card at swiggy.payu@hdfcbank via UPI 999888777666 on 28/09/26 at 20:15. Not you? Block your Card: https://1.hdfc.bank.in"
        val template = SmsTemplateEntity(
            bankName = "HDFC Pixel Play",
            senderFilter = "HDFC",
            regexPattern = success.regexPattern,
            direction = TransactionDirection.DEBIT,
            sampleText = sample
        )

        val parsedSecond = DynamicTemplateParser.parseSingle(secondSms, System.currentTimeMillis(), template)
        assertNotNull(parsedSecond)
        assertEquals(840.50, parsedSecond!!.amount, 0.001)
        assertEquals("Swiggy", parsedSecond.cleanMerchant)
        assertEquals("999888777666", parsedSecond.referenceNumber)
    }

    @Test
    fun testDateOnlyMultilineSmsFusesSmsTime() {
        val sample = "Sent Rs.25.00\nFrom HDFC Bank A/C *5164\nTo ANUGRHA STORE\nOn 26/09/26\nRef 626954558817"

        val detected = TemplateSynthesizer.autoDetectSlots(sample)
        assertEquals("25.00", detected.amount)
        assertEquals("26/09/26", detected.date)
        assertNull(detected.time) // No time in text!

        val result = TemplateSynthesizer.synthesize(
            sampleText = sample,
            amountSnippet = detected.amount!!,
            merchantSnippet = "ANUGRHA STORE",
            dateSnippet = detected.date,
            timeSnippet = null,
            accountSnippet = "*5164",
            refSnippet = "626954558817",
            direction = TransactionDirection.DEBIT,
            bankName = "HDFC UPI",
            senderFilter = "HDFC"
        )

        if (result is SynthesisResult.Error) {
            fail(result.message)
        }
        val success = result as SynthesisResult.Success

        val template = SmsTemplateEntity(
            bankName = "HDFC UPI",
            senderFilter = "HDFC",
            regexPattern = success.regexPattern,
            direction = TransactionDirection.DEBIT,
            sampleText = sample
        )

        // Simulate SMS received at 3:45:12 PM (15:45:12)
        val calSms = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 26, 15, 45, 12)
        }
        val smsTimestamp = calSms.timeInMillis

        val parsed = DynamicTemplateParser.parseSingle(sample, smsTimestamp, template)
        assertNotNull(parsed)
        assertEquals(25.00, parsed!!.amount, 0.001)
        assertEquals("Anugrha Store", parsed.cleanMerchant)

        val calResult = Calendar.getInstance().apply { timeInMillis = parsed.timestampMillis }
        // Verify time was preserved from SMS delivery rather than midnight!
        assertEquals(15, calResult.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, calResult.get(Calendar.MINUTE))
    }

    @Test
    fun testIciciBankCreditSms() {
        val sample = "INR 3,500.00 credited to your ICICI Bank A/C XX0129 on 28-SEP-26 by RAMESH. UPI Ref 112233445566"

        val detected = TemplateSynthesizer.autoDetectSlots(sample)
        assertEquals("3,500.00", detected.amount)
        assertEquals(TransactionDirection.CREDIT, detected.direction)
        assertEquals("28-SEP-26", detected.date)
        assertEquals("112233445566", detected.ref)

        val result = TemplateSynthesizer.synthesize(
            sampleText = sample,
            amountSnippet = detected.amount!!,
            merchantSnippet = "RAMESH",
            dateSnippet = detected.date,
            timeSnippet = null,
            accountSnippet = "XX0129",
            refSnippet = detected.ref,
            direction = detected.direction,
            bankName = "ICICI Bank",
            senderFilter = "ICICI"
        )

        if (result is SynthesisResult.Error) {
            fail(result.message)
        }
        val success = result as SynthesisResult.Success
        assertEquals(3500.00, success.preview.amount, 0.001)
        assertEquals(TransactionDirection.CREDIT, success.preview.direction)
        assertEquals("Ramesh", success.preview.cleanMerchant)
    }
}
