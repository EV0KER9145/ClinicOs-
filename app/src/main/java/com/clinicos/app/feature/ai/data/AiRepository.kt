package com.clinicos.app.feature.ai.data

import android.util.Log
import com.clinicos.app.core.network.AiApiService
import com.clinicos.app.core.network.dto.AnalyticsQueryRequest
import com.clinicos.app.core.network.dto.AnalyticsQueryResponse
import com.clinicos.app.core.network.dto.ClinicInsightsResponse
import com.clinicos.app.core.network.dto.FollowUpRecommendationsResponse
import com.clinicos.app.core.network.dto.GenerateMessageRequest
import com.clinicos.app.core.network.dto.GenerateMessageResponse
import org.json.JSONObject
import retrofit2.Response

class AiRepository(
    private val apiService: AiApiService
) {

    suspend fun getRecommendations(limit: Int = 10): Result<FollowUpRecommendationsResponse> {
        return try {
            val response = apiService.getFollowUpRecommendations(limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "Error fetching AI recommendations", e)
            Result.failure(Exception("Unable to load AI recommendations. Please check your network connection."))
        }
    }

    suspend fun generateMessage(request: GenerateMessageRequest): Result<GenerateMessageResponse> {
        return try {
            val response = apiService.generateMessage(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "Error generating AI message", e)
            Result.failure(Exception("Unable to generate AI message."))
        }
    }

    suspend fun getClinicInsights(): Result<ClinicInsightsResponse> {
        return try {
            val response = apiService.getClinicInsights()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "Error fetching AI insights", e)
            Result.failure(Exception("Unable to load AI clinic insights."))
        }
    }

    suspend fun queryAnalytics(request: AnalyticsQueryRequest): Result<AnalyticsQueryResponse> {
        return try {
            val response = apiService.queryAnalytics(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "Error querying AI analytics", e)
            Result.failure(Exception("Unable to process natural language analytics question."))
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
