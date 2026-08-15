package com.gelengeden.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TransactionRepository(
    private val database: AppDatabase
) {
    private val transactionDao: TransactionDao = database.transactionDao()
    private val categoryDao: CategoryDao = database.categoryDao()
    private val bankSenderDao: BankSenderDao = database.bankSenderDao()
    private val pendingBankSmsDao: PendingBankSmsDao = database.pendingBankSmsDao()
    private val quickAddTemplateDao: QuickAddTemplateDao = database.quickAddTemplateDao()

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    fun getTransactionsByCategory(
        category: String,
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category, type, startMillis, endMillis)

    fun getTotalIncome(): Flow<Double> = transactionDao.getTotalByType(TransactionType.INCOME)

    fun getTotalExpense(): Flow<Double> = transactionDao.getTotalByType(TransactionType.EXPENSE)

    fun getBalance(): Flow<Double> =
        combine(getTotalIncome(), getTotalExpense()) { income, expense ->
            income - expense
        }

    suspend fun getTotalsOnce(): Triple<Double, Double, Double> {
        val income = transactionDao.getTotalByTypeOnce(TransactionType.INCOME)
        val expense = transactionDao.getTotalByTypeOnce(TransactionType.EXPENSE)
        return Triple(income, expense, income - expense)
    }

    suspend fun getTransactionById(id: Long): Transaction? =
        transactionDao.getTransactionById(id)

    suspend fun insert(transaction: Transaction): Long = transactionDao.insert(transaction)

    suspend fun update(transaction: Transaction) = transactionDao.update(transaction)

    suspend fun delete(transaction: Transaction) = transactionDao.delete(transaction)

    suspend fun deleteById(id: Long) = transactionDao.deleteById(id)

    // --- Categories ---

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type)

    suspend fun addCategory(name: String, type: TransactionType): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("نام دسته نمی‌تواند خالی باشد"))
        }
        if (categoryDao.countByNameAndType(trimmed, type) > 0) {
            return Result.failure(IllegalArgumentException("این دسته از قبل وجود دارد"))
        }
        val sortOrder = categoryDao.countByType(type)
        categoryDao.insert(
            Category(name = trimmed, type = type, sortOrder = sortOrder)
        )
        return Result.success(Unit)
    }

    suspend fun renameCategory(category: Category, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("نام دسته نمی‌تواند خالی باشد"))
        }
        if (categoryDao.countByNameAndType(trimmed, category.type, category.id) > 0) {
            return Result.failure(IllegalArgumentException("این دسته از قبل وجود دارد"))
        }
        val oldName = category.name
        database.withTransaction {
            categoryDao.update(category.copy(name = trimmed))
            if (oldName != trimmed) {
                categoryDao.renameInTransactions(oldName, trimmed, category.type)
                quickAddTemplateDao.renameCategory(oldName, trimmed, category.type)
            }
        }
        return Result.success(Unit)
    }

    suspend fun deleteCategory(category: Category): Result<Unit> {
        if (categoryDao.countByType(category.type) <= 1) {
            return Result.failure(
                IllegalArgumentException(
                    "حداقل یک دسته از این نوع باید باقی بماند"
                )
            )
        }
        val fallback = categoryDao.findFallbackName(category.type, category.id)
            ?: return Result.failure(
                IllegalArgumentException("دستهٔ جایگزین پیدا نشد")
            )
        database.withTransaction {
            categoryDao.reassignTransactions(category.name, fallback, category.type)
            quickAddTemplateDao.renameCategory(category.name, fallback, category.type)
            categoryDao.delete(category)
        }
        return Result.success(Unit)
    }

    // --- Quick-add templates ---

    fun getQuickAddTemplates(): Flow<List<QuickAddTemplate>> = quickAddTemplateDao.getEnabled()

    fun getAllQuickAddTemplates(): Flow<List<QuickAddTemplate>> = quickAddTemplateDao.getAll()

    suspend fun getQuickAddTemplateById(id: Long): QuickAddTemplate? = quickAddTemplateDao.getById(id)

    suspend fun addQuickAddTemplate(
        title: String,
        type: TransactionType,
        category: String,
        note: String
    ): Result<Unit> {
        val cleanTitle = title.trim()
        val cleanCategory = category.trim()
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("نام میان‌بر نمی‌تواند خالی باشد"))
        }
        if (cleanCategory.isEmpty()) {
            return Result.failure(IllegalArgumentException("دستهٔ میان‌بر را انتخاب کنید"))
        }
        quickAddTemplateDao.insert(
            QuickAddTemplate(
                title = cleanTitle,
                type = type,
                category = cleanCategory,
                note = note.trim(),
                sortOrder = quickAddTemplateDao.nextSortOrder()
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateQuickAddTemplate(template: QuickAddTemplate): Result<Unit> {
        if (template.title.isBlank() || template.category.isBlank()) {
            return Result.failure(IllegalArgumentException("نام و دستهٔ میان‌بر الزامی است"))
        }
        quickAddTemplateDao.update(
            template.copy(
                title = template.title.trim(),
                category = template.category.trim(),
                note = template.note.trim()
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteQuickAddTemplate(template: QuickAddTemplate) = quickAddTemplateDao.delete(template)

    // --- Bank SMS senders and review queue ---

    fun getAllBankSenders(): Flow<List<BankSender>> = bankSenderDao.getAll()

    suspend fun getAllBankSendersOnce(): List<BankSender> = bankSenderDao.getAllOnce()

    suspend fun addBankSender(
        label: String,
        address: String,
        amountWasRial: Boolean
    ): Result<Unit> {
        val cleanLabel = label.trim()
        val cleanAddress = address.trim()
        if (cleanLabel.isEmpty() || cleanAddress.isEmpty()) {
            return Result.failure(IllegalArgumentException("Bank name and sender address are required"))
        }
        if (bankSenderDao.getAllOnce().any { it.address.equals(cleanAddress, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("This sender address already exists"))
        }
        bankSenderDao.insert(
            BankSender(
                label = cleanLabel,
                address = cleanAddress,
                amountWasRial = amountWasRial
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteBankSender(sender: BankSender) = bankSenderDao.delete(sender)

    fun getPendingBankSms(): Flow<List<PendingBankSms>> =
        pendingBankSmsDao.getByStatus(PendingSmsStatus.PENDING)

    suspend fun enqueuePendingBankSms(sms: PendingBankSms): Boolean =
        pendingBankSmsDao.insert(sms) != -1L

    suspend fun recordPendingBankSms(
        pendingId: Long,
        title: String,
        category: String
    ): Result<Unit> {
        val cleanTitle = title.trim()
        val cleanCategory = category.trim()
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title is required"))
        }
        if (cleanCategory.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category is required"))
        }
        return try {
            database.withTransaction {
                val pending = pendingBankSmsDao.getById(pendingId)
                    ?: throw IllegalArgumentException("Pending SMS not found")
                if (pending.status != PendingSmsStatus.PENDING) {
                    throw IllegalStateException("This SMS has already been handled")
                }
                val amount = pending.displayAmount
                    ?: throw IllegalArgumentException("SMS amount is invalid")
                transactionDao.insert(
                    Transaction(
                        title = cleanTitle,
                        amount = amount,
                        type = pending.suggestedType,
                        category = cleanCategory,
                        note = "ثبت‌شده از پیامک ${pending.senderLabel}",
                        dateMillis = pending.receivedAt
                    )
                )
                pendingBankSmsDao.updateStatus(pendingId, PendingSmsStatus.RECORDED)
            }
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // --- Backup / restore ---

    suspend fun createBackup(): BackupData = database.withTransaction {
        val categories = categoryDao.getAllCategoriesOnce()
        val transactions = transactionDao.getAllTransactionsOnce()
        val quickAddTemplates = quickAddTemplateDao.getAllOnce()
        BackupData(
            version = BACKUP_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            categories = categories,
            transactions = transactions,
            quickAddTemplates = quickAddTemplates
        )
    }

    suspend fun createBackupJson(): String = BackupManager.encode(createBackup())

    /**
     * Replaces all local categories and transactions with the backup contents.
     * Runs inside a single Room transaction so a failure leaves data unchanged.
     */
    suspend fun restoreFromJson(json: String): Result<BackupSummary> {
        val data = BackupManager.decode(json).getOrElse { return Result.failure(it) }
        return restoreFromBackup(data)
    }

    suspend fun restoreFromBackup(data: BackupData): Result<BackupSummary> {
        val normalized = BackupManager.normalizeForRestore(data)
        return try {
            database.withTransaction {
                transactionDao.deleteAll()
                quickAddTemplateDao.deleteAll()
                categoryDao.deleteAll()

                val categoriesToInsert = normalized.categories.map { it.copy(id = 0) }
                if (categoriesToInsert.isNotEmpty()) {
                    categoryDao.insertAll(categoriesToInsert)
                } else {
                    // Keep the app usable if a backup has no categories.
                    seedFallbackCategories()
                }

                val transactionsToInsert = normalized.transactions.map { it.copy(id = 0) }
                if (transactionsToInsert.isNotEmpty()) {
                    transactionDao.insertAll(transactionsToInsert)
                }

                val templatesToInsert = normalized.quickAddTemplates.map { it.copy(id = 0) }
                if (templatesToInsert.isNotEmpty()) {
                    quickAddTemplateDao.insertAll(templatesToInsert)
                }
            }
            Result.success(BackupManager.summaryOf(normalized))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Restore failed: ${e.message}", e))
        }
    }

    suspend fun currentCounts(): Pair<Int, Int> =
        categoryDao.count() to transactionDao.count()

    private suspend fun seedFallbackCategories() {
        val income = DefaultCategories.income.mapIndexed { index, name ->
            Category(name = name, type = TransactionType.INCOME, sortOrder = index)
        }
        val expense = DefaultCategories.expense.mapIndexed { index, name ->
            Category(name = name, type = TransactionType.EXPENSE, sortOrder = index)
        }
        categoryDao.insertAll(income + expense)
    }
}
