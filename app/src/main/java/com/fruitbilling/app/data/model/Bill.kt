package com.fruitbilling.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "bills",
    indices = [
        Index(value = ["billNumber"]),
        Index(value = ["status"]),
        Index(value = ["completedAt"]),
        Index(value = ["status", "completedAt"]),
        Index(value = ["paymentMethod"])
    ]
)
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billNumber: Int,
    val status: BillStatus = BillStatus.ACTIVE,
    val calculatedTotal: BigDecimal = BigDecimal.ZERO,
    val finalAmount: BigDecimal? = null,
    val paymentMethod: PaymentMethod? = null,
    val customerName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val formattedBillNumber: String
        get() = String.format(java.util.Locale.US, "#%03d", billNumber)

    val effectiveChargedAmount: BigDecimal
        get() = finalAmount ?: calculatedTotal
}
