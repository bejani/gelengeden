package com.gelengeden.app.data

/**
 * Builds spreadsheet exports of the transaction list (CSV and real .xlsx).
 */
object TransactionExporter {

    data class Labels(
        val date: String,
        val type: String,
        val title: String,
        val category: String,
        val amount: String,
        val note: String,
        val income: String,
        val expense: String,
        val sheetName: String = "Transactions",
        val summarySheet: String = "Summary",
        val incomeSheet: String = "Income",
        val expenseSheet: String = "Expense",
        val countLabel: String = "Count",
        val totalIncomeLabel: String = "Total income",
        val totalExpenseLabel: String = "Total expense",
        val balanceLabel: String = "Balance",
        val incomeByCategory: String = "Income by category",
        val expenseByCategory: String = "Expense by category"
    )

    fun toCsv(
        transactions: List<Transaction>,
        labels: Labels,
        formatDate: (Long) -> String
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM so Excel on Windows recognizes Persian/Unicode correctly
        sb.append('\uFEFF')
        sb.append(
            listOf(
                labels.date,
                labels.type,
                labels.title,
                labels.category,
                labels.amount,
                labels.note
            ).joinToString(",") { csvEscape(it) }
        )
        sb.append('\n')
        transactions.forEach { tx ->
            sb.append(
                listOf(
                    formatDate(tx.dateMillis),
                    typeLabel(tx.type, labels),
                    tx.title,
                    tx.category,
                    formatAmountPlain(tx.amount),
                    tx.note
                ).joinToString(",") { csvEscape(it) }
            )
            sb.append('\n')
        }
        return sb.toString()
    }

    /**
     * Real Office Open XML workbook (.xlsx) with summary, all, income, and expense sheets.
     */
    fun toXlsx(
        transactions: List<Transaction>,
        labels: Labels,
        formatDate: (Long) -> String,
        rightToLeft: Boolean = false
    ): ByteArray {
        val income = transactions.filter { it.type == TransactionType.INCOME }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalIncome = income.sumOf { it.amount }
        val totalExpense = expense.sumOf { it.amount }

        val workbook = XlsxWorkbook(rightToLeft = rightToLeft)
        val summaryCols = listOf(
            XlsxWorkbook.Column(1, 28.0),
            XlsxWorkbook.Column(2, 18.0)
        )
        val tableCols = listOf(
            XlsxWorkbook.Column(1, 16.0),
            XlsxWorkbook.Column(2, 12.0),
            XlsxWorkbook.Column(3, 28.0),
            XlsxWorkbook.Column(4, 18.0),
            XlsxWorkbook.Column(5, 16.0),
            XlsxWorkbook.Column(6, 28.0)
        )

        val summary = workbook.addSheet(labels.summarySheet, summaryCols, autoFilter = false)
        summary.addHeader(labels.summarySheet, labels.amount)
        summary.addRow(
            XlsxWorkbook.Cell.Text(labels.countLabel),
            XlsxWorkbook.Cell.Number(transactions.size.toDouble(), XlsxWorkbook.STYLE_TOTAL)
        )
        summary.addRow(
            XlsxWorkbook.Cell.Text(labels.totalIncomeLabel),
            XlsxWorkbook.Cell.Number(totalIncome, XlsxWorkbook.STYLE_TOTAL)
        )
        summary.addRow(
            XlsxWorkbook.Cell.Text(labels.totalExpenseLabel),
            XlsxWorkbook.Cell.Number(totalExpense, XlsxWorkbook.STYLE_TOTAL)
        )
        summary.addRow(
            XlsxWorkbook.Cell.Text(labels.balanceLabel),
            XlsxWorkbook.Cell.Number(totalIncome - totalExpense, XlsxWorkbook.STYLE_TOTAL)
        )
        writeCategorySection(summary, labels.incomeByCategory, labels, income)
        writeCategorySection(summary, labels.expenseByCategory, labels, expense)

        writeTransactionSheet(
            sheet = workbook.addSheet(labels.sheetName, tableCols, autoFilter = true),
            transactions = transactions,
            labels = labels,
            formatDate = formatDate
        )
        writeTransactionSheet(
            sheet = workbook.addSheet(labels.incomeSheet, tableCols, autoFilter = true),
            transactions = income,
            labels = labels,
            formatDate = formatDate
        )
        writeTransactionSheet(
            sheet = workbook.addSheet(labels.expenseSheet, tableCols, autoFilter = true),
            transactions = expense,
            labels = labels,
            formatDate = formatDate
        )

        return workbook.toByteArray()
    }

    private fun writeCategorySection(
        sheet: XlsxWorkbook.Sheet,
        title: String,
        labels: Labels,
        transactions: List<Transaction>
    ) {
        sheet.addBlankRow()
        sheet.addHeader(title, labels.amount)
        val totals = transactions
            .groupBy { it.category.ifBlank { "—" } }
            .map { (category, items) -> category to items.sumOf { it.amount } }
            .sortedByDescending { it.second }
        if (totals.isEmpty()) {
            sheet.addRow(
                XlsxWorkbook.Cell.Text("—"),
                XlsxWorkbook.Cell.Number(0.0)
            )
        } else {
            totals.forEach { (category, amount) ->
                sheet.addRow(
                    XlsxWorkbook.Cell.Text(category),
                    XlsxWorkbook.Cell.Number(amount)
                )
            }
        }
    }

    private fun writeTransactionSheet(
        sheet: XlsxWorkbook.Sheet,
        transactions: List<Transaction>,
        labels: Labels,
        formatDate: (Long) -> String
    ) {
        sheet.addHeader(
            labels.date,
            labels.type,
            labels.title,
            labels.category,
            labels.amount,
            labels.note
        )
        transactions.forEach { tx ->
            sheet.addRow(
                XlsxWorkbook.Cell.Text(formatDate(tx.dateMillis)),
                XlsxWorkbook.Cell.Text(typeLabel(tx.type, labels)),
                XlsxWorkbook.Cell.Text(tx.title),
                XlsxWorkbook.Cell.Text(tx.category),
                XlsxWorkbook.Cell.Number(tx.amount),
                XlsxWorkbook.Cell.Text(tx.note)
            )
        }
    }

    private fun typeLabel(type: TransactionType, labels: Labels): String =
        when (type) {
            TransactionType.INCOME -> labels.income
            TransactionType.EXPENSE -> labels.expense
        }

    /** Plain amount without grouping so spreadsheets parse it as a number. */
    private fun formatAmountPlain(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            amount.toString()
        }
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
