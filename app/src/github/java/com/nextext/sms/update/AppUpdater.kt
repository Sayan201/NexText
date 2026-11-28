package com.nextext.sms.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateRelease(
    val version: String,
    val tagName: String,
    val changelog: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val isPreRelease: Boolean = false
)

object AppUpdater {
    const val isSupported: Boolean = true

    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/Sayan201/NexText/releases/latest"
    private const val ALL_RELEASES_API =
        "https://api.github.com/repos/Sayan201/NexText/releases?per_page=10"

    suspend fun fetchLatestRelease(includeDev: Boolean = false): AppUpdateRelease? = withContext(Dispatchers.IO) {
        val url = if (includeDev) ALL_RELEASES_API else LATEST_RELEASE_API
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "NexText-Android")
        }
        try {
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            if (!includeDev) {
                return@withContext parseReleaseJson(JSONObject(body))
            }
            val array = JSONArray(body)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.optBoolean("draft", false)) continue
                val parsed = parseReleaseJson(obj)
                if (parsed != null) return@withContext parsed
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun parseReleaseJson(json: JSONObject): AppUpdateRelease? {
        val tagName = json.optString("tag_name").trim()
        if (tagName.isBlank()) return null
        val version = tagName.removePrefix("v").removePrefix("V")
        val changelog = json.optString("body").trim()
            .ifBlank { "Bug fixes and performance improvements." }
        val releaseUrl = json.optString("html_url")
            .ifBlank { "https://github.com/Sayan201/NexText/releases" }
        val isPreRelease = json.optBoolean("prerelease", false)

        var apkUrl = ""
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (name.equals("app-github-release.apk", ignoreCase = true) ||
                    name.equals("app-release.apk", ignoreCase = true)
                ) {
                    apkUrl = url
                    break
                }
                if (apkUrl.isBlank() && name.endsWith(".apk", ignoreCase = true) &&
                    !name.contains("debug", ignoreCase = true) &&
                    !name.contains("unsigned", ignoreCase = true)
                ) {
                    apkUrl = url
                }
            }
        }
        if (apkUrl.isBlank()) return null

        return AppUpdateRelease(
            version = version,
            tagName = tagName,
            changelog = changelog,
            downloadUrl = apkUrl,
            releaseUrl = releaseUrl,
            isPreRelease = isPreRelease
        )
    }

    fun isNewerVersion(remoteVersionOrTag: String, currentVersion: String): Boolean {
        val remoteClean = remoteVersionOrTag.removePrefix("v").removePrefix("V").trim()
        val localClean = currentVersion.removePrefix("v").removePrefix("V").trim()

        val remoteCore = remoteClean.substringBefore("-").substringBefore("+")
            .split(".").mapNotNull { it.toIntOrNull() }
        val localCore = localClean.substringBefore("-").substringBefore("+")
            .split(".").mapNotNull { it.toIntOrNull() }

        val maxCore = maxOf(remoteCore.size, localCore.size)
        for (i in 0 until maxCore) {
            val r = remoteCore.getOrElse(i) { 0 }
            val l = localCore.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }

        // Core versions are equal (e.g. 1.6.9 vs 1.6.9-dev.1)
        val remotePre = remoteClean.substringAfter("-", "")
        val localPre = localClean.substringAfter("-", "")
        if (remotePre.isEmpty() && localPre.isNotEmpty()) return true // Stable > Pre-release
        if (remotePre.isNotEmpty() && localPre.isEmpty()) return false // Pre-release < Stable
        if (remotePre.isEmpty() && localPre.isEmpty()) return false

        val remotePreNums = remotePre.split(".", "-", "+").mapNotNull { it.toIntOrNull() }
        val localPreNums = localPre.split(".", "-", "+").mapNotNull { it.toIntOrNull() }
        val maxPre = maxOf(remotePreNums.size, localPreNums.size)
        for (i in 0 until maxPre) {
            val r = remotePreNums.getOrElse(i) { 0 }
            val l = localPreNums.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        version: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "NexText-$version.apk")
        val tempFile = File(updatesDir, "NexText-$version.apk.part")

        var conn: HttpURLConnection? = null
        try {
            var currentUrl = downloadUrl
            var redirects = 0
            while (redirects < 5) {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 12000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "NexText-Android")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val next = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (next.isNullOrBlank()) return@withContext null
                    currentUrl = next
                    redirects++
                } else {
                    break
                }
            }
            val activeConn = conn ?: return@withContext null
            if (activeConn.responseCode !in 200..299) return@withContext null

            val totalBytes = activeConn.contentLengthLong.coerceAtLeast(-1L)
            var downloaded = 0L
            var lastReported = -1

            activeConn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val pct = ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (pct != lastReported) {
                                lastReported = pct
                                withContext(Dispatchers.Main) { onProgress(pct) }
                            }
                        }
                    }
                    output.flush()
                }
            }
            if (targetFile.exists()) targetFile.delete()
            if (tempFile.renameTo(targetFile)) targetFile else null
        } catch (_: Exception) {
            tempFile.delete()
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Launches Android's package installer for [apkFile].
     * Returns true if the installer intent was launched, false if Unknown Sources permission is needed first.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val permIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(permIntent)
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
        return true
    }
}
