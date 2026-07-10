package com.example.reservas.data.model

import android.util.Log
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int? = null
)

data class ApiError(
    val detail: String
)

@JsonAdapter(SensorLatestResponseAdapter::class)
data class SensorLatestResponse(
    val id: Int? = null,
    val sensor_name: String,
    val temp: Double? = null,
    val humi: Double? = null,
    val batt: Double? = null,
    val last_update: String? = null,
    val is_active: Boolean? = null,
    val battery_level: String? = null
)

class SensorLatestResponseAdapter : JsonDeserializer<SensorLatestResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SensorLatestResponse {
        val obj = json.asJsonObject
        
        Log.d("SENSOR_DEBUG", "Deserializing JSON: $obj")

        val id = (obj.get("id") ?: obj.get("sensor_id"))?.asInt
        val name = obj.get("sensor_name")?.takeIf { it.isJsonPrimitive }?.asString ?: "Unknown"
        val update = obj.get("last_update")?.takeIf { it.isJsonPrimitive }?.asString

        // Direct mapping for verified fields with safe parsing
        fun JsonElement?.asDoubleSafe(): Double? = try {
            this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.let {
                if (it.isNumber) it.asDouble else it.asString.toDoubleOrNull()
            }
        } catch (e: Exception) { null }

        val temp = obj.get("temp").asDoubleSafe()
        val humi = obj.get("humi").asDoubleSafe()
        val batt = obj.get("batt").asDoubleSafe()
        val batteryLevel = obj.get("battery_level")?.takeIf { it.isJsonPrimitive }?.asString
        
        val activeElement = obj.get("active") ?: obj.get("is_active")
        val isActive = activeElement?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.let {
            try {
                if (it.isBoolean) {
                    it.asBoolean
                } else {
                    when (it.asString.trim().lowercase()) {
                        "true", "1", "active", "online" -> true
                        "false", "0", "inactive", "offline" -> false
                        else -> null
                    }
                }
            } catch (e: Exception) { null }
        }

        val result = SensorLatestResponse(
            id = id,
            sensor_name = name,
            temp = temp,
            humi = humi,
            batt = batt,
            last_update = update,
            is_active = isActive,
            battery_level = batteryLevel
        )

        Log.d("SENSOR_DEBUG", "Final Object -> name=${result.sensor_name}, batt=${result.batt}, level=${result.battery_level}")
        
        return result
    }
}

data class ApplicationSensorsResponse(
    val application_id: String,
    val sensors: List<SensorLatestResponse>
)

data class SensorHistoryResponse(
    val sensor_name: String,
    val range: String,
    val count: Int,
    val readings: List<SensorHistoryReading>
)

data class SensorHistoryReading(
    val timestamp: String,
    val temp: Double? = null,
    val humi: Double? = null,
    val batt: Double? = null
)

data class ApplicationCreate(
    val nombre: String,
    val observaciones: String? = null,
    val idUsuario: Int? = null,
    val id_app_lora: String? = null
)

data class ApplicationUpdate(
    val nombre: String,
    val observaciones: String? = null,
    val id_app_lora: String? = null
)

data class ApplicationResponse(
    val id: Int,
    val nombre: String,
    val observaciones: String?,
    val idUsuario: Int,
    val id_app_lora: String?
)

data class ApplicationListResponse(
    val count: Int,
    val applications: List<ApplicationResponse>
)

data class LoraMetricValue<T>(
    val key: String,
    val label: String,
    val value: T,
    val unit: String?
)

data class LoraSummary(
    val battery: LoraMetricValue<Double>?,
    val rssi: LoraMetricValue<Double>?,
    val snr: LoraMetricValue<Double>?,
    val dr: LoraMetricValue<Int>?
)

data class LoraStatus(
    val adr: LoraMetricValue<Boolean>?,
    val uplinks_24h: LoraMetricValue<Int>?,
    val errors_24h: LoraMetricValue<Int>?
)

data class LoraTechnicalDetails(
    val dev_addr: LoraMetricValue<String>?,
    val frequency: LoraMetricValue<Double>?,
    val frame_counter: LoraMetricValue<Int>?,
    val port: LoraMetricValue<Int>?
)

data class SensorLoRaMetricsResponse(
    val application_id: Int,
    val sensor_name: String,
    val status: String,
    val last_update: String?,
    val summary: LoraSummary,
    val lorawan_status: LoraStatus,
    val technical_details: LoraTechnicalDetails
)

data class SensorLoRaHistoryResponse(
    val sensor_name: String,
    val range: String,
    val count: Int,
    val readings: List<SensorLoRaHistoryReading>
)

data class SensorLoRaHistoryReading(
    val timestamp: String,
    val rssi: Double?,
    val snr: Double?,
    val uplinks: Int?
)

data class UserResponse(
    val id: Int,
    val username: String,
    val role: String,
    val is_active: Boolean,
    val created_at: String?
)

data class DashboardSummary(
    val active_applications: Int,
    val active_sensors: Int,
    val active_rules: Int,
    val unread_notifications: Int,
    val recent_alerts: Int
)

data class DashboardCharts(
    val applications: Map<String, Int>,
    val devices: Map<String, Int>
)

data class DashboardHomeResponse(
    val summary: DashboardSummary,
    val charts: DashboardCharts?,
    val recent_notifications: List<NotificationResponse>?
)

data class NotificationResponse(
    val id: Int,
    val usuario_id: Int,
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    val referencia_tipo: String?,
    val referencia_id: String?,
    val leida: Boolean,
    val creada_en: String?
)

data class NotificationListResponse(
    val count: Int,
    val notifications: List<NotificationResponse>
)

data class UnreadNotificationCountResponse(
    val usuario_id: Int,
    val unread_count: Int
)

data class AlertRuleResponse(
    val id: Int,
    val aplicacion_id: Int,
    val sensor_id: Int,
    val sensor_name: String,
    val variable: String,
    val operador: String,
    val umbral: Double,
    val titulo: String,
    val activa: Boolean,
    val mensaje: String?
)

data class AlertRuleRequest(
    val aplicacion_id: Int,
    val sensor_name: String,
    val variable: String,
    val operador: String,
    val umbral: Double,
    val titulo: String,
    val mensaje: String?,
    val es_sistema: Boolean = false,
    val activa: Boolean,
    val sensor_activo: Boolean = true
)

data class AlertRuleListResponse(
    val count: Int,
    val rules: List<AlertRuleResponse>
)

data class AlertRuleUpdate(
    val activa: Boolean
)

// Dashboard Configuration Models
data class DashboardConfig(
    val layout: DashboardLayout = DashboardLayout(),
    val widgets: List<DashboardWidget> = emptyList()
)

data class DashboardLayout(
    val columns: Int = 1,
    val theme: String = "default"
)

data class DashboardWidget(
    val id: String,
    val title: String,
    val application_id: String,
    val sensor_name: String,
    val variable_key: String,
    val variable_label: String,
    val unit: String,
    val visualization: WidgetVisualization = WidgetVisualization(),
    val position: WidgetPosition = WidgetPosition()
)

data class WidgetVisualization(
    val type: String = "stat_card",
    val chart_type: String = "none",
    val future_chart_type: String = "line_chart"
)

data class WidgetPosition(
    val order: Int=1,
    val width: String = "full"
)

data class DashboardResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val config: DashboardConfig,
    val user_id: Int,
    val created_at: String?
)

data class DashboardCreateRequest(
    val name: String,
    val description: String?,
    val config: DashboardConfig
)

data class DashboardListResponse(
    val count: Int,
    val dashboards: List<DashboardResponse>
)
