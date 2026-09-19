package com.fruitbilling.app.data.repository

import androidx.room.withTransaction
import com.fruitbilling.app.data.db.AppDatabase
import com.fruitbilling.app.data.db.dao.BillDao
import com.fruitbilling.app.data.db.dao.BillItemDao
import com.fruitbilling.app.data.model.Bill
import com.fruitbilling.app.data.model.BillItem
import com.fruitbilling.app.data.model.BillStatus
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit
import com.fruitbilling.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TodayStats(
    val todaySales: BigDecimal = BigDecimal.ZERO,
    val todayCompletedBillCount: Int = 0,
    val cashSales: BigDecimal = BigDecimal.ZERO,
    val upiSales: BigDecimal = BigDecimal.ZERO,
    val unspecifiedSales: BigDecimal = BigDecimal.ZERO,
    val unspecifiedBillCount: Int = 0,
    val activeAndHeldBillCount: Int = 0
)

data class DaySalesSummary(
    val dateString: String,
    val timestamp: Long,
    val totalSales: BigDecimal,
    val billCount: Int,
    val cashSales: BigDecimal,
    val upiSales: BigDecimal
)

data class MonthSalesSummary(
    val monthString: String,
    val totalSales: BigDecimal,
    val totalBills: Int,
    val cashSales: BigDecimal,
    val upiSales: BigDecimal,
    val avgBill: BigDecimal,
    val dailyBreakdown: List<DaySalesSummary>
)

class BillRepository(
    private val database: AppDatabase,
    private val billDao: BillDao,
    private val billItemDao: BillItemDao
) {

    val activeAndHeldBills: Flow<List<BillWithItems>> = billDao.getActiveAndHeldBills()
    val completedBills: Flow<List<BillWithItems>> = billDao.getCompletedBills()

    fun getBillWithItems(billId: Long): Flow<BillWithItems?> = billDao.getBillWithItems(billId)

    /**
     * Today's live sales statistics (Today's Sales, Bill count, Cash, UPI, Unspecified, Active/Held).
     * Always derived live from Room (§16).
     */
    fun getTodayStats(): Flow<TodayStats> {
        val (startOfDay, endOfDay) = DateUtils.getTodayStartAndEndMillis()
        val todayBillsFlow = billDao.getTodayCompletedBills(startOfDay, endOfDay)
        val todayCountFlow = billDao.getTodayCompletedBillsCount(startOfDay, endOfDay)
        val activeCountFlow = billDao.getActiveAndHeldBillsCount()

        return combine(todayBillsFlow, todayCountFlow, activeCountFlow) { bills, count, activeCount ->
            var cash = BigDecimal.ZERO
            var upi = BigDecimal.ZERO
            var unspecified = BigDecimal.ZERO
            var unspecifiedCount = 0
            var total = BigDecimal.ZERO

            for (bill in bills) {
                val amount = bill.effectiveChargedAmount
                total = total.add(amount)
                when (bill.paymentMethod) {
                    PaymentMethod.CASH -> cash = cash.add(amount)
                    PaymentMethod.UPI -> upi = upi.add(amount)
                    null -> {
                        unspecified = unspecified.add(amount)
                        unspecifiedCount++
                    }
                }
            }

            TodayStats(
                todaySales = total,
                todayCompletedBillCount = count,
                cashSales = cash,
                upiSales = upi,
                unspecifiedSales = unspecified,
                unspecifiedBillCount = unspecifiedCount,
                activeAndHeldBillCount = activeCount
            )
        }
    }

    /**
     * Business Summary (§16):
     * Month total block (Total Sales, Total Bills, Cash, UPI, Avg Bill)
     * followed by date-wise breakdown list, newest first, directly with no date picker required.
     */
    fun getBusinessSummary(): Flow<MonthSalesSummary> {
        // Bounded query: load only current month to avoid unbounded memory use on long-running shops
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val monthEnd = cal.timeInMillis

        return billDao.getCompletedBillsInRange(monthStart, monthEnd).map { bills ->
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

            var monthTotal = BigDecimal.ZERO
            var monthCash = BigDecimal.ZERO
            var monthUpi = BigDecimal.ZERO
            var monthBillsCount = 0

            // Group by calendar day string
            val dayGroups = mutableMapOf<String, MutableList<Bill>>()
            val dayTimestamps = mutableMapOf<String, Long>()

            for (bill in bills) {
                val completedTime = bill.completedAt ?: bill.createdAt
                val billDay = dayFormat.format(Date(completedTime))

                dayGroups.getOrPut(billDay) { mutableListOf() }.add(bill)
                if (!dayTimestamps.containsKey(billDay)) {
                    dayTimestamps[billDay] = completedTime
                }

                // All bills from the query are already in the current month
                val amount = bill.effectiveChargedAmount
                monthTotal = monthTotal.add(amount)
                monthBillsCount++
                when (bill.paymentMethod) {
                    PaymentMethod.CASH -> monthCash = monthCash.add(amount)
                    PaymentMethod.UPI -> monthUpi = monthUpi.add(amount)
                    null -> {}
                }
            }

            val avgBill = if (monthBillsCount > 0) {
                monthTotal.divide(BigDecimal(monthBillsCount), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            val dailyBreakdown = dayGroups.map { (dayStr, dayBills) ->
                var dayTotal = BigDecimal.ZERO
                var dayCash = BigDecimal.ZERO
                var dayUpi = BigDecimal.ZERO
                for (b in dayBills) {
                    val amt = b.effectiveChargedAmount
                    dayTotal = dayTotal.add(amt)
                    when (b.paymentMethod) {
                        PaymentMethod.CASH -> dayCash = dayCash.add(amt)
                        PaymentMethod.UPI -> dayUpi = dayUpi.add(amt)
                        null -> {}
                    }
                }
                DaySalesSummary(
                    dateString = dayStr,
                    timestamp = dayTimestamps[dayStr] ?: 0L,
                    totalSales = dayTotal,
                    billCount = dayBills.size,
                    cashSales = dayCash,
                    upiSales = dayUpi
                )
            }.sortedByDescending { it.timestamp }

            MonthSalesSummary(
                monthString = monthFormat.format(Date()),
                totalSales = monthTotal,
                totalBills = monthBillsCount,
                cashSales = monthCash,
                upiSales = monthUpi,
                avgBill = avgBill,
                dailyBreakdown = dailyBreakdown
            )
        }
    }

    /**
     * Initializes or loads the active bill.
     * Enforces the rule: ONLY ONE empty/unused draft bill may exist at a time (§11).
     */
    suspend fun getOrCreateActiveBill(): BillWithItems = withContext(Dispatchers.IO) {
        database.withTransaction {
            val latestActive = billDao.getLatestActiveBillSync()
            if (latestActive != null) {
                return@withTransaction latestActive
            }

            // Check if an empty draft already exists in the database.
            // Reusing it must give a genuinely clean bill (§17.9), so clear any
            // stray final price / payment method left over from a previous session.
            val emptyDraft = billDao.getEmptyDraftBill()
            if (emptyDraft != null) {
                val cleaned = emptyDraft.bill.copy(
                    status = BillStatus.ACTIVE,
                    finalAmount = null,
                    paymentMethod = null
                )
                billDao.updateBill(cleaned)
                return@withTransaction emptyDraft.copy(bill = cleaned)
            }

            // Otherwise create a new draft
            val nextNumber = getNextSequentialBillNumber()
            val newBill = Bill(
                billNumber = nextNumber,
                status = BillStatus.ACTIVE,
                calculatedTotal = BigDecimal.ZERO
            )
            val billId = billDao.insertBill(newBill)
            billDao.getBillWithItemsSync(billId) ?: BillWithItems(newBill.copy(id = billId), emptyList())
        }
    }

    /**
     * Creates a new bill or switches to existing empty draft (§11).
     * Repeated "New Bill" taps reuse the existing empty draft rather than spawning multiple empty bills.
     */
    suspend fun createNewBill(): BillWithItems = withContext(Dispatchers.IO) {
        database.withTransaction {
            // Reusing an existing empty draft must give a genuinely clean bill (§17.9),
            // so clear any stray final price / payment method left on it.
            val emptyDraft = billDao.getEmptyDraftBill()
            if (emptyDraft != null) {
                val cleaned = emptyDraft.bill.copy(
                    status = BillStatus.ACTIVE,
                    finalAmount = null,
                    paymentMethod = null
                )
                billDao.updateBill(cleaned)
                return@withTransaction emptyDraft.copy(bill = cleaned)
            }

            val nextNumber = getNextSequentialBillNumber()
            val newBill = Bill(
                billNumber = nextNumber,
                status = BillStatus.ACTIVE,
                calculatedTotal = BigDecimal.ZERO
            )
            val billId = billDao.insertBill(newBill)
            billDao.getBillWithItemsSync(billId) ?: BillWithItems(newBill.copy(id = billId), emptyList())
        }
    }

    suspend fun holdBill(billId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val bill = billDao.getBillById(billId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Bill not found."))
            billDao.updateBill(bill.copy(status = BillStatus.HELD))
            Result.success(Unit)
        }
    }

    suspend fun activateBill(billId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val bill = billDao.getBillById(billId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Bill not found."))
            billDao.updateBill(bill.copy(status = BillStatus.ACTIVE))
            Result.success(Unit)
        }
    }

    /**
     * Persists a draft final-price entry on an ACTIVE/HELD bill immediately, rather than
     * holding it only in ViewModel memory. This is what lets final price survive app
     * restart / process death, and keeps Room the single source of truth (§12, §18).
     * Pass null to clear it. Never touches status/completedAt -- this is not completion.
     */
    suspend fun updateDraftFinalAmount(billId: Long, amount: BigDecimal?): Result<Unit> =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val bill = billDao.getBillById(billId)
                    ?: return@withTransaction Result.failure(IllegalArgumentException("Bill not found."))
                billDao.updateBill(bill.copy(finalAmount = amount?.setScale(2, RoundingMode.HALF_UP)))
                Result.success(Unit)
            }
        }

    /**
     * Persists a draft payment-method selection on an ACTIVE/HELD bill immediately, for the
     * same reason as updateDraftFinalAmount above. Pass null for "not specified".
     */
    suspend fun updateDraftPaymentMethod(billId: Long, method: PaymentMethod?): Result<Unit> =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val bill = billDao.getBillById(billId)
                    ?: return@withTransaction Result.failure(IllegalArgumentException("Bill not found."))
                billDao.updateBill(bill.copy(paymentMethod = method))
                Result.success(Unit)
            }
        }

    /**
     * Updates payment method on any bill (e.g. from History screen where cashier tags
     * Cash or UPI retrospectively, or switches payment method).
     */
    suspend fun updateCompletedBillPaymentMethod(billId: Long, method: PaymentMethod?): Result<Unit> =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                if (billDao.getBillById(billId) == null) {
                    return@withTransaction Result.failure(IllegalArgumentException("Bill not found."))
                }
                billDao.updateBillPaymentMethod(billId, method)
                Result.success(Unit)
            }
        }


    /**
     * Empty-bill delete is allowed for a genuinely empty draft (§11).
     * Never reaches or deletes a completed bill.
     */
    suspend fun deleteEmptyDraft(billId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val deletedRows = billDao.deleteEmptyDraft(billId)
            Result.success(deletedRows > 0)
        }
    }

    private suspend fun getNextSequentialBillNumber(): Int {
        val maxUsed = billDao.getMaxUsedBillNumber() ?: 0
        return maxUsed + 1
    }

    /**
     * Adds an arithmetic calculation line to the bill (§3, §4, §5).
     * Supports pure arithmetic (Experienced Mode) and tagged product lines (Beginner Mode).
     */
    suspend fun addCalculation(
        billId: Long,
        expression: String,
        amount: BigDecimal,
        product: Product? = null,
        quantityOrWeight: BigDecimal? = null,
        normalizedWeight: BigDecimal? = null
    ): Result<BillItem> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val newItem = BillItem(
                billId = billId,
                expression = expression,
                calculatedAmount = amount.setScale(2, RoundingMode.HALF_UP),
                productId = product?.id,
                productNameSnapshot = product?.name,
                unitPriceSnapshot = product?.price,
                unit = product?.unit,
                quantityOrWeight = quantityOrWeight,
                normalizedWeight = normalizedWeight,
                createdAt = System.currentTimeMillis()
            )

            val itemId = billItemDao.insertItem(newItem)
            val savedItem = newItem.copy(id = itemId)

            // Recalculate bill total immediately (§5)
            recalculateBillTotal(billId)

            Result.success(savedItem)
        }
    }

    /**
     * Updates an existing calculation line on the bill (§5).
     */
    suspend fun updateCalculation(
        item: BillItem,
        newExpression: String,
        newAmount: BigDecimal,
        newQuantityOrWeight: BigDecimal? = null,
        newNormalizedWeight: BigDecimal? = null
    ): Result<BillItem> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val updated = item.copy(
                expression = newExpression,
                calculatedAmount = newAmount.setScale(2, RoundingMode.HALF_UP),
                quantityOrWeight = newQuantityOrWeight ?: item.quantityOrWeight,
                normalizedWeight = newNormalizedWeight ?: item.normalizedWeight
            )
            billItemDao.updateItem(updated)
            recalculateBillTotal(item.billId)
            Result.success(updated)
        }
    }

    suspend fun updateItem(item: BillItem): Result<Unit> = withContext(Dispatchers.IO) {
        database.withTransaction {
            billItemDao.updateItem(item)
            recalculateBillTotal(item.billId)
            Result.success(Unit)
        }
    }

    /**
     * Deletes a calculation line with immediate bill total recalculation (§5).
     */
    suspend fun removeCalculation(item: BillItem): Result<Unit> = withContext(Dispatchers.IO) {
        database.withTransaction {
            billItemDao.deleteItem(item)
            recalculateBillTotal(item.billId)
            Result.success(Unit)
        }
    }

    suspend fun restoreItem(item: BillItem): Result<Unit> = withContext(Dispatchers.IO) {
        database.withTransaction {
            billItemDao.insertItem(item)
            recalculateBillTotal(item.billId)
            Result.success(Unit)
        }
    }

    /**
     * Clears all calculation items from a bill (§5, prompt 4 CC prompt).
     */
    suspend fun clearAllItems(billId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        database.withTransaction {
            billItemDao.deleteItemsForBill(billId)
            recalculateBillTotal(billId)
            Result.success(Unit)
        }
    }

    private suspend fun recalculateBillTotal(billId: Long) {
        val items = billItemDao.getItemsForBillSync(billId)
        val newTotal = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.calculatedAmount)
        }.setScale(2, RoundingMode.HALF_UP)

        val bill = billDao.getBillById(billId)
        if (bill != null) {
            billDao.updateBill(bill.copy(calculatedTotal = newTotal))
        }
    }

    /**
     * Atomic 1-tap bill completion (§10):
     * - Completes on first tap with no confirmation popup.
     * - Protects against double-taps.
     * - Final price is optional (defaults to calculated total).
     * - Payment method is optional (Cash, UPI, or null for Not Specified).
     */
    suspend fun completeBill(
        billId: Long,
        finalAmount: BigDecimal?,
        paymentMethod: PaymentMethod?
    ): Result<Bill> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val bill = billDao.getBillById(billId)
                ?: return@withTransaction Result.failure(IllegalStateException("Bill not found."))

            val items = billItemDao.getItemsForBillSync(billId)
            if (items.isEmpty()) {
                return@withTransaction Result.failure(IllegalStateException("Bill cannot be empty."))
            }

            val chargedAmount = finalAmount ?: bill.calculatedTotal
            if (chargedAmount < BigDecimal.ZERO) {
                return@withTransaction Result.failure(IllegalArgumentException("Amount cannot be negative."))
            }

            val completedBill = bill.copy(
                status = BillStatus.COMPLETED,
                finalAmount = finalAmount?.setScale(2, RoundingMode.HALF_UP),
                paymentMethod = paymentMethod,
                completedAt = System.currentTimeMillis()
            )

            billDao.updateBill(completedBill)
            Result.success(completedBill)
        }
    }
}
