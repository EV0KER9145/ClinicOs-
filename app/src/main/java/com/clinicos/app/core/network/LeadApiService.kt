package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.ConvertLeadRequest
import com.clinicos.app.core.network.dto.CreateLeadNoteRequest
import com.clinicos.app.core.network.dto.CreateLeadRequest
import com.clinicos.app.core.network.dto.LeadDetailDto
import com.clinicos.app.core.network.dto.LeadDto
import com.clinicos.app.core.network.dto.LeadListResponse
import com.clinicos.app.core.network.dto.LeadNoteDto
import com.clinicos.app.core.network.dto.LeadStatusRequest
import com.clinicos.app.core.network.dto.UpdateLeadRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LeadApiService {

    @GET("api/v1/leads")
    suspend fun getLeads(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("source") source: String? = null,
        @Query("assigned_user_id") assignedUserId: String? = null,
        @Query("active_only") activeOnly: Boolean = true
    ): Response<LeadListResponse>

    @POST("api/v1/leads")
    suspend fun createLead(
        @Body request: CreateLeadRequest
    ): Response<LeadDto>

    @GET("api/v1/leads/{id}")
    suspend fun getLead(
        @Path("id") id: String
    ): Response<LeadDetailDto>

    @PATCH("api/v1/leads/{id}")
    suspend fun updateLead(
        @Path("id") id: String,
        @Body request: UpdateLeadRequest
    ): Response<LeadDto>

    @PATCH("api/v1/leads/{id}/status")
    suspend fun updateLeadStatus(
        @Path("id") id: String,
        @Body request: LeadStatusRequest
    ): Response<LeadDto>

    @POST("api/v1/leads/{id}/convert")
    suspend fun convertLead(
        @Path("id") id: String,
        @Body request: ConvertLeadRequest
    ): Response<LeadDto>

    @GET("api/v1/leads/{id}/notes")
    suspend fun getLeadNotes(
        @Path("id") id: String
    ): Response<List<LeadNoteDto>>

    @POST("api/v1/leads/{id}/notes")
    suspend fun createLeadNote(
        @Path("id") id: String,
        @Body request: CreateLeadNoteRequest
    ): Response<LeadNoteDto>
}
