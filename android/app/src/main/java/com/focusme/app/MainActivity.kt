package com.focusme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focusme.app.ui.screens.DashboardScreen
import com.focusme.app.ui.screens.MazeGameScreen
import com.focusme.app.ui.screens.PushUpCounterScreen
import com.focusme.app.ui.screens.SettingsScreen
import com.focusme.app.ui.screens.ShakeChallengeScreen
import com.focusme.app.ui.theme.AccentCyan
import com.focusme.app.ui.theme.CardDark
import com.focusme.app.ui.theme.FocusMeTheme
import com.focusme.app.ui.theme.TextDim
import com.focusme.app.ui.theme.TextMain

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusMeTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf("dashboard") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardDark
            ) {
                NavigationBarItem(
                    selected = selectedTab == "dashboard",
                    onClick = {
                        selectedTab = "dashboard"
                        navController.navigate("dashboard")
                    },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        selectedTextColor = TextMain,
                        unselectedIconColor = TextDim,
                        unselectedTextColor = TextDim
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "challenges",
                    onClick = {
                        selectedTab = "challenges"
                        navController.navigate("maze")
                    },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                    label = { Text("Gauntlet") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        selectedTextColor = TextMain,
                        unselectedIconColor = TextDim,
                        unselectedTextColor = TextDim
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "settings",
                    onClick = {
                        selectedTab = "settings"
                        navController.navigate("settings")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        selectedTextColor = TextMain,
                        unselectedIconColor = TextDim,
                        unselectedTextColor = TextDim
                    )
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onLaunchMaze = { navController.navigate("maze") },
                    onLaunchShakes = { navController.navigate("shakes") },
                    onLaunchPushUps = { navController.navigate("pushups") }
                )
            }
            composable("maze") {
                MazeGameScreen(onCompleted = { navController.popBackStack() })
            }
            composable("shakes") {
                ShakeChallengeScreen(onCompleted = { navController.popBackStack() })
            }
            composable("pushups") {
                PushUpCounterScreen(onCompleted = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}
