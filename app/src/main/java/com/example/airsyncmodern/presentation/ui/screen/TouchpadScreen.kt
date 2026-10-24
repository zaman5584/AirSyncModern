package com.example.airsyncmodern.presentation.ui.screen

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.airsyncmodern.data.network.model.ConnectionState
import com.example.airsyncmodern.presentation.ui.theme.*
import com.example.airsyncmodern.presentation.viewmodel.RemoteViewModel
import kotlin.math.abs

/**
 * Touchpad Screen for Air Mouse / Touch control
 * 
 * Features:
 * - Large touch/swipe area for relative mouse movement
 * - Left/Right click buttons
 * - Vertical scroll zone on the right
 * - Air Mouse toggle (gyroscope-based)
 * - Material 3 Dark OLED theme
 * - Haptic feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadScreen(
    viewModel: RemoteViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current

    Scaffold(
        containerColor = md_theme_dark_background,
        topBar = {
            TouchpadTopBar(
                connectionState = uiState.connectionState,
                deviceName = uiState.connectedDevice?.getDisplayName() ?: "No Device",
                isAirMouseActive = uiState.isAirMouseActive,
                onToggleAirMouse = viewModel::toggleAirMouse,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        if (uiState.isConnected) {
            TouchpadLayout(
                onMouseMove = { deltaX, deltaY ->
                    viewModel.sendMouseMove(deltaX, deltaY)
                },
                onLeftClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    viewModel.sendMouseLeftClick()
                },
                onRightClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    viewModel.sendMouseRightClick()
                },
                onScroll = { delta ->
                    viewModel.sendMouseScroll(delta)
                },
                isAirMouseActive = uiState.isAirMouseActive,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // Not connected state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = md_theme_dark_onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Device Connected",
                        style = MaterialTheme.typography.headlineMedium,
                        color = md_theme_dark_onSurface
                    )
                    Text(
                        text = "Connect to a device to use touchpad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = md_theme_dark_onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Top bar for touchpad screen
 */
@Composable
private fun TouchpadTopBar(
    connectionState: ConnectionState,
    deviceName: String,
    isAirMouseActive: Boolean,
    onToggleAirMouse: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = md_theme_dark_surface,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = md_theme_dark_onSurface
                    )
                }
                
                Column {
                    Text(
                        text = "Touchpad / Air Mouse",
                        style = MaterialTheme.typography.titleMedium,
                        color = md_theme_dark_onSurface
                    )
                    if (connectionState is ConnectionState.Connected) {
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = md_theme_dark_onSurfaceVariant
                        )
                    }
                }
            }
            
            // Air Mouse Toggle
            Surface(
                onClick = onToggleAirMouse,
                shape = RoundedCornerShape(20.dp),
                color = if (isAirMouseActive) md_theme_dark_primary else md_theme_dark_surfaceVariant,
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAirMouseActive) Icons.Default.PhonelinkRing else Icons.Default.PhonelinkOff,
                        contentDescription = null,
                        tint = if (isAirMouseActive) md_theme_dark_onPrimary else md_theme_dark_onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isAirMouseActive) "Air Mouse ON" else "Air Mouse OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAirMouseActive) md_theme_dark_onPrimary else md_theme_dark_onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Main touchpad layout with touch area, buttons, and scroll zone
 */
@Composable
private fun TouchpadLayout(
    onMouseMove: (Int, Int) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onScroll: (Int) -> Unit,
    isAirMouseActive: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var lastDragPosition by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status indicator
        if (isAirMouseActive) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = md_theme_dark_primary.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = md_theme_dark_primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Air Mouse Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = md_theme_dark_onSurface
                        )
                        Text(
                            text = "Move your phone to control the cursor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = md_theme_dark_onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Main touchpad area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Touch/swipe area
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = md_theme_dark_surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    lastDragPosition = offset
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    
                                    // Convert drag to mouse movement
                                    val deltaX = (dragAmount.x * 0.5f).toInt()
                                    val deltaY = (dragAmount.y * 0.5f).toInt()
                                    
                                    if (abs(deltaX) > 0 || abs(deltaY) > 0) {
                                        onMouseMove(deltaX, deltaY)
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    onLeftClick()
                                    onLeftClick()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Touchpad indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = md_theme_dark_onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Swipe to Move",
                            style = MaterialTheme.typography.bodyMedium,
                            color = md_theme_dark_onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Double Tap = Double Click",
                            style = MaterialTheme.typography.labelSmall,
                            color = md_theme_dark_onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            // Scroll zone
            Card(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = md_theme_dark_surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                
                                // Convert vertical drag to scroll
                                val scrollDelta = (-dragAmount.y * 0.05f).toInt()
                                
                                if (abs(scrollDelta) > 0) {
                                    onScroll(scrollDelta)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = null,
                            tint = md_theme_dark_onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Scroll",
                            style = MaterialTheme.typography.labelSmall,
                            color = md_theme_dark_onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Click buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Click Button
            Button(
                onClick = onLeftClick,
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = md_theme_dark_primary,
                    contentColor = md_theme_dark_onPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Left Click",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            // Right Click Button
            Button(
                onClick = onRightClick,
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = md_theme_dark_surfaceVariant,
                    contentColor = md_theme_dark_onSurface
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Right Click",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
