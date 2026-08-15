package com.gelengeden.app.ui.util

import java.util.Calendar

/** A Solar Hijri (Jalali) date. Month is zero-based. */
data class PersianDate(val year: Int, val month: Int, val day: Int)

val PersianMonthNames = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

fun persianDateFromMillis(millis: Long): PersianDate {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    return gregorianToPersian(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

fun persianDateToMillis(date: PersianDate): Long {
    val (year, month, day) = persianToGregorian(date.year, date.month, date.day)
    return Calendar.getInstance().apply {
        set(year, month, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun persianMonthLength(year: Int, month: Int): Int {
    val nextMonth = if (month == 11) 0 else month + 1
    val nextYear = if (month == 11) year + 1 else year
    val currentStart = persianDateToMillis(PersianDate(year, month, 1))
    val nextStart = persianDateToMillis(PersianDate(nextYear, nextMonth, 1))
    return ((nextStart - currentStart) / (24 * 60 * 60 * 1000L)).toInt()
}

fun formatPersianDate(millis: Long): String {
    val date = persianDateFromMillis(millis)
    return "${toPersianDigits(date.day)} ${PersianMonthNames[date.month]} ${toPersianDigits(date.year)}"
}

/** Compact Solar Hijri label used where the year is unnecessary, such as transaction cards. */
fun formatShortPersianDate(millis: Long): String {
    val date = persianDateFromMillis(millis)
    return "${toPersianDigits(date.day)} ${PersianMonthNames[date.month]}"
}

fun formatPersianMonthYear(year: Int, month: Int): String =
    "${PersianMonthNames[month]} ${toPersianDigits(year)}"

fun toPersianDigits(value: Int): String = value.toString().map { digit ->
    if (digit in '0'..'9') ('۰'.code + digit.code - '0'.code).toChar() else digit
}.joinToString("")

private fun gregorianToPersian(year: Int, month: Int, day: Int): PersianDate {
    val daysBeforeMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val adjustedYear = if (month > 2) year + 1 else year
    var days = 355_666 + 365 * year + (adjustedYear + 3) / 4 -
        (adjustedYear + 99) / 100 + (adjustedYear + 399) / 400 + day + daysBeforeMonth[month - 1]
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

private fun isGregorianLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
