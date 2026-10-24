package com.example.airsyncmodern.data.network.discovery

import android.util.Log
import com.example.airsyncmodern.data.network.model.DiscoveryResult
import com.example.airsyncmodern.data.network.model.StbDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Repository for discovering STB devices on the local network using SSDP (Simple Service Discovery Protocol).
 * 
 * This implementation uses MulticastSocket to send M-SEARCH requests to the SSDP multicast address
 * and listens for responses from compatible HiSilicon/Formuler Set-Top Boxes.
 * 
 * Discovery flow:
 * 1. Creates a MulticastSocket and joins the SSDP multicast group (239.255.255.250:1900)
 * 2. Sends M-SEARCH discovery requests for UPnP devices
 * 3. Listens for SSDP responses containing device location URLs
 * 4. Fetches device description XML from each discovered device
 * 5. Parses device information and filters for compatible STB devices
 * 6. Emits discovered devices as a Flow
 */
@Singleton
class SsdpDiscoveryRepository @Inject constructor(
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "SsdpDiscovery"
        
        // SSDP Constants
        private const val SSDP_MULTICAST_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SSDP_TIMEOUT_MS = 5000
        private const val RESPONSE_BUFFER_SIZE = 8192
        
        // UPnP Search Targets
        private const val SEARCH_TARGET_ALL = "ssdp:all"
        private const val SEARCH_TARGET_ROOT_DEVICE = "upnp:rootdevice"
        private const val SEARCH_TARGET_MEDIA_RENDERER = "urn:schemas-upnp-org:device:MediaRenderer:1"
        
        // HiSilicon/Formuler specific identifiers
        private val COMPATIBLE_MANUFACTURERS = listOf("HiSilicon", "Formuler", "Aloys")
        private val COMPATIBLE_DEVICE_TYPES = listOf(
            "urn:schemas-upnp-org:device:MediaRenderer",
            "urn:schemas-upnp-org:device:Basic",
            "HiSilicon"
        )
        
        // M-SEARCH request template
        private const val MSEARCH_REQUEST = 
            "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: $SSDP_MULTICAST_ADDRESS:$SSDP_PORT\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 3\r\n" +
            "ST: %s\r\n" +
            "\r\n"
    }
    
    /**
     * Discovers STB devices on the local network.
     * 
     * @param searchDurationMs Maximum duration to search for devices (default 5000ms)
     * @return Flow emitting DiscoveryResult updates during the search process
     */
    fun discoverDevices(searchDurationMs: Long = SSDP_TIMEOUT_MS.toLong()): Flow<DiscoveryResult> = flow {
        emit(DiscoveryResult.Searching)
        
        val discoveredDevices = mutableSetOf<StbDevice>()
        val processedUrls = mutableSetOf<String>()
        
        try {
            withTimeoutOrNull(searchDurationMs) {
                withContext(Dispatchers.IO) {
                    var socket: MulticastSocket? = null
                    
                    try {
                        // Create and configure multicast socket
                        socket = MulticastSocket(SSDP_PORT)
                        socket.soTimeout = SSDP_TIMEOUT_MS
                        socket.reuseAddress = true
                        
                        val group = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
                        socket.joinGroup(group)
                        
                        Log.d(TAG, "Multicast socket created, joined group $SSDP_MULTICAST_ADDRESS")
                        
                        // Send M-SEARCH requests for different device types
                        sendMSearchRequest(socket, SEARCH_TARGET_ALL)
                        sendMSearchRequest(socket, SEARCH_TARGET_ROOT_DEVICE)
                        sendMSearchRequest(socket, SEARCH_TARGET_MEDIA_RENDERER)
                        
                        // Listen for responses
                        val buffer = ByteArray(RESPONSE_BUFFER_SIZE)
                        val packet = DatagramPacket(buffer, buffer.size)
                        
                        while (true) {
                            try {
                                socket.receive(packet)
                                
                                val response = String(packet.data, 0, packet.length)
                                Log.d(TAG, "Received SSDP response from ${packet.address.hostAddress}")
                                
                                // Extract location URL from response
                                val locationUrl = extractLocationUrl(response)
                                
                                if (locationUrl != null && !processedUrls.contains(locationUrl)) {
                                    processedUrls.add(locationUrl)
                                    
                                    // Fetch and parse device description
                                    val device = fetchDeviceDescription(locationUrl, packet.address.hostAddress)
                                    
                                    if (device != null && isCompatibleDevice(device)) {
                                        discoveredDevices.add(device)
                                        emit(DiscoveryResult.Success(discoveredDevices.toList()))
                                        Log.d(TAG, "Compatible device found: ${device.name} at ${device.ipAddress}")
                                    }
                                }
                                
                            } catch (e: SocketTimeoutException) {
                                // Timeout waiting for response, continue to next iteration
                                Log.d(TAG, "Socket timeout, finishing discovery")
                                break
                            }
                        }
                        
                        socket.leaveGroup(group)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during SSDP discovery", e)
                        throw e
                    } finally {
                        socket?.close()
                    }
                }
            }
            
            // Emit final results
            if (discoveredDevices.isEmpty()) {
                emit(DiscoveryResult.Error("No compatible STB devices found on the network"))
            } else {
                emit(DiscoveryResult.Success(discoveredDevices.toList()))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Discovery failed", e)
            emit(DiscoveryResult.Error("Discovery failed: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Sends an M-SEARCH multicast request to discover UPnP devices.
     */
    private fun sendMSearchRequest(socket: MulticastSocket, searchTarget: String) {
        try {
            val message = String.format(MSEARCH_REQUEST, searchTarget)
            val requestData = message.toByteArray()
            val packet = DatagramPacket(
                requestData,
                requestData.size,
                InetAddress.getByName(SSDP_MULTICAST_ADDRESS),
                SSDP_PORT
            )
            socket.send(packet)
            Log.d(TAG, "Sent M-SEARCH request for $searchTarget")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send M-SEARCH request for $searchTarget", e)
        }
    }
    
    /**
     * Extracts the LOCATION header value from an SSDP response.
     */
    private fun extractLocationUrl(response: String): String? {
        val lines = response.split("\r\n")
        for (line in lines) {
            if (line.startsWith("LOCATION:", ignoreCase = true)) {
                return line.substring(9).trim()
            }
        }
        return null
    }
    
    /**
     * Fetches and parses the device description XML from the given URL.
     */
    private suspend fun fetchDeviceDescription(locationUrl: String, ipAddress: String): StbDevice? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching device description from $locationUrl")
                
                val request = Request.Builder()
                    .url(locationUrl)
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to fetch device description: ${response.code}")
                    return@withContext null
                }
                
                val xmlContent = response.body?.string() ?: return@withContext null
                
                // Parse device description XML
                parseDeviceDescription(xmlContent, locationUrl, ipAddress)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching device description from $locationUrl", e)
                null
            }
        }
    }
    
    /**
     * Parses the UPnP device description XML and extracts device information.
     */
    private fun parseDeviceDescription(xmlContent: String, locationUrl: String, ipAddress: String): StbDevice? {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(InputSource(StringReader(xmlContent)))
            
            document.documentElement.normalize()
            
            // Extract device information
            val deviceElement = document.getElementsByTagName("device").item(0) ?: return null
            
            val friendlyName = getTextContent(deviceElement, "friendlyName") ?: "Unknown Device"
            val modelName = getTextContent(deviceElement, "modelName") ?: ""
            val manufacturer = getTextContent(deviceElement, "manufacturer") ?: ""
            val deviceType = getTextContent(deviceElement, "deviceType") ?: ""
            val uuid = getTextContent(deviceElement, "UDN")?.removePrefix("uuid:") ?: ""
            
            // Extract port from location URL
            val port = extractPortFromUrl(locationUrl)
            
            // Extract service information (looking for VInput or RemoteControl service)
            var serviceType = ""
            var controlUrl = ""
            
            val serviceList = document.getElementsByTagName("service")
            for (i in 0 until serviceList.length) {
                val service = serviceList.item(i)
                val serviceTypeValue = getTextContent(service, "serviceType") ?: ""
                
                // Look for VInput or RemoteControl service
                if (serviceTypeValue.contains("VInput", ignoreCase = true) ||
                    serviceTypeValue.contains("RemoteControl", ignoreCase = true) ||
                    serviceTypeValue.contains("RenderingControl", ignoreCase = true)) {
                    serviceType = serviceTypeValue
                    controlUrl = getTextContent(service, "controlURL") ?: ""
                    break
                }
            }
            
            // If no specific service found, use the first service
            if (serviceType.isEmpty() && serviceList.length > 0) {
                val firstService = serviceList.item(0)
                serviceType = getTextContent(firstService, "serviceType") ?: ""
                controlUrl = getTextContent(firstService, "controlURL") ?: ""
            }
            
            return StbDevice(
                name = friendlyName,
                ipAddress = ipAddress,
                port = port,
                modelName = modelName,
                manufacturer = manufacturer,
                uuid = uuid,
                deviceType = deviceType,
                serviceType = serviceType,
                controlUrl = controlUrl,
                descriptionUrl = locationUrl
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device description XML", e)
            return null
        }
    }
    
    /**
     * Helper function to extract text content from XML element by tag name.
     */
    private fun getTextContent(parent: org.w3c.dom.Node, tagName: String): String? {
        if (parent is org.w3c.dom.Element) {
            val nodeList = parent.getElementsByTagName(tagName)
            if (nodeList.length > 0) {
                return nodeList.item(0)?.textContent?.trim()
            }
        }
        return null
    }
    
    /**
     * Extracts the port number from a URL string.
     */
    private fun extractPortFromUrl(url: String): Int {
        return try {
            val regex = Regex(":(\\d+)/")
            val match = regex.find(url)
            match?.groupValues?.get(1)?.toInt() ?: 49152
        } catch (e: Exception) {
            49152 // Default UPnP port
        }
    }
    
    /**
     * Checks if the discovered device is a compatible STB device.
     */
    private fun isCompatibleDevice(device: StbDevice): Boolean {
        // Check manufacturer
        val manufacturerMatch = COMPATIBLE_MANUFACTURERS.any { 
            device.manufacturer.contains(it, ignoreCase = true) ||
            device.modelName.contains(it, ignoreCase = true)
        }
        
        // Check device type
        val deviceTypeMatch = COMPATIBLE_DEVICE_TYPES.any { 
            device.deviceType.contains(it, ignoreCase = true)
        }
        
        // A device is compatible if it matches manufacturer OR has a valid service type
        return manufacturerMatch || (deviceTypeMatch && device.serviceType.isNotEmpty())
    }
    
    /**
     * Discovers a device by directly connecting to a known IP address.
     * Useful when the user manually enters an IP address.
     * 
     * @param ipAddress The IP address to connect to
     * @param port The port number (default 49152)
     * @return The discovered device or null if not found/compatible
     */
    suspend fun discoverDeviceByIp(ipAddress: String, port: Int = 49152): StbDevice? {
        return withContext(Dispatchers.IO) {
            try {
                val locationUrl = "http://$ipAddress:$port/description.xml"
                Log.d(TAG, "Attempting to discover device at $locationUrl")
                
                fetchDeviceDescription(locationUrl, ipAddress)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error discovering device by IP: $ipAddress", e)
                null
            }
        }
    }
}
