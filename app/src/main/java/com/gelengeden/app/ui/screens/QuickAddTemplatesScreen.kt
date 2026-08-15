package com.gelengeden.app.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gelengeden.app.R
import com.gelengeden.app.data.Category
import com.gelengeden.app.data.QuickAddTemplate
import com.gelengeden.app.data.TransactionType
import com.gelengeden.app.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTemplatesScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    onUseTemplate: (Long) -> Unit
) {
    val templates by viewModel.allQuickAddTemplates.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingTemplate by remember { mutableStateOf<QuickAddTemplate?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deletingTemplate by remember { mutableStateOf<QuickAddTemplate?>(null) }

    LaunchedEffect(Unit) {
        viewModel.quickAddMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_add_templates_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.quick_add_new_template)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.quick_add_templates_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (templates.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.quick_add_templates_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(templates, key = { it.id }) { template ->
                    QuickAddTemplateRow(
                        template = template,
                        onUse = { onUseTemplate(template.id) },
                        onEdit = { editingTemplate = template },
                        onDelete = { deletingTemplate = template }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        QuickAddTemplateDialog(
            title = stringResource(R.string.quick_add_create_title),
            initialTemplate = null,
            categories = categories,
            confirmLabel = stringResource(R.string.add),
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, type, category, note ->
                viewModel.addQuickAddTemplate(title, type, category, note)
                showCreateDialog = false
            }
        )
    }

    editingTemplate?.let { template ->
        QuickAddTemplateDialog(
            title = stringResource(R.string.quick_add_edit_title),
            initialTemplate = template,
            categories = categories,
            confirmLabel = stringResource(R.string.save),
            onDismiss = { editingTemplate = null },
            onConfirm = { title, type, category, note ->
                viewModel.updateQuickAddTemplate(
                    template.copy(title = title, type = type, category = category, note = note)
                )
                editingTemplate = null
            }
        )
    }

    deletingTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { deletingTemplate = null },
            title = { Text(stringResource(R.string.quick_add_delete_title)) },
            text = { Text(stringResource(R.string.quick_add_delete_message, template.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteQuickAddTemplate(template)
                        deletingTemplate = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTemplate = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun QuickAddTemplateRow(
    template: QuickAddTemplate,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.quick_add_template_meta,
                        stringResource(
                            if (template.type == TransactionType.INCOME) R.string.income else R.string.expense
                        ),
                        template.category
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onUse) {
                Text(stringResource(R.string.quick_add_use))
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.quick_add_edit_cd, template.title)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.quick_add_delete_cd, template.title),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddTemplateDialog(
    title: String,
    initialTemplate: QuickAddTemplate?,
    categories: List<Category>,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, TransactionType, String, String) -> Unit
) {
    var name by remember(initialTemplate?.id) { mutableStateOf(initialTemplate?.title.orEmpty()) }
    var note by remember(initialTemplate?.id) { mutableStateOf(initialTemplate?.note.orEmpty()) }
    var type by remember(initialTemplate?.id) {
        mutableStateOf(initialTemplate?.type ?: TransactionType.EXPENSE)
    }
    var category by remember(initialTemplate?.id) { mutableStateOf(initialTemplate?.category.orEmpty()) }
    var error by remember(initialTemplate?.id) { mutableStateOf<String?>(null) }
    val categoryOptions = categories.filter { it.type == type }.map { it.name }
    val nameError = stringResource(R.string.quick_add_name_required)
    val categoryError = stringResource(R.string.quick_add_category_required)

    LaunchedEffect(type, categoryOptions) {
        if (categoryOptions.isNotEmpty() && category !in categoryOptions) {
            category = categoryOptions.first()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text(stringResource(R.string.quick_add_name_label)) },
                    placeholder = { Text(stringResource(R.string.quick_add_name_hint)) },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text(stringResource(R.string.income)) }
                    SegmentedButton(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text(stringResource(R.string.expense)) }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.category),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryOptions.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option; error = null },
                            label = { Text(option) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.quick_add_note_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = when {
                        name.isBlank() -> nameError
                        category.isBlank() -> categoryError
                        else -> null
                    }
                    if (error == null) onConfirm(name, type, category, note)
                }
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
