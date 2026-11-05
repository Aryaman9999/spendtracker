package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.local.entity.SmsTemplateEntity
import com.aryaman.spendtracker.data.model.TransactionDirection
import java.util.Locale

data class DetectedSlots(
    val amount: String? = null,
    val merchant: String? = null,
    val date: String? = null,
    val time: String? = null,
    val account: String? = null,
    val ref: String? = null,
    val direction: TransactionDirection = TransactionDirection.DEBIT,
    val isCreditCard: Boolean = false
)

sealed class SynthesisResult {
    data class Success(val regexPattern: String, val preview: ParsedTransaction) : SynthesisResult()
    data class Error(val message: String) : SynthesisResult()
}

object TemplateSynthesizer {

    /**
     * Automatically scans sample SMS text to infer candidate slots.
     */
    fun autoDetectSlots(text: String): DetectedSlots {
        val trimmed = text.trim()

        // 1. Detect Direction
        val isCredit = Regex("""\b(credited|received|deposit|deposited|refund|reversal)\b""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)
        val direction = if (isCredit) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        // 2. Detect Amount (prefer currency prefixed, else decimal amount)
        val amountRegex = Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)|\b([\d,]+\.\d{2})\b""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(trimmed)
        val amount = amountMatch?.groups?.get(1)?.value ?: amountMatch?.groups?.get(2)?.value

        // 3. Detect Date
        val dateRegex = Regex("""\b(\d{1,2}[/-](?:\d{1,2}|[A-Za-z]{3})[/-]\d{2,4}|\d{1,2}[A-Za-z]{3}\d{2,4})\b""")
        val date = dateRegex.find(trimmed)?.groupValues?.get(1)

        // 4. Detect Time
        val timeRegex = Regex("""\b(\d{1,2}:\d{2}(?::\d{2})?(?:\s*[AaPp][Mm])?)\b""")
        val time = timeRegex.find(trimmed)?.groupValues?.get(1)

        // 5. Detect Reference / UTR
        val refRegex = Regex("""(?:Ref|UPI|UTR|rrn|txn)\s*(?:no\.?|id|num)?\s*[:#\s]*(\d{8,16})""", RegexOption.IGNORE_CASE)
        val ref = refRegex.find(trimmed)?.groupValues?.get(1)

        // 6. Detect Account / Card
        val accountRegex = Regex("""(?:A/C|Card|acct)\s*([*X\d\w\s]+?)(?=\s+(?:at|to|on|ref|via|debited|credited)|$)""", RegexOption.IGNORE_CASE)
        val account = accountRegex.find(trimmed)?.groupValues?.get(1)?.trim()

        // 7. Detect Merchant / Payee
        val merchantRegex = Regex("""(?:at|to|from|by)\s+([A-Za-z0-9\s.@_-]+?)(?=\s+(?:via|on|ref|using|avail|bal|avl)|$)""", RegexOption.IGNORE_CASE)
        val merchant = merchantRegex.find(trimmed)?.groupValues?.get(1)?.trim()

        // 8. Detect Credit Card
        val isCreditCard = Regex("""\b(credit card|creditcard|pixel play|card ending)\b""", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)

        return DetectedSlots(
            amount = amount,
            merchant = merchant,
            date = date,
            time = time,
            account = account,
            ref = ref,
            direction = direction,
            isCreditCard = isCreditCard
        )
    }

    private data class SlotMatch(
        val name: String,
        val snippet: String,
        val startIndex: Int,
        val endIndex: Int
    )

    /**
     * Synthesizes a generalized, resilient regex pattern from sample SMS and tagged slots.
     */
    fun synthesize(
        sampleText: String,
        amountSnippet: String,
        merchantSnippet: String,
        dateSnippet: String?,
        timeSnippet: String?,
        accountSnippet: String?,
        refSnippet: String?,
        direction: TransactionDirection,
        bankName: String,
        senderFilter: String?,
        isCreditCard: Boolean = false
    ): SynthesisResult {
        val trimmed = sampleText.trim()
        if (trimmed.isBlank()) {
            return SynthesisResult.Error("Sample SMS text cannot be blank.")
        }
        if (amountSnippet.isBlank()) {
            return SynthesisResult.Error("Amount snippet is required.")
        }
        if (merchantSnippet.isBlank()) {
            return SynthesisResult.Error("Payee / Merchant snippet is required.")
        }

        // Clean amount snippet in case user included currency symbols (e.g. "Rs. 179.49" -> "179.49")
        val cleanAmount = amountSnippet.replace(Regex("""^[^\d]*"""), "").trim()

        // Find occurrences of snippets in sampleText
        val slots = mutableListOf<SlotMatch>()

        fun addSlot(name: String, snippet: String?) {
            if (snippet.isNullOrBlank()) return
            val cleanSnippet = snippet.trim()
            val idx = trimmed.indexOf(cleanSnippet, ignoreCase = true)
            if (idx != -1) {
                slots.add(SlotMatch(name, cleanSnippet, idx, idx + cleanSnippet.length))
            }
        }

        addSlot("amount", cleanAmount)
        addSlot("merchant", merchantSnippet.trim())
        if (!dateSnippet.isNullOrBlank()) addSlot("date", dateSnippet.trim())
        if (!timeSnippet.isNullOrBlank()) addSlot("time", timeSnippet.trim())
        if (!accountSnippet.isNullOrBlank()) addSlot("account", accountSnippet.trim())
        if (!refSnippet.isNullOrBlank()) addSlot("ref", refSnippet.trim())

        // Verify required slots found
        if (slots.none { it.name == "amount" }) {
            return SynthesisResult.Error("Could not find amount '$cleanAmount' in the sample text.")
        }
        if (slots.none { it.name == "merchant" }) {
            return SynthesisResult.Error("Could not find merchant '$merchantSnippet' in the sample text.")
        }

        // Sort slots by position in text
        slots.sortBy { it.startIndex }

        // Check for overlaps
        for (i in 0 until slots.size - 1) {
            if (slots[i].endIndex > slots[i + 1].startIndex) {
                return SynthesisResult.Error("Highlighted fields '${slots[i].name}' and '${slots[i + 1].name}' overlap in the text.")
            }
        }

        // Build generalized regex pattern
        val sb = StringBuilder()
        var lastIndex = 0

        for (slot in slots) {
            val staticChunk = trimmed.substring(lastIndex, slot.startIndex)
            if (staticChunk.isNotEmpty()) {
                val escaped = escapeStaticText(staticChunk)
                sb.append(escaped)
            }

            // Append generalized capture group for the slot
            when (slot.name) {
                "amount" -> {
                    sb.append("""(?<amount>[\d,]+(?:\.\d{1,2})?)""")
                }
                "merchant" -> {
                    sb.append("""(?<merchant>.+?)""")
                }
                "date" -> {
                    sb.append("""(?<date>\d{1,2}[/-](?:\d{1,2}|[A-Za-z]{3})[/-]\d{2,4}|\d{1,2}[A-Za-z]{3}\d{2,4})""")
                }
                "time" -> {
                    sb.append("""(?<time>\d{1,2}:\d{2}(?::\d{2})?(?:\s*[AaPp][Mm])?)""")
                }
                "account" -> {
                    sb.append("""(?<account>[A-Za-z0-9\s*X._-]+?)""")
                }
                "ref" -> {
                    sb.append("""(?<ref>\d{6,18})""")
                }
            }

            lastIndex = slot.endIndex
        }

        // Trailing text: allow wildcard so minor disclaimer/URL variations match
        if (lastIndex < trimmed.length) {
            sb.append(".*")
        }

        val pattern = sb.toString()

        // Create temporary template and test against sample text
        val testTemplate = SmsTemplateEntity(
            bankName = bankName,
            senderFilter = senderFilter,
            regexPattern = pattern,
            direction = direction,
            sampleText = sampleText,
            isCreditCard = isCreditCard
        )

        val preview = DynamicTemplateParser.parseSingle(trimmed, System.currentTimeMillis(), testTemplate)
            ?: return SynthesisResult.Error("Generated pattern failed to match sample text. Pattern: $pattern")

        return SynthesisResult.Success(pattern, preview)
    }

    private fun escapeStaticText(text: String): String {
        if (text.isEmpty()) return ""
        val leadingWs = text.first().isWhitespace()
        val trailingWs = text.last().isWhitespace()
        val words = text.split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return """\s+"""

        var inner = words.joinToString(separator = """\s+""") { Regex.escape(it) }

        // If static text ends with currency symbol (Rs., INR, ₹), allow 0 or more spaces after it
        if (inner.endsWith("""Rs\.""") || inner.endsWith("""INR""") || inner.endsWith("""₹""")) {
            inner += """\s*"""
        } else if (trailingWs) {
            inner += """\s+"""
        }

        val prefix = if (leadingWs) """\s+""" else ""
        return prefix + inner
    }
}
