package com.gelengeden.app.ui.viewmodel

import com.gelengeden.app.data.Transaction
import com.gelengeden.app.data.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionViewModelSearchTest {
    private val transaction = Transaction(
        title = "خرید روزانه",
        amount = 125000.0,
        type = TransactionType.EXPENSE,
        category = "مواد غذایی",
        note = "سوپرمارکت محله"
    )

    @Test
    fun searchMatchesTitleCategoryNoteAndAmount() {
        assertTrue(TransactionViewModel.matchesSearch(transaction, "روزانه"))
        assertTrue(TransactionViewModel.matchesSearch(transaction, "غذایی"))
        assertTrue(TransactionViewModel.matchesSearch(transaction, "سوپرمارکت"))
        assertTrue(TransactionViewModel.matchesSearch(transaction, "۱۲۵۰۰۰"))
    }

    @Test
    fun searchNormalizesArabicLettersAndEmptyQueries() {
        assertTrue(TransactionViewModel.matchesSearch(transaction, "خريد"))
        assertTrue(TransactionViewModel.matchesSearch(transaction, "   "))
        assertFalse(TransactionViewModel.matchesSearch(transaction, "اجاره"))
    }
}
