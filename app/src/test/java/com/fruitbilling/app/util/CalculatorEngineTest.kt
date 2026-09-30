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

    @Test
    fun testEvaluate550gExpression() {
        // 200/kg × 550g = 110.00
        val result = CalculatorEngine.evaluate("200 × 550g")
        assertTrue(result.isSuccess)
        assertEquals(BigDecimal("110.00"), result.getOrNull())
    }

    @Test
    fun testUnitPreservingBackspace() {
        // Erasing digits from "200 × 500g" should preserve 'g' suffix
        val step1 = CalculatorEngine.applyBackspace("200 × 500g")
        assertEquals("200 × 50g", step1)

        val step2 = CalculatorEngine.applyBackspace(step1)
        assertEquals("200 × 5g", step2)

        // Erasing the last digit removes the weight token cleanly
        val step3 = CalculatorEngine.applyBackspace(step2)
        assertEquals("200 ×", step3)

        // Kg unit preservation
        val kgStep1 = CalculatorEngine.applyBackspace("150 × 1.5kg")
        assertEquals("150 × 1kg", kgStep1)

        val kgStep2 = CalculatorEngine.applyBackspace(kgStep1)
        assertEquals("150 ×", kgStep2)
    }

    @Test
    fun testDigitInsertionWithUnitSuffix() {
        // Cashier has "200 × 5g", types 5 and then 0 to make 550g
        val step1 = CalculatorEngine.appendDigit("200 × 5g", "5")
        assertEquals("200 × 55g", step1)

        val step2 = CalculatorEngine.appendDigit(step1, "0")
        assertEquals("200 × 550g", step2)

        // Evaluation of resulting expression
        val result = CalculatorEngine.evaluate(step2)
        assertTrue(result.isSuccess)
        assertEquals(BigDecimal("110.00"), result.getOrNull())
    }

    @Test
    fun testShortcutReplacement() {
        // Cashier selected 500g, then changes mind to 750g -> should replace, not append
        val replaced = CalculatorEngine.applyShortcut("200 × 500g", "750g")
        assertEquals("200 × 750g", replaced)

        // Shortcut from bare rate
        val fromRate = CalculatorEngine.applyShortcut("200", "500g")
        assertEquals("200 × 500g", fromRate)

        // Shortcut with tagged product price
        val tagged = CalculatorEngine.applyShortcut("", "500g", "120")
        assertEquals("120 × 500g", tagged)
    }

    @Test
    fun testAutoGramConversionThreshold() {
        // When >= 50 follows multiply without explicit unit or decimal point, auto-convert to grams
        val g50 = CalculatorEngine.evaluate("200 × 50")
        assertTrue(g50.isSuccess)
        assertEquals(BigDecimal("10.00"), g50.getOrNull())

        val g850 = CalculatorEngine.evaluate("200 × 850")
        assertTrue(g850.isSuccess)
        assertEquals(BigDecimal("170.00"), g850.getOrNull())

        val g250 = CalculatorEngine.evaluate("80 × 250")
        assertTrue(g250.isSuccess)
        assertEquals(BigDecimal("20.00"), g250.getOrNull())

        // Numbers < 50 without unit are treated as count/piece (e.g. 4 apples or 3 boxes)
        val count4 = CalculatorEngine.evaluate("200 × 4")
        assertTrue(count4.isSuccess)
        assertEquals(BigDecimal("800.00"), count4.getOrNull())

        // Numbers in addition/subtraction are never auto-converted to grams
        val add50 = CalculatorEngine.evaluate("200 + 50")
        assertTrue(add50.isSuccess)
        assertEquals(BigDecimal("250.00"), add50.getOrNull())
    }

    @Test
    fun testExplicitUnitCaseInsensitivity() {
        val upperKg = CalculatorEngine.evaluate("300 × 1.5KG")
        assertTrue(upperKg.isSuccess)
        assertEquals(BigDecimal("450.00"), upperKg.getOrNull())

        val upperG = CalculatorEngine.evaluate("200 × 500G")
        assertTrue(upperG.isSuccess)
        assertEquals(BigDecimal("100.00"), upperG.getOrNull())

        val lowerKg = CalculatorEngine.evaluate("120 × 0.25kg")
        assertTrue(lowerKg.isSuccess)
        assertEquals(BigDecimal("30.00"), lowerKg.getOrNull())
    }

    @Test
    fun testNegativeNumbersAndComplexChaining() {
        val negStart = CalculatorEngine.evaluate("-50 + 100")
        assertTrue(negStart.isSuccess)
        assertEquals(BigDecimal("50.00"), negStart.getOrNull())

        // 50 × 2 + 100 × 500g = 100 + 50 = 150.00
        val mixedChain = CalculatorEngine.evaluate("50 × 2 + 100 × 500g")
        assertTrue(mixedChain.isSuccess)
        assertEquals(BigDecimal("150.00"), mixedChain.getOrNull())

        // 1000 ÷ 2 - 200 × 0.5 = 500 - 100 = 400.00
        val divMulChain = CalculatorEngine.evaluate("1000 ÷ 2 - 200 × 0.5")
        assertTrue(divMulChain.isSuccess)
        assertEquals(BigDecimal("400.00"), divMulChain.getOrNull())
    }

    @Test
    fun testInvalidCharacterAndSyntaxErrors() {
        val invalidChar = CalculatorEngine.evaluate("200 & 4")
        assertTrue(invalidChar.isFailure)

        val doubleOp = CalculatorEngine.evaluate("200 ++ 50")
        assertTrue(doubleOp.isFailure)

        val trailingOp = CalculatorEngine.evaluate("100 +")
        assertTrue(trailingOp.isFailure)
    }
}
