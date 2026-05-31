package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.advanced.AdvancedScreen
import com.example.ui.home.HomeScreen
import com.example.ui.radar.RadarScreen
import com.example.ui.satellite.SatelliteScreen
import com.example.ui.alerts.AlertsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()

    val navItems = listOf(
        Pair("home", "Acasă" to Icons.Default.Home),
        Pair("radar", "Radar" to Icons.Default.Radar),
        Pair("satellite", "Satelit" to Icons.Default.Satellite),
        Pair("advanced", "Avansat" to Icons.Default.Analytics),
        Pair("alerts", "Alerte" to Icons.Default.Warning)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { (route, info) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(info.second, contentDescription = info.first) },
                        label = { Text(info.first) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(onNavigateToAdvanced = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("selected_tab", "Info")
                    navController.navigate("advanced") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable("radar") { RadarScreen() }
            composable("satellite") { SatelliteScreen() }
            composable("advanced") {
                val selectedTab = navController.previousBackStackEntry?.savedStateHandle?.remove<String>("selected_tab") ?: ""
                AdvancedScreen(initialTab = selectedTab)
            }
            composable("alerts") { AlertsScreen() }
        }
    }
}
