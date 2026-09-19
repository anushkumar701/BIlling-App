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

    /**
     * Appends a digit to an expression, preserving weight suffixes (e.g. 'g' or 'kg').
     * If the expression ends with "50g" and '5' is pressed, it becomes "505g".
     */
    fun appendDigit(expression: String, digit: String): String {
        val trimmed = expression.trimEnd()
        return when {
            trimmed.endsWith("kg", ignoreCase = true) -> {
                val prefix = trimmed.dropLast(2)
                "$prefix$digit" + "kg"
            }
            trimmed.endsWith("g", ignoreCase = true) -> {
                val prefix = trimmed.dropLast(1)
                "$prefix$digit" + "g"
            }
            else -> {
                var expr = expression + digit
                if (digit == "0") {
                    val lastSignificant = trimmed.lastOrNull()
                    if (lastSignificant == '×' || lastSignificant == '*' ||
                        lastSignificant == '÷' || lastSignificant == '/') {
                        expr += "."
                    }
                }
                expr
            }
        }
    }

    /**
     * Appends a decimal point to an expression, respecting weight suffixes.
     */
    fun appendDecimal(expression: String): String {
        val trimmed = expression.trimEnd()
        return when {
            trimmed.endsWith("kg", ignoreCase = true) -> {
                val prefix = trimmed.dropLast(2)
                val lastToken = prefix.split(' ', '+', '\u2212', '-', '\u00D7', '*', '\u00F7', '/').lastOrNull() ?: ""
                if (!lastToken.contains('.')) {
                    val toAdd = if (lastToken.isEmpty()) "0." else "."
                    "$prefix$toAdd" + "kg"
                } else expression
            }
            trimmed.endsWith("g", ignoreCase = true) -> {
                val prefix = trimmed.dropLast(1)
                val lastToken = prefix.split(' ', '+', '\u2212', '-', '\u00D7', '*', '\u00F7', '/').lastOrNull() ?: ""
                if (!lastToken.contains('.')) {
                    val toAdd = if (lastToken.isEmpty()) "0." else "."
                    "$prefix$toAdd" + "g"
                } else expression
            }
            else -> {
                val lastToken = expression.split(' ', '+', '\u2212', '-', '\u00D7', '*', '\u00F7', '/').lastOrNull() ?: ""
                if (!lastToken.contains('.')) {
                    val toAppend = if (lastToken.isEmpty()) "0." else "."
                    expression + toAppend
                } else expression
            }
        }
    }

    /**
     * Erases the last character from an expression.
     * When ending with 'g' or 'kg', erases the digits before the unit instead of deleting the unit itself!
     * e.g. "200 × 500g" -> "200 × 50g" -> "200 × 5g" -> "200 × "
     */
    fun applyBackspace(expression: String): String {
        if (expression.isEmpty()) return ""
        val trimmed = expression.trimEnd()
        return when {
            // Case 1: Ends with "kg" -> erase digit before "kg" without deleting unit
            trimmed.endsWith("kg", ignoreCase = true) -> {
                val prefix = trimmed.dropLast(2)
                val match = Regex("""(\d+\.?\d*)$""").find(prefix)
                if (match != null) {
                    val numStr = match.value
                    val beforeNum = prefix.substring(0, match.range.first)
                    if (numStr.length > 1) {
                        val updatedNum = numStr.dropLast(1).trimEnd('.')
                        "$beforeNum${updatedNum}kg"
                    } else {
                        // Only 1 digit left (e.g. "1kg"), removing it removes the whole token
                        beforeNum.trimEnd()
                    }
                } else {
                    trimmed.dropLast(2).trimEnd()
                }
            }
            // Case 2: Ends with "g" -> erase digit before "g" without deleting unit
            trimmed.endsWith("g", ignoreCase = true) -> {
                val prefix = trimmed.dropLast(1)
                val match = Regex("""(\d+\.?\d*)$""").find(prefix)
                if (match != null) {
                    val numStr = match.value
                    val beforeNum = prefix.substring(0, match.range.first)
                    if (numStr.length > 1) {
                        val updatedNum = numStr.dropLast(1).trimEnd('.')
                        "$beforeNum${updatedNum}g"
                    } else {
                        // Only 1 digit left (e.g. "5g"), removing it removes the whole token
                        beforeNum.trimEnd()
                    }
                } else {
                    trimmed.dropLast(1).trimEnd()
                }
            }
            // Case 3: Standard backspace
            trimmed.length > 1 && trimmed.endsWith(" ") -> trimmed.dropLast(1).trimEnd()
            else -> trimmed.dropLast(1)
        }
    }

    /**
     * Applies a weight shortcut (e.g. "500g", "1kg", "250g") to the expression.
     * If the expression already ends with a weight shortcut, it replaces it instead of appending.
     */
    fun applyShortcut(expression: String, shortcut: String, taggedProductPriceStr: String? = null): String {
        val current = expression.trim()
        return when {
            current.isEmpty() -> {
                if (taggedProductPriceStr != null) {
                    "$taggedProductPriceStr × $shortcut"
                } else {
                    shortcut
                }
            }
            // If current expression already ends with a weight shortcut (e.g. 500g, 1.5kg), replace it
            Regex("""[×*]\s*\d+\.?\d*(?:g|kg)$""", RegexOption.IGNORE_CASE).containsMatchIn(current) -> {
                val base = current.replace(Regex("""[×*]\s*\d+\.?\d*(?:g|kg)$""", RegexOption.IGNORE_CASE), "").trimEnd()
                "$base × $shortcut"
            }
            current.endsWith("×") || current.endsWith("*") -> {
                "$current $shortcut"
            }
            current.last() in listOf('+', '\u2212', '-', '\u00D7', '\u00F7', '/') -> {
                "$current $shortcut"
            }
            else -> {
                // If it's just a number (e.g. "200"), multiply by shortcut
                "$current × $shortcut"
            }
        }
    }
}

