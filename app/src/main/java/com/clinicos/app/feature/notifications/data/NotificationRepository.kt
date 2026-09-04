package com.clinicos.app.feature.notifications.data

import android.util.Log
import com.clinicos.app.core.network.NotificationApiService
import com.clinicos.app.core.network.dto.NotificationDto
import com.clinicos.app.core.network.dto.NotificationListResponse
import com.clinicos.app.core.network.dto.UnreadCountResponse
import org.json.JSONObject
import retrofit2.Response

class NotificationRepository(
    private val apiService: NotificationApiService
) {

    suspend fun getNotifications(
        page: Int = 1,
        pageSize: Int = 50,
        unreadOnly: Boolean = false,
        typeFilter: String? = null
    ): Result<NotificationListResponse> {
        return try {
            val response = apiService.getNotifications(page, pageSize, unreadOnly, typeFilter)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error fetching notifications", e)
            Result.failure(Exception("Unable to load notifications. Please check your network connection."))
        }
    }

    suspend fun getUnreadCount(): Result<UnreadCountResponse> {
        return try {
            val response = apiService.getUnreadCount()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error fetching unread count", e)
            Result.failure(Exception("Unable to check unread notifications."))
        }
    }

    suspend fun markNotificationRead(id: String): Result<NotificationDto> {
        return try {
            val response = apiService.markNotificationRead(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error marking notification as read", e)
            Result.failure(Exception("Unable to update notification."))
        }
    }

    suspend fun markAllNotificationsRead(): Result<UnreadCountResponse> {
        return try {
            val response = apiService.markAllNotificationsRead()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error marking all notifications read", e)
            Result.failure(Exception("Unable to mark all as read."))
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val json = JSONObject(errorBody)
                if (json.has("detail")) {
                    val detail = json.get("detail")
                    if (detail is String) return detail
                }
            }
            when (response.code()) {
                401 -> "Session expired. Please log in again."
                404 -> "Notification not found."
                else -> "Server error (${response.code()}). Please try again."
            }
        } catch (e: Exception) {
            "An unexpected error occurred (${response.code()})."
        }
    }
}
