package com.clinicos.app.feature.ai.presentation

import com.clinicos.app.core.network.dto.AnalyticsQueryResponse
import com.clinicos.app.core.network.dto.ClinicInsightsResponse
import com.clinicos.app.core.network.dto.FollowUpRecommendationsResponse
import com.clinicos.app.core.network.dto.GenerateMessageResponse

sealed interface RecommendationsUiState {
    object Loading : RecommendationsUiState
    data class Success(val data: FollowUpRecommendationsResponse) : RecommendationsUiState
    data class Error(val message: String) : RecommendationsUiState
}

sealed interface InsightsUiState {
    object Loading : InsightsUiState
    data class Success(val data: ClinicInsightsResponse) : InsightsUiState
    data class Error(val message: String) : InsightsUiState
}

sealed interface MessageGenerationUiState {
    object Idle : MessageGenerationUiState
    object Loading : MessageGenerationUiState
    data class Success(val response: GenerateMessageResponse) : MessageGenerationUiState
    data class Error(val message: String) : MessageGenerationUiState
}

sealed interface AnalyticsQueryUiState {
    object Idle : AnalyticsQueryUiState
    object Loading : AnalyticsQueryUiState
    data class Success(val response: AnalyticsQueryResponse) : AnalyticsQueryUiState
    data class Error(val message: String) : AnalyticsQueryUiState
}
