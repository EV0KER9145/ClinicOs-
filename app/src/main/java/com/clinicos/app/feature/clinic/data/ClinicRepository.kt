package com.clinicos.app.feature.clinic.data

import android.util.Log
import com.clinicos.app.core.network.ClinicApiService
import com.clinicos.app.core.network.dto.ClinicDto
import com.clinicos.app.core.network.dto.UpdateClinicRequest
import org.json.JSONObject
import retrofit2.Response

class ClinicRepository(
    private val apiService: ClinicApiService
) {

    suspend fun getMyClinic(): Result<ClinicDto> {
        return try {
            val response = apiService.getMyClinic()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ClinicRepository", "Error fetching clinic profile", e)
            val msg = e.localizedMessage ?: e.message ?: "Server unreachable"
            Result.failure(Exception("Unable to connect to ClinicOS ($msg). Please check your connection."))
        }
    }

    suspend fun updateMyClinic(request: UpdateClinicRequest): Result<ClinicDto> {
        return try {
            val response = apiService.updateMyClinic(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ClinicRepository", "Error updating clinic profile", e)
            val msg = e.localizedMessage ?: e.message ?: "Server unreachable"
            Result.failure(Exception("Unable to save clinic details ($msg). Please try again."))
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
                401 -> "Session expired. Please log in again."
                403 -> "You do not have permission to update clinic details."
                404 -> "Clinic profile not found."
                else -> "Server error (${response.code()}). Please try again."
            }
        } catch (e: Exception) {
            "An unexpected error occurred (${response.code()})."
        }
    }
}
