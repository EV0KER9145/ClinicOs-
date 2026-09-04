package com.clinicos.app.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clinicos.app.feature.notifications.data.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _notificationsState = MutableStateFlow<NotificationsListState>(NotificationsListState.Loading)
    val notificationsState: StateFlow<NotificationsListState> = _notificationsState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _actionState = MutableStateFlow<NotificationActionState>(NotificationActionState.Idle)
    val actionState: StateFlow<NotificationActionState> = _actionState.asStateFlow()

    init {
        loadNotifications()
        loadUnreadCount()
    }

    fun loadNotifications(unreadOnly: Boolean = false) {
        viewModelScope.launch {
            _notificationsState.value = NotificationsListState.Loading
            repository.getNotifications(page = 1, pageSize = 50, unreadOnly = unreadOnly)
                .onSuccess { res ->
                    _notificationsState.value = NotificationsListState.Success(res.items, res.unreadCount)
                    _unreadCount.value = res.unreadCount
                }
                .onFailure { err ->
                    _notificationsState.value = NotificationsListState.Error(err.message ?: "Failed to load notifications.")
                }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            repository.getUnreadCount()
                .onSuccess { res ->
                    _unreadCount.value = res.count
                }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
                .onSuccess {
                    loadNotifications()
                    loadUnreadCount()
                }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _actionState.value = NotificationActionState.Loading
            repository.markAllNotificationsRead()
                .onSuccess {
                    _actionState.value = NotificationActionState.Success("All notifications marked as read.")
                    _unreadCount.value = 0
                    loadNotifications()
                }
                .onFailure { err ->
                    _actionState.value = NotificationActionState.Error(err.message ?: "Failed to mark all as read.")
                }
        }
    }

    fun clearActionState() {
        _actionState.value = NotificationActionState.Idle
    }

    class Factory(private val repository: NotificationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationViewModel(repository) as T
        }
    }
}
