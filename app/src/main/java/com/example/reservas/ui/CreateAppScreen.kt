package com.example.reservas.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reservas.R
import com.example.reservas.data.model.ApplicationCreate
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAppScreen(onBack: () -> Unit, onSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }
    var useExistingApp by remember { mutableStateOf(false) }
    var appIdInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = CoriotDarkGray,
                unfocusedTextColor = CoriotDarkGray,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )

        OutlinedTextField(
            value = observations,
            onValueChange = { observations = it },
            label = { Text("Observations") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = CoriotDarkGray,
                unfocusedTextColor = CoriotDarkGray,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = useExistingApp,
                onCheckedChange = { useExistingApp = it },
                colors = CheckboxDefaults.colors(checkedColor = CoriotNavy),
                enabled = !isLoading
            )
            Text(
                text = "Use existing application",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (useExistingApp) {
            OutlinedTextField(
                value = appIdInput,
                onValueChange = { appIdInput = it },
                label = { Text("LoRa Application ID (UUID)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CoriotDarkGray,
                    unfocusedTextColor = CoriotDarkGray,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (name.isBlank()) {
                    Toast.makeText(context, "Enter the name", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (useExistingApp && appIdInput.isBlank()) {
                    Toast.makeText(context, "Enter the application ID", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                val token = SecurityManager.getToken(context)
                val userId = SecurityManager.getUserId(context)
                
                if (token == null || userId == -1) {
                    Toast.makeText(context, "Session error", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                scope.launch {
                    try {
                        // Verificamos si la aplicación existe si se marcó el checkbox
                        if (useExistingApp) {
                            val sensorsResponse = RetrofitClient.instance.getSensorsByApplication("Bearer $token", appIdInput)
                            if (!sensorsResponse.isSuccessful) {
                                Toast.makeText(context, "The application does not exist in the IoT system", Toast.LENGTH_LONG).show()
                                isLoading = false
                                return@launch
                            }
                        }

                        val appRequest = ApplicationCreate(
                            nombre = name,
                            observaciones = observations,
                            idUsuario = userId,
                            id_app_lora = if (useExistingApp) appIdInput else null
                        )
                        
                        val response = RetrofitClient.instance.createApplication("Bearer $token", appRequest)
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Application saved", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } else {
                            Toast.makeText(context, "Error saving: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoriotNavy),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    "SAVE",
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceMonoFontFamily
                )
            }
        }
    }
}
