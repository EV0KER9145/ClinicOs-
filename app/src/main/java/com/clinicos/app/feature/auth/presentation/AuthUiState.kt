package com.clinicos.app.feature.auth.presentation

import com.clinicos.app.core.network.dto.CurrentUserResponse

sealed interface AuthState {
    object Loading : AuthState
    object Unauthenticated : AuthState
    data class Authenticated(val user: CurrentUserResponse) : AuthState
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
