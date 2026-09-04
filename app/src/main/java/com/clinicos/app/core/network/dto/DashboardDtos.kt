package com.clinicos.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TodayMetricsDto(
    val date: String,
    @Json(name = "appointments_count") val appointmentsCount: Int = 0,
    @Json(name = "completed_appointments") val completedAppointments: Int = 0,
    @Json(name = "no_shows_count") val noShowsCount: Int = 0,
    @Json(name = "new_leads_count") val newLeadsCount: Int = 0,
    @Json(name = "new_patients_count") val newPatientsCount: Int = 0,
    @Json(name = "pending_follow_ups_count") val pendingFollowUpsCount: Int = 0,
    @Json(name = "overdue_follow_ups_count") val overdueFollowUpsCount: Int = 0,
    @Json(name = "due_today_follow_ups_count") val dueTodayFollowUpsCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class AttentionSummaryDto(
    @Json(name = "overdue_follow_ups") val overdueFollowUps: Int = 0,
    @Json(name = "no_shows_today") val noShowsToday: Int = 0,
    @Json(name = "new_leads") val newLeads: Int = 0
)

@JsonClass(generateAdapter = true)
data class UpcomingAppointmentSummaryDto(
    val id: String,
    @Json(name = "scheduled_at") val scheduledAt: String,
    @Json(name = "duration_minutes") val durationMinutes: Int = 30,
    @Json(name = "patient_name") val patientName: String,
    @Json(name = "doctor_name") val doctorName: String,
    val status: String = "SCHEDULED"
)

@JsonClass(generateAdapter = true)
data class RecentNoShowSummaryDto(
    val id: String,
    @Json(name = "scheduled_at") val scheduledAt: String,
    @Json(name = "patient_name") val patientName: String,
    @Json(name = "doctor_name") val doctorName: String
)

@JsonClass(generateAdapter = true)
data class DashboardSummaryResponse(
    @Json(name = "clinic_name") val clinicName: String,
    @Json(name = "today_date") val todayDate: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "user_role") val userRole: String,
    val today: TodayMetricsDto,
    val attention: AttentionSummaryDto,
    @Json(name = "upcoming_appointments") val upcomingAppointments: List<UpcomingAppointmentSummaryDto> = emptyList(),
    @Json(name = "recent_no_shows") val recentNoShows: List<RecentNoShowSummaryDto> = emptyList()
)
