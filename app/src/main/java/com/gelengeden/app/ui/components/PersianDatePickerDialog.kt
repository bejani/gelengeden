package com.gelengeden.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gelengeden.app.ui.util.PersianDate
import com.gelengeden.app.ui.util.PersianMonthNames
import com.gelengeden.app.ui.util.persianDateFromMillis
import com.gelengeden.app.ui.util.persianDateToMillis
import com.gelengeden.app.ui.util.persianMonthLength
import com.gelengeden.app.ui.util.toPersianDigits
import java.util.Calendar

/** A compact, explicit Solar Hijri calendar picker. */
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

    fun previousMonth() {
        if (visibleMonth == 0) {
            visibleMonth = 11
            visibleYear--
        } else visibleMonth--
    }
    fun nextMonth() {
        if (visibleMonth == 11) {
            visibleMonth = 0
            visibleYear++
        } else visibleMonth++
    }

    val firstDayMillis = persianDateToMillis(PersianDate(visibleYear, visibleMonth, 1))
    val weekdayOffset = Calendar.getInstance().apply { timeInMillis = firstDayMillis }
        .get(Calendar.DAY_OF_WEEK) % 7 // Saturday is first in the Persian week.
    val dayCount = persianMonthLength(visibleYear, visibleMonth)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { previousMonth() }) { Text("ماه قبل") }
                    Text(
                        text = "${PersianMonthNames[visibleMonth]} ${toPersianDigits(visibleYear)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { nextMonth() }) { Text("ماه بعد") }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { dayName ->
                        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Text(dayName, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                    repeat(weekdayOffset) { Spacer(modifier = Modifier.size(30.dp)) }
                    (1..dayCount).forEach { day ->
                        val isSelected = selectedDate == PersianDate(visibleYear, visibleMonth, day)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clickable {
                                    selectedDate = PersianDate(visibleYear, visibleMonth, day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = toPersianDigits(day),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDateSelected(persianDateToMillis(selectedDate)) }) {
                Text("تأیید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
