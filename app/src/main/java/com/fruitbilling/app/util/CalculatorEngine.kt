package com.fruitbilling.app.util

import java.math.BigDecimal
import java.math.RoundingMode

object CalculatorEngine {

    /**
     * Evaluates a mathematical expression that may contain standard operators (+, -, *, /)
     * and weight units (g, kg).
     *
     * Examples:
     * - "30 × 40" -> 1200
     * - "300 × 0.4" -> 120
     * - "300 × 450g" -> 135 (450g = 0.450kg)
     * - "300 × 1.5kg" -> 450
     * - "100 + 50 - 20" -> 130
     * - "1000 ÷ 4" -> 250
     */
    fun evaluate(expression: String): Result<BigDecimal> {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Expression is empty"))
        }

        // Normalize operators
        val normalized = trimmed
            .replace('×', '*')
            .replace('x', '*')
            .replace('X', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace(" ", "")

        try {
            val tokens = tokenize(normalized)
            if (tokens.isEmpty()) {
                return Result.failure(IllegalArgumentException("Invalid expression"))
            }

            // Check if last token is an operator
            val lastToken = tokens.last()
            if (lastToken is Token.Operator) {
                return Result.failure(IllegalArgumentException("Incomplete expression"))
            }

            val result = parseAndEvaluate(tokens)
            return Result.success(result.setScale(2, RoundingMode.HALF_UP))
        } catch (e: ArithmeticException) {
            return Result.failure(e)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Invalid expression: ${e.message}"))
        }
    }

    private sealed class Token {
        data class Number(val value: BigDecimal) : Token()
        data class Operator(val op: Char) : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = expr.length

        while (i < len) {
            val c = expr[i]

            if (c in listOf('+', '-', '*', '/')) {
                // Handle negative numbers at the beginning or after an operator
                if (c == '-' && (tokens.isEmpty() || tokens.last() is Token.Operator)) {
                    // Start of negative number
                    val numStart = i
                    i++
                    while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                        i++
                    }
                    val numStr = expr.substring(numStart, i)
                    val unitInfo = parseUnit(expr, i)
                    i = unitInfo.newIndex
                    val finalVal = BigDecimal(numStr).multiply(unitInfo.multiplier)
                    tokens.add(Token.Number(finalVal))
                    continue
                } else {
                    tokens.add(Token.Operator(c))
                    i++
                    continue
                }
            }

            if (c.isDigit() || c == '.') {
                val numStart = i
                while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                    i++
                }
                val numStr = expr.substring(numStart, i)
                val unitInfo = parseUnit(expr, i)
                i = unitInfo.newIndex
                val baseVal = BigDecimal(numStr)

                // Auto-gram logic (§fruit-shop workflow):
                // If the number follows '*' (multiply) without an explicit unit (multiplier == 1),
                // without a decimal point, and is >= 50 (e.g. 200 * 850, 80 * 250),
                // it is automatically treated as grams (e.g. 850g = 0.850kg).
                val isAfterMultiply = tokens.lastOrNull() is Token.Operator && (tokens.last() as Token.Operator).op == '*'
                val isIntegerGramCandidate = !numStr.contains('.') &&
                        unitInfo.multiplier.compareTo(BigDecimal.ONE) == 0 &&
                        baseVal >= BigDecimal("50")

                val finalMultiplier = if (isAfterMultiply && isIntegerGramCandidate) {
                    BigDecimal("0.001")
                } else {
                    unitInfo.multiplier
                }

                val finalVal = baseVal.multiply(finalMultiplier)
                tokens.add(Token.Number(finalVal))
                continue
            }

            // Unknown character
            throw IllegalArgumentException("Unexpected character '$c' at position $i")
        }

        return tokens
    }

    private data class UnitParseResult(val multiplier: BigDecimal, val newIndex: Int)

    private fun parseUnit(expr: String, startIndex: Int): UnitParseResult {
        var idx = startIndex
        val len = expr.length

        if (idx < len) {
            // Check for "kg" or "KG"
            if (idx + 1 < len && expr.substring(idx, idx + 2).equals("kg", ignoreCase = true)) {
                return UnitParseResult(BigDecimal.ONE, idx + 2)
            }
            // Check for "g" or "G"
            if (expr[idx].equals('g', ignoreCase = true)) {
                // 450g = 0.450kg -> multiplier = 0.001
                return UnitParseResult(BigDecimal("0.001"), idx + 1)
            }
        }
        return UnitParseResult(BigDecimal.ONE, startIndex)
    }

    /**
     * Standard 2-pass expression evaluation respecting operator precedence:
     * 1. Multiplications (*) and Divisions (/)
     * 2. Additions (+) and Subtractions (-)
     */
    private fun parseAndEvaluate(tokens: List<Token>): BigDecimal {
        if (tokens.isEmpty()) return BigDecimal.ZERO

        // First pass: *, /
        val intermediateTokens = mutableListOf<Token>()
        var idx = 0

        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token is Token.Operator && (token.op == '*' || token.op == '/')) {
                val prevNumber = (intermediateTokens.removeAt(intermediateTokens.size - 1) as Token.Number).value
                val nextToken = tokens.getOrNull(idx + 1)
                    ?: throw IllegalArgumentException("Missing operand after '${token.op}'")
                if (nextToken !is Token.Number) {
                    throw IllegalArgumentException("Expected number after '${token.op}'")
                }
                val nextNumber = nextToken.value

                val evaluated = if (token.op == '*') {
                    prevNumber.multiply(nextNumber)
                } else {
                    if (nextNumber.compareTo(BigDecimal.ZERO) == 0) {
                        throw ArithmeticException("Division by zero")
                    }
                    prevNumber.divide(nextNumber, 6, RoundingMode.HALF_UP)
                }
                intermediateTokens.add(Token.Number(evaluated))
                idx += 2
            } else {
                intermediateTokens.add(token)
                idx++
            }
        }

        // Second pass: +, -
        if (intermediateTokens.isEmpty()) return BigDecimal.ZERO
        var result = (intermediateTokens[0] as? Token.Number)?.value
            ?: throw IllegalArgumentException("Invalid expression start")

        var opIdx = 1
        while (opIdx < intermediateTokens.size) {
            val opToken = intermediateTokens[opIdx] as Token.Operator
            val numToken = intermediateTokens[opIdx + 1] as Token.Number
            result = if (opToken.op == '+') {
                result.add(numToken.value)
            } else {
                result.subtract(numToken.value)
            }
            opIdx += 2
        }

        return result
    }

    /**
     * Formats the result nicely (e.g. 1200 or 135 or 12.50)
     */
    fun formatDisplayAmount(amount: BigDecimal): String {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        return if (scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            scaled.setScale(0).toPlainString()
        } else {
            scaled.stripTrailingZeros().toPlainString()
        }
    }

    /**
     * Formats weight naturally for local fruit-shop usage:
     * - Under 1000g: "100g", "250g", "500g", "750g", "850g" (NEVER decimals like 0.75 or 0.5)
     * - Exactly 1000g or whole kgs: "1kg", "2kg"
     * - Fractional kgs: "1.5kg", "2.25kg"
     */
    fun formatWeight(kgAmount: BigDecimal): String {
        val grams = kgAmount.multiply(BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP)
        return if (grams.compareTo(BigDecimal.ZERO) <= 0) {
            "${kgAmount.stripTrailingZeros().toPlainString()}kg"
        } else if (grams < BigDecimal("1000")) {
            "${grams.toPlainString()}g"
        } else if (grams.remainder(BigDecimal("1000")).compareTo(BigDecimal.ZERO) == 0) {
            "${grams.divide(BigDecimal("1000"), 0, RoundingMode.HALF_UP).toPlainString()}kg"
        } else {
            "${kgAmount.stripTrailingZeros().toPlainString()}kg"
        }
    }

    /**
     * Cleans up an expression for display so weight fractions or auto-grams show with natural units:
     * e.g. "200 × 0.75" -> "200 × 750g"
     *      "200 × 0.5"  -> "200 × 500g"
     *      "200 × 850"  -> "200 × 850g"
     *      "200 × 1.5"  -> "200 × 1.5kg"
     */
    fun prettyExpression(expr: String): String {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return ""

        val multiplyRegex = Regex("""([×*])\s*(\d*\.?\d+)(?![\w\d])""")
        return multiplyRegex.replace(trimmed) { match ->
            val op = match.groupValues[1]
            val numStr = match.groupValues[2]
            val num = numStr.toBigDecimalOrNull()
            if (num != null) {
                if (numStr.startsWith("0.") || numStr.startsWith(".")) {
                    "$op ${formatWeight(num)}"
                } else if (!numStr.contains('.') && num >= BigDecimal("50")) {
                    "$op ${num.setScale(0, RoundingMode.HALF_UP).toPlainString()}g"
                } else if (numStr.contains('.') && num >= BigDecimal.ONE) {
                    "$op ${num.stripTrailingZeros().toPlainString()}kg"
                } else {
                    match.value
                }
            } else {
                match.value
            }
        }
    }
}

