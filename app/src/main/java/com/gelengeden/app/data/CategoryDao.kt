package com.gelengeden.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY type ASC, sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY type ASC, sortOrder ASC, name ASC")
    suspend fun getAllCategoriesOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC, name ASC")
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT COUNT(*) FROM categories WHERE type = :type")
    suspend fun countByType(type: TransactionType): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM categories WHERE type = :type AND LOWER(name) = LOWER(:name) AND id != :excludeId")
    suspend fun countByNameAndType(name: String, type: TransactionType, excludeId: Long = 0): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName AND type = :type")
    suspend fun renameInTransactions(oldName: String, newName: String, type: TransactionType)

    @Query("UPDATE transactions SET category = :fallbackName WHERE category = :deletedName AND type = :type")
    suspend fun reassignTransactions(deletedName: String, fallbackName: String, type: TransactionType)

    @Query("SELECT COUNT(*) FROM transactions WHERE category = :name AND type = :type")
    suspend fun countTransactionsUsing(name: String, type: TransactionType): Int

    /** Prefer "Other", otherwise the first category of the same type that is not [excludeId]. */
    @Query(
        """
        SELECT name FROM categories
        WHERE type = :type AND id != :excludeId
        ORDER BY CASE WHEN LOWER(name) = 'other' THEN 0 ELSE 1 END, sortOrder ASC, name ASC
        LIMIT 1
        """
    )
    suspend fun findFallbackName(type: TransactionType, excludeId: Long): String?
}

