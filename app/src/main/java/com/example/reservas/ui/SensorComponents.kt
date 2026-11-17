package com.example.reservas.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.data.model.ApplicationResponse
import com.example.reservas.data.model.SensorLatestResponse
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.CoriotNavy
import com.example.reservas.ui.theme.SpaceMonoFontFamily
import com.example.reservas.util.formatDate

data class DashboardSensorOption(
    val applicationId: String,
    val sensor: SensorLatestResponse
)

fun dashboardSensorKey(option: DashboardSensorOption): String {
    return "${option.applicationId}::${option.sensor.sensor_name}"
}

fun humanizeVariableKey(key: String): String {
    val trimmed = key.trim()
    if (trimmed.isEmpty()) return trimmed
    return when (trimmed.lowercase()) {
        "temp" -> "Temperature"
        "humi" -> "Relative Humidity"
        "batt" -> "Battery Voltage"
        else -> trimmed.replace("_", " ").replace("-", " ")
            .split(" ").filter { it.isNotBlank() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }
}

fun telemetryVariableLabel(key: String): String {
    return humanizeVariableKey(key)
}

fun telemetryVariableLabel(key: String, labels: Map<String, String>): String {
    labels[key.lowercase().trim()]?.takeIf { it.isNotBlank() }?.let { return it }
    return humanizeVariableKey(key)
}

fun telemetryVariableUnit(key: String): String {
    return when (key.lowercase().trim()) {
        "temp" -> "°C"
        "humi" -> "%"
        "batt" -> "V"
        else -> ""
    }
}

fun telemetryVariableUnit(key: String, units: Map<String, String>): String {
    units[key.lowercase().trim()]?.takeIf { it.isNotBlank() }?.let { return it }
    return telemetryVariableUnit(key)
}

fun defaultFutureChartType(key: String): String {
    return when (key.lowercase().trim()) {
        "temp" -> "line_chart"
        "humi" -> "line_chart"
        "batt" -> "gauge"
        else -> "line_chart"
    }
}

@Composable
fun SensorVariableAccordion(
    sensor: SensorLatestResponse,
    selectedVariables: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = sensor.is_active == true
    val availableVariables = buildAvailableTelemetryVariables(sensor, selectedVariables)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (isActive) Color(0xFF52C41A).copy(alpha = 0.14f) else Color.Red.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = if (isActive) Color(0xFF2E7D32) else Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sensor.sensor_name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoriotNavy,
                        fontFamily = SpaceMonoFontFamily
                    )
                    Text(
                        text = if (isActive) "Online" else "Offline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFF2E7D32) else Color.Red
                    )
                    Text(
                        text = if (isActive) "Latest telemetry: ${formatDate(sensor.last_update)}" else "Last seen: ${formatDate(sensor.last_update)}",
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Gray.copy(alpha = 0.2f)
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Available Telemetry Variables",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (availableVariables.isEmpty()) {
                        Text(
                            text = "No telemetry variables available",
                            fontSize = 14.sp,
                            color = Color.Red.copy(alpha = 0.7f)
                        )
                    } else {
                        availableVariables.forEach { variable ->
                            TelemetryVariableOption(
                                variable = variable,
                                onToggle = { checked ->
                                    val newSelection = if (checked) selectedVariables + variable.key else selectedVariables - variable.key
                                    onSelectionChanged(newSelection)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryVariableOption(
    variable: TelemetryVariableUi,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!variable.isSelected) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = variable.isSelected,
            onCheckedChange = { onToggle(it) },
            colors = CheckboxDefaults.colors(checkedColor = CoriotNavy)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = variable.label, fontSize = 14.sp, color = CoriotNavy, fontWeight = FontWeight.Medium)
            Text(text = variable.unitLabel, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

fun buildAvailableTelemetryVariables(
    sensor: SensorLatestResponse,
    selectedVariables: Set<String>
): List<TelemetryVariableUi> {
    // Fase 1 genérica: exponer toda key de sensor.variables + compat temp/humi/batt.
    val keys = sensor.availableKeys()
    if (keys.isEmpty()) return emptyList()
    return keys.sorted().map { key ->
        val label = sensor.getLabel(key).ifBlank { humanizeVariableKey(key) }
        val unit = sensor.getUnit(key).ifBlank { telemetryVariableUnit(key) }
        val unitLabel = if (unit.isBlank()) "Unit: --" else "Unit: $unit"
        TelemetryVariableUi(key, label, unitLabel, selectedVariables.contains(key))
    }
}

data class TelemetryVariableUi(
    val key: String,
    val label: String,
    val unitLabel: String,
    val isSelected: Boolean
)

data class ApplicationWithSensors(
    val application: ApplicationResponse,
    val sensors: List<SensorLatestResponse>
)

@Composable
fun ApplicationAccordion(
    applicationWithSensors: ApplicationWithSensors,
    selectionMap: Map<String, Set<String>>,
    onSelectionChanged: (sensorKey: String, variables: Set<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val app = applicationWithSensors.application
    val sensors = applicationWithSensors.sensors

    val selectedCountInApp = sensors.sumOf { sensor ->
        val key = "${app.id_app_lora.orEmpty()}::${sensor.sensor_name}"
        selectionMap[key]?.size ?: 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = CoriotNavy.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = CoriotNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.nombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoriotNavy,
                        fontFamily = SpaceMonoFontFamily
                    )
                    if (!app.id_app_lora.isNullOrBlank()) {
                        Text(
                            text = "App ID: ${app.id_app_lora}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = "${sensors.size} sensor${if (sensors.size != 1) "es" else ""}" +
                                if (selectedCountInApp > 0) " ($selectedCountInApp seleccionado${if (selectedCountInApp != 1) "s" else ""})" else "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedCountInApp > 0) CoriotNavy else Color.Gray
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Gray.copy(alpha = 0.2f)
                )
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (sensors.isEmpty()) {
                        Text(
                            text = "No hay sensores disponibles en esta aplicación",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        sensors.forEach { sensor ->
                            val sensorKey = "${app.id_app_lora.orEmpty()}::${sensor.sensor_name}"
                            SensorVariableAccordion(
                                sensor = sensor,
                                selectedVariables = selectionMap[sensorKey].orEmpty(),
                                onSelectionChanged = { variables ->
                                    onSelectionChanged(sensorKey, variables)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

suspend fun fetchSensorsForApp(token: String, app: ApplicationResponse): List<SensorLatestResponse> {
    val candidates = listOfNotNull(
        app.id_app_lora?.takeIf { it.isNotBlank() },
        app.id.toString()
    ).distinct()

    for (appIdParam in candidates) {
        try {
            val resp = RetrofitClient.instance.getSensorsByApplication("Bearer $token", appIdParam)
            if (resp.isSuccessful) {
                val list = resp.body()?.sensors.orEmpty()
                if (list.isNotEmpty()) return list
            }
        } catch (e: Exception) {
            Log.w("SensorComponents", "getSensorsByApplication failed for $appIdParam", e)
        }

        try {
            val resp = RetrofitClient.instance.getSensorsListByAppLora("Bearer $token", appIdParam)
            if (resp.isSuccessful) {
                val list = resp.body()?.sensors.orEmpty()
                if (list.isNotEmpty()) return list
            }
        } catch (e: Exception) {
            Log.w("SensorComponents", "getSensorsListByAppLora failed for $appIdParam", e)
        }
    }

    return emptyList()
}
