package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.model.TransactionSource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object SbiSmsParser {

    /**
     * Checks if sender matches State Bank of India DLT headers (e.g., JM-SBIUPI-S, VK-SBIUPI, SBIINB, SBISMS, etc.)
     */
    fun isSbiSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val s = sender.uppercase(Locale.ROOT)
        return s.contains("SBI")
    }

    /**
     * Parses an incoming SBI SMS into a ParsedTransaction.
     */
    fun parse(smsBody: String, smsTimestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val trimmed = smsBody.trim()

        // 1. SBI UPI Debit pattern
        // "Dear UPI user A/C X1618 debited by 25.00 on date 28Sep26 trf to HungerBox Refno 125802739536 If not u? call-1800111109 for other services-18001234-SBI"
        val upiDebitRegex = Regex(
            """Dear UPI user\s+A/C\s*\*?X*(\w+)\s+debited by\s+(?:Rs\.?|INR)?\s*([\d,]+(?:\.\d{1,2})?)\s+on date\s+([A-Za-z0-9/-]+)\s+trf to\s+(.+?)\s+Ref\s*no\s*(\d+)""",
            RegexOption.IGNORE_CASE
        )
        val debitMatch = upiDebitRegex.find(trimmed)
        if (debitMatch != null) {
            val acctLast4 = debitMatch.groupValues[1].takeLast(4)
            val amountStr = debitMatch.groupValues[2].replace(",", "")
            val dateStr = debitMatch.groupValues[3].trim()
            val rawMerchant = debitMatch.groupValues[4].trim()
            val refNumber = debitMatch.groupValues[5].trim()

            val amount = amountStr.toDoubleOrNull() ?: return null
            val timestamp = parseSbiDateWithTime(dateStr, smsTimestamp)

            return ParsedTransaction(
                amount = amount,
                direction = TransactionDirection.DEBIT,
                rawMerchant = rawMerchant,
                cleanMerchant = cleanSbiMerchant(rawMerchant),
                accountInfo = "SBI *$acctLast4",
                referenceNumber = refNumber,
                timestampMillis = timestamp,
                source = TransactionSource.SMS,
                rawText = trimmed
            )
        }

        // 2. SBI UPI Credit pattern
        // "Dear UPI User, your A/c XXXXXX1618-credited by Rs.533.00 on 29-05-26 transfer from ARUSHI D/O RAJENDRA SINGH BAGRI Ref No 614972592157 -SBI"
        val upiCreditRegex = Regex(
            """your A/c\s*(?:X+)?(\w+)[-\s]+credited by\s+(?:Rs\.?|INR)?\s*([\d,]+(?:\.\d{1,2})?)\s+on\s+([A-Za-z0-9/-]+)\s+transfer from\s+(.+?)\s+Ref(?:\s*No)?\s*(\d+)""",
            RegexOption.IGNORE_CASE
        )
        val creditMatch = upiCreditRegex.find(trimmed)
        if (creditMatch != null) {
            val acctLast4 = creditMatch.groupValues[1].takeLast(4)
            val amountStr = creditMatch.groupValues[2].replace(",", "")
            val dateStr = creditMatch.groupValues[3].trim()
            val rawSender = creditMatch.groupValues[4].trim()
            val refNumber = creditMatch.groupValues[5].trim()

            val amount = amountStr.toDoubleOrNull() ?: return null
            val timestamp = parseSbiDateWithTime(dateStr, smsTimestamp)

            return ParsedTransaction(
                amount = amount,
                direction = TransactionDirection.CREDIT,
                rawMerchant = rawSender,
                cleanMerchant = cleanSbiMerchant(rawSender),
                accountInfo = "SBI *$acctLast4",
                referenceNumber = refNumber,
                timestampMillis = timestamp,
                source = TransactionSource.SMS,
                rawText = trimmed
            )
        }

        // 3. SBI Generic Debit / Account Transfer
        val genericDebitRegex = Regex(
            """(?:A/C|Account)\s*(?:X+)?(\w+)\s+(?:debited by|is debited for)\s+(?:Rs\.?|INR)?\s*([\d,]+(?:\.\d{1,2})?)\s+(?:on|on date)\s+([A-Za-z0-9/-]+)(?:\s+by transfer to|\s+to|\s+at)\s+([A-Za-z0-9\s._-]+?)(?:\.|\s+Ref|\s+Bal|$)""",
            RegexOption.IGNORE_CASE
        )
        val genMatch = genericDebitRegex.find(trimmed)
        if (genMatch != null) {
            val acctLast4 = genMatch.groupValues[1].takeLast(4)
            val amountStr = genMatch.groupValues[2].replace(",", "")
            val dateStr = genMatch.groupValues[3].trim()
            val rawMerchant = genMatch.groupValues[4].trim()

            val amount = amountStr.toDoubleOrNull() ?: return null
            val timestamp = parseSbiDateWithTime(dateStr, smsTimestamp)

            return ParsedTransaction(
                amount = amount,
                direction = TransactionDirection.DEBIT,
                rawMerchant = rawMerchant,
                cleanMerchant = cleanSbiMerchant(rawMerchant),
                accountInfo = "SBI *$acctLast4",
                referenceNumber = extractRef(trimmed),
                timestampMillis = timestamp,
                source = TransactionSource.SMS,
                rawText = trimmed
            )
        }

        return null
    }

    /**
     * Parses SBI dates (e.g. "28Sep26" or "29-05-26" or "29/05/26")
     * and crucially preserves the actual hour/minute/second from smsTimestamp
     * so transactions never default to 12:00 AM!
     */
    fun parseSbiDateWithTime(dateStr: String, smsTimestamp: Long): Long {
        val patterns = listOf("ddMMMyy", "dd-MM-yy", "dd/MM/yy", "dd-MMM-yy", "ddMMMyyyy")
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.ENGLISH)
                val parsed = format.parse(dateStr)
                if (parsed != null) {
                    val calParsed = Calendar.getInstance().apply { time = parsed }
                    val calSms = Calendar.getInstance().apply { timeInMillis = smsTimestamp }

                    calParsed.set(Calendar.HOUR_OF_DAY, calSms.get(Calendar.HOUR_OF_DAY))
                    calParsed.set(Calendar.MINUTE, calSms.get(Calendar.MINUTE))
                    calParsed.set(Calendar.SECOND, calSms.get(Calendar.SECOND))
                    calParsed.set(Calendar.MILLISECOND, calSms.get(Calendar.MILLISECOND))

                    return calParsed.timeInMillis
                }
            } catch (_: Exception) {}
        }
        return smsTimestamp
    }

    fun cleanSbiMerchant(raw: String): String {
        var clean = raw.trim()

        // Clean transfer prefixes
        clean = clean.replace(Regex("""^(TRANSFER FROM|TRF TO|TRANSFER TO|VPA|UPI/)\s+""", RegexOption.IGNORE_CASE), "")

        // Clean Indian relation suffixes like "D/O RAJENDRA SINGH BAGRI" or "S/O..."
        if (clean.contains(" D/O ", ignoreCase = true)) {
            clean = clean.substringBefore(" D/O ")
        } else if (clean.contains(" S/O ", ignoreCase = true)) {
            clean = clean.substringBefore(" S/O ")
        } else if (clean.contains(" W/O ", ignoreCase = true)) {
            clean = clean.substringBefore(" W/O ")
        }

        return HdfcSmsParser.cleanMerchantName(clean)
    }

    private fun extractRef(text: String): String? {
        val refRegex = Regex("""(?:Ref\s*no|Ref|UPI|UTR|txn)\s*[:\s-]*(\d{9,16})""", RegexOption.IGNORE_CASE)
        return refRegex.find(text)?.groupValues?.get(1)
    }
}
