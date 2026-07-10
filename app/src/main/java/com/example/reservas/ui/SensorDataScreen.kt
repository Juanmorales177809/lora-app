package com.example.reservas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.R
import com.example.reservas.data.model.SensorLatestResponse
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.BackgroundLight
import com.example.reservas.ui.theme.CoriotNavy
import com.example.reservas.ui.theme.SpaceMonoFontFamily
import com.example.reservas.util.SecurityManager
import com.example.reservas.util.formatDate
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDataScreen(
    appId: String,
    selectedSensorNames: List<String>,
    selectedVariableKeys: List<String>,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var sensorsData by remember {
        mutableStateOf<List<SensorLatestResponse>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(appId, selectedSensorNames, selectedVariableKeys) {
        val token = SecurityManager.getToken(context)

        if (token == null) {
            onBack()
            return@LaunchedEffect
        }

        while (true) {
            try {
                val response = RetrofitClient.instance.getSensorsByApplication(
                    "Bearer $token",
                    appId
                )

                if (response.isSuccessful) {
                    val allSensors = response.body()?.sensors ?: emptyList()

                    sensorsData = allSensors.filter { sensor ->
                        selectedSensorNames.contains(sensor.sensor_name)
                    }
                }
            } catch (e: Exception) {
                // Silent background refresh error
            } finally {
                isLoading = false
            }

            delay(30000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CoriotNavy
                )
            }

            sensorsData.isEmpty() -> {
                Text(
                    text = "No telemetry data available for the selected variables",
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(sensorsData) { sensor ->
                        SensorDataCard(
                            sensor = sensor,
                            selectedVariableKeys = selectedVariableKeys
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorDataCard(
    sensor: SensorLatestResponse,
    selectedVariableKeys: List<String>
) {
    val variablesToShow = buildTelemetryValues(
        sensor = sensor,
        selectedVariableKeys = selectedVariableKeys
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CoriotNavy
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sensor.sensor_name,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceMonoFontFamily
                )

                Text(
                    text = formatDate(sensor.last_update),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (variablesToShow.isEmpty()) {
                Text(
                    text = "No telemetry data available for the selected variables",
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    variablesToShow.chunked(2).forEach { rowVariables ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowVariables.forEach { variable ->
                                VariableDisplay(
                                    label = variable.label,
                                    value = variable.value,
                                    unit = variable.unit,
                                    precision = variable.precision,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (rowVariables.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VariableDisplay(
    label: String,
    value: Double,
    unit: String,
    precision: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Text(
            text = "%.${precision}f".format(value),
            color = Color(0xFF00CFFF),
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            fontFamily = SpaceMonoFontFamily
        )

        Text(
            text = unit,
            color = Color(0xFF00CFFF).copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun buildTelemetryValues(
    sensor: SensorLatestResponse,
    selectedVariableKeys: List<String>
): List<TelemetryValueUi> {
    val variables = mutableListOf<TelemetryValueUi>()

    if (selectedVariableKeys.contains("temp") && sensor.temp != null) {
        variables.add(
            TelemetryValueUi(
                key = "temp",
                label = "Temperature",
                value = sensor.temp,
                unit = "°C",
                precision = 2
            )
        )
    }

    if (selectedVariableKeys.contains("humi") && sensor.humi != null) {
        variables.add(
            TelemetryValueUi(
                key = "humi",
                label = "Relative Humidity",
                value = sensor.humi,
                unit = "%",
                precision = 2
            )
        )
    }

    if (selectedVariableKeys.contains("batt") && sensor.batt != null) {
        variables.add(
            TelemetryValueUi(
                key = "batt",
                label = "Battery Voltage",
                value = sensor.batt,
                unit = "V",
                precision = 3
            )
        )
    }

    return variables
}

data class TelemetryValueUi(
    val key: String,
    val label: String,
    val value: Double,
    val unit: String,
    val precision: Int
)