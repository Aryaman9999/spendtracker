package com.aryaman.spendtracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean
)

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_REPO_API = "https://api.github.com/repos/Aryaman9999/spendtracker/releases/latest"
    private const val PREFS_NAME = "spendtracker_update_prefs"
    private const val KEY_LAST_CHECK_TIME = "last_auto_check_time"
    private const val KEY_DISMISSED_VERSION = "dismissed_version"
    private const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

    /**
     * Gets the actual installed version name from Android PackageManager.
     */
    fun getAppVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: com.aryaman.spendtracker.BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            com.aryaman.spendtracker.BuildConfig.VERSION_NAME
        }
    }

    /**
     * Determines if a background check should run (rate limited to once every 24h).
     */
    fun shouldAutoCheck(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        val now = System.currentTimeMillis()
        return (now - lastCheck) >= AUTO_CHECK_INTERVAL_MS
    }

    /**
     * Records the timestamp of an automatic background check.
     */
    fun recordAutoCheck(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * Checks if the user dismissed this specific version.
     */
    fun isVersionDismissed(context: Context, version: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dismissed = prefs.getString(KEY_DISMISSED_VERSION, null)
        return dismissed != null && dismissed.equals(version.trim().removePrefix("v"), ignoreCase = true)
    }

    /**
     * Records that the user dismissed/snoozed an update prompt for this version.
     */
    fun markVersionDismissed(context: Context, version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISMISSED_VERSION, version.trim().removePrefix("v")).apply()
    }

    /**
     * Clears dismissed version state (e.g. when manually checking for updates).
     */
    fun clearDismissedVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DISMISSED_VERSION).apply()
    }

    /**
     * Checks GitHub Releases for a newer version than currentVersion.
     * Runs completely on Dispatchers.IO.
     */
    suspend fun checkForUpdate(currentVersion: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_REPO_API)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SpendTracker-Android")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GitHub API returned HTTP ${connection.responseCode}")
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }
            val json = JSONObject(response)

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", "New Update").trim()
            val releaseNotes = json.optString("body", "Bug fixes and improvements.").trim()

            // Find APK asset URL
            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            if (tagName.isBlank() || apkUrl.isNullOrBlank()) {
                return@withContext null
            }

            val isNewer = isNewerVersion(tagName, currentVersion)

            AppUpdateInfo(
                latestVersion = tagName.trim().removePrefix("v"),
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                downloadUrl = apkUrl,
                isUpdateAvailable = isNewer
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compares semantic version strings e.g. "2.6.2" vs "2.6.1".
     * Returns true ONLY if remoteTag represents a strictly higher version than currentVersion.
     */
    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteClean = remoteTag.trim().trimStart { !it.isDigit() }
        val currentClean = currentVersion.trim().trimStart { !it.isDigit() }

        if (remoteClean.isBlank() || currentClean.isBlank()) return false
        if (remoteClean == currentClean) return false

        val remoteParts = remoteClean.split(Regex("[^0-9]+")).filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
        val currentParts = currentClean.split(Regex("[^0-9]+")).filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /**
     * Directly downloads the APK using HttpURLConnection into the app's cache dir
     * and launches the system package installer.
     * Bypasses DownloadManager and Android 14 BroadcastReceiver restrictions entirely.
     */
    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        version: String,
        onProgress: (Int) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check unknown sources permission on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Please allow SpendTracker to install apps", Toast.LENGTH_LONG).show()
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                    return@withContext Result.failure(IllegalStateException("Install permission required"))
                }
            }

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            // Clean up any old downloaded update files
            updatesDir.listFiles()?.forEach { if (it.isFile) it.delete() }

            val targetFile = File(updatesDir, "SpendTracker-v$version.apk")

            val url = URL(apkUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "SpendTracker-Android")
            }

            // Handle HTTP 302/301 redirects if needed
            val responseCode = connection.responseCode
            val finalConnection = if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER
            ) {
                val redirectUrl = connection.getHeaderField("Location")
                connection.disconnect()
                (URL(redirectUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "SpendTracker-Android")
                }
            } else {
                connection
            }

            val totalBytes = finalConnection.contentLengthLong
            var downloadedBytes = 0L

            finalConnection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }
            finalConnection.disconnect()

            if (!targetFile.exists() || targetFile.length() == 0L) {
                return@withContext Result.failure(IllegalStateException("Downloaded file is empty"))
            }

            // Launch system package installer
            withContext(Dispatchers.Main) {
                installApk(context, targetFile)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Download and install failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Update download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            Result.failure(e)
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            Toast.makeText(context, "Failed to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
