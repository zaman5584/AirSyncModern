package com.example.airsyncmodern

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.airsyncmodern.presentation.ui.screen.GamepadScreen
import com.example.airsyncmodern.presentation.ui.screen.RemoteScreen
import com.example.airsyncmodern.presentation.ui.screen.TouchpadScreen
import com.example.airsyncmodern.presentation.ui.theme.AirSyncModernTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Air Sync Remote Z
 * 
 * Annotated with @AndroidEntryPoint to enable Hilt dependency injection
 * Implements Compose Navigation for switching between Remote and Touchpad screens
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirSyncModernTheme {
                AirSyncNavigation()
            }
        }
    }
}

/**
 * Navigation component for Air Sync Remote Z
 * Routes:
 * - "remote" (default): Main remote control screen with D-Pad
 * - "touchpad": Touchpad/Air Mouse screen
 */
@Composable
fun AirSyncNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "remote"
    ) {
        composable("remote") {
            RemoteScreen(
                onNavigateToTouchpad = {
                    navController.navigate("touchpad")
                },
                onNavigateToGamepad = {
                    navController.navigate("gamepad")
                }
            )
        }
        
        composable("touchpad") {
            TouchpadScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("gamepad") {
            GamepadScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
