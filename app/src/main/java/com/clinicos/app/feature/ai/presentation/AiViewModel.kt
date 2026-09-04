package com.clinicos.app.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.core.network.dto.AnalyticsQueryRequest
import com.clinicos.app.core.network.dto.GenerateMessageRequest
import com.clinicos.app.feature.ai.data.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiViewModel(
    private val repository: AiRepository
) : ViewModel() {

    private val _recommendationsState = MutableStateFlow<RecommendationsUiState>(RecommendationsUiState.Loading)
    val recommendationsState: StateFlow<RecommendationsUiState> = _recommendationsState.asStateFlow()

    private val _insightsState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val insightsState: StateFlow<InsightsUiState> = _insightsState.asStateFlow()

    private val _messageState = MutableStateFlow<MessageGenerationUiState>(MessageGenerationUiState.Idle)
    val messageState: StateFlow<MessageGenerationUiState> = _messageState.asStateFlow()

    private val _analyticsState = MutableStateFlow<AnalyticsQueryUiState>(AnalyticsQueryUiState.Idle)
    val analyticsState: StateFlow<AnalyticsQueryUiState> = _analyticsState.asStateFlow()

    init {
        loadRecommendations()
        loadInsights()
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _recommendationsState.value = RecommendationsUiState.Loading
            repository.getRecommendations(limit = 10)
                .onSuccess { data ->
                    _recommendationsState.value = RecommendationsUiState.Success(data)
                }
                .onFailure { err ->
                    _recommendationsState.value = RecommendationsUiState.Error(err.message ?: "Failed to load AI recommendations.")
                }
        }
    }

    fun loadInsights() {
        viewModelScope.launch {
            _insightsState.value = InsightsUiState.Loading
            repository.getClinicInsights()
                .onSuccess { data ->
                    _insightsState.value = InsightsUiState.Success(data)
                }
                .onFailure { err ->
                    _insightsState.value = InsightsUiState.Error(err.message ?: "Failed to load AI clinic insights.")
                }
        }
    }

    fun generateMessage(
        entityType: String,
        entityId: String,
        purpose: String,
        tone: String = "FRIENDLY",
        additionalContext: String? = null
    ) {
        viewModelScope.launch {
            _messageState.value = MessageGenerationUiState.Loading
            val req = GenerateMessageRequest(
                entityType = entityType,
                entityId = entityId,
                purpose = purpose,
                tone = tone,
                additionalContext = additionalContext
            )

            repository.generateMessage(req)
                .onSuccess { res ->
                    _messageState.value = MessageGenerationUiState.Success(res)
                }
                .onFailure { err ->
                    _messageState.value = MessageGenerationUiState.Error(err.message ?: "Failed to generate AI message.")
                }
        }
    }

    fun queryAnalytics(question: String, period: String? = null) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _analyticsState.value = AnalyticsQueryUiState.Loading
            val req = AnalyticsQueryRequest(question = question.trim(), period = period)

            repository.queryAnalytics(req)
                .onSuccess { res ->
                    _analyticsState.value = AnalyticsQueryUiState.Success(res)
                }
                .onFailure { err ->
                    _analyticsState.value = AnalyticsQueryUiState.Error(err.message ?: "Failed to answer question.")
                }
        }
    }

    fun clearMessageState() {
        _messageState.value = MessageGenerationUiState.Idle
    }

    class Factory(private val repository: AiRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiViewModel(repository) as T
        }
    }
}
