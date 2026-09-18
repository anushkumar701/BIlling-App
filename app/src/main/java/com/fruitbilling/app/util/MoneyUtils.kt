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
}
