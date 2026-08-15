package com.gelengeden.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BankSenderDao {

    @Query("SELECT * FROM bank_senders ORDER BY label COLLATE NOCASE ASC")
    fun getAll(): Flow<List<BankSender>>

    @Query("SELECT * FROM bank_senders ORDER BY label COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<BankSender>

    @Query("SELECT * FROM bank_senders WHERE id = :id")
    suspend fun getById(id: Long): BankSender?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(sender: BankSender): Long

    @Update
    suspend fun update(sender: BankSender)

    @Delete
    suspend fun delete(sender: BankSender)

    @Query("DELETE FROM bank_senders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM bank_senders")
    suspend fun count(): Int
}
