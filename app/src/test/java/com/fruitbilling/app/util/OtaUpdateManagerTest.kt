package com.fruitbilling.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaUpdateManagerTest {

    @Test
    fun testVersionComparison() {
        // Newer minor/patch versions
        assertTrue(OtaUpdateManager.isNewerVersion("1.2.1", "1.2.0"))
        assertTrue(OtaUpdateManager.isNewerVersion("1.3.0", "1.2.0"))
        assertTrue(OtaUpdateManager.isNewerVersion("2.0.0", "1.2.0"))
        assertTrue(OtaUpdateManager.isNewerVersion("1.10.0", "1.9.0"))

        // Equal versions
        assertFalse(OtaUpdateManager.isNewerVersion("1.2.0", "1.2.0"))

        // Older versions
        assertFalse(OtaUpdateManager.isNewerVersion("1.1.0", "1.2.0"))
        assertFalse(OtaUpdateManager.isNewerVersion("1.1.9", "1.2.0"))
        assertFalse(OtaUpdateManager.isNewerVersion("0.9.0", "1.0.0"))

        // Blank or invalid
        assertFalse(OtaUpdateManager.isNewerVersion("", "1.2.0"))
        assertFalse(OtaUpdateManager.isNewerVersion("1.2.0", ""))
    }
}
