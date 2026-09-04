package com.clinicos.app.feature.team.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.core.network.dto.CreateDoctorRequest
import com.clinicos.app.core.network.dto.CreateUserRequest
import com.clinicos.app.core.network.dto.UpdateDoctorRequest
import com.clinicos.app.core.network.dto.UpdateUserRequest
import com.clinicos.app.feature.team.data.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeamViewModel(
    private val repository: TeamRepository
) : ViewModel() {

    private val _doctorsState = MutableStateFlow<DoctorsListState>(DoctorsListState.Loading)
    val doctorsState: StateFlow<DoctorsListState> = _doctorsState.asStateFlow()

    private val _staffState = MutableStateFlow<StaffListState>(StaffListState.Loading)
    val staffState: StateFlow<StaffListState> = _staffState.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    init {
        loadDoctors()
        loadStaff()
    }

    fun loadDoctors() {
        viewModelScope.launch {
            _doctorsState.value = DoctorsListState.Loading
            repository.getDoctors()
                .onSuccess { docs -> _doctorsState.value = DoctorsListState.Success(docs) }
                .onFailure { err -> _doctorsState.value = DoctorsListState.Error(err.message ?: "Failed to load doctors.") }
        }
    }

    fun loadStaff() {
        viewModelScope.launch {
            _staffState.value = StaffListState.Loading
            repository.getUsers()
                .onSuccess { users -> _staffState.value = StaffListState.Success(users) }
                .onFailure { err -> _staffState.value = StaffListState.Error(err.message ?: "Failed to load staff team.") }
        }
    }

    fun createDoctor(fullName: String, specialty: String?) {
        if (fullName.isBlank()) {
            _actionState.value = ActionState.Error("Doctor Name cannot be blank.")
            return
        }

        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            repository.createDoctor(CreateDoctorRequest(fullName.trim(), specialty?.trim()?.ifEmpty { null }))
                .onSuccess {
                    _actionState.value = ActionState.Success("Doctor added successfully!")
                    loadDoctors()
                }
                .onFailure { err ->
                    _actionState.value = ActionState.Error(err.message ?: "Failed to add doctor.")
                }
        }
    }

    fun updateDoctor(id: String, fullName: String, specialty: String?, isActive: Boolean) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val req = UpdateDoctorRequest(
                fullName = fullName.trim().ifEmpty { null },
                specialty = specialty?.trim()?.ifEmpty { null },
                isActive = isActive
            )
            repository.updateDoctor(id, req)
                .onSuccess {
                    _actionState.value = ActionState.Success("Doctor updated successfully!")
                    loadDoctors()
                }
                .onFailure { err ->
                    _actionState.value = ActionState.Error(err.message ?: "Failed to update doctor.")
                }
        }
    }

    fun toggleDoctorStatus(id: String, currentStatus: Boolean) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            repository.updateDoctorStatus(id, !currentStatus)
                .onSuccess {
                    _actionState.value = ActionState.Success("Doctor status updated!")
                    loadDoctors()
                }
                .onFailure { err ->
                    _actionState.value = ActionState.Error(err.message ?: "Failed to update status.")
                }
        }
    }

    fun createUser(fullName: String, email: String, password: String, role: String, phone: String?) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _actionState.value = ActionState.Error("Full Name, Email, and Password are required.")
            return
        }
        if (password.length < 8) {
            _actionState.value = ActionState.Error("Password must be at least 8 characters long.")
            return
        }

        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val req = CreateUserRequest(
                fullName = fullName.trim(),
                email = email.trim(),
                password = password,
                role = role,
                phone = phone?.trim()?.ifEmpty { null }
            )
            repository.createUser(req)
                .onSuccess {
                    _actionState.value = ActionState.Success("Staff member added successfully!")
                    loadStaff()
                }
                .onFailure { err ->
                    _actionState.value = ActionState.Error(err.message ?: "Failed to add staff member.")
                }
        }
    }

    fun updateUser(id: String, fullName: String, phone: String?, role: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val req = UpdateUserRequest(
                fullName = fullName.trim().ifEmpty { null },
                phone = phone?.trim()?.ifEmpty { null },
                role = role
            )
            repository.updateUser(id, req)
                .onSuccess {
                    _actionState.value = ActionState.Success("Staff details updated!")
                    loadStaff()
                }
                .onFailure { err ->
                    _actionState.value = ActionState.Error(err.message ?: "Failed to update staff member.")
                }
        }
    }

    fun toggleUserStatus(id: String, currentStatus: Boolean) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            repository.updateUserStatus(id, !currentStatus)
                .onSuccess {
                    _actionState.value = ActionState.Success("Staff status updated!")
                    loadStaff()
                }
                .onFailure { err ->
                    _actionState.value = ActionState.Error(err.message ?: "Failed to update staff status.")
                }
        }
    }

    fun clearActionState() {
        _actionState.value = ActionState.Idle
    }

    class Factory(private val repository: TeamRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TeamViewModel(repository) as T
        }
    }
}
