package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FollowUpRecommendationDto(
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: String,
    val category: String,
    val priority: String = "MEDIUM",
    @Json(name = "entity_name") val entityName: String,
    val reason: String,
    @Json(name = "suggested_action") val suggestedAction: String,
    @Json(name = "suggested_message") val suggestedMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class FollowUpRecommendationsResponse(
    val recommendations: List<FollowUpRecommendationDto> = emptyList(),
    @Json(name = "total_candidates") val totalCandidates: Int = 0
)

@JsonClass(generateAdapter = true)
data class GenerateMessageRequest(
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: String,
    val purpose: String,
    val tone: String = "FRIENDLY",
    @Json(name = "additional_context") val additionalContext: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateMessageResponse(
    @Json(name = "entity_type") val entityType: String,
    @Json(name = "entity_id") val entityId: String,
    @Json(name = "recipient_name") val recipientName: String,
    val message: String,
    val purpose: String,
    val tone: String
)

@JsonClass(generateAdapter = true)
data class ClinicInsightDto(
    val title: String,
    val category: String,
    val priority: String = "MEDIUM",
    val summary: String,
    @Json(name = "why_it_matters") val whyItMatters: String,
    @Json(name = "suggested_action") val suggestedAction: String,
    @Json(name = "action_route") val actionRoute: String? = null
)

@JsonClass(generateAdapter = true)
data class ClinicInsightsResponse(
    @Json(name = "clinic_name") val clinicName: String,
    val period: String,
    val insights: List<ClinicInsightDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AnalyticsQueryRequest(
    val question: String,
    val period: String? = null
)

@JsonClass(generateAdapter = true)
data class AnalyticsQueryResponse(
    val question: String,
    val intent: String,
    val period: String,
    val answer: String,
    @Json(name = "suggested_actions") val suggestedActions: List<String> = emptyList()
)
