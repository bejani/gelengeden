package com.gelengeden.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PersianCalendarTest {

    @Test
    fun `formats Nowruz as first of Farvardin`() {
        assertEquals("۱ فروردین", formatShortPersianDate(noonMillis(2024, Calendar.MARCH, 20)))
    }

    @Test
    fun `formats August date as Jalali Mordad`() {
        assertEquals("۱۳ مرداد", formatShortPersianDate(noonMillis(2024, Calendar.AUGUST, 3)))
    }

    private fun noonMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
