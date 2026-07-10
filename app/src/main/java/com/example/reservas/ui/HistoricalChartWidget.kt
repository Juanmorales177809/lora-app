package com.example.reservas.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.data.model.DashboardWidget
import com.example.reservas.data.model.SensorHistoryReading
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.CoriotNavy
import com.example.reservas.util.SecurityManager
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

private val historyRanges = listOf("1m", "5m", "15m", "1h", "3h", "6h", "12h", "24h", "2d", "7d", "30d")
private val supportedChartTypes = setOf("line_chart", "bar_chart", "scatter_chart", "table")

fun DashboardWidget.isHistoricalChart(): Boolean {
    return visualization.type.equals("chart", ignoreCase = true) ||
        visualization.chart_type.lowercase() in supportedChartTypes
}

fun DashboardWidget.resolvedChartType(): String {
    return visualization.chart_type.lowercase()
        .takeIf { it in supportedChartTypes }
        ?: visualization.future_chart_type.lowercase().takeIf { it in supportedChartTypes }
        ?: "line_chart"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalChartWidget(
    widget: DashboardWidget,
    liveValue: Double?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val chartType = widget.resolvedChartType()
    var selectedRange by rememberSaveable(widget.id) { mutableStateOf("1h") }
    var rangeMenuExpanded by remember { mutableStateOf(false) }
    var settingsMenuExpanded by remember { mutableStateOf(false) }
    var readings by remember(widget.id) { mutableStateOf<List<SensorHistoryReading>>(emptyList()) }
    var isLoading by remember(widget.id) { mutableStateOf(true) }
    var hasError by remember(widget.id) { mutableStateOf(false) }
    var modelReady by remember(widget.id) { mutableStateOf(false) }
    val modelProducer = remember(widget.id) { CartesianChartModelProducer() }

    val dataPoints = remember(readings, widget.variable_key) {
        readings.mapNotNull { reading ->
            val ms = reading.timestamp.toEpochMillis() ?: return@mapNotNull null
            val y = reading.valueFor(widget.variable_key) ?: return@mapNotNull null
            if (y.isFinite()) {
                HistoricalPoint(x = ms.toDouble(), y = y)
            } else null
        }.distinctBy { it.x }.sortedBy { it.x }
    }

    val dynamicRangeProvider = remember(dataPoints, widget.variable_key) {
        val readings = dataPoints.map { it.y }
        if (readings.isEmpty()) {
            CartesianLayerRangeProvider.auto()
        } else {
            val rawMin = readings.min()
            val rawMax = readings.max()
            val avg = readings.average()

            when (widget.variable_key.lowercase()) {
                "temp" -> {
                    val padding = 1.0
                    val minWindow = 5.0
                    var minY = rawMin - padding
                    var maxY = rawMax + padding
                    if ((maxY - minY) < minWindow) {
                        minY = avg - (minWindow / 2.0)
                        maxY = avg + (minWindow / 2.0)
                    }
                    CartesianLayerRangeProvider.fixed(minY = minY, maxY = maxY)
                }
                "humi" -> {
                    val padding = 3.0
                    val minWindow = 10.0
                    var minY = (rawMin - padding).coerceAtLeast(0.0)
                    var maxY = (rawMax + padding).coerceAtMost(100.0)
                    if ((maxY - minY) < minWindow) {
                        minY = (avg - (minWindow / 2.0)).coerceAtLeast(0.0)
                        maxY = (minY + minWindow).coerceAtMost(100.0)
                        if (maxY == 100.0) minY = (maxY - minWindow).coerceAtLeast(0.0)
                    }
                    CartesianLayerRangeProvider.fixed(minY = minY, maxY = maxY)
                }
                "batt" -> {
                    val delta = rawMax - rawMin
                    val margin = if (delta == 0.0) 0.1 else delta * 0.1
                    CartesianLayerRangeProvider.fixed(minY = rawMin - margin, maxY = rawMax + margin)
                }
                else -> {
                    val delta = rawMax - rawMin
                    val margin = if (delta == 0.0) 1.0 else delta * 0.15
                    CartesianLayerRangeProvider.fixed(minY = rawMin - margin, maxY = rawMax + margin)
                }
            }
        }
    }

    val navyLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(Color(0xFF00CFFF))), // Cyan like image
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp)
    )
    val navyLineLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(navyLine),
        rangeProvider = dynamicRangeProvider
    )

    val navyColumn = rememberLineComponent(
        fill = Fill(Color(0xFF00CFFF).copy(alpha = 0.6f)),
        thickness = 8.dp
    )
    val navyColumnLayer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(navyColumn),
        rangeProvider = dynamicRangeProvider
    )

    val scatterPoint = LineCartesianLayer.Point(
        component = rememberShapeComponent(fill = Fill(Color(0xFF00CFFF))),
        size = 7.dp
    )
    val scatterLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill.Transparent),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 0.dp),
        pointProvider = LineCartesianLayer.PointProvider.single(scatterPoint)
    )
    val scatterLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(scatterLine),
        rangeProvider = dynamicRangeProvider
    )

    val xAxisFormatter = remember(selectedRange) {
        CartesianValueFormatter { _, value, _ ->
            formatHistoryAxisLabel(value, selectedRange)
        }
    }
    val horizontalItemPlacer = remember {
        HorizontalAxis.ItemPlacer.aligned()
    }
    LaunchedEffect(widget.sensor_name, widget.variable_key, selectedRange) {
        isLoading = true
        hasError = false
        readings = emptyList()

        val token = SecurityManager.getToken(context)
        if (token == null) {
            hasError = true
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val response = RetrofitClient.instance.getSensorHistory(
                token = "Bearer $token",
                sensorName = widget.sensor_name,
                range = selectedRange
            )

            if (response.isSuccessful) {
                readings = response.body()
                    ?.readings
                    .orEmpty()
                    .sortedBy { it.timestamp }
                    .filter { it.valueFor(widget.variable_key) != null }
            } else {
                hasError = true
                Log.w(
                    "HistoricalChartWidget",
                    "History request failed: sensor=${widget.sensor_name}, range=$selectedRange, code=${response.code()}"
                )
            }
        } catch (e: Exception) {
            hasError = true
            Log.e("HistoricalChartWidget", "Error loading chart", e)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(dataPoints, chartType) {
        if (dataPoints.isNotEmpty() && chartType != "table") {
            Log.d("HistoricalChartWidget", "Updating chart model with ${dataPoints.size} points for ${widget.sensor_name}")
            val xValues = dataPoints.map { it.x }
            val yValues = dataPoints.map { it.y }
            modelProducer.runTransaction {
                if (chartType == "bar_chart") {
                    columnModel {
                        series(x = xValues, y = yValues)
                    }
                } else {
                    lineModel {
                        series(x = xValues, y = yValues)
                    }
                }
            }
            modelReady = true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = widget.sensor_name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoriotNavy
                    )
                    Text(
                        text = "${widget.variable_label} (${widget.unit})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoriotNavy
                    )
                    Text(text = "Historical trend", fontSize = 11.sp, color = Color.Gray)
                }
                Box {
                    IconButton(onClick = { settingsMenuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Chart settings",
                            tint = CoriotNavy
                        )
                    }
                    DropdownMenu(
                        expanded = settingsMenuExpanded,
                        onDismissRequest = { settingsMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                settingsMenuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                settingsMenuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Live value", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = liveValue?.let { value ->
                            val formatted = if (widget.variable_key == "batt") {
                                "%.3f".format(value)
                            } else {
                                "%.2f".format(value)
                            }
                            "$formatted ${widget.unit}"
                        } ?: "-- ${widget.unit}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoriotNavy
                    )
                }

                Box {
                    Surface(
                        modifier = Modifier
                            .width(80.dp)
                            .clickable { rangeMenuExpanded = true },
                        color = Color(0xFF0B1120),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = selectedRange,
                                color = Color(0xFF00CFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
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

            Box(
                modifier = Modifier.fillMaxWidth().height(if (chartType == "table") 250.dp else 210.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator(color = CoriotNavy)
                    hasError -> Text(
                        text = "Error loading chart",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                    readings.isEmpty() || chartType != "table" && dataPoints.isEmpty() -> Text(
                        text = "No historical data available",
                        color = Color.Gray
                    )
                    chartType == "table" -> HistoricalReadingsTable(
                        readings = readings.takeLast(5).reversed(),
                        variableKey = widget.variable_key,
                        unit = widget.unit
                    )
                    !modelReady -> CircularProgressIndicator(color = CoriotNavy, modifier = Modifier.size(24.dp))
                    else -> {
                        val layer = when (chartType) {
                            "bar_chart" -> navyColumnLayer
                            "scatter_chart" -> scatterLayer
                            else -> navyLineLayer
                        }
                        
                        val yAxisFormatter = remember(widget.variable_key, dataPoints) {
                            val minY = dataPoints.minOfOrNull { it.y } ?: 0.0
                            val maxY = dataPoints.maxOfOrNull { it.y } ?: 100.0
                            val delta = maxY - minY
                            
                            CartesianValueFormatter { _, value, _ ->
                                when {
                                    widget.variable_key == "batt" -> "%.3f".format(value)
                                    delta < 0.5 -> "%.2f".format(value)
                                    else -> "%.1f".format(value)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0B1120), shape = RoundedCornerShape(8.dp))
                                .padding(top = 12.dp, bottom = 4.dp, start = 8.dp, end = 16.dp)
                        ) {
                            CartesianChartHost(
                                chart = rememberCartesianChart(
                                    layer,
                                    startAxis = VerticalAxis.rememberStart(
                                        valueFormatter = yAxisFormatter,
                                        label = rememberAxisLabelComponent(
                                            style = TextStyle(color = Color.LightGray, fontSize = 9.sp)
                                        ),
                                        horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Outside,
                                        line = null,
                                        tick = null,
                                        guideline = rememberLineComponent(
                                            fill = Fill(Color.Gray.copy(alpha = 0.15f)),
                                            thickness = 1.dp
                                        ),
                                        itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 5 }) }
                                    ),
                                    bottomAxis = HorizontalAxis.rememberBottom(
                                        valueFormatter = xAxisFormatter,
                                        itemPlacer = horizontalItemPlacer,
                                        label = rememberAxisLabelComponent(
                                            style = TextStyle(color = Color.LightGray, fontSize = 9.sp)
                                        ),
                                        line = null,
                                        tick = null,
                                        guideline = rememberLineComponent(
                                            fill = Fill(Color.Gray.copy(alpha = 0.1f)),
                                            thickness = 1.dp
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
        }
    }
}

@Composable
private fun HistoricalReadingsTable(
    readings: List<SensorHistoryReading>,
    variableKey: String,
    unit: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("Timestamp", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = CoriotNavy)
            Text("Value", fontWeight = FontWeight.Bold, color = CoriotNavy)
        }
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
        readings.forEach { reading ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reading.timestamp.replace("T", " ").take(19),
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                val value = reading.valueFor(variableKey)
                Text(
                    text = if (value != null) "${"%.3f".format(value)} $unit" else "-- $unit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoriotNavy
                )
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.25f))
        }
    }
}

private data class HistoricalPoint(
    val x: Double,
    val y: Double
)

private fun String.toEpochMillis(): Long? {
    return try {
        // Handle ISO8601 variations: 2026-06-29T13:31:19.004518Z, 2026-06-29 13:31:19, etc.
        val cleaned = this.replace(" ", "T").replace("Z", "")
        val base = cleaned.split(".")[0]
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(base) ?: return null
        
        // Add milliseconds if present
        val ms = if (cleaned.contains(".")) {
            val fraction = "0." + cleaned.split(".")[1].take(3)
            (fraction.toDoubleOrNull() ?: 0.0) * 1000.0
        } else 0.0
        
        date.time + ms.toLong()
    } catch (_: Exception) {
        null
    }
}

private fun formatHistoryAxisLabel(epochMillis: Double, range: String): String {
    val pattern = when (range) {
        "1h", "3h", "6h" -> "HH:mm:ss"
        "12h", "24h" -> "HH:mm"
        else -> "dd/MM HH:mm"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(
        Date(epochMillis.toLong())
    )
}
private fun SensorHistoryReading.valueFor(variableKey: String): Double? {
    return when (variableKey.lowercase()) {
        "temp" -> temp
        "humi" -> humi
        "batt" -> batt
        else -> null
    }
}