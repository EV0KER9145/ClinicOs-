package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LeadNoteDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "lead_id") val leadId: String,
    @Json(name = "author_user_id") val authorUserId: String,
    val author: AuthorDto? = null,
    val content: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CreateLeadNoteRequest(
    val content: String
)

@JsonClass(generateAdapter = true)
data class LeadDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "full_name") val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val source: String = "PHONE",
    val status: String = "NEW",
    @Json(name = "assigned_user_id") val assignedUserId: String? = null,
    @Json(name = "interested_service") val interestedService: String? = null,
    val notes: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "converted_patient_id") val convertedPatientId: String? = null,
    @Json(name = "converted_at") val convertedAt: String? = null,
    @Json(name = "assigned_user") val assignedUser: UserDto? = null,
    @Json(name = "converted_patient") val convertedPatient: PatientDto? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class LeadDetailDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "full_name") val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val source: String = "PHONE",
    val status: String = "NEW",
    @Json(name = "assigned_user_id") val assignedUserId: String? = null,
    @Json(name = "interested_service") val interestedService: String? = null,
    val notes: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "converted_patient_id") val convertedPatientId: String? = null,
    @Json(name = "converted_at") val convertedAt: String? = null,
    @Json(name = "assigned_user") val assignedUser: UserDto? = null,
    @Json(name = "converted_patient") val convertedPatient: PatientDto? = null,
    @Json(name = "notes_list") val notesList: List<LeadNoteDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateLeadRequest(
    @Json(name = "full_name") val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    val source: String = "PHONE",
    @Json(name = "assigned_user_id") val assignedUserId: String? = null,
    @Json(name = "interested_service") val interestedService: String? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateLeadRequest(
    @Json(name = "full_name") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val source: String? = null,
    @Json(name = "assigned_user_id") val assignedUserId: String? = null,
    @Json(name = "interested_service") val interestedService: String? = null,
    val notes: String? = null,
    @Json(name = "is_active") val isActive: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class LeadStatusRequest(
    val status: String
)

@JsonClass(generateAdapter = true)
data class ConvertLeadRequest(
    @Json(name = "existing_patient_id") val existingPatientId: String? = null,
    @Json(name = "new_patient") val newPatient: CreatePatientRequest? = null
)

@JsonClass(generateAdapter = true)
data class LeadListResponse(
    val items: List<LeadDto>,
    val total: Int,
    val page: Int,
    @Json(name = "page_size") val pageSize: Int
)
