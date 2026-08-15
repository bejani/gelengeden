package com.gelengeden.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TransactionRepository(
    private val database: AppDatabase
) {
    private val transactionDao: TransactionDao = database.transactionDao()
    private val categoryDao: CategoryDao = database.categoryDao()

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

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
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }
        if (categoryDao.countByNameAndType(trimmed, type) > 0) {
            return Result.failure(IllegalArgumentException("That category already exists"))
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
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }
        if (categoryDao.countByNameAndType(trimmed, category.type, category.id) > 0) {
            return Result.failure(IllegalArgumentException("That category already exists"))
        }
        val oldName = category.name
        categoryDao.update(category.copy(name = trimmed))
        if (oldName != trimmed) {
            categoryDao.renameInTransactions(oldName, trimmed, category.type)
        }
        return Result.success(Unit)
    }

    suspend fun deleteCategory(category: Category): Result<Unit> {
        if (categoryDao.countByType(category.type) <= 1) {
            return Result.failure(
                IllegalArgumentException(
                    "Keep at least one ${category.type.name.lowercase()} category"
                )
            )
        }
        val fallback = categoryDao.findFallbackName(category.type, category.id)
            ?: return Result.failure(
                IllegalArgumentException("Could not find a fallback category")
            )
        categoryDao.reassignTransactions(category.name, fallback, category.type)
        categoryDao.delete(category)
        return Result.success(Unit)
    }

    // --- Backup / restore ---

    suspend fun createBackup(): BackupData {
        val categories = categoryDao.getAllCategoriesOnce()
        val transactions = transactionDao.getAllTransactionsOnce()
        return BackupData(
            version = BACKUP_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            categories = categories,
            transactions = transactions
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
