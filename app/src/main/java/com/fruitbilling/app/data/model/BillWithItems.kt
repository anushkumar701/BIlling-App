package com.fruitbilling.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class BillWithItems(
    @Embedded
    val bill: Bill,
    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val items: List<BillItem> = emptyList()
) {
    val totalItemCount: Int
        get() = items.size
}
