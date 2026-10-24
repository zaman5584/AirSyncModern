package com.example.airsyncmodern.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airsyncmodern.data.network.discovery.SsdpDiscoveryRepository
import com.example.airsyncmodern.data.network.model.ConnectionState
import com.example.airsyncmodern.data.network.model.DiscoveryResult
import com.example.airsyncmodern.data.network.model.StbDevice
import com.example.airsyncmodern.data.network.remote.UpnpRemoteController
import com.example.airsyncmodern.data.sensor.AirMouseSensorEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing remote control operations and device discovery.
 * 
 * Responsibilities:
 * - Device discovery and connection management
 * - Remote control command execution
 * - Mouse/Touchpad and Air Mouse sensor management
 * - UI state management
 * - Network latency monitoring
 */
@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val discoveryRepository: SsdpDiscoveryRepository,
    private val remoteController: UpnpRemoteController,
    private val airMouseSensor: AirMouseSensorEngine
) : ViewModel() {

    companion object {
        private const val TAG = "RemoteViewModel"
        private const val LATENCY_CHECK_INTERVAL_MS = 5000L
        private const val MOUSE_MOVE_THROTTLE_MS = 50L // 20Hz mouse updates
    }

    // UI State
    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private var latencyCheckJob: Job? = null
    private var airMouseJob: Job? = null
    private var lastMouseMoveTime = 0L

    /**
     * Starts device discovery on the local network
     */
    fun startDeviceDiscovery() {
        viewModelScope.launch {
            Log.d(TAG, "Starting device discovery")
            _uiState.update { it.copy(isDiscovering = true, discoveryError = null) }

            discoveryRepository.discoverDevices().collect { result ->
                when (result) {
                    is DiscoveryResult.Searching -> {
                        _uiState.update { it.copy(isDiscovering = true) }
                    }
                    is DiscoveryResult.Success -> {
                        Log.d(TAG, "Discovery success: ${result.devices.size} devices found")
                        _uiState.update {
                            it.copy(
                                discoveredDevices = result.devices,
                                isDiscovering = false,
                                discoveryError = null
                            )
                        }
                    }
                    is DiscoveryResult.Error -> {
                        Log.e(TAG, "Discovery error: ${result.message}")
                        _uiState.update {
                            it.copy(
                                isDiscovering = false,
                                discoveryError = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Discovers a device by manually entered IP address
     */
    fun discoverDeviceByIp(ipAddress: String, port: Int = 49152) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true, discoveryError = null) }

            try {
                val device = discoveryRepository.discoverDeviceByIp(ipAddress, port)
                if (device != null) {
                    Log.d(TAG, "Device discovered at $ipAddress: ${device.name}")
                    _uiState.update {
                        it.copy(
                            discoveredDevices = it.discoveredDevices + device,
                            isDiscovering = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isDiscovering = false,
                            discoveryError = "No device found at $ipAddress"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error discovering device by IP", e)
                _uiState.update {
                    it.copy(
                        isDiscovering = false,
                        discoveryError = "Failed to connect: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Connects to a selected STB device
     */
    fun connectToDevice(device: StbDevice) {
        viewModelScope.launch {
            Log.d(TAG, "Connecting to device: ${device.name}")
            _uiState.update { it.copy(connectionState = ConnectionState.Connecting) }

            try {
                // Test connection
                val isReachable = remoteController.testConnection(device)

                if (isReachable) {
                    // Measure initial latency
                    val latency = remoteController.measureLatency(device)

                    _uiState.update {
                        it.copy(
                            connectionState = ConnectionState.Connected(device, latency),
                            connectedDevice = device,
                            lastError = null
                        )
                    }

                    Log.d(TAG, "Connected successfully to ${device.name} (${latency}ms)")

                    // Start periodic latency checks
                    startLatencyMonitoring(device)
                } else {
                    _uiState.update {
                        it.copy(
                            connectionState = ConnectionState.Error("Device is not reachable"),
                            lastError = "Failed to connect to ${device.name}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.Error(e.message ?: "Unknown error"),
                        lastError = "Connection error: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Disconnects from the current device
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from device")
        latencyCheckJob?.cancel()
        _uiState.update {
            it.copy(
                connectionState = ConnectionState.Disconnected,
                connectedDevice = null
            )
        }
    }

    /**
     * Connects to a dummy device for UI preview/development mode.
     * This allows testing the UI without a real STB device.
     */
    fun connectToDummyDevice() {
        Log.d(TAG, "Connecting to dummy device (Preview Mode)")
        
        val dummyDevice = StbDevice(
            name = "Formuler Z8 Pro (Preview)",
            ipAddress = "192.168.1.99",
            port = 49152,
            modelName = "Z8 Pro",
            manufacturer = "Formuler",
            uuid = "dummy-device-preview-mode",
            deviceType = "urn:schemas-upnp-org:device:MediaRenderer:1",
            serviceType = "urn:schemas-upnp-org:service:VInput:1",
            controlUrl = "/upnp/control/VInput1",
            descriptionUrl = "http://192.168.1.99:49152/description.xml"
        )
        
        // Simulate connection with random latency
        val simulatedLatency = (12L..45L).random()
        
        _uiState.update {
            it.copy(
                connectionState = ConnectionState.Connected(dummyDevice, simulatedLatency),
                connectedDevice = dummyDevice,
                lastError = null
            )
        }
        
        Log.d(TAG, "Connected to dummy device (Preview Mode) - Latency: ${simulatedLatency}ms")
    }

    /**
     * Sends a remote control key command
     */
    fun sendKey(key: UpnpRemoteController.RemoteKey) {
        val device = _uiState.value.connectedDevice ?: run {
            Log.w(TAG, "Cannot send key: No device connected")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCommandInProgress = true) }

            val result = remoteController.sendKey(device, key)

            when (result) {
                is UpnpRemoteController.RemoteControlResult.Success -> {
                    Log.d(TAG, "Key sent successfully: ${key.keyName}")
                    _uiState.update { it.copy(isCommandInProgress = false, lastError = null) }
                }
                is UpnpRemoteController.RemoteControlResult.Error -> {
                    Log.e(TAG, "Failed to send key: ${result.message}")
                    _uiState.update {
                        it.copy(
                            isCommandInProgress = false,
                            lastError = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Sends a digit key (0-9)
     */
    fun sendDigit(digit: Int) {
        val device = _uiState.value.connectedDevice ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCommandInProgress = true) }

            val result = remoteController.sendDigit(device, digit)

            when (result) {
                is UpnpRemoteController.RemoteControlResult.Success -> {
                    _uiState.update { it.copy(isCommandInProgress = false, lastError = null) }
                }
                is UpnpRemoteController.RemoteControlResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCommandInProgress = false,
                            lastError = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Sends a channel number
     */
    fun enterChannelNumber(channelNumber: Int) {
        val device = _uiState.value.connectedDevice ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCommandInProgress = true) }

            val result = remoteController.enterChannelNumber(device, channelNumber)

            when (result) {
                is UpnpRemoteController.RemoteControlResult.Success -> {
                    _uiState.update { it.copy(isCommandInProgress = false, lastError = null) }
                }
                is UpnpRemoteController.RemoteControlResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCommandInProgress = false,
                            lastError = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Convenience methods for common actions
     */
    fun navigateUp() = sendKey(UpnpRemoteController.RemoteKey.UP)
    fun navigateDown() = sendKey(UpnpRemoteController.RemoteKey.DOWN)
    fun navigateLeft() = sendKey(UpnpRemoteController.RemoteKey.LEFT)
    fun navigateRight() = sendKey(UpnpRemoteController.RemoteKey.RIGHT)
    fun pressOk() = sendKey(UpnpRemoteController.RemoteKey.OK)
    fun pressBack() = sendKey(UpnpRemoteController.RemoteKey.BACK)
    fun pressHome() = sendKey(UpnpRemoteController.RemoteKey.HOME)
    fun pressMenu() = sendKey(UpnpRemoteController.RemoteKey.MENU)

    fun pressGamepadA() = sendKey(UpnpRemoteController.RemoteKey.GAMEPAD_A)
    fun pressGamepadB() = sendKey(UpnpRemoteController.RemoteKey.GAMEPAD_B)
    fun pressGamepadX() = sendKey(UpnpRemoteController.RemoteKey.GAMEPAD_X)
    fun pressGamepadY() = sendKey(UpnpRemoteController.RemoteKey.GAMEPAD_Y)

    fun volumeUp() = sendKey(UpnpRemoteController.RemoteKey.VOLUME_UP)
    fun volumeDown() = sendKey(UpnpRemoteController.RemoteKey.VOLUME_DOWN)
    fun toggleMute() = sendKey(UpnpRemoteController.RemoteKey.MUTE)

    fun channelUp() = sendKey(UpnpRemoteController.RemoteKey.CHANNEL_UP)
    fun channelDown() = sendKey(UpnpRemoteController.RemoteKey.CHANNEL_DOWN)

    fun play() = sendKey(UpnpRemoteController.RemoteKey.PLAY)
    fun pause() = sendKey(UpnpRemoteController.RemoteKey.PAUSE)
    fun stop() = sendKey(UpnpRemoteController.RemoteKey.STOP)
    fun togglePlayPause() = sendKey(UpnpRemoteController.RemoteKey.PLAY_PAUSE)

    fun power() = sendKey(UpnpRemoteController.RemoteKey.POWER)
    fun info() = sendKey(UpnpRemoteController.RemoteKey.INFO)
    fun epg() = sendKey(UpnpRemoteController.RemoteKey.EPG)

    /**
     * Clears the last error message
     */
    fun clearError() {
        _uiState.update { it.copy(lastError = null, discoveryError = null) }
    }
    
    // ========== Mouse/Touchpad Functions ==========
    
    /**
     * Sends mouse movement from touchpad
     */
    fun sendMouseMove(deltaX: Int, deltaY: Int) {
        val device = _uiState.value.connectedDevice ?: return
        
        // Throttle mouse movements to avoid overwhelming the network
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMouseMoveTime < MOUSE_MOVE_THROTTLE_MS) {
            return
        }
        lastMouseMoveTime = currentTime
        
        viewModelScope.launch {
            remoteController.sendMouseMove(device, deltaX, deltaY)
        }
    }
    
    /**
     * Sends mouse left click
     */
    fun sendMouseLeftClick() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCommandInProgress = true) }
            val result = remoteController.sendMouseLeftClick(device)
            _uiState.update { it.copy(isCommandInProgress = false) }
            
            if (result is UpnpRemoteController.RemoteControlResult.Error) {
                _uiState.update { it.copy(lastError = result.message) }
            }
        }
    }
    
    /**
     * Sends mouse right click
     */
    fun sendMouseRightClick() {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCommandInProgress = true) }
            val result = remoteController.sendMouseRightClick(device)
            _uiState.update { it.copy(isCommandInProgress = false) }
            
            if (result is UpnpRemoteController.RemoteControlResult.Error) {
                _uiState.update { it.copy(lastError = result.message) }
            }
        }
    }
    
    /**
     * Sends mouse scroll
     */
    fun sendMouseScroll(delta: Int) {
        val device = _uiState.value.connectedDevice ?: return
        viewModelScope.launch {
            remoteController.sendMouseScroll(device, delta)
        }
    }
    
    // ========== Air Mouse Sensor Functions ==========
    
    /**
     * Toggles Air Mouse sensor on/off
     */
    fun toggleAirMouse() {
        if (_uiState.value.isAirMouseActive) {
            stopAirMouse()
        } else {
            startAirMouse()
        }
    }
    
    /**
     * Starts Air Mouse sensor and begins sending movement data
     */
    fun startAirMouse() {
        if (!airMouseSensor.isGyroscopeAvailable) {
            _uiState.update { it.copy(lastError = "Gyroscope not available on this device") }
            return
        }
        
        val device = _uiState.value.connectedDevice ?: return
        
        Log.d(TAG, "Starting Air Mouse")
        airMouseSensor.start()
        _uiState.update { it.copy(isAirMouseActive = true) }
        
        // Start job to monitor sensor data and send mouse movements
        airMouseJob = viewModelScope.launch {
            airMouseSensor.cursorDelta.collect { (deltaX, deltaY) ->
                if (deltaX != 0f || deltaY != 0f) {
                    // Convert float deltas to int and send
                    val x = deltaX.toInt()
                    val y = deltaY.toInt()
                    
                    if (x != 0 || y != 0) {
                        remoteController.sendMouseMove(device, x, y)
                    }
                }
            }
        }
    }
    
    /**
     * Stops Air Mouse sensor
     */
    fun stopAirMouse() {
        Log.d(TAG, "Stopping Air Mouse")
        airMouseSensor.stop()
        airMouseJob?.cancel()
        airMouseJob = null
        _uiState.update { it.copy(isAirMouseActive = false) }
    }

    /**
     * Starts periodic latency monitoring
     */
    private fun startLatencyMonitoring(device: StbDevice) {
        latencyCheckJob?.cancel()
        latencyCheckJob = viewModelScope.launch {
            while (true) {
                delay(LATENCY_CHECK_INTERVAL_MS)

                val latency = remoteController.measureLatency(device)

                if (latency > 0) {
                    _uiState.update { currentState ->
                        val connectionState = currentState.connectionState
                        if (connectionState is ConnectionState.Connected) {
                            currentState.copy(
                                connectionState = ConnectionState.Connected(device, latency)
                            )
                        } else {
                            currentState
                        }
                    }
                } else {
                    // Connection lost
                    Log.w(TAG, "Connection lost to device")
                    _uiState.update {
                        it.copy(
                            connectionState = ConnectionState.Error("Connection lost"),
                            lastError = "Lost connection to ${device.name}"
                        )
                    }
                    break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        latencyCheckJob?.cancel()
        airMouseJob?.cancel()
        airMouseSensor.stop()
        Log.d(TAG, "ViewModel cleared")
    }
}

/**
 * UI State for the Remote Control screen
 */
data class RemoteUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val connectedDevice: StbDevice? = null,
    val discoveredDevices: List<StbDevice> = emptyList(),
    val isDiscovering: Boolean = false,
    val discoveryError: String? = null,
    val isCommandInProgress: Boolean = false,
    val lastError: String? = null,
    val isAirMouseActive: Boolean = false
) {
    val isConnected: Boolean
        get() = connectionState is ConnectionState.Connected

    val latencyMs: Long
        get() = (connectionState as? ConnectionState.Connected)?.latencyMs ?: 0L
}
