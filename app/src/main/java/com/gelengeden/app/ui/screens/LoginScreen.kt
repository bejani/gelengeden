package com.gelengeden.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gelengeden.app.R
import com.gelengeden.app.data.AuthManager
import com.gelengeden.app.data.AuthManager.LoginMethod
import com.gelengeden.app.ui.components.AppLogo
import com.gelengeden.app.ui.components.PatternGrid
import com.gelengeden.app.ui.viewmodel.AuthPhase
import com.gelengeden.app.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var patternNodes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var usePasswordFallback by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.phase, uiState.loginMethod) {
        password = ""
        confirmPassword = ""
        passwordVisible = false
        confirmVisible = false
        patternNodes = emptyList()
        usePasswordFallback = false
        authViewModel.clearError()
    }

    if (uiState.phase == AuthPhase.LOADING) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val isSetup = uiState.phase == AuthPhase.SETUP
    val usesPatternLogin = !isSetup &&
        uiState.loginMethod == LoginMethod.PATTERN &&
        !usePasswordFallback
    val errorText = authErrorMessage(uiState.errorMessageKey)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(size = 96.dp, cornerRadius = 24.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    when {
                        isSetup -> R.string.login_setup_title
                        usesPatternLogin -> R.string.login_pattern_title
                        else -> R.string.login_title
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    when {
                        isSetup -> R.string.login_setup_subtitle
                        usesPatternLogin -> R.string.login_pattern_subtitle
                        else -> R.string.login_subtitle
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (usesPatternLogin) {
                PatternGrid(
                    selectedNodes = patternNodes,
                    onNodeTapped = { node ->
                        if (node !in patternNodes && !uiState.isBusy) {
                            patternNodes = patternNodes + node
                            if (uiState.errorMessageKey != null) authViewModel.clearError()
                        }
                    },
                    enabled = !uiState.isBusy,
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = stringResource(R.string.pattern_instruction)
                )
                TextButton(
                    onClick = { patternNodes = emptyList() },
                    enabled = patternNodes.isNotEmpty() && !uiState.isBusy
                ) {
                    Text(stringResource(R.string.pattern_clear))
                }

                if (errorText != null) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = { authViewModel.loginWithPattern(patternNodes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isBusy,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.login_sign_in),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                TextButton(
                    onClick = { usePasswordFallback = true },
                    enabled = !uiState.isBusy
                ) {
                    Text(stringResource(R.string.login_use_password))
                }
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (uiState.errorMessageKey != null) authViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    label = {
                        Text(
                            stringResource(
                                if (isSetup) R.string.login_new_password else R.string.login_password
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = stringResource(
                                    if (passwordVisible) R.string.login_hide_password
                                    else R.string.login_show_password
                                )
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isSetup) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = {
                            if (!isSetup && !uiState.isBusy) {
                                focusManager.clearFocus()
                                authViewModel.login(password)
                            }
                        }
                    ),
                    enabled = !uiState.isBusy,
                    isError = errorText != null
                )

                if (isSetup) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (uiState.errorMessageKey != null) authViewModel.clearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        label = { Text(stringResource(R.string.login_confirm_password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    imageVector = if (confirmVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = stringResource(
                                        if (confirmVisible) R.string.login_hide_password
                                        else R.string.login_show_password
                                    )
                                )
                            }
                        },
                        visualTransformation = if (confirmVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!uiState.isBusy) {
                                    focusManager.clearFocus()
                                    authViewModel.setupPassword(password, confirmPassword)
                                }
                            }
                        ),
                        enabled = !uiState.isBusy,
                        isError = errorText != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.login_password_hint,
                            AuthManager.MIN_PASSWORD_LENGTH
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (isSetup) {
                            authViewModel.setupPassword(password, confirmPassword)
                        } else {
                            authViewModel.login(password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isBusy,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = stringResource(
                                if (isSetup) R.string.login_create_password else R.string.login_sign_in
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (!isSetup && uiState.isPatternSet) {
                    TextButton(
                        onClick = { usePasswordFallback = false },
                        enabled = !uiState.isBusy
                    ) {
                        Text(stringResource(R.string.login_use_pattern))
                    }
                }
            }
        }
    }
}

@Composable
fun authErrorMessage(key: String?): String? {
    if (key == null) return null
    return when (key) {
        AuthManager.ERROR_TOO_SHORT -> stringResource(
            R.string.login_error_too_short,
            AuthManager.MIN_PASSWORD_LENGTH
        )
        AuthManager.ERROR_PATTERN_TOO_SHORT -> stringResource(R.string.login_error_pattern_too_short)
        AuthManager.ERROR_PATTERN_NOT_SET -> stringResource(R.string.login_error_pattern_not_set)
        AuthManager.ERROR_WRONG_CURRENT,
        AuthViewModel.ERROR_WRONG_PASSWORD -> stringResource(R.string.login_error_wrong_password)
        AuthViewModel.ERROR_WRONG_PATTERN -> stringResource(R.string.login_error_wrong_pattern)
        AuthViewModel.ERROR_MISMATCH -> stringResource(R.string.login_error_mismatch)
        AuthViewModel.ERROR_EMPTY,
        AuthViewModel.ERROR_EMPTY_CURRENT -> stringResource(R.string.login_error_empty)
        AuthManager.ERROR_SAME_PASSWORD -> stringResource(R.string.settings_error_same_password)
        else -> stringResource(R.string.login_error_generic)
    }
}
