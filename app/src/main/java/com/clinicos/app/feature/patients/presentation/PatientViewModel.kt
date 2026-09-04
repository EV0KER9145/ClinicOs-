package com.clinicos.app.feature.patients.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.core.network.dto.CreatePatientRequest
import com.clinicos.app.core.network.dto.UpdatePatientRequest
import com.clinicos.app.feature.patients.data.PatientRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PatientViewModel(
    private val repository: PatientRepository
) : ViewModel() {

    private val _patientsState = MutableStateFlow<PatientsListState>(PatientsListState.Loading)
    val patientsState: StateFlow<PatientsListState> = _patientsState.asStateFlow()

    private val _detailState = MutableStateFlow<PatientDetailState>(PatientDetailState.Loading)
    val detailState: StateFlow<PatientDetailState> = _detailState.asStateFlow()

    private val _actionState = MutableStateFlow<PatientActionState>(PatientActionState.Idle)
    val actionState: StateFlow<PatientActionState> = _actionState.asStateFlow()

    private val _tagsState = MutableStateFlow(AvailableTagsState())
    val tagsState: StateFlow<AvailableTagsState> = _tagsState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        loadPatients()
        loadTags()
    }

    fun loadPatients(search: String? = null) {
        viewModelScope.launch {
            _patientsState.value = PatientsListState.Loading
            repository.getPatients(page = 1, pageSize = 50, search = search)
                .onSuccess { res ->
                    _patientsState.value = PatientsListState.Success(res.items, res.total)
                }
                .onFailure { err ->
                    _patientsState.value = PatientsListState.Error(err.message ?: "Failed to load patients.")
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(350) // 350ms debounce
            loadPatients(search = query.ifBlank { null })
        }
    }

    fun loadTags() {
        viewModelScope.launch {
            _tagsState.value = _tagsState.value.copy(isLoading = true)
            repository.getTags()
                .onSuccess { tags ->
                    _tagsState.value = AvailableTagsState(tags = tags, isLoading = false)
                }
                .onFailure { err ->
                    _tagsState.value = AvailableTagsState(isLoading = false, errorMessage = err.message)
                }
        }
    }

    fun createTag(name: String, onTagCreated: (String) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createTag(name.trim())
                .onSuccess { tag ->
                    loadTags()
                    onTagCreated(tag.id)
                }
        }
    }

    fun loadPatientDetail(patientId: String) {
        viewModelScope.launch {
            _detailState.value = PatientDetailState.Loading
            repository.getPatientDetail(patientId)
                .onSuccess { detail ->
                    _detailState.value = PatientDetailState.Success(detail)
                }
                .onFailure { err ->
                    _detailState.value = PatientDetailState.Error(err.message ?: "Failed to load patient detail.")
                }
        }
    }

    fun createPatient(
        fullName: String,
        phone: String?,
        email: String?,
        dateOfBirth: String?,
        gender: String?,
        address: String?,
        notes: String?,
        selectedTagIds: List<String>
    ) {
        if (fullName.isBlank()) {
            _actionState.value = PatientActionState.Error("Patient Name cannot be blank.")
            return
        }

        viewModelScope.launch {
            _actionState.value = PatientActionState.Loading
            val req = CreatePatientRequest(
                fullName = fullName.trim(),
                phone = phone?.trim()?.ifEmpty { null },
                email = email?.trim()?.ifEmpty { null },
                dateOfBirth = dateOfBirth?.trim()?.ifEmpty { null },
                gender = gender?.trim()?.ifEmpty { null },
                address = address?.trim()?.ifEmpty { null },
                notes = notes?.trim()?.ifEmpty { null },
                tagIds = selectedTagIds.ifEmpty { null }
            )

            repository.createPatient(req)
                .onSuccess { newPatient ->
                    _actionState.value = PatientActionState.Success("Patient added successfully!", newPatient.id)
                    loadPatients(_searchQuery.value.ifBlank { null })
                }
                .onFailure { err ->
                    _actionState.value = PatientActionState.Error(err.message ?: "Failed to create patient.")
                }
        }
    }

    fun updatePatient(
        patientId: String,
        fullName: String,
        phone: String?,
        email: String?,
        dateOfBirth: String?,
        gender: String?,
        address: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _actionState.value = PatientActionState.Loading
            val req = UpdatePatientRequest(
                fullName = fullName.trim().ifEmpty { null },
                phone = phone?.trim()?.ifEmpty { null },
                email = email?.trim()?.ifEmpty { null },
                dateOfBirth = dateOfBirth?.trim()?.ifEmpty { null },
                gender = gender?.trim()?.ifEmpty { null },
                address = address?.trim()?.ifEmpty { null },
                notes = notes?.trim()?.ifEmpty { null }
            )

            repository.updatePatient(patientId, req)
                .onSuccess { updated ->
                    _actionState.value = PatientActionState.Success("Patient profile updated!")
                    loadPatientDetail(patientId)
                    loadPatients(_searchQuery.value.ifBlank { null })
                }
                .onFailure { err ->
                    _actionState.value = PatientActionState.Error(err.message ?: "Failed to update patient.")
                }
        }
    }

    fun archivePatient(patientId: String) {
        viewModelScope.launch {
            _actionState.value = PatientActionState.Loading
            repository.updatePatientStatus(patientId, isActive = false)
                .onSuccess {
                    _actionState.value = PatientActionState.Success("Patient archived.")
                    loadPatients(_searchQuery.value.ifBlank { null })
                }
                .onFailure { err ->
                    _actionState.value = PatientActionState.Error(err.message ?: "Failed to archive patient.")
                }
        }
    }

    fun updatePatientTags(patientId: String, tagIds: List<String>) {
        viewModelScope.launch {
            _actionState.value = PatientActionState.Loading
            repository.updatePatientTags(patientId, tagIds)
                .onSuccess {
                    _actionState.value = PatientActionState.Success("Tags updated!")
                    loadPatientDetail(patientId)
                    loadPatients(_searchQuery.value.ifBlank { null })
                }
                .onFailure { err ->
                    _actionState.value = PatientActionState.Error(err.message ?: "Failed to update tags.")
                }
        }
    }

    fun addPatientNote(patientId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _actionState.value = PatientActionState.Loading
            repository.createPatientNote(patientId, content.trim())
                .onSuccess {
                    _actionState.value = PatientActionState.Success("Note added!")
                    loadPatientDetail(patientId)
                }
                .onFailure { err ->
                    _actionState.value = PatientActionState.Error(err.message ?: "Failed to add note.")
                }
        }
    }

    fun clearActionState() {
        _actionState.value = PatientActionState.Idle
    }

    class Factory(private val repository: PatientRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PatientViewModel(repository) as T
        }
    }
}
