package com.aryaman.spendtracker.domain

import com.aryaman.spendtracker.data.local.entity.CategoryRuleEntity

data class RuleMatchResult(
    val category: String,
    val ruleId: Long
)

object RuleEngine {

    /**
     * Evaluates a merchant/payee text against a priority-ordered list of CategoryRuleEntity.
     * Returns the matched category and ruleId, or null if no rule matched.
     */
    fun match(
        merchantText: String?,
        rawText: String? = null,
        rules: List<CategoryRuleEntity>
    ): RuleMatchResult? {
        val targets = listOfNotNull(merchantText?.trim()?.ifEmpty { null }, rawText?.trim()?.ifEmpty { null })
        if (targets.isEmpty()) return null

        // Rules are expected to be ordered by priority ASC
        for (rule in rules) {
            val keyword = rule.keyword.trim()
            if (keyword.isEmpty()) continue

            for (target in targets) {
                val matches = if (rule.isRegex) {
                    try {
                        Regex(keyword, RegexOption.IGNORE_CASE).containsMatchIn(target)
                    } catch (_: Exception) {
                        false
                    }
                } else {
                    target.contains(keyword, ignoreCase = true)
                }

                if (matches) {
                    return RuleMatchResult(
                        category = rule.category,
                        ruleId = rule.id
                    )
                }
            }
        }

        return null
    }
}
