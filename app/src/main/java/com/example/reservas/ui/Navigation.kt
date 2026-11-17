package com.example.reservas.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.reservas.R
import com.example.reservas.ui.theme.*
import com.example.reservas.util.SecurityManager
import com.example.reservas.util.SessionManager
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("inicio")
    object AppDetails : Screen("app_details/{appId}") {
        fun createRoute(appId: String) = "app_details/$appId"
    }
    object SensorData : Screen("sensor_data/{appId}/{sensorNames}/{variables}") {
        fun createRoute(appId: String, sensorNames: List<String>, variables: List<String>): String {
            val names = URLEncoder.encode(sensorNames.joinToString(","), StandardCharsets.UTF_8.toString())
            val vars = URLEncoder.encode(variables.joinToString(","), StandardCharsets.UTF_8.toString())
            return "sensor_data/$appId/$names/$vars"
        }
    }
    object Notifications : Screen("notifications")
    object Alerts : Screen("alerts")
    object AppDevices : Screen("app_devices/{sensorName}") {
        fun createRoute(sensorName: String) = "app_devices/$sensorName"
    }
    object DashboardDetail : Screen("dashboard_detail/{dashboardId}") {
        fun createRoute(id: Int) = "dashboard_detail/$id"
    }
    object GatewayDetails : Screen("gateway_details/{gatewayId}") {
        fun createRoute(gatewayId: String) = "gateway_details/${URLEncoder.encode(gatewayId, StandardCharsets.UTF_8.toString())}"
    }
}

sealed class BottomNavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    object Inicio : BottomNavItem("inicio", Icons.Default.Home, "Home")
    object Dashboards : BottomNavItem("dashboards", Icons.Default.SpaceDashboard, "Dashboard")
    object Apps : BottomNavItem("apps", Icons.Default.Apps, "Apps")
    object Gateways : BottomNavItem("gateways", Icons.Default.Router, "Gateways")
    object Alerts : BottomNavItem("alerts", Icons.Default.NotificationsActive, "Alerts")
    object Perfil : BottomNavItem("perfil", Icons.Default.Person, "Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(navController: NavHostController, startDestination: String = Screen.Login.route) {
    val context = LocalContext.current
    val sessionExpired by SessionManager.sessionExpired.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            SecurityManager.clear(context)
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val showBars = (currentDestination?.route != Screen.Login.route) && (currentDestination?.route != null)

    val bottomNavItems = listOf(
        BottomNavItem.Inicio,
        BottomNavItem.Dashboards,
        BottomNavItem.Apps,
        BottomNavItem.Gateways,
        BottomNavItem.Alerts,
        BottomNavItem.Perfil
    )

    val rootScreens = listOf("inicio", "dashboards", "apps", "gateways", "alerts", "perfil")
    val currentRoute = currentDestination?.route
    val isRootScreen = currentRoute in rootScreens

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            if (showBars) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.favicon_coriot),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        navController.navigate(Screen.Main.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = false
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when {
                                    currentRoute == "inicio" -> "Home"
                                    currentRoute == "dashboards" -> "Dashboard"
                                    currentRoute == "apps" -> "Applications"
                                    currentRoute == "gateways" -> "Gateways"
                                    currentRoute == "alerts" -> "Alert Rules"
                                    currentRoute == "perfil" -> "Profile"
                                    currentRoute?.startsWith("app_details") == true -> "Sensors"
                                    currentRoute?.startsWith("app_devices") == true -> "Devices"
                                    currentRoute?.startsWith("sensor_data") == true -> "Live Telemetry"
                                    currentRoute?.startsWith("create_dashboard") == true -> "Create Dashboard"
                                    currentRoute?.startsWith("dashboard_detail") == true -> "Dashboard"
                                    else -> "CorIoT"
                                },
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    },
                    navigationIcon = {
                        if (currentRoute != null && !isRootScreen) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = CoriotNavy,
                        navigationIconContentColor = CoriotNavy
                    )
                )
            }
        },
        bottomBar = {
            if (showBars) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CoriotNavy,
                                selectedTextColor = CoriotNavy,
                                indicatorColor = CoriotNavy.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            /* Handled within screens */
        }
    ) { paddingValues ->
        NavHost(
            navController = navController, 
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate("inicio") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            // Tab destinations
            composable("inicio") {
                DashboardScreen(
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToApps = { navController.navigate("apps") },
                    onNavigateToApp = { appId -> navController.navigate(Screen.AppDetails.createRoute(appId)) },
                    onNavigateToSensor = { appId, sensorName ->
                        // Fase 3 genérica: lista vacía = mostrar todas las variables del sensor.
                        // SensorDataScreen resuelve availableKeys() cuando viene vacío.
                        navController.navigate(Screen.SensorData.createRoute(appId, listOf(sensorName), emptyList()))
                    }
                )
            }
            composable("dashboards") {
                DashboardsScreen(
                    onDashboardClick = { db -> navController.navigate(Screen.DashboardDetail.createRoute(db.id)) }
                )
            }
            composable("apps") {
                AppsListScreen(
                    onAppClick = { app ->
                        val idToNav = app.id_app_lora ?: app.id.toString()
                        navController.navigate(Screen.AppDetails.createRoute(idToNav))
                    }
                )
            }
            composable("gateways") {
                GatewaysScreen(
                    onGatewayClick = { gateway ->
                        navController.navigate(Screen.GatewayDetails.createRoute(gateway.gateway_id ?: gateway.id?.toString().orEmpty()))
                    }
                )
            }
            composable("alerts") {
                AlertRulesScreen()
            }
            composable("perfil") {
                ProfileScreen(onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }

            // Sub-destinations
            composable(
                route = Screen.AppDetails.route,
                arguments = listOf(navArgument("appId") { type = NavType.StringType })
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getString("appId") ?: ""
                AppDetailsScreen(
                    appId = appId, 
                    onBack = { navController.popBackStack() },
                    onSensorClick = { sensorName ->
                        navController.navigate(Screen.AppDevices.createRoute(sensorName))
                    }
                )
            }
            composable(
                route = Screen.AppDevices.route,
                arguments = listOf(navArgument("sensorName") { type = NavType.StringType })
            ) { backStackEntry ->
                val sensorName = backStackEntry.arguments?.getString("sensorName") ?: ""
                DevicesScreen(sensorName = sensorName, onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.SensorData.route,
                arguments = listOf(
                    navArgument("appId") { type = NavType.StringType },
                    navArgument("sensorNames") { type = NavType.StringType },
                    navArgument("variables") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getString("appId") ?: ""
                val sensorNamesStr = URLDecoder.decode(backStackEntry.arguments?.getString("sensorNames") ?: "", StandardCharsets.UTF_8.toString())
                val sensorNames = sensorNamesStr.split(",").filter { it.isNotBlank() }
                val variablesStr = URLDecoder.decode(backStackEntry.arguments?.getString("variables") ?: "", StandardCharsets.UTF_8.toString())
                val variables = variablesStr.split(",").filter { it.isNotBlank() }
                
                SensorDataScreen(
                    appId = appId, 
                    selectedSensorNames = sensorNames, 
                    selectedVariableKeys = variables,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.GatewayDetails.route,
                arguments = listOf(navArgument("gatewayId") { type = NavType.StringType })
            ) { backStackEntry ->
                val gatewayId = backStackEntry.arguments?.getString("gatewayId").orEmpty()
                GatewayDetailsScreen(gatewayId = gatewayId, onBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.DashboardDetail.route,
                arguments = listOf(navArgument("dashboardId") { type = NavType.IntType })
            ) { backStackEntry ->
                val dashboardId = backStackEntry.arguments?.getInt("dashboardId")
                if (dashboardId != null) {
                    DashboardDetailScreen(
                        dashboardId = dashboardId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}


