package com.gelengeden.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gelengeden.app.data.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AuthPhase {
    /** Checking whether a password already exists. */
    LOADING,

    /** First launch — user must create a password. */
    SETUP,

    /** Password exists — user must sign in. */
    LOGIN,

    /** Session unlocked for this process. */
    AUTHENTICATED
}

data class AuthUiState(
    val phase: AuthPhase = AuthPhase.LOADING,
    val isBusy: Boolean = false,
    val errorMessageKey: String? = null
)

data class ChangePasswordUiState(
    val isBusy: Boolean = false,
    val errorMessageKey: String? = null,
    val success: Boolean = false
)

class AuthViewModel(
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _changePasswordState = MutableStateFlow(ChangePasswordUiState())
    val changePasswordState: StateFlow<ChangePasswordUiState> = _changePasswordState.asStateFlow()

    private val _passwordChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val passwordChanged: SharedFlow<Unit> = _passwordChanged.asSharedFlow()

    init {
        refreshPhase()
    }

    fun refreshPhase() {
        viewModelScope.launch {
            val set = withContext(Dispatchers.IO) { authManager.isPasswordSet() }
            val current = _uiState.value.phase
            // Keep authenticated across config changes / re-check
            if (current == AuthPhase.AUTHENTICATED) {
                _uiState.update { it.copy(phase = AuthPhase.AUTHENTICATED, isBusy = false, errorMessageKey = null) }
            } else {
                _uiState.update {
                    it.copy(
                        phase = if (set) AuthPhase.LOGIN else AuthPhase.SETUP,
                        isBusy = false,
                        errorMessageKey = null
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessageKey = null) }
    }

    fun clearChangePasswordFeedback() {
        _changePasswordState.update {
            it.copy(errorMessageKey = null, success = false)
        }
    }

    fun setupPassword(password: String, confirmPassword: String) {
        if (_uiState.value.isBusy) return
        val trimmed = password.trim()
        val trimmedConfirm = confirmPassword.trim()

        when {
            trimmed.length < AuthManager.MIN_PASSWORD_LENGTH -> {
                _uiState.update { it.copy(errorMessageKey = AuthManager.ERROR_TOO_SHORT) }
                return
            }
            trimmed != trimmedConfirm -> {
                _uiState.update { it.copy(errorMessageKey = ERROR_MISMATCH) }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessageKey = null) }
            val result = withContext(Dispatchers.IO) { authManager.setPassword(trimmed) }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            phase = AuthPhase.AUTHENTICATED,
                            isBusy = false,
                            errorMessageKey = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessageKey = e.message ?: ERROR_GENERIC
                        )
                    }
                }
            )
        }
    }

    fun login(password: String) {
        if (_uiState.value.isBusy) return
        val trimmed = password.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(errorMessageKey = ERROR_EMPTY) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessageKey = null) }
            val ok = withContext(Dispatchers.IO) { authManager.verifyPassword(trimmed) }
            if (ok) {
                _uiState.update {
                    it.copy(
                        phase = AuthPhase.AUTHENTICATED,
                        isBusy = false,
                        errorMessageKey = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessageKey = ERROR_WRONG_PASSWORD
                    )
                }
            }
        }
    }

    fun logout() {
        _uiState.update {
            it.copy(
                phase = if (authManager.isPasswordSet()) AuthPhase.LOGIN else AuthPhase.SETUP,
                isBusy = false,
                errorMessageKey = null
            )
        }
        _changePasswordState.value = ChangePasswordUiState()
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        if (_changePasswordState.value.isBusy) return

        val current = currentPassword.trim()
        val new = newPassword.trim()
        val confirm = confirmPassword.trim()

        when {
            current.isEmpty() -> {
                _changePasswordState.update {
                    it.copy(errorMessageKey = ERROR_EMPTY_CURRENT, success = false)
                }
                return
            }
            new.length < AuthManager.MIN_PASSWORD_LENGTH -> {
                _changePasswordState.update {
                    it.copy(errorMessageKey = AuthManager.ERROR_TOO_SHORT, success = false)
                }
                return
            }
            new != confirm -> {
                _changePasswordState.update {
                    it.copy(errorMessageKey = ERROR_MISMATCH, success = false)
                }
                return
            }
        }

        viewModelScope.launch {
            _changePasswordState.update {
                it.copy(isBusy = true, errorMessageKey = null, success = false)
            }
            val result = withContext(Dispatchers.IO) {
                authManager.changePassword(current, new)
            }
            result.fold(
                onSuccess = {
                    _changePasswordState.update {
                        it.copy(isBusy = false, errorMessageKey = null, success = true)
                    }
                    _passwordChanged.tryEmit(Unit)
                },
                onFailure = { e ->
                    _changePasswordState.update {
                        it.copy(
                            isBusy = false,
                            errorMessageKey = e.message ?: ERROR_GENERIC,
                            success = false
                        )
                    }
                }
            )
        }
    }

    companion object {
        const val ERROR_MISMATCH = "mismatch"
        const val ERROR_EMPTY = "empty"
        const val ERROR_EMPTY_CURRENT = "empty_current"
        const val ERROR_WRONG_PASSWORD = "wrong_password"
        const val ERROR_GENERIC = "generic"

        fun factory(authManager: AuthManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                        return AuthViewModel(authManager) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
