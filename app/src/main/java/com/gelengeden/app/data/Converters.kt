package com.gelengeden.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromPendingSmsStatus(status: PendingSmsStatus): String = status.name

    @TypeConverter
    fun toPendingSmsStatus(value: String): PendingSmsStatus = PendingSmsStatus.valueOf(value)
}
