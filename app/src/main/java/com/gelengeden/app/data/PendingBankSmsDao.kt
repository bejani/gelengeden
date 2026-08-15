package com.gelengeden.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingBankSmsDao {

    @Query(
        """
        SELECT * FROM pending_bank_sms
        WHERE status = :status
        ORDER BY receivedAt DESC
        """
    )
    fun getByStatus(status: PendingSmsStatus): Flow<List<PendingBankSms>>

    @Query(
        """
        SELECT * FROM pending_bank_sms
        WHERE status = :status
        ORDER BY receivedAt DESC
        """
    )
    suspend fun getByStatusOnce(status: PendingSmsStatus): List<PendingBankSms>

    @Query("SELECT * FROM pending_bank_sms WHERE id = :id")
    suspend fun getById(id: Long): PendingBankSms?

    @Query("SELECT COUNT(*) FROM pending_bank_sms WHERE status = :status")
    fun countByStatus(status: PendingSmsStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_bank_sms WHERE fingerprint = :fingerprint")
    suspend fun countByFingerprint(fingerprint: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sms: PendingBankSms): Long

    @Query("UPDATE pending_bank_sms SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: PendingSmsStatus)

    @Query("DELETE FROM pending_bank_sms WHERE id = :id")
    suspend fun deleteById(id: Long)
}
