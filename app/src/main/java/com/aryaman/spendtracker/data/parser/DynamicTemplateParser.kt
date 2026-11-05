package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.local.entity.SmsTemplateEntity
import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.model.TransactionSource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DynamicTemplateParser {

    private val regexCache = java.util.concurrent.ConcurrentHashMap<String, Regex>()

    private fun getCompiledRegex(pattern: String): Regex {
        return regexCache.computeIfAbsent(pattern) {
            Regex(it, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        }
    }

    /**
     * Attempts to parse an incoming SMS using a list of user-defined templates.
     * Evaluates active templates matching the sender filter first, then any generic templates.
     */
    fun parse(
        smsBody: String,
        sender: String?,
        smsTimestamp: Long = System.currentTimeMillis(),
        templates: List<SmsTemplateEntity>
    ): ParsedTransaction? {
        val trimmed = smsBody.trim()
        if (trimmed.isBlank() || templates.isEmpty()) return null

        val cleanSender = sender?.uppercase(Locale.ROOT) ?: ""

        // Prioritize templates with matching sender prefix, followed by general templates
        val sortedTemplates = templates.filter { it.isActive }.sortedByDescending {
            val filter = it.senderFilter?.trim()?.uppercase(Locale.ROOT)
            if (!filter.isNullOrBlank() && cleanSender.contains(filter)) 2 else if (filter.isNullOrBlank()) 1 else 0
        }

        for (template in sortedTemplates) {
            val filter = template.senderFilter?.trim()?.uppercase(Locale.ROOT)
            if (!filter.isNullOrBlank() && !cleanSender.contains(filter)) {
                // Sender filter explicitly defined and doesn't match
                continue
            }

            val parsed = parseSingle(trimmed, smsTimestamp, template)
            if (parsed != null) {
                return parsed
            }
        }

        return null
    }

    /**
     * Executes a single template against SMS text.
     */
    fun parseSingle(
        smsBody: String,
        smsTimestamp: Long = System.currentTimeMillis(),
        template: SmsTemplateEntity
    ): ParsedTransaction? {
        return try {
            val regex = getCompiledRegex(template.regexPattern)
            val match = regex.find(smsBody.trim()) ?: return null

            // 1. Amount (Required)
            val amountStr = safeGroup(match, "amount")?.replace(",", "")?.trim() ?: return null
            val amount = amountStr.toDoubleOrNull() ?: return null

            // 2. Merchant / Payee (Required)
            val rawMerchant = safeGroup(match, "merchant")?.trim()?.ifBlank { "Unknown Payee" } ?: "Unknown Payee"
            val cleanMerchant = MerchantCleaner.clean(rawMerchant)

            // 3. Direction (Debit or Credit)
            val capturedDir = safeGroup(match, "direction")?.lowercase(Locale.ROOT)
            val direction = when {
                capturedDir != null && (capturedDir.contains("credit") || capturedDir.contains("rec") || capturedDir.contains("dep")) -> TransactionDirection.CREDIT
                capturedDir != null && (capturedDir.contains("deb") || capturedDir.contains("spent") || capturedDir.contains("sent") || capturedDir.contains("paid")) -> TransactionDirection.DEBIT
                else -> template.direction
            }

            // 4. Date & Time
            val dateStr = safeGroup(match, "date")?.trim()
            val timeStr = safeGroup(match, "time")?.trim()
            val timestamp = resolveTimestamp(dateStr, timeStr, smsTimestamp)

            // 5. Account / Card Info (Optional)
            val accountInfo = safeGroup(match, "account")?.trim()?.takeIf { it.isNotBlank() }

            // 6. Reference Number / UTR (Optional)
            val referenceNumber = safeGroup(match, "ref")?.trim()?.takeIf { it.isNotBlank() }

            ParsedTransaction(
                amount = amount,
                direction = direction,
                rawMerchant = rawMerchant,
                cleanMerchant = cleanMerchant,
                accountInfo = accountInfo,
                referenceNumber = referenceNumber,
                timestampMillis = timestamp,
                source = TransactionSource.SMS,
                rawText = smsBody.trim(),
                isCreditCard = template.isCreditCard
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun safeGroup(match: MatchResult, groupName: String): String? {
        return try {
            match.groups[groupName]?.value
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Resolves the timestamp from captured date and time strings.
     * If time is missing from the SMS, fuses the parsed date with the network arrival time!
     */
    fun resolveTimestamp(dateStr: String?, timeStr: String?, smsTimestamp: Long): Long {
        if (dateStr.isNullOrBlank()) {
            return smsTimestamp
        }

        // Case 1: Both Date and Time captured
        if (!timeStr.isNullOrBlank()) {
            val combined = "$dateStr $timeStr".trim()
            val patterns = listOf(
                "dd/MM/yy HH:mm",
                "dd/MM/yyyy HH:mm",
                "dd-MM-yy HH:mm",
                "dd-MM-yyyy HH:mm",
                "dd-MMM-yy HH:mm",
                "dd-MMM-yyyy HH:mm",
                "ddMMMyy HH:mm:ss",
                "dd-MMM-yy HH:mm:ss",
                "dd/MM/yy hh:mm a",
                "dd/MM/yyyy hh:mm a",
                "dd-MMM-yy hh:mm a",
                "dd-MMM-yyyy hh:mm a"
            )

            for (p in patterns) {
                try {
                    val sdf = SimpleDateFormat(p, Locale.ENGLISH)
                    val parsed = sdf.parse(combined)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {}
            }
        }

        // Case 2: Only Date captured (e.g. "26/09/26")
        // Fuse date with SMS network arrival time!
        val datePatterns = listOf(
            "dd/MM/yy",
            "dd/MM/yyyy",
            "dd-MM-yy",
            "dd-MM-yyyy",
            "dd-MMM-yy",
            "dd-MMM-yyyy",
            "ddMMMyy",
            "ddMMMyyyy",
            "yyyy-MM-dd"
        )

        for (dp in datePatterns) {
            try {
                val sdf = SimpleDateFormat(dp, Locale.ENGLISH)
                val parsedDate = sdf.parse(dateStr)
                if (parsedDate != null) {
                    val calParsed = Calendar.getInstance().apply { time = parsedDate }
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
}
