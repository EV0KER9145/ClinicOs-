package com.clinicos.app.feature.automation.data

import android.util.Log
import com.clinicos.app.core.network.AutomationApiService
import com.clinicos.app.core.network.dto.CommunicationDraftDto
import com.clinicos.app.core.network.dto.CommunicationDraftListResponse
import com.clinicos.app.core.network.dto.DraftStatusUpdate
import org.json.JSONObject
import retrofit2.Response

class AutomationRepository(
    private val apiService: AutomationApiService
) {

    suspend fun evaluateTimeRules(): Result<Unit> {
        return try {
            val response = apiService.evaluateTimeRules()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AutomationRepository", "Error evaluating time rules", e)
            Result.failure(Exception("Unable to evaluate automation rules."))
        }
    }

    suspend fun getCommunicationDrafts(status: String? = null): Result<CommunicationDraftListResponse> {
        return try {
            val response = apiService.getCommunicationDrafts(status = status)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AutomationRepository", "Error fetching communication drafts", e)
            Result.failure(Exception("Unable to load prepared communication drafts."))
        }
    }

    suspend fun updateDraftStatus(id: String, status: String): Result<CommunicationDraftDto> {
        return try {
            val response = apiService.updateDraftStatus(id, DraftStatusUpdate(status))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AutomationRepository", "Error updating draft status", e)
            Result.failure(Exception("Unable to update draft status."))
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
