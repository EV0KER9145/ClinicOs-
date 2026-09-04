package com.clinicos.app.feature.clinic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.core.network.dto.UpdateClinicRequest
import com.clinicos.app.feature.clinic.data.ClinicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClinicViewModel(
    private val repository: ClinicRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ClinicProfileState>(ClinicProfileState.Loading)
    val profileState: StateFlow<ClinicProfileState> = _profileState.asStateFlow()

    private val _saveState = MutableStateFlow<ClinicSaveState>(ClinicSaveState.Idle)
    val saveState: StateFlow<ClinicSaveState> = _saveState.asStateFlow()

    init {
        loadClinicProfile()
    }

    fun loadClinicProfile() {
        viewModelScope.launch {
            _profileState.value = ClinicProfileState.Loading
            repository.getMyClinic()
                .onSuccess { clinic ->
                    _profileState.value = ClinicProfileState.Success(clinic)
                }
                .onFailure { error ->
                    _profileState.value = ClinicProfileState.Error(
                        error.message ?: "Failed to load clinic profile."
                    )
                }
        }
    }

    fun updateClinicProfile(
        name: String,
        clinicType: String?,
        phone: String?,
        email: String?,
        timezone: String? = "Asia/Kolkata"
    ) {
        if (name.isBlank()) {
            _saveState.value = ClinicSaveState.Error("Clinic Name cannot be blank.")
            return
        }

        viewModelScope.launch {
            _saveState.value = ClinicSaveState.Loading
            val request = UpdateClinicRequest(
                name = name.trim(),
                clinicType = clinicType?.trim()?.ifEmpty { null },
                phone = phone?.trim()?.ifEmpty { null },
                email = email?.trim()?.ifEmpty { null },
                timezone = timezone?.trim()?.ifEmpty { "Asia/Kolkata" }
            )

            repository.updateMyClinic(request)
                .onSuccess { updatedClinic ->
                    _saveState.value = ClinicSaveState.Success(updatedClinic)
                    _profileState.value = ClinicProfileState.Success(updatedClinic)
                }
                .onFailure { error ->
                    _saveState.value = ClinicSaveState.Error(
                        error.message ?: "Failed to update clinic profile."
                    )
                }
        }
    }

    fun clearSaveState() {
        _saveState.value = ClinicSaveState.Idle
    }

    class Factory(private val repository: ClinicRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClinicViewModel(repository) as T
        }
    }
}
