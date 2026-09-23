package com.fruitbilling.app.util

import android.content.Context
import android.content.Intent
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import java.math.BigDecimal

object ReceiptUtils {

    /**
     * Generates a clean, professional retail receipt text suitable for WhatsApp, SMS, or printing.
     */
    fun generateReceiptText(
        billWithItems: BillWithItems,
        storeName: String = "Retail Store",
        storePhone: String? = null,
        changeAmount: BigDecimal? = null
    ): String {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val dateStr = DateUtils.formatDetailedTimestamp(bill.completedAt ?: bill.createdAt)
        val payMode = bill.paymentMethod?.label ?: "Paid"

        val phoneHeader = if (!storePhone.isNullOrBlank()) "\n📞 Contact: $storePhone" else ""

        val itemsFormatted = items.mapIndexed { index, item ->
            val title = item.productNameSnapshot ?: item.displayExpression
            val priceStr = MoneyUtils.formatPrice(item.calculatedAmount)
            val subLine = if (item.productNameSnapshot != null && item.expression.isNotBlank()) {
                val exprDisplay = item.displayExpression.replace("*", "×")
                "   $exprDisplay = $priceStr"
            } else null

            if (subLine != null) {
                "${index + 1}. $title\n$subLine"
            } else {
                "${index + 1}. $title: $priceStr"
            }
        }.joinToString("\n")

        val totalCalculated = MoneyUtils.formatPrice(bill.calculatedTotal)
        val chargedAmount = MoneyUtils.formatPrice(bill.effectiveChargedAmount)

        val discountPart = if (bill.finalAmount != null && bill.finalAmount < bill.calculatedTotal) {
            val discountVal = bill.calculatedTotal.subtract(bill.finalAmount)
            "\nOriginal Total: $totalCalculated\nDiscount: -${MoneyUtils.formatPrice(discountVal)}"
        } else ""

        val changePart = if (changeAmount != null && changeAmount > BigDecimal.ZERO) {
            "\nCash Change Returned: ${MoneyUtils.formatPrice(changeAmount)}"
        } else ""

        val payEmoji = when (bill.paymentMethod) {
            PaymentMethod.CASH -> "💵 "
            PaymentMethod.UPI -> "📱 "
            else -> "💳 "
        }

        return """
            🧾 *RETAIL INVOICE*
            🏪 *$storeName*$phoneHeader
            ──────────────────────────────
            Bill No: ${bill.formattedBillNumber}
            Date: $dateStr
            ──────────────────────────────
            *ITEMS:*
            $itemsFormatted
            ──────────────────────────────$discountPart
            *GRAND TOTAL: $chargedAmount*
            Payment Mode: $payEmoji$payMode$changePart
            ──────────────────────────────
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
