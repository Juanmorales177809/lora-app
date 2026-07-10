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
import androidx.navigation.NavController
import com.example.reservas.data.model.*
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsListScreen(onAppClick: (ApplicationResponse) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<ApplicationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var showSheet by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<ApplicationResponse?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var appToDelete by remember { mutableStateOf<ApplicationResponse?>(null) }

    fun loadApps() {
        val token = SecurityManager.getToken(context)
        if (token != null) {
            isLoading = true
            scope.launch {
                try {
                    val response = RetrofitClient.instance.getApplications("Bearer $token")
                    if (response.isSuccessful) {
                        apps = response.body()?.applications ?: emptyList()
                    }
                } catch (e: Exception) {
                    Log.e("AppsList", "Error loading apps", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadApps()
    }

    if (appToDelete != null) {
        AlertDialog(
            onDismissRequest = { appToDelete = null },
            title = { Text("Delete Application") },
            text = { Text("Are you sure you want to delete \"${appToDelete?.nombre}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val token = SecurityManager.getToken(context)
                        val toDelete = appToDelete ?: return@TextButton
                        if (token != null) {
                            scope.launch {
                                try {
                                    val response = RetrofitClient.instance.deleteApplication("Bearer $token", toDelete.id)
                                    if (response.isSuccessful) {
                                        loadApps()
                                        Toast.makeText(context, "Application deleted", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show()
                                } finally {
                                    appToDelete = null
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { appToDelete = null }) {
                    Text("Cancel")
                }
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
                    selectedApp = null
                    showSheet = true
                },
                containerColor = CoriotNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Application")
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
                text = "Applications",
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
            } else if (apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No applications found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(apps) { app ->
                        AppCard(
                            app = app,
                            onClick = { onAppClick(app) },
                            onEdit = {
                                selectedApp = app
                                showSheet = true
                            },
                            onDelete = { appToDelete = app }
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
                    AppForm(
                        app = selectedApp,
                        onSave = {
                            showSheet = false
                            loadApps()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppForm(
    app: ApplicationResponse?,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf(app?.nombre ?: "") }
    var observations by remember { mutableStateOf(app?.observaciones ?: "") }
    var idAppLora by remember { mutableStateOf(app?.id_app_lora ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (app == null) "Create Application" else "Edit Application",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CoriotNavy,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSaving
        )

        OutlinedTextField(
            value = observations,
            onValueChange = { observations = it },
            label = { Text("Observations") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(12.dp),
            enabled = !isSaving
        )

        OutlinedTextField(
            value = idAppLora,
            onValueChange = { idAppLora = it },
            label = { Text("LoRa Application ID (UUID)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSaving
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isBlank()) {
                    Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                val token = SecurityManager.getToken(context)
                val userId = SecurityManager.getUserId(context)
                if (token == null) {
                    Toast.makeText(context, "Session error", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isSaving = true
                scope.launch {
                    try {
                        val response = if (app == null) {
                            val createReq = ApplicationCreate(
                                nombre = name,
                                observaciones = observations,
                                idUsuario = userId,
                                id_app_lora = idAppLora.ifBlank { null }
                            )
                            RetrofitClient.instance.createApplication("Bearer $token", createReq)
                        } else {
                            val updateReq = ApplicationUpdate(
                                nombre = name,
                                observaciones = observations,
                                id_app_lora = idAppLora.ifBlank { null }
                            )
                            RetrofitClient.instance.updateApplication("Bearer $token", app.id, updateReq)
                        }
                        
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
                            onSave()
                        } else {
                            Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Connection error", Toast.LENGTH_LONG).show()
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
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    if (app == null) "SAVE" else "SAVE CHANGES",
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceMonoFontFamily
                )
            }
        }
    }
}

@Composable
fun AppCard(app: ApplicationResponse, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
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
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = CoriotNavy.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = CoriotNavy)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoriotNavy
                )
                Text(
                    text = "ID: ${app.id_app_lora ?: "N/A"}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Settings view coming soon",
            fontFamily = SpaceMonoFontFamily,
            fontSize = 18.sp,
            color = Color.Gray
        )
    }
}
