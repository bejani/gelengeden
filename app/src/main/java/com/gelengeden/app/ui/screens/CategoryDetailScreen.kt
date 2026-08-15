package com.gelengeden.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gelengeden.app.R
import com.gelengeden.app.data.Transaction
import com.gelengeden.app.data.TransactionType
import com.gelengeden.app.ui.theme.ExpenseRed
import com.gelengeden.app.ui.theme.IncomeGreen
import com.gelengeden.app.ui.util.formatDate
import com.gelengeden.app.ui.util.formatMoney
import com.gelengeden.app.ui.util.formatPersianDate
import com.gelengeden.app.ui.viewmodel.TransactionViewModel

const val CATEGORY_DETAIL_ALL_TIME_START = 0L
const val CATEGORY_DETAIL_ALL_TIME_END = Long.MAX_VALUE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    viewModel: TransactionViewModel,
    categoryName: String,
    type: TransactionType,
    startMillis: Long,
    endMillis: Long,
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit
) {
    val transactionFlow = remember(categoryName, type, startMillis, endMillis) {
        viewModel.getTransactionsForCategory(categoryName, type, startMillis, endMillis)
    }
    val transactions by transactionFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val total = transactions.sumOf { it.amount }
    val accent = if (type == TransactionType.INCOME) IncomeGreen else ExpenseRed
    val isAllTime = startMillis == CATEGORY_DETAIL_ALL_TIME_START &&
        endMillis == CATEGORY_DETAIL_ALL_TIME_END

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                if (type == TransactionType.INCOME) R.string.income else R.string.expense
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = formatMoney(total),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.category_detail_transaction_count,
                                transactions.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAllTime) {
                                stringResource(R.string.category_detail_all_time)
                            } else {
                                stringResource(
                                    R.string.category_detail_period,
                                    formatPersianDate(startMillis),
                                    formatPersianDate(endMillis)
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.category_detail_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(transactions, key = { it.id }) { transaction ->
                    CategoryDetailTransactionRow(
                        transaction = transaction,
                        accent = accent,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryDetailTransactionRow(
    transaction: Transaction,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDate(transaction.dateMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = formatMoney(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}
