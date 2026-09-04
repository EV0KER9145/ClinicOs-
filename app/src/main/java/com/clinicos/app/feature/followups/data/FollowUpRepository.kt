package com.clinicos.app.feature.followups.data

import android.util.Log
import com.clinicos.app.core.network.FollowUpApiService
import com.clinicos.app.core.network.dto.CreateFollowUpRequest
import com.clinicos.app.core.network.dto.FollowUpDto
import com.clinicos.app.core.network.dto.FollowUpListResponse
import com.clinicos.app.core.network.dto.UpdateFollowUpRequest
import org.json.JSONObject
import retrofit2.Response

class FollowUpRepository(
    private val apiService: FollowUpApiService
) {

    suspend fun getFollowUps(
        page: Int = 1,
        pageSize: Int = 50,
        status: String? = null,
        assignedUserId: String? = null,
        patientId: String? = null,
        leadId: String? = null,
        overdueOnly: Boolean = false,
        dueFrom: String? = null,
        dueTo: String? = null
    ): Result<FollowUpListResponse> {
        return try {
            val response = apiService.getFollowUps(page, pageSize, status, assignedUserId, patientId, leadId, overdueOnly, dueFrom, dueTo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("FollowUpRepository", "Error fetching follow-ups", e)
            Result.failure(Exception("Unable to load follow-ups list. Please check your network connection."))
        }
    }

    suspend fun createFollowUp(request: CreateFollowUpRequest): Result<FollowUpDto> {
        return try {
            val response = apiService.createFollowUp(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("FollowUpRepository", "Error creating follow-up", e)
            Result.failure(Exception("Unable to create follow-up task. Please try again."))
        }
    }

    suspend fun getFollowUp(id: String): Result<FollowUpDto> {
        return try {
            val response = apiService.getFollowUp(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("FollowUpRepository", "Error fetching follow-up details", e)
            Result.failure(Exception("Unable to load follow-up details."))
        }
    }

    suspend fun updateFollowUp(id: String, request: UpdateFollowUpRequest): Result<FollowUpDto> {
        return try {
            val response = apiService.updateFollowUp(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("FollowUpRepository", "Error updating follow-up", e)
            Result.failure(Exception("Unable to update follow-up task."))
        }
    }

    suspend fun completeFollowUp(id: String): Result<FollowUpDto> {
        return try {
            val response = apiService.completeFollowUp(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("FollowUpRepository", "Error completing follow-up", e)
            Result.failure(Exception("Unable to mark follow-up complete."))
        }
    }

    suspend fun cancelFollowUp(id: String): Result<FollowUpDto> {
        return try {
            val response = apiService.cancelFollowUp(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("FollowUpRepository", "Error cancelling follow-up", e)
            Result.failure(Exception("Unable to cancel follow-up."))
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
                400 -> "Invalid operation or relationship."
                401 -> "Session expired. Please log in again."
                403 -> "You do not have permission to perform this action."
                404 -> "Follow-up record not found."
                else -> "Server error (${response.code()}). Please try again."
            }
        } catch (e: Exception) {
            "An unexpected error occurred (${response.code()})."
        }
    }
}
