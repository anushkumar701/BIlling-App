package com.fruitbilling.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fruitbilling.app.data.model.BillItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BillItemDao {
    @Query("SELECT * FROM bill_items WHERE billId = :billId ORDER BY id ASC")
    fun getItemsForBill(billId: Long): Flow<List<BillItem>>

    @Query("SELECT * FROM bill_items WHERE billId = :billId ORDER BY id ASC")
    suspend fun getItemsForBillSync(billId: Long): List<BillItem>

    @Query("SELECT * FROM bill_items WHERE billId = :billId AND productId = :productId LIMIT 1")
    suspend fun getItemByBillAndProduct(billId: Long, productId: Long): BillItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BillItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BillItem>)

    @Update
    suspend fun updateItem(item: BillItem)

    @Delete
    suspend fun deleteItem(item: BillItem)

    @Query("DELETE FROM bill_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteItemsForBill(billId: Long)
}
