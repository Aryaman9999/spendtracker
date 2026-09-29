package com.aryaman.spendtracker.domain

import com.aryaman.spendtracker.data.local.entity.CategoryRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RuleEngineTest {

    @Test
    fun testSubstringMatchPriority() {
        val rules = listOf(
            CategoryRuleEntity(id = 1, keyword = "zomato", category = "Food & Dining", priority = 1),
            CategoryRuleEntity(id = 2, keyword = "anugrha", category = "Groceries", priority = 2),
            CategoryRuleEntity(id = 3, keyword = "store", category = "General Store", priority = 3)
        )

        // Matches zomato
        val res1 = RuleEngine.match("Zomato", null, rules)
        assertNotNull(res1)
        assertEquals("Food & Dining", res1!!.category)
        assertEquals(1L, res1.ruleId)

        // Matches anugrha over store due to priority
        val res2 = RuleEngine.match("ANUGRHA STORE", null, rules)
        assertNotNull(res2)
        assertEquals("Groceries", res2!!.category)
        assertEquals(2L, res2.ruleId)
    }

    @Test
    fun testRegexMatch() {
        val rules = listOf(
            CategoryRuleEntity(id = 10, keyword = """(?:uber|rapido|ola)""", category = "Travel", priority = 1, isRegex = true)
        )

        val res = RuleEngine.match("Rapido Ride", null, rules)
        assertNotNull(res)
        assertEquals("Travel", res!!.category)
        assertEquals(10L, res.ruleId)
    }

    @Test
    fun testNoMatchReturnsNull() {
        val rules = listOf(
            CategoryRuleEntity(id = 1, keyword = "netflix", category = "Entertainment", priority = 1)
        )

        val res = RuleEngine.match("Unknown Merchant", null, rules)
        assertNull(res)
    }
}
