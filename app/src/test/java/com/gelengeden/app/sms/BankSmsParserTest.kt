package com.gelengeden.app.sms

import com.gelengeden.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun `parses supplied Bank Melli withdrawal message`() {
        val parsed = BankSmsParser.parse(
            """
            بانك ملي ايران
            برداشت:2,397,273-
            حساب:58003
            مانده:2,259,682
            0524-13:19
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(2_397_273.0, parsed.amount, 0.0)
    }

    @Test
    fun `parses Bank Resalat signed withdrawal without treating balance as income`() {
        val parsed = BankSmsParser.parse(
            """
            10.1651829.1
            -1,100,000
            05/24_14:12
            مانده: 1,645,473
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(1_100_000.0, parsed.amount, 0.0)
    }

    @Test
    fun `parses Persian digit income message`() {
        val parsed = BankSmsParser.parse("واریز: ۱۲٬۵۰۰")

        requireNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(12_500.0, parsed.amount, 0.0)
    }

    @Test
    fun `ignores messages without a transaction amount`() {
        assertEquals(null, BankSmsParser.parse("رمز ورود شما: ۱۲۳۴۵"))
    }

    @Test
    fun `matches sender numbers despite country code formatting`() {
        assertTrue(BankSmsParser.senderMatches("+989121234567", "09121234567"))
        assertFalse(BankSmsParser.senderMatches("09121234567", "09351234567"))
    }

    @Test
    fun `parses withdrawal when amount comes after description`() {
        val parsed = BankSmsParser.parse("برداشت از کارت\nمبلغ تراکنش: ۱۲۳٬۴۵۶ ریال")
        requireNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(123_456.0, parsed.amount, 0.0)
    }

    @Test
    fun `parses deposit when amount uses simple amount label`() {
        val parsed = BankSmsParser.parse("واریز به حساب\nمبلغ: 2,500,000")
        requireNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(2_500_000.0, parsed.amount, 0.0)
    }

    @Test
    fun `matches short phone sender without country prefix`() {
        assertTrue(BankSmsParser.senderMatches("+98700717", "98700717"))
    }

    @Test
    fun `fingerprint is stable for same message`() {
        val first = BankSmsParser.fingerprint("BMI", "برداشت: 1,000", 1234L)
        val second = BankSmsParser.fingerprint("BMI", "برداشت: 1,000", 1234L)
        assertEquals(first, second)
    }
}
