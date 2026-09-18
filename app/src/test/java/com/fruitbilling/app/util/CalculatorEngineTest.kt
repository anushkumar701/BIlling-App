package com.fruitbilling.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorEngineTest {

    @Test
    fun testPlainMultiplication() {
        val result = CalculatorEngine.evaluate("30 × 40")
        assertTrue(result.isSuccess)
        assertEquals(BigDecimal("1200.00"), result.getOrNull())
    }

    @Test
    fun testDecimalKgMultiplication() {
        val result1 = CalculatorEngine.evaluate("300 × 0.4")
        assertTrue(result1.isSuccess)
        assertEquals(BigDecimal("120.00"), result1.getOrNull())

        val result2 = CalculatorEngine.evaluate("300 × 0.450")
        assertTrue(result2.isSuccess)
        assertEquals(BigDecimal("135.00"), result2.getOrNull())

        val result3 = CalculatorEngine.evaluate("300 × 1.5kg")
        assertTrue(result3.isSuccess)
        assertEquals(BigDecimal("450.00"), result3.getOrNull())
    }

    @Test
    fun testGramsMultiplication() {
        // 300 × 450g = 135
        val result = CalculatorEngine.evaluate("300 × 450g")
        assertTrue(result.isSuccess)
        assertEquals(BigDecimal("135.00"), result.getOrNull())

        // 200 × 250g = 50
        val result2 = CalculatorEngine.evaluate("200 × 250g")
        assertTrue(result2.isSuccess)
        assertEquals(BigDecimal("50.00"), result2.getOrNull())
    }

    @Test
    fun testBasicArithmetic() {
        val add = CalculatorEngine.evaluate("100 + 50")
        assertEquals(BigDecimal("150.00"), add.getOrNull())

        val sub = CalculatorEngine.evaluate("100 - 35")
        assertEquals(BigDecimal("65.00"), sub.getOrNull())

        val div = CalculatorEngine.evaluate("1000 ÷ 4")
        assertEquals(BigDecimal("250.00"), div.getOrNull())

        val chained = CalculatorEngine.evaluate("100 + 50 - 25")
        assertEquals(BigDecimal("125.00"), chained.getOrNull())
    }

    @Test
    fun testPrecedence() {
        val result = CalculatorEngine.evaluate("10 + 20 × 3")
        assertEquals(BigDecimal("70.00"), result.getOrNull())
    }

    @Test
    fun testDivisionByZero() {
        val result = CalculatorEngine.evaluate("100 ÷ 0")
        assertTrue(result.isFailure)
    }

    @Test
    fun testInvalidExpression() {
        val empty = CalculatorEngine.evaluate("")
        assertTrue(empty.isFailure)

        val incomplete = CalculatorEngine.evaluate("300 ×")
        assertTrue(incomplete.isFailure)
    }
}
