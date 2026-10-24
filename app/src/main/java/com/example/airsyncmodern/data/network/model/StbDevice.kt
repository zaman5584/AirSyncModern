package com.example.airsyncmodern.data.network.model

/**
 * Data model representing a discovered Set-Top Box device on the network.
 * 
 * @property name The friendly name of the device (e.g., "Formuler Z8 Pro")
 * @property ipAddress The IP address of the device
 * @property port The port number for UPnP control (typically 49152 or 49153)
 * @property modelName The model name from UPnP discovery
 * @property manufacturer The manufacturer name (e.g., "HiSilicon" or "Formuler")
 * @property uuid Unique device identifier from SSDP discovery
 * @property deviceType UPnP device type URN
 * @property serviceType UPnP service type URN (typically VInput service)
 * @property controlUrl The control URL path for sending SOAP commands
 * @property descriptionUrl The full URL to the device description XML
 */
data class StbDevice(
    val name: String,
    val ipAddress: String,
    val port: Int = 49152,
    val modelName: String = "",
    val manufacturer: String = "",
    val uuid: String = "",
    val deviceType: String = "",
    val serviceType: String = "",
    val controlUrl: String = "",
    val descriptionUrl: String = ""
) {
    /**
     * Returns the full control endpoint URL for SOAP commands
     */
    fun getControlEndpoint(): String {
        return "http://$ipAddress:$port$controlUrl"
    }
    
    /**
     * Returns a display name for the device (name or IP if name is empty)
     */
    fun getDisplayName(): String {
        return name.ifEmpty { ipAddress }
    }
    
    /**
     * Returns true if this is a HiSilicon-based device
     */
    fun isHiSiliconDevice(): Boolean {
        return manufacturer.contains("HiSilicon", ignoreCase = true) ||
               manufacturer.contains("Formuler", ignoreCase = true) ||
               deviceType.contains("HiSilicon", ignoreCase = true)
    }
}

/**
 * Represents the connection state of an STB device
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val device: StbDevice, val latencyMs: Long = 0) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Result wrapper for device discovery operations
 */
sealed class DiscoveryResult {
    data class Success(val devices: List<StbDevice>) : DiscoveryResult()
    data class Error(val message: String, val throwable: Throwable? = null) : DiscoveryResult()
    object Searching : DiscoveryResult()
}
