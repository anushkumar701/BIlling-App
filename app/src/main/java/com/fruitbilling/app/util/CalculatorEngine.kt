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
                val finalVal = baseVal.multiply(unitInfo.multiplier)
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
}
