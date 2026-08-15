package com.gelengeden.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gelengeden.app.R
import com.gelengeden.app.data.TransactionType
import com.gelengeden.app.ui.report.CategoryTotal
import com.gelengeden.app.ui.report.MonthlyPoint
import com.gelengeden.app.ui.report.ReportTypeFilter
import com.gelengeden.app.ui.theme.ExpenseRed
import com.gelengeden.app.ui.theme.IncomeGreen
import com.gelengeden.app.ui.util.formatCompactMoney
import com.gelengeden.app.ui.util.formatMoney
import com.gelengeden.app.ui.util.formatPersianMonthYear
import com.gelengeden.app.ui.util.formatPercent
import kotlin.math.max
import kotlin.math.min

/** Distinct palette for pie slices (cycles if more categories). */
val CategoryChartColors = listOf(
    Color(0xFF1565C0),
    Color(0xFF00897B),
    Color(0xFF6A1B9A),
    Color(0xFFEF6C00),
    Color(0xFFC62828),
    Color(0xFF2E7D32),
    Color(0xFF5D4037),
    Color(0xFF455A64),
    Color(0xFFAD1457),
    Color(0xFF00838F),
    Color(0xFF4527A0),
    Color(0xFFF9A825)
)

@Composable
fun ChartLegend(
    typeFilter: ReportTypeFilter = ReportTypeFilter.ALL,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (typeFilter) {
            ReportTypeFilter.ALL -> {
                LegendDot(color = IncomeGreen, label = stringResource(R.string.income))
                Spacer(modifier = Modifier.width(16.dp))
                LegendDot(color = ExpenseRed, label = stringResource(R.string.expense))
            }
            ReportTypeFilter.INCOME -> LegendDot(color = IncomeGreen, label = stringResource(R.string.income))
            ReportTypeFilter.EXPENSE -> LegendDot(color = ExpenseRed, label = stringResource(R.string.expense))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MonthlyBarChart(
    points: List<MonthlyPoint>,
    typeFilter: ReportTypeFilter = ReportTypeFilter.ALL,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    val density = LocalDensity.current
    val labelTextSize = with(density) { 10.sp.toPx() }
    val axisTextSize = with(density) { 9.sp.toPx() }

    val maxValue = remember(points, typeFilter) {
        val peak = when (typeFilter) {
            ReportTypeFilter.ALL -> points.maxOfOrNull { max(it.income, it.expense) } ?: 0.0
            ReportTypeFilter.INCOME -> points.maxOfOrNull { it.income } ?: 0.0
            ReportTypeFilter.EXPENSE -> points.maxOfOrNull { it.expense } ?: 0.0
        }
        niceMax(peak)
    }

    val hasData = points.any {
        when (typeFilter) {
            ReportTypeFilter.ALL -> it.hasData
            ReportTypeFilter.INCOME -> it.income > 0
            ReportTypeFilter.EXPENSE -> it.expense > 0
        }
    }

    // Scroll when many months so labels/bars stay readable
    val minGroupWidth = 48.dp
    val chartHeight = 230.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.chart_monthly_comparison),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (typeFilter) {
                    ReportTypeFilter.ALL -> stringResource(R.string.chart_income_vs_expense)
                    ReportTypeFilter.INCOME -> stringResource(R.string.chart_income_by_month)
                    ReportTypeFilter.EXPENSE -> stringResource(R.string.chart_expense_by_month)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChartLegend(typeFilter = typeFilter)
            points.firstOrNull()?.let { first ->
                val last = points.last()
                Text(
                    text = "بازهٔ نمودار: ${formatPersianMonthYear(first.year, first.month)} تا ${formatPersianMonthYear(last.year, last.month)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (!hasData) {
                EmptyChartPlaceholder()
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Y-axis labels
                    YAxisLabels(
                        maxValue = maxValue,
                        height = chartHeight,
                        textSizePx = axisTextSize,
                        color = axisColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    val scroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(scroll)
                    ) {
                        val canvasWidth = minGroupWidth * points.size.coerceAtLeast(1)
                        Canvas(
                            modifier = Modifier
                                .width(canvasWidth)
                                .height(chartHeight)
                        ) {
                            val leftPad = 4f
                            val rightPad = 4f
                            val topPad = 12f
                            val bottomPad = 32f
                            val chartW = size.width - leftPad - rightPad
                            val chartH = size.height - topPad - bottomPad
                            val groupCount = points.size.coerceAtLeast(1)
                            val groupWidth = chartW / groupCount

                            val showBoth = typeFilter == ReportTypeFilter.ALL
                            val barWidth = if (showBoth) groupWidth * 0.30f else groupWidth * 0.48f
                            val gap = groupWidth * 0.06f

                            // Grid
                            val gridLines = 4
                            for (i in 0..gridLines) {
                                val y = topPad + chartH * (i / gridLines.toFloat())
                                drawLine(
                                    color = gridColor,
                                    start = Offset(leftPad, y),
                                    end = Offset(size.width - rightPad, y),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                                )
                            }

                            val labelPaint = android.graphics.Paint().apply {
                                color = labelColor.toArgb()
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = labelTextSize
                                isAntiAlias = true
                            }

                            points.forEachIndexed { index, point ->
                                val groupCenter = leftPad + groupWidth * index + groupWidth / 2f
                                val incomeH = ((point.income / maxValue).toFloat().coerceIn(0f, 1f)) * chartH
                                val expenseH = ((point.expense / maxValue).toFloat().coerceIn(0f, 1f)) * chartH

                                when (typeFilter) {
                                    ReportTypeFilter.ALL -> {
                                        val incomeLeft = groupCenter - barWidth - gap / 2f
                                        val expenseLeft = groupCenter + gap / 2f
                                        if (incomeH > 0f) {
                                            drawRoundRect(
                                                color = IncomeGreen,
                                                topLeft = Offset(incomeLeft, topPad + chartH - incomeH),
                                                size = Size(barWidth, incomeH),
                                                cornerRadius = CornerRadius(6f, 6f)
                                            )
                                        }
                                        if (expenseH > 0f) {
                                            drawRoundRect(
                                                color = ExpenseRed,
                                                topLeft = Offset(expenseLeft, topPad + chartH - expenseH),
                                                size = Size(barWidth, expenseH),
                                                cornerRadius = CornerRadius(6f, 6f)
                                            )
                                        }
                                    }
                                    ReportTypeFilter.INCOME -> {
                                        if (incomeH > 0f) {
                                            drawRoundRect(
                                                color = IncomeGreen,
                                                topLeft = Offset(groupCenter - barWidth / 2f, topPad + chartH - incomeH),
                                                size = Size(barWidth, incomeH),
                                                cornerRadius = CornerRadius(6f, 6f)
                                            )
                                        }
                                    }
                                    ReportTypeFilter.EXPENSE -> {
                                        if (expenseH > 0f) {
                                            drawRoundRect(
                                                color = ExpenseRed,
                                                topLeft = Offset(groupCenter - barWidth / 2f, topPad + chartH - expenseH),
                                                size = Size(barWidth, expenseH),
                                                cornerRadius = CornerRadius(6f, 6f)
                                            )
                                        }
                                    }
                                }

                                drawContext.canvas.nativeCanvas.drawText(
                                    point.label,
                                    groupCenter,
                                    size.height - 8f,
                                    labelPaint
                                )
                            }
                        }
                    }
                }

                val peak = when (typeFilter) {
                    ReportTypeFilter.ALL -> points.maxByOrNull { max(it.income, it.expense) }
                    ReportTypeFilter.INCOME -> points.maxByOrNull { it.income }
                    ReportTypeFilter.EXPENSE -> points.maxByOrNull { it.expense }
                }
                if (peak != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (typeFilter) {
                            ReportTypeFilter.ALL ->
                                stringResource(R.string.chart_peak_all, peak.label, formatMoney(peak.income), formatMoney(peak.expense))
                            ReportTypeFilter.INCOME ->
                                stringResource(R.string.chart_peak_single, peak.label, formatMoney(peak.income))
                            ReportTypeFilter.EXPENSE ->
                                stringResource(R.string.chart_peak_single, peak.label, formatMoney(peak.expense))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyLineChart(
    points: List<MonthlyPoint>,
    typeFilter: ReportTypeFilter = ReportTypeFilter.ALL,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
    val labelTextSize = with(density) { 10.sp.toPx() }
    val axisTextSize = with(density) { 9.sp.toPx() }

    val maxValue = remember(points, typeFilter) {
        val peak = when (typeFilter) {
            ReportTypeFilter.ALL -> points.maxOfOrNull { max(it.income, it.expense) } ?: 0.0
            ReportTypeFilter.INCOME -> points.maxOfOrNull { it.income } ?: 0.0
            ReportTypeFilter.EXPENSE -> points.maxOfOrNull { it.expense } ?: 0.0
        }
        niceMax(peak)
    }

    val hasData = points.any {
        when (typeFilter) {
            ReportTypeFilter.ALL -> it.hasData
            ReportTypeFilter.INCOME -> it.income > 0
            ReportTypeFilter.EXPENSE -> it.expense > 0
        }
    }

    val minGroupWidth = 48.dp
    val chartHeight = 230.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.chart_trend_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (typeFilter) {
                    ReportTypeFilter.ALL -> stringResource(R.string.chart_trend_all)
                    ReportTypeFilter.INCOME -> stringResource(R.string.chart_trend_income)
                    ReportTypeFilter.EXPENSE -> stringResource(R.string.chart_trend_expense)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChartLegend(typeFilter = typeFilter)
            points.firstOrNull()?.let { first ->
                val last = points.last()
                Text(
                    text = "بازهٔ نمودار: ${formatPersianMonthYear(first.year, first.month)} تا ${formatPersianMonthYear(last.year, last.month)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (!hasData) {
                EmptyChartPlaceholder()
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    YAxisLabels(
                        maxValue = maxValue,
                        height = chartHeight,
                        textSizePx = axisTextSize,
                        color = axisColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    val scroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(scroll)
                    ) {
                        val canvasWidth = minGroupWidth * points.size.coerceAtLeast(1)
                        Canvas(
                            modifier = Modifier
                                .width(canvasWidth)
                                .height(chartHeight)
                        ) {
                            val leftPad = 12f
                            val rightPad = 12f
                            val topPad = 12f
                            val bottomPad = 32f
                            val chartW = size.width - leftPad - rightPad
                            val chartH = size.height - topPad - bottomPad
                            val count = points.size.coerceAtLeast(1)

                            val gridLines = 4
                            for (i in 0..gridLines) {
                                val y = topPad + chartH * (i / gridLines.toFloat())
                                drawLine(
                                    color = gridColor,
                                    start = Offset(leftPad, y),
                                    end = Offset(size.width - rightPad, y),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                                )
                            }

                            fun xFor(index: Int): Float {
                                return if (count == 1) {
                                    leftPad + chartW / 2f
                                } else {
                                    leftPad + chartW * (index / (count - 1).toFloat())
                                }
                            }

                            fun yFor(value: Double): Float {
                                val ratio = (value / maxValue).toFloat().coerceIn(0f, 1f)
                                return topPad + chartH * (1f - ratio)
                            }

                            fun drawSeries(values: List<Double>, color: Color) {
                                if (values.isEmpty()) return

                                // Area fill under line
                                val area = Path()
                                values.forEachIndexed { index, value ->
                                    val x = xFor(index)
                                    val y = yFor(value)
                                    if (index == 0) area.moveTo(x, y) else area.lineTo(x, y)
                                }
                                area.lineTo(xFor(values.lastIndex), topPad + chartH)
                                area.lineTo(xFor(0), topPad + chartH)
                                area.close()
                                drawPath(
                                    path = area,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f)),
                                        startY = topPad,
                                        endY = topPad + chartH
                                    )
                                )

                                val path = Path()
                                values.forEachIndexed { index, value ->
                                    val x = xFor(index)
                                    val y = yFor(value)
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                                )
                                values.forEachIndexed { index, value ->
                                    val c = Offset(xFor(index), yFor(value))
                                    drawCircle(color = color, radius = 6.5f, center = c)
                                    drawCircle(color = surfaceColor, radius = 3.2f, center = c)
                                }
                            }

                            if (typeFilter != ReportTypeFilter.EXPENSE) {
                                drawSeries(points.map { it.income }, IncomeGreen)
                            }
                            if (typeFilter != ReportTypeFilter.INCOME) {
                                drawSeries(points.map { it.expense }, ExpenseRed)
                            }

                            val labelPaint = android.graphics.Paint().apply {
                                color = labelColor.toArgb()
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = labelTextSize
                                isAntiAlias = true
                            }
                            // Avoid label clutter: show all if ≤8, else every other
                            val step = if (points.size <= 8) 1 else 2
                            points.forEachIndexed { index, point ->
                                if (index % step == 0 || index == points.lastIndex) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        point.label,
                                        xFor(index),
                                        size.height - 8f,
                                        labelPaint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryPieChart(
    categories: List<CategoryTotal>,
    typeFilter: ReportTypeFilter = ReportTypeFilter.ALL,
    modifier: Modifier = Modifier
) {
    val items = categories.take(8)
    val otherLabel = stringResource(R.string.other)
    val otherAmount = categories.drop(8).sumOf { it.amount }
    val slices = buildList {
        addAll(items)
        if (otherAmount > 0) {
            add(
                CategoryTotal(
                    name = otherLabel,
                    amount = otherAmount,
                    type = items.firstOrNull()?.type ?: TransactionType.EXPENSE,
                    percent = 0f,
                    transactionCount = categories.drop(8).sumOf { it.transactionCount }
                )
            )
        }
    }
    val total = slices.sumOf { it.amount }.coerceAtLeast(1e-9)
    val withPercent = slices.map { it.copy(percent = (it.amount / total).toFloat()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.chart_category_share),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (typeFilter) {
                    ReportTypeFilter.ALL -> stringResource(R.string.chart_category_all)
                    ReportTypeFilter.INCOME -> stringResource(R.string.chart_category_income)
                    ReportTypeFilter.EXPENSE -> stringResource(R.string.chart_category_expense)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (withPercent.isEmpty()) {
                EmptyChartPlaceholder(message = stringResource(R.string.chart_no_category_data))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val diameter = min(size.width, size.height)
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        val rectSize = Size(diameter, diameter)
                        var startAngle = -90f
                        val gap = if (withPercent.size > 1) 1.5f else 0f

                        withPercent.forEachIndexed { index, cat ->
                            val sweep = (cat.percent * 360f).coerceAtLeast(0.5f) - gap
                            val color = CategoryChartColors[index % CategoryChartColors.size]
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep.coerceAtLeast(0.5f),
                                useCenter = true,
                                topLeft = topLeft,
                                size = rectSize
                            )
                            startAngle += sweep + gap
                        }

                        // Donut hole
                        val hole = diameter * 0.52f
                        drawCircle(
                            color = Color.Transparent,
                            radius = hole / 2f
                        )
                        // Use a path-style hole by drawing surface-colored circle — surface is passed via parent bg
                    }

                    // Overlay center ring using Box so hole matches card surface
                    Box(
                        modifier = Modifier
                            .size(94.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatCompactMoney(total),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.chart_cats_count, withPercent.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                withPercent.forEachIndexed { index, cat ->
                    val color = CategoryChartColors[index % CategoryChartColors.size]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cat.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatPercent(cat.percent.toDouble()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(
                            text = formatMoney(cat.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (cat.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceSparkline(
    points: List<MonthlyPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val positive = IncomeGreen
    val negative = ExpenseRed

    val balances = points.map { it.balance }
    val hasData = points.any { it.hasData }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.chart_balance_trend),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.chart_balance_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (!hasData) {
                EmptyChartPlaceholder()
            } else {
                val maxAbs = balances.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1.0)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val leftPad = 8f
                    val rightPad = 8f
                    val topPad = 12f
                    val bottomPad = 12f
                    val chartW = size.width - leftPad - rightPad
                    val chartH = size.height - topPad - bottomPad
                    val midY = topPad + chartH / 2f
                    val count = balances.size.coerceAtLeast(1)

                    // Zero line
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPad, midY),
                        end = Offset(size.width - rightPad, midY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )

                    fun xFor(index: Int): Float {
                        return if (count == 1) leftPad + chartW / 2f
                        else leftPad + chartW * (index / (count - 1).toFloat())
                    }

                    fun yFor(value: Double): Float {
                        val ratio = (value / maxAbs).toFloat().coerceIn(-1f, 1f)
                        return midY - ratio * (chartH / 2f)
                    }

                    val path = Path()
                    balances.forEachIndexed { index, value ->
                        val x = xFor(index)
                        val y = yFor(value)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                    balances.forEachIndexed { index, value ->
                        val c = Offset(xFor(index), yFor(value))
                        val dot = if (value >= 0) positive else negative
                        drawCircle(color = dot, radius = 6f, center = c)
                    }
                }

                val last = balances.lastOrNull() ?: 0.0
                val sum = balances.sum()
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.chart_latest, formatMoney(last)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (last >= 0) IncomeGreen else ExpenseRed
                    )
                    Text(
                        text = stringResource(R.string.chart_window_net, formatMoney(sum)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun YAxisLabels(
    maxValue: Double,
    height: Dp,
    textSizePx: Float,
    color: Color
) {
    val ticks = listOf(1.0, 0.75, 0.5, 0.25, 0.0)
    Column(
        modifier = Modifier
            .height(height)
            .width(40.dp)
            .padding(bottom = 28.dp, top = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        ticks.forEach { t ->
            Text(
                text = formatCompactMoney(maxValue * t),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(message: String? = null) {
    val displayMessage = message ?: stringResource(R.string.chart_empty)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Round max up to a readable scale (1 / 2 / 5 × 10^n). */
private fun niceMax(raw: Double): Double {
    if (raw <= 0.0) return 1.0
    val exp = kotlin.math.floor(kotlin.math.log10(raw)).toInt()
    var base = 1.0
    if (exp > 0) {
        repeat(exp) { base *= 10.0 }
    } else if (exp < 0) {
        repeat(-exp) { base /= 10.0 }
    }
    val norm = raw / base
    val nice = when {
        norm <= 1.0 -> 1.0
        norm <= 2.0 -> 2.0
        norm <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * base
}
