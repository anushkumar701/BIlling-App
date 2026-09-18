package com.fruitbilling.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * MoneyUtils now only formats currency for display. The weight/quantity-detection and
 * line-amount tests that used to live here (Papali 650g, Orange 250g, Big apple 1kg,
 * Banana piece count) tested a bare-number auto-detection path that the current
 * calculator-first billing flow doesn't use -- unit interpretation now comes from
 * explicit "g"/"kg" suffixes typed into the calculator, which CalculatorEngineTest
 * already covers with the same worked examples (300×450g, 300×1.5kg, etc.).
 */
class MoneyUtilsTest {

    @Test
    fun formatPrice_roundsAndPadsToTwoDecimals() {
        assertEquals("₹45.50", MoneyUtils.formatPrice(BigDecimal("45.5")))
        assertEquals("₹135.00", MoneyUtils.formatPrice(BigDecimal("135")))
        assertEquals("₹0.00", MoneyUtils.formatPrice(null))
    }

    @Test
    fun formatPrice_roundsHalfUp() {
        assertEquals("₹45.51", MoneyUtils.formatPrice(BigDecimal("45.505")))
    }

    @Test
    fun formatWholePrice_dropsDecimalsWhenExact() {
        assertEquals("₹300", MoneyUtils.formatWholePrice(BigDecimal("300")))
        assertEquals("₹0", MoneyUtils.formatWholePrice(null))
    }

    @Test
    fun formatWholePrice_keepsDecimalsWhenFractional() {
        assertEquals("₹45.50", MoneyUtils.formatWholePrice(BigDecimal("45.5")))
    }
}
