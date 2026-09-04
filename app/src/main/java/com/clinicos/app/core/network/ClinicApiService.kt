package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.ClinicDto
import com.clinicos.app.core.network.dto.UpdateClinicRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface ClinicApiService {

    @GET("api/v1/clinics/me")
    suspend fun getMyClinic(): Response<ClinicDto>

    @PATCH("api/v1/clinics/me")
    suspend fun updateMyClinic(
        @Body request: UpdateClinicRequest
    ): Response<ClinicDto>
}
