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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.data.model.*
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertRulesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<AlertRuleResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var showSheet by remember { mutableStateOf(false) }
    var selectedRule by remember { mutableStateOf<AlertRuleResponse?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    fun loadRules() {
        val token = SecurityManager.getToken(context)
        if (token != null) {
            isLoading = true
            scope.launch {
                try {
                    val response = RetrofitClient.instance.getAlertRules("Bearer $token")
                    if (response.isSuccessful) {
                        rules = response.body()?.rules ?: emptyList()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error loading alert rules", Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRules()
    }
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = configuration.screenHeightDp.dp * 0.75f
    Scaffold(
        containerColor = BackgroundLight,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedRule = null
                    showSheet = true
                },
                containerColor = CoriotNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alert Rule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Alerts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceMonoFontFamily,
                color = CoriotNavy,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CoriotNavy)
                }
            } else if (rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No alert rules defined yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rules) { rule ->
                        AlertRuleCard(
                            rule = rule,
                            onClick = {
                                selectedRule = rule
                                showSheet = true
                            },
                            onToggle = { isChecked ->
                                val token = SecurityManager.getToken(context)
                                if (token != null) {
                                    scope.launch {
                                        try {
                                            val response = RetrofitClient.instance.toggleAlertRule(
                                                "Bearer $token",
                                                rule.id,
                                                AlertRuleUpdate(activa = isChecked)
                                            )
                                            if (response.isSuccessful) {
                                                rules = rules.map {
                                                    if (it.id == rule.id) it.copy(activa = isChecked) else it
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error updating rule", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
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
                    AlertRuleForm(
                        rule = selectedRule,
                        onSave = {
                            showSheet = false
                            loadRules()
                        },
                        onDelete = {
                            showSheet = false
                            loadRules()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertRuleForm(
    rule: AlertRuleResponse?,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var apps by remember { mutableStateOf<List<ApplicationResponse>>(emptyList()) }
    var sensors by remember { mutableStateOf<List<SensorLatestResponse>>(emptyList()) }
    
    var selectedApp by remember { mutableStateOf<ApplicationResponse?>(null) }
    var selectedSensor by remember { mutableStateOf<SensorLatestResponse?>(null) }
    // Fase 3 genérica: sin default fijo; se autoselecciona al cargar sensor.
    var variable by remember { mutableStateOf(rule?.variable ?: "") }
    var operator by remember { mutableStateOf(rule?.operador ?: "gt") }
    var threshold by remember { mutableStateOf(rule?.umbral?.toString() ?: "") }
    var title by remember { mutableStateOf(rule?.titulo ?: "") }
    var message by remember { mutableStateOf(rule?.mensaje ?: "") }
    var active by remember { mutableStateOf(rule?.activa ?: true) }
    
    var isSaving by remember { mutableStateOf(false) }

    val operators = listOf(
        "gt" to ">",
        "gte" to "≥",
        "lt" to "<",
        "lte" to "≤",
        "eq" to "=="
    )
    
    // Fase 3 genérica: variables del sensor seleccionado + fallback a la regla existente.
    val sensorKeys = selectedSensor?.availableKeys()?.sorted().orEmpty()
    val variableKeys = (sensorKeys + listOfNotNull(rule?.variable?.takeIf { it.isNotBlank() }, variable.takeIf { it.isNotBlank() }))
        .map { it.lowercase().trim() }.filter { it.isNotBlank() }.distinct()
    val variables = variableKeys.map { k ->
        val sensor = selectedSensor
        // Usa label del sensor si existe, si no humaniza.
        val label = sensor?.getLabel(k)?.ifBlank { telemetryVariableLabel(k) } ?: telemetryVariableLabel(k)
        k to label
    }.ifEmpty {
        // Sin sensor aún: al menos mostrar la variable guardada o temp como compat.
        if (rule?.variable != null) listOf(rule.variable to telemetryVariableLabel(rule.variable))
        else listOf("temp" to telemetryVariableLabel("temp"))
    }

    // Autoseleccionar primera variable disponible si la actual no es válida.
    LaunchedEffect(sensorKeys) {
        if (sensorKeys.isNotEmpty() && !sensorKeys.contains(variable.lowercase().trim())) {
            variable = sensorKeys.first()
        }
    }

    // Load initial data
    LaunchedEffect(Unit) {
        val token = SecurityManager.getToken(context)
        if (token != null) {
            try {
                val appResponse = RetrofitClient.instance.getApplications("Bearer $token")
                if (appResponse.isSuccessful) {
                    apps = appResponse.body()?.applications ?: emptyList()
                    if (rule != null) {
                        selectedApp = apps.find { it.id == rule.aplicacion_id }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Load sensors when app changes
    LaunchedEffect(selectedApp) {
        val token = SecurityManager.getToken(context)
        val appId = selectedApp?.id_app_lora
        if (token != null && appId != null) {
            try {
                val sensorResponse = RetrofitClient.instance.getSensorsByApplication("Bearer $token", appId)
                if (sensorResponse.isSuccessful) {
                    sensors = sensorResponse.body()?.sensors ?: emptyList()
                    if (rule != null && selectedSensor == null) {
                        selectedSensor = sensors.find { it.id == rule.sensor_id || it.sensor_name == rule.sensor_name }
                    }
                }
            } catch (_: Exception) {}
        } else {
            sensors = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = if (rule == null) "Create Alert Rule" else "Edit Alert Rule",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CoriotNavy,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // App Dropdown
            DropdownField(
                label = "Application",
                options = apps.map { it.nombre },
                selectedOption = selectedApp?.nombre ?: "Select App",
                onOptionSelected = { name ->
                    selectedApp = apps.find { it.nombre == name }
                    selectedSensor = null
                },
                modifier = Modifier.weight(1f)
            )
            
            // Sensor Dropdown
            DropdownField(
                label = "Device",
                options = sensors.map { it.sensor_name },
                selectedOption = selectedSensor?.sensor_name ?: "Select Device",
                onOptionSelected = { name ->
                    selectedSensor = sensors.find { it.sensor_name == name }
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Variable
            DropdownField(
                label = "Variable",
                options = variables.map { it.second },
                selectedOption = variables.find { it.first.equals(variable, ignoreCase = true) }?.second ?: "Variable",
                onOptionSelected = { name ->
                    variable = variables.find { it.second == name }?.first ?: variable
                },
                modifier = Modifier.weight(1f)
            )
            
            // Condition
            DropdownField(
                label = "Condition",
                options = operators.map { it.second },
                selectedOption = operators.find { it.first == operator }?.second ?: "Condition",
                onOptionSelected = { symbol ->
                    operator = operators.find { it.second == symbol }?.first ?: "gt"
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = threshold,
            onValueChange = { threshold = it },
            label = { Text("Threshold") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Active", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Switch(
                checked = active,
                onCheckedChange = { active = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CoriotNavy
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (rule != null) {
                TextButton(
                    onClick = {
                        val token = SecurityManager.getToken(context)
                        if (token != null) {
                            scope.launch {
                                try {
                                    val response = RetrofitClient.instance.deleteAlertRule("Bearer $token", rule.id)
                                    if (response.isSuccessful) {
                                        onDelete()
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                ) {
                    Text("DELETE", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    val token = SecurityManager.getToken(context)
                    if (token != null && selectedApp != null && selectedSensor != null && variable.isNotBlank()) {
                        isSaving = true
                        val request = AlertRuleRequest(
                            aplicacion_id = selectedApp!!.id,
                            sensor_name = selectedSensor!!.sensor_name,
                            variable = variable,
                            operador = operator,
                            umbral = threshold.toDoubleOrNull() ?: 0.0,
                            titulo = title,
                            mensaje = message.ifBlank { null },
                            es_sistema = false,
                            activa = active,
                            sensor_activo = selectedSensor!!.is_active ?: true
                        )
                        
                        scope.launch {
                            try {
                                val response = if (rule == null) {
                                    RetrofitClient.instance.createAlertRule("Bearer $token", request)
                                } else {
                                    RetrofitClient.instance.updateAlertRule("Bearer $token", rule.id, request)
                                }
                                
                                if (response.isSuccessful) {
                                    onSave()
                                } else {
                                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                                    Log.e("AlertRuleForm", "Error saving rule: $errorMsg")
                                    Toast.makeText(context, "Error saving rule", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("AlertRuleForm", "Network error", e)
                                Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoriotNavy),
                shape = RoundedCornerShape(8.dp),
                enabled = !isSaving && selectedApp != null && selectedSensor != null && title.isNotBlank() && variable.isNotBlank()
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                else Text("SAVE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CoriotNavy,
                focusedLabelColor = CoriotNavy
            ),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AlertRuleCard(
    rule: AlertRuleResponse,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    // Fase 3 genérica: iconos especiales para conocidas, genérico Sensors para el resto.
    val (variableIcon, iconColor) = when (rule.variable.lowercase().trim()) {
        "temp", "temperature" -> Icons.Default.DeviceThermostat to Color(0xFF1890FF)
        "humi", "humidity" -> Icons.Default.WaterDrop to Color(0xFF13C2C2)
        "batt", "battery" -> Icons.Default.BatteryChargingFull to Color(0xFF52C41A)
        "co2", "co₂" -> Icons.Default.Cloud to Color(0xFF722ED1)
        "ph" -> Icons.Default.Science to Color(0xFF13C2C2)
        "pressure", "press" -> Icons.Default.Speed to Color(0xFFFAAD14)
        "lux", "light" -> Icons.Default.LightMode to Color(0xFFFFC107)
        else -> Icons.Default.Sensors to CoriotNavy
    }

    val operatorSymbol = when (rule.operador.lowercase()) {
        "gt" -> ">"
        "gte" -> "≥"
        "lt" -> "<"
        "lte" -> "≤"
        "eq" -> "=="
        "neq" -> "≠"
        else -> rule.operador
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(variableIcon, contentDescription = null, tint = iconColor)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoriotNavy
                )
                Text(
                    text = "Sensor: ${rule.sensor_name}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Condition: ${rule.variable.uppercase()} $operatorSymbol ${rule.umbral}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Switch(
                checked = rule.activa,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CoriotNavy,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }
    }
}
