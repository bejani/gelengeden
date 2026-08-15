package com.gelengeden.app.data

/** Built-in Persian category names, used to seed the database on first run. */
object DefaultCategories {
    val income = listOf(
        "حقوق", "فریلنسری", "سرمایه‌گذاری", "هدیه", "بازگشت وجه", "سایر"
    )

    val expense = listOf(
        "خوراک", "حمل‌ونقل", "اجاره", "قبوض", "خرید", "سلامت", "تفریح", "آموزش", "سایر"
    )

    /** English names from older app versions — used for one-time rename migration. */
    val legacyIncomeEnglish = listOf("Salary", "Freelance", "Investment", "Gift", "Refund", "Other")
    val legacyExpenseEnglish = listOf(
        "Food", "Transport", "Rent", "Bills", "Shopping", "Health", "Entertainment", "Education", "Other"
    )

    fun seedList(): List<Category> =
        income.mapIndexed { index, name ->
            Category(name = name, type = TransactionType.INCOME, sortOrder = index)
        } + expense.mapIndexed { index, name ->
            Category(name = name, type = TransactionType.EXPENSE, sortOrder = index)
        }
}
