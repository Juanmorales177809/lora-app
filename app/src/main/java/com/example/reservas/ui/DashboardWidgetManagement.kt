package com.example.reservas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.reservas.data.model.DashboardWidget
import com.example.reservas.data.model.SensorLatestResponse
import com.example.reservas.ui.theme.CoriotNavy

data class AddWidgetSelection(
    val option: DashboardSensorOption,
    val variableKey: String,
    val chartType: String?
)

@Composable
fun DashboardWidgetsGrid(
    widgets: List<DashboardWidget>,
    sensorsData: Map<String, SensorLatestResponse>,
    charts: Boolean,
    onEditChart: (DashboardWidget) -> Unit = {},
    onDeleteWidget: (DashboardWidget) -> Unit = {}
) {
    if (widgets.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (charts) "No chart widgets configured" else "No overview widgets configured",
                color = Color.Gray
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(
            items = widgets,
            key = { it.id },
            span = { widget ->
                if (charts) GridItemSpan(maxLineSpan) else GridItemSpan(1)
            }
        ) { widget ->
            val sensor = sensorsData[widget.sensor_name]
            // Fase 2 genérica: lookup en mapa, compat con temp/humi/batt vía getVariable().
            val value = sensor?.getVariable(widget.variable_key)

            if (charts) {
                HistoricalChartWidget(
                    widget = widget,
                    liveValue = value,
                    onEdit = { onEditChart(widget) },
                    onDelete = { onDeleteWidget(widget) }
                )
            } else {
                RefinedWidgetCard(
                    widget = widget,
                    value = value,
                    lastUpdate = sensor?.last_update,
                    isActive = sensor?.is_active,
                    batteryLevel = sensor?.battery_level,
                    onDelete = { onDeleteWidget(widget) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDashboardWidgetDialog(
    mode: String,
    sensorOptions: List<DashboardSensorOption>,
    initialWidget: DashboardWidget? = null,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAdd: (AddWidgetSelection) -> Unit
) {
    var selectedOption by remember(sensorOptions, initialWidget?.id) {
        mutableStateOf(
            initialWidget?.let { widget ->
                sensorOptions.firstOrNull {
                    it.applicationId == widget.application_id &&
                        it.sensor.sensor_name == widget.sensor_name
                }
            } ?: sensorOptions.firstOrNull()
        )
    }
    var selectedVariable by remember(initialWidget?.id) {
        mutableStateOf(initialWidget?.variable_key)
    }
    var selectedChartType by remember(initialWidget?.id) {
        mutableStateOf(initialWidget?.resolvedChartType() ?: "line_chart")
    }
    var sensorMenuExpanded by remember { mutableStateOf(false) }
    var variableMenuExpanded by remember { mutableStateOf(false) }
    var chartTypeMenuExpanded by remember { mutableStateOf(false) }

    val variables = selectedOption?.sensor?.let(::availableWidgetVariables).orEmpty()
    val chartTypes = listOf(
        "line_chart" to "Line chart",
        "bar_chart" to "Bar chart",
        "scatter_chart" to "Scatter plot",
        "table" to "Table (last 5)"
    )
    val isChart = mode == "Charts"
    val canAdd = selectedOption != null && selectedVariable != null && !isSaving

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = when {
                    initialWidget != null -> "Edit chart widget"
                    isChart -> "Add chart widget"
                    else -> "Add overview widget"
                },
                color = CoriotNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sensorOptions.isEmpty()) {
                    Text("No sensor nodes available.", color = Color.Gray)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = sensorMenuExpanded,
                        onExpandedChange = { sensorMenuExpanded = !sensorMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedOption?.sensor?.sensor_name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sensor") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(sensorMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = sensorMenuExpanded,
                            onDismissRequest = { sensorMenuExpanded = false }
                        ) {
                            sensorOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.sensor.sensor_name) },
                                    onClick = {
                                        selectedOption = option
                                        selectedVariable = null
                                        sensorMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = variableMenuExpanded,
                        onExpandedChange = {
                            if (variables.isNotEmpty()) variableMenuExpanded = !variableMenuExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedVariable?.let(::telemetryVariableLabel).orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Variable") },
                            enabled = variables.isNotEmpty(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(variableMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = variableMenuExpanded,
                            onDismissRequest = { variableMenuExpanded = false }
                        ) {
                            variables.forEach { variable ->
                                DropdownMenuItem(
                                    text = { Text(telemetryVariableLabel(variable)) },
                                    onClick = {
                                        selectedVariable = variable
                                        variableMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (isChart) {
                        ExposedDropdownMenuBox(
                            expanded = chartTypeMenuExpanded,
                            onExpandedChange = {
                                chartTypeMenuExpanded = !chartTypeMenuExpanded
                            }
                        ) {
                            OutlinedTextField(
                                value = chartTypes.first { it.first == selectedChartType }.second,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Chart type") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(chartTypeMenuExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = chartTypeMenuExpanded,
                                onDismissRequest = { chartTypeMenuExpanded = false }
                            ) {
                                chartTypes.forEach { (type, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedChartType = type
                                            chartTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val option = selectedOption ?: return@Button
                    val variable = selectedVariable ?: return@Button
                    onAdd(
                        AddWidgetSelection(
                            option = option,
                            variableKey = variable,
                            chartType = selectedChartType.takeIf { isChart }
                        )
                    )
                },
                enabled = canAdd,
                colors = ButtonDefaults.buttonColors(containerColor = CoriotNavy)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (initialWidget != null) "Save" else "Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

private fun availableWidgetVariables(sensor: SensorLatestResponse): List<String> {
    // Fase 2 genérica: todas las keys disponibles, ordenadas.
    return sensor.availableKeys().sorted()
}