package com.aryaman.spendtracker.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CategoryUtilsTest {

    @Test
    fun testNewCategoryColors() {
        assertEquals(CategoryRent, getCategoryColor("Rent"))
        assertEquals(CategorySubscriptions, getCategoryColor("Subscriptions"))
        assertEquals(CategorySubscriptions, getCategoryColor("subscription"))
        assertEquals(CategoryEmi, getCategoryColor("EMI & Loans"))
        assertEquals(CategoryEmi, getCategoryColor("emi"))
        assertEquals(CategoryInvestments, getCategoryColor("Investments"))
        assertEquals(CategoryEducation, getCategoryColor("Education"))
        assertEquals(CategoryFuel, getCategoryColor("Fuel"))
        assertEquals(CategoryFuel, getCategoryColor("petrol"))
        assertEquals(CategoryInsurance, getCategoryColor("Insurance"))
        assertEquals(CategoryGifts, getCategoryColor("Gifts & Donations"))
        assertEquals(CategoryGifts, getCategoryColor("gifts"))
        assertEquals(CategoryOther, getCategoryColor("Unknown Category"))
    }

    @Test
    fun testNewCategoryInitials() {
        assertEquals("R", getCategoryInitial("Rent"))
        assertEquals("S", getCategoryInitial("Subscriptions"))
        assertEquals("L", getCategoryInitial("EMI & Loans"))
        assertEquals("I", getCategoryInitial("Investments"))
        assertEquals("E", getCategoryInitial("Education"))
        assertEquals("F", getCategoryInitial("Fuel"))
        assertEquals("I", getCategoryInitial("Insurance"))
        assertEquals("G", getCategoryInitial("Gifts & Donations"))
        assertEquals("?", getCategoryInitial("Unknown"))
    }

    @Test
    fun testNewCategoryIcons() {
        assertNotNull(getCategoryIcon("Rent"))
        assertNotNull(getCategoryIcon("Subscriptions"))
        assertNotNull(getCategoryIcon("EMI & Loans"))
        assertNotNull(getCategoryIcon("Investments"))
        assertNotNull(getCategoryIcon("Education"))
        assertNotNull(getCategoryIcon("Fuel"))
        assertNotNull(getCategoryIcon("Insurance"))
        assertNotNull(getCategoryIcon("Gifts & Donations"))
        assertNotNull(getCategoryIcon("Unknown"))
    }
}
