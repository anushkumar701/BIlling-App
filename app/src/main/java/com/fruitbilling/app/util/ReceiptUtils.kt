package com.fruitbilling.app.util

import android.content.Context
import android.content.Intent
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import java.math.BigDecimal

object ReceiptUtils {

    /**
     * Generates a clean, professional retail receipt text with aligned prices
     * and structured layout suitable for WhatsApp, SMS, or thermal printing.
     */
    fun generateReceiptText(
        billWithItems: BillWithItems,
        storeName: String = "Retail Billing POS",
        storePhone: String? = null,
        changeAmount: BigDecimal? = null
    ): String {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val dateStr = DateUtils.formatDetailedTimestamp(bill.completedAt ?: bill.createdAt)

        val payText = when (bill.paymentMethod) {
            PaymentMethod.CASH -> "💵 Cash"
            PaymentMethod.UPI -> "📱 UPI"
            PaymentMethod.PENDING -> "⏳ Pending (Pay Later)"
            null -> "⚠️ Unspecified"
        }

        val phoneHeader = if (!storePhone.isNullOrBlank()) "\n📞 Contact: $storePhone" else ""
        val customerLine = if (!bill.customerName.isNullOrBlank()) "\n👤 Customer: ${bill.customerName}" else ""

        // Target line width for dot leader alignment
        val targetWidth = 32

        val itemsFormatted = items.mapIndexed { index, item ->
            val title = item.productNameSnapshot ?: item.displayExpression
            val priceStr = MoneyUtils.formatPrice(item.calculatedAmount)

            if (item.productNameSnapshot != null && item.expression.isNotBlank()) {
                val exprDisplay = item.displayExpression.replace("*", "×")
                // Format: "   2 kg × ₹150 ... ₹300.00"
                val prefix = "   $exprDisplay "
                val dotsNeeded = (targetWidth - prefix.length - priceStr.length).coerceAtLeast(3)
                val dots = ".".repeat(dotsNeeded)
                "${index + 1}. $title\n$prefix$dots $priceStr"
            } else {
                // Expression item: e.g. "1. 500 × 700g ... ₹350.00"
                val prefix = "${index + 1}. $title "
                val dotsNeeded = (targetWidth - prefix.length - priceStr.length).coerceAtLeast(3)
                val dots = ".".repeat(dotsNeeded)
                "$prefix$dots $priceStr"
            }
        }.joinToString("\n")

        val totalCalculated = MoneyUtils.formatPrice(bill.calculatedTotal)
        val chargedAmount = MoneyUtils.formatPrice(bill.effectiveChargedAmount)

        val discountPart = if (bill.finalAmount != null && bill.finalAmount < bill.calculatedTotal) {
            val discountVal = bill.calculatedTotal.subtract(bill.finalAmount)
            "\nItems Total:  $totalCalculated\nDiscount:     -${MoneyUtils.formatPrice(discountVal)}"
        } else ""

        val changePart = if (changeAmount != null && changeAmount > BigDecimal.ZERO) {
            "\nChange Due:   ${MoneyUtils.formatPrice(changeAmount)}"
        } else ""

        return """
🧾 *RETAIL INVOICE*
🏪 *$storeName*$phoneHeader
━━━━━━━━━━━━━━━━━━━━━━━━━━
Bill No:  ${bill.formattedBillNumber}
Date:     $dateStr$customerLine
━━━━━━━━━━━━━━━━━━━━━━━━━━
*ITEMS:*
$itemsFormatted
━━━━━━━━━━━━━━━━━━━━━━━━━━$discountPart
*GRAND TOTAL: $chargedAmount*
Payment: $payText$changePart
━━━━━━━━━━━━━━━━━━━━━━━━━━
Thank you! Please visit again! 🙏
        """.trimIndent()
    }

    /**
     * Launches Android's system share sheet for WhatsApp, Messages, or direct sharing.
     */
    fun shareReceipt(context: Context, receiptText: String, title: String = "Share Receipt") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, receiptText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }
}
