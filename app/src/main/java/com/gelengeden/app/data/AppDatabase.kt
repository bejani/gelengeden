package com.gelengeden.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, Category::class, BankSender::class, PendingBankSms::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bankSenderDao(): BankSenderDao
    abstract fun pendingBankSmsDao(): PendingBankSmsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name_type ON categories(name, type)"
                )
                seedDefaultCategories(db)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bank_senders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        address TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_bank_senders_address ON bank_senders(address)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_bank_sms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        senderAddress TEXT NOT NULL,
                        senderLabel TEXT NOT NULL,
                        body TEXT NOT NULL,
                        receivedAt INTEGER NOT NULL,
                        rawAmount REAL,
                        amountWasRial INTEGER NOT NULL,
                        suggestedTitle TEXT NOT NULL,
                        suggestedType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        fingerprint TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_pending_bank_sms_fingerprint ON pending_bank_sms(fingerprint)"
                )
            }
        }

        private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
            DefaultCategories.income.forEachIndexed { index, name ->
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (name, type, sortOrder) VALUES (?, ?, ?)",
                    arrayOf(name, TransactionType.INCOME.name, index)
                )
            }
            DefaultCategories.expense.forEachIndexed { index, name ->
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (name, type, sortOrder) VALUES (?, ?, ?)",
                    arrayOf(name, TransactionType.EXPENSE.name, index)
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gelengeden.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed defaults on brand-new installs (v2+)
                            seedDefaultCategories(db)
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
