package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DoctorDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "full_name") val fullName: String,
    val specialty: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CreateDoctorRequest(
    @Json(name = "full_name") val fullName: String,
    val specialty: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateDoctorRequest(
    @Json(name = "full_name") val fullName: String? = null,
    val specialty: String? = null,
    @Json(name = "is_active") val isActive: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class StatusRequest(
    @Json(name = "is_active") val isActive: Boolean
)

@JsonClass(generateAdapter = true)
data class CreateUserRequest(
    @Json(name = "full_name") val fullName: String,
    val email: String,
    val password: String,
    val role: String = "STAFF",
    val phone: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    @Json(name = "full_name") val fullName: String? = null,
    val phone: String? = null,
    val role: String? = null,
    @Json(name = "is_active") val isActive: Boolean? = null
)
