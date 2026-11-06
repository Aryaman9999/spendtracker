package com.aryaman.spendtracker.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun sameVersion_returnsFalse() {
        assertFalse(UpdateChecker.isNewerVersion("v2.6.2", "2.6.2"))
        assertFalse(UpdateChecker.isNewerVersion("2.6.2", "2.6.2"))
        assertFalse(UpdateChecker.isNewerVersion("v2.6.2", "v2.6.2"))
        assertFalse(UpdateChecker.isNewerVersion(" 2.6.2 ", "2.6.2"))
        assertFalse(UpdateChecker.isNewerVersion("v2.6.2", "2.6.2-debug"))
        assertFalse(UpdateChecker.isNewerVersion("v2.6.2", "2.6.2-release"))
    }

    @Test
    fun olderRemoteVersion_returnsFalse() {
        assertFalse(UpdateChecker.isNewerVersion("v2.6.1", "2.6.2"))
        assertFalse(UpdateChecker.isNewerVersion("v2.5.9", "2.6.2"))
        assertFalse(UpdateChecker.isNewerVersion("v1.9.9", "2.6.2"))
    }

    @Test
    fun newerRemoteVersion_returnsTrue() {
        assertTrue(UpdateChecker.isNewerVersion("v2.6.3", "2.6.2"))
        assertTrue(UpdateChecker.isNewerVersion("v2.7.0", "2.6.2"))
        assertTrue(UpdateChecker.isNewerVersion("v3.0.0", "2.6.2"))
        assertTrue(UpdateChecker.isNewerVersion("v2.6.2", "2.6.1"))
    }
}
