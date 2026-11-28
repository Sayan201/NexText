package com.nextext.sms.update

import android.content.Context
import java.io.File

data class AppUpdateRelease(
    val version: String,
    val tagName: String,
    val changelog: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val isPreRelease: Boolean = false
)

/**
 * F-Droid distribution stub: updates are handled by the F-Droid client itself,
 * keeping the F-Droid build 100% offline with zero network or self-install code.
 */
object AppUpdater {
    const val isSupported: Boolean = false

    suspend fun fetchLatestRelease(includeDev: Boolean = false): AppUpdateRelease? = null

    fun isNewerVersion(remoteVersionOrTag: String, currentVersion: String): Boolean = false

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        version: String,
        onProgress: (Int) -> Unit
    ): File? = null

    fun installApk(context: Context, apkFile: File): Boolean = false
}
