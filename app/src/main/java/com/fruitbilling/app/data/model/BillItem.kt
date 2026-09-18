package com.fruitbilling.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode

@Entity(
    tableName = "bill_items",
    foreignKeys = [
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["billId"]),
        Index(value = ["productId"])
    ]
)
data class BillItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long,
    val expression: String = "",
    val calculatedAmount: BigDecimal = BigDecimal.ZERO,
    val productId: Long? = null,
    val productNameSnapshot: String? = null,
    val unitPriceSnapshot: BigDecimal? = null,
    val unit: ProductUnit? = null,
    val quantityOrWeight: BigDecimal? = null,
    val normalizedWeight: BigDecimal? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Formatted expression with product name if tagged (Beginner mode)
     * e.g. "Apple 450g" or "300 × 450g"
     */
    val displayExpression: String
        get() = if (!productNameSnapshot.isNullOrBlank()) {
            "$productNameSnapshot: $expression"
        } else {
            expression
        }

    /**
     * Formats weight or quantity for display if unit is available
     */
    val formattedQuantityOrWeight: String?
        get() {
            return when (unit) {
                ProductUnit.KG -> {
                    val qty = quantityOrWeight ?: return null
                    val grams = normalizedWeight ?: qty.multiply(BigDecimal("1000"))
                    val gramsLong = grams.setScale(0, RoundingMode.HALF_UP).toLong()
                    if (gramsLong >= 1000 && gramsLong % 1000 == 0L) {
                        "${gramsLong / 1000}kg"
                    } else if (gramsLong >= 1000) {
                        val kgStr = qty.stripTrailingZeros().toPlainString()
                        "${kgStr}kg"
                    } else {
                        "${gramsLong}g"
                    }
                }
                ProductUnit.PIECE -> {
                    val qty = quantityOrWeight?.toInt() ?: return null
                    if (qty == 1) "1 pc" else "$qty pcs"
                }
                null -> null
            }
        }
}
