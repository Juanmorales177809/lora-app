package com.example.reservas.data.network

import com.example.reservas.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface CoriotApiService {
    @GET("api/gateways")
    suspend fun getGateways(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 10
    ): Response<GatewayListResponse>

    @GET("api/gateway/{gatewayId}")
    suspend fun getGateway(
        @Header("Authorization") token: String,
        @Path("gatewayId", encoded = true) gatewayId: String
    ): Response<GatewayDetailResponse>

    @PUT("api/gateways/{gatewayId}")
    suspend fun updateGateway(
        @Header("Authorization") token: String,
        @Path("gatewayId", encoded = true) gatewayId: String,
        @Body request: GatewayUpdateEnvelope
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") user: String,
        @Field("password") pass: String
    ): Response<TokenResponse>

    @GET("api/auth/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<UserResponse>

    @GET("api/sensors/by-application")
    suspend fun getSensorsByApplication(
        @Header("Authorization") token: String,
        @Query("application_id") applicationId: String
    ): Response<ApplicationSensorsResponse>

    @GET("https://api.coriotlab.co/api/sensors/sensors-list")
    suspend fun getSensorsListByAppLora(
        @Header("Authorization") token: String,
        @Query("id_app_lora") appLoraId: String
    ): Response<ApplicationSensorsResponse>

    @GET("api/sensors/history")
    suspend fun getSensorHistory(
        @Header("Authorization") token: String,
        @Query("application_id") applicationId: String,
        @Query("sensor_name") sensorName: String,
        @Query("range") range: String = "1h"
    ): Response<SensorHistoryResponse>

    @GET("api/sensors/last-seen")
    suspend fun getSensorLastSeen(
        @Header("Authorization") token: String,
        @Query("sensor_name") sensorName: String
    ): Response<SensorLatestResponse>

    @GET("api/lora/metrics")
    suspend fun getLoraMetrics(
        @Header("Authorization") token: String,
        @Query("sensor_name") sensorName: String
    ): Response<SensorLoRaMetricsResponse>

    @GET("api/lora/history")
    suspend fun getLoraHistory(
        @Header("Authorization") token: String,
        @Query("sensor_name") sensorName: String,
        @Query("range") range: String = "1h"
    ): Response<SensorLoRaHistoryResponse>

    @POST("api/applications")
    suspend fun createApplication(
        @Header("Authorization") token: String,
        @Body application: ApplicationCreate
    ): Response<ApplicationResponse>

    @GET("api/applications")
    suspend fun getApplications(
        @Header("Authorization") token: String
    ): Response<ApplicationListResponse>

    @DELETE("api/applications/{application_id}")
    suspend fun deleteApplication(
        @Header("Authorization") token: String,
        @Path("application_id") id: Int
    ): Response<Unit>

    @PUT("api/applications/{application_id}")
    suspend fun updateApplication(
        @Header("Authorization") token: String,
        @Path("application_id") id: Int,
        @Body application: ApplicationUpdate
    ): Response<ApplicationResponse>

    @GET("api/notifications")
    suspend fun getMyNotifications(
        @Header("Authorization") token: String,
        @Query("unread_only") unreadOnly: Boolean? = false,
        @Query("limit") limit: Int? = 100
    ): Response<NotificationListResponse>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(
        @Header("Authorization") token: String
    ): Response<UnreadNotificationCountResponse>

    @PATCH("api/notifications/{notification_id}/read")
    suspend fun markNotificationAsRead(
        @Header("Authorization") token: String,
        @Path("notification_id") notificationId: Int
    ): Response<NotificationResponse>

    @GET("api/home")
    suspend fun getDashboardHome(
        @Header("Authorization") token: String
    ): Response<DashboardHomeResponse>

    @GET("api/alert-rules")
    suspend fun getAlertRules(
        @Header("Authorization") token: String
    ): Response<AlertRuleListResponse>

    @PATCH("api/alert-rules/{rule_id}/status")
    suspend fun toggleAlertRule(
        @Header("Authorization") token: String,
        @Path("rule_id") ruleId: Int,
        @Body update: AlertRuleUpdate
    ): Response<AlertRuleResponse>

    @POST("api/alert-rules")
    suspend fun createAlertRule(
        @Header("Authorization") token: String,
        @Body rule: AlertRuleRequest
    ): Response<AlertRuleResponse>

    @PUT("api/alert-rules/{rule_id}")
    suspend fun updateAlertRule(
        @Header("Authorization") token: String,
        @Path("rule_id") ruleId: Int,
        @Body rule: AlertRuleRequest
    ): Response<AlertRuleResponse>

    @DELETE("api/alert-rules/{rule_id}")
    suspend fun deleteAlertRule(
        @Header("Authorization") token: String,
        @Path("rule_id") ruleId: Int
    ): Response<Unit>

    // Custom Dashboards
    @GET("api/dashboards")
    suspend fun getDashboards(
        @Header("Authorization") token: String
    ): Response<DashboardListResponse>

    @POST("api/dashboards")
    suspend fun createDashboard(
        @Header("Authorization") token: String,
        @Body request: DashboardCreateRequest
    ): Response<DashboardResponse>

    @GET("api/dashboards/{id}")
    suspend fun getDashboardById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<DashboardResponse>

    @PUT("api/dashboards/{id}")
    suspend fun updateDashboard(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: DashboardCreateRequest
    ): Response<DashboardResponse>

    @DELETE("api/dashboards/{id}")
    suspend fun deleteDashboard(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Unit>
}
