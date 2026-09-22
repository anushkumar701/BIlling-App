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
     * E.g., for ₹760 bill -> [Exact (760), ₹800, ₹1000, ₹2000]
     */
    fun getCashTenderSuggestions(amount: BigDecimal): List<Pair<String, String>> {
        val total = amount.setScale(0, RoundingMode.CEILING).toInt()
        if (total <= 0) return listOf("Exact" to "0")

        val suggestions = mutableListOf<Pair<String, String>>()
        suggestions.add("Exact" to total.toString())

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
}
