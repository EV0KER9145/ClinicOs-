package com.clinicos.app.feature.team.data

import android.util.Log
import com.clinicos.app.core.network.DoctorApiService
import com.clinicos.app.core.network.UserApiService
import com.clinicos.app.core.network.dto.CreateDoctorRequest
import com.clinicos.app.core.network.dto.CreateUserRequest
import com.clinicos.app.core.network.dto.DoctorDto
import com.clinicos.app.core.network.dto.StatusRequest
import com.clinicos.app.core.network.dto.UpdateDoctorRequest
import com.clinicos.app.core.network.dto.UpdateUserRequest
import com.clinicos.app.core.network.dto.UserDto
import org.json.JSONObject
import retrofit2.Response

class TeamRepository(
    private val doctorApiService: DoctorApiService,
    private val userApiService: UserApiService
) {

    // Doctor operations
    suspend fun getDoctors(): Result<List<DoctorDto>> {
        return try {
            val response = doctorApiService.getDoctors()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error fetching doctors", e)
            Result.failure(Exception("Unable to fetch doctors. Please check your network connection."))
        }
    }

    suspend fun createDoctor(request: CreateDoctorRequest): Result<DoctorDto> {
        return try {
            val response = doctorApiService.createDoctor(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error creating doctor", e)
            Result.failure(Exception("Unable to create doctor. Please try again."))
        }
    }

    suspend fun updateDoctor(id: String, request: UpdateDoctorRequest): Result<DoctorDto> {
        return try {
            val response = doctorApiService.updateDoctor(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error updating doctor", e)
            Result.failure(Exception("Unable to update doctor. Please try again."))
        }
    }

    suspend fun updateDoctorStatus(id: String, isActive: Boolean): Result<DoctorDto> {
        return try {
            val response = doctorApiService.updateDoctorStatus(id, StatusRequest(isActive))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error updating doctor status", e)
            Result.failure(Exception("Unable to change doctor status."))
        }
    }

    // User/Staff operations
    suspend fun getUsers(): Result<List<UserDto>> {
        return try {
            val response = userApiService.getUsers()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error fetching users", e)
            Result.failure(Exception("Unable to fetch team members. Please check your connection."))
        }
    }

    suspend fun createUser(request: CreateUserRequest): Result<UserDto> {
        return try {
            val response = userApiService.createUser(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error creating user", e)
            Result.failure(Exception("Unable to add team member. Please try again."))
        }
    }

    suspend fun updateUser(id: String, request: UpdateUserRequest): Result<UserDto> {
        return try {
            val response = userApiService.updateUser(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error updating user", e)
            Result.failure(Exception("Unable to update team member."))
        }
    }

    suspend fun updateUserStatus(id: String, isActive: Boolean): Result<UserDto> {
        return try {
            val response = userApiService.updateUserStatus(id, StatusRequest(isActive))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "Error updating user status", e)
            Result.failure(Exception("Unable to change team member status."))
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
                    if (detail is org.json.JSONArray && detail.length() > 0) {
                        val firstErr = detail.getJSONObject(0)
                        return firstErr.optString("msg", "Validation error occurred.")
                    }
                }
            }
            when (response.code()) {
                401 -> "Session expired. Please log in again."
                403 -> "You do not have permission to perform this action."
                404 -> "Item not found."
                409 -> "An account with this email address already exists."
                else -> "Server error (${response.code()}). Please try again."
            }
        } catch (e: Exception) {
            "An unexpected error occurred (${response.code()})."
        }
    }
}
