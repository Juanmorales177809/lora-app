package com.example.reservas.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.R
import com.example.reservas.data.model.NotificationResponse
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import com.example.reservas.util.formatDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<NotificationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadNotifications() {
        val token = SecurityManager.getToken(context)
        
        if (token != null) {
            scope.launch {
                try {
                    val response = RetrofitClient.instance.getMyNotifications("Bearer $token")
                    if (response.isSuccessful) {
                        notifications = response.body()?.notifications ?: emptyList()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al cargar notificaciones", Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadNotifications()
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
        } else if (notifications.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.LightGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No notifications available",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = {
                            val token = SecurityManager.getToken(context)
                            if (token != null && !notification.leida) {
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.instance.markNotificationAsRead("Bearer $token", notification.id)
                                        if (response.isSuccessful) {
                                            // Update local state to show it's read
                                            notifications = notifications.map {
                                                if (it.id == notification.id) it.copy(leida = true) else it
                                            }
                                            
                                            // Prepare navigation logic
                                            handleNotificationClick(notification, context)
                                        }
                                    } catch (e: Exception) {
                                        // Optional: Handle error
                                    }
                                }
                            } else {
                                handleNotificationClick(notification, context)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun handleNotificationClick(notification: NotificationResponse, context: android.content.Context) {
    if (!notification.referencia_tipo.isNullOrBlank() && !notification.referencia_id.isNullOrBlank()) {
        // Navigation logic based on reference type
        // E.g., if type is "sensor", navigate to AppDetails or SensorData
        Toast.makeText(context, "Ver detalle: ${notification.referencia_id}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun NotificationCard(notification: NotificationResponse, onMarkAsRead: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMarkAsRead),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.leida) Color.White else Color(0xFFF0F7FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (notification.leida) Color.Gray.copy(alpha = 0.1f) else CoriotNavy.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (notification.leida) Color.Gray else CoriotNavy,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.titulo,
                    fontSize = 16.sp,
                    fontWeight = if (notification.leida) FontWeight.Medium else FontWeight.Bold,
                    color = CoriotNavy
                )
                Text(
                    text = notification.mensaje,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(notification.creada_en),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            if (!notification.leida) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = Color.Red
                ) {}
            }
        }
    }
}
