package com.gelengeden.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gelengeden.app.data.TransactionType
import com.gelengeden.app.ui.components.PersianMonthPickerDialog
import com.gelengeden.app.ui.components.charts.BalanceSparkline
import com.gelengeden.app.ui.components.charts.CategoryPieChart
import com.gelengeden.app.ui.components.charts.MonthlyBarChart
import com.gelengeden.app.ui.components.charts.MonthlyLineChart
import com.gelengeden.app.ui.report.CategoryTotal
import com.gelengeden.app.ui.report.ReportInsight
import com.gelengeden.app.ui.report.ReportPeriodPreset
import com.gelengeden.app.ui.report.ReportTypeFilter
import com.gelengeden.app.ui.theme.ExpenseRed
import com.gelengeden.app.ui.theme.IncomeGreen
import com.gelengeden.app.ui.util.formatPersianDate
import com.gelengeden.app.ui.util.formatMoney
import com.gelengeden.app.ui.util.formatPercent
import com.gelengeden.app.ui.viewmodel.TransactionViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val report by viewModel.reportState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMonthPicker by remember { mutableStateOf(false) }

    fun openStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = report.range.startMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val start = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                viewModel.setCustomDateRange(start, report.range.endMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = report.range.endMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val end = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                viewModel.setCustomDateRange(report.range.startMillis, end)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Period ──────────────────────────────────────────────
            item {
                SectionTitle("Period")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeriodChip("This month", ReportPeriodPreset.THIS_MONTH, report.preset) {
                        viewModel.setReportPreset(it)
                    }
                    PeriodChip("Last month", ReportPeriodPreset.LAST_MONTH, report.preset) {
                        viewModel.setReportPreset(it)
                    }
                    PeriodChip("3 months", ReportPeriodPreset.LAST_3_MONTHS, report.preset) {
                        viewModel.setReportPreset(it)
                    }
                    PeriodChip("6 months", ReportPeriodPreset.LAST_6_MONTHS, report.preset) {
                        viewModel.setReportPreset(it)
                    }
                    PeriodChip("This year", ReportPeriodPreset.THIS_YEAR, report.preset) {
                        viewModel.setReportPreset(it)
                    }
                    PeriodChip("Custom", ReportPeriodPreset.CUSTOM, report.preset) {
                        viewModel.setReportPreset(it)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showMonthPicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick month")
                    }
                    if (report.preset == ReportPeriodPreset.CUSTOM) {
                        TextButton(onClick = { openStartDatePicker() }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("From")
                        }
                        TextButton(onClick = { openEndDatePicker() }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("To")
                        }
                    }
                }
                Text(
                    text = "${formatPersianDate(report.range.startMillis)}  تا  ${formatPersianDate(report.range.endMillis)}" +
                        "  ·  ${report.range.dayCount} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Type filter ─────────────────────────────────────────
            item {
                SectionTitle("Show")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = report.typeFilter == ReportTypeFilter.ALL,
                        onClick = { viewModel.setReportTypeFilter(ReportTypeFilter.ALL) },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = report.typeFilter == ReportTypeFilter.INCOME,
                        onClick = { viewModel.setReportTypeFilter(ReportTypeFilter.INCOME) },
                        label = { Text("Income") }
                    )
                    FilterChip(
                        selected = report.typeFilter == ReportTypeFilter.EXPENSE,
                        onClick = { viewModel.setReportTypeFilter(ReportTypeFilter.EXPENSE) },
                        label = { Text("Expense") }
                    )
                }
            }

            // ── Summary ─────────────────────────────────────────────
            item {
                PeriodSummaryCard(
                    income = report.totalIncome,
                    expense = report.totalExpense,
                    balance = report.balance,
                    count = report.transactionCount,
                    typeFilter = report.typeFilter
                )
            }

            // ── Insights ────────────────────────────────────────────
            if (report.insights.isNotEmpty()) {
                item {
                    SectionTitle("Insights")
                    Spacer(modifier = Modifier.height(8.dp))
                    InsightsRow(insights = report.insights)
                }
            }

            // ── Category pie ────────────────────────────────────────
            item {
                SectionTitle("Categories")
            }

            item {
                CategoryPieChart(
                    categories = report.categoryBreakdown,
                    typeFilter = report.typeFilter
                )
            }

            // ── Category bars ───────────────────────────────────────
            if (report.categoryBreakdown.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No transactions in this period.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val maxCategory = report.categoryBreakdown.maxOf { it.amount }.coerceAtLeast(1.0)
                items(report.categoryBreakdown) { item ->
                    CategoryBreakdownRow(item = item, maxAmount = maxCategory)
                }
            }

            // ── Chart window ────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle("Monthly charts")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = report.chartMonths == 3,
                        onClick = { viewModel.setChartMonths(3) },
                        label = { Text("3 mo") }
                    )
                    FilterChip(
                        selected = report.chartMonths == 6,
                        onClick = { viewModel.setChartMonths(6) },
                        label = { Text("6 mo") }
                    )
                    FilterChip(
                        selected = report.chartMonths == 12,
                        onClick = { viewModel.setChartMonths(12) },
                        label = { Text("12 mo") }
                    )
                }
                Text(
                    text = "Charts always cover the last ${report.chartMonths} months (independent of period above).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                MonthlyBarChart(
                    points = report.monthlySeries,
                    typeFilter = report.typeFilter
                )
            }

            item {
                MonthlyLineChart(
                    points = report.monthlySeries,
                    typeFilter = report.typeFilter
                )
            }

            if (report.typeFilter == ReportTypeFilter.ALL) {
                item {
                    BalanceSparkline(points = report.monthlySeries)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showMonthPicker) {
        PersianMonthPickerDialog(
            initialDateMillis = report.range.startMillis,
            onDismiss = { showMonthPicker = false },
            onMonthSelected = { year, month ->
                viewModel.setMonth(year, month)
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun PeriodChip(
    label: String,
    preset: ReportPeriodPreset,
    selected: ReportPeriodPreset,
    onSelect: (ReportPeriodPreset) -> Unit
) {
    FilterChip(
        selected = selected == preset,
        onClick = { onSelect(preset) },
        label = { Text(label) }
    )
}

@Composable
private fun InsightsRow(insights: List<ReportInsight>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        insights.forEach { insight ->
            Card(
                modifier = Modifier.width(148.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = insight.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = insight.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (insight.detail.isNotBlank()) {
                        Text(
                            text = insight.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSummaryCard(
    income: Double,
    expense: Double,
    balance: Double,
    count: Int,
    typeFilter: ReportTypeFilter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Period summary",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            when (typeFilter) {
                ReportTypeFilter.ALL -> {
                    Text(
                        text = formatMoney(balance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Net balance · $count transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Income",
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                            Text(
                                formatMoney(income),
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Expense",
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                            Text(
                                formatMoney(expense),
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
                ReportTypeFilter.INCOME -> {
                    Text(
                        text = formatMoney(income),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                    Text(
                        text = "Total income · $count transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
                ReportTypeFilter.EXPENSE -> {
                    Text(
                        text = formatMoney(expense),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                    Text(
                        text = "Total expense · $count transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    item: CategoryTotal,
    maxAmount: Double
) {
    val accent = if (item.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
    val progress = (item.amount / maxAmount).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = buildString {
                            append(if (item.type == TransactionType.INCOME) "Income" else "Expense")
                            append(" · ")
                            append(formatPercent(item.percent.toDouble()))
                            if (item.transactionCount > 0) {
                                append(" · ")
                                append(item.transactionCount)
                                append(if (item.transactionCount == 1) " tx" else " txs")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatMoney(item.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.15f)
            )
        }
    }
}
