package com.fruitbilling.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fruitbilling.app.data.model.Bill
import com.fruitbilling.app.data.model.BillWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Long): Bill?

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    fun getBillWithItems(id: Long): Flow<BillWithItems?>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillWithItemsSync(id: Long): BillWithItems?

    @Transaction
    @Query("SELECT * FROM bills WHERE status IN ('ACTIVE', 'HELD') ORDER BY id DESC")
    fun getActiveAndHeldBills(): Flow<List<BillWithItems>>

    @Transaction
    @Query("SELECT * FROM bills WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun getLatestActiveBill(): Flow<BillWithItems?>

    @Transaction
    @Query("SELECT * FROM bills WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    suspend fun getLatestActiveBillSync(): BillWithItems?

    @Transaction
    @Query("SELECT * FROM bills WHERE status = 'COMPLETED' ORDER BY completedAt DESC, id DESC")
    fun getCompletedBills(): Flow<List<BillWithItems>>

    @Transaction
    @Query("""
        SELECT * FROM bills 
        WHERE status IN ('ACTIVE', 'HELD') 
        AND id NOT IN (SELECT DISTINCT billId FROM bill_items)
        LIMIT 1
    """)
    suspend fun getEmptyDraftBill(): BillWithItems?

    @Query("""
        DELETE FROM bills 
        WHERE id = :id 
        AND status != 'COMPLETED' 
        AND id NOT IN (SELECT DISTINCT billId FROM bill_items)
    """)
    suspend fun deleteEmptyDraft(id: Long): Int

    @Query("""
        SELECT MAX(billNumber) FROM bills 
        WHERE status = 'COMPLETED' 
        OR id IN (SELECT DISTINCT billId FROM bill_items)
    """)
    suspend fun getMaxUsedBillNumber(): Int?

    @Query("SELECT * FROM bills WHERE status = 'COMPLETED' AND completedAt BETWEEN :startOfDay AND :endOfDay")
    fun getTodayCompletedBills(startOfDay: Long, endOfDay: Long): Flow<List<Bill>>

    @Query("SELECT COUNT(*) FROM bills WHERE status = 'COMPLETED' AND completedAt BETWEEN :startOfDay AND :endOfDay")
    fun getTodayCompletedBillsCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM bills WHERE status IN ('ACTIVE', 'HELD')")
    fun getActiveAndHeldBillsCount(): Flow<Int>

    @Query("SELECT * FROM bills WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getAllCompletedBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE status = 'COMPLETED' AND completedAt >= :sinceTimestamp ORDER BY completedAt DESC")
    fun getCompletedBillsSince(sinceTimestamp: Long): Flow<List<Bill>>

    /** Bounded month query: only loads bills in the given time range to avoid unbounded memory usage */
    @Query("SELECT * FROM bills WHERE status = 'COMPLETED' AND completedAt BETWEEN :startMs AND :endMs ORDER BY completedAt DESC")
    fun getCompletedBillsInRange(startMs: Long, endMs: Long): Flow<List<Bill>>
}
