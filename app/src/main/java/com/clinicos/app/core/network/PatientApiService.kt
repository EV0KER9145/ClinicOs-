package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.CreatePatientNoteRequest
import com.clinicos.app.core.network.dto.CreatePatientRequest
import com.clinicos.app.core.network.dto.CreateTagRequest
import com.clinicos.app.core.network.dto.DuplicateCheckResponse
import com.clinicos.app.core.network.dto.PatientDetailDto
import com.clinicos.app.core.network.dto.PatientDto
import com.clinicos.app.core.network.dto.PatientListResponse
import com.clinicos.app.core.network.dto.PatientNoteDto
import com.clinicos.app.core.network.dto.StatusRequest
import com.clinicos.app.core.network.dto.TagDto
import com.clinicos.app.core.network.dto.UpdatePatientRequest
import com.clinicos.app.core.network.dto.UpdatePatientTagsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PatientApiService {

    @GET("api/v1/patients")
    suspend fun getPatients(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("search") search: String? = null,
        @Query("active_only") activeOnly: Boolean = true
    ): Response<PatientListResponse>

    @GET("api/v1/patients/check-duplicate")
    suspend fun checkDuplicatePatient(
        @Query("full_name") fullName: String? = null,
        @Query("phone") phone: String? = null
    ): Response<DuplicateCheckResponse>

    @POST("api/v1/patients")
    suspend fun createPatient(
        @Body request: CreatePatientRequest
    ): Response<PatientDto>

    @GET("api/v1/patients/{id}")
    suspend fun getPatient(
        @Path("id") id: String
    ): Response<PatientDetailDto>

    @PATCH("api/v1/patients/{id}")
    suspend fun updatePatient(
        @Path("id") id: String,
        @Body request: UpdatePatientRequest
    ): Response<PatientDto>

    @PATCH("api/v1/patients/{id}/status")
    suspend fun updatePatientStatus(
        @Path("id") id: String,
        @Body request: StatusRequest
    ): Response<PatientDto>

    @PUT("api/v1/patients/{id}/tags")
    suspend fun updatePatientTags(
        @Path("id") id: String,
        @Body request: UpdatePatientTagsRequest
    ): Response<PatientDto>

    @GET("api/v1/patients/{id}/notes")
    suspend fun getPatientNotes(
        @Path("id") id: String
    ): Response<List<PatientNoteDto>>

    @POST("api/v1/patients/{id}/notes")
    suspend fun createPatientNote(
        @Path("id") id: String,
        @Body request: CreatePatientNoteRequest
    ): Response<PatientNoteDto>
}

interface TagApiService {

    @GET("api/v1/tags")
    suspend fun getTags(): Response<List<TagDto>>

    @POST("api/v1/tags")
    suspend fun createTag(
        @Body request: CreateTagRequest
    ): Response<TagDto>
}
