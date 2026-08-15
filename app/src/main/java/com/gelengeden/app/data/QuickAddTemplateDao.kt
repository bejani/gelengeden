package com.gelengeden.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickAddTemplateDao {

    @Query(
        "SELECT * FROM quick_add_templates " +
            "WHERE isEnabled = 1 ORDER BY sortOrder ASC, id ASC"
    )
    fun getEnabled(): Flow<List<QuickAddTemplate>>

    @Query("SELECT * FROM quick_add_templates ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<QuickAddTemplate>>

    @Query("SELECT * FROM quick_add_templates WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): QuickAddTemplate?

    @Query("SELECT * FROM quick_add_templates ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllOnce(): List<QuickAddTemplate>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM quick_add_templates")
    suspend fun nextSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: QuickAddTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<QuickAddTemplate>)

    @Update
    suspend fun update(template: QuickAddTemplate)

    @Delete
    suspend fun delete(template: QuickAddTemplate)

    @Query(
        "UPDATE quick_add_templates SET category = :newName " +
            "WHERE category = :oldName AND type = :type"
    )
    suspend fun renameCategory(oldName: String, newName: String, type: TransactionType)

    @Query("DELETE FROM quick_add_templates")
    suspend fun deleteAll()
}
