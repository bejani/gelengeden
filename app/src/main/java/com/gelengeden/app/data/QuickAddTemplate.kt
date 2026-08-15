package com.gelengeden.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A reusable transaction preset for fast manual recording.
 * The amount is intentionally not stored because it can vary on every use.
 */
@Entity(
    tableName = "quick_add_templates",
    indices = [Index(value = ["sortOrder"])]
)
data class QuickAddTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: TransactionType,
    val category: String,
    val note: String = "",
    val sortOrder: Int = 0,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
