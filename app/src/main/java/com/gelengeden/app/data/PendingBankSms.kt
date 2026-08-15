package com.gelengeden.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PendingSmsStatus {
    PENDING,
    RECORDED,
    DISMISSED
}

@Entity(
    tableName = "pending_bank_sms",
    indices = [Index(value = ["fingerprint"], unique = true)]
)
data class PendingBankSms(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderAddress: String,
    val senderLabel: String,
    val body: String,
    val receivedAt: Long,
    /** Amount as printed in the SMS, before Rial→Toman conversion. */
    val rawAmount: Double? = null,
    val amountWasRial: Boolean = false,
    val suggestedTitle: String,
    val suggestedType: TransactionType = TransactionType.EXPENSE,
    val status: PendingSmsStatus = PendingSmsStatus.PENDING,
    val fingerprint: String
) {
    val displayAmount: Double?
        get() {
            val raw = rawAmount ?: return null
            return if (amountWasRial) raw / 10.0 else raw
        }
}
