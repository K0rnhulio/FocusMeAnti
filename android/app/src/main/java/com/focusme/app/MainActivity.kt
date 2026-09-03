package com.focusme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focusme.app.ui.screens.DashboardScreen
import com.focusme.app.ui.screens.MazeGameScreen
import com.focusme.app.ui.screens.PushUpCounterScreen
import com.focusme.app.ui.screens.SettingsScreen
import com.focusme.app.ui.screens.ShakeChallengeScreen
import com.focusme.app.ui.theme.AccentCyan
import com.focusme.app.ui.theme.AccentIndigo
import com.focusme.app.ui.theme.BgDark
import com.focusme.app.ui.theme.FocusMeTheme
import com.focusme.app.ui.theme.PrimaryGradient
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Floating Glass Pill Navigation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(20.dp, CircleShape, spotColor = AccentIndigo.copy(alpha = 0.35f))
                            .clip(CircleShape)
                            .background(Color(0xE60F172A))
                            .border(1.dp, Color(0x3338BDF8), CircleShape)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernNavItem(
                            title = "Dashboard",
                            icon = Icons.Rounded.Dashboard,
                            isSelected = selectedTab == "dashboard",
                            onClick = {
                                selectedTab = "dashboard"
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        )

                        ModernNavItem(
                            title = "Gauntlet",
                            icon = Icons.Rounded.FitnessCenter,
                            isSelected = selectedTab == "challenges",
                            onClick = {
                                selectedTab = "challenges"
                                navController.navigate("maze")
                            }
                        )

                        ModernNavItem(
                            title = "Shields",
                            icon = Icons.Rounded.Settings,
                            isSelected = selectedTab == "settings",
                            onClick = {
                                selectedTab = "settings"
                                navController.navigate("settings")
                            }
                        )
                    }
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
}

@Composable
fun ModernNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) AccentIndigo.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_bg"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AccentCyan else TextDim,
        label = "nav_icon"
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )

        AnimatedVisibility(visible = isSelected) {
            Text(
                text = "  $title",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
        }
    }
}
