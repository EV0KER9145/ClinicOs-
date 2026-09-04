package com.clinicos.app.feature.leads.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.core.network.dto.ConvertLeadRequest
import com.clinicos.app.core.network.dto.CreateLeadRequest
import com.clinicos.app.core.network.dto.CreatePatientRequest
import com.clinicos.app.core.network.dto.UpdateLeadRequest
import com.clinicos.app.feature.leads.data.LeadRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeadViewModel(
    private val repository: LeadRepository
) : ViewModel() {

    private val _leadsState = MutableStateFlow<LeadsListState>(LeadsListState.Loading)
    val leadsState: StateFlow<LeadsListState> = _leadsState.asStateFlow()

    private val _detailState = MutableStateFlow<LeadDetailState>(LeadDetailState.Loading)
    val detailState: StateFlow<LeadDetailState> = _detailState.asStateFlow()

    private val _actionState = MutableStateFlow<LeadActionState>(LeadActionState.Idle)
    val actionState: StateFlow<LeadActionState> = _actionState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    private val _sourceFilter = MutableStateFlow<String?>(null)
    val sourceFilter: StateFlow<String?> = _sourceFilter.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        loadLeads()
    }

    fun loadLeads() {
        viewModelScope.launch {
            _leadsState.value = LeadsListState.Loading
            repository.getLeads(
                page = 1,
                pageSize = 50,
                search = _searchQuery.value.ifBlank { null },
                status = _statusFilter.value,
                source = _sourceFilter.value
            )
                .onSuccess { res -> _leadsState.value = LeadsListState.Success(res.items, res.total) }
                .onFailure { err -> _leadsState.value = LeadsListState.Error(err.message ?: "Failed to load leads.") }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(350)
            loadLeads()
        }
    }

    fun onStatusFilterChanged(status: String?) {
        _statusFilter.value = status
        loadLeads()
    }

    fun onSourceFilterChanged(source: String?) {
        _sourceFilter.value = source
        loadLeads()
    }

    fun loadLeadDetail(leadId: String) {
        viewModelScope.launch {
            _detailState.value = LeadDetailState.Loading
            repository.getLeadDetail(leadId)
                .onSuccess { detail -> _detailState.value = LeadDetailState.Success(detail) }
                .onFailure { err -> _detailState.value = LeadDetailState.Error(err.message ?: "Failed to load lead details.") }
        }
    }

    fun createLead(
        fullName: String,
        phone: String?,
        email: String?,
        source: String,
        interestedService: String?,
        assignedUserId: String?,
        notes: String?
    ) {
        if (fullName.isBlank()) {
            _actionState.value = LeadActionState.Error("Lead Name cannot be blank.")
            return
        }

        viewModelScope.launch {
            _actionState.value = LeadActionState.Loading
            val req = CreateLeadRequest(
                fullName = fullName.trim(),
                phone = phone?.trim()?.ifEmpty { null },
                email = email?.trim()?.ifEmpty { null },
                source = source,
                assignedUserId = assignedUserId,
                interestedService = interestedService?.trim()?.ifEmpty { null },
                notes = notes?.trim()?.ifEmpty { null }
            )

            repository.createLead(req)
                .onSuccess {
                    _actionState.value = LeadActionState.Success("Enquiry lead captured!")
                    loadLeads()
                }
                .onFailure { err ->
                    _actionState.value = LeadActionState.Error(err.message ?: "Failed to capture lead.")
                }
        }
    }

    fun updateLead(
        leadId: String,
        fullName: String,
        phone: String?,
        email: String?,
        source: String?,
        assignedUserId: String?,
        interestedService: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _actionState.value = LeadActionState.Loading
            val req = UpdateLeadRequest(
                fullName = fullName.trim().ifEmpty { null },
                phone = phone?.trim()?.ifEmpty { null },
                email = email?.trim()?.ifEmpty { null },
                source = source,
                assignedUserId = assignedUserId,
                interestedService = interestedService?.trim()?.ifEmpty { null },
                notes = notes?.trim()?.ifEmpty { null }
            )

            repository.updateLead(leadId, req)
                .onSuccess {
                    _actionState.value = LeadActionState.Success("Lead details updated!")
                    loadLeadDetail(leadId)
                    loadLeads()
                }
                .onFailure { err ->
                    _actionState.value = LeadActionState.Error(err.message ?: "Failed to update lead.")
                }
        }
    }

    fun updateLeadStatus(leadId: String, newStatus: String) {
        viewModelScope.launch {
            _actionState.value = LeadActionState.Loading
            repository.updateLeadStatus(leadId, newStatus)
                .onSuccess {
                    _actionState.value = LeadActionState.Success("Lead status updated to $newStatus!")
                    loadLeadDetail(leadId)
                    loadLeads()
                }
                .onFailure { err ->
                    _actionState.value = LeadActionState.Error(err.message ?: "Failed to update lead status.")
                }
        }
    }

    fun convertLeadToNewPatient(
        leadId: String,
        patientName: String,
        phone: String?,
        email: String?,
        address: String?
    ) {
        viewModelScope.launch {
            _actionState.value = LeadActionState.Loading
            val req = ConvertLeadRequest(
                newPatient = CreatePatientRequest(
                    fullName = patientName.trim(),
                    phone = phone?.trim()?.ifEmpty { null },
                    email = email?.trim()?.ifEmpty { null },
                    address = address?.trim()?.ifEmpty { null }
                )
            )

            repository.convertLead(leadId, req)
                .onSuccess { convertedLead ->
                    _actionState.value = LeadActionState.Success(
                        message = "Lead successfully converted to patient!",
                        convertedPatientId = convertedLead.convertedPatientId
                    )
                    loadLeadDetail(leadId)
                    loadLeads()
                }
                .onFailure { err ->
                    _actionState.value = LeadActionState.Error(err.message ?: "Failed to convert lead.")
                }
        }
    }

    fun convertLeadToExistingPatient(leadId: String, existingPatientId: String) {
        viewModelScope.launch {
            _actionState.value = LeadActionState.Loading
            val req = ConvertLeadRequest(existingPatientId = existingPatientId)

            repository.convertLead(leadId, req)
                .onSuccess { convertedLead ->
                    _actionState.value = LeadActionState.Success(
                        message = "Lead linked and converted to patient!",
                        convertedPatientId = convertedLead.convertedPatientId
                    )
                    loadLeadDetail(leadId)
                    loadLeads()
                }
                .onFailure { err ->
                    _actionState.value = LeadActionState.Error(err.message ?: "Failed to convert lead.")
                }
        }
    }

    fun addLeadNote(leadId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _actionState.value = LeadActionState.Loading
            repository.createLeadNote(leadId, content.trim())
                .onSuccess {
                    _actionState.value = LeadActionState.Success("Note added!")
                    loadLeadDetail(leadId)
                }
                .onFailure { err ->
                    _actionState.value = LeadActionState.Error(err.message ?: "Failed to add note.")
                }
        }
    }

    fun clearActionState() {
        _actionState.value = LeadActionState.Idle
    }

    class Factory(private val repository: LeadRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LeadViewModel(repository) as T
        }
    }
}
