package com.example.reservas.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reservas.R
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import com.example.reservas.util.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.favicon_coriot),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Coriot-Lora",
                            fontFamily = SpaceMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = CoriotNavy,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Image(
                painter = painterResource(id = R.drawable.isotipo_coriot),
                contentDescription = "CorIoT Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_coriot_text),
                contentDescription = "CorIoT",
                modifier = Modifier.height(45.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp)
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceMonoFontFamily,
                        color = CoriotNavy,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Plataforma IoT LoRaWAN",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoriotDarkGray.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CoriotDarkGray,
                            unfocusedTextColor = CoriotDarkGray,
                            focusedBorderColor = CoriotNavy,
                            unfocusedBorderColor = Color(0xFFE5E6EB),
                            focusedLabelColor = CoriotNavy,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CoriotDarkGray,
                            unfocusedTextColor = CoriotDarkGray,
                            focusedBorderColor = CoriotNavy,
                            unfocusedBorderColor = Color(0xFFE5E6EB),
                            focusedLabelColor = CoriotNavy,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isLoading = true
                            scope.launch {
                                try {
                                    val trimmedUsername = username.trim()
                                    val trimmedPassword = password.trim()
                                    
                                    val response = RetrofitClient.instance.login(trimmedUsername, trimmedPassword)
                                    android.util.Log.d("Login", "Response code: ${response.code()}")
                                    
                                    if (response.isSuccessful) {
                                        val token = response.body()?.accessToken
                                        if (token != null) {
                                            val userResponse = RetrofitClient.instance.getMe("Bearer $token")
                                            val userId = userResponse.body()?.id

                                            if (userResponse.isSuccessful && userId != null) {
                                                SecurityManager.saveSession(context, token, userId)
                                                SessionManager.markAuthenticated()
                                                Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            } else {
                                                Toast.makeText(context, "No se pudo validar la sesión", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        val errorMsg = when (response.code()) {
                                            401 -> "Usuario o contraseña incorrectos"
                                            404 -> "Servicio de autenticación no encontrado"
                                            else -> "Error del servidor: ${response.code()}"
                                        }
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("Login", "Error during login", e)
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoriotNavy,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "INGRESAR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceMonoFontFamily
                            )
                        }
                    }
                    
                    TextButton(
                        onClick = { /* Forgot Password */ },
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp)
                    ) {
                        Text(
                            "¿Olvidaste tu contraseña?", 
                            color = CoriotMagenta,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    ReservasTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
