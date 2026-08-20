package com.gelengeden.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Transaction::class,
        Category::class,
        BankSender::class,
        PendingBankSms::class,
        QuickAddTemplate::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bankSenderDao(): BankSenderDao
    abstract fun pendingBankSmsDao(): PendingBankSmsDao
    abstract fun quickAddTemplateDao(): QuickAddTemplateDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE bank_senders ADD COLUMN amountWasRial INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quick_add_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        note TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_quick_add_templates_sortOrder " +
                        "ON quick_add_templates(sortOrder)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                seedDefaultBankSenders(db)
            }
        }

        private fun seedDefaultBankSenders(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO bank_senders (label, address, amountWasRial) VALUES (?, ?, ?)",
                arrayOf("بانک ملی", "+98700717", 0)
            )
            db.execSQL(
                "INSERT OR IGNORE INTO bank_senders (label, address, amountWasRial) VALUES (?, ?, ?)",
                arrayOf("بانک رسالت", "ResalatBank", 0)
            )
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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed defaults on brand-new installs (v2+)
                            seedDefaultCategories(db)
                            seedDefaultBankSenders(db)
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
