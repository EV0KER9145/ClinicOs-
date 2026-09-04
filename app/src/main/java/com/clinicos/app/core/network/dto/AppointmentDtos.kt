package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PatientSummaryDto(
    val id: String,
    @Json(name = "full_name") val fullName: String,
    val phone: String? = null
)

@JsonClass(generateAdapter = true)
data class DoctorSummaryDto(
    val id: String,
    @Json(name = "full_name") val fullName: String,
    val specialty: String? = null
)

@JsonClass(generateAdapter = true)
data class AppointmentDto(
    val id: String,
    @Json(name = "clinic_id") val clinicId: String,
    @Json(name = "patient_id") val patientId: String,
    @Json(name = "doctor_id") val doctorId: String,
    val patient: PatientSummaryDto,
    val doctor: DoctorSummaryDto,
    @Json(name = "scheduled_at") val scheduledAt: String,
    @Json(name = "duration_minutes") val durationMinutes: Int = 30,
    val status: String = "SCHEDULED",
    val notes: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateAppointmentRequest(
    @Json(name = "patient_id") val patientId: String,
    @Json(name = "doctor_id") val doctorId: String,
    @Json(name = "scheduled_at") val scheduledAt: String,
    @Json(name = "duration_minutes") val durationMinutes: Int = 30,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateAppointmentRequest(
    @Json(name = "patient_id") val patientId: String? = null,
    @Json(name = "doctor_id") val doctorId: String? = null,
    @Json(name = "scheduled_at") val scheduledAt: String? = null,
    @Json(name = "duration_minutes") val durationMinutes: Int? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class AppointmentStatusRequest(
    val status: String
)

@JsonClass(generateAdapter = true)
data class AppointmentListResponse(
    val items: List<AppointmentDto>,
    val total: Int,
    val page: Int,
    @Json(name = "page_size") val pageSize: Int
)
