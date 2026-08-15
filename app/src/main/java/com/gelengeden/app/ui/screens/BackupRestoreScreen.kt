package com.gelengeden.app.ui.screens

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gelengeden.app.R
import com.gelengeden.app.data.AutoBackupManager
import com.gelengeden.app.data.BackupManager
import com.gelengeden.app.ui.util.formatPersianDate
import com.gelengeden.app.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: TransactionViewModel,
    autoBackupManager: AutoBackupManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val busy by viewModel.backupBusy.collectAsStateWithLifecycle()
    val dataCounts by viewModel.dataCounts.collectAsStateWithLifecycle()
    val autoBackupState by autoBackupManager.state.collectAsStateWithLifecycle()

    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var restorePreview by remember { mutableStateOf<String?>(null) }
    var lastSavedBackupUri by remember { mutableStateOf<Uri?>(null) }
    var isAutoBackupRunning by remember { mutableStateOf(false) }

    fun shareBackup(uri: Uri) {
        runCatching {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(
                    share,
                    context.getString(R.string.backup_share_title)
                )
            )
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingBackupJson
        pendingBackupJson = null
        if (uri == null || json == null) return@rememberLauncherForActivityResult

        scope.launch {
            val writeResult = viewModel.withBackupLock {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: error("open failed")
                }
            }

            if (writeResult.isFailure) {
                snackbarHostState.showSnackbar(context.getString(R.string.backup_failed))
                return@launch
            }

            lastSavedBackupUri = uri
            val data = BackupManager.decode(json).getOrNull()
            val message = context.getString(
                R.string.backup_saved,
                data?.transactions?.size ?: 0,
                data?.categories?.size ?: 0
            )
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.getString(R.string.backup_share_action),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                shareBackup(uri)
            }
        }
    }

    val selectBackupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val permissionFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val permissionResult = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, permissionFlags)
        }
        if (permissionResult.isFailure) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.auto_backup_folder_permission_failed)) }
            return@rememberLauncherForActivityResult
        }
        val folderName = DocumentFile.fromTreeUri(context, uri)?.name
        autoBackupManager.configureFolder(uri, folderName)
        isAutoBackupRunning = true
        scope.launch {
            val result = viewModel.withBackupLock { autoBackupManager.runNow().getOrThrow() }
            isAutoBackupRunning = false
            snackbarHostState.showSnackbar(
                context.getString(
                    if (result.isSuccess) R.string.auto_backup_now_success else R.string.auto_backup_now_failed
                )
            )
        }
    }

    fun runAutomaticBackupNow() {
        if (isAutoBackupRunning || busy || !autoBackupState.isConfigured) return
        isAutoBackupRunning = true
        scope.launch {
            val result = viewModel.withBackupLock { autoBackupManager.runNow().getOrThrow() }
            isAutoBackupRunning = false
            snackbarHostState.showSnackbar(
                context.getString(
                    if (result.isSuccess) R.string.auto_backup_now_success else R.string.auto_backup_now_failed
                )
            )
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val previewResult = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: error("empty")
                    val data = BackupManager.decode(text).getOrThrow()
                    val dateLabel = if (data.exportedAt > 0) {
                        formatPersianDate(data.exportedAt)
                    } else {
                        "—"
                    }
                    val label = context.getString(
                        R.string.restore_preview,
                        data.transactions.size,
                        data.categories.size,
                        dateLabel
                    )
                    text to label
                }
            }
            previewResult.fold(
                onSuccess = { (json, label) ->
                    pendingRestoreJson = json
                    restorePreview = label
                    showRestoreConfirm = true
                },
                onFailure = {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.restore_invalid_file)
                    )
                }
            )
        }
    }

    fun startBackup() {
        if (busy) return
        scope.launch {
            // Build JSON outside the write lock so the SAF picker is free to open.
            val exportResult = runCatching { viewModel.exportBackupJson().getOrThrow() }
            exportResult.fold(
                onSuccess = { json ->
                    pendingBackupJson = json
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                    createDocumentLauncher.launch("gelengeden-backup-$stamp.json")
                },
                onFailure = {
                    snackbarHostState.showSnackbar(context.getString(R.string.backup_failed))
                }
            )
        }
    }

    fun confirmRestore() {
        val json = pendingRestoreJson ?: return
        showRestoreConfirm = false
        scope.launch {
            viewModel.restoreFromBackupJson(json).fold(
                onSuccess = { summary ->
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.restore_success,
                            summary.transactionCount,
                            summary.categoryCount
                        )
                    )
                },
                onFailure = {
                    val message = when {
                        it.message.equals("busy", ignoreCase = true) ->
                            context.getString(R.string.backup_busy)
                        it is IllegalArgumentException ->
                            context.getString(R.string.restore_invalid_file)
                        else ->
                            context.getString(R.string.restore_failed)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            )
            pendingRestoreJson = null
            restorePreview = null
        }
    }

    fun clearRestorePending() {
        showRestoreConfirm = false
        pendingRestoreJson = null
        restorePreview = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !busy) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.backup_restore_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.backup_current_data_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(
                                    R.string.backup_current_data,
                                    dataCounts.second,
                                    dataCounts.first
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.backup_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.backup_section_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { startBackup() },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(stringResource(R.string.backup_create))
                        }
                        lastSavedBackupUri?.let { uri ->
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { shareBackup(uri) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.backup_share_again))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auto_backup_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.auto_backup_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (autoBackupState.isConfigured) {
                            Text(
                                text = stringResource(
                                    R.string.auto_backup_folder_selected,
                                    autoBackupState.folderName ?: stringResource(R.string.auto_backup_folder_fallback)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            autoBackupState.lastSuccessAt?.let { timestamp ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.auto_backup_last_success,
                                        formatPersianDate(timestamp)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { runAutomaticBackupNow() },
                                enabled = !busy && !isAutoBackupRunning,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isAutoBackupRunning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(stringResource(R.string.auto_backup_run_now))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { selectBackupFolderLauncher.launch(null) },
                                enabled = !busy && !isAutoBackupRunning,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.auto_backup_change_folder))
                            }
                            TextButton(
                                onClick = { autoBackupManager.disable() },
                                enabled = !busy && !isAutoBackupRunning,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.auto_backup_disable))
                            }
                        } else {
                            Button(
                                onClick = { selectBackupFolderLauncher.launch(null) },
                                enabled = !busy && !isAutoBackupRunning,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.auto_backup_select_folder))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.restore_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.restore_section_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                openDocumentLauncher.launch(
                                    arrayOf("application/json", "text/*", "*/*")
                                )
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.restore_from_file))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.backup_tips),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!busy) clearRestorePending()
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.restore_confirm_message))
                    restorePreview?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { confirmRestore() },
                    enabled = !busy
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.restore_confirm_action),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { clearRestorePending() },
                    enabled = !busy
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
