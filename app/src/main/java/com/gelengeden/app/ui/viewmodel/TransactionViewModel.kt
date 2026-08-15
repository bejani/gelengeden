package com.gelengeden.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gelengeden.app.data.BackupManager
import com.gelengeden.app.data.BankSender
import com.gelengeden.app.data.PendingBankSms
import com.gelengeden.app.data.BackupSummary
import com.gelengeden.app.data.Category
import com.gelengeden.app.data.Transaction
import com.gelengeden.app.data.TransactionRepository
import com.gelengeden.app.data.TransactionType
import com.gelengeden.app.ui.report.DateRange
import com.gelengeden.app.ui.report.ReportPeriodPreset
import com.gelengeden.app.ui.report.ReportTypeFilter
import com.gelengeden.app.ui.report.ReportUiState
import com.gelengeden.app.ui.report.buildReport
import com.gelengeden.app.ui.report.currentMonthRange
import com.gelengeden.app.ui.report.persianMonthRange
import com.gelengeden.app.ui.report.rangeForPreset
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FilterType {
    ALL, INCOME, EXPENSE
}

data class DashboardUiState(
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val filter: FilterType = FilterType.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

private data class ReportSelection(
    val preset: ReportPeriodPreset = ReportPeriodPreset.THIS_MONTH,
    val customRange: DateRange = currentMonthRange(),
    val typeFilter: ReportTypeFilter = ReportTypeFilter.ALL,
    val chartMonths: Int = 6
)

class TransactionViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(FilterType.ALL)
    val filter: StateFlow<FilterType> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _reportSelection = MutableStateFlow(ReportSelection())

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllTransactions(),
        repository.getTotalIncome(),
        repository.getTotalExpense(),
        repository.getBalance(),
        combine(_filter, _searchQuery) { filter, query -> filter to query }
    ) { transactions, income, expense, balance, filterAndQuery ->
        val (filter, query) = filterAndQuery
        val byType = when (filter) {
            FilterType.ALL -> transactions
            FilterType.INCOME -> transactions.filter { it.type == TransactionType.INCOME }
            FilterType.EXPENSE -> transactions.filter { it.type == TransactionType.EXPENSE }
        }
        val trimmedQuery = query.trim()
        val filtered = if (trimmedQuery.isEmpty()) {
            byType
        } else {
            byType.filter { tx -> matchesSearch(tx, trimmedQuery) }
        }
        DashboardUiState(
            transactions = filtered,
            totalIncome = income,
            totalExpense = expense,
            balance = balance,
            filter = filter,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    val reportState: StateFlow<ReportUiState> = combine(
        repository.getAllTransactions(),
        _reportSelection
    ) { transactions, selection ->
        val range = if (selection.preset == ReportPeriodPreset.CUSTOM) {
            selection.customRange
        } else {
            rangeForPreset(selection.preset, selection.customRange)
        }
        buildReport(
            transactions = transactions,
            range = range,
            typeFilter = selection.typeFilter,
            chartMonths = selection.chartMonths,
            preset = selection.preset
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportUiState()
    )

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val incomeCategories: StateFlow<List<Category>> = categories
        .map { list -> list.filter { it.type == TransactionType.INCOME } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val expenseCategories: StateFlow<List<Category>> = categories
        .map { list -> list.filter { it.type == TransactionType.EXPENSE } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _categoryMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val categoryMessage: SharedFlow<String> = _categoryMessage.asSharedFlow()

    val bankSenders: StateFlow<List<BankSender>> = repository.getAllBankSenders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val pendingBankSms: StateFlow<List<PendingBankSms>> = repository.getPendingBankSms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _bankSmsMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val bankSmsMessage: SharedFlow<String> = _bankSmsMessage.asSharedFlow()

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    private val _dataCounts = MutableStateFlow(0 to 0)
    /** Pair of (categoryCount, transactionCount) for the backup screen. */
    val dataCounts: StateFlow<Pair<Int, Int>> = _dataCounts.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllCategories(),
                repository.getAllTransactions()
            ) { cats, txs -> cats.size to txs.size }
                .collect { _dataCounts.value = it }
        }
    }

    fun setFilter(filter: FilterType) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun setReportPreset(preset: ReportPeriodPreset) {
        _reportSelection.value = _reportSelection.value.copy(preset = preset)
    }

    fun setReportTypeFilter(filter: ReportTypeFilter) {
        _reportSelection.value = _reportSelection.value.copy(typeFilter = filter)
    }

    fun setChartMonths(months: Int) {
        _reportSelection.value = _reportSelection.value.copy(chartMonths = months.coerceIn(3, 24))
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val start = minOf(startMillis, endMillis)
        val end = maxOf(startMillis, endMillis)
        _reportSelection.value = _reportSelection.value.copy(
            preset = ReportPeriodPreset.CUSTOM,
            customRange = DateRange(start, end)
        )
    }

    /** Sets the report range to a Solar Hijri (Shamsi) month. [month] is zero-based. */
    fun setMonth(year: Int, month: Int) {
        _reportSelection.value = _reportSelection.value.copy(
            preset = ReportPeriodPreset.CUSTOM,
            customRange = persianMonthRange(year, month)
        )
    }

    fun categoriesFor(type: TransactionType): List<String> {
        val list = categories.value.filter { it.type == type }.map { it.name }
        return list.ifEmpty {
            // Fallback while DB is still loading / seeding
            if (type == TransactionType.INCOME) {
                listOf("Other")
            } else {
                listOf("Other")
            }
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        note: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insert(
                Transaction(
                    title = title.trim(),
                    amount = amount,
                    type = type,
                    category = category,
                    note = note.trim(),
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    suspend fun getTransactionById(id: Long): Transaction? =
        repository.getTransactionById(id)

    fun addCategory(name: String, type: TransactionType) {
        viewModelScope.launch {
            repository.addCategory(name, type)
                .onFailure { _categoryMessage.emit(it.message ?: "Could not add category") }
        }
    }

    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch {
            repository.renameCategory(category, newName)
                .onFailure { _categoryMessage.emit(it.message ?: "Could not rename category") }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
                .onFailure { _categoryMessage.emit(it.message ?: "Could not delete category") }
        }
    }

    fun addBankSender(label: String, address: String, amountWasRial: Boolean) {
        viewModelScope.launch {
            repository.addBankSender(label, address, amountWasRial)
                .onFailure { _bankSmsMessage.emit(it.message ?: "Could not add bank sender") }
        }
    }

    fun deleteBankSender(sender: BankSender) {
        viewModelScope.launch {
            repository.deleteBankSender(sender)
        }
    }

    fun recordPendingBankSms(pendingId: Long, title: String, category: String) {
        viewModelScope.launch {
            repository.recordPendingBankSms(pendingId, title, category)
                .onFailure { _bankSmsMessage.emit(it.message ?: "Could not record bank SMS") }
        }
    }

    /**
     * Runs [block] while [backupBusy] is true. Concurrent calls fail fast.
     * Use for the full backup/restore pipeline (including file I/O on the UI side).
     */
    suspend fun <T> withBackupLock(block: suspend () -> T): Result<T> {
        if (!_backupBusy.compareAndSet(expect = false, update = true)) {
            return Result.failure(IllegalStateException("busy"))
        }
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _backupBusy.value = false
        }
    }

    /** Builds JSON backup text. Caller writes it to a file URI. */
    suspend fun exportBackupJson(): Result<String> = withBackupLock {
        repository.createBackupJson()
    }

    /** Parses backup JSON and returns a summary without changing local data. */
    fun previewBackupJson(json: String): Result<BackupSummary> {
        return BackupManager.decode(json).map { BackupManager.summaryOf(it) }
    }

    /** Replaces all local data with the given backup JSON. */
    suspend fun restoreFromBackupJson(json: String): Result<BackupSummary> = withBackupLock {
        repository.restoreFromJson(json).getOrThrow()
    }

    suspend fun currentDataCounts(): Pair<Int, Int> = repository.currentCounts()

    companion object {
        /** Case-insensitive match on transaction title (name). Also checks category as a light bonus. */
        fun matchesSearch(transaction: Transaction, query: String): Boolean {
            val q = query.trim()
            if (q.isEmpty()) return true
            return transaction.title.contains(q, ignoreCase = true) ||
                transaction.category.contains(q, ignoreCase = true)
        }

        fun factory(repository: TransactionRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                        return TransactionViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
