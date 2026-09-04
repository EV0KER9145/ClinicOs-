package com.clinicos.app.feature.appointments.data

import android.util.Log
import com.clinicos.app.core.network.AppointmentApiService
import com.clinicos.app.core.network.dto.AppointmentDto
import com.clinicos.app.core.network.dto.AppointmentListResponse
import com.clinicos.app.core.network.dto.AppointmentStatusRequest
import com.clinicos.app.core.network.dto.CreateAppointmentRequest
import com.clinicos.app.core.network.dto.UpdateAppointmentRequest
import org.json.JSONObject
import retrofit2.Response

class AppointmentRepository(
    private val apiService: AppointmentApiService
) {

    suspend fun getAppointments(
        page: Int = 1,
        pageSize: Int = 50,
        startDate: String? = null,
        endDate: String? = null,
        doctorId: String? = null,
        patientId: String? = null,
        status: String? = null
    ): Result<AppointmentListResponse> {
        return try {
            val response = apiService.getAppointments(page, pageSize, startDate, endDate, doctorId, patientId, status)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error fetching appointments", e)
            Result.failure(Exception("Unable to load appointments. Please check your network connection."))
        }
    }

    suspend fun createAppointment(request: CreateAppointmentRequest): Result<AppointmentDto> {
        return try {
            val response = apiService.createAppointment(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error creating appointment", e)
            Result.failure(Exception("Unable to book appointment. Please try again."))
        }
    }

    suspend fun getAppointment(id: String): Result<AppointmentDto> {
        return try {
            val response = apiService.getAppointment(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error fetching appointment details", e)
            Result.failure(Exception("Unable to load appointment details."))
        }
    }

    suspend fun updateAppointment(id: String, request: UpdateAppointmentRequest): Result<AppointmentDto> {
        return try {
            val response = apiService.updateAppointment(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error updating appointment", e)
            Result.failure(Exception("Unable to update appointment."))
        }
    }

    suspend fun updateAppointmentStatus(id: String, status: String): Result<AppointmentDto> {
        return try {
            val response = apiService.updateAppointmentStatus(id, AppointmentStatusRequest(status))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error updating appointment status", e)
            Result.failure(Exception("Unable to update appointment status."))
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val json = JSONObject(errorBody)
                if (json.has("detail")) {
                    val detail = json.get("detail")
                    if (detail is String) return detail
                    if (detail is org.json.JSONArray && detail.length() > 0) {
                        val firstErr = detail.getJSONObject(0)
                        return firstErr.optString("msg", "Validation error occurred.")
                    }
                }
            }
            when (response.code()) {
                400 -> "Invalid appointment details or inactive patient/doctor."
                401 -> "Session expired. Please log in again."
                403 -> "You do not have permission to perform this action."
                404 -> "Appointment record not found."
                409 -> "Doctor already has an active appointment during this time window."
                else -> "Server error (${response.code()}). Please try again."
            }
        } catch (e: Exception) {
            "An unexpected error occurred (${response.code()})."
        }
    }
}
