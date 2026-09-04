package com.clinicos.app.feature.appointments.presentation

import com.clinicos.app.core.network.dto.AppointmentDto

sealed interface AppointmentsListState {
    object Loading : AppointmentsListState
    data class Success(val appointments: List<AppointmentDto>, val total: Int) : AppointmentsListState
    data class Error(val message: String) : AppointmentsListState
}

sealed interface AppointmentActionState {
    object Idle : AppointmentActionState
    object Loading : AppointmentActionState
    data class Success(val message: String) : AppointmentActionState
    data class Error(val message: String) : AppointmentActionState
}
