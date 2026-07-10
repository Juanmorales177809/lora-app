package com.example.reservas.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.reservas.data.model.SensorLatestResponse
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

fun telemetryVariableLabel(key: String): String {
    return when (key.lowercase().trim()) {
        "temp" -> "Temperature"
        "humi" -> "Relative Humidity"
        "batt" -> "Battery Voltage"
        else -> key.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

fun telemetryVariableUnit(key: String): String {
    return when (key.lowercase().trim()) {
        "temp" -> "°C"
        "humi" -> "%"
        "batt" -> "V"
        else -> ""
    }
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
    val variables = mutableListOf<TelemetryVariableUi>()
    if (sensor.temp != null) {
        variables.add(TelemetryVariableUi("temp", "Temperature", "Unit: °C", selectedVariables.contains("temp")))
    }
    if (sensor.humi != null) {
        variables.add(TelemetryVariableUi("humi", "Relative Humidity", "Unit: %", selectedVariables.contains("humi")))
    }
    if (sensor.batt != null) {
        variables.add(TelemetryVariableUi("batt", "Battery Voltage", "Unit: V", selectedVariables.contains("batt")))
    }
    return variables
}

data class TelemetryVariableUi(
    val key: String,
    val label: String,
    val unitLabel: String,
    val isSelected: Boolean
)
