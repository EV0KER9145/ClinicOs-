package com.clinicos.app.feature.automation.presentation

import com.clinicos.app.core.network.dto.CommunicationDraftDto

sealed interface DraftsListState {
    object Loading : DraftsListState
    data class Success(val drafts: List<CommunicationDraftDto>, val total: Int) : DraftsListState
    data class Error(val message: String) : DraftsListState
}

sealed interface DraftActionState {
    object Idle : DraftActionState
    object Loading : DraftActionState
    data class Success(val message: String) : DraftActionState
    data class Error(val message: String) : DraftActionState
}
