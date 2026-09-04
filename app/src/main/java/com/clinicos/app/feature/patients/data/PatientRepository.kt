package com.clinicos.app.feature.patients.data

import android.util.Log
import com.clinicos.app.core.network.PatientApiService
import com.clinicos.app.core.network.TagApiService
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
import org.json.JSONObject
import retrofit2.Response

class PatientRepository(
    private val patientApiService: PatientApiService,
    private val tagApiService: TagApiService
) {

    suspend fun getPatients(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null,
        activeOnly: Boolean = true
    ): Result<PatientListResponse> {
        return try {
            val response = patientApiService.getPatients(page, pageSize, search, activeOnly)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error fetching patients", e)
            Result.failure(Exception("Unable to load patients list. Please check your network connection."))
        }
    }

    suspend fun checkDuplicatePatient(fullName: String?, phone: String?): Result<DuplicateCheckResponse> {
        return try {
            val response = patientApiService.checkDuplicatePatient(fullName, phone)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error checking duplicate patient", e)
            Result.failure(Exception("Error checking duplicates."))
        }
    }

    suspend fun createPatient(request: CreatePatientRequest): Result<PatientDto> {
        return try {
            val response = patientApiService.createPatient(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error creating patient", e)
            Result.failure(Exception("Unable to create patient. Please try again."))
        }
    }

    suspend fun getPatientDetail(id: String): Result<PatientDetailDto> {
        return try {
            val response = patientApiService.getPatient(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error fetching patient detail", e)
            Result.failure(Exception("Unable to load patient profile."))
        }
    }

    suspend fun updatePatient(id: String, request: UpdatePatientRequest): Result<PatientDto> {
        return try {
            val response = patientApiService.updatePatient(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error updating patient", e)
            Result.failure(Exception("Unable to update patient profile."))
        }
    }

    suspend fun updatePatientStatus(id: String, isActive: Boolean): Result<PatientDto> {
        return try {
            val response = patientApiService.updatePatientStatus(id, StatusRequest(isActive))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error updating patient status", e)
            Result.failure(Exception("Unable to archive patient."))
        }
    }

    suspend fun updatePatientTags(id: String, tagIds: List<String>): Result<PatientDto> {
        return try {
            val response = patientApiService.updatePatientTags(id, UpdatePatientTagsRequest(tagIds))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error updating patient tags", e)
            Result.failure(Exception("Unable to update tags."))
        }
    }

    suspend fun createPatientNote(id: String, content: String): Result<PatientNoteDto> {
        return try {
            val response = patientApiService.createPatientNote(id, CreatePatientNoteRequest(content))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error adding patient note", e)
            Result.failure(Exception("Unable to add note."))
        }
    }

    // Tag Operations
    suspend fun getTags(): Result<List<TagDto>> {
        return try {
            val response = tagApiService.getTags()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error fetching tags", e)
            Result.failure(Exception("Unable to load tags."))
        }
    }

    suspend fun createTag(name: String): Result<TagDto> {
        return try {
            val response = tagApiService.createTag(CreateTagRequest(name))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error creating tag", e)
            Result.failure(Exception("Unable to create tag."))
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
                404 -> "Patient record not found."
                else -> "Server error (${response.code()}). Please try again."
            }
        } catch (e: Exception) {
            "An unexpected error occurred (${response.code()})."
        }
    }
}
