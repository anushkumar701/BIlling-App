package com.fruitbilling.app.data.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
 * saves copies to multiple persistent device locations (Documents, Downloads, MediaStore)
 * that survive app reinstallation, and allows saving directly to Google Drive / Gmail.
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

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "FruitBilling_Backup_$timestamp.json"
                val jsonString = root.toString(2)

                // 1. Primary app-internal storage file
                val backupDir = File(context.filesDir, "cloud_backups")
                if (!backupDir.exists()) backupDir.mkdirs()
                val primaryBackupFile = File(backupDir, fileName)
                primaryBackupFile.writeText(jsonString)

                // 2. Persistent public directories (survive app uninstall & reinstall)
                val persistentDirs = listOfNotNull(
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "FruitBilling"),
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FruitBilling"),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    context.getExternalFilesDir("backups")
                )

                for (dir in persistentDirs) {
                    try {
                        if (!dir.exists()) dir.mkdirs()
                        val persistentFile = File(dir, fileName)
                        persistentFile.writeText(jsonString)
                    } catch (_: Exception) {
                        // Ignore permission restrictions on specific target dirs
                    }
                }

                // 3. Android Q+ MediaStore Downloads collection (accessible across installs)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FruitBilling")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                stream.write(jsonString.toByteArray(Charsets.UTF_8))
                            }
                        }
                    } catch (_: Exception) {}
                }

                setLastBackupTime(context, System.currentTimeMillis())
                Result.success(primaryBackupFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Scans all persistent locations on device (Documents, Downloads, internal storage,
     * external storage, MediaStore) to find any existing backup file.
     * Crucial for restoring data after an app re-installation.
     */
    fun getAllCandidateBackupFiles(context: Context): List<File> {
        val searchDirs = listOfNotNull(
            File(context.filesDir, "cloud_backups"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "FruitBilling"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FruitBilling"),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir("backups"),
            context.getExternalFilesDir(null),
            File("/sdcard/Download"),
            File("/sdcard/Documents")
        )

        val foundFiles = mutableListOf<File>()
        val seenPaths = mutableSetOf<String>()

        for (dir in searchDirs) {
            try {
                if (!dir.exists() || !dir.isDirectory) continue
                val files = dir.listFiles { file ->
                    file.isFile && (
                        file.name.startsWith("FruitBilling_Backup_", ignoreCase = true) ||
                        (file.name.contains("FruitBilling", ignoreCase = true) && file.extension.equals("json", ignoreCase = true)) ||
                        (file.name.contains("backup", ignoreCase = true) && file.extension.equals("json", ignoreCase = true))
                    )
                } ?: continue

                for (f in files) {
                    if (f.canRead() && seenPaths.add(f.canonicalPath)) {
                        foundFiles.add(f)
                    }
                }
            } catch (_: Exception) {}
        }

        return foundFiles.sortedByDescending { it.lastModified() }
    }

    fun getLatestBackupFile(context: Context): File? {
        return getAllCandidateBackupFiles(context).firstOrNull()
    }

    /**
     * Retrieves the latest backup JSON string either from file system or MediaStore Downloads.
     */
    fun getLatestBackupJsonString(context: Context): String? {
        // 1. Check all candidate files on file system
        val files = getAllCandidateBackupFiles(context)
        for (f in files) {
            try {
                val text = f.readText()
                if (isValidBackupJson(text)) {
                    return text
                }
            } catch (_: Exception) {}
        }

        // 2. Check MediaStore Downloads for Android Q+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATE_MODIFIED
                )
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("FruitBilling_Backup_%.json")
                val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val contentUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        try {
                            val text = context.contentResolver.openInputStream(contentUri)?.bufferedReader()?.readText()
                            if (text != null && isValidBackupJson(text)) {
                                return text
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        return null
    }

    private fun isValidBackupJson(jsonString: String): Boolean {
        if (jsonString.isBlank()) return false
        return try {
            val root = JSONObject(jsonString)
            root.has("products") || root.has("bills") || root.optString("appName") == "FruitBillingApp"
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Attempts to automatically restore the most recent backup found on device.
     */
    suspend fun autoRestoreLatestBackupIfAvailable(context: Context, database: AppDatabase): Result<RestoreResult>? {
        val jsonString = getLatestBackupJsonString(context) ?: return null
        return try {
            restoreFromJson(jsonString, database)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores backup directly from a content:// or file:// URI (e.g. chosen from Gmail, Google Drive, or Downloads).
     */
    suspend fun restoreFromUri(context: Context, uri: Uri, database: AppDatabase): Result<RestoreResult> =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: return@withContext Result.failure(Exception("Could not open selected backup file"))
                restoreFromJson(jsonString, database)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Saves / shares the backup JSON file to Google Drive (via Android native Drive integration),
     * Files, WhatsApp, or Gmail. Pre-addresses email to signed-in user's Gmail if available.
     */
    fun saveToGoogleDriveOrShare(context: Context, backupFile: File, userEmail: String? = null) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (!userEmail.isNullOrBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, "Fruit Billing Cloud Backup - ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Fruit Billing App Database Backup.\n\n" +
                "To restore your data:\n" +
                "1. Open this email on your phone and tap the attachment to open with Fruit Billing, OR\n" +
                "2. Download this attachment to your phone, then open Fruit Billing and sign in to auto-restore."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Send to Gmail / Google Drive / Files")
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

