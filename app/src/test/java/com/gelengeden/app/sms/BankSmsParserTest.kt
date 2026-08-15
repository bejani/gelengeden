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
    fun `fingerprint is stable for same message`() {
        val first = BankSmsParser.fingerprint("BMI", "برداشت: 1,000", 1234L)
        val second = BankSmsParser.fingerprint("BMI", "برداشت: 1,000", 1234L)
        assertEquals(first, second)
    }
}
