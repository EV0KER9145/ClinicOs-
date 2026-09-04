package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.AutomationExecutionListResponse
import com.clinicos.app.core.network.dto.CommunicationDraftDto
import com.clinicos.app.core.network.dto.CommunicationDraftListResponse
import com.clinicos.app.core.network.dto.DraftStatusUpdate
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AutomationApiService {

    @POST("api/v1/automations/evaluate-time-rules")
    suspend fun evaluateTimeRules(): Response<Map<String, Any>>

    @GET("api/v1/automations/executions")
    suspend fun getExecutions(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): Response<AutomationExecutionListResponse>

    @GET("api/v1/automations/drafts")
    suspend fun getCommunicationDrafts(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("status") status: String? = null
    ): Response<CommunicationDraftListResponse>

    @PATCH("api/v1/automations/drafts/{id}/status")
    suspend fun updateDraftStatus(
        @Path("id") id: String,
        @Body request: DraftStatusUpdate
    ): Response<CommunicationDraftDto>
}
