package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.AnalyticsQueryRequest
import com.clinicos.app.core.network.dto.AnalyticsQueryResponse
import com.clinicos.app.core.network.dto.ClinicInsightsResponse
import com.clinicos.app.core.network.dto.FollowUpRecommendationsResponse
import com.clinicos.app.core.network.dto.GenerateMessageRequest
import com.clinicos.app.core.network.dto.GenerateMessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AiApiService {

    @POST("api/v1/ai/follow-up-recommendations")
    suspend fun getFollowUpRecommendations(
        @Query("limit") limit: Int = 10
    ): Response<FollowUpRecommendationsResponse>

    @POST("api/v1/ai/generate-message")
    suspend fun generateMessage(
        @Body request: GenerateMessageRequest
    ): Response<GenerateMessageResponse>

    @GET("api/v1/ai/clinic-insights")
    suspend fun getClinicInsights(): Response<ClinicInsightsResponse>

    @POST("api/v1/ai/analytics/query")
    suspend fun queryAnalytics(
        @Body request: AnalyticsQueryRequest
    ): Response<AnalyticsQueryResponse>
}
