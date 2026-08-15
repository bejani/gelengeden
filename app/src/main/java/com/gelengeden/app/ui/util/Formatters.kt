package com.gelengeden.app.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val appLocale: Locale get() = LocaleHelper.APP_LOCALE

private val integerNumberFormat: NumberFormat
    get() = NumberFormat.getIntegerInstance(appLocale).apply {
        isGroupingUsed = true
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

fun formatMoney(amount: Double): String =
    "${integerNumberFormat.format(amount)} تومان"

fun formatCompactMoney(amount: Double): String {
    val absolute = abs(amount)
    val sign = if (amount < 0) "−" else ""
    val number = NumberFormat.getNumberInstance(appLocale).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    return when {
        absolute >= 1_000_000_000 -> "$sign${number.format(absolute / 1_000_000_000.0)} میلیارد"
        absolute >= 1_000_000 -> "$sign${number.format(absolute / 1_000_000.0)} میلیون"
        absolute >= 1_000 -> "$sign${number.format(absolute / 1_000.0)} هزار"
        else -> "$sign${number.format(absolute)}"
    }
}

fun formatPercent(ratio: Double): String =
    "${NumberFormat.getNumberInstance(appLocale).apply { maximumFractionDigits = 0 }.format((ratio * 100).coerceIn(0.0, 100.0))}٪"

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", appLocale).format(Date(millis))

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy · HH:mm", appLocale).format(Date(millis))

fun formatMonthYear(year: Int, month: Int): String =
    SimpleDateFormat("MMM yyyy", appLocale).format(
        java.util.Calendar.getInstance(appLocale).apply { set(year, month, 1) }.time
    )

fun formatShortAmount(amount: Double): String {
    val absolute = abs(amount)
    val sign = if (amount < 0) "−" else ""
    val number = NumberFormat.getNumberInstance(appLocale).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    return when {
        absolute >= 1_000_000 -> "$sign${number.format(absolute / 1_000_000.0)} میلیون"
        absolute >= 1_000 -> "$sign${number.format(absolute / 1_000.0)} هزار"
        else -> "$sign${number.format(absolute)}"
    }
}

/** Max whole-number digits allowed in the amount field (avoids Long overflow). */
private const val MAX_AMOUNT_DIGITS = 15

/**
 * Converts Persian/Arabic-Indic digits to Western digits so parsing stays reliable
 * regardless of keyboard or locale digit shape.
 */
fun normalizeDigits(input: String): String {
    if (input.isEmpty()) return input
    val out = StringBuilder(input.length)
    for (c in input) {
        out.append(
            when (c) {
                in '۰'..'۹' -> '0' + (c - '۰') // Persian
                in '٠'..'٩' -> '0' + (c - '٠') // Arabic-Indic
                else -> c
            }
        )
    }
    return out.toString()
}

/**
 * Keeps only digits from free-form amount input (strips separators, decimals, signs).
 * Used while typing so the field stores a plain digit string (no leading zeros).
 */
fun sanitizeAmountInput(input: String): String {
    val digits = normalizeDigits(input).filter { it.isDigit() }.take(MAX_AMOUNT_DIGITS)
    if (digits.isEmpty()) return ""
    val trimmed = digits.trimStart('0')
    return trimmed.ifEmpty { "0" }
}

/**
 * Formats a whole amount for display/edit in the amount text field (grouped, no decimals).
 * Returns empty string for non-positive values so the field can start blank.
 */
fun formatAmountForInput(amount: Double): String {
    if (!amount.isFinite() || amount <= 0.0) return ""
    return amount.roundToLong().toString()
}

/**
 * Parses a formatted or raw amount string into a Double.
 * Accepts thousand separators and Persian/Arabic digits; ignores any decimal portion.
 */
fun parseAmountInput(text: String): Double? {
    val digits = sanitizeAmountInput(text)
    if (digits.isEmpty()) return null
    return digits.toLongOrNull()?.toDouble()
}

/**
 * Shows thousand separators in the amount field while the underlying value stays digit-only.
 * Cursor offsets are mapped so editing still feels natural as separators appear/disappear.
 */
class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val digits = normalizeDigits(raw).filter { it.isDigit() }
        if (digits.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val number = digits.toLongOrNull()
            ?: return TransformedText(AnnotatedString(digits), OffsetMapping.Identity)
        val formatted = integerNumberFormat.format(number)

        // Map original digit indices ↔ formatted indices (skip grouping chars in formatted text).
        val originalToTransformed = IntArray(digits.length + 1) { formatted.length }
        val transformedToOriginal = IntArray(formatted.length + 1)

        var digitIndex = 0
        for (i in formatted.indices) {
            transformedToOriginal[i] = digitIndex.coerceAtMost(digits.length)
            val ch = formatted[i]
            if (ch.isDigit() || ch in '۰'..'۹' || ch in '٠'..'٩') {
                if (digitIndex <= digits.length) {
                    originalToTransformed[digitIndex] = i
                }
                digitIndex++
            }
        }
        originalToTransformed[digits.length] = formatted.length
        transformedToOriginal[formatted.length] = digits.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, digits.length)
                return originalToTransformed[safe]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, formatted.length)
                return transformedToOriginal[safe]
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
