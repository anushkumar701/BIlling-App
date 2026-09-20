package com.fruitbilling.app.data.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.fruitbilling.app.data.db.AppDatabase
import com.fruitbilling.app.data.model.Bill
import com.fruitbilling.app.data.model.BillItem
import com.fruitbilling.app.data.model.BillStatus
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.model.Product
import com.fruitbilling.app.data.model.ProductUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cloud Backup Manager:
 * Creates portable JSON backups of all products and bills, tracks last backup timestamp,
 * and allows saving directly to Google Drive / Cloud or restoring data.
 */
object CloudBackupManager {

    private const val PREFS_NAME = "fruit_cloud_backup_prefs"
    private const val KEY_LAST_CLOUD_BACKUP = "last_cloud_backup_time"

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_CLOUD_BACKUP, 0L)
    }

    private fun setLastBackupTime(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CLOUD_BACKUP, timestamp).apply()
    }

    /**
     * Checks whether a backup has already been completed today.
     * If already backed up today, no need to check or backup again that day.
     */
    fun hasBackedUpToday(context: Context): Boolean {
        val lastTime = getLastBackupTime(context)
        if (lastTime <= 0L) return false
        val (start, end) = com.fruitbilling.app.util.DateUtils.getTodayStartAndEndMillis()
        return lastTime in start..end
    }

    /**
     * Automatically backs up database data if not already done today.
     * Once backed up today, returns null and does nothing.
     */
    suspend fun autoBackupIfDailyDue(context: Context, database: AppDatabase): Result<File>? {
        if (hasBackedUpToday(context)) {
            return null
        }
        return createBackupJson(context, database)
    }

    suspend fun createBackupJson(context: Context, database: AppDatabase): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val products = database.productDao().getAllProductsSync()
                val bills = database.billDao().getAllCompletedBillsSync()

                val root = JSONObject().apply {
                    put("version", 1)
                    put("appName", "FruitBillingApp")
                    put("createdAt", System.currentTimeMillis())

                    // Products
                    val productsArray = JSONArray()
                    for (p in products) {
                        productsArray.put(JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("price", p.price.toPlainString())
                            put("unit", p.unit.name)
                            put("active", p.active)
                            put("createdAt", p.createdAt)
                        })
                    }
                    put("products", productsArray)

                    // Bills & items
                    val billsArray = JSONArray()
                    for (b in bills) {
                        val items = database.billItemDao().getItemsForBillSync(b.id)
                        val billObj = JSONObject().apply {
                            put("billNumber", b.billNumber)
                            put("status", b.status.name)
                            put("calculatedTotal", b.calculatedTotal.toPlainString())
                            b.finalAmount?.let { put("finalAmount", it.toPlainString()) }
                            b.paymentMethod?.let { put("paymentMethod", it.name) }
                            put("createdAt", b.createdAt)
                            b.completedAt?.let { put("completedAt", it) }

                            val itemsArray = JSONArray()
                            for (it in items) {
                                itemsArray.put(JSONObject().apply {
                                    put("expression", it.expression)
                                    put("calculatedAmount", it.calculatedAmount.toPlainString())
                                    it.productId?.let { pid -> put("productId", pid) }
                                    it.productNameSnapshot?.let { pns -> put("productNameSnapshot", pns) }
                                    it.unitPriceSnapshot?.let { ups -> put("unitPriceSnapshot", ups.toPlainString()) }
                                    it.unit?.let { u -> put("unit", u.name) }
                                    it.quantityOrWeight?.let { qw -> put("quantityOrWeight", qw.toPlainString()) }
                                })
                            }
                            put("items", itemsArray)
                        }
                        billsArray.put(billObj)
                    }
                    put("bills", billsArray)
                }

                val backupDir = File(context.filesDir, "cloud_backups")
                if (!backupDir.exists()) backupDir.mkdirs()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "FruitBilling_Backup_$timestamp.json")

                FileWriter(backupFile).use { writer ->
                    writer.write(root.toString(2))
                }

                setLastBackupTime(context, System.currentTimeMillis())
                Result.success(backupFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun getLatestBackupFile(context: Context): File? {
        val backupDir = File(context.filesDir, "cloud_backups")
        if (!backupDir.exists()) return null
        return backupDir.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
            ?.maxByOrNull { it.lastModified() }
    }

    suspend fun autoRestoreLatestBackupIfAvailable(context: Context, database: AppDatabase): Result<RestoreResult>? {
        val file = getLatestBackupFile(context) ?: return null
        return try {
            val jsonString = file.readText()
            if (jsonString.isNotBlank()) {
                restoreFromJson(jsonString, database)
            } else null
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves / shares the backup JSON file to Google Drive (via Android native Drive integration),
     * Files, WhatsApp, or Gmail.
     */
    fun saveToGoogleDriveOrShare(context: Context, backupFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Fruit Billing Cloud Backup")
            putExtra(Intent.EXTRA_TEXT, "Fruit Billing App Database Backup. Keep this file safe.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Save to Google Drive / Files")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    suspend fun restoreFromJson(jsonString: String, database: AppDatabase): Result<RestoreResult> =
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)
                val productsArray = root.optJSONArray("products") ?: JSONArray()
                val billsArray = root.optJSONArray("bills") ?: JSONArray()
                var restoredProducts = 0
                var restoredBills = 0

                // 1. Restore products
                for (i in 0 until productsArray.length()) {
                    val pObj = productsArray.getJSONObject(i)
                    val name = pObj.getString("name")
                    val price = BigDecimal(pObj.getString("price"))
                    val unit = try {
                        ProductUnit.valueOf(pObj.optString("unit", "KG"))
                    } catch (_: Exception) { ProductUnit.KG }
                    val active = pObj.optBoolean("active", true)

                    val existing = database.productDao().getProductByName(name)
                    if (existing == null) {
                        database.productDao().insertProduct(
                            Product(name = name, price = price, unit = unit, active = active)
                        )
                        restoredProducts++
                    }
                }

                // 2. Restore completed bills and items
                for (i in 0 until billsArray.length()) {
                    val bObj = billsArray.getJSONObject(i)
                    val billNumber = bObj.getInt("billNumber")
                    val createdAt = bObj.getLong("createdAt")

                    // Check if exact same bill already exists
                    val existingBillId = database.billDao().getBillByNumberAndCreatedAt(billNumber, createdAt)
                    if (existingBillId == null) {
                        // Check if billNumber is already occupied by a different bill; if so, allocate next number
                        val assignedBillNumber = if (database.billDao().getBillIdByBillNumber(billNumber) != null) {
                            (database.billDao().getMaxUsedBillNumber() ?: 0) + 1
                        } else {
                            billNumber
                        }

                        val status = try {
                            BillStatus.valueOf(bObj.optString("status", "COMPLETED"))
                        } catch (_: Exception) { BillStatus.COMPLETED }
                        val calculatedTotal = BigDecimal(bObj.getString("calculatedTotal"))
                        val finalAmount = if (bObj.has("finalAmount") && !bObj.isNull("finalAmount")) {
                            BigDecimal(bObj.getString("finalAmount"))
                        } else null
                        val paymentMethod = if (bObj.has("paymentMethod") && !bObj.isNull("paymentMethod")) {
                            try {
                                PaymentMethod.valueOf(bObj.getString("paymentMethod"))
                            } catch (_: Exception) { null }
                        } else null
                        val completedAt = if (bObj.has("completedAt") && !bObj.isNull("completedAt")) {
                            bObj.getLong("completedAt")
                        } else null

                        val billToInsert = Bill(
                            billNumber = assignedBillNumber,
                            status = status,
                            calculatedTotal = calculatedTotal,
                            finalAmount = finalAmount,
                            paymentMethod = paymentMethod,
                            createdAt = createdAt,
                            completedAt = completedAt
                        )

                        val newBillId = database.billDao().insertBill(billToInsert)

                        // Insert items for this bill
                        val itemsArray = bObj.optJSONArray("items") ?: JSONArray()
                        val itemsToInsert = mutableListOf<BillItem>()
                        for (j in 0 until itemsArray.length()) {
                            val itObj = itemsArray.getJSONObject(j)
                            val expression = itObj.optString("expression", "")
                            val calculatedAmount = BigDecimal(itObj.getString("calculatedAmount"))
                            val productId = if (itObj.has("productId") && !itObj.isNull("productId")) {
                                itObj.getLong("productId")
                            } else null
                            val productNameSnapshot = itObj.optString("productNameSnapshot").takeIf { it.isNotBlank() }
                            val unitPriceSnapshot = if (itObj.has("unitPriceSnapshot") && !itObj.isNull("unitPriceSnapshot")) {
                                BigDecimal(itObj.getString("unitPriceSnapshot"))
                            } else null
                            val unit = if (itObj.has("unit") && !itObj.isNull("unit")) {
                                try {
                                    ProductUnit.valueOf(itObj.getString("unit"))
                                } catch (_: Exception) { null }
                            } else null
                            val quantityOrWeight = if (itObj.has("quantityOrWeight") && !itObj.isNull("quantityOrWeight")) {
                                BigDecimal(itObj.getString("quantityOrWeight"))
                            } else null

                            itemsToInsert.add(
                                BillItem(
                                    billId = newBillId,
                                    expression = expression,
                                    calculatedAmount = calculatedAmount,
                                    productId = productId,
                                    productNameSnapshot = productNameSnapshot,
                                    unitPriceSnapshot = unitPriceSnapshot,
                                    unit = unit,
                                    quantityOrWeight = quantityOrWeight,
                                    createdAt = createdAt
                                )
                            )
                        }

                        if (itemsToInsert.isNotEmpty()) {
                            database.billItemDao().insertAll(itemsToInsert)
                        }

                        restoredBills++
                    }
                }

                Result.success(RestoreResult(restoredProducts, restoredBills))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

data class RestoreResult(
    val productsRestored: Int,
    val billsRestored: Int
) {
    val totalCount: Int get() = productsRestored + billsRestored
}

