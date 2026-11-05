package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.model.TransactionDirection
import com.aryaman.spendtracker.data.model.TransactionSource

object UpiNotificationParser {

    const val PACKAGE_GPAY = "com.google.android.apps.nbu.paisa.user"
    const val PACKAGE_PHONEPE = "com.phonepe.app"
    const val PACKAGE_PAYTM = "net.one97.paytm"
    const val PACKAGE_CRED = "com.dreamplug.androidapp"

    val SUPPORTED_PACKAGES = setOf(
        PACKAGE_GPAY,
        PACKAGE_PHONEPE,
        PACKAGE_PAYTM,
        PACKAGE_CRED
    )

    fun isSupportedPackage(packageName: String): Boolean = SUPPORTED_PACKAGES.contains(packageName)

    /**
     * Parses notification title and text into a ParsedTransaction.
     */
    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        postTime: Long = System.currentTimeMillis()
    ): ParsedTransaction? {
        val combined = "${title ?: ""} ${text ?: ""}".trim()
        if (combined.isBlank()) return null

        // Pattern 1: "Paid Rs. 150 to Swiggy" or "You paid ₹250.00 to Chai Point"
        val paidRegex = Regex(
            """(?:Paid|Sent|transferred)\s+(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+(?:to|at)\s+([A-Za-z0-9\s._&-]+?)(?:\.|\s+using|\s+from|\s+Ref|$)""",
            RegexOption.IGNORE_CASE
        )
        val paidMatch = paidRegex.find(combined)
        if (paidMatch != null) {
            val amountStr = paidMatch.groupValues[1].replace(",", "")
            val rawMerchant = paidMatch.groupValues[2].trim()
            val amount = amountStr.toDoubleOrNull() ?: return null

            return ParsedTransaction(
                amount = amount,
                direction = TransactionDirection.DEBIT,
                rawMerchant = rawMerchant,
                cleanMerchant = MerchantCleaner.clean(rawMerchant),
                referenceNumber = extractRef(combined),
                timestampMillis = postTime,
                source = TransactionSource.NOTIFICATION,
                rawText = combined
            )
        }

        // Pattern 2: "Received ₹500 from John"
        val receivedRegex = Regex(
            """(?:Received|Got)\s+(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+from\s+([A-Za-z0-9\s._&-]+?)(?:\.|\s+in|\s+Ref|$)""",
            RegexOption.IGNORE_CASE
        )
        val recMatch = receivedRegex.find(combined)
        if (recMatch != null) {
            val amountStr = recMatch.groupValues[1].replace(",", "")
            val rawMerchant = recMatch.groupValues[2].trim()
            val amount = amountStr.toDoubleOrNull() ?: return null

            return ParsedTransaction(
                amount = amount,
                direction = TransactionDirection.CREDIT,
                rawMerchant = rawMerchant,
                cleanMerchant = MerchantCleaner.clean(rawMerchant),
                referenceNumber = extractRef(combined),
                timestampMillis = postTime,
                source = TransactionSource.NOTIFICATION,
                rawText = combined
            )
        }

        return null
    }

    private fun extractRef(text: String): String? {
        val refRegex = Regex("""(?:Ref|UPI|UTR|txn)\s*(?:no\.?|id)?\s*[:\s-]*(\d{9,16})""", RegexOption.IGNORE_CASE)
        return refRegex.find(text)?.groupValues?.get(1)
    }
}
