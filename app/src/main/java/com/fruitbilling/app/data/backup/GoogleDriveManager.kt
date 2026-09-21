package com.fruitbilling.app.data.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Manages cloud backup and restore directly with Google Drive AppData Folder.
 * Files in appDataFolder are completely hidden from the user's regular Google Drive files
 * and NEVER touch the device's public file system (no files in Documents or Downloads).
 */
object GoogleDriveManager {

    private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
    private const val BACKUP_FILE_NAME = "fruit_billing_cloud_backup.json"

    var pendingAuthIntent: Intent? = null

    suspend fun getAccessToken(context: Context, email: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val account = Account(email, "com.google")
                GoogleAuthUtil.getToken(context, account, DRIVE_SCOPE)
            } catch (e: UserRecoverableAuthException) {
                pendingAuthIntent = e.intent
                null
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Uploads the backup JSON directly to Google Drive appDataFolder in Google Cloud.
     * ZERO files are saved to the device's local file system.
     */
    suspend fun uploadToCloud(context: Context, email: String, jsonString: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken(context, email)
                    ?: return@withContext Result.failure(
                        Exception("Could not obtain Google Drive authorization for $email. Please check your internet connection.")
                    )

                val existingFileId = findBackupFileId(token)

                if (existingFileId != null) {
                    // Update existing backup in appDataFolder
                    val updateUrl = URL("https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media")
                    val conn = updateUrl.openConnection() as HttpURLConnection
                    conn.requestMethod = "PATCH"
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    conn.doOutput = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 20000

                    conn.outputStream.use { os ->
                        os.write(jsonString.toByteArray(Charsets.UTF_8))
                    }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        Result.success(true)
                    } else {
                        Result.failure(Exception("Google Drive update returned HTTP $code"))
                    }
                } else {
                    // Create new backup file in appDataFolder via multipart upload
                    val createUrl = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    val boundary = "FruitBillingBoundary" + System.currentTimeMillis()
                    val conn = createUrl.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                    conn.doOutput = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 20000

                    val metadata = JSONObject().apply {
                        put("name", BACKUP_FILE_NAME)
                        put("parents", JSONArray().put("appDataFolder"))
                    }.toString()

                    conn.outputStream.use { os ->
                        val writer = os.bufferedWriter(Charsets.UTF_8)
                        // Part 1: Metadata
                        writer.write("--$boundary\r\n")
                        writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                        writer.write(metadata)
                        writer.write("\r\n")

                        // Part 2: Media JSON Content
                        writer.write("--$boundary\r\n")
                        writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                        writer.write(jsonString)
                        writer.write("\r\n")

                        writer.write("--$boundary--\r\n")
                        writer.flush()
                    }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        Result.success(true)
                    } else {
                        Result.failure(Exception("Google Drive upload returned HTTP $code"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Downloads the backup JSON directly from Google Drive appDataFolder in Google Cloud.
     * Directly loads into memory without saving to the local device file system.
     */
    suspend fun downloadFromCloud(context: Context, email: String): Result<String?> =
        withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken(context, email)
                    ?: return@withContext Result.failure(
                        Exception("Could not obtain Google Drive authorization for $email. Please check your internet connection.")
                    )

                val fileId = findBackupFileId(token)
                    ?: return@withContext Result.success(null)

                val downloadUrl = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                val conn = downloadUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 15000
                conn.readTimeout = 20000

                val code = conn.responseCode
                if (code in 200..299) {
                    val content = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    Result.success(content)
                } else {
                    Result.failure(Exception("Google Drive download returned HTTP $code"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun findBackupFileId(token: String): String? {
        return try {
            val query = URLEncoder.encode("name = '$BACKUP_FILE_NAME' and trashed = false", "UTF-8")
            val url = URL("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$query&fields=files(id,name,modifiedTime)")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            if (conn.responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(response)
                val files = root.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    files.getJSONObject(0).getString("id")
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
