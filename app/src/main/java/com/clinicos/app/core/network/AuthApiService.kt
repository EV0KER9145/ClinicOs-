package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.CurrentUserResponse
import com.clinicos.app.core.network.dto.LoginRequest
import com.clinicos.app.core.network.dto.RegisterRequest
import com.clinicos.app.core.network.dto.RegisterResponse
import com.clinicos.app.core.network.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): Response<CurrentUserResponse>
}
