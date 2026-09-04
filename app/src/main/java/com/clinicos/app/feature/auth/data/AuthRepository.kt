package com.clinicos.app.feature.auth.data

import android.util.Log
import com.clinicos.app.core.network.AuthApiService
import com.clinicos.app.core.network.dto.CurrentUserResponse
import com.clinicos.app.core.network.dto.LoginRequest
import com.clinicos.app.core.network.dto.RegisterRequest
import com.clinicos.app.core.network.dto.RegisterResponse
import com.clinicos.app.core.network.dto.TokenResponse
import com.clinicos.app.core.security.TokenManager
import org.json.JSONObject
import retrofit2.Response

class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) {

    suspend fun registerClinic(request: RegisterRequest): Result<RegisterResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration network error", e)
            val causeMsg = e.localizedMessage ?: e.message ?: "Server unreachable"
            Result.failure(Exception("Unable to connect to ClinicOS backend ($causeMsg). Please ensure the backend server is running."))
        }
    }

    suspend fun login(request: LoginRequest): Result<TokenResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                tokenManager.saveToken(tokenResponse.accessToken)
                Result.success(tokenResponse)
            } else {
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login network error", e)
            val causeMsg = e.localizedMessage ?: e.message ?: "Server unreachable"
            Result.failure(Exception("Unable to connect to ClinicOS backend ($causeMsg). Please ensure the backend server is running."))
        }
    }

    suspend fun getCurrentUser(): Result<CurrentUserResponse> {
        if (!tokenManager.hasToken()) {
            return Result.failure(Exception("No authentication token found."))
        }

        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                tokenManager.clearToken()
                val errorMsg = parseErrorMessage(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Get current user network error", e)
            Result.failure(Exception("Unable to connect to ClinicOS. Please check your network connection."))
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }

    fun hasSavedToken(): Boolean {
        return tokenManager.hasToken()
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
                401 -> "Invalid email or password."
                409 -> "An account with this email address already exists."
                403 -> "Account is deactivated."
                else -> "Server error (${response.code()}). Please try again."
            }
        } catch (e: Exception) {
            "An unexpected server error occurred (${response.code()})."
        }
    }
}
