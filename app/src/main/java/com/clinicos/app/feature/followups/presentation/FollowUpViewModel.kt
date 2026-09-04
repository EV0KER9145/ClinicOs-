package com.clinicos.app.feature.followups.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.core.network.dto.CreateFollowUpRequest
import com.clinicos.app.core.network.dto.UpdateFollowUpRequest
import com.clinicos.app.feature.followups.data.FollowUpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FollowUpViewModel(
    private val repository: FollowUpRepository
) : ViewModel() {

    private val _followUpsState = MutableStateFlow<FollowUpsListState>(FollowUpsListState.Loading)
    val followUpsState: StateFlow<FollowUpsListState> = _followUpsState.asStateFlow()

    private val _actionState = MutableStateFlow<FollowUpActionState>(FollowUpActionState.Idle)
    val actionState: StateFlow<FollowUpActionState> = _actionState.asStateFlow()

    private val _activeFilter = MutableStateFlow("PENDING") // PENDING, OVERDUE, TODAY, UPCOMING, COMPLETED, MY_TASKS
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    init {
        loadFollowUps()
    }

    fun setFilter(filter: String, currentUserId: String? = null) {
        _activeFilter.value = filter
        loadFollowUps(currentUserId)
    }

    fun loadFollowUps(currentUserId: String? = null) {
        viewModelScope.launch {
            _followUpsState.value = FollowUpsListState.Loading

            val status = when (_activeFilter.value) {
                "COMPLETED" -> "COMPLETED"
                "CANCELLED" -> "CANCELLED"
                else -> "PENDING"
            }

            val overdueOnly = _activeFilter.value == "OVERDUE"
            val assignedUserId = if (_activeFilter.value == "MY_TASKS") currentUserId else null

            repository.getFollowUps(
                page = 1,
                pageSize = 100,
                status = status,
                assignedUserId = assignedUserId,
                overdueOnly = overdueOnly
            )
                .onSuccess { res -> _followUpsState.value = FollowUpsListState.Success(res.items, res.total) }
                .onFailure { err -> _followUpsState.value = FollowUpsListState.Error(err.message ?: "Failed to load follow-ups.") }
        }
    }

    fun createFollowUp(
        title: String,
        dueAtIso: String,
        notes: String?,
        assignedUserId: String?,
        patientId: String?,
        leadId: String?
    ) {
        if (title.isBlank()) {
            _actionState.value = FollowUpActionState.Error("Title cannot be blank.")
            return
        }
        if (patientId == null && leadId == null) {
            _actionState.value = FollowUpActionState.Error("Follow-up must be associated with either a Patient or a Lead.")
            return
        }

        viewModelScope.launch {
            _actionState.value = FollowUpActionState.Loading
            val req = CreateFollowUpRequest(
                title = title.trim(),
                dueAt = dueAtIso,
                notes = notes?.trim()?.ifEmpty { null },
                assignedUserId = assignedUserId,
                patientId = patientId,
                leadId = leadId
            )

            repository.createFollowUp(req)
                .onSuccess {
                    _actionState.value = FollowUpActionState.Success("Follow-up task created!")
                    loadFollowUps()
                }
                .onFailure { err ->
                    _actionState.value = FollowUpActionState.Error(err.message ?: "Failed to create follow-up.")
                }
        }
    }

    fun updateFollowUp(
        id: String,
        title: String?,
        dueAtIso: String?,
        notes: String?,
        assignedUserId: String?
    ) {
        viewModelScope.launch {
            _actionState.value = FollowUpActionState.Loading
            val req = UpdateFollowUpRequest(
                title = title?.trim()?.ifEmpty { null },
                dueAt = dueAtIso,
                notes = notes?.trim()?.ifEmpty { null },
                assignedUserId = assignedUserId
            )

            repository.updateFollowUp(id, req)
                .onSuccess {
                    _actionState.value = FollowUpActionState.Success("Follow-up updated!")
                    loadFollowUps()
                }
                .onFailure { err ->
                    _actionState.value = FollowUpActionState.Error(err.message ?: "Failed to update follow-up.")
                }
        }
    }

    fun completeFollowUp(id: String) {
        viewModelScope.launch {
            _actionState.value = FollowUpActionState.Loading
            repository.completeFollowUp(id)
                .onSuccess {
                    _actionState.value = FollowUpActionState.Success("Follow-up marked complete!")
                    loadFollowUps()
                }
                .onFailure { err ->
                    _actionState.value = FollowUpActionState.Error(err.message ?: "Failed to complete follow-up.")
                }
        }
    }

    fun cancelFollowUp(id: String) {
        viewModelScope.launch {
            _actionState.value = FollowUpActionState.Loading
            repository.cancelFollowUp(id)
                .onSuccess {
                    _actionState.value = FollowUpActionState.Success("Follow-up task cancelled.")
                    loadFollowUps()
                }
                .onFailure { err ->
                    _actionState.value = FollowUpActionState.Error(err.message ?: "Failed to cancel follow-up.")
                }
        }
    }

    fun clearActionState() {
        _actionState.value = FollowUpActionState.Idle
    }

    class Factory(private val repository: FollowUpRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FollowUpViewModel(repository) as T
        }
    }
}
