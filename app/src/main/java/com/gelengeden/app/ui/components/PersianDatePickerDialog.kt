package com.gelengeden.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gelengeden.app.ui.util.PersianDate
import com.gelengeden.app.ui.util.PersianMonthNames
import com.gelengeden.app.ui.util.persianDateFromMillis
import com.gelengeden.app.ui.util.persianDateToMillis
import com.gelengeden.app.ui.util.persianMonthLength
import com.gelengeden.app.ui.util.toPersianDigits
import java.util.Calendar

/** Mobile-first Solar Hijri calendar picker with an explicit selected-date summary. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersianDatePickerDialog(
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    var selectedDate by remember { mutableStateOf(persianDateFromMillis(initialDateMillis)) }
    var visibleYear by remember { mutableStateOf(selectedDate.year) }
    var visibleMonth by remember { mutableStateOf(selectedDate.month) }
    val today = remember { persianDateFromMillis(System.currentTimeMillis()) }

    fun previousMonth() {
        if (visibleMonth == 0) {
            visibleMonth = 11
            visibleYear--
        } else {
            visibleMonth--
        }
    }

    fun nextMonth() {
        if (visibleMonth == 11) {
            visibleMonth = 0
            visibleYear++
        } else {
            visibleMonth++
        }
    }

    fun selectToday() {
        selectedDate = today
        visibleYear = today.year
        visibleMonth = today.month
    }

    val firstDayMillis = persianDateToMillis(PersianDate(visibleYear, visibleMonth, 1))
    val weekdayOffset = Calendar.getInstance().apply { timeInMillis = firstDayMillis }
        .get(Calendar.DAY_OF_WEEK) % 7 // Saturday is the first day of the Persian week.
    val dayCount = persianMonthLength(visibleYear, visibleMonth)
    val selectedDateLabel = "${toPersianDigits(selectedDate.day)} ${PersianMonthNames[selectedDate.month]} ${toPersianDigits(selectedDate.year)}"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "انتخاب تاریخ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = ::selectToday) {
                        Text("امروز")
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "تاریخ انتخاب‌شده",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                        )
                        Text(
                            text = selectedDateLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = ::previousMonth,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "ماه قبل"
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = PersianMonthNames[visibleMonth],
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = toPersianDigits(visibleYear),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = ::nextMonth,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ماه بعد"
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { dayName ->
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    repeat(weekdayOffset) { Spacer(modifier = Modifier.size(40.dp)) }
                    (1..dayCount).forEach { day ->
                        val date = PersianDate(visibleYear, visibleMonth, day)
                        val isSelected = selectedDate == date
                        val isToday = today == date
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .then(
                                    when {
                                        isSelected -> Modifier.background(
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                        isToday -> Modifier.border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                            CircleShape
                                        )
                                        else -> Modifier
                                    }
                                )
                                .clickable { selectedDate = date },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = toPersianDigits(day),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = { onDateSelected(persianDateToMillis(selectedDate)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("تأیید تاریخ")
                    }
                }
            }
        }
    }
}
