package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.NotificationDto
import com.clinicos.app.core.network.dto.NotificationListResponse
import com.clinicos.app.core.network.dto.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {

    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("unread_only") unreadOnly: Boolean = false,
        @Query("type") typeFilter: String? = null
    ): Response<NotificationListResponse>

    @GET("api/v1/notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    @PATCH("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Path("id") id: String
    ): Response<NotificationDto>

    @PATCH("api/v1/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<UnreadCountResponse>
}
