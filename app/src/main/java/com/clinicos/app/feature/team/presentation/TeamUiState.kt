package com.clinicos.app.feature.team.presentation

import com.clinicos.app.core.network.dto.DoctorDto
import com.clinicos.app.core.network.dto.UserDto

sealed interface DoctorsListState {
    object Loading : DoctorsListState
    data class Success(val doctors: List<DoctorDto>) : DoctorsListState
    data class Error(val message: String) : DoctorsListState
}

sealed interface StaffListState {
    object Loading : StaffListState
    data class Success(val users: List<UserDto>) : StaffListState
    data class Error(val message: String) : StaffListState
}

sealed interface ActionState {
    object Idle : ActionState
    object Loading : ActionState
    data class Success(val message: String) : ActionState
    data class Error(val message: String) : ActionState
}
