package com.gelengeden.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gelengeden.app.R
import com.gelengeden.app.data.PendingBankSms
import com.gelengeden.app.data.TransactionType
import com.gelengeden.app.ui.util.formatDateTime
import com.gelengeden.app.ui.util.formatMoney

/**
 * A mandatory human confirmation gate for imported bank-SMS drafts.
 * Selecting "later" leaves the draft pending and does not create a transaction.
 */
@Composable
fun PendingBankSmsReviewDialog(
    pendingSms: PendingBankSms,
    categories: List<String>,
    onLater: () -> Unit,
    onConfirm: (title: String, category: String) -> Unit
) {
    var title by remember(pendingSms.id) { mutableStateOf("") }
    var showTitleError by remember(pendingSms.id) { mutableStateOf(false) }
    val other = stringResource(R.string.other)
    var category by remember(categories, other) {
        mutableStateOf(
            categories.firstOrNull { it == other } ?: categories.firstOrNull().orEmpty()
        )
    }
    val typeName = stringResource(
        if (pendingSms.suggestedType == TransactionType.INCOME) R.string.income else R.string.expense
    )

    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.sms_review_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        R.string.sms_review_source,
                        pendingSms.senderLabel,
                        formatDateTime(pendingSms.receivedAt)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMoney(pendingSms.displayAmount ?: 0.0),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = typeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (pendingSms.suggestedType == TransactionType.INCOME) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        showTitleError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.sms_review_title_label)) },
                    placeholder = { Text(stringResource(R.string.sms_review_title_hint)) },
                    isError = showTitleError,
                    supportingText = {
                        if (showTitleError) {
                            Text(stringResource(R.string.sms_review_missing_title))
                        }
                    }
                )
                if (categories.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.sms_review_category_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { category = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank()) {
                        showTitleError = true
                    } else if (category.isNotBlank()) {
                        onConfirm(title, category)
                    }
                },
                enabled = category.isNotBlank()
            ) {
                Text(stringResource(R.string.sms_review_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.sms_review_later))
            }
        }
    )
}
