package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AutomationExecutionDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "event_type") val eventType: String,
    @Json(name = "rule_type") val ruleType: String,
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: String,
    @Json(name = "action_type") val actionType: String,
    val status: String,
    @Json(name = "idempotency_key") val idempotencyKey: String,
    @Json(name = "executed_at") val executedAt: String,
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class AutomationExecutionListResponse(
    val items: List<AutomationExecutionDto>,
    val total: Int,
    val page: Int,
    @Json(name = "page_size") val pageSize: Int
)

@JsonClass(generateAdapter = true)
data class CommunicationDraftDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: String,
    @Json(name = "recipient_name") val recipientName: String,
    @Json(name = "recipient_phone") val recipientPhone: String? = null,
    val purpose: String,
    @Json(name = "message_content") val messageContent: String,
    val status: String = "DRAFT",
    @Json(name = "created_by_user_id") val createdByUserId: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class CommunicationDraftListResponse(
    val items: List<CommunicationDraftDto>,
    val total: Int,
    val page: Int,
    @Json(name = "page_size") val pageSize: Int
)

@JsonClass(generateAdapter = true)
data class DraftStatusUpdate(
    val status: String
)
