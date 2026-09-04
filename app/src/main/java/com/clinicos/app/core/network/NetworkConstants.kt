package com.clinicos.app.core.network

import com.clinicos.app.BuildConfig

object NetworkConstants {
    /**
     * Centralized API Base URL obtained dynamically from Gradle BuildConfig.
     * Debug build default: http://10.0.2.2:8000/ (Android Emulator to local FastAPI server)
     * Release build default: https://clinicos-api.onrender.com/ (Public Deployed HTTPS API)
     */
    val BASE_URL: String = BuildConfig.BASE_URL
}
