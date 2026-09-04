package com.clinicos.app.feature.dashboard.presentation

import com.clinicos.app.core.network.dto.DashboardSummaryResponse

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val summary: DashboardSummaryResponse) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
