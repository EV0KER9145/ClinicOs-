package com.clinicos.app.feature.dashboard.data

import android.util.Log
import com.clinicos.app.core.network.DashboardApiService
import com.clinicos.app.core.network.dto.DashboardSummaryResponse
import org.json.JSONObject
import retrofit2.Response

class DashboardRepository(
    private val apiService: DashboardApiService
) {

    suspend fun getSummary(): Result<DashboardSummaryResponse> {
        return try {
            val response = apiService.getDashboardSummary()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("DashboardRepository", "Error fetching dashboard summary", e)
            Result.failure(Exception("Unable to connect to ClinicOS. Please check your network connection."))
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
                }
            }
            when (response.code()) {
                401 -> "Session expired. Please log in again."
                else -> "Server error (${response.code()}). Please try again."
            }
        } catch (e: Exception) {
            "An unexpected error occurred (${response.code()})."
        }
    }
}
