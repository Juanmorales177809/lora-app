package com.example.reservas.data

import com.example.reservas.data.model.ApplicationListResponse
import com.example.reservas.data.model.ApplicationSensorsResponse
import com.example.reservas.data.network.CoriotApiService
import retrofit2.Response

/** Shared boundary for authenticated API access used by UI/ViewModels. */
class CoriotRepository(private val api: CoriotApiService) {
    suspend fun getApplications(token: String): Response<ApplicationListResponse> =
        api.getApplications(bearer(token))

    suspend fun getSensorsByApplication(
        token: String,
        applicationId: String
    ): Response<ApplicationSensorsResponse> =
        api.getSensorsByApplication(bearer(token), applicationId)

    private fun bearer(token: String): String =
        if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
}
