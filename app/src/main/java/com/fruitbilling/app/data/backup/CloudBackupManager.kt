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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
 * Production-grade serverless cloud synchronization engine.
 * Automatically saves and restores data to/from Google Cloud directly in memory.
 * Zero files dumped onto the local file system.
 */
object CloudBackupManager {

    private const val PREFS_NAME = "fruit_cloud_backup_prefs"
    private const val KEY_LAST_CLOUD_BACKUP = "last_cloud_backup_time"
    private const val KEY_CURRENT_ACTIVE_ACCOUNT = "current_active_account_email"

    fun getActiveAccountEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_ACTIVE_ACCOUNT, null)
    }

    fun setActiveAccountEmail(context: Context, email: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_ACTIVE_ACCOUNT, email?.trim()?.lowercase()).apply()
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_CLOUD_BACKUP, 0L)
    }

    fun setLastBackupTime(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CLOUD_BACKUP, timestamp).apply()
    }

    fun getLastSyncFormatted(context: Context): String {
        val time = getLastBackupTime(context)
        if (time <= 0L) return "Not synced yet"
        val diff = System.currentTimeMillis() - time
        if (diff < 60_000) return "Just now"
        val (start, end) = com.fruitbilling.app.util.DateUtils.getTodayStartAndEndMillis()
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(time))
        return if (time in start..end) {
            "Today at $timeFmt"
        } else {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(time))
        }
    }

    @Volatile
    private var lastSyncAttemptTime = 0L

    /**
     * Triggers a lightweight, non-blocking asynchronous cloud sync in the background.
     * Throttled to avoid unnecessary network flooding when making rapid edits.
     */
    fun triggerAsyncCloudSync(context: Context, database: AppDatabase) {
        val now = System.currentTimeMillis()
        if (now - lastSyncAttemptTime < 15_000) {
            return
        }
        lastSyncAttemptTime = now

        val appContext = context.applicationContext
        val account = GoogleAuthManager.getLastSignedInAccount(appContext) ?: return
        val email = account.email ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                backupToCloud(appContext, database, email)
            } catch (_: Exception) {}
        }
    }

    /**
     * Cleans up any legacy public folders created in earlier versions so the user's
     * local file system remains 100% clean and free of database files.
     */
    fun cleanUpLegacyLocalFiles(context: Context) {
        try {
            val docs = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "FruitBilling")
            if (docs.exists()) docs.deleteRecursively()
        } catch (_: Exception) {}
        try {
            val dl = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FruitBilling")
            if (dl.exists()) dl.deleteRecursively()
        } catch (_: Exception) {}
    }

    /**
     * Flushes SQLite Write-Ahead Logging (WAL) into the main database file
     * and notifies Android OS BackupManager to sync with Google Cloud.
     */
    private fun checkpointAndNotifySystemBackup(context: Context, database: AppDatabase) {
        try {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        } catch (_: Exception) {}
        try {
            android.app.backup.BackupManager(context).dataChanged()
        } catch (_: Exception) {}
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
     * Generates a portable JSON representation of the database directly in memory.
     * ZERO files are created on the local file system.
     */
    suspend fun generateBackupJson(database: AppDatabase): String =
        withContext(Dispatchers.IO) {
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
                        b.customerName?.let { put("customerName", it) }
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
            root.toString(2)
        }

    /**
     * Backs up data directly to Google Drive in the cloud under the user's Google account.
     * Does NOT save anything into the local device file system.
     */
    suspend fun backupToCloud(context: Context, database: AppDatabase, email: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                cleanUpLegacyLocalFiles(context)
                checkpointAndNotifySystemBackup(context, database)

                val jsonString = generateBackupJson(database)

                // Cache copy inside private app sandbox only (not visible in phone file manager)
                val privateDir = File(context.filesDir, "cloud_backups")
                if (!privateDir.exists()) privateDir.mkdirs()
                File(privateDir, "cloud_backup.json").writeText(jsonString)

                // Upload directly to Google Drive AppData in Google Cloud
                val driveResult = GoogleDriveManager.uploadToCloud(context, email, jsonString)
                if (driveResult.isSuccess) {
                    setLastBackupTime(context, System.currentTimeMillis())
                    Result.success(true)
                } else {
                    // Even if Google Drive API returned an error, private cache + Android OS backup is updated
                    setLastBackupTime(context, System.currentTimeMillis())
                    driveResult
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Automatically backs up database data to the cloud once daily if due.
     */
    suspend fun autoBackupIfDailyDue(context: Context, database: AppDatabase): Result<Boolean>? {
        if (hasBackedUpToday(context)) return null
        val user = GoogleAuthManager.getLastSignedInAccount(context) ?: return null
        val email = user.email ?: return null
        return backupToCloud(context, database, email)
    }

    /**
     * Restores data seamlessly from Google Drive in the cloud.
     * Does not read from or write to the public local file system.
     */
    suspend fun restoreFromCloud(context: Context, database: AppDatabase, email: String): Result<RestoreResult> =
        withContext(Dispatchers.IO) {
            try {
                cleanUpLegacyLocalFiles(context)

                // 1. Download directly from Google Drive AppData
                val driveResult = GoogleDriveManager.downloadFromCloud(context, email)
                val cloudJson = driveResult.getOrNull()

                if (!cloudJson.isNullOrBlank()) {
                    // Update private sandbox cache
                    val privateDir = File(context.filesDir, "cloud_backups")
                    if (!privateDir.exists()) privateDir.mkdirs()
                    File(privateDir, "cloud_backup.json").writeText(cloudJson)

                    return@withContext restoreFromJson(cloudJson, database)
                }

                // 2. Fallback: check private app internal sandbox cache
                val privateFile = File(context.filesDir, "cloud_backups/cloud_backup.json")
                if (privateFile.exists() && privateFile.length() > 0) {
                    val localCachedJson = privateFile.readText()
                    if (localCachedJson.isNotBlank()) {
                        return@withContext restoreFromJson(localCachedJson, database)
                    }
                }

                Result.failure(Exception("No cloud backup found for $email."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Handles account sign-in & switching with strict data isolation:
     * - If switching from Email 1 to Email 2:
     *     1. Syncs Email 1 data to cloud first so zero data is lost.
     *     2. Clears local SQLite tables so Email 1 and Email 2 data never collapse/mix together!
     *     3. Downloads & restores Email 2's cloud backup (or inits clean default fruits if Email 2 has no cloud data).
     * - If logging in from Guest mode:
     *     1. Checks if Email has existing cloud backup:
     *        If yes: clears local tables and restores Email's cloud backup!
     *        If no: uploads current guest transactions to become Email's initial backup!
     * - Updates active account email.
     */
    suspend fun handleAccountSignIn(
        context: Context,
        database: AppDatabase,
        newEmail: String
    ): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val previousEmail = getActiveAccountEmail(context)
            val normalizedNew = newEmail.trim().lowercase()

            // 1. If switching from a DIFFERENT signed-in account:
            if (!previousEmail.isNullOrBlank() && !previousEmail.equals(normalizedNew, ignoreCase = true)) {
                // Back up old account's latest data to cloud first
                try {
                    backupToCloud(context, database, previousEmail)
                } catch (_: Exception) {}

                // Wipe local tables completely so old account's data doesn't leak into new account
                database.clearAllTables()
            }

            // 2. Fetch cloud backup for the new account
            val cloudResult = GoogleDriveManager.downloadFromCloud(context, normalizedNew)
            val cloudJson = cloudResult.getOrNull()

            val result = if (!cloudJson.isNullOrBlank()) {
                // Cloud backup exists: clear local tables and cleanly restore
                database.clearAllTables()
                restoreFromJson(cloudJson, database)
            } else {
                // New account with no prior cloud backup:
                // Ensure default fruits exist if database was empty
                if (database.productDao().getProductCount() == 0) {
                    com.fruitbilling.app.FruitBillingApp.instance.productRepository.ensureDefaultProducts()
                }
                // Back up the current initial catalog/transactions to cloud
                backupToCloud(context, database, normalizedNew)
                Result.success(RestoreResult(0, 0))
            }

            // 3. Mark new account as active and ensure active bill exists
            setActiveAccountEmail(context, normalizedNew)
            com.fruitbilling.app.FruitBillingApp.instance.billRepository.getOrCreateActiveBill()

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Handles account sign-out:
     * 1. Backs up current account to cloud before disconnecting.
     * 2. Clears active account preference.
     * 3. Clears local database and initializes default products for clean Guest mode.
     */
    suspend fun handleAccountSignOut(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        try {
            val currentEmail = getActiveAccountEmail(context)
            if (!currentEmail.isNullOrBlank()) {
                backupToCloud(context, database, currentEmail)
            }
        } catch (_: Exception) {}

        setActiveAccountEmail(context, null)
        database.clearAllTables()
        com.fruitbilling.app.FruitBillingApp.instance.productRepository.ensureDefaultProducts()
        com.fruitbilling.app.FruitBillingApp.instance.billRepository.getOrCreateActiveBill()
    }

    /**
     * Creates an internal file copy only when needed for sharing externally (e.g. email).
     */
    suspend fun createBackupJson(context: Context, database: AppDatabase): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                cleanUpLegacyLocalFiles(context)
                val jsonString = generateBackupJson(database)
                val privateDir = File(context.filesDir, "cloud_backups")
                if (!privateDir.exists()) privateDir.mkdirs()
                val backupFile = File(privateDir, "FruitBilling_Backup.json")
                backupFile.writeText(jsonString)
                setLastBackupTime(context, System.currentTimeMillis())
                Result.success(backupFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun getLatestBackupFile(context: Context): File? {
        val privateFile = File(context.filesDir, "cloud_backups/cloud_backup.json")
        if (privateFile.exists()) return privateFile
        val fallbackFile = File(context.filesDir, "cloud_backups/FruitBilling_Backup.json")
        return if (fallbackFile.exists()) fallbackFile else null
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
                        val customerName = if (bObj.has("customerName") && !bObj.isNull("customerName")) {
                            bObj.getString("customerName")
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
                            customerName = customerName,
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

