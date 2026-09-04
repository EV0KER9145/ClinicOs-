package com.clinicos.app.feature.clinic.presentation

import com.clinicos.app.core.network.dto.ClinicDto

sealed interface ClinicProfileState {
    object Loading : ClinicProfileState
    data class Success(val clinic: ClinicDto) : ClinicProfileState
    data class Error(val message: String) : ClinicProfileState
}

sealed interface ClinicSaveState {
    object Idle : ClinicSaveState
    object Loading : ClinicSaveState
    data class Success(val clinic: ClinicDto) : ClinicSaveState
    data class Error(val message: String) : ClinicSaveState
}
