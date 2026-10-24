package com.example.airsyncmodern.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.airsyncmodern.data.network.model.StbDevice
import com.example.airsyncmodern.presentation.ui.theme.*

/**
 * Bottom sheet component for discovering and selecting STB devices.
 * 
 * Features:
 * - Network device discovery via SSDP
 * - Manual IP address entry
 * - Device list with connection status
 * - Preview Mode for UI testing without real device
 * - Material 3 Dark theme
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoveryBottomSheet(
    sheetState: SheetState,
    devices: List<StbDevice>,
    isDiscovering: Boolean,
    discoveryError: String?,
    onStartDiscovery: () -> Unit,
    onConnectToDevice: (StbDevice) -> Unit,
    onDiscoverByIp: (String, Int) -> Unit,
    onConnectToDummyDevice: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualEntry by remember { mutableStateOf(false) }
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("49152") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = md_theme_dark_surface,
        contentColor = md_theme_dark_onSurface,
        tonalElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discover Devices",
                    style = MaterialTheme.typography.headlineMedium,
                    color = md_theme_dark_onSurface
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = md_theme_dark_onSurfaceVariant
                    )
                }
            }

            // Manual IP Entry Toggle
            if (!showManualEntry) {
                // Discovery Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStartDiscovery,
                        enabled = !isDiscovering,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = md_theme_dark_primary,
                            contentColor = md_theme_dark_onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isDiscovering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = md_theme_dark_onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isDiscovering) "Searching..." else "Search Network")
                    }
                    
                    OutlinedButton(
                        onClick = { showManualEntry = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = md_theme_dark_primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(md_theme_dark_primary)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Enter IP")
                    }
                }
                
                // Preview Mode Button (Dev/Testing)
                Button(
                    onClick = {
                        onConnectToDummyDevice()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = md_theme_dark_secondary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Preview UI (Dev Mode)")
                }
            } else {
                // Manual IP Entry Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text("IP Address") },
                        placeholder = { Text("192.168.1.100") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = md_theme_dark_primary,
                            unfocusedBorderColor = md_theme_dark_outline,
                            focusedLabelColor = md_theme_dark_primary,
                            unfocusedLabelColor = md_theme_dark_onSurfaceVariant,
                            cursorColor = md_theme_dark_primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        placeholder = { Text("49152") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (ipAddress.isNotBlank()) {
                                    onDiscoverByIp(ipAddress, port.toIntOrNull() ?: 49152)
                                    showManualEntry = false
                                    ipAddress = ""
                                    port = "49152"
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = md_theme_dark_primary,
                            unfocusedBorderColor = md_theme_dark_outline,
                            focusedLabelColor = md_theme_dark_primary,
                            unfocusedLabelColor = md_theme_dark_onSurfaceVariant,
                            cursorColor = md_theme_dark_primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showManualEntry = false
                                ipAddress = ""
                                port = "49152"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = md_theme_dark_onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                if (ipAddress.isNotBlank()) {
                                    onDiscoverByIp(ipAddress, port.toIntOrNull() ?: 49152)
                                    showManualEntry = false
                                    ipAddress = ""
                                    port = "49152"
                                }
                            },
                            enabled = ipAddress.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = md_theme_dark_primary,
                                contentColor = md_theme_dark_onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Connect")
                        }
                    }
                }
            }

            // Error Message
            if (discoveryError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = md_theme_dark_error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = discoveryError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = md_theme_dark_error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Device List
            if (devices.isEmpty() && !isDiscovering) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = md_theme_dark_onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No devices found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = md_theme_dark_onSurfaceVariant
                        )
                        Text(
                            text = "Tap 'Search Network' to discover devices",
                            style = MaterialTheme.typography.labelSmall,
                            color = md_theme_dark_onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Available Devices (${devices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = md_theme_dark_onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices, key = { it.uuid.ifEmpty { it.ipAddress } }) { device ->
                        DeviceListItem(
                            device = device,
                            onConnect = { onConnectToDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual device list item
 */
@Composable
private fun DeviceListItem(
    device: StbDevice,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onConnect),
        colors = CardDefaults.cardColors(
            containerColor = md_theme_dark_surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = device.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    color = md_theme_dark_onSurface
                )
                
                Text(
                    text = device.ipAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = md_theme_dark_onSurfaceVariant
                )
                
                if (device.modelName.isNotEmpty()) {
                    Text(
                        text = device.modelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = md_theme_dark_onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Connect",
                tint = if (device.isHiSiliconDevice()) md_theme_dark_primary else md_theme_dark_onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
