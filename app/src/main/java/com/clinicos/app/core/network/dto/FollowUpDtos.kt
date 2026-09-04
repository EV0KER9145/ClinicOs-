package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LeadSummaryDto(
    val id: String,
    @Json(name = "full_name") val fullName: String,
    val phone: String? = null
)

@JsonClass(generateAdapter = true)
data class FollowUpDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "assigned_user_id") val assignedUserId: String,
    @Json(name = "patient_id") val patientId: String? = null,
    @Json(name = "lead_id") val leadId: String? = null,
    val title: String,
    val notes: String? = null,
    @Json(name = "due_at") val dueAt: String,
    val status: String = "PENDING",
    @Json(name = "is_overdue") val isOverdue: Boolean = false,
    @Json(name = "completed_at") val completedAt: String? = null,
    @Json(name = "cancelled_at") val cancelledAt: String? = null,
    @Json(name = "assigned_user") val assignedUser: UserDto? = null,
    val patient: PatientSummaryDto? = null,
    val lead: LeadSummaryDto? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateFollowUpRequest(
    val title: String,
    @Json(name = "due_at") val dueAt: String,
    val notes: String? = null,
    @Json(name = "assigned_user_id") val assignedUserId: String? = null,
    @Json(name = "patient_id") val patientId: String? = null,
    @Json(name = "lead_id") val leadId: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateFollowUpRequest(
    val title: String? = null,
    @Json(name = "due_at") val dueAt: String? = null,
    val notes: String? = null,
    @Json(name = "assigned_user_id") val assignedUserId: String? = null
)

@JsonClass(generateAdapter = true)
data class FollowUpListResponse(
    val items: List<FollowUpDto>,
    val total: Int,
    val page: Int,
    @Json(name = "page_size") val pageSize: Int
)
