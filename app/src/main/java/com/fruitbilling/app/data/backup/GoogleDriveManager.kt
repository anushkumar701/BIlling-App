package com.fruitbilling.app.data.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cloud Sync Manager:
 * Provides seamless, 100% serverless cloud backup and restore powered by Google Cloud Firestore.
 * Zero files on the local file system.
 * Zero OAuth configuration hurdles on user devices.
 * Automatically saves and restores when the user signs in with their Google/Gmail account.
 */
object GoogleDriveManager {

    private const val FIRESTORE_PROJECT_ID = "dip-sense"
    private const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1/projects/$FIRESTORE_PROJECT_ID/databases/(default)/documents/fruit_backups"

    // Kept for backward-compatibility if referenced elsewhere
    var pendingAuthIntent: android.content.Intent? = null

    private fun getDocumentIdForEmail(email: String): String {
        val clean = email.trim().lowercase()
            .replace(".", "_")
            .replace("@", "_")
            .replace("-", "_")
            .filter { it.isLetterOrDigit() || it == '_' }
        return clean.ifBlank { "default_user" }
    }

    /**
     * Uploads the backup JSON directly to Google Cloud Firestore under the user's account.
     * ZERO files are saved to the device's local file system.
     */
    suspend fun uploadToCloud(context: Context, email: String, jsonString: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val docId = getDocumentIdForEmail(email)
                val url = URL("$FIRESTORE_BASE_URL/$docId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 20000

                val payload = JSONObject().apply {
                    val fields = JSONObject().apply {
                        put("backupJson", JSONObject().put("stringValue", jsonString))
                        put("email", JSONObject().put("stringValue", email.trim().lowercase()))
                        put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                    }
                    put("fields", fields)
                }

                conn.outputStream.use { os ->
                    val writer = os.bufferedWriter(Charsets.UTF_8)
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    Result.success(true)
                } else {
                    val errorMsg = try {
                        conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                    } catch (_: Exception) { "HTTP $code" }
                    Result.failure(Exception("Cloud sync failed (HTTP $code): $errorMsg"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Could not connect to cloud server. Please check your internet connection: ${e.message}"))
            }
        }

    /**
     * Downloads the backup JSON directly from Google Cloud Firestore for the user's account.
     * Directly loads into memory without saving to the local device file system.
     */
    suspend fun downloadFromCloud(context: Context, email: String): Result<String?> =
        withContext(Dispatchers.IO) {
            try {
                val docId = getDocumentIdForEmail(email)
                val url = URL("$FIRESTORE_BASE_URL/$docId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 20000

                val code = conn.responseCode
                if (code == 404) {
                    // No backup found yet for this account
                    Result.success(null)
                } else if (code in 200..299) {
                    val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val root = JSONObject(response)
                    val fields = root.optJSONObject("fields")
                    val backupJsonObj = fields?.optJSONObject("backupJson")
                    val jsonContent = backupJsonObj?.optString("stringValue")

                    if (!jsonContent.isNullOrBlank()) {
                        Result.success(jsonContent)
                    } else {
                        Result.success(null)
                    }
                } else {
                    val errorMsg = try {
                        conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                    } catch (_: Exception) { "HTTP $code" }
                    Result.failure(Exception("Cloud fetch failed (HTTP $code): $errorMsg"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Could not connect to cloud server. Please check your internet connection: ${e.message}"))
            }
        }
}
