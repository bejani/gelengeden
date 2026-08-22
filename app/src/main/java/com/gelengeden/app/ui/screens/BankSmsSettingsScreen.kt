package com.gelengeden.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gelengeden.app.R
import com.gelengeden.app.data.BankSender
import com.gelengeden.app.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSmsSettingsScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val senders by viewModel.bankSenders.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var hasSmsPermission by remember { mutableStateOf(context.hasPermission(Manifest.permission.RECEIVE_SMS)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSender by remember { mutableStateOf<BankSender?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasSmsPermission = context.hasPermission(Manifest.permission.RECEIVE_SMS)
                hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.bankSmsMessage.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sms_settings_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sms, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.sms_auto_capture_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.sms_auto_capture_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        if (hasSmsPermission) {
                            Text(
                                text = stringResource(R.string.sms_permission_granted),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        requestSmsPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                            NOTIFICATION_PERMISSION_REQUEST_CODE
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.sms_enable_notifications))
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    requestSmsPermission(
                                        context,
                                        Manifest.permission.RECEIVE_SMS,
                                        SMS_PERMISSION_REQUEST_CODE
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.sms_enable_permission))
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sms_senders_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.sms_add_sender)
                        )
                    }
                }
            }

            if (senders.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.sms_no_senders),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(senders, key = { it.id }) { sender ->
                    SenderCard(
                        sender = sender,
                        onEdit = { editingSender = sender },
                        onDelete = { viewModel.deleteBankSender(sender) }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.sms_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            }
        }
    }

    if (showAddDialog) {
        AddBankSenderDialog(
            initialSender = null,
            onDismiss = { showAddDialog = false },
            onSave = { label, address, amountWasRial ->
                viewModel.addBankSender(label, address, amountWasRial)
                showAddDialog = false
            }
        )
    }
    editingSender?.let { sender ->
        AddBankSenderDialog(
            initialSender = sender,
            onDismiss = { editingSender = null },
            onSave = { label, address, amountWasRial ->
                viewModel.updateBankSender(
                    sender.copy(
                        label = label,
                        address = address,
                        amountWasRial = amountWasRial
                    )
                )
                editingSender = null
            }
        )
    }
}

@Composable
private fun SenderCard(
    sender: BankSender,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sender.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(sender.address, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        if (sender.amountWasRial) R.string.sms_unit_rial else R.string.sms_unit_toman
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.sms_edit_sender, sender.label)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddBankSenderDialog(
    initialSender: BankSender?,
    onDismiss: () -> Unit,
    onSave: (label: String, address: String, amountWasRial: Boolean) -> Unit
) {
    var label by remember(initialSender?.id) { mutableStateOf(initialSender?.label.orEmpty()) }
    var address by remember(initialSender?.id) { mutableStateOf(initialSender?.address.orEmpty()) }
    var amountWasRial by remember(initialSender?.id) { mutableStateOf(initialSender?.amountWasRial == true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initialSender == null) R.string.sms_add_sender else R.string.sms_edit_sender_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.sms_bank_label)) },
                    placeholder = { Text(stringResource(R.string.sms_bank_label_hint)) }
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.sms_sender_address)) },
                    placeholder = { Text(stringResource(R.string.sms_sender_address_hint)) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sms_amount_is_rial),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = amountWasRial, onCheckedChange = { amountWasRial = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label, address, amountWasRial) },
                enabled = label.isNotBlank() && address.isNotBlank()
            ) {
                Text(stringResource(if (initialSender == null) R.string.sms_add else R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private const val SMS_PERMISSION_REQUEST_CODE = 4101
private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 4102

private fun requestSmsPermission(context: Context, permission: String, requestCode: Int) {
    val activity = context as? Activity ?: return
    ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
}
