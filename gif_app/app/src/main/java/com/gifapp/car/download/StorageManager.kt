package com.gifapp.car.download

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

object StorageManager {
    const val DIR_NAME = "wear_gif"

    val downloadDir: File
        get() = File(Environment.getExternalStorageDirectory(), DIR_NAME)

    fun ensureDir(): Boolean {
        val dir = downloadDir
        if (!dir.exists()) dir.mkdirs()
        return dir.exists() && dir.canWrite()
    }

    fun hasPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    fun allFilesAccessIntent(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            null
        }

    fun localFiles(): List<File> {
        val dir = downloadDir
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f ->
            f.isFile && (f.extension.equals("gif", true) || f.extension.equals("mp4", true))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun isDownloaded(name: String): Boolean = targetFile(name).exists()

    fun targetFile(name: String): File {
        val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "file" }
        return File(downloadDir, safe)
    }
}