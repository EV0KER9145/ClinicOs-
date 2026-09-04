package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "user_id") val userId: String,
    val title: String,
    val message: String,
    val type: String = "GENERAL",
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "read_at") val readAt: String? = null,
    @Json(name = "entity_type") val entityType: String? = null,
    @Json(name = "entity_id") val entityId: String? = null,
    @Json(name = "action_route") val actionRoute: String? = null,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class NotificationListResponse(
    val items: List<NotificationDto>,
    val total: Int,
    @Json(name = "unread_count") val unreadCount: Int,
    val page: Int,
    @Json(name = "page_size") val pageSize: Int
)

@JsonClass(generateAdapter = true)
data class UnreadCountResponse(
    val count: Int
)
