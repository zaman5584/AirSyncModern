package com.example.airsyncmodern.presentation.ui.screen

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.airsyncmodern.data.network.model.ConnectionState
import com.example.airsyncmodern.data.network.remote.UpnpRemoteController
import com.example.airsyncmodern.presentation.ui.components.DeviceDiscoveryBottomSheet
import com.example.airsyncmodern.presentation.ui.theme.*
import com.example.airsyncmodern.presentation.viewmodel.RemoteViewModel

/**
 * Main Remote Control Screen for Air Sync Remote Z
 * 
 * Features:
 * - Material 3 Dark theme with OLED ergonomics
 * - Tactile haptic feedback on all button presses
 * - Thumb-zone optimized layout (D-Pad in bottom 60%)
 * - Real-time connection status and latency display
 * - Device discovery bottom sheet
 * - Navigation to Touchpad/Air Mouse screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel = hiltViewModel(),
    onNavigateToTouchpad: () -> Unit,
    onNavigateToGamepad: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    
    var showDiscoverySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show discovery sheet if not connected
    LaunchedEffect(uiState.isConnected) {
        if (!uiState.isConnected && !showDiscoverySheet) {
            showDiscoverySheet = true
        }
    }

    Scaffold(
        containerColor = md_theme_dark_background,
        topBar = {
            RemoteTopBar(
                connectionState = uiState.connectionState,
                deviceName = uiState.connectedDevice?.getDisplayName() ?: "No Device",
                latencyMs = uiState.latencyMs,
                onDisconnect = viewModel::disconnect,
                onShowDiscovery = { showDiscoverySheet = true },
                onNavigateToTouchpad = onNavigateToTouchpad,
                onNavigateToGamepad = onNavigateToGamepad
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isConnected) {
                RemoteControlLayout(
                    onKeyPress = { key ->
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        viewModel.sendKey(key)
                    },
                    isCommandInProgress = uiState.isCommandInProgress
                )
            } else {
                // Not connected state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = md_theme_dark_onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Not Connected",
                            style = MaterialTheme.typography.headlineMedium,
                            color = md_theme_dark_onSurface
                        )
                        Text(
                            text = "Connect to a device to start",
                            style = MaterialTheme.typography.bodyMedium,
                            color = md_theme_dark_onSurfaceVariant
                        )
                        
                        if (uiState.lastError != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = md_theme_dark_error.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Text(
                                    text = uiState.lastError ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = md_theme_dark_error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Discovery Bottom Sheet
            if (showDiscoverySheet) {
                DeviceDiscoveryBottomSheet(
                    sheetState = sheetState,
                    devices = uiState.discoveredDevices,
                    isDiscovering = uiState.isDiscovering,
                    discoveryError = uiState.discoveryError,
                    onStartDiscovery = viewModel::startDeviceDiscovery,
                    onConnectToDevice = { device ->
                        viewModel.connectToDevice(device)
                        showDiscoverySheet = false
                    },
                    onDiscoverByIp = viewModel::discoverDeviceByIp,
                    onConnectToDummyDevice = {
                        viewModel.connectToDummyDevice()
                        showDiscoverySheet = false
                    },
                    onDismiss = {
                        if (uiState.isConnected) {
                            showDiscoverySheet = false
                        }
                    }
                )
            }
        }
    }
}

/**
 * Top bar with connection status and device info
 */
@Composable
private fun RemoteTopBar(
    connectionState: ConnectionState,
    deviceName: String,
    latencyMs: Long,
    onDisconnect: () -> Unit,
    onShowDiscovery: () -> Unit,
    onNavigateToTouchpad: () -> Unit,
    onNavigateToGamepad: () -> Unit,
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
            // Device Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Status Indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (connectionState) {
                                is ConnectionState.Connected -> md_theme_dark_success
                                is ConnectionState.Connecting -> md_theme_dark_secondary
                                is ConnectionState.Error -> md_theme_dark_error
                                else -> md_theme_dark_onSurfaceVariant
                            }
                        )
                )
                
                Column {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = md_theme_dark_onSurface
                    )
                    if (connectionState is ConnectionState.Connected && latencyMs > 0) {
                        Text(
                            text = "${latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (latencyMs < 50) md_theme_dark_success 
                                   else if (latencyMs < 150) md_theme_dark_secondary
                                   else md_theme_dark_error
                        )
                    }
                }
            }
            
            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Touchpad/Air Mouse button
                if (connectionState is ConnectionState.Connected) {
                    IconButton(onClick = onNavigateToGamepad) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = "Gamepad",
                            tint = md_theme_dark_primary
                        )
                    }
                    IconButton(onClick = onNavigateToTouchpad) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = "Touchpad / Air Mouse",
                            tint = md_theme_dark_primary
                        )
                    }
                }
                
                IconButton(onClick = onShowDiscovery) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Change Device",
                        tint = md_theme_dark_primary
                    )
                }
                
                if (connectionState is ConnectionState.Connected) {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Disconnect",
                            tint = md_theme_dark_onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Main remote control layout with D-Pad and buttons
 * Optimized for one-thumb reach (bottom 60% of screen)
 */
@Composable
private fun RemoteControlLayout(
    onKeyPress: (UpnpRemoteController.RemoteKey) -> Unit,
    isCommandInProgress: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.4f)) // Top 40% spacing for status/future modes
        
        // Main remote chassis - bottom 60%
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = md_theme_dark_surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // D-Pad Section
                DPadControl(
                    onDirectionPress = onKeyPress,
                    enabled = !isCommandInProgress
                )
                
                // Quick Action Buttons (Back, Home, Menu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteButton(
                        icon = Icons.Default.ArrowBack,
                        label = "Back",
                        onClick = { onKeyPress(UpnpRemoteController.RemoteKey.BACK) },
                        enabled = !isCommandInProgress
                    )
                    RemoteButton(
                        icon = Icons.Default.Home,
                        label = "Home",
                        onClick = { onKeyPress(UpnpRemoteController.RemoteKey.HOME) },
                        enabled = !isCommandInProgress
                    )
                    RemoteButton(
                        icon = Icons.Default.Menu,
                        label = "Menu",
                        onClick = { onKeyPress(UpnpRemoteController.RemoteKey.MENU) },
                        enabled = !isCommandInProgress
                    )
                }
                
                // Volume and Channel Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Volume Controls
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "VOL",
                            style = MaterialTheme.typography.labelSmall,
                            color = md_theme_dark_onSurfaceVariant
                        )
                        RemoteButton(
                            icon = Icons.Default.Add,
                            label = "",
                            onClick = { onKeyPress(UpnpRemoteController.RemoteKey.VOLUME_UP) },
                            enabled = !isCommandInProgress,
                            size = 48.dp
                        )
                        RemoteButton(
                            icon = Icons.Default.Remove,
                            label = "",
                            onClick = { onKeyPress(UpnpRemoteController.RemoteKey.VOLUME_DOWN) },
                            enabled = !isCommandInProgress,
                            size = 48.dp
                        )
                    }
                    
                    // Mute Button
                    RemoteButton(
                        icon = Icons.Default.VolumeOff,
                        label = "Mute",
                        onClick = { onKeyPress(UpnpRemoteController.RemoteKey.MUTE) },
                        enabled = !isCommandInProgress
                    )
                    
                    // Channel Controls
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CH",
                            style = MaterialTheme.typography.labelSmall,
                            color = md_theme_dark_onSurfaceVariant
                        )
                        RemoteButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            label = "",
                            onClick = { onKeyPress(UpnpRemoteController.RemoteKey.CHANNEL_UP) },
                            enabled = !isCommandInProgress,
                            size = 48.dp
                        )
                        RemoteButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            label = "",
                            onClick = { onKeyPress(UpnpRemoteController.RemoteKey.CHANNEL_DOWN) },
                            enabled = !isCommandInProgress,
                            size = 48.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * D-Pad control with center OK button
 * Circular layout with tactile buttons
 */
@Composable
private fun DPadControl(
    onDirectionPress: (UpnpRemoteController.RemoteKey) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(md_theme_dark_surfaceVariant)
        )
        
        // Directional buttons
        Box(modifier = Modifier.fillMaxSize()) {
            // Up
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 16.dp)
            ) {
                DirectionalButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    onClick = { onDirectionPress(UpnpRemoteController.RemoteKey.UP) },
                    enabled = enabled
                )
            }
            
            // Down
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-16).dp)
            ) {
                DirectionalButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    onClick = { onDirectionPress(UpnpRemoteController.RemoteKey.DOWN) },
                    enabled = enabled
                )
            }
            
            // Left
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 16.dp)
            ) {
                DirectionalButton(
                    icon = Icons.Default.KeyboardArrowLeft,
                    onClick = { onDirectionPress(UpnpRemoteController.RemoteKey.LEFT) },
                    enabled = enabled
                )
            }
            
            // Right
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-16).dp)
            ) {
                DirectionalButton(
                    icon = Icons.Default.KeyboardArrowRight,
                    onClick = { onDirectionPress(UpnpRemoteController.RemoteKey.RIGHT) },
                    enabled = enabled
                )
            }
        }
        
        // Center OK button with gradient glow
        OkButton(
            onClick = { onDirectionPress(UpnpRemoteController.RemoteKey.OK) },
            enabled = enabled
        )
    }
}

/**
 * Directional button for D-Pad
 */
@Composable
private fun DirectionalButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )
    
    Surface(
        modifier = modifier
            .size(56.dp)
            .scale(scale)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() }
                    )
                }
            },
        shape = RoundedCornerShape(20.dp),
        color = if (isPressed) md_theme_dark_surfaceContainerHigh else md_theme_dark_surface,
        tonalElevation = if (isPressed) 0.dp else 2.dp
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) md_theme_dark_onSurface else md_theme_dark_onSurfaceVariant,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        )
    }
}

/**
 * Center OK button with primary gradient
 */
@Composable
private fun OkButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ok_button_scale"
    )
    
    Surface(
        modifier = modifier
            .size(84.dp)
            .scale(scale)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() }
                    )
                }
            },
        shape = CircleShape,
        color = md_theme_dark_primary,
        tonalElevation = if (isPressed) 0.dp else 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGradientStart,
                            primaryGradientEnd
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                style = MaterialTheme.typography.titleMedium,
                color = md_theme_dark_onPrimary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

/**
 * Standard remote button with icon and label
 */
@Composable
private fun RemoteButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                },
            shape = RoundedCornerShape(20.dp),
            color = if (isPressed) md_theme_dark_surfaceContainerHigh else md_theme_dark_surfaceVariant,
            tonalElevation = if (isPressed) 0.dp else 2.dp
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) md_theme_dark_onSurface else md_theme_dark_onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            )
        }
        
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = md_theme_dark_onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
