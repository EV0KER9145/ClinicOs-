package com.clinicos.app.core.network

import com.clinicos.app.core.security.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var authApiService: AuthApiService? = null
    private var clinicApiService: ClinicApiService? = null
    private var doctorApiService: DoctorApiService? = null
    private var userApiService: UserApiService? = null
    private var patientApiService: PatientApiService? = null
    private var tagApiService: TagApiService? = null
    private var leadApiService: LeadApiService? = null
    private var appointmentApiService: AppointmentApiService? = null
    private var followUpApiService: FollowUpApiService? = null

    private fun getRetrofit(tokenManager: TokenManager): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun getAuthApiService(tokenManager: TokenManager): AuthApiService {
        if (authApiService == null) {
            authApiService = getRetrofit(tokenManager).create(AuthApiService::class.java)
        }
        return authApiService!!
    }

    fun getClinicApiService(tokenManager: TokenManager): ClinicApiService {
        if (clinicApiService == null) {
            clinicApiService = getRetrofit(tokenManager).create(ClinicApiService::class.java)
        }
        return clinicApiService!!
    }

    fun getDoctorApiService(tokenManager: TokenManager): DoctorApiService {
        if (doctorApiService == null) {
            doctorApiService = getRetrofit(tokenManager).create(DoctorApiService::class.java)
        }
        return doctorApiService!!
    }

    fun getUserApiService(tokenManager: TokenManager): UserApiService {
        if (userApiService == null) {
            userApiService = getRetrofit(tokenManager).create(UserApiService::class.java)
        }
        return userApiService!!
    }

    fun getPatientApiService(tokenManager: TokenManager): PatientApiService {
        if (patientApiService == null) {
            patientApiService = getRetrofit(tokenManager).create(PatientApiService::class.java)
        }
        return patientApiService!!
    }

    fun getTagApiService(tokenManager: TokenManager): TagApiService {
        if (tagApiService == null) {
            tagApiService = getRetrofit(tokenManager).create(TagApiService::class.java)
        }
        return tagApiService!!
    }

    fun getLeadApiService(tokenManager: TokenManager): LeadApiService {
        if (leadApiService == null) {
            leadApiService = getRetrofit(tokenManager).create(LeadApiService::class.java)
        }
        return leadApiService!!
    }

    fun getAppointmentApiService(tokenManager: TokenManager): AppointmentApiService {
        if (appointmentApiService == null) {
            appointmentApiService = getRetrofit(tokenManager).create(AppointmentApiService::class.java)
        }
        return appointmentApiService!!
    }

    fun getFollowUpApiService(tokenManager: TokenManager): FollowUpApiService {
        if (followUpApiService == null) {
            followUpApiService = getRetrofit(tokenManager).create(FollowUpApiService::class.java)
        }
        return followUpApiService!!
    }
}
