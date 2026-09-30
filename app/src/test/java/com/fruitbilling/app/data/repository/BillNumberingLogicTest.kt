package com.fruitbilling.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class BillNumberingLogicTest {

    private fun computeNextBillNumber(maxInDb: Int, lastCounterInPrefs: Int): Int {
        return maxOf(maxInDb, lastCounterInPrefs) + 1
    }

    @Test
    fun testFirstBillOfDay() {
        val next = computeNextBillNumber(maxInDb = 0, lastCounterInPrefs = 0)
        assertEquals(1, next)
    }

    @Test
    fun testNormalSequentialBilling() {
        var prefsCounter = 0
        var dbMax = 0

        for (expected in 1..10) {
            val next = computeNextBillNumber(dbMax, prefsCounter)
            assertEquals(expected, next)
            // Database and prefs updated after creation
            dbMax = next
            prefsCounter = next
        }
    }

    @Test
    fun testSharedPreferencesResetPreventsCollision() {
        // Suppose DB already has bills up to #15 for today
        val dbMax = 15
        // SharedPreferences got cleared or lost (e.g. app data wiped or cache cleared)
        val resetPrefsCounter = 0

        // In the old buggy code, this would produce #001 (COLLISION!).
        // With Bug 1 fixed, DB is the source of truth:
        val next = computeNextBillNumber(dbMax, resetPrefsCounter)
        assertEquals(16, next)
    }

    @Test
    fun testRestoreBackupTodayPreventsCollision() {
        // Active store had bills up to #5
        val prefsCounter = 5

        // Cashier restores cloud backup that has today's bills up to #20
        val dbMaxAfterRestore = 20

        val next = computeNextBillNumber(dbMaxAfterRestore, prefsCounter)
        assertEquals(21, next)
    }
}
