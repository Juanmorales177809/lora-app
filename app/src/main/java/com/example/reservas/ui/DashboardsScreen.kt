package com.example.reservas.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.data.model.*
import com.example.reservas.data.CoriotRepository
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardsScreen(
    onDashboardClick: (DashboardResponse) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dashboards by remember { mutableStateOf<List<DashboardResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    
    var showSheet by remember { mutableStateOf(false) }
    var selectedDashboard by remember { mutableStateOf<DashboardResponse?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var dashboardToDelete by remember { mutableStateOf<DashboardResponse?>(null) }
    var deletingDashboardId by remember { mutableStateOf<Int?>(null) }

    fun loadDashboards() {
        isLoading = true
        isError = false
        val token = SecurityManager.getToken(context)
        if (token == null) {
            isLoading = false
            return
        }
        scope.launch {
            try {
                val response = RetrofitClient.instance.getDashboards("Bearer $token")
                if (response.isSuccessful) {
                    dashboards = response.body()?.dashboards ?: emptyList()
                } else {
                    isError = true
                }
            } catch (e: Exception) {
                Log.e("DashboardsScreen", "Error fetching dashboards", e)
                isError = true
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(reloadKey) {
        loadDashboards()
    }

    if (dashboardToDelete != null) {
        AlertDialog(
            onDismissRequest = { if (deletingDashboardId == null) dashboardToDelete = null },
            title = { Text("Delete dashboard") },
            text = { Text("Are you sure you want to delete \"${dashboardToDelete?.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    enabled = deletingDashboardId == null,
                    onClick = {
                        val token = SecurityManager.getToken(context)
                        if (token == null) {
                            Toast.makeText(context, "Session expired.", Toast.LENGTH_SHORT).show()
                            dashboardToDelete = null
                            return@TextButton
                        }
                        deletingDashboardId = dashboardToDelete!!.id
                        scope.launch {
                            try {
                                val response = RetrofitClient.instance.deleteDashboard("Bearer $token", dashboardToDelete!!.id)
                                if (response.isSuccessful) {
                                    dashboards = dashboards.filterNot { it.id == dashboardToDelete!!.id }
                                    dashboardToDelete = null
                                    Toast.makeText(context, "Dashboard deleted.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Unable to delete dashboard (${response.code()}).", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Log.e("DashboardsScreen", "Error deleting dashboard", e)
                                Toast.makeText(context, "Network error deleting dashboard.", Toast.LENGTH_LONG).show()
                            } finally {
                                deletingDashboardId = null
                            }
                        }
                    }
                ) {
                    if (deletingDashboardId == dashboardToDelete?.id) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = deletingDashboardId == null,
                    onClick = { dashboardToDelete = null }
                ) { Text("Cancel") }
            }
        )
    }

    val configuration = LocalConfiguration.current
    val sheetMaxHeight = configuration.screenHeightDp.dp * 0.75f

    Scaffold(
        containerColor = BackgroundLight,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedDashboard = null
                    showSheet = true
                },
                containerColor = CoriotNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Dashboard")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CoriotNavy)
                isError -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error loading dashboards", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { reloadKey++ },
                        colors = ButtonDefaults.buttonColors(containerColor = CoriotNavy)
                    ) { Text("Retry") }
                }
                dashboards.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No dashboards configured yet.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 16.sp)
                }
                else -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        "Dashboards",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceMonoFontFamily,
                        color = CoriotNavy,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(dashboards, key = { it.id }) { dashboard ->
                            DashboardListItem(
                                dashboard = dashboard,
                                onClick = { onDashboardClick(dashboard) },
                                onEdit = {
                                    selectedDashboard = dashboard
                                    showSheet = true
                                },
                                onDelete = { dashboardToDelete = dashboard }
                            )
                        }
                    }
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
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
                        dashboard = selectedDashboard,
                        onSave = {
                            showSheet = false
                            loadDashboards()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardForm(
    dashboard: DashboardResponse?,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CoriotRepository(RetrofitClient.instance) }
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf(dashboard?.name ?: "") }
    var description by remember { mutableStateOf(dashboard?.description ?: "") }
    
    var applicationsWithSensors by remember { mutableStateOf<List<ApplicationWithSensors>>(emptyList()) }
    var sensorOptions by remember { mutableStateOf<List<DashboardSensorOption>>(emptyList()) }
    var selectionMap by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var isLoadingSensors by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val originalWidgets = dashboard?.config?.widgets ?: emptyList()

    LaunchedEffect(Unit) {
        val token = SecurityManager.getToken(context)
        if (token == null) return@LaunchedEffect

        try {
            if (dashboard != null) {
                selectionMap = dashboard.config.widgets
                    .groupBy { dashboardWidgetSensorKey(it) }
                    .mapValues { (_, widgets) -> widgets.map { it.variable_key }.toSet() }
            }

            val appResponse = repository.getApplications(token)
            if (appResponse.isSuccessful) {
                val loadedAppsWithSensors = mutableListOf<ApplicationWithSensors>()
                val loadedOptions = mutableListOf<DashboardSensorOption>()
                appResponse.body()?.applications.orEmpty().forEach { app ->
                    val appSensors = fetchSensorsForApp(token, app)
                    val loraOrNumericId = app.id_app_lora.takeUnless { it.isNullOrBlank() } ?: app.id.toString()
                    appSensors.forEach { sensor ->
                        loadedOptions.add(DashboardSensorOption(applicationId = loraOrNumericId, sensor = sensor))
                    }
                    loadedAppsWithSensors.add(ApplicationWithSensors(app, appSensors))
                }
                applicationsWithSensors = loadedAppsWithSensors
                sensorOptions = loadedOptions.distinctBy(::dashboardSensorKey)
            }
        } catch (e: Exception) {
            Log.e("DashboardForm", "Error loading sensors", e)
        } finally {
            isLoadingSensors = false
        }
    }

    val canSave = name.isNotBlank() && selectionMap.any { it.value.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (dashboard == null) "Create Dashboard" else "Edit Dashboard",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CoriotNavy,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Dashboard name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description optional") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        )

        Text(
            text = "Applications",
            fontWeight = FontWeight.Bold,
            color = CoriotNavy
        )

        if (isLoadingSensors) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = CoriotNavy)
        } else if (applicationsWithSensors.isEmpty()) {
            Text("No applications available.", color = Color.Gray)
        } else {
            applicationsWithSensors.forEach { appWithSensors ->
                ApplicationAccordion(
                    applicationWithSensors = appWithSensors,
                    selectionMap = selectionMap,
                    onSelectionChanged = { sensorKey, variables ->
                        selectionMap = if (variables.isEmpty()) {
                            selectionMap - sensorKey
                        } else {
                            selectionMap + (sensorKey to variables)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val token = SecurityManager.getToken(context) ?: return@Button
                isSaving = true

                val originalByKey = originalWidgets.associateBy(::dashboardWidgetKey)
                val selectedWidgets = mutableListOf<DashboardWidget>()

                // Keep existing selected widgets or add new ones
                selectionMap.forEach { (sensorKey, variableKeys) ->
                    val option = sensorOptions.find { dashboardSensorKey(it) == sensorKey }
                    variableKeys.forEach { variableKey ->
                        val widgetKey = if (option != null) {
                            dashboardWidgetKey(option.applicationId, option.sensor.sensor_name, variableKey)
                        } else {
                            // This might happen for "unavailable" sensors, we should ideally handle them
                            ""
                        }
                        
                        val existing = originalByKey[widgetKey]
                        if (existing != null) {
                            selectedWidgets.add(existing)
                        } else if (option != null) {
                            // Fase 2 genérica: usar labels/units del sensor si vienen del backend.
                            val rawLabel = option.sensor.getLabel(variableKey)
                            val label = rawLabel.ifBlank { telemetryVariableLabel(variableKey) }
                            val rawUnit = option.sensor.getUnit(variableKey)
                            val unit = rawUnit.ifBlank { telemetryVariableUnit(variableKey) }
                            selectedWidgets.add(
                                DashboardWidget(
                                    id = UUID.randomUUID().toString(),
                                    title = "${option.sensor.sensor_name} - $label",
                                    application_id = option.applicationId,
                                    sensor_name = option.sensor.sensor_name,
                                    variable_key = variableKey,
                                    variable_label = label,
                                    unit = unit,
                                    visualization = WidgetVisualization(
                                        type = "stat_card",
                                        chart_type = "none",
                                        future_chart_type = defaultFutureChartType(variableKey)
                                    ),
                                    position = WidgetPosition()
                                )
                            )
                        }
                    }
                }

                val orderedWidgets = selectedWidgets.mapIndexed { index, widget ->
                    widget.copy(position = widget.position.copy(order = index + 1))
                }

                val request = DashboardCreateRequest(
                    name = name.trim(),
                    description = description.trim().ifBlank { null },
                    config = (dashboard?.config ?: DashboardConfig()).copy(widgets = orderedWidgets)
                )

                scope.launch {
                    try {
                        val response = if (dashboard == null) {
                            RetrofitClient.instance.createDashboard("Bearer $token", request)
                        } else {
                            RetrofitClient.instance.updateDashboard("Bearer $token", dashboard.id, request)
                        }

                        if (response.isSuccessful) {
                            Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
                            onSave()
                        } else {
                            Toast.makeText(context, "Error saving dashboard", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
                    } finally {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoriotNavy),
            enabled = !isSaving && canSave
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    if (dashboard == null) "SAVE" else "SAVE CHANGES",
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceMonoFontFamily
                )
            }
        }
    }
}

@Composable
fun DashboardListItem(
    dashboard: DashboardResponse,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = CoriotNavy.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Dashboard, contentDescription = null, tint = CoriotNavy)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(dashboard.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CoriotNavy)
                if (!dashboard.description.isNullOrEmpty()) {
                    Text(dashboard.description, fontSize = 12.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${dashboard.name}", tint = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${dashboard.name}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun dashboardWidgetSensorKey(widget: DashboardWidget): String {
    return "${widget.application_id}::${widget.sensor_name}"
}

private fun dashboardWidgetKey(widget: DashboardWidget): String {
    return dashboardWidgetKey(
        widget.application_id,
        widget.sensor_name,
        widget.variable_key
    )
}

private fun dashboardWidgetKey(
    applicationId: String,
    sensorName: String,
    variableKey: String
): String {
    return "$applicationId::$sensorName::$variableKey"
}
