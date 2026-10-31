package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.repository.BastaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AutoBackupManager {

    private const val PREFS_NAME = "basta_auto_backup_prefs"
    private const val KEY_ENABLED = "auto_backup_enabled"
    private const val KEY_LAST_BACKUP = "last_backup_timestamp"
    private const val MAX_BACKUP_FILES = 10
    private const val MIN_INTERVAL_MILLIS = 90_000L // 1.5 minutes throttle between auto-backups

    private fun getBackupDir(context: Context): File {
        val dir = File(context.filesDir, "auto_backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP, 0L)
    }

    suspend fun performAutoBackup(
        context: Context,
        repository: BastaRepository,
        force: Boolean = false
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!force && !isAutoBackupEnabled(context)) {
                return@withContext null
            }

            val now = System.currentTimeMillis()
            val lastTime = getLastBackupTime(context)

            if (!force && (now - lastTime < MIN_INTERVAL_MILLIS)) {
                return@withContext null
            }

            val backupJson = repository.createFullJsonBackup()
            val dir = getBackupDir(context)

            val timeFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fileName = "basta_autobackup_${timeFormatter.format(Date(now))}.json"
            val file = File(dir, fileName)

            FileOutputStream(file).use { output ->
                output.write(backupJson.toByteArray(Charsets.UTF_8))
            }

            // Update timestamp
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_BACKUP, now).apply()

            // Prune old backups
            pruneOldBackups(dir)

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun pruneOldBackups(dir: File) {
        val files = dir.listFiles { f -> f.extension == "json" } ?: return
        if (files.size > MAX_BACKUP_FILES) {
            files.sortBy { it.lastModified() }
            val excessCount = files.size - MAX_BACKUP_FILES
            for (i in 0 until excessCount) {
                files[i].delete()
            }
        }
    }

    fun getAutoBackupFiles(context: Context): List<File> {
        val dir = getBackupDir(context)
        val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
    }

    suspend fun restoreFromBackupFile(
        file: File,
        repository: BastaRepository
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext false
            val content = file.readText(Charsets.UTF_8)
            repository.restoreFromJsonBackup(content)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteBackupFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - مدير البسطة")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة ملف النسخة الاحتياطية"))
    }
}
