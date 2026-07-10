package com.example.reservas.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.R
import com.example.reservas.data.model.*
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import java.text.SimpleDateFormat
import java.util.*
private val historyRanges = listOf("1m", "5m", "15m", "1h", "3h", "6h", "12h", "24h", "2d", "7d", "30d")

@Composable
fun DevicesScreen(sensorName: String, onBack: () -> Unit) {
    val context = LocalContext.current

    var metrics by remember(sensorName) { mutableStateOf<SensorLoRaMetricsResponse?>(null) }
    var history by remember(sensorName) { mutableStateOf<SensorLoRaHistoryResponse?>(null) }
    var isLoading by remember(sensorName) { mutableStateOf(true) }
    var selectedRange by remember { mutableStateOf("1h") }
    var rangeMenuExpanded by remember { mutableStateOf(false) }
    var selectedChartTab by remember { mutableIntStateOf(0) }

    val modelProducer = remember { CartesianChartModelProducer() }

    val isOnline = metrics?.status.equals("online", ignoreCase = true)

    val chartPoints = remember(history, selectedChartTab, isOnline) {
        if (!isOnline) {
            emptyList()
        } else {
            val readings = history?.readings ?: emptyList()

            readings.mapIndexedNotNull { index, reading ->
                val y = if (selectedChartTab == 0) {
                    reading.rssi
                } else {
                    reading.snr
                }

                y?.let { value ->
                    index.toDouble() to value
                }
            }
        }
    }

    val hasValidChartData = chartPoints.isNotEmpty()

    val dynamicRangeProvider = remember(chartPoints, selectedChartTab, isOnline) {
        if (!isOnline || chartPoints.isEmpty()) {
            CartesianLayerRangeProvider.auto()
        } else {
            val values = chartPoints.map { it.second }

            if (selectedChartTab == 0) {
                val rawMin = values.minOrNull() ?: 0.0
                val rawMax = values.maxOrNull() ?: 0.0
                val padding = 5.0
                val minWindow = 10.0

                var minY = rawMin - padding
                var maxY = rawMax + padding
                val currentWindow = maxY - minY

                if (currentWindow < minWindow) {
                    val diff = (minWindow - currentWindow) / 2.0
                    minY -= diff
                    maxY += diff
                }

                CartesianLayerRangeProvider.fixed(minY = minY, maxY = maxY)
            } else {
                val rawMin = values.minOrNull() ?: 0.0
                val rawMax = values.maxOrNull() ?: 0.0
                val padding = 1.0
                val minWindow = 4.0

                var minY = rawMin - padding
                var maxY = rawMax + padding
                val currentWindow = maxY - minY

                if (currentWindow < minWindow) {
                    val diff = (minWindow - currentWindow) / 2.0
                    minY -= diff
                    maxY += diff
                }

                CartesianLayerRangeProvider.fixed(minY = minY, maxY = maxY)
            }
        }
    }

    LaunchedEffect(sensorName, selectedRange) {
        val token = SecurityManager.getToken(context)
        if (token == null) {
            onBack()
            return@LaunchedEffect
        }

        isLoading = true

        try {
            val metricsResp = RetrofitClient.instance.getLoraMetrics("Bearer $token", sensorName)
            if (metricsResp.isSuccessful) {
                metrics = metricsResp.body()
            } else {
                metrics = null
            }

            val historyResp = RetrofitClient.instance.getLoraHistory("Bearer $token", sensorName, selectedRange)
            if (historyResp.isSuccessful) {
                history = historyResp.body()
            } else {
                history = null
            }
        } catch (e: Exception) {
            Log.e("DevicesScreen", "Error loading data", e)
            metrics = null
            history = null
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(chartPoints, selectedChartTab, isOnline) {
        if (!isOnline || chartPoints.isEmpty()) {
            return@LaunchedEffect
        }

        val xValues = chartPoints.map { it.first }
        val yValues = chartPoints.map { it.second }

        modelProducer.runTransaction {
            lineModel {
                series(x = xValues, y = yValues)
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CoriotNavy)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Router,
                            contentDescription = null,
                            tint = CoriotLightBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = sensorName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoriotNavy
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = if (isOnline) Color(0xFF52C41A) else Color.Red
                        ) {}

                        Text(
                            text = if (isOnline) " Online" else " Offline",
                            fontSize = 14.sp,
                            color = if (isOnline) Color(0xFF52C41A) else Color.Red
                        )
                    }

                    Text(
                        text = "Last update: ${if (isOnline) formatRelativeTime(metrics?.last_update) else "N/A"}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Summary",
                color = CoriotLightBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider(
                color = CoriotLightBlue,
                thickness = 2.dp,
                modifier = Modifier.width(80.dp)
            )

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricCard(
                    label = "Battery",
                    value = formatMetric(
                        isOnline = isOnline,
                        value = metrics?.summary?.battery?.value,
                        unit = metrics?.summary?.battery?.unit ?: "V"
                    ),
                    icon = Icons.Default.BatteryChargingFull,
                    iconColor = Color(0xFF52C41A),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    label = "RSSI",
                    value = formatMetricInt(
                        isOnline = isOnline,
                        value = metrics?.summary?.rssi?.value,
                        unit = metrics?.summary?.rssi?.unit ?: "dBm"
                    ),
                    icon = Icons.Default.SignalCellularAlt,
                    iconColor = Color(0xFF1890FF),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricCard(
                    label = "SNR",
                    value = formatMetric(
                        isOnline = isOnline,
                        value = metrics?.summary?.snr?.value,
                        unit = metrics?.summary?.snr?.unit ?: "dB"
                    ),
                    icon = Icons.Default.Waves,
                    iconColor = Color(0xFF722ED1),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    label = "DR",
                    value = formatMetric(
                        isOnline = isOnline,
                        value = metrics?.summary?.dr?.value
                    ),
                    icon = Icons.Default.BarChart,
                    iconColor = Color(0xFFFAAD14),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("LoRaWAN Status", fontWeight = FontWeight.Bold, color = CoriotNavy)

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val adrValue = isOnline && metrics?.lorawan_status?.adr?.value == true

                    StatusItem(
                        label = "ADR",
                        value = if (!isOnline) "--" else if (adrValue) "Active" else "Inactive",
                        icon = Icons.Default.CheckCircle,
                        iconColor = if (adrValue) Color(0xFF52C41A) else Color.Gray
                    )

                    StatusItem(
                        label = "Uplinks 24 h",
                        value = formatStatusMetric(
                            isOnline = isOnline,
                            value = metrics?.lorawan_status?.uplinks_24h?.value
                        ),
                        icon = Icons.Default.ArrowUpward,
                        iconColor = if (isOnline) Color(0xFF1890FF) else Color.Gray
                    )

                    StatusItem(
                        label = "Errors",
                        value = formatStatusMetric(
                            isOnline = isOnline,
                            value = metrics?.lorawan_status?.errors_24h?.value
                        ),
                        icon = Icons.Default.Warning,
                        iconColor = if (isOnline) Color.Red else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Link History", fontWeight = FontWeight.Bold, color = CoriotNavy)

                Box {
                    Surface(
                        modifier = Modifier
                            .clickable { rangeMenuExpanded = true }
                            .padding(vertical = 4.dp),
                        color = Color(0xFF0B1120),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedRange,
                                color = Color(0xFF00CFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF00CFFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = rangeMenuExpanded,
                        onDismissRequest = { rangeMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF0B1120))
                    ) {
                        historyRanges.forEach { range ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        range,
                                        color = Color(0xFF00CFFF),
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    selectedRange = range
                                    rangeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TabRow(
                        selectedTabIndex = selectedChartTab,
                        containerColor = Color.Transparent,
                        contentColor = CoriotNavy,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedChartTab == 0,
                            onClick = { selectedChartTab = 0 }
                        ) {
                            Text("RSSI", modifier = Modifier.padding(vertical = 8.dp))
                        }

                        Tab(
                            selected = selectedChartTab == 1,
                            onClick = { selectedChartTab = 1 }
                        ) {
                            Text("SNR", modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFF0B1120), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!hasValidChartData) {
                            Text(
                                text = "No hay datos disponibles para este rango",
                                color = Color(0xFF00CFFF).copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = SpaceMonoFontFamily
                            )
                        } else {
                            CartesianChartHost(
                                chart = rememberCartesianChart(
                                    rememberLineCartesianLayer(
                                        lineProvider = LineCartesianLayer.LineProvider.series(
                                            LineCartesianLayer.rememberLine(
                                                fill = LineCartesianLayer.LineFill.single(Fill(Color(0xFF00CFFF))),
                                                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp)
                                            )
                                        ),
                                        rangeProvider = dynamicRangeProvider
                                    ),
                                    startAxis = VerticalAxis.rememberStart(
                                        label = rememberAxisLabelComponent(
                                            style = TextStyle(
                                                color = Color.LightGray,
                                                fontSize = 8.sp
                                            )
                                        ),
                                        guideline = rememberLineComponent(
                                            Fill(Color.Gray.copy(alpha = 0.2f))
                                        )
                                    ),
                                    bottomAxis = HorizontalAxis.rememberBottom(
                                        label = rememberAxisLabelComponent(
                                            style = TextStyle(
                                                color = Color.LightGray,
                                                fontSize = 8.sp
                                            )
                                        ),
                                        guideline = rememberLineComponent(
                                            Fill(Color.Gray.copy(alpha = 0.2f))
                                        )
                                    )
                                ),
                                modelProducer = modelProducer,
                                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Technical Details", fontWeight = FontWeight.Bold, color = CoriotNavy)

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(
                        icon = Icons.Default.Tag,
                        label = "DevAddr",
                        value = formatTextMetric(
                            isOnline = isOnline,
                            value = metrics?.technical_details?.dev_addr?.value
                        )
                    )

                    DetailRow(
                        icon = Icons.Default.SettingsInputAntenna,
                        label = "Frequency",
                        value = formatMetric(
                            isOnline = isOnline,
                            value = metrics?.technical_details?.frequency?.value,
                            unit = metrics?.technical_details?.frequency?.unit ?: "MHz",
                            emptyValue = "N/A"
                        )
                    )

                    DetailRow(
                        icon = Icons.Default.SdCard,
                        label = "Frame counter",
                        value = formatStatusMetric(
                            isOnline = isOnline,
                            value = metrics?.technical_details?.frame_counter?.value,
                            emptyValue = "N/A"
                        )
                    )

                    DetailRow(
                        icon = Icons.AutoMirrored.Filled.Input,
                        label = "Port",
                        value = formatStatusMetric(
                            isOnline = isOnline,
                            value = metrics?.technical_details?.port?.value,
                            emptyValue = "N/A"
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(label, fontSize = 11.sp, color = Color.Gray)

                Text(
                    value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoriotNavy
                )
            }
        }
    }
}

@Composable
fun StatusItem(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(14.dp))

            Spacer(modifier = Modifier.width(4.dp))

            Text(label, fontSize = 11.sp, color = Color.Gray)
        }

        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CoriotNavy
        )
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon,
            null,
            tint = CoriotNavy.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(label, fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = CoriotNavy
        )
    }
}

private fun formatMetric(
    isOnline: Boolean,
    value: Number?,
    unit: String? = null,
    emptyValue: String = "--"
): String {
    if (!isOnline || value == null) return emptyValue

    return if (unit.isNullOrBlank()) {
        value.toString()
    } else {
        "$value $unit"
    }
}

private fun formatMetricInt(
    isOnline: Boolean,
    value: Number?,
    unit: String? = null,
    emptyValue: String = "--"
): String {
    if (!isOnline || value == null) return emptyValue

    return if (unit.isNullOrBlank()) {
        value.toInt().toString()
    } else {
        "${value.toInt()} $unit"
    }
}

private fun formatStatusMetric(
    isOnline: Boolean,
    value: Number?,
    emptyValue: String = "--"
): String {
    if (!isOnline || value == null) return emptyValue
    return value.toString()
}

private fun formatTextMetric(
    isOnline: Boolean,
    value: String?,
    emptyValue: String = "N/A"
): String {
    if (!isOnline || value.isNullOrBlank()) return emptyValue
    return value
}

fun formatRelativeTime(dateIso: String?): String {
    if (dateIso.isNullOrBlank()) return "N/A"

    return try {
        val cleaned = dateIso.replace(" ", "T").replace("Z", "")
        val base = cleaned.split(".")[0]
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(base) ?: return "N/A"
        val diff = System.currentTimeMillis() - date.time
        val mins = diff / (1000 * 60)

        when {
            mins < 1 -> "Just now"
            mins < 60 -> "$mins min ago"
            mins < 1440 -> "${mins / 60} hours ago"
            else -> "${mins / 1440} days ago"
        }
    } catch (e: Exception) {
        "N/A"
    }
}
