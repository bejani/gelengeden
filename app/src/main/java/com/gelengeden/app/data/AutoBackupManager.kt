package com.gelengeden.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Persistent state shown in the backup screen for automatic folder backups. */
data class AutoBackupState(
    val folderUri: String? = null,
    val folderName: String? = null,
    val lastSuccessAt: Long? = null
) {
    val isConfigured: Boolean get() = !folderUri.isNullOrBlank()
}

/**
 * Schedules an approximate daily local backup. Android may defer background work to preserve
 * battery life, so this never claims an exact wall-clock execution time.
 */
class AutoBackupManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<AutoBackupState> = _state.asStateFlow()

    fun configureFolder(uri: Uri, folderName: String?) {
        prefs.edit()
            .putString(KEY_FOLDER_URI, uri.toString())
            .putString(KEY_FOLDER_NAME, folderName.orEmpty())
            .apply()
        _state.value = readState()
        ensureScheduled()
    }

    fun disable() {
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
        prefs.edit()
            .remove(KEY_FOLDER_URI)
            .remove(KEY_FOLDER_NAME)
            .remove(KEY_LAST_SUCCESS_AT)
            .apply()
        _state.value = readState()
    }

    fun ensureScheduled() {
        if (!readState().isConfigured) return
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    suspend fun runNow(): Result<Long> {
        val result = AutoBackupRunner.run(appContext)
        if (result.isSuccess) _state.value = readState()
        return result
    }

    private fun readState(): AutoBackupState = AutoBackupState(
        folderUri = prefs.getString(KEY_FOLDER_URI, null),
        folderName = prefs.getString(KEY_FOLDER_NAME, null)?.ifBlank { null },
        lastSuccessAt = prefs.getLong(KEY_LAST_SUCCESS_AT, 0L).takeIf { it > 0 }
    )

    companion object {
        internal const val PREFS_NAME = "gelengeden_auto_backup"
        internal const val KEY_FOLDER_URI = "folder_uri"
        internal const val KEY_FOLDER_NAME = "folder_name"
        internal const val KEY_LAST_SUCCESS_AT = "last_success_at"
        internal const val WORK_NAME = "gelengeden_daily_auto_backup"
    }
}

/** WorkManager entry point for the automatic daily backup. */
class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val result = AutoBackupRunner.run(applicationContext)
        if (result.isSuccess) return Result.success()
        val error = result.exceptionOrNull()
        return if (error is SecurityException || error?.message?.contains("folder", ignoreCase = true) == true) {
            Result.failure()
        } else {
            Result.retry()
        }
    }
}

internal object AutoBackupRunner {
    suspend fun run(context: Context): Result<Long> = runCatching {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(
            AutoBackupManager.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val folderUri = prefs.getString(AutoBackupManager.KEY_FOLDER_URI, null)
            ?.let(Uri::parse)
            ?: error("Automatic backup folder is not configured")
        val folder = DocumentFile.fromTreeUri(appContext, folderUri)
            ?.takeIf { it.exists() && it.canWrite() }
            ?: error("Selected backup folder is unavailable")

        val json = TransactionRepository(AppDatabase.getInstance(appContext)).createBackupJson()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = folder.createFile("application/json", "gelengeden-auto-backup-$stamp.json")
            ?: error("Could not create backup file")
        appContext.contentResolver.openOutputStream(file.uri)?.use { output ->
            output.write(json.toByteArray(Charsets.UTF_8))
            output.flush()
        } ?: error("Could not open backup file")

        val completedAt = System.currentTimeMillis()
        prefs.edit().putLong(AutoBackupManager.KEY_LAST_SUCCESS_AT, completedAt).apply()
        completedAt
    }
}
