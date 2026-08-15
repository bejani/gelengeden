package com.gelengeden.app.ui.report

import com.gelengeden.app.data.Transaction
import com.gelengeden.app.data.TransactionType
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class ReportPeriodPreset {
    THIS_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    THIS_YEAR,
    CUSTOM
}

enum class ReportTypeFilter {
    ALL, INCOME, EXPENSE
}

data class DateRange(
    val startMillis: Long,
    val endMillis: Long
) {
    val dayCount: Int
        get() {
            val span = (endMillis - startMillis).coerceAtLeast(0L)
            return (TimeUnit.MILLISECONDS.toDays(span) + 1).toInt().coerceAtLeast(1)
        }
}

data class CategoryTotal(
    val name: String,
    val amount: Double,
    val type: TransactionType,
    val percent: Float = 0f,
    val transactionCount: Int = 0
)

data class MonthlyPoint(
    val year: Int,
    val month: Int, // 0-11 (Calendar)
    val label: String,
    val shortLabel: String,
    val income: Double,
    val expense: Double
) {
    val balance: Double get() = income - expense
    val hasData: Boolean get() = income > 0.0 || expense > 0.0
}

data class ReportInsight(
    val label: String,
    val value: String,
    val detail: String = ""
)

data class ReportUiState(
    val preset: ReportPeriodPreset = ReportPeriodPreset.THIS_MONTH,
    val typeFilter: ReportTypeFilter = ReportTypeFilter.ALL,
    val range: DateRange = currentMonthRange(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val transactionCount: Int = 0,
    val categoryBreakdown: List<CategoryTotal> = emptyList(),
    val monthlySeries: List<MonthlyPoint> = emptyList(),
    val chartMonths: Int = 6,
    val avgDailyIncome: Double = 0.0,
    val avgDailyExpense: Double = 0.0,
    val savingsRate: Double = 0.0, // 0..1 when income > 0
    val topCategory: CategoryTotal? = null,
    val busiestMonthLabel: String = "",
    val insights: List<ReportInsight> = emptyList()
)

fun currentMonthRange(now: Calendar = Calendar.getInstance()): DateRange {
    val start = now.clone() as Calendar
    start.set(Calendar.DAY_OF_MONTH, 1)
    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)

    val end = start.clone() as Calendar
    end.add(Calendar.MONTH, 1)
    end.add(Calendar.MILLISECOND, -1)
    return DateRange(start.timeInMillis, end.timeInMillis)
}

fun lastMonthRange(now: Calendar = Calendar.getInstance()): DateRange {
    val start = now.clone() as Calendar
    start.set(Calendar.DAY_OF_MONTH, 1)
    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)
    start.add(Calendar.MONTH, -1)

    val end = start.clone() as Calendar
    end.add(Calendar.MONTH, 1)
    end.add(Calendar.MILLISECOND, -1)
    return DateRange(start.timeInMillis, end.timeInMillis)
}

fun lastNMonthsRange(months: Int, now: Calendar = Calendar.getInstance()): DateRange {
    val end = now.clone() as Calendar
    end.set(Calendar.HOUR_OF_DAY, 23)
    end.set(Calendar.MINUTE, 59)
    end.set(Calendar.SECOND, 59)
    end.set(Calendar.MILLISECOND, 999)

    val start = now.clone() as Calendar
    start.set(Calendar.DAY_OF_MONTH, 1)
    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)
    start.add(Calendar.MONTH, -(months - 1))
    return DateRange(start.timeInMillis, end.timeInMillis)
}

fun thisYearRange(now: Calendar = Calendar.getInstance()): DateRange {
    val start = now.clone() as Calendar
    start.set(Calendar.MONTH, Calendar.JANUARY)
    start.set(Calendar.DAY_OF_MONTH, 1)
    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)

    val end = now.clone() as Calendar
    end.set(Calendar.HOUR_OF_DAY, 23)
    end.set(Calendar.MINUTE, 59)
    end.set(Calendar.SECOND, 59)
    end.set(Calendar.MILLISECOND, 999)
    return DateRange(start.timeInMillis, end.timeInMillis)
}

fun rangeForPreset(preset: ReportPeriodPreset, custom: DateRange? = null): DateRange {
    return when (preset) {
        ReportPeriodPreset.THIS_MONTH -> currentMonthRange()
        ReportPeriodPreset.LAST_MONTH -> lastMonthRange()
        ReportPeriodPreset.LAST_3_MONTHS -> lastNMonthsRange(3)
        ReportPeriodPreset.LAST_6_MONTHS -> lastNMonthsRange(6)
        ReportPeriodPreset.THIS_YEAR -> thisYearRange()
        ReportPeriodPreset.CUSTOM -> custom ?: currentMonthRange()
    }
}

/** Gregorian month range (year + zero-based month). Kept for any legacy callers. */
fun monthRange(year: Int, month: Int): DateRange {
    val start = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = (start.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
        add(Calendar.MILLISECOND, -1)
    }
    return DateRange(start.timeInMillis, end.timeInMillis)
}

/** Solar Hijri (Shamsi) month range. [month] is zero-based (0 = Farvardin). */
fun persianMonthRange(year: Int, month: Int): DateRange {
    val start = persianMonthStartMillis(year, month)
    val nextMonth = if (month == 11) 0 else month + 1
    val nextYear = if (month == 11) year + 1 else year
    val end = persianMonthStartMillis(nextYear, nextMonth) - 1
    return DateRange(start, end)
}

/** A date in the Solar Hijri (Persian/Jalali) calendar. Months are zero based. */
private data class PersianDate(val year: Int, val month: Int, val day: Int)

private val persianMonthNames = arrayOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

private val persianMonthShortNames = arrayOf(
    "فرو", "ارد", "خرد", "تیر", "مرد", "شهر",
    "مهر", "آبا", "آذر", "دی", "بهم", "اسف"
)

/** Converts a Gregorian date to its Solar Hijri equivalent. */
private fun gregorianToPersian(year: Int, month: Int, day: Int): PersianDate {
    val gregorianDaysBeforeMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val adjustedYear = if (month > 2) year + 1 else year
    var days = 355_666 + 365 * year + (adjustedYear + 3) / 4 -
        (adjustedYear + 99) / 100 + (adjustedYear + 399) / 400 + day + gregorianDaysBeforeMonth[month - 1]
    var jalaliYear = -1595 + 33 * (days / 12_053)
    days %= 12_053
    jalaliYear += 4 * (days / 1_461)
    days %= 1_461
    if (days > 365) {
        jalaliYear += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jalaliMonth = if (days < 186) days / 31 else (days - 186) / 30 + 6
    val jalaliDay = if (days < 186) days % 31 + 1 else (days - 186) % 30 + 1
    return PersianDate(jalaliYear, jalaliMonth, jalaliDay)
}

/** Converts a Solar Hijri date to its Gregorian equivalent. */
private fun persianToGregorian(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
    val jalaliYear = year + 1595
    var days = -355_668 + 365 * jalaliYear + (jalaliYear / 33) * 8 +
        ((jalaliYear % 33 + 3) / 4) + day + if (month < 7) month * 31 else 186 + (month - 6) * 30
    var gregorianYear = 400 * (days / 146_097)
    days %= 146_097
    if (days > 36_524) {
        days -= 1
        gregorianYear += 100 * (days / 36_524)
        if (days >= 365) days++
    }
    gregorianYear += 4 * (days / 1_461)
    days %= 1_461
    if (days > 365) {
        gregorianYear += (days - 1) / 365
        days = (days - 1) % 365
    }
    var gregorianDay = days + 1
    val monthLengths = intArrayOf(31, if (isGregorianLeapYear(gregorianYear)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var gregorianMonth = 0
    while (gregorianDay > monthLengths[gregorianMonth]) {
        gregorianDay -= monthLengths[gregorianMonth]
        gregorianMonth++
    }
    return Triple(gregorianYear, gregorianMonth, gregorianDay)
}

private fun isGregorianLeapYear(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun persianMonthStartMillis(year: Int, month: Int): Long {
    val (gregorianYear, gregorianMonth, gregorianDay) = persianToGregorian(year, month, 1)
    return Calendar.getInstance().apply {
        set(gregorianYear, gregorianMonth, gregorianDay, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun buildReport(
    transactions: List<Transaction>,
    range: DateRange,
    typeFilter: ReportTypeFilter,
    chartMonths: Int,
    preset: ReportPeriodPreset = ReportPeriodPreset.THIS_MONTH
): ReportUiState {
    val inRange = transactions.filter {
        it.dateMillis in range.startMillis..range.endMillis
    }
    val filtered = when (typeFilter) {
        ReportTypeFilter.ALL -> inRange
        ReportTypeFilter.INCOME -> inRange.filter { it.type == TransactionType.INCOME }
        ReportTypeFilter.EXPENSE -> inRange.filter { it.type == TransactionType.EXPENSE }
    }

    val income = filtered
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }
    val expense = filtered
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }

    val days = range.dayCount.toDouble()
    val avgDailyIncome = income / days
    val avgDailyExpense = expense / days
    val savingsRate = if (income > 0.0) ((income - expense) / income).coerceIn(-1.0, 1.0) else 0.0

    val totalForPercent = when (typeFilter) {
        ReportTypeFilter.ALL -> max(income + expense, 0.0)
        ReportTypeFilter.INCOME -> max(income, 0.0)
        ReportTypeFilter.EXPENSE -> max(expense, 0.0)
    }.coerceAtLeast(1e-9)

    val breakdown = filtered
        .groupBy { it.category to it.type }
        .map { (key, list) ->
            val amount = list.sumOf { it.amount }
            CategoryTotal(
                name = key.first,
                amount = amount,
                type = key.second,
                percent = (amount / totalForPercent).toFloat().coerceIn(0f, 1f),
                transactionCount = list.size
            )
        }
        .sortedByDescending { it.amount }

    val monthlySeries = buildMonthlySeries(
        transactions = transactions,
        months = chartMonths,
        typeFilter = typeFilter
    )

    val busiest = monthlySeries.maxByOrNull { it.income + it.expense }
    val busiestLabel = if (busiest != null && busiest.hasData) busiest.label else "—"

    val top = breakdown.firstOrNull()

    val insights = buildInsights(
        typeFilter = typeFilter,
        income = income,
        expense = expense,
        avgDailyIncome = avgDailyIncome,
        avgDailyExpense = avgDailyExpense,
        savingsRate = savingsRate,
        transactionCount = filtered.size,
        topCategory = top,
        busiestMonthLabel = busiestLabel
    )

    return ReportUiState(
        preset = preset,
        typeFilter = typeFilter,
        range = range,
        totalIncome = income,
        totalExpense = expense,
        balance = income - expense,
        transactionCount = filtered.size,
        categoryBreakdown = breakdown,
        monthlySeries = monthlySeries,
        chartMonths = chartMonths,
        avgDailyIncome = avgDailyIncome,
        avgDailyExpense = avgDailyExpense,
        savingsRate = savingsRate,
        topCategory = top,
        busiestMonthLabel = busiestLabel,
        insights = insights
    )
}

private fun buildInsights(
    typeFilter: ReportTypeFilter,
    income: Double,
    expense: Double,
    avgDailyIncome: Double,
    avgDailyExpense: Double,
    savingsRate: Double,
    transactionCount: Int,
    topCategory: CategoryTotal?,
    busiestMonthLabel: String
): List<ReportInsight> {
    val list = mutableListOf<ReportInsight>()

    when (typeFilter) {
        ReportTypeFilter.ALL -> {
            list += ReportInsight(
                label = "Savings rate",
                value = if (income > 0) String.format("%.0f%%", savingsRate * 100) else "—",
                detail = if (income > 0) "of income kept" else "No income in period"
            )
            list += ReportInsight(
                label = "Avg / day",
                value = formatShort(avgDailyExpense),
                detail = "daily spending"
            )
        }
        ReportTypeFilter.INCOME -> {
            list += ReportInsight(
                label = "Avg / day",
                value = formatShort(avgDailyIncome),
                detail = "daily income"
            )
            list += ReportInsight(
                label = "Total",
                value = formatShort(income),
                detail = "$transactionCount entries"
            )
        }
        ReportTypeFilter.EXPENSE -> {
            list += ReportInsight(
                label = "Avg / day",
                value = formatShort(avgDailyExpense),
                detail = "daily spending"
            )
            list += ReportInsight(
                label = "Total",
                value = formatShort(expense),
                detail = "$transactionCount entries"
            )
        }
    }

    if (topCategory != null) {
        list += ReportInsight(
            label = "Top category",
            value = topCategory.name,
            detail = formatShort(topCategory.amount)
        )
    }

    list += ReportInsight(
        label = "Busiest month",
        value = busiestMonthLabel,
        detail = "in chart window"
    )

    return list
}

private fun formatShort(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        abs >= 1_000_000 -> String.format("%s%.1fM", sign, abs / 1_000_000.0)
        abs >= 1_000 -> String.format("%s%.1fK", sign, abs / 1_000.0)
        else -> String.format("%s%.0f", sign, abs)
    }
}

fun buildMonthlySeries(
    transactions: List<Transaction>,
    months: Int,
    typeFilter: ReportTypeFilter = ReportTypeFilter.ALL,
    now: Calendar = Calendar.getInstance()
): List<MonthlyPoint> {
    val points = mutableListOf<MonthlyPoint>()
    val currentPersianDate = gregorianToPersian(
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH) + 1,
        now.get(Calendar.DAY_OF_MONTH)
    )
    var cursorYear = currentPersianDate.year
    var cursorMonth = currentPersianDate.month - (months - 1)
    while (cursorMonth < 0) {
        cursorMonth += 12
        cursorYear--
    }

    repeat(months) {
        val start = persianMonthStartMillis(cursorYear, cursorMonth)
        val nextMonth = if (cursorMonth == 11) 0 else cursorMonth + 1
        val nextYear = if (cursorMonth == 11) cursorYear + 1 else cursorYear
        val end = persianMonthStartMillis(nextYear, nextMonth) - 1

        val monthTx = transactions.filter { it.dateMillis in start..end }
        val rawIncome = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val rawExpense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // Respect type filter so charts match the selected view
        val income = when (typeFilter) {
            ReportTypeFilter.ALL, ReportTypeFilter.INCOME -> rawIncome
            ReportTypeFilter.EXPENSE -> 0.0
        }
        val expense = when (typeFilter) {
            ReportTypeFilter.ALL, ReportTypeFilter.EXPENSE -> rawExpense
            ReportTypeFilter.INCOME -> 0.0
        }

        val shortLabel = persianMonthShortNames[cursorMonth]
        val label = if (months > 6) {
            "$shortLabel ${toPersianDigits(cursorYear % 100)}"
        } else {
            persianMonthNames[cursorMonth]
        }

        points += MonthlyPoint(
            year = cursorYear,
            month = cursorMonth,
            label = label,
            shortLabel = shortLabel,
            income = income,
            expense = expense
        )
        cursorMonth = nextMonth
        cursorYear = nextYear
    }
    return points
}

private fun toPersianDigits(value: Int): String = value.toString().map { digit ->
    if (digit in '0'..'9') ('۰'.code + digit.code - '0'.code).toChar() else digit
}.joinToString("")
