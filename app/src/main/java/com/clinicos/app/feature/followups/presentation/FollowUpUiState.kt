package com.clinicos.app.feature.followups.presentation

import com.clinicos.app.core.network.dto.FollowUpDto

sealed interface FollowUpsListState {
    object Loading : FollowUpsListState
    data class Success(val followUps: List<FollowUpDto>, val total: Int) : FollowUpsListState
    data class Error(val message: String) : FollowUpsListState
}

sealed interface FollowUpActionState {
    object Idle : FollowUpActionState
    object Loading : FollowUpActionState
    data class Success(val message: String) : FollowUpActionState
    data class Error(val message: String) : FollowUpActionState
}
