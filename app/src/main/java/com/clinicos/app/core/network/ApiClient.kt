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
}
