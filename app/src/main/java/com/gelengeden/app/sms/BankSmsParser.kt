package com.gelengeden.app.sms

import com.gelengeden.app.data.TransactionType
import java.security.MessageDigest

/**
 * Parses only the financial facts required to create a local, user-reviewed draft.
 * The original SMS body is deliberately not retained in the pending transaction queue.
 */
data class ParsedBankSms(
    val amount: Double,
    val type: TransactionType
)

object BankSmsParser {

    private val transactionPattern = Regex(
        pattern = """(?im)(برداشت|واریز|واريز|بستانکار|بستانكار)\s*[:：-]?\s*([0-9۰-۹٠-٩,٬\s]+)"""
    )

    /**
     * Some bank messages omit a transaction label and put the signed amount on its own line.
     * Anchoring to a whole line avoids treating a positive "مانده" (balance) as an income.
     */
    private val signedAmountLinePattern = Regex(
        pattern = """(?m)^\s*([+-])\s*([0-9۰-۹٠-٩,٬\s]+)\s*$"""
    )

    fun parse(body: String): ParsedBankSms? {
        val match = transactionPattern.find(body)
        if (match != null) {
            val type = when (match.groupValues[1]) {
                "برداشت" -> TransactionType.EXPENSE
                "واریز", "واريز", "بستانکار", "بستانكار" -> TransactionType.INCOME
                else -> return null
            }
            return parsedAmount(match.groupValues[2], type)
        }

        val signedMatch = signedAmountLinePattern.find(body) ?: return null
        val type = if (signedMatch.groupValues[1] == "-") {
            TransactionType.EXPENSE
        } else {
            TransactionType.INCOME
        }
        return parsedAmount(signedMatch.groupValues[2], type)
    }

    private fun parsedAmount(rawAmount: String, type: TransactionType): ParsedBankSms? {
        val amountDigits = normalizeDigits(rawAmount).filter { it.isDigit() }
        val amount = amountDigits.toLongOrNull()?.toDouble()?.takeIf { it > 0.0 } ?: return null
        return ParsedBankSms(amount = amount, type = type)
    }

    /**
     * Uses a stable digest so Android redelivery of the same SMS cannot generate duplicate drafts.
     */
    fun fingerprint(senderAddress: String, body: String, receivedAt: Long): String {
        val source = "${normalizeAddress(senderAddress)}|$receivedAt|${normalizeDigits(body)}"
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun senderMatches(configuredAddress: String, incomingAddress: String): Boolean {
        val configured = normalizeAddress(configuredAddress)
        val incoming = normalizeAddress(incomingAddress)
        if (configured.isBlank() || incoming.isBlank()) return false
        if (configured == incoming) return true

        // Phone-number senders can differ only by country-code formatting.
        val configuredDigits = configured.filter(Char::isDigit)
        val incomingDigits = incoming.filter(Char::isDigit)
        return configuredDigits.length >= 7 && incomingDigits.length >= 7 &&
            configuredDigits.takeLast(10) == incomingDigits.takeLast(10)
    }

    private fun normalizeAddress(value: String): String =
        normalizeDigits(value)
            .uppercase()
            .filter { it.isLetterOrDigit() || it == '+' }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    in '۰'..'۹' -> '0' + (char - '۰')
                    in '٠'..'٩' -> '0' + (char - '٠')
                    else -> char
                }
            )
        }
    }
}
