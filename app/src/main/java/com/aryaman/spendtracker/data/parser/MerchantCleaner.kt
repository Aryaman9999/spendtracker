package com.aryaman.spendtracker.data.parser

import java.util.Locale

object MerchantCleaner {

    /**
     * Cleans messy VPA handles, relation suffixes, and all-caps merchant names into friendly titles.
     * E.g. "zomato.eternaltsp.payu@hdfcbank" -> "Zomato"
     * "ANUGRHA STORE" -> "Anugrha Store"
     * "ARUSHI D/O RAJENDRA SINGH BAGRI" -> "Arushi"
     */
    fun clean(raw: String): String {
        var clean = raw.trim()

        // 1. Clean Indian relation suffixes (e.g. "ARUSHI D/O RAJENDRA SINGH")
        if (clean.contains(" D/O ", ignoreCase = true)) {
            clean = clean.substringBefore(" D/O ")
        } else if (clean.contains(" S/O ", ignoreCase = true)) {
            clean = clean.substringBefore(" S/O ")
        } else if (clean.contains(" W/O ", ignoreCase = true)) {
            clean = clean.substringBefore(" W/O ")
        }

        // 2. Handle UPI VPAs: e.g., zomato.eternaltsp.payu@hdfcbank or swiggy@icici
        if (clean.contains("@")) {
            val prefix = clean.substringBefore("@")
            // Take the root brand name before dot or dash: "zomato.eternaltsp.payu" -> "zomato"
            clean = prefix.split(".", "-", "_").firstOrNull { it.isNotBlank() } ?: prefix
        }

        // 3. Strip common prefixes
        clean = clean.replace(Regex("""^(VPA|UPI/|PAYMENT TO|TRANSFER TO|TRF TO|TRANSFER FROM)\s+""", RegexOption.IGNORE_CASE), "")

        // 4. Strip common legal entity suffixes
        clean = clean.replace(Regex("""\s+(PVT\s+LTD|LTD|LLP|INC)\.?$""", RegexOption.IGNORE_CASE), "")

        // 5. Clean trailing IDs or numbers if length > 4 (e.g. flexiridesoluti961282 -> flexiridesoluti)
        clean = clean.replace(Regex("""([a-zA-Z]{3,})\d{4,}$"""), "$1")

        // 6. Title case for clean, friendly display only if all caps or all lowercase (preserves CamelCase like HungerBox)
        val isAllUpper = clean == clean.uppercase(Locale.ROOT)
        val isAllLower = clean == clean.lowercase(Locale.ROOT)
        if (clean.length > 1 && (isAllUpper || isAllLower)) {
            clean = clean.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }

        return clean.ifBlank { raw.trim() }
    }
}
