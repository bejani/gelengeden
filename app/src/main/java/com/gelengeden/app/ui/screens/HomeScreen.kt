package com.gelengeden.app.ui.screens

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gelengeden.app.R
import com.gelengeden.app.data.Transaction
import com.gelengeden.app.data.TransactionExporter
import com.gelengeden.app.ui.components.AppLogo
import com.gelengeden.app.ui.components.EmptyTransactions
import com.gelengeden.app.ui.components.PendingBankSmsReviewDialog
import com.gelengeden.app.ui.components.SummaryCard
import com.gelengeden.app.ui.components.TransactionItem
import com.gelengeden.app.ui.util.formatPersianDate
import com.gelengeden.app.ui.viewmodel.FilterType
import com.gelengeden.app.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val XLSX_MIME =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

private enum class ExportFormat {
    CSV,
    EXCEL
}

private data class PendingExport(
    val format: ExportFormat,
    val transactions: List<Transaction>,
    val labels: TransactionExporter.Labels
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TransactionViewModel,
    onAddClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onManageCategoriesClick: () -> Unit,
    onReportsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onQuickAddTemplatesClick: () -> Unit,
    onQuickAddClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingBankSms by viewModel.pendingBankSms.collectAsStateWithLifecycle()
    val quickAddTemplates by viewModel.quickAddTemplates.collectAsStateWithLifecycle()
    val selectedCategory = uiState.selectedCategory
    var deferredBankSmsId by remember { mutableStateOf<Long?>(null) }
    val reviewSms = pendingBankSms.firstOrNull { it.id != deferredBankSmsId }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<PendingExport?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val hasSearch = uiState.searchQuery.isNotBlank()
    val hasActiveFilters = hasSearch || uiState.filter != FilterType.ALL || selectedCategory != null

    fun shareExport(uri: Uri, mime: String) {
        runCatching {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(
                    share,
                    context.getString(R.string.export_share_title)
                )
            )
        }
    }

    fun writeExportAndNotify(uri: Uri, payload: PendingExport) {
        val mime = when (payload.format) {
            ExportFormat.CSV -> "text/csv"
            ExportFormat.EXCEL -> XLSX_MIME
        }
        val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        scope.launch {
            val writeResult = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = when (payload.format) {
                        ExportFormat.CSV -> TransactionExporter.toCsv(
                            payload.transactions,
                            payload.labels,
                            ::formatPersianDate
                        ).toByteArray(Charsets.UTF_8)
                        ExportFormat.EXCEL -> TransactionExporter.toXlsx(
                            payload.transactions,
                            payload.labels,
                            ::formatPersianDate,
                            rightToLeft = rtl
                        )
                    }
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                        out.flush()
                    } ?: error("open failed")
                }
            }
            isExporting = false
            if (writeResult.isFailure) {
                snackbarHostState.showSnackbar(context.getString(R.string.export_failed))
                return@launch
            }
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.export_saved, payload.transactions.size),
                actionLabel = context.getString(R.string.export_share_action),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                shareExport(uri, mime)
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val payload = pendingExport
        pendingExport = null
        if (uri == null || payload == null) {
            isExporting = false
            return@rememberLauncherForActivityResult
        }
        writeExportAndNotify(uri, payload)
    }

    val createExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(XLSX_MIME)
    ) { uri ->
        val payload = pendingExport
        pendingExport = null
        if (uri == null || payload == null) {
            isExporting = false
            return@rememberLauncherForActivityResult
        }
        writeExportAndNotify(uri, payload)
    }

    fun startExport(format: ExportFormat) {
        if (isExporting) return
        val list = uiState.transactions
        if (list.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.export_empty))
            }
            return
        }
        showExportDialog = false
        isExporting = true
        val labels = TransactionExporter.Labels(
            date = context.getString(R.string.export_column_date),
            type = context.getString(R.string.export_column_type),
            title = context.getString(R.string.export_column_title),
            category = context.getString(R.string.export_column_category),
            amount = context.getString(R.string.export_column_amount),
            note = context.getString(R.string.export_column_note),
            income = context.getString(R.string.income),
            expense = context.getString(R.string.expense),
            sheetName = context.getString(R.string.export_sheet_name),
            summarySheet = context.getString(R.string.export_sheet_summary),
            incomeSheet = context.getString(R.string.income),
            expenseSheet = context.getString(R.string.expense),
            countLabel = context.getString(R.string.export_summary_count),
            totalIncomeLabel = context.getString(R.string.export_summary_total_income),
            totalExpenseLabel = context.getString(R.string.export_summary_total_expense),
            balanceLabel = context.getString(R.string.balance),
            incomeByCategory = context.getString(R.string.export_summary_income_by_category),
            expenseByCategory = context.getString(R.string.export_summary_expense_by_category)
        )
        pendingExport = PendingExport(format, list, labels)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        when (format) {
            ExportFormat.CSV -> createCsvLauncher.launch("gelengeden-transactions-$stamp.csv")
            ExportFormat.EXCEL -> createExcelLauncher.launch("gelengeden-transactions-$stamp.xlsx")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onReportsClick) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.reports)
                        )
                    }
                    IconButton(onClick = onManageCategoriesClick) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = stringResource(R.string.manage_categories)
                        )
                    }
                    IconButton(onClick = onBackupClick) {
                        Icon(
                            imageVector = Icons.Default.SettingsBackupRestore,
                            contentDescription = stringResource(R.string.backup_cd)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_cd)
                        )
                    }
                    IconButton(onClick = onAboutClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.about_cd)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_transaction)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Same width as before; shorter height + tighter top/bottom margins only
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(
                            width = 280.dp,
                            height = 160.dp,
                            cornerRadius = 10.dp
                        )
                    }
                }
                item {
                    SummaryCard(
                        balance = uiState.balance,
                        totalIncome = uiState.totalIncome,
                        totalExpense = uiState.totalExpense
                    )
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.quick_add_home_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onQuickAddTemplatesClick) {
                                Text(stringResource(R.string.quick_add_manage))
                            }
                        }
                        if (quickAddTemplates.isEmpty()) {
                            Text(
                                text = stringResource(R.string.quick_add_home_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickAddTemplates.forEach { template ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { onQuickAddClick(template.id) },
                                        label = { Text(template.title) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.transactions),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (uiState.transactions.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.export_empty)
                                        )
                                    }
                                } else {
                                    showExportDialog = true
                                }
                            },
                            enabled = !isExporting
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = stringResource(R.string.export_list_cd)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        placeholder = {
                            Text(stringResource(R.string.search_transactions_hint))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_transactions)
                            )
                        },
                        trailingIcon = {
                            if (hasSearch) {
                                IconButton(onClick = {
                                    viewModel.clearSearch()
                                    focusManager.clearFocus()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.search_clear)
                                    )
                                }
                            }
                        },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { focusManager.clearFocus() }
                            )
                        )
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = stringResource(R.string.advanced_filters)
                            )
                        }
                    }
                    if (selectedCategory != null) {
                        Text(
                            text = stringResource(R.string.active_category_filter, selectedCategory),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = uiState.filter == FilterType.ALL,
                            onClick = { viewModel.setFilter(FilterType.ALL) },
                            label = { Text(stringResource(R.string.all)) }
                        )
                        FilterChip(
                            selected = uiState.filter == FilterType.INCOME,
                            onClick = { viewModel.setFilter(FilterType.INCOME) },
                            label = { Text(stringResource(R.string.income)) }
                        )
                        FilterChip(
                            selected = uiState.filter == FilterType.EXPENSE,
                            onClick = { viewModel.setFilter(FilterType.EXPENSE) },
                            label = { Text(stringResource(R.string.expense)) }
                        )
                        if (selectedCategory != null) {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.setCategoryFilter(null) },
                                label = { Text(selectedCategory) }
                            )
                        }
                    }
                    if (hasActiveFilters) {
                        TextButton(
                            onClick = { viewModel.clearAllFilters() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.clear_all_filters))
                        }
                    }
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        if (hasSearch) {
                            EmptySearchResults()
                        } else {
                            EmptyTransactions()
                        }
                    }
                } else {
                    items(
                        items = uiState.transactions,
                        key = { transaction -> "${uiState.listGeneration}:${transaction.id}" }
                    ) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) },
                            onDelete = { pendingDelete = transaction }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text(stringResource(R.string.advanced_filters)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.filter_by_category),
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (uiState.availableCategories.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_categories_for_filter),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.availableCategories.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        viewModel.setCategoryFilter(
                                            if (selectedCategory == category) null else category
                                        )
                                    },
                                    label = { Text(category) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearAllFilters() }) {
                    Text(stringResource(R.string.clear_all_filters))
                }
            }
        )
    }
    pendingDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_transaction_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_transaction_message,
                        transaction.title
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction)
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    reviewSms?.let { pendingSms ->
        PendingBankSmsReviewDialog(
            pendingSms = pendingSms,
            categories = viewModel.categoriesFor(pendingSms.suggestedType),
            onLater = { deferredBankSmsId = pendingSms.id },
            onConfirm = { title, category ->
                viewModel.recordPendingBankSms(pendingSms.id, title, category)
                deferredBankSmsId = null
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { if (!isExporting) showExportDialog = false },
            title = { Text(stringResource(R.string.export_choose_format)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { startExport(ExportFormat.CSV) },
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TextSnippet,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = stringResource(R.string.export_as_csv),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.export_csv_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(
                        onClick = { startExport(ExportFormat.EXCEL) },
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = stringResource(R.string.export_as_excel),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.export_excel_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false },
                    enabled = !isExporting
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun EmptySearchResults(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.no_search_results),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.no_search_results_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
