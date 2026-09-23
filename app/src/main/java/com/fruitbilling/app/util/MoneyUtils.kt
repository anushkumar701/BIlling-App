package com.fruitbilling.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Currency formatting only.
 *
 * Weight/quantity auto-detection and line-amount calculation used to live here too,
 * left over from an earlier product-grid billing flow. The current calculator-first
 * flow gets unit interpretation entirely from CalculatorEngine (explicit "g"/"kg"
 * suffixes typed by the cashier), so that logic was dead code and has been removed --
 * keeping two independent implementations of the same rule around is exactly the kind
 * of duplicated business logic the project spec calls out to avoid.
 */
object MoneyUtils {
    private val currencyFormat = (NumberFormat.getCurrencyInstance(Locale("en", "IN")) as DecimalFormat).apply {
        currency = java.util.Currency.getInstance("INR")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val wholeCurrencyFormat = (NumberFormat.getCurrencyInstance(Locale("en", "IN")) as DecimalFormat).apply {
        currency = java.util.Currency.getInstance("INR")
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }

    fun formatPrice(amount: BigDecimal?): String {
        if (amount == null) return "₹0.00"
        return currencyFormat.format(amount.setScale(2, RoundingMode.HALF_UP))
    }

    fun formatWholePrice(amount: BigDecimal?): String {
        if (amount == null) return "₹0"
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        return if (scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            wholeCurrencyFormat.format(scaled)
        } else {
            currencyFormat.format(scaled)
        }
    }

    /**
     * Generates dynamic, context-aware cash tender note suggestions based on bill total.
     * Never suggests amounts lower than the bill amount.
     * Displays actual money amounts (e.g., [₹760, ₹800, ₹1000, ₹2000]).
     */
    fun getCashTenderSuggestions(amount: BigDecimal): List<Pair<String, String>> {
        val total = amount.setScale(0, RoundingMode.CEILING).toInt()
        if (total <= 0) return listOf("₹0" to "0")

        val suggestions = mutableListOf<Pair<String, String>>()
        // Show exact money amount instead of "Exact" text (§human counter speed)
        suggestions.add("₹$total" to total.toString())

        val candidates = sortedSetOf<Int>()

        if (total % 10 != 0) {
            candidates.add(((total / 10) + 1) * 10)
        }
        if (total % 50 != 0) {
            candidates.add(((total / 50) + 1) * 50)
        }
        if (total % 100 != 0) {
            candidates.add(((total / 100) + 1) * 100)
        }
        if (total % 500 != 0) {
            candidates.add(((total / 500) + 1) * 500)
        }

        val currencyNotes = listOf(50, 100, 200, 500, 1000, 2000)
        for (note in currencyNotes) {
            if (note > total) {
                candidates.add(note)
            }
        }

        val validCandidates = candidates.filter { it > total }.take(3)
        for (cand in validCandidates) {
            suggestions.add("₹$cand" to cand.toString())
        }

        return suggestions
    }

    /**
     * Generates smart rounded final price / discount suggestions.
     * Cashiers often round down slightly to close bills quickly (e.g. ₹1304 -> ₹1300 or ₹1290).
     * Returns list of Pair(DisplayLabel, ValueToSet).
     */
    fun getFinalPriceSuggestions(amount: BigDecimal): List<Pair<String, String>> {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        if (scaled <= BigDecimal.ZERO) return emptyList()

        val suggestions = mutableListOf<Pair<String, String>>()
        val totalInt = scaled.toInt()

        // 1. Exact amount
        val exactStr = if (scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            totalInt.toString()
        } else {
            scaled.stripTrailingZeros().toPlainString()
        }
        suggestions.add("₹$exactStr (Exact)" to exactStr)

        val seenValues = mutableSetOf<String>()
        seenValues.add(exactStr)

        // Helper to add suggestion
        fun addSuggestion(targetVal: Int) {
            if (targetVal > 0 && targetVal < scaled.toDouble()) {
                val str = targetVal.toString()
                if (seenValues.add(str)) {
                    val discount = scaled.subtract(BigDecimal(targetVal))
                    val discountStr = if (discount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                        "₹${discount.toInt()}"
                    } else {
                        "₹${discount.stripTrailingZeros().toPlainString()}"
                    }
                    suggestions.add("₹$targetVal (-$discountStr)" to str)
                }
            }
        }

        // 2. Nearest 10 rounded down (e.g. 1304 -> 1300)
        val round10Down = (totalInt / 10) * 10
        addSuggestion(round10Down)

        // 3. 10 below that (e.g. 1304 -> 1290)
        if (round10Down >= 20) {
            addSuggestion(round10Down - 10)
        }

        // 4. Nearest 5 down if between 10s (e.g. 1308 -> 1305)
        val round5Down = (totalInt / 5) * 5
        addSuggestion(round5Down)

        // 5. Nearest 50 or 100 down if larger amount (e.g. 1340 -> 1300)
        if (totalInt >= 100) {
            val round100Down = (totalInt / 100) * 100
            addSuggestion(round100Down)
        }

        return suggestions.take(4)
    }
}
