package com.clinicos.app.feature.patients.presentation

import com.clinicos.app.core.network.dto.PatientDetailDto
import com.clinicos.app.core.network.dto.PatientDto
import com.clinicos.app.core.network.dto.TagDto

sealed interface PatientsListState {
    object Loading : PatientsListState
    data class Success(val patients: List<PatientDto>, val total: Int) : PatientsListState
    data class Error(val message: String) : PatientsListState
}

sealed interface PatientDetailState {
    object Loading : PatientDetailState
    data class Success(val patient: PatientDetailDto) : PatientDetailState
    data class Error(val message: String) : PatientDetailState
}

sealed interface PatientActionState {
    object Idle : PatientActionState
    object Loading : PatientActionState
    data class Success(val message: String, val patientId: String? = null) : PatientActionState
    data class Error(val message: String) : PatientActionState
}

data class AvailableTagsState(
    val tags: List<TagDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
