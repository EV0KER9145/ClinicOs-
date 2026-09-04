package com.clinicos.app.core.network

import com.clinicos.app.core.network.dto.CreateDoctorRequest
import com.clinicos.app.core.network.dto.CreateUserRequest
import com.clinicos.app.core.network.dto.DoctorDto
import com.clinicos.app.core.network.dto.StatusRequest
import com.clinicos.app.core.network.dto.UpdateDoctorRequest
import com.clinicos.app.core.network.dto.UpdateUserRequest
import com.clinicos.app.core.network.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DoctorApiService {

    @GET("api/v1/doctors")
    suspend fun getDoctors(
        @Query("active_only") activeOnly: Boolean = false
    ): Response<List<DoctorDto>>

    @POST("api/v1/doctors")
    suspend fun createDoctor(
        @Body request: CreateDoctorRequest
    ): Response<DoctorDto>

    @PATCH("api/v1/doctors/{id}")
    suspend fun updateDoctor(
        @Path("id") id: String,
        @Body request: UpdateDoctorRequest
    ): Response<DoctorDto>

    @PATCH("api/v1/doctors/{id}/status")
    suspend fun updateDoctorStatus(
        @Path("id") id: String,
        @Body request: StatusRequest
    ): Response<DoctorDto>
}

interface UserApiService {

    @GET("api/v1/users")
    suspend fun getUsers(
        @Query("active_only") activeOnly: Boolean = false
    ): Response<List<UserDto>>

    @POST("api/v1/users")
    suspend fun createUser(
        @Body request: CreateUserRequest
    ): Response<UserDto>

    @PATCH("api/v1/users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: UpdateUserRequest
    ): Response<UserDto>

    @PATCH("api/v1/users/{id}/status")
    suspend fun updateUserStatus(
        @Path("id") id: String,
        @Body request: StatusRequest
    ): Response<UserDto>
}
