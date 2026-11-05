package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.model.TransactionSource
import java.text.SimpleDateFormat
import java.util.Locale

object HdfcSmsParser {

    /**
     * Checks if sender matches HDFC Bank DLT headers (e.g., AD-HDFCT, VM-HDFCS, HDFCBK, etc.)
     */
    fun isHdfcSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val s = sender.uppercase(Locale.ROOT)
        return s.contains("HDFC")
    }

    /**
     * Parses an incoming HDFC SMS string into a ParsedTransaction if it represents a financial event.
     */
    fun parse(smsBody: String, smsTimestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val trimmed = smsBody.trim()

        // 1. Pixel Play / Credit Card via UPI pattern
        // "A transaction of Rs. 179.49 was made using your HDFC Bank Pixel Play Credit Card at zomato.eternaltsp.payu@hdfcbank via UPI 626963138975 on 26/09/26 at 19:44..."
        val ccUpiRegex = Regex(
            """(?:transaction of|spent)\s+(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)\s+was made using your HDFC Bank\s+(.+?)\s+at\s+(.+?)\s+via UPI\s+(\d+)\s+on\s+(\d{2}/\d{2}/\d{2})\s+at\s+(\d{2}:\d{2})""",
            RegexOption.IGNORE_CASE
        )
        val ccUpiMatch = ccUpiRegex.find(trimmed)
        if (ccUpiMatch != null) {
            val amountStr = ccUpiMatch.groupValues[1].replace(",", "")
            val cardName = ccUpiMatch.groupValues[2].trim()
            val rawMerchant = ccUpiMatch.groupValues[3].trim()
            val refNumber = ccUpiMatch.groupValues[4].trim()
            val dateStr = ccUpiMatch.groupValues[5].trim()
            val timeStr = ccUpiMatch.groupValues[6].trim()

            val amount = amountStr.toDoubleOrNull() ?: return null
            val timestamp = parseDateTime(dateStr, timeStr) ?: smsTimestamp

            return ParsedTransaction(
                amount = amount,
                direction = TransactionDirection.DEBIT,
                rawMerchant = rawMerchant,
                cleanMerchant = cleanMerchantName(rawMerchant),
                accountInfo = cardName,
                referenceNumber = refNumber,
                timestampMillis = timestamp,
                source = TransactionSource.SMS,
                rawText = trimmed,
                isCreditCard = true
            )
        }

        // 2. Direct HDFC Credit Card Spend Pattern (POS, Swipes, Online E-comm)
        // e.g. "Alert: You've spent Rs.1,500.00 on HDFC Bank Credit Card ending 1234 at AMAZON on 24-09-26..."
        // e.g. "Thank you for using HDFC Bank Credit Card ending 1234 for Rs 550.00 at SWIGGY..."
        val directCcRegex = Regex(
            """(?:spent|using|spent\s+Rs\.?|paid)\s+(?:Rs\.?|INR)?\s*([\d,]+(?:\.\d{1,2})?)\s+(?:on|using|for)?\s*HDFC\s+Bank\s+(?:Credit\s+Card|Card)\s*(?:ending)?\s*\*?(\w+)?\s+(?:at|to|for|on)\s+([A-Za-z0-9\s._-]+?)(?:\s+on|\.|\s+at|\s+via|\s+Bal|$)""",
            RegexOption.IGNORE_CASE
        )
        val directCcMatch = directCcRegex.find(trimmed)
        if (directCcMatch != null) {
            val amountStr = directCcMatch.groupValues[1].replace(",", "")
            val cardEnding = directCcMatch.groupValues.getOrNull(2)?.trim()?.ifEmpty { null }
            val rawMerchant = directCcMatch.groupValues.getOrNull(3)?.trim() ?: "Unknown"

            val amount = amountStr.toDoubleOrNull()
            if (amount != null) {
                return ParsedTransaction(
                    amount = amount,
                    direction = TransactionDirection.DEBIT,
                    rawMerchant = rawMerchant,
                    cleanMerchant = cleanMerchantName(rawMerchant),
                    accountInfo = if (cardEnding != null) "HDFC CC *$cardEnding" else "HDFC Credit Card",
                    referenceNumber = extractReferenceNumber(trimmed),
                    timestampMillis = smsTimestamp,
                    source = TransactionSource.SMS,
                    rawText = trimmed,
                    isCreditCard = true
                )
            }
        }

        // 2. Multi-line Account UPI Debit pattern
        // "Sent Rs.25.00\nFrom HDFC Bank A/C *5164\nTo ANUGRHA STORE\nOn 26/09/26\nRef 626954558817"
        val sentUpiRegex = Regex(
            """Sent\s+(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)\s+From\s+HDFC\s+Bank\s+A/C\s*\*?(\w+)\s+To\s+(.+?)\s+On\s+(\d{2}/\d{2}/\d{2})\s+Ref\s+(\d+)""",
            RegexOption.IGNORE_CASE
        )
        val sentUpiMatch = sentUpiRegex.find(trimmed)
        if (sentUpiMatch != null) {
            val amountStr = sentUpiMatch.groupValues[1].replace(",", "")
            val acctNumber = sentUpiMatch.groupValues[2].trim()
            val rawMerchant = sentUpiMatch.groupValues[3].trim()
            val dateStr = sentUpiMatch.groupValues[4].trim()
            val refNumber = sentUpiMatch.groupValues[5].trim()

            val amount = amountStr.toDoubleOrNull() ?: return null
            val timestamp = parseDateWithSmsTime(dateStr, smsTimestamp)

            return ParsedTransaction(
                amount = amount,
                direction = TransactionDirection.DEBIT,
                rawMerchant = rawMerchant,
                cleanMerchant = cleanMerchantName(rawMerchant),
                accountInfo = "HDFC *$acctNumber",
                referenceNumber = refNumber,
                timestampMillis = timestamp,
                source = TransactionSource.SMS,
                rawText = trimmed
            )
        }

        // 3. Multi-line Account UPI Credit / Received pattern
        // "Received Rs.500.00 in HDFC Bank A/C *5164 From RAMESH On 25/09/26 Ref 626849833983"
        val receivedUpiRegex = Regex(
            """Received\s+(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)\s+in\s+HDFC\s+Bank\s+A/C\s*\*?(\w+)\s+From\s+(.+?)\s+On\s+(\d{2}/\d{2}/\d{2})\s+Ref\s+(\d+)""",
            RegexOption.IGNORE_CASE
        )
        val receivedMatch = receivedUpiRegex.find(trimmed)
        if (receivedMatch != null) {
            val amountStr = receivedMatch.groupValues[1].replace(",", "")
            val acctNumber = receivedMatch.groupValues[2].trim()
            val senderName = receivedMatch.groupValues[3].trim()
            val dateStr = receivedMatch.groupValues[4].trim()
            val refNumber = receivedMatch.groupValues[5].trim()

            val amount = amountStr.toDoubleOrNull() ?: return null
            val timestamp = parseDateWithSmsTime(dateStr, smsTimestamp)

            return ParsedTransaction(
                amount = amount,
                direction = TransactionDirection.CREDIT,
                rawMerchant = senderName,
                cleanMerchant = cleanMerchantName(senderName),
                accountInfo = "HDFC *$acctNumber",
                referenceNumber = refNumber,
                timestampMillis = timestamp,
                source = TransactionSource.SMS,
                rawText = trimmed
            )
        }

        // 4. Standard Debit Alert (POS / NetBanking / ATM / Card)
        // e.g. "Rs.450.00 debited from a/c **5164 on 24-09-26 to VENDOR. Bal..."
        val genericDebitRegex = Regex(
            """(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)\s+(?:debited|spent|deducted).*?(?:a/c|card|account)\s*(?:\*+|xx)?(\w+)?.*?(?:to|at|info)\s+([A-Za-z0-9\s._-]+?)(?:\.|\s+on|\s+bal|\s+avail|$)""",
            RegexOption.IGNORE_CASE
        )
        val genericMatch = genericDebitRegex.find(trimmed)
        if (genericMatch != null) {
            val amountStr = genericMatch.groupValues[1].replace(",", "")
            val acctNumber = genericMatch.groupValues.getOrNull(2)?.trim()?.ifEmpty { null }
            val rawMerchant = genericMatch.groupValues.getOrNull(3)?.trim() ?: "Unknown"

            val amount = amountStr.toDoubleOrNull() ?: return null

            val isCc = trimmed.contains("credit card", ignoreCase = true) ||
                       trimmed.contains("pixel play", ignoreCase = true) ||
                       trimmed.contains("card ending", ignoreCase = true)

            return ParsedTransaction(
                amount = amount,
                direction = TransactionDirection.DEBIT,
                rawMerchant = rawMerchant,
                cleanMerchant = cleanMerchantName(rawMerchant),
                accountInfo = if (isCc) (acctNumber?.let { "HDFC CC *$it" } ?: "HDFC Credit Card") else acctNumber?.let { "HDFC *$it" },
                referenceNumber = extractReferenceNumber(trimmed),
                timestampMillis = smsTimestamp,
                source = TransactionSource.SMS,
                rawText = trimmed,
                isCreditCard = isCc
            )
        }

        return null
    }

    /**
     * Cleans messy VPA handles or all-caps merchant names into friendly titles.
     * E.g. "zomato.eternaltsp.payu@hdfcbank" -> "Zomato"
     * "ANUGRHA STORE" -> "Anugrha Store"
     */
    fun cleanMerchantName(raw: String): String {
        return MerchantCleaner.clean(raw)
    }

    private fun extractReferenceNumber(text: String): String? {
        val refRegex = Regex("""(?:Ref|UPI|UTR|rrn|txn)\s*(?:no\.?|id|num)?\s*[:\s-]*(\d{9,16})""", RegexOption.IGNORE_CASE)
        return refRegex.find(text)?.groupValues?.get(1)
    }

    private fun parseDateTime(dateStr: String, timeStr: String): Long? {
        return try {
            val format = SimpleDateFormat("dd/MM/yy HH:mm", Locale.ENGLISH)
            format.parse("$dateStr $timeStr")?.time
        } catch (_: Exception) {
            null
        }
    }

    fun parseDateWithSmsTime(dateStr: String, smsTimestamp: Long, pattern: String = "dd/MM/yy"): Long {
        return try {
            val format = SimpleDateFormat(pattern, Locale.ENGLISH)
            val parsedDate = format.parse(dateStr) ?: return smsTimestamp

            val calParsed = java.util.Calendar.getInstance().apply { time = parsedDate }
            val calSms = java.util.Calendar.getInstance().apply { timeInMillis = smsTimestamp }

            calParsed.set(java.util.Calendar.HOUR_OF_DAY, calSms.get(java.util.Calendar.HOUR_OF_DAY))
            calParsed.set(java.util.Calendar.MINUTE, calSms.get(java.util.Calendar.MINUTE))
            calParsed.set(java.util.Calendar.SECOND, calSms.get(java.util.Calendar.SECOND))
            calParsed.set(java.util.Calendar.MILLISECOND, calSms.get(java.util.Calendar.MILLISECOND))

            calParsed.timeInMillis
        } catch (_: Exception) {
            smsTimestamp
        }
    }
}
