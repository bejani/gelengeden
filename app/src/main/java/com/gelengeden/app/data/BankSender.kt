package com.gelengeden.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_senders",
    indices = [Index(value = ["address"], unique = true)]
)
data class BankSender(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val address: String
)
