package com.example.reservas.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.data.model.SensorLatestResponse
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.BackgroundLight
import com.example.reservas.ui.theme.CoriotNavy
import com.example.reservas.ui.theme.SpaceMonoFontFamily
import com.example.reservas.util.SecurityManager
import com.example.reservas.util.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    appId: String,
    onBack: () -> Unit,
    onSensorClick: (String) -> Unit
) {
    val context = LocalContext.current

    var sensors by remember {
        mutableStateOf<List<SensorLatestResponse>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(appId) {
        val token = SecurityManager.getToken(context)

        if (token == null) {
            Toast.makeText(
                context,
                "Session expired. Please sign in again.",
                Toast.LENGTH_SHORT
            ).show()
            onBack()
            return@LaunchedEffect
        }

        try {
            val response = RetrofitClient.instance.getSensorsByApplication(
                "Bearer $token",
                appId
            )

            if (response.isSuccessful) {
                val allSensors = response.body()?.sensors ?: emptyList()
                sensors = allSensors.sortedByDescending { it.is_active ?: false }
            } else {
                Toast.makeText(
                    context,
                    "Unable to load sensor nodes.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Network error. Check your connection.",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = CoriotNavy
            )
        } else if (sensors.isEmpty()) {
            Text(
                text = "No sensor nodes are linked to this application.",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sensors) { sensor ->
                    SensorCard(
                        sensor = sensor,
                        onClick = { onSensorClick(sensor.sensor_name) }
                    )
                }
            }
        }
    }
}

@Composable
fun SensorCard(
    sensor: SensorLatestResponse,
    onClick: () -> Unit
) {
    val isActive = sensor.is_active == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (isActive) {
                    Color(0xFF52C41A).copy(alpha = 0.14f)
                } else {
                    Color.Red.copy(alpha = 0.1f)
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = if (isActive) {
                            Color(0xFF2E7D32)
                        } else {
                            Color.Red
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                    color = if (isActive) {
                        Color(0xFF2E7D32)
                    } else {
                        Color.Red
                    }
                )

                Text(
                    text = if (isActive) {
                        "Latest telemetry: ${formatDate(sensor.last_update)}"
                    } else {
                        "Last seen: ${formatDate(sensor.last_update)}"
                    },
                    fontSize = 11.sp,
                    color = Color.Black.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
