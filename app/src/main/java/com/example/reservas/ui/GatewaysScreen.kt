package com.example.reservas.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.data.model.GatewayResponse
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import com.example.reservas.util.formatLastSeen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GatewaysScreen(onGatewayClick: (GatewayResponse) -> Unit) {
    val context = LocalContext.current
    var gateways by remember { mutableStateOf<List<GatewayResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = SecurityManager.getToken(context)
        if (token.isNullOrBlank()) {
            isLoading = false
            error = "Session unavailable"
            return@LaunchedEffect
        }
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.instance.getGateways("Bearer $token", limit = 10)
            }
            if (response.isSuccessful) gateways = response.body()?.gateways.orEmpty()
            else error = "Unable to load gateways (${response.code()})"
        } catch (e: Exception) {
            Log.e("GatewaysScreen", "Error loading gateways", e)
            error = "Connection error"
        } finally {
            isLoading = false
        }
    }

    Scaffold(containerColor = BackgroundLight) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Gateways",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceMonoFontFamily,
                color = CoriotNavy,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CoriotNavy)
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error.orEmpty(), color = Color.Gray)
                }
                gateways.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No gateways found.", color = Color.Gray)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(gateways) { gateway -> GatewayCard(gateway, onClick = { onGatewayClick(gateway) }) }
                }
            }
        }
    }
}

@Composable
private fun GatewayCard(gateway: GatewayResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = gateway.name ?: gateway.gateway_id ?: "Gateway",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CoriotNavy
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${gateway.gateway_id ?: gateway.id ?: "N/A"}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                gateway.status?.takeIf { it.isNotBlank() }?.let { status ->
                    val isOnline = status.equals("ONLINE", ignoreCase = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                        ) {}
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = status.uppercase(),
                            color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            gateway.last_seen?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Last seen: ${formatLastSeen(it)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
