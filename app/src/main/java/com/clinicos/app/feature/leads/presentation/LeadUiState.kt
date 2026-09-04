package com.clinicos.app.feature.leads.presentation

import com.clinicos.app.core.network.dto.LeadDetailDto
import com.clinicos.app.core.network.dto.LeadDto

sealed interface LeadsListState {
    object Loading : LeadsListState
    data class Success(val leads: List<LeadDto>, val total: Int) : LeadsListState
    data class Error(val message: String) : LeadsListState
}

sealed interface LeadDetailState {
    object Loading : LeadDetailState
    data class Success(val lead: LeadDetailDto) : LeadDetailState
    data class Error(val message: String) : LeadDetailState
}

sealed interface LeadActionState {
    object Idle : LeadActionState
    object Loading : LeadActionState
    data class Success(val message: String, val convertedPatientId: String? = null) : LeadActionState
    data class Error(val message: String) : LeadActionState
}
