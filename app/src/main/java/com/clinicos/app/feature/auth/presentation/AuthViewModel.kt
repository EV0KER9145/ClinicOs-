package com.clinicos.app.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.core.network.dto.LoginRequest
import com.clinicos.app.core.network.dto.RegisterRequest
import com.clinicos.app.feature.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (!repository.hasSavedToken()) {
                _authState.value = AuthState.Unauthenticated
                return@launch
            }

            val result = repository.getCurrentUser()
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = {
                    _authState.value = AuthState.Unauthenticated
                }
            )
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginUiState.value = LoginUiState(errorMessage = "Please enter both email and password.")
            return
        }

        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true)
            val loginResult = repository.login(LoginRequest(email.trim(), password))

            loginResult.fold(
                onSuccess = {
                    _loginUiState.value = LoginUiState(isLoading = false)
                    // Retrieve user session details
                    val userResult = repository.getCurrentUser()
                    userResult.fold(
                        onSuccess = { user ->
                            _authState.value = AuthState.Authenticated(user)
                        },
                        onFailure = { err ->
                            _loginUiState.value = LoginUiState(errorMessage = err.message ?: "Failed to load user session.")
                            _authState.value = AuthState.Unauthenticated
                        }
                    )
                },
                onFailure = { err ->
                    _loginUiState.value = LoginUiState(isLoading = false, errorMessage = err.message ?: "Authentication failed.")
                }
            )
        }
    }

    fun register(
        clinicName: String,
        clinicType: String?,
        clinicPhone: String?,
        clinicEmail: String?,
        fullName: String,
        email: String,
        password: String
    ) {
        if (clinicName.isBlank()) {
            _registerUiState.value = RegisterUiState(errorMessage = "Clinic name is required.")
            return
        }
        if (fullName.isBlank()) {
            _registerUiState.value = RegisterUiState(errorMessage = "Owner full name is required.")
            return
        }
        if (email.isBlank() || !email.contains("@")) {
            _registerUiState.value = RegisterUiState(errorMessage = "Please enter a valid email address.")
            return
        }
        if (password.length < 8) {
            _registerUiState.value = RegisterUiState(errorMessage = "Password must be at least 8 characters.")
            return
        }

        viewModelScope.launch {
            _registerUiState.value = RegisterUiState(isLoading = true)
            val request = RegisterRequest(
                clinicName = clinicName.trim(),
                clinicType = clinicType?.ifBlank { null }?.trim(),
                clinicPhone = clinicPhone?.ifBlank { null }?.trim(),
                clinicEmail = clinicEmail?.ifBlank { null }?.trim(),
                fullName = fullName.trim(),
                email = email.trim(),
                password = password
            )

            val result = repository.registerClinic(request)
            result.fold(
                onSuccess = {
                    _registerUiState.value = RegisterUiState(isSuccess = true)
                },
                onFailure = { err ->
                    _registerUiState.value = RegisterUiState(
                        isLoading = false,
                        errorMessage = err.message ?: "Registration failed."
                    )
                }
            )
        }
    }

    fun clearLoginError() {
        _loginUiState.value = _loginUiState.value.copy(errorMessage = null)
    }

    fun clearRegisterState() {
        _registerUiState.value = RegisterUiState()
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.Unauthenticated
            _loginUiState.value = LoginUiState()
            _registerUiState.value = RegisterUiState()
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository) as T
        }
    }
}
