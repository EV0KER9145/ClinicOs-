package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class CreateTagRequest(
    val name: String
)

@JsonClass(generateAdapter = true)
data class AuthorDto(
    val id: String,
    @Json(name = "full_name") val fullName: String,
    val email: String
)

@JsonClass(generateAdapter = true)
data class PatientNoteDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "patient_id") val patientId: String,
    @Json(name = "author_user_id") val authorUserId: String,
    val author: AuthorDto? = null,
    val content: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CreatePatientNoteRequest(
    val content: String
)

@JsonClass(generateAdapter = true)
data class PatientDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "full_name") val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    @Json(name = "date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    val tags: List<TagDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PatientDetailDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "full_name") val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    @Json(name = "date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    val tags: List<TagDto> = emptyList(),
    @Json(name = "notes_list") val notesList: List<PatientNoteDto> = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreatePatientRequest(
    @Json(name = "full_name") val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    @Json(name = "date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @Json(name = "tag_ids") val tagIds: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class UpdatePatientRequest(
    @Json(name = "full_name") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @Json(name = "date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @Json(name = "is_active") val isActive: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class UpdatePatientTagsRequest(
    @Json(name = "tag_ids") val tagIds: List<String>
)

@JsonClass(generateAdapter = true)
data class PatientListResponse(
    val items: List<PatientDto>,
    val total: Int,
    val page: Int,
    @Json(name = "page_size") val pageSize: Int
)

@JsonClass(generateAdapter = true)
data class DuplicateCheckResponse(
    @Json(name = "has_duplicates") val hasDuplicates: Boolean,
    @Json(name = "possible_matches") val possibleMatches: List<PatientDto> = emptyList()
)
