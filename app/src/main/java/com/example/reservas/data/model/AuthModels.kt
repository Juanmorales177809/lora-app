package com.example.reservas.data.model

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

@JsonAdapter(GatewayListResponseAdapter::class)
data class GatewayListResponse(
    val totalCount: Int = 0,
    val gateways: List<GatewayResponse> = emptyList()
)

data class GatewayResponse(
    val id: Int? = null,
    val tenantId: String? = null,
    val name: String? = null,
    @SerializedName("gatewayId") val gateway_id: String? = null,
    @SerializedName("state") val status: String? = null,
    @SerializedName("lastSeenAt") val last_seen: String? = null,
    val description: String? = null,
    val location: GatewayLocation? = null,
    val properties: Map<String, String> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
    val tags: Map<String, String> = emptyMap(),
    val statsInterval: Int? = null
)

data class GatewayLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val source: String? = null,
    val accuracy: Double? = null
)

data class GatewayDetailResponse(
    val createdAt: String? = null,
    val gateway: GatewayResponse,
    val lastSeenAt: String? = null,
    val updatedAt: String? = null
)

data class GatewayUpdateEnvelope(
    val gateway: GatewayUpdateRequest
)

data class GatewayUpdateRequest(
    val name: String,
    val description: String,
    val location: GatewayLocationUpdate,
    val metadata: Map<String, String> = emptyMap(),
    val tags: Map<String, String> = emptyMap(),
    val statsInterval: Int,
    val tenantId: String? = null
)

data class GatewayLocationUpdate(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val source: String = "UNKNOWN",
    val accuracy: Double = 0.0
)

class GatewayListResponseAdapter : JsonDeserializer<GatewayListResponse> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): GatewayListResponse {
        val elements = when {
            json.isJsonArray -> json.asJsonArray.toList()
            json.isJsonObject -> listOf("result", "gateways", "data", "items", "results")
                .firstNotNullOfOrNull { key -> json.asJsonObject.get(key)?.takeIf { it.isJsonArray } }
                ?.asJsonArray?.toList().orEmpty()
            else -> emptyList()
        }
        return GatewayListResponse(
            totalCount = json.asJsonObject.takeIf { json.isJsonObject }
                ?.get("totalCount")?.asInt ?: elements.size,
            gateways = elements.mapNotNull {
            runCatching { context.deserialize<GatewayResponse>(it, GatewayResponse::class.java) }.getOrNull()
            }
        )
    }
}

@JsonAdapter(SensorLatestResponseAdapter::class)
data class SensorLatestResponse(
    val id: Int? = null,
    val sensor_name: String,
    val temp: Double? = null,
    val humi: Double? = null,
    val batt: Double? = null,
    val last_update: String? = null,
    val is_active: Boolean? = null,
    val battery_level: String? = null,
    // Fase 1: soporte genérico para cualquier variable.
    // `variables` guarda keys normalizadas en minúsculas (temp, co2, ph...).
    val variables: Map<String, Double> = emptyMap(),
    val units: Map<String, String> = emptyMap(),
    val labels: Map<String, String> = emptyMap()
) {
    /** Lookup genérico, case-insensitive. Mantiene compat con código que usa .temp/.humi/.batt */
    fun getVariable(key: String): Double? {
        val k = key.lowercase().trim()
        variables[k]?.let { return it }
        return when (k) {
            "temp", "temperature" -> temp
            "humi", "humidity" -> humi
            "batt", "battery" -> batt
            else -> null
        }
    }

    fun availableKeys(): Set<String> {
        val keys = variables.keys.toMutableSet()
        if (temp != null) keys.add("temp")
        if (humi != null) keys.add("humi")
        if (batt != null) keys.add("batt")
        return keys
    }

    fun getUnit(key: String): String = units[key.lowercase().trim()].orEmpty()

    fun getLabel(key: String): String = labels[key.lowercase().trim()].orEmpty()
}

class SensorLatestResponseAdapter : JsonDeserializer<SensorLatestResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SensorLatestResponse {
        val obj = json.asJsonObject
        
        fun firstPrimitive(vararg keys: String): JsonPrimitive? {
            return keys.firstNotNullOfOrNull { key ->
                obj.get(key)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
            }
        }

        fun JsonElement?.asDoubleSafe(): Double? = try {
            this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.let {
                if (it.isNumber) it.asDouble else it.asString.toDoubleOrNull()
            }
        } catch (e: Exception) { null }

        fun JsonPrimitive.asBooleanLike(): Boolean? = try {
            when {
                isBoolean -> asBoolean
                isNumber -> asInt != 0
                else -> when (asString.trim().lowercase()) {
                    "true", "1", "active", "activo", "online", "on", "connected" -> true
                    "false", "0", "inactive", "inactivo", "offline", "off", "disconnected" -> false
                    else -> null
                }
            }
        } catch (e: Exception) { null }

        val id = firstPrimitive("id", "sensor_id")?.asInt
        val name = firstPrimitive("sensor_name", "name", "device_name", "dev_eui")?.asString ?: "Unknown"
        val update = firstPrimitive(
            "last_update",
            "lastUpdate",
            "latest_telemetry",
            "latestTelemetry",
            "last_seen",
            "lastSeen",
            "timestamp",
            "received_at",
            "updated_at",
            "fecha"
        )?.asString

        val temp = (obj.get("temp") ?: obj.get("temperature")).asDoubleSafe()
        val humi = (obj.get("humi") ?: obj.get("humidity")).asDoubleSafe()
        val batt = (obj.get("batt") ?: obj.get("battery")).asDoubleSafe()
        val batteryLevel = firstPrimitive("battery_level", "batteryLevel")?.asString
        val isActive = firstPrimitive("active", "is_active", "online", "status", "estado")?.asBooleanLike()

        // Fase 1: recolectar variables genéricas.
        // 1) Si el backend manda objeto anidado "variables"/"telemetry"/"data"/"values", usarlo.
        // 2) Si manda plano {"co2":450, "ph":7.1}, recolectar todo numérico no-metadata.
        val variables = mutableMapOf<String, Double>()
        val units = mutableMapOf<String, String>()
        val labels = mutableMapOf<String, String>()

        fun putNormalized(rawKey: String, value: Double) {
            val k = rawKey.lowercase().trim()
            val normalized = when (k) {
                "temperature" -> "temp"
                "humidity" -> "humi"
                "battery" -> "batt"
                else -> k
            }
            variables[normalized] = value
        }

        // Objetos anidados conocidos
        listOf("variables", "telemetry", "values", "measurements").forEach { nestedKey ->
            obj.get(nestedKey)?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (k, v) ->
                v.asDoubleSafe()?.let { putNormalized(k, it) }
            }
        }
        // Unidades y labels anidados si el backend los provee
        obj.get("units")?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (k, v) ->
            if (v.isJsonPrimitive) units[k.lowercase().trim()] = v.asString
        }
        obj.get("labels")?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (k, v) ->
            if (v.isJsonPrimitive) labels[k.lowercase().trim()] = v.asString
        }
        // Campos planos: todo lo numérico que no sea metadata conocida
        val metadataKeys = setOf(
            "id", "sensor_id", "sensor_name", "name", "device_name", "dev_eui",
            "last_update", "lastupdate", "latest_telemetry", "latesttelemetry",
            "last_seen", "lastseen", "timestamp", "received_at", "updated_at", "fecha",
            "active", "is_active", "online", "status", "estado",
            "battery_level", "batterylevel",
            "variables", "telemetry", "values", "measurements", "units", "labels",
            "sensors", "devices", "data", "items", "results",
            "application_id", "id_app_lora", "latitude", "longitude", "lat", "lon"
        )
        obj.entrySet().forEach { (k, v) ->
            val lk = k.lowercase().trim()
            if (!metadataKeys.contains(lk) && !variables.containsKey(
                    when (lk) {
                        "temperature" -> "temp"
                        "humidity" -> "humi"
                        "battery" -> "batt"
                        else -> lk
                    }
                )
            ) {
                v.asDoubleSafe()?.let { putNormalized(k, it) }
            }
        }
        // Asegurar que los 3 clásicos queden también en el mapa para compatibilidad
        if (temp != null) variables.putIfAbsent("temp", temp)
        if (humi != null) variables.putIfAbsent("humi", humi)
        if (batt != null) variables.putIfAbsent("batt", batt)

        val result = SensorLatestResponse(
            id = id,
            sensor_name = name,
            temp = temp,
            humi = humi,
            batt = batt,
            last_update = update,
            is_active = isActive,
            battery_level = batteryLevel,
            variables = variables,
            units = units,
            labels = labels
        )

        return result
    }
}

@JsonAdapter(ApplicationSensorsResponseAdapter::class)
data class ApplicationSensorsResponse(
    val application_id: String? = null,
    val sensors: List<SensorLatestResponse>
)

class ApplicationSensorsResponseAdapter : JsonDeserializer<ApplicationSensorsResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ApplicationSensorsResponse {
        if (json.isJsonArray) {
            val sensors = json.asJsonArray.mapNotNull { element ->
                try { context.deserialize<SensorLatestResponse>(element, SensorLatestResponse::class.java) } catch (e: Exception) { null }
            }
            return ApplicationSensorsResponse(sensors = sensors)
        }

        if (json.isJsonObject) {
            val obj = json.asJsonObject
            val keysToSearch = listOf(
                "sensors", "devices", "data", "items", "results",
                "sensores", "dispositivos", "nodes", "telemetry", "lista"
            )
            val arrayElement = keysToSearch.firstNotNullOfOrNull { key ->
                obj.get(key)?.takeIf { it.isJsonArray }
            }
            if (arrayElement != null) {
                val sensors = arrayElement.asJsonArray.mapNotNull { element ->
                    try { context.deserialize<SensorLatestResponse>(element, SensorLatestResponse::class.java) } catch (e: Exception) { null }
                }
                val applicationId = (obj.get("application_id") ?: obj.get("id_app_lora"))
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
                return ApplicationSensorsResponse(application_id = applicationId, sensors = sensors)
            }

            val nestedSensors = mutableListOf<SensorLatestResponse>()
            obj.entrySet().forEach { (_, v) ->
                if (v.isJsonObject) {
                    val childObj = v.asJsonObject
                    if (childObj.has("sensor_name") || childObj.has("name") || childObj.has("dev_eui") || childObj.has("temp")) {
                        try {
                            val sensor = context.deserialize<SensorLatestResponse>(childObj, SensorLatestResponse::class.java)
                            if (sensor != null) nestedSensors.add(sensor)
                        } catch (_: Exception) {}
                    }
                }
            }
            if (nestedSensors.isNotEmpty()) {
                val applicationId = (obj.get("application_id") ?: obj.get("id_app_lora"))
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
                return ApplicationSensorsResponse(application_id = applicationId, sensors = nestedSensors)
            }

            if (obj.has("sensor_name") || obj.has("name") || obj.has("dev_eui") || obj.has("temp")) {
                try {
                    val singleSensor = context.deserialize<SensorLatestResponse>(obj, SensorLatestResponse::class.java)
                    if (singleSensor != null) {
                        return ApplicationSensorsResponse(sensors = listOf(singleSensor))
                    }
                } catch (_: Exception) {}
            }
        }

        return ApplicationSensorsResponse(sensors = emptyList())
    }
}

@JsonAdapter(SensorHistoryResponseAdapter::class)
data class SensorHistoryResponse(
    val sensor_name: String,
    val range: String,
    val count: Int,
    val readings: List<SensorHistoryReading>
)

/**
 * The history endpoint has returned both an envelope and a raw array in
 * different backend versions. Keep the chart independent from that shape.
 */
class SensorHistoryResponseAdapter : JsonDeserializer<SensorHistoryResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SensorHistoryResponse {
        if (json.isJsonArray) {
            val readings = json.asJsonArray.mapNotNull { element ->
                runCatching {
                    context.deserialize<SensorHistoryReading>(element, SensorHistoryReading::class.java)
                }.getOrNull()
            }
            return SensorHistoryResponse(
                sensor_name = "",
                range = "",
                count = readings.size,
                readings = readings
            )
        }

        if (!json.isJsonObject) {
            return SensorHistoryResponse("", "", 0, emptyList())
        }

        val obj = json.asJsonObject
        fun string(vararg keys: String): String = keys.firstNotNullOfOrNull { key ->
            obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString
        }.orEmpty()
        fun int(vararg keys: String): Int? = keys.firstNotNullOfOrNull { key ->
            obj.get(key)?.takeIf { it.isJsonPrimitive }?.let {
                runCatching { if (it.asJsonPrimitive.isNumber) it.asInt else it.asString.toInt() }
                    .getOrNull()
            }
        }

        val readingsElement = listOf("readings", "data", "items", "results", "values")
            .firstNotNullOfOrNull { key -> obj.get(key)?.takeIf { it.isJsonArray } }
        val readings = readingsElement?.asJsonArray?.mapNotNull { element ->
            runCatching {
                context.deserialize<SensorHistoryReading>(element, SensorHistoryReading::class.java)
            }.getOrNull()
        }.orEmpty()

        return SensorHistoryResponse(
            sensor_name = string("sensor_name", "sensor", "name"),
            range = string("range", "period"),
            count = int("count", "total") ?: readings.size,
            readings = readings
        )
    }
}

@JsonAdapter(SensorHistoryReadingAdapter::class)
data class SensorHistoryReading(
    val timestamp: String,
    val temp: Double? = null,
    val humi: Double? = null,
    val batt: Double? = null,
    // Fase 1: valores genéricos normalizados en minúsculas.
    val values: Map<String, Double> = emptyMap()
) {
    /** Lookup genérico usado por gráficas/tablas. Miembro gana sobre la extensión legacy. */
    fun valueFor(variableKey: String): Double? {
        val k = variableKey.lowercase().trim()
        values[k]?.let { return it }
        return when (k) {
            "temp", "temperature" -> temp
            "humi", "humidity" -> humi
            "batt", "battery" -> batt
            else -> null
        }
    }
}

class SensorHistoryReadingAdapter : JsonDeserializer<SensorHistoryReading> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SensorHistoryReading {
        val obj = json.asJsonObject

        fun JsonElement?.asDoubleSafe(): Double? = try {
            this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.let {
                if (it.isNumber) it.asDouble else it.asString.toDoubleOrNull()
            }
        } catch (e: Exception) { null }

        val timestamp = listOf("timestamp", "time", "received_at", "created_at", "fecha", "ts")
            .firstNotNullOfOrNull { k -> obj.get(k)?.takeIf { it.isJsonPrimitive }?.asString }
            .orEmpty()

        val values = mutableMapOf<String, Double>()
        fun putNormalized(rawKey: String, value: Double) {
            val k = rawKey.lowercase().trim()
            val normalized = when (k) {
                "temperature" -> "temp"
                "humidity" -> "humi"
                "battery" -> "batt"
                else -> k
            }
            values[normalized] = value
        }

        // Anidados: values/variables/telemetry/data
        listOf("values", "variables", "telemetry", "measurements", "data").forEach { nestedKey ->
            obj.get(nestedKey)?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()
                ?.forEach { (k, v) -> v.asDoubleSafe()?.let { putNormalized(k, it) } }
        }
        // Planos
        val metadata = setOf(
            "timestamp", "time", "received_at", "created_at", "fecha", "ts",
            "sensor_name", "sensor_id", "id",
            "values", "variables", "telemetry", "measurements", "data"
        )
        obj.entrySet().forEach { (k, v) ->
            if (!metadata.contains(k.lowercase().trim())) {
                v.asDoubleSafe()?.let { putNormalized(k, it) }
            }
        }

        val temp = values["temp"] ?: (obj.get("temp") ?: obj.get("temperature")).asDoubleSafe()
        val humi = values["humi"] ?: (obj.get("humi") ?: obj.get("humidity")).asDoubleSafe()
        val batt = values["batt"] ?: (obj.get("batt") ?: obj.get("battery")).asDoubleSafe()
        if (temp != null) values.putIfAbsent("temp", temp)
        if (humi != null) values.putIfAbsent("humi", humi)
        if (batt != null) values.putIfAbsent("batt", batt)

        return SensorHistoryReading(
            timestamp = timestamp,
            temp = temp,
            humi = humi,
            batt = batt,
            values = values
        )
    }
}

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
