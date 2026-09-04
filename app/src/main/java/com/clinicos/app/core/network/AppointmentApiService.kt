package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.AppointmentDto
import com.clinicos.app.core.network.dto.AppointmentListResponse
import com.clinicos.app.core.network.dto.AppointmentStatusRequest
import com.clinicos.app.core.network.dto.CreateAppointmentRequest
import com.clinicos.app.core.network.dto.UpdateAppointmentRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppointmentApiService {

    @GET("api/v1/appointments")
    suspend fun getAppointments(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("doctor_id") doctorId: String? = null,
        @Query("patient_id") patientId: String? = null,
        @Query("status") status: String? = null
    ): Response<AppointmentListResponse>

    @POST("api/v1/appointments")
    suspend fun createAppointment(
        @Body request: CreateAppointmentRequest
    ): Response<AppointmentDto>

    @GET("api/v1/appointments/{id}")
    suspend fun getAppointment(
        @Path("id") id: String
    ): Response<AppointmentDto>

    @PATCH("api/v1/appointments/{id}")
    suspend fun updateAppointment(
        @Path("id") id: String,
        @Body request: UpdateAppointmentRequest
    ): Response<AppointmentDto>

    @PATCH("api/v1/appointments/{id}/status")
    suspend fun updateAppointmentStatus(
        @Path("id") id: String,
        @Body request: AppointmentStatusRequest
    ): Response<AppointmentDto>
}
