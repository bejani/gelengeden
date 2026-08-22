package com.gelengeden.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Current on-disk backup schema version. */
const val BACKUP_SCHEMA_VERSION = 2

const val BACKUP_APP_ID = "gelengeden"

data class BackupData(
    val version: Int,
    val exportedAt: Long,
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val quickAddTemplates: List<QuickAddTemplate> = emptyList()
)

data class BackupSummary(
    val categoryCount: Int,
    val transactionCount: Int,
    val exportedAt: Long,
    val quickAddTemplateCount: Int = 0
)

object BackupManager {

    fun encode(data: BackupData): String {
        val root = JSONObject()
            .put("version", data.version)
            .put("app", BACKUP_APP_ID)
            .put("exportedAt", data.exportedAt)
            .put("categories", encodeCategories(data.categories))
            .put("transactions", encodeTransactions(data.transactions))
            .put("quickAddTemplates", encodeTemplates(data.quickAddTemplates))
        return root.toString(2)
    }

    private fun encodeCategories(categories: List<Category>): JSONArray {
        val result = JSONArray()
        for (category in categories) {
            result.put(
                JSONObject()
                    .put("name", category.name)
                    .put("type", category.type.name)
                    .put("sortOrder", category.sortOrder)
            )
        }
        return result
    }

    private fun encodeTransactions(transactions: List<Transaction>): JSONArray {
        val result = JSONArray()
        for (transaction in transactions) {
            result.put(
                JSONObject()
                    .put("title", transaction.title)
                    .put("amount", transaction.amount)
                    .put("type", transaction.type.name)
                    .put("category", transaction.category)
                    .put("note", transaction.note)
                    .put("dateMillis", transaction.dateMillis)
            )
        }
        return result
    }

    private fun encodeTemplates(templates: List<QuickAddTemplate>): JSONArray {
        val result = JSONArray()
        for (template in templates) {
            result.put(
                JSONObject()
                    .put("title", template.title)
                    .put("type", template.type.name)
                    .put("category", template.category)
                    .put("note", template.note)
                    .put("sortOrder", template.sortOrder)
                    .put("isEnabled", template.isEnabled)
                    .put("createdAt", template.createdAt)
            )
        }
        return result
    }

    fun decode(json: String): Result<BackupData> = try {
        val root = JSONObject(json.trim())
        validateRoot(root)
        val version = root.optInt("version", 0)
        Result.success(
            BackupData(
                version = version,
                exportedAt = root.optLong("exportedAt", 0L),
                categories = decodeCategories(root.optJSONArray("categories")),
                transactions = decodeTransactions(root.optJSONArray("transactions")),
                quickAddTemplates = decodeTemplates(root.optJSONArray("quickAddTemplates"))
            )
        )
    } catch (error: Exception) {
        Result.failure(
            IllegalArgumentException("Could not read backup file: ${error.message}", error)
        )
    }

    private fun validateRoot(root: JSONObject) {
        if (root.optString("app", "").let { it.isNotEmpty() && it != BACKUP_APP_ID }) {
            throw IllegalArgumentException("This file is not a Gelengeden backup")
        }
        val version = root.optInt("version", 0)
        if (version !in 1..BACKUP_SCHEMA_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version")
        }
        if (!root.has("categories") && !root.has("transactions")) {
            throw IllegalArgumentException("Backup is missing data sections")
        }
    }

    private fun decodeCategories(json: JSONArray?): List<Category> {
        val result = ArrayList<Category>()
        val seen = HashSet<String>()
        if (json == null) return result
        for (index in 0 until json.length()) {
            val item = json.getJSONObject(index)
            val type = parseType(item.getString("type"))
            val name = item.getString("name").trim()
            if (name.isEmpty()) continue
            val key = "${type.name}|${name.lowercase()}"
            if (!seen.add(key)) continue
            result.add(Category(0, name, type, item.optInt("sortOrder", result.size)))
        }
        return result
    }

    private fun decodeTransactions(json: JSONArray?): List<Transaction> {
        val result = ArrayList<Transaction>()
        if (json == null) return result
        for (index in 0 until json.length()) {
            val item = json.getJSONObject(index)
            val type = parseType(item.getString("type"))
            val amount = item.getDouble("amount")
            if (!amount.isFinite() || amount < 0) {
                throw IllegalArgumentException("Invalid amount in backup")
            }
            val category = item.optString("category", "").trim()
            result.add(
                Transaction(
                    id = 0,
                    title = item.optString("title", "").ifBlank { "—" },
                    amount = amount,
                    type = type,
                    category = category.ifBlank { fallbackCategoryName(type) },
                    note = item.optString("note", ""),
                    dateMillis = item.optLong("dateMillis", 0L).takeIf { it > 0 }
                        ?: System.currentTimeMillis()
                )
            )
        }
        return result
    }

    private fun decodeTemplates(json: JSONArray?): List<QuickAddTemplate> {
        val result = ArrayList<QuickAddTemplate>()
        if (json == null) return result
        for (index in 0 until json.length()) {
            val item = json.getJSONObject(index)
            val type = parseType(item.getString("type"))
            val title = item.optString("title", "").trim()
            if (title.isEmpty()) continue
            val category = item.optString("category", "").trim()
            result.add(
                QuickAddTemplate(
                    id = 0,
                    title = title,
                    type = type,
                    category = category.ifBlank { fallbackCategoryName(type) },
                    note = item.optString("note", ""),
                    sortOrder = item.optInt("sortOrder", result.size),
                    isEnabled = item.optBoolean("isEnabled", true),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        return result
    }

    private fun parseType(value: String): TransactionType = runCatching {
        TransactionType.valueOf(value)
    }.getOrElse {
        throw IllegalArgumentException("Invalid transaction type: $value")
    }

    /** Ensures every transaction category exists and both transaction types remain usable. */
    fun normalizeForRestore(data: BackupData): BackupData {
        val categories = data.categories.toMutableList()
        val existing = HashSet<Pair<TransactionType, String>>()
        for (category in categories) {
            existing.add(category.type to category.name.lowercase())
        }

        fun ensureCategory(name: String, type: TransactionType) {
            val key = type to name.lowercase()
            if (!existing.add(key)) return
            categories.add(Category(name = name, type = type, sortOrder = categories.count { it.type == type }))
        }

        for (transaction in data.transactions) ensureCategory(transaction.category, transaction.type)
        for (template in data.quickAddTemplates) ensureCategory(template.category, template.type)
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
        exportedAt = data.exportedAt,
        quickAddTemplateCount = data.quickAddTemplates.size
    )

    private fun fallbackCategoryName(type: TransactionType): String = when (type) {
        TransactionType.INCOME -> DefaultCategories.income.lastOrNull() ?: "سایر"
        TransactionType.EXPENSE -> DefaultCategories.expense.lastOrNull() ?: "سایر"
    }
}
