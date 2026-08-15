package com.gelengeden.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Current on-disk backup schema version. */
const val BACKUP_SCHEMA_VERSION = 1

const val BACKUP_APP_ID = "gelengeden"

data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val categories: List<Category>,
    val transactions: List<Transaction>
)

data class BackupSummary(
    val categoryCount: Int,
    val transactionCount: Int,
    val exportedAt: Long
)

object BackupManager {

    fun encode(data: BackupData): String {
        val root = JSONObject()
        root.put("version", data.version)
        root.put("app", BACKUP_APP_ID)
        root.put("exportedAt", data.exportedAt)

        val categoriesJson = JSONArray()
        data.categories.forEach { category ->
            categoriesJson.put(
                JSONObject().apply {
                    put("name", category.name)
                    put("type", category.type.name)
                    put("sortOrder", category.sortOrder)
                }
            )
        }
        root.put("categories", categoriesJson)

        val transactionsJson = JSONArray()
        data.transactions.forEach { tx ->
            transactionsJson.put(
                JSONObject().apply {
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("type", tx.type.name)
                    put("category", tx.category)
                    put("note", tx.note)
                    put("dateMillis", tx.dateMillis)
                }
            )
        }
        root.put("transactions", transactionsJson)

        return root.toString(2)
    }

    fun decode(json: String): Result<BackupData> {
        return try {
            val trimmed = json.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("Backup file is empty"))
            }

            val root = JSONObject(trimmed)
            val app = root.optString("app", "")
            if (app.isNotEmpty() && app != BACKUP_APP_ID) {
                return Result.failure(IllegalArgumentException("This file is not a Gelengeden backup"))
            }
            val version = root.optInt("version", 0)
            if (version < 1 || version > BACKUP_SCHEMA_VERSION) {
                return Result.failure(IllegalArgumentException("Unsupported backup version: $version"))
            }
            if (!root.has("categories") && !root.has("transactions")) {
                return Result.failure(IllegalArgumentException("Backup is missing data sections"))
            }

            val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
            val seenCategoryKeys = linkedSetOf<String>()
            val categories = buildList {
                for (i in 0 until categoriesJson.length()) {
                    val obj = categoriesJson.getJSONObject(i)
                    val typeName = obj.getString("type")
                    val type = runCatching { TransactionType.valueOf(typeName) }.getOrElse {
                        return Result.failure(IllegalArgumentException("Invalid category type: $typeName"))
                    }
                    val name = obj.getString("name").trim()
                    if (name.isEmpty()) continue
                    val key = "${type.name}|${name.lowercase()}"
                    if (!seenCategoryKeys.add(key)) continue
                    add(
                        Category(
                            id = 0,
                            name = name,
                            type = type,
                            sortOrder = obj.optInt("sortOrder", size)
                        )
                    )
                }
            }

            val transactionsJson = root.optJSONArray("transactions") ?: JSONArray()
            val transactions = buildList {
                for (i in 0 until transactionsJson.length()) {
                    val obj = transactionsJson.getJSONObject(i)
                    val typeName = obj.getString("type")
                    val type = runCatching { TransactionType.valueOf(typeName) }.getOrElse {
                        return Result.failure(IllegalArgumentException("Invalid transaction type: $typeName"))
                    }
                    val amount = obj.getDouble("amount")
                    if (!amount.isFinite() || amount < 0) {
                        return Result.failure(IllegalArgumentException("Invalid amount in backup"))
                    }
                    val rawCategory = obj.optString("category", "").trim()
                    add(
                        Transaction(
                            id = 0,
                            title = obj.optString("title", "").ifBlank { "—" },
                            amount = amount,
                            type = type,
                            category = rawCategory.ifBlank { fallbackCategoryName(type) },
                            note = obj.optString("note", ""),
                            dateMillis = obj.optLong("dateMillis", 0L).takeIf { it > 0 }
                                ?: System.currentTimeMillis()
                        )
                    )
                }
            }

            Result.success(
                BackupData(
                    version = version,
                    exportedAt = root.optLong("exportedAt", 0L),
                    categories = categories,
                    transactions = transactions
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Could not read backup file: ${e.message}", e))
        }
    }

    /**
     * Ensures every transaction category exists in the category list, and that both
     * income and expense have at least one category so the app stays usable.
     */
    fun normalizeForRestore(data: BackupData): BackupData {
        val categories = data.categories.toMutableList()
        val existing = categories
            .map { it.type to it.name.lowercase() }
            .toMutableSet()

        fun ensureCategory(name: String, type: TransactionType) {
            val key = type to name.lowercase()
            if (key in existing) return
            val sortOrder = categories.count { it.type == type }
            categories.add(Category(name = name, type = type, sortOrder = sortOrder))
            existing.add(key)
        }

        data.transactions.forEach { tx ->
            ensureCategory(tx.category, tx.type)
        }

        if (categories.none { it.type == TransactionType.INCOME }) {
            ensureCategory(fallbackCategoryName(TransactionType.INCOME), TransactionType.INCOME)
        }
        if (categories.none { it.type == TransactionType.EXPENSE }) {
            ensureCategory(fallbackCategoryName(TransactionType.EXPENSE), TransactionType.EXPENSE)
        }

        return data.copy(categories = categories)
    }

    fun summaryOf(data: BackupData): BackupSummary = BackupSummary(
        categoryCount = data.categories.size,
        transactionCount = data.transactions.size,
        exportedAt = data.exportedAt
    )

    private fun fallbackCategoryName(type: TransactionType): String {
        return when (type) {
            TransactionType.INCOME -> DefaultCategories.income.lastOrNull() ?: "سایر"
            TransactionType.EXPENSE -> DefaultCategories.expense.lastOrNull() ?: "سایر"
        }
    }
}
