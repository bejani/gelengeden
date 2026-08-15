package com.gelengeden.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gelengeden.app.R
import com.gelengeden.app.data.Transaction
import com.gelengeden.app.data.TransactionType
import com.gelengeden.app.ui.components.PersianDatePickerDialog
import com.gelengeden.app.ui.util.ThousandSeparatorVisualTransformation
import com.gelengeden.app.ui.util.formatAmountForInput
import com.gelengeden.app.ui.util.formatPersianDate
import com.gelengeden.app.ui.util.parseAmountInput
import com.gelengeden.app.ui.util.sanitizeAmountInput
import com.gelengeden.app.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: TransactionViewModel,
    transactionId: Long? = null,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val isEdit = transactionId != null
    val allCategories by viewModel.categories.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category by remember { mutableStateOf("") }
    var existing by remember { mutableStateOf<Transaction?>(null) }
    var typeIndex by remember { mutableIntStateOf(1) } // 0 income, 1 expense
    var error by remember { mutableStateOf<String?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val categoryOptions = remember(allCategories, type) {
        allCategories
            .filter { it.type == type }
            .map { it.name }
    }

    // Include orphan category name when editing a transaction whose category was removed
    val selectableCategories = remember(categoryOptions, category, isEdit, existing, type) {
        if (
            category.isNotBlank() &&
            category !in categoryOptions &&
            isEdit &&
            existing?.type == type &&
            existing?.category == category
        ) {
            listOf(category) + categoryOptions
        } else {
            categoryOptions
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            val tx = viewModel.getTransactionById(transactionId)
            if (tx != null) {
                existing = tx
                title = tx.title
                amountText = formatAmountForInput(tx.amount)
                note = tx.note
                selectedDateMillis = tx.dateMillis
                type = tx.type
                category = tx.category
                typeIndex = if (tx.type == TransactionType.INCOME) 0 else 1
            }
        }
    }

    // Keep selected category valid for the current type / live list
    LaunchedEffect(type, categoryOptions, category) {
        if (categoryOptions.isEmpty()) return@LaunchedEffect
        if (category.isBlank() || category !in categoryOptions) {
            // Keep orphan category from edited transaction visible if still selected string-only
            if (isEdit && existing?.type == type && category == existing?.category && category.isNotBlank()) {
                return@LaunchedEffect
            }
            category = categoryOptions.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) {
                            stringResource(R.string.edit_transaction_title)
                        } else {
                            stringResource(R.string.add_transaction_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.type),
                style = MaterialTheme.typography.titleMedium
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = typeIndex == 0,
                    onClick = {
                        typeIndex = 0
                        type = TransactionType.INCOME
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(stringResource(R.string.income))
                }
                SegmentedButton(
                    selected = typeIndex == 1,
                    onClick = {
                        typeIndex = 1
                        type = TransactionType.EXPENSE
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(stringResource(R.string.expense))
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    error = null
                },
                label = { Text(stringResource(R.string.title_label)) },
                placeholder = { Text(stringResource(R.string.title_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        R.string.transaction_date_label,
                        formatPersianDate(selectedDateMillis)
                    )
                )
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    // Digits only; thousand separators are shown via VisualTransformation
                    amountText = sanitizeAmountInput(input)
                    error = null
                },
                label = { Text(stringResource(R.string.amount)) },
                placeholder = { Text(stringResource(R.string.amount_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.titleMedium
            )
            if (selectableCategories.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_categories_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                CategorySelectorField(
                    selectedCategory = category.ifBlank {
                        stringResource(R.string.select_category)
                    },
                    onClick = { showCategoryPicker = true }
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note_optional)) },
                placeholder = { Text(stringResource(R.string.note_placeholder)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val errorEnterTitle = stringResource(R.string.error_enter_title)
            val errorValidAmount = stringResource(R.string.error_valid_amount)
            val errorChooseCategory = stringResource(R.string.error_choose_category)

            Button(
                onClick = {
                    val amount = parseAmountInput(amountText)
                    when {
                        title.isBlank() -> error = errorEnterTitle
                        amount == null || amount <= 0 -> error = errorValidAmount
                        category.isBlank() -> error = errorChooseCategory
                        else -> {
                            if (isEdit && existing != null) {
                                viewModel.updateTransaction(
                                    existing!!.copy(
                                        title = title.trim(),
                                        amount = amount,
                                        type = type,
                                        category = category,
                                        note = note.trim(),
                                        dateMillis = selectedDateMillis
                                    )
                                )
                            } else {
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    category = category,
                                    note = note,
                                    dateMillis = selectedDateMillis
                                )
                            }
                            onDone()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (isEdit) {
                        stringResource(R.string.save_changes)
                    } else {
                        stringResource(R.string.save_transaction)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        PersianDatePickerDialog(
            initialDateMillis = selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onDateSelected = { millis ->
                selectedDateMillis = millis
                showDatePicker = false
            }
        )
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            categories = selectableCategories,
            selectedCategory = category,
            onSelect = { selected ->
                category = selected
                error = null
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

/**
 * Compact tappable field that shows the selected category and opens the picker.
 * Looks like an OutlinedTextField so it matches the rest of the form.
 */
@Composable
private fun CategorySelectorField(
    selectedCategory: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            singleLine = true,
            label = { Text(stringResource(R.string.select_category)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        // Full-area click target over the disabled field
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<String>,
    selectedCategory: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val filtered = remember(categories, query) {
        val q = query.trim()
        if (q.isEmpty()) categories
        else categories.filter { it.contains(q, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.select_category),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_categories)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.search_clear)
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_category_match),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filtered, key = { it }) { name ->
                        val selected = name == selectedCategory
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(name) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelect(name) }
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
