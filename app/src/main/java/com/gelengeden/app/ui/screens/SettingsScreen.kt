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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gelengeden.app.R
import com.gelengeden.app.data.AppearanceManager
import com.gelengeden.app.data.AuthManager
import com.gelengeden.app.data.AuthManager.LoginMethod
import com.gelengeden.app.data.PatternCredential
import com.gelengeden.app.ui.components.PatternGrid
import com.gelengeden.app.ui.viewmodel.AuthViewModel

private enum class PatternSetupStep {
    IDLE,
    CREATE,
    CONFIRM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    appearanceManager: AppearanceManager,
    onBack: () -> Unit,
    onBankSmsClick: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val changeState by authViewModel.changePasswordState.collectAsStateWithLifecycle()
    val patternState by authViewModel.patternSettingsState.collectAsStateWithLifecycle()
    val themeMode by appearanceManager.themeMode.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var newPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var confirmPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var patternSetupStep by remember { mutableStateOf(PatternSetupStep.IDLE) }
    var localPatternErrorKey by remember { mutableStateOf<String?>(null) }

    val successMessage = stringResource(R.string.settings_password_changed)
    val patternSuccessMessage = stringResource(R.string.settings_pattern_saved)
    LaunchedEffect(changeState.success) {
        if (changeState.success) {
            currentPassword = ""
            newPassword = ""
            confirmPassword = ""
            currentVisible = false
            newVisible = false
            confirmVisible = false
            snackbarHostState.showSnackbar(successMessage)
            authViewModel.clearChangePasswordFeedback()
        }
    }

    val errorText = authErrorMessage(changeState.errorMessageKey)
    val patternErrorText = authErrorMessage(localPatternErrorKey ?: patternState.errorMessageKey)

    LaunchedEffect(patternState.success) {
        if (patternState.success) {
            newPattern = emptyList()
            confirmPattern = emptyList()
            patternSetupStep = PatternSetupStep.IDLE
            localPatternErrorKey = null
            snackbarHostState.showSnackbar(patternSuccessMessage)
            authViewModel.clearPatternFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_appearance_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_appearance_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AppearanceModeOption(
                            label = stringResource(R.string.settings_theme_system),
                            selected = themeMode == AppearanceManager.ThemeMode.SYSTEM,
                            onClick = { appearanceManager.setThemeMode(AppearanceManager.ThemeMode.SYSTEM) }
                        )
                        AppearanceModeOption(
                            label = stringResource(R.string.settings_theme_light),
                            selected = themeMode == AppearanceManager.ThemeMode.LIGHT,
                            onClick = { appearanceManager.setThemeMode(AppearanceManager.ThemeMode.LIGHT) }
                        )
                        AppearanceModeOption(
                            label = stringResource(R.string.settings_theme_dark),
                            selected = themeMode == AppearanceManager.ThemeMode.DARK,
                            onClick = { appearanceManager.setThemeMode(AppearanceManager.ThemeMode.DARK) }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_change_password_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.settings_change_password_body,
                                AuthManager.MIN_PASSWORD_LENGTH
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        PasswordField(
                            value = currentPassword,
                            onValueChange = {
                                currentPassword = it
                                if (changeState.errorMessageKey != null) {
                                    authViewModel.clearChangePasswordFeedback()
                                }
                            },
                            label = stringResource(R.string.settings_current_password),
                            visible = currentVisible,
                            onToggleVisible = { currentVisible = !currentVisible },
                            enabled = !changeState.isBusy,
                            isError = errorText != null,
                            imeAction = ImeAction.Next,
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PasswordField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                if (changeState.errorMessageKey != null) {
                                    authViewModel.clearChangePasswordFeedback()
                                }
                            },
                            label = stringResource(R.string.settings_new_password),
                            visible = newVisible,
                            onToggleVisible = { newVisible = !newVisible },
                            enabled = !changeState.isBusy,
                            isError = errorText != null,
                            imeAction = ImeAction.Next,
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PasswordField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                if (changeState.errorMessageKey != null) {
                                    authViewModel.clearChangePasswordFeedback()
                                }
                            },
                            label = stringResource(R.string.settings_confirm_new_password),
                            visible = confirmVisible,
                            onToggleVisible = { confirmVisible = !confirmVisible },
                            enabled = !changeState.isBusy,
                            isError = errorText != null,
                            imeAction = ImeAction.Done,
                            onDone = {
                                focusManager.clearFocus()
                                authViewModel.changePassword(
                                    currentPassword,
                                    newPassword,
                                    confirmPassword
                                )
                            }
                        )

                        if (errorText != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                authViewModel.changePassword(
                                    currentPassword,
                                    newPassword,
                                    confirmPassword
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !changeState.isBusy,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (changeState.isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.settings_save_password))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_login_method_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_login_method_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (authState.loginMethod == LoginMethod.PASSWORD) {
                                Button(onClick = {}, enabled = false) {
                                    Text(stringResource(R.string.settings_login_method_password))
                                }
                            } else {
                                OutlinedButton(onClick = { authViewModel.selectLoginMethod(LoginMethod.PASSWORD) }) {
                                    Text(stringResource(R.string.settings_login_method_password))
                                }
                            }
                            if (authState.loginMethod == LoginMethod.PATTERN) {
                                Button(onClick = {}, enabled = false) {
                                    Text(stringResource(R.string.settings_login_method_pattern))
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { authViewModel.selectLoginMethod(LoginMethod.PATTERN) },
                                    enabled = authState.isPatternSet
                                ) {
                                    Text(stringResource(R.string.settings_login_method_pattern))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_pattern_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_pattern_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        when (patternSetupStep) {
                            PatternSetupStep.IDLE -> {
                                Button(
                                    onClick = {
                                        newPattern = emptyList()
                                        confirmPattern = emptyList()
                                        localPatternErrorKey = null
                                        authViewModel.clearPatternFeedback()
                                        patternSetupStep = PatternSetupStep.CREATE
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !patternState.isBusy,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.settings_pattern_define))
                                }
                            }

                            PatternSetupStep.CREATE -> {
                                Text(
                                    text = stringResource(R.string.settings_pattern_first),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                PatternGrid(
                                    selectedNodes = newPattern,
                                    onPatternChanged = { newPattern = it },
                                    onPatternStarted = {
                                        newPattern = emptyList()
                                        localPatternErrorKey = null
                                        authViewModel.clearPatternFeedback()
                                    },
                                    onPatternCompleted = { pattern ->
                                        newPattern = pattern
                                        if (PatternCredential.canonicalize(pattern) == null) {
                                            localPatternErrorKey = AuthManager.ERROR_PATTERN_TOO_SHORT
                                        } else {
                                            confirmPattern = emptyList()
                                            patternSetupStep = PatternSetupStep.CONFIRM
                                        }
                                    },
                                    enabled = !patternState.isBusy,
                                    activeColor = MaterialTheme.colorScheme.primary,
                                    inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    contentDescription = stringResource(R.string.settings_pattern_first)
                                )
                                TextButton(
                                    onClick = { newPattern = emptyList() },
                                    enabled = newPattern.isNotEmpty() && !patternState.isBusy
                                ) { Text(stringResource(R.string.pattern_clear)) }
                            }

                            PatternSetupStep.CONFIRM -> {
                                Text(
                                    text = stringResource(R.string.settings_pattern_confirm),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                PatternGrid(
                                    selectedNodes = confirmPattern,
                                    onPatternChanged = { confirmPattern = it },
                                    onPatternStarted = {
                                        confirmPattern = emptyList()
                                        localPatternErrorKey = null
                                        authViewModel.clearPatternFeedback()
                                    },
                                    onPatternCompleted = { pattern ->
                                        confirmPattern = pattern
                                        val original = PatternCredential.canonicalize(newPattern)
                                        val confirmation = PatternCredential.canonicalize(pattern)
                                        when {
                                            original == null || confirmation == null -> {
                                                localPatternErrorKey = AuthManager.ERROR_PATTERN_TOO_SHORT
                                            }
                                            original != confirmation -> {
                                                localPatternErrorKey = AuthViewModel.ERROR_MISMATCH
                                            }
                                            else -> authViewModel.savePattern(newPattern, pattern)
                                        }
                                    },
                                    enabled = !patternState.isBusy,
                                    activeColor = MaterialTheme.colorScheme.primary,
                                    inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    contentDescription = stringResource(R.string.settings_pattern_confirm)
                                )
                                TextButton(
                                    onClick = { confirmPattern = emptyList() },
                                    enabled = confirmPattern.isNotEmpty() && !patternState.isBusy
                                ) { Text(stringResource(R.string.pattern_clear)) }
                            }
                        }

                        patternErrorText?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (patternSetupStep != PatternSetupStep.IDLE && patternState.isBusy) {
                            Spacer(modifier = Modifier.height(12.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.height(22.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_sms_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_sms_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onBankSmsClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.settings_sms_open))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_session_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_session_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                authViewModel.logout()
                                onLoggedOut()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_lock_app))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AppearanceModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    enabled: Boolean,
    isError: Boolean,
    imeAction: ImeAction,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        label = { Text(label) },
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.login_hide_password else R.string.login_show_password
                    )
                )
            }
        },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext?.invoke() },
            onDone = { onDone?.invoke() }
        ),
        enabled = enabled,
        isError = isError
    )
}
