package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.CreateFollowUpRequest
import com.clinicos.app.core.network.dto.FollowUpDto
import com.clinicos.app.core.network.dto.FollowUpListResponse
import com.clinicos.app.core.network.dto.UpdateFollowUpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FollowUpApiService {

    @GET("api/v1/follow-ups")
    suspend fun getFollowUps(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("status") status: String? = null,
        @Query("assigned_user_id") assignedUserId: String? = null,
        @Query("patient_id") patientId: String? = null,
        @Query("lead_id") leadId: String? = null,
        @Query("overdue_only") overdueOnly: Boolean = false,
        @Query("due_from") dueFrom: String? = null,
        @Query("due_to") dueTo: String? = null
    ): Response<FollowUpListResponse>

    @POST("api/v1/follow-ups")
    suspend fun createFollowUp(
        @Body request: CreateFollowUpRequest
    ): Response<FollowUpDto>

    @GET("api/v1/follow-ups/{id}")
    suspend fun getFollowUp(
        @Path("id") id: String
    ): Response<FollowUpDto>

    @PATCH("api/v1/follow-ups/{id}")
    suspend fun updateFollowUp(
        @Path("id") id: String,
        @Body request: UpdateFollowUpRequest
    ): Response<FollowUpDto>

    @PATCH("api/v1/follow-ups/{id}/complete")
    suspend fun completeFollowUp(
        @Path("id") id: String
    ): Response<FollowUpDto>

    @PATCH("api/v1/follow-ups/{id}/cancel")
    suspend fun cancelFollowUp(
        @Path("id") id: String
    ): Response<FollowUpDto>
}
