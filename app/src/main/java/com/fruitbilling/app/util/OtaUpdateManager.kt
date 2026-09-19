package com.fruitbilling.app.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.fruitbilling.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String?
)

/**
 * Over-The-Air (OTA) App Update Manager:
 * Checks for updates from GitHub Releases and facilitates 1-tap download & install.
 */
object OtaUpdateManager {

    private const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/anushkumar701/BIlling-App/releases/latest"

    suspend fun checkForUpdates(): Result<AppReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "FruitBillingApp-${BuildConfig.VERSION_NAME}")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("Server returned code ${connection.responseCode}"))
            }

            val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val json = JSONObject(response)

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", "New Update")
            val releaseNotes = json.optString("body", "Bug fixes and improvements.")

            // Find APK download URL from assets
            var apkDownloadUrl: String? = null
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            val currentVersion = BuildConfig.VERSION_NAME
            val cleanRemoteVersion = tagName.removePrefix("v").trim()
            val isAvailable = isNewerVersion(cleanRemoteVersion, currentVersion)

            Result.success(
                AppReleaseInfo(
                    isUpdateAvailable = isAvailable,
                    currentVersion = currentVersion,
                    latestVersion = tagName,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkDownloadUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val remoteParts = remote.split('.').mapNotNull { it.toIntOrNull() }
        val currentParts = current.split('.').mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /**
     * Downloads the APK file using Android's DownloadManager and prompts for installation.
     */
    fun startDownloadAndInstall(context: Context, downloadUrl: String, versionTag: String) {
        val fileName = "FruitBilling_${versionTag.replace(".", "_")}.apk"
        val destinationFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (destinationFile.exists()) destinationFile.delete()

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Fruit Billing App Update ($versionTag)")
            setDescription("Downloading latest version…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(destinationFile))
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId && destinationFile.exists()) {
                    installApk(context, destinationFile)
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(onCompleteReceiver, filter)
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            // Fallback: Open file directly via system intent
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }
}
