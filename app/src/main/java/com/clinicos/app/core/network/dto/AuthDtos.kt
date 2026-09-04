package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "clinic_name") val clinicName: String,
    @Json(name = "clinic_type") val clinicType: String? = null,
    @Json(name = "clinic_phone") val clinicPhone: String? = null,
    @Json(name = "clinic_email") val clinicEmail: String? = null,
    @Json(name = "full_name") val fullName: String,
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class ClinicDto(
    val id: String,
    val name: String,
    @Json(name = "clinic_type") val clinicType: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val timezone: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "full_name") val fullName: String,
    val email: String,
    val role: String,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    val clinic: ClinicDto,
    val user: UserDto
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer"
)

@JsonClass(generateAdapter = true)
data class CurrentUserResponse(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "full_name") val fullName: String,
    val email: String,
    val role: String
)
