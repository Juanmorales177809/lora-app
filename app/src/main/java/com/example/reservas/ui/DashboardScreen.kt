package com.example.reservas.ui

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.data.model.DashboardHomeResponse
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.BackgroundLight
import com.example.reservas.ui.theme.CoriotNavy
import com.example.reservas.ui.theme.SpaceMonoFontFamily
import com.example.reservas.util.SecurityManager
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onNavigateToNotifications: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToApp: (String) -> Unit,
    onNavigateToSensor: (String, String) -> Unit
) {
    val context = LocalContext.current
    var dashboardData by remember { mutableStateOf<DashboardHomeResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadDashboard() {
        isLoading = true
        isError = false

        val token = SecurityManager.getToken(context)

        if (token == null) {
            isLoading = false
            isError = true
            return
        }

        scope.launch {
            try {
                val response = RetrofitClient.instance.getDashboardHome("Bearer $token")

                if (response.isSuccessful) {
                    dashboardData = response.body()
                } else if (response.code() == 401) {
                    SecurityManager.clear(context)
                    isError = true
                } else {
                    isError = true
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Error loading dashboard data", e)
                isError = true
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadDashboard()
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

            isError -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connection error with CorIoT",
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { loadDashboard() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoriotNavy
                        )
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            dashboardData != null -> {
                val data = dashboardData!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Home Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CoriotNavy,
                        fontFamily = SpaceMonoFontFamily,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 1. Tarjetas de Resumen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            label = "Apps",
                            value = data.summary.active_applications.toString(),
                            icon = Icons.Default.Apps,
                            onClick = onNavigateToApps,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryCard(
                            label = "Notif.",
                            value = data.summary.unread_notifications.toString(),
                            icon = Icons.Default.Notifications,
                            onClick = onNavigateToNotifications,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ChartSection(
                            title = "App status",
                            stats = data.charts?.applications ?: emptyMap(),
                            modifier = Modifier.weight(1f)
                        )

                        ChartSection(
                            title = "Device status",
                            stats = data.charts?.devices ?: emptyMap(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Additional information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoriotNavy,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            SecondaryInfoRow(
                                label = "Active devices",
                                value = data.summary.active_sensors.toString()
                            )

                            SecondaryInfoRow(
                                label = "Configured rules",
                                value = data.summary.active_rules.toString()
                            )

                            SecondaryInfoRow(
                                label = "Active alerts",
                                value = data.summary.recent_alerts.toString(),
                                color = if (data.summary.recent_alerts > 0) {
                                    Color.Red
                                } else {
                                    Color.Black
                                }
                            )

                            SecondaryInfoRow(
                                label = "Alerts last 24h",
                                value = "0"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CoriotNavy,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CoriotNavy,
                fontFamily = SpaceMonoFontFamily
            )

            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ChartSection(
    title: String,
    stats: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val statusColors = mapOf(
        "activas" to Color(0xFF52C41A),
        "inactivas" to Color(0xFFF5222D),
        "activos" to Color(0xFF52C41A),
        "inactivos" to Color(0xFFF5222D)
    )

    val legendStats = stats.mapKeys { it.key.lowercase().trim() }
    val chartStats = legendStats.filterValues { it > 0 }

    Card(
        modifier = modifier.height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CoriotNavy,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                legendStats.entries.forEachIndexed { index, entry ->
                    DonutLegendItem(
                        label = formatStatusLabel(entry.key),
                        count = entry.value,
                        color = statusColors[entry.key] ?: Color.Gray
                    )

                    if (index < legendStats.entries.size - 1) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (chartStats.isNotEmpty()) {
                    SimpleDonutChart(
                        data = chartStats,
                        colors = statusColors,
                        modifier = Modifier.size(122.dp),
                        strokeWidth = 30.dp
                    )
                } else {
                    Text(
                        text = "Sin datos",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun DonutLegendItem(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(
                width = 14.dp,
                height = 6.dp
            ),
            color = color,
            shape = RoundedCornerShape(3.dp)
        ) {}

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "$label: $count",
            fontSize = 7.sp,
            color = Color.DarkGray,
            maxLines = 1
        )
    }
}

@Composable
fun SimpleDonutChart(
    data: Map<String, Int>,
    colors: Map<String, Color>,
    modifier: Modifier = Modifier,
    strokeWidth: androidx.compose.ui.unit.Dp = 30.dp
) {
    val total = data.values.sum()

    if (total == 0) return

    Canvas(modifier = modifier) {
        var startAngle = -90f
        val gapAngle = if (data.size > 1) 2f else 0f

        data.forEach { (label, value) ->
            val key = label.lowercase().trim()
            val rawSweepAngle = (value.toFloat() / total) * 360f
            val sweepAngle = (rawSweepAngle - gapAngle).coerceAtLeast(0f)

            drawArc(
                color = colors[key] ?: Color.Gray,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Butt
                )
            )

            startAngle += rawSweepAngle
        }
    }
}

@Composable
fun SecondaryInfoRow(
    label: String,
    value: String,
    color: Color = Color.Black
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun formatStatusLabel(status: String): String {
    return when (status.lowercase().trim()) {
        "activas" -> "ACTIVAS"
        "inactivas" -> "INACTIVAS"
        "activos" -> "ACTIVOS"
        "inactivos" -> "INACTIVAS"
        else -> status.replace("_", " ").uppercase()
    }
}
