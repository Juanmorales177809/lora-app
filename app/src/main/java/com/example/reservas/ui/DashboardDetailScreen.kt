package com.example.reservas.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.data.model.*
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.BackgroundLight
import com.example.reservas.ui.theme.CoriotNavy
import com.example.reservas.ui.theme.SpaceMonoFontFamily
import com.example.reservas.util.SecurityManager
import com.example.reservas.util.isInactive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardDetailScreen(
    dashboardId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var dashboard by remember { mutableStateOf<DashboardResponse?>(null) }
    var sensorsData by remember { mutableStateOf<Map<String, SensorLatestResponse>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf("Overview") }
    var menuExpanded by remember { mutableStateOf(false) }
    var sensorOptions by remember { mutableStateOf<List<DashboardSensorOption>>(emptyList()) }
    var showAddWidgetDialog by remember { mutableStateOf(false) }
    var isAddingWidget by remember { mutableStateOf(false) }
    var editingChart by remember { mutableStateOf<DashboardWidget?>(null) }
    val scope = rememberCoroutineScope()
    
    // Add state for editing dashboard within detail view
    var showEditDashboardSheet by remember { mutableStateOf(false) }
    val editDashboardSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler(onBack = onBack)

    fun saveWidget(selection: AddWidgetSelection) {
        val currentDashboard = dashboard ?: return
        val token = SecurityManager.getToken(context)
        if (token == null) {
            Toast.makeText(context, "Session expired.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentWidget = editingChart
        val chartType = selection.chartType
        val duplicate = currentDashboard.config.widgets.any { existing ->
            existing.id != currentWidget?.id &&
                existing.application_id == selection.option.applicationId &&
                existing.sensor_name == selection.option.sensor.sensor_name &&
                existing.variable_key == selection.variableKey &&
                if (chartType == null) {
                    existing.visualization.type == "stat_card"
                } else {
                    existing.isHistoricalChart() &&
                        existing.visualization.chart_type == chartType
                }
        }
        if (duplicate) {
            Toast.makeText(context, "This widget already exists.", Toast.LENGTH_SHORT).show()
            return
        }

        val label = telemetryVariableLabel(selection.variableKey)
        val visualization = if (chartType == null) {
            WidgetVisualization(
                type = "stat_card",
                chart_type = "none",
                future_chart_type = defaultFutureChartType(selection.variableKey)
            )
        } else {
            WidgetVisualization(
                type = "chart",
                chart_type = chartType,
                future_chart_type = chartType
            )
        }
        val nextOrder = (currentDashboard.config.widgets.maxOfOrNull { it.position.order } ?: 0) + 1
        val widget = DashboardWidget(
            id = currentWidget?.id ?: UUID.randomUUID().toString(),
            title = "${selection.option.sensor.sensor_name} - $label",
            application_id = selection.option.applicationId,
            sensor_name = selection.option.sensor.sensor_name,
            variable_key = selection.variableKey,
            variable_label = label,
            unit = telemetryVariableUnit(selection.variableKey),
            visualization = visualization,
            position = currentWidget?.position ?: WidgetPosition(order = nextOrder, width = "full")
        )
        val updatedWidgets = if (currentWidget == null) {
            currentDashboard.config.widgets + widget
        } else {
            currentDashboard.config.widgets.map {
                if (it.id == currentWidget.id) widget else it
            }
        }
        val updatedConfig = currentDashboard.config.copy(widgets = updatedWidgets)

        isAddingWidget = true
        scope.launch {
            try {
                val response = RetrofitClient.instance.updateDashboard(
                    token = "Bearer $token",
                    id = currentDashboard.id,
                    request = DashboardCreateRequest(
                        name = currentDashboard.name,
                        description = currentDashboard.description,
                        config = updatedConfig
                    )
                )
                if (response.isSuccessful) {
                    dashboard = response.body() ?: currentDashboard.copy(config = updatedConfig)
                    showAddWidgetDialog = false
                    editingChart = null
                    Toast.makeText(
                        context,
                        if (currentWidget == null) "Widget added." else "Chart updated.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Unable to save widget (${response.code()}).",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error saving widget.", Toast.LENGTH_LONG).show()
            } finally {
                isAddingWidget = false
            }
        }
    }

    fun deleteWidget(widgetId: String) {
        val currentDashboard = dashboard ?: return
        val token = SecurityManager.getToken(context)
        if (token == null) return

        val updatedWidgets = currentDashboard.config.widgets.filterNot { it.id == widgetId }
        val updatedConfig = currentDashboard.config.copy(widgets = updatedWidgets)

        scope.launch {
            try {
                val response = RetrofitClient.instance.updateDashboard(
                    token = "Bearer $token",
                    id = currentDashboard.id,
                    request = DashboardCreateRequest(
                        name = currentDashboard.name,
                        description = currentDashboard.description,
                        config = updatedConfig
                    )
                )
                if (response.isSuccessful) {
                    dashboard = response.body() ?: currentDashboard.copy(config = updatedConfig)
                    Toast.makeText(context, "Widget removed.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error removing widget.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(dashboardId) {
        val token = SecurityManager.getToken(context)
        if (token == null) {
            onBack()
            return@LaunchedEffect
        }

        try {
            val response = RetrofitClient.instance.getDashboardById("Bearer $token", dashboardId)
            if (response.isSuccessful) {
                dashboard = response.body()
            }
        } catch (e: Exception) {
            // Silent error
        } finally {
            isLoading = false
        }
    }

    // Telemetry polling
    LaunchedEffect(dashboard) {
        val token = SecurityManager.getToken(context)
        if (token == null || dashboard == null) return@LaunchedEffect

        val sensorNames = dashboard!!.config.widgets.map { it.sensor_name }.distinct()

        while (true) {
            try {
                val appResponse = RetrofitClient.instance.getApplications("Bearer $token")
                if (appResponse.isSuccessful) {
                    val apps = appResponse.body()?.applications ?: emptyList()
                    val allData = mutableMapOf<String, SensorLatestResponse>()
                    val loadedOptions = mutableListOf<DashboardSensorOption>()
                    apps.forEach { app ->
                        app.id_app_lora?.let { loraId ->
                            val sensorsResp = RetrofitClient.instance.getSensorsByApplication("Bearer $token", loraId)
                            if (sensorsResp.isSuccessful) {
                                sensorsResp.body()?.sensors?.forEach { sensor ->
                                    loadedOptions.add(
                                        DashboardSensorOption(
                                            applicationId = loraId,
                                            sensor = sensor
                                        )
                                    )
                                    if (sensorNames.contains(sensor.sensor_name)) {
                                        allData[sensor.sensor_name] = sensor
                                    }
                                }
                            }
                        }
                    }
                    sensorOptions = loadedOptions.distinctBy(::dashboardSensorKey)
                    sensorsData = allData
                }
            } catch (e: Exception) {
                // Silent refresh error
            }
            delay(30000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CoriotNavy)
                }
            } else if (dashboard == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Dashboard not found", color = Color.Gray)
                }
            } else {
                val data = dashboard!!
                
                // 1. Header (Icon removed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoriotNavy
                        )
                        Text(
                            text = "${data.config.widgets.size} widgets · Live telemetry",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = CoriotNavy)
                        }
                        
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    menuExpanded = false
                                    showEditDashboardSheet = true
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 2. Tab Selector
                TabSwitcher(
                    tabs = listOf("Overview", "Charts", "Map"),
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 3. Tab Content
                when (selectedTab) {
                    "Overview" -> DashboardWidgetsGrid(
                        widgets = data.config.widgets
                            .filterNot { it.isHistoricalChart() }
                            .sortedBy { it.position.order },
                        sensorsData = sensorsData,
                        charts = false,
                        onDeleteWidget = { widget -> deleteWidget(widget.id) }
                    )
                    "Charts" -> DashboardWidgetsGrid(
                        widgets = data.config.widgets
                            .filter { it.isHistoricalChart() }
                            .sortedBy { it.position.order },
                        sensorsData = sensorsData,
                        charts = true,
                        onEditChart = { widget ->
                            editingChart = widget
                            showAddWidgetDialog = true
                        },
                        onDeleteWidget = { widget -> deleteWidget(widget.id) }
                    )
                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Map view coming soon",
                            color = Color.Gray,
                            fontFamily = SpaceMonoFontFamily
                        )
                    }
                }
            }
        }

        if (!isLoading && dashboard != null) {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == "Map") {
                        Toast.makeText(
                            context,
                            "Map widgets are not available yet.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        editingChart = null
                        showAddWidgetDialog = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = CoriotNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add widget")
            }
        }
    }

    if (showAddWidgetDialog && selectedTab != "Map") {
        AddDashboardWidgetDialog(
            mode = selectedTab,
            sensorOptions = sensorOptions,
            initialWidget = editingChart,
            isSaving = isAddingWidget,
            onDismiss = {
                showAddWidgetDialog = false
                editingChart = null
            },
            onAdd = ::saveWidget
        )
    }

    if (showEditDashboardSheet && dashboard != null) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val sheetMaxHeight = configuration.screenHeightDp.dp * 0.75f
        
        ModalBottomSheet(
            onDismissRequest = { showEditDashboardSheet = false },
            sheetState = editDashboardSheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardForm(
                    dashboard = dashboard,
                    onSave = {
                        showEditDashboardSheet = false
                        // Reload dashboard data
                        scope.launch {
                            val token = SecurityManager.getToken(context)
                            if (token != null) {
                                val response = RetrofitClient.instance.getDashboardById("Bearer $token", dashboardId)
                                if (response.isSuccessful) {
                                    dashboard = response.body()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TabSwitcher(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) CoriotNavy else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RefinedWidgetCard(
    widget: DashboardWidget,
    value: Double?,
    lastUpdate: String?,
    isActive: Boolean? = null,
    batteryLevel: String? = null,
    onDelete: (() -> Unit)? = null
) {
    val isOffline = if (isActive != null) !isActive else isInactive(lastUpdate)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = widget.sensor_name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoriotNavy
                    )
                    Text(
                        text = widget.variable_label,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                if (onDelete != null) {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Live value",
                fontSize = 10.sp,
                color = CoriotNavy.copy(alpha = 0.65f),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                val icon = when(widget.variable_key) {
                    "temp" -> Icons.Default.DeviceThermostat
                    "humi" -> Icons.Default.WaterDrop
                    "batt" -> Icons.Default.BatteryChargingFull
                    else -> Icons.Default.Bolt
                }
                
                val iconColor = when(widget.variable_key) {
                    "temp" -> Color(0xFF1890FF)
                    "humi" -> Color(0xFF13C2C2)
                    "batt" -> {
                        when (batteryLevel?.lowercase()?.trim()) {
                            "bajo", "low" -> Color.Red
                            "medio", "medium" -> Color(0xFFFFC107) // Amarillo
                            "alta", "high", "alto" -> Color(0xFF52C41A) // Verde
                            else -> Color(0xFF52C41A) // Default Verde
                        }
                    }
                    else -> CoriotNavy
                }
                
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (value != null) {
                            if (widget.variable_key == "batt") "%.3f".format(value) else "%.2f".format(value)
                        } else "--.---",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoriotNavy,
                        fontFamily = SpaceMonoFontFamily
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = widget.unit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoriotNavy.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(6.dp),
                    shape = CircleShape,
                    color = if (isOffline) Color.Red else Color(0xFF52C41A)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOffline) "Offline" else "Online",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOffline) Color.Red else Color(0xFF2E7D32)
                )
            }
            
            Text(
                text = "Updated 30s ago",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}
