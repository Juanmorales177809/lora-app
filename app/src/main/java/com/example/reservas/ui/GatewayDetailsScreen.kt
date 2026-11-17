package com.example.reservas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.reservas.R
import com.example.reservas.data.model.GatewayLocationUpdate
import com.example.reservas.data.model.GatewayResponse
import com.example.reservas.data.model.GatewayUpdateEnvelope
import com.example.reservas.data.model.GatewayUpdateRequest
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import com.example.reservas.util.formatLastSeen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource

@Composable
fun GatewayDetailsScreen(gatewayId: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var gateway by remember { mutableStateOf<GatewayResponse?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gatewayId) {
        try {
            val token = SecurityManager.getToken(context)
            if (token.isNullOrBlank()) {
                error = "Session unavailable"
                return@LaunchedEffect
            }
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.instance.getGateway("Bearer $token", gatewayId)
            }
            if (response.isSuccessful) {
                response.body()?.let { detail ->
                    gateway = detail.gateway.copy(last_seen = detail.lastSeenAt)
                } ?: run { error = "Gateway not found" }
            } else {
                error = "Unable to load gateway (${response.code()})"
            }
        } catch (e: Exception) {
            error = "Connection error"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val current = gateway
        Text(
            text = current?.name ?: gatewayId,
            style = MaterialTheme.typography.headlineSmall,
            color = CoriotNavy
        )
        Text(
            text = "ID: ${current?.gateway_id ?: gatewayId}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        TabRow(selectedTabIndex = selectedTab) {
            listOf("Dashboard", "Configuration", "LoRaWan frames").forEachIndexed { index, label ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
            }
        }

        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp), color = CoriotNavy)
            error != null -> Text(error.orEmpty(), color = Color.Gray, modifier = Modifier.padding(top = 24.dp))
            else -> when (selectedTab) {
                0 -> GatewayDashboardTab(current)
                1 -> GatewayConfigurationTab(
                    gateway = current,
                    gatewayId = gatewayId,
                    onGatewayUpdated = { updated -> gateway = updated }
                )
                2 -> GatewayFramesTab()
            }
        }
    }
}

@Composable
private fun GatewayDashboardTab(gateway: GatewayResponse?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Gateway information", color = CoriotNavy, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Last seen: ${formatLastSeen(gateway?.last_seen)}", color = Color.Gray)
                Text("Region ID: ${gateway?.regionId() ?: "N/A"}", color = Color.Gray)
            }
        }
        GatewayMap(gateway)
    }
}

@Composable
private fun GatewayMap(gateway: GatewayResponse?) {
    val location = gateway?.location ?: return
    val latitude = location.latitude ?: return
    val longitude = location.longitude ?: return
    val context = androidx.compose.ui.platform.LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE9EEF2))
    ) {
        AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val osmdroidConfig = Configuration.getInstance()
            osmdroidConfig.userAgentValue = "com.example.reservas/0.2.4 (contact: soporte@coriotlab.co)"
            osmdroidConfig.additionalHttpRequestProperties["User-Agent"] =
                "com.example.reservas/0.2.4 (contact: soporte@coriotlab.co)"
            MapView(context).also { createdMap ->
                mapView = createdMap
            }.apply {
                setTileSource(
                    XYTileSource(
                        "OpenStreetMap HOT",
                        0,
                        19,
                        256,
                        ".png",
                        arrayOf(
                            "https://a.tile.openstreetmap.fr/hot/",
                            "https://b.tile.openstreetmap.fr/hot/",
                            "https://c.tile.openstreetmap.fr/hot/"
                        )
                    )
                )
                setMultiTouchControls(true)
                setBuiltInZoomControls(false)
                setUseDataConnection(true)
            }
        },
        update = { mapView ->
            val point = GeoPoint(latitude, longitude)
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(point)
            mapView.overlays.clear()
            mapView.overlays.add(Marker(mapView).apply {
                position = point
                title = gateway.name ?: gateway.gateway_id
                icon = ContextCompat.getDrawable(context, R.drawable.ic_gateway_marker)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
            mapView.invalidate()
        }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmallMapControl(label = "+") { mapView?.controller?.zoomIn() }
            SmallMapControl(label = "−") { mapView?.controller?.zoomOut() }
        }

        Text(
            text = "© OpenStreetMap contributors",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            color = Color.DarkGray,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SmallMapControl(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 20.sp, color = CoriotNavy)
        }
    }
}

@Composable
private fun GatewayConfigurationTab(
    gateway: GatewayResponse?,
    gatewayId: String,
    onGatewayUpdated: (GatewayResponse) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(gateway?.gateway_id) { mutableStateOf(gateway?.name.orEmpty()) }
    var description by remember(gateway?.gateway_id) { mutableStateOf(gateway?.description.orEmpty()) }
    var latitude by remember(gateway?.gateway_id) { mutableStateOf(gateway?.location?.latitude?.toString().orEmpty()) }
    var longitude by remember(gateway?.gateway_id) { mutableStateOf(gateway?.location?.longitude?.toString().orEmpty()) }
    var altitude by remember(gateway?.gateway_id) { mutableStateOf(gateway?.location?.altitude?.toString().orEmpty()) }
    var statsInterval by remember(gateway?.gateway_id) { mutableStateOf(gateway?.statsInterval?.toString().orEmpty()) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Configuration", style = MaterialTheme.typography.titleMedium, color = CoriotNavy)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = latitude,
                onValueChange = { latitude = it },
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = longitude,
                onValueChange = { longitude = it },
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        OutlinedTextField(
            value = altitude,
            onValueChange = { altitude = it },
            label = { Text("Altitude") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            value = statsInterval,
            onValueChange = { statsInterval = it },
            label = { Text("Stats Interval (secs)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = {
                val parsedLatitude = latitude.toDoubleOrNull()
                val parsedLongitude = longitude.toDoubleOrNull()
                val parsedAltitude = altitude.toDoubleOrNull()
                val parsedInterval = statsInterval.toIntOrNull()
                if (parsedLatitude == null || parsedLongitude == null || parsedAltitude == null || parsedInterval == null) {
                    Toast.makeText(context, "Check coordinates and stats interval", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val token = SecurityManager.getToken(context) ?: return@Button
                isSubmitting = true
                scope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.instance.updateGateway(
                                token = "Bearer $token",
                                gatewayId = gatewayId,
                                request = GatewayUpdateEnvelope(
                                    gateway = GatewayUpdateRequest(
                                        name = name,
                                        description = description,
                                        location = GatewayLocationUpdate(
                                            latitude = parsedLatitude,
                                            longitude = parsedLongitude,
                                            altitude = parsedAltitude,
                                            source = gateway?.location?.source ?: "UNKNOWN",
                                            accuracy = gateway?.location?.accuracy ?: 0.0
                                        ),
                                        metadata = gateway?.metadata.orEmpty(),
                                        tags = gateway?.tags.orEmpty(),
                                        statsInterval = parsedInterval,
                                        tenantId = gateway?.tenantId
                                    ),
                                )
                            )
                        }
                        if (response.isSuccessful) {
                            onGatewayUpdated(
                                gateway?.copy(
                                    name = name,
                                    description = description,
                                    location = gateway.location?.copy(
                                        latitude = parsedLatitude,
                                        longitude = parsedLongitude,
                                        altitude = parsedAltitude
                                    ),
                                    statsInterval = parsedInterval
                                ) ?: return@launch
                            )
                            Toast.makeText(context, "Gateway updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Unable to update gateway (${response.code()})", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GatewayDetails", "Error updating gateway", e)
                        Toast.makeText(context, "Connection error: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CoriotNavy),
            modifier = Modifier.wrapContentWidth(),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
            else Text("Submit", fontWeight = FontWeight.Bold)
        }
    }
}

private fun GatewayResponse.regionId(): String? =
    properties["region_config_id"] ?: metadata["region_config_id"]

@Composable
private fun GatewayFramesTab() {
    Box(Modifier.fillMaxSize().padding(top = 16.dp)) {
        Text("No LoRaWan frames available.", color = Color.Gray)
    }
}
