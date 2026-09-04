package com.clinicos.app.feature.automation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.feature.automation.data.AutomationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AutomationViewModel(
    private val repository: AutomationRepository
) : ViewModel() {

    private val _draftsState = MutableStateFlow<DraftsListState>(DraftsListState.Loading)
    val draftsState: StateFlow<DraftsListState> = _draftsState.asStateFlow()

    private val _actionState = MutableStateFlow<DraftActionState>(DraftActionState.Idle)
    val actionState: StateFlow<DraftActionState> = _actionState.asStateFlow()

    init {
        evaluateTimeRulesAndLoadDrafts()
    }

    fun evaluateTimeRulesAndLoadDrafts() {
        viewModelScope.launch {
            _draftsState.value = DraftsListState.Loading
            repository.evaluateTimeRules()
            loadDrafts()
        }
    }

    fun loadDrafts(status: String? = "DRAFT") {
        viewModelScope.launch {
            _draftsState.value = DraftsListState.Loading
            repository.getCommunicationDrafts(status = status)
                .onSuccess { res ->
                    _draftsState.value = DraftsListState.Success(res.items, res.total)
                }
                .onFailure { err ->
                    _draftsState.value = DraftsListState.Error(err.message ?: "Failed to load prepared drafts.")
                }
        }
    }

    fun approveDraft(id: String) {
        viewModelScope.launch {
            _actionState.value = DraftActionState.Loading
            repository.updateDraftStatus(id, "APPROVED")
                .onSuccess {
                    _actionState.value = DraftActionState.Success("Draft approved!")
                    loadDrafts()
                }
                .onFailure { err ->
                    _actionState.value = DraftActionState.Error(err.message ?: "Failed to approve draft.")
                }
        }
    }

    fun discardDraft(id: String) {
        viewModelScope.launch {
            _actionState.value = DraftActionState.Loading
            repository.updateDraftStatus(id, "DISCARDED")
                .onSuccess {
                    _actionState.value = DraftActionState.Success("Draft discarded.")
                    loadDrafts()
                }
                .onFailure { err ->
                    _actionState.value = DraftActionState.Error(err.message ?: "Failed to discard draft.")
                }
        }
    }

    fun clearActionState() {
        _actionState.value = DraftActionState.Idle
    }

    class Factory(private val repository: AutomationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AutomationViewModel(repository) as T
        }
    }
}
