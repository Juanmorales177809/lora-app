package com.example.reservas

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.reservas.data.network.RetrofitClient
import com.example.reservas.ui.AppNavigation
import com.example.reservas.ui.Screen
import com.example.reservas.ui.theme.CoriotNavy
import com.example.reservas.ui.theme.ReservasTheme
import com.example.reservas.util.SecurityManager
import com.example.reservas.util.SessionManager

private const val SESSION_VALIDATION_ERROR = "__session_validation_error__"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ReservasTheme {
                var startScreen by remember { mutableStateOf<String?>(null) }
                var retryKey by remember { mutableStateOf(0) }

                LaunchedEffect(retryKey) {
                    startScreen = null
                    val token = SecurityManager.getToken(this@MainActivity)

                    if (token.isNullOrBlank()) {
                        startScreen = Screen.Login.route
                        return@LaunchedEffect
                    }

                    try {
                        val response = RetrofitClient.instance.getMe("Bearer $token")
                        startScreen = if (response.isSuccessful) {
                            SessionManager.markAuthenticated()
                            Screen.Main.route
                        } else if (response.code() == 401 || response.code() == 403) {
                            SecurityManager.clear(this@MainActivity)
                            Screen.Login.route
                        } else {
                            Log.w(
                                "MainActivity",
                                "Unable to validate session at startup. HTTP ${response.code()}"
                            )
                            SESSION_VALIDATION_ERROR
                        }
                    } catch (e: Exception) {
                        Log.w("MainActivity", "Unable to validate session at startup", e)
                        startScreen = SESSION_VALIDATION_ERROR
                    }
                }

                val destination = startScreen
                when (destination) {
                    null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CoriotNavy)
                        }
                    }

                    SESSION_VALIDATION_ERROR -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(text = "No se pudo validar la sesión")
                                Button(onClick = { retryKey++ }) {
                                    Text(text = "Reintentar")
                                }
                            }
                        }
                    }

                    else -> {
                        val navController = rememberNavController()
                        AppNavigation(
                            navController = navController,
                            startDestination = destination
                        )
                    }
                }
            }
        }
    }
}
