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

                val backupDir = File(context.cacheDir, "cloud_backups")
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

    suspend fun restoreFromJson(jsonString: String, database: AppDatabase): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)
                val productsArray = root.optJSONArray("products") ?: JSONArray()
                var restoredCount = 0

                // Restore products
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
                        restoredCount++
                    }
                }

                Result.success(restoredCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
