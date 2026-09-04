package com.clinicos.app.feature.notifications.presentation

import com.clinicos.app.core.network.dto.NotificationDto

sealed interface NotificationsListState {
    object Loading : NotificationsListState
    data class Success(val notifications: List<NotificationDto>, val unreadCount: Int) : NotificationsListState
    data class Error(val message: String) : NotificationsListState
}

sealed interface NotificationActionState {
    object Idle : NotificationActionState
    object Loading : NotificationActionState
    data class Success(val message: String) : NotificationActionState
    data class Error(val message: String) : NotificationActionState
}
