package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.DashboardSummaryResponse
import retrofit2.Response
import retrofit2.http.GET

interface DashboardApiService {

    @GET("api/v1/dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>
}
