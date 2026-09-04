package com.clinicos.app.feature.leads.data

import android.util.Log
import com.clinicos.app.core.network.LeadApiService
import com.clinicos.app.core.network.dto.ConvertLeadRequest
import com.clinicos.app.core.network.dto.CreateLeadNoteRequest
import com.clinicos.app.core.network.dto.CreateLeadRequest
import com.clinicos.app.core.network.dto.LeadDetailDto
import com.clinicos.app.core.network.dto.LeadDto
import com.clinicos.app.core.network.dto.LeadListResponse
import com.clinicos.app.core.network.dto.LeadNoteDto
import com.clinicos.app.core.network.dto.LeadStatusRequest
import com.clinicos.app.core.network.dto.UpdateLeadRequest
import org.json.JSONObject
import retrofit2.Response

class LeadRepository(
    private val leadApiService: LeadApiService
) {

    suspend fun getLeads(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null,
        status: String? = null,
        source: String? = null,
        assignedUserId: String? = null,
        activeOnly: Boolean = true
    ): Result<LeadListResponse> {
        return try {
            val response = leadApiService.getLeads(page, pageSize, search, status, source, assignedUserId, activeOnly)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("LeadRepository", "Error fetching leads", e)
            Result.failure(Exception("Unable to load leads list. Please check your network connection."))
        }
    }

    suspend fun createLead(request: CreateLeadRequest): Result<LeadDto> {
        return try {
            val response = leadApiService.createLead(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("LeadRepository", "Error creating lead", e)
            Result.failure(Exception("Unable to capture enquiry lead. Please try again."))
        }
    }

    suspend fun getLeadDetail(id: String): Result<LeadDetailDto> {
        return try {
            val response = leadApiService.getLead(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("LeadRepository", "Error fetching lead detail", e)
            Result.failure(Exception("Unable to load lead details."))
        }
    }

    suspend fun updateLead(id: String, request: UpdateLeadRequest): Result<LeadDto> {
        return try {
            val response = leadApiService.updateLead(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("LeadRepository", "Error updating lead", e)
            Result.failure(Exception("Unable to update lead details."))
        }
    }

    suspend fun updateLeadStatus(id: String, status: String): Result<LeadDto> {
        return try {
            val response = leadApiService.updateLeadStatus(id, LeadStatusRequest(status))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("LeadRepository", "Error updating lead status", e)
            Result.failure(Exception("Unable to update lead status."))
        }
    }

    suspend fun convertLead(id: String, request: ConvertLeadRequest): Result<LeadDto> {
        return try {
            val response = leadApiService.convertLead(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("LeadRepository", "Error converting lead", e)
            Result.failure(Exception("Unable to convert lead to patient."))
        }
    }

    suspend fun createLeadNote(id: String, content: String): Result<LeadNoteDto> {
        return try {
            val response = leadApiService.createLeadNote(id, CreateLeadNoteRequest(content))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("LeadRepository", "Error adding lead note", e)
            Result.failure(Exception("Unable to add note."))
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
                400 -> "Invalid operation or transition."
                401 -> "Session expired. Please log in again."
                403 -> "You do not have permission to perform this action."
                404 -> "Lead record not found."
                else -> "Server error (${response.code()}). Please try again."
            }
        } catch (e: Exception) {
            "An unexpected error occurred (${response.code()})."
        }
    }
}
