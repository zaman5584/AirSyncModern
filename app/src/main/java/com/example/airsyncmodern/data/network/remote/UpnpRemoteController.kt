package com.example.airsyncmodern.data.network.remote

import android.util.Log
import com.example.airsyncmodern.data.network.model.StbDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for sending UPnP/SOAP commands to STB devices.
 * 
 * This implementation uses OkHttp to send SOAP/XML requests to the VInput service
 * on HiSilicon/Formuler Set-Top Boxes for remote control functionality.
 * 
 * Supports standard remote control keys including:
 * - D-Pad navigation (Up, Down, Left, Right, OK/Select)
 * - Transport controls (Play, Pause, Stop, FastForward, Rewind)
 * - Menu navigation (Menu, Back, Home, Exit)
 * - Volume and Channel controls
 * - Number keys (0-9)
 * - Special function keys (Info, EPG, Power, etc.)
 */
@Singleton
class UpnpRemoteController @Inject constructor(
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "UpnpRemoteController"
        
        // SOAP/UPnP Constants
        private const val SOAP_ACTION_SEND_KEY = "urn:schemas-upnp-org:service:VInput:1#SendKey"
        private const val CONTENT_TYPE_SOAP = "text/xml; charset=utf-8"
        private const val REQUEST_TIMEOUT_MS = 3000L
        
        // SOAP Envelope Template
        private const val SOAP_ENVELOPE_TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
        <u:SendKey xmlns:u="urn:schemas-upnp-org:service:VInput:1">
            <KeyCode>%s</KeyCode>
            <KeyName>%s</KeyName>
        </u:SendKey>
    </s:Body>
</s:Envelope>"""
    }
    
    /**
     * Enum representing all supported remote control keys.
     * Each key has a numeric code and a descriptive name.
     */
    enum class RemoteKey(val code: Int, val keyName: String) {
        // D-Pad Navigation
        UP(103, "KEY_UP"),
        DOWN(108, "KEY_DOWN"),
        LEFT(105, "KEY_LEFT"),
        RIGHT(106, "KEY_RIGHT"),
        OK(28, "KEY_ENTER"),
        SELECT(28, "KEY_SELECT"),
        
        // Transport Controls
        PLAY(207, "KEY_PLAY"),
        PAUSE(119, "KEY_PAUSE"),
        STOP(128, "KEY_STOP"),
        PLAY_PAUSE(164, "KEY_PLAYPAUSE"),
        FAST_FORWARD(208, "KEY_FASTFORWARD"),
        REWIND(168, "KEY_REWIND"),
        RECORD(167, "KEY_RECORD"),
        
        // Menu Navigation
        MENU(139, "KEY_MENU"),
        BACK(158, "KEY_BACK"),
        HOME(102, "KEY_HOME"),
        EXIT(174, "KEY_EXIT"),
        INFO(358, "KEY_INFO"),
        EPG(365, "KEY_EPG"),
        
        // Volume Controls
        VOLUME_UP(115, "KEY_VOLUMEUP"),
        VOLUME_DOWN(114, "KEY_VOLUMEDOWN"),
        MUTE(113, "KEY_MUTE"),
        
        // Channel Controls
        CHANNEL_UP(402, "KEY_CHANNELUP"),
        CHANNEL_DOWN(403, "KEY_CHANNELDOWN"),
        
        // Number Keys
        KEY_0(11, "KEY_0"),
        KEY_1(2, "KEY_1"),
        KEY_2(3, "KEY_2"),
        KEY_3(4, "KEY_3"),
        KEY_4(5, "KEY_4"),
        KEY_5(6, "KEY_5"),
        KEY_6(7, "KEY_6"),
        KEY_7(8, "KEY_7"),
        KEY_8(9, "KEY_8"),
        KEY_9(10, "KEY_9"),
        
        // Special Keys
        POWER(116, "KEY_POWER"),
        RED(398, "KEY_RED"),
        GREEN(399, "KEY_GREEN"),
        YELLOW(400, "KEY_YELLOW"),
        BLUE(401, "KEY_BLUE"),
        
        // Media Navigation
        NEXT(407, "KEY_NEXT"),
        PREVIOUS(412, "KEY_PREVIOUS"),
        
        // Additional Keys
        SUBTITLE(370, "KEY_SUBTITLE"),
        AUDIO(392, "KEY_AUDIO"),
        TEXT(388, "KEY_TEXT"),
        FAVORITES(364, "KEY_FAVORITES"),
        LIST(395, "KEY_LIST"),
        SEARCH(217, "KEY_SEARCH"),
        
        // Gamepad Keys
        GAMEPAD_A(304, "BTN_A"),
        GAMEPAD_B(305, "BTN_B"),
        GAMEPAD_X(307, "BTN_X"),
        GAMEPAD_Y(308, "BTN_Y"),
        GAMEPAD_L1(310, "BTN_TL"),
        GAMEPAD_L2(312, "BTN_TL2"),
        GAMEPAD_R1(311, "BTN_TR"),
        GAMEPAD_R2(313, "BTN_TR2"),
        GAMEPAD_SELECT(314, "BTN_SELECT"),
        GAMEPAD_START(315, "BTN_START"),
        GAMEPAD_THUMBL(317, "BTN_THUMBL"),
        GAMEPAD_THUMBR(318, "BTN_THUMBR");
        
        companion object {
            /**
             * Finds a RemoteKey by its numeric code
             */
            fun fromCode(code: Int): RemoteKey? {
                return values().find { it.code == code }
            }
            
            /**
             * Finds a RemoteKey by its name
             */
            fun fromName(name: String): RemoteKey? {
                return values().find { it.keyName.equals(name, ignoreCase = true) }
            }
        }
    }
    
    /**
     * Result wrapper for remote control operations
     */
    sealed class RemoteControlResult {
        object Success : RemoteControlResult()
        data class Error(val message: String, val throwable: Throwable? = null) : RemoteControlResult()
    }
    
    /**
     * Sends a remote control key command to the STB device.
     * 
     * @param device The target STB device
     * @param key The remote control key to send
     * @return RemoteControlResult indicating success or failure
     */
    suspend fun sendKey(device: StbDevice, key: RemoteKey): RemoteControlResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Sending key ${key.keyName} (${key.code}) to ${device.name}")
                
                // Build SOAP request body
                val soapBody = String.format(
                    SOAP_ENVELOPE_TEMPLATE,
                    key.code,
                    key.keyName
                )
                
                // Get control endpoint URL
                val controlUrl = device.getControlEndpoint()
                
                if (controlUrl.isEmpty() || device.controlUrl.isEmpty()) {
                    Log.e(TAG, "Control URL is empty for device ${device.name}")
                    return@withContext RemoteControlResult.Error("Device control URL is not available")
                }
                
                // Build HTTP request
                val request = Request.Builder()
                    .url(controlUrl)
                    .addHeader("SOAPACTION", "\"$SOAP_ACTION_SEND_KEY\"")
                    .addHeader("Content-Type", CONTENT_TYPE_SOAP)
                    .post(soapBody.toRequestBody(CONTENT_TYPE_SOAP.toMediaType()))
                    .build()
                
                // Execute request
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Key command sent successfully: ${key.keyName}")
                    RemoteControlResult.Success
                } else {
                    val errorMsg = "Failed to send key command: HTTP ${response.code}"
                    Log.e(TAG, errorMsg)
                    RemoteControlResult.Error(errorMsg)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending key command", e)
                RemoteControlResult.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Sends a sequence of key commands with optional delays between them.
     * Useful for macros or complex navigation sequences.
     * 
     * @param device The target STB device
     * @param keys List of remote control keys to send in sequence
     * @param delayMs Delay in milliseconds between each key press (default 100ms)
     * @return RemoteControlResult indicating success or failure of the entire sequence
     */
    suspend fun sendKeySequence(
        device: StbDevice,
        keys: List<RemoteKey>,
        delayMs: Long = 100L
    ): RemoteControlResult {
        return withContext(Dispatchers.IO) {
            try {
                for ((index, key) in keys.withIndex()) {
                    val result = sendKey(device, key)
                    
                    if (result is RemoteControlResult.Error) {
                        return@withContext RemoteControlResult.Error(
                            "Sequence failed at key ${index + 1}/${keys.size}: ${result.message}",
                            result.throwable
                        )
                    }
                    
                    // Add delay between keys (except after the last key)
                    if (index < keys.size - 1) {
                        kotlinx.coroutines.delay(delayMs)
                    }
                }
                
                Log.d(TAG, "Key sequence completed successfully (${keys.size} keys)")
                RemoteControlResult.Success
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending key sequence", e)
                RemoteControlResult.Error("Sequence error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Sends a custom SOAP action to the device.
     * Advanced method for sending non-standard UPnP commands.
     * 
     * @param device The target STB device
     * @param action The SOAP action URN
     * @param soapBody The complete SOAP XML body
     * @return RemoteControlResult indicating success or failure
     */
    suspend fun sendCustomAction(
        device: StbDevice,
        action: String,
        soapBody: String
    ): RemoteControlResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Sending custom action: $action to ${device.name}")
                
                val controlUrl = device.getControlEndpoint()
                
                if (controlUrl.isEmpty()) {
                    return@withContext RemoteControlResult.Error("Device control URL is not available")
                }
                
                val request = Request.Builder()
                    .url(controlUrl)
                    .addHeader("SOAPACTION", "\"$action\"")
                    .addHeader("Content-Type", CONTENT_TYPE_SOAP)
                    .post(soapBody.toRequestBody(CONTENT_TYPE_SOAP.toMediaType()))
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Custom action sent successfully")
                    RemoteControlResult.Success
                } else {
                    val errorMsg = "Failed to send custom action: HTTP ${response.code}"
                    Log.e(TAG, errorMsg)
                    RemoteControlResult.Error(errorMsg)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending custom action", e)
                RemoteControlResult.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Tests connectivity to the device by sending a simple request.
     * 
     * @param device The device to test
     * @return True if the device is reachable and responding, false otherwise
     */
    suspend fun testConnection(device: StbDevice): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val descriptionUrl = device.descriptionUrl.ifEmpty { 
                    "http://${device.ipAddress}:${device.port}/description.xml"
                }
                
                val request = Request.Builder()
                    .url(descriptionUrl)
                    .head()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val isReachable = response.isSuccessful
                
                Log.d(TAG, "Connection test for ${device.name}: ${if (isReachable) "SUCCESS" else "FAILED"}")
                isReachable
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed for ${device.name}", e)
                false
            }
        }
    }
    
    /**
     * Measures the network latency to the device.
     * 
     * @param device The device to measure latency to
     * @return Latency in milliseconds, or -1 if the measurement failed
     */
    suspend fun measureLatency(device: StbDevice): Long {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                
                val descriptionUrl = device.descriptionUrl.ifEmpty { 
                    "http://${device.ipAddress}:${device.port}/description.xml"
                }
                
                val request = Request.Builder()
                    .url(descriptionUrl)
                    .head()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val latency = System.currentTimeMillis() - startTime
                    Log.d(TAG, "Latency to ${device.name}: ${latency}ms")
                    latency
                } else {
                    -1L
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Latency measurement failed for ${device.name}", e)
                -1L
            }
        }
    }
    
    /**
     * Convenience methods for common actions
     */
    
    suspend fun navigateUp(device: StbDevice) = sendKey(device, RemoteKey.UP)
    suspend fun navigateDown(device: StbDevice) = sendKey(device, RemoteKey.DOWN)
    suspend fun navigateLeft(device: StbDevice) = sendKey(device, RemoteKey.LEFT)
    suspend fun navigateRight(device: StbDevice) = sendKey(device, RemoteKey.RIGHT)
    suspend fun pressOk(device: StbDevice) = sendKey(device, RemoteKey.OK)
    suspend fun pressBack(device: StbDevice) = sendKey(device, RemoteKey.BACK)
    suspend fun pressHome(device: StbDevice) = sendKey(device, RemoteKey.HOME)
    suspend fun pressMenu(device: StbDevice) = sendKey(device, RemoteKey.MENU)
    
    suspend fun volumeUp(device: StbDevice) = sendKey(device, RemoteKey.VOLUME_UP)
    suspend fun volumeDown(device: StbDevice) = sendKey(device, RemoteKey.VOLUME_DOWN)
    suspend fun toggleMute(device: StbDevice) = sendKey(device, RemoteKey.MUTE)
    
    suspend fun channelUp(device: StbDevice) = sendKey(device, RemoteKey.CHANNEL_UP)
    suspend fun channelDown(device: StbDevice) = sendKey(device, RemoteKey.CHANNEL_DOWN)
    
    suspend fun play(device: StbDevice) = sendKey(device, RemoteKey.PLAY)
    suspend fun pause(device: StbDevice) = sendKey(device, RemoteKey.PAUSE)
    suspend fun stop(device: StbDevice) = sendKey(device, RemoteKey.STOP)
    suspend fun togglePlayPause(device: StbDevice) = sendKey(device, RemoteKey.PLAY_PAUSE)
    
    suspend fun power(device: StbDevice) = sendKey(device, RemoteKey.POWER)
    
    /**
     * Sends a numeric key press (0-9)
     * 
     * @param device The target device
     * @param digit The digit to send (0-9)
     * @return RemoteControlResult
     */
    suspend fun sendDigit(device: StbDevice, digit: Int): RemoteControlResult {
        if (digit !in 0..9) {
            return RemoteControlResult.Error("Invalid digit: $digit (must be 0-9)")
        }
        
        val key = when (digit) {
            0 -> RemoteKey.KEY_0
            1 -> RemoteKey.KEY_1
            2 -> RemoteKey.KEY_2
            3 -> RemoteKey.KEY_3
            4 -> RemoteKey.KEY_4
            5 -> RemoteKey.KEY_5
            6 -> RemoteKey.KEY_6
            7 -> RemoteKey.KEY_7
            8 -> RemoteKey.KEY_8
            9 -> RemoteKey.KEY_9
            else -> return RemoteControlResult.Error("Invalid digit")
        }
        
        return sendKey(device, key)
    }
    
    /**
     * Sends a channel number by pressing digit keys in sequence
     * 
     * @param device The target device
     * @param channelNumber The channel number to enter
     * @param delayMs Delay between digit presses (default 150ms)
     * @return RemoteControlResult
     */
    suspend fun enterChannelNumber(
        device: StbDevice,
        channelNumber: Int,
        delayMs: Long = 150L
    ): RemoteControlResult {
        if (channelNumber < 0) {
            return RemoteControlResult.Error("Invalid channel number: $channelNumber")
        }
        
        val digits = channelNumber.toString().map { it.digitToInt() }
        val keys = digits.mapNotNull { digit ->
            RemoteKey.values().find { it.keyName == "KEY_$digit" }
        }
        
        return sendKeySequence(device, keys, delayMs)
    }
    
    // ========== Mouse/Touchpad Commands ==========
    
    /**
     * Sends a mouse movement command to the device.
     * 
     * @param device The target device
     * @param deltaX Horizontal movement delta (-127 to 127)
     * @param deltaY Vertical movement delta (-127 to 127)
     * @return RemoteControlResult
     */
    suspend fun sendMouseMove(
        device: StbDevice,
        deltaX: Int,
        deltaY: Int
    ): RemoteControlResult {
        return withContext(Dispatchers.IO) {
            try {
                // Clamp values to valid range
                val clampedX = deltaX.coerceIn(-127, 127)
                val clampedY = deltaY.coerceIn(-127, 127)
                
                // Build SOAP request for mouse movement
                val soapBody = """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
        <u:SendMouseEvent xmlns:u="urn:schemas-upnp-org:service:VInput:1">
            <EventType>Move</EventType>
            <DeltaX>$clampedX</DeltaX>
            <DeltaY>$clampedY</DeltaY>
        </u:SendMouseEvent>
    </s:Body>
</s:Envelope>"""
                
                val controlUrl = device.getControlEndpoint()
                if (controlUrl.isEmpty()) {
                    return@withContext RemoteControlResult.Error("Device control URL is not available")
                }
                
                val request = Request.Builder()
                    .url(controlUrl)
                    .addHeader("SOAPACTION", "\"urn:schemas-upnp-org:service:VInput:1#SendMouseEvent\"")
                    .addHeader("Content-Type", CONTENT_TYPE_SOAP)
                    .post(soapBody.toRequestBody(CONTENT_TYPE_SOAP.toMediaType()))
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    RemoteControlResult.Success
                } else {
                    RemoteControlResult.Error("Failed to send mouse move: HTTP ${response.code}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending mouse move", e)
                RemoteControlResult.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Sends a mouse left click command.
     */
    suspend fun sendMouseLeftClick(device: StbDevice): RemoteControlResult {
        return sendMouseClick(device, "LeftClick")
    }
    
    /**
     * Sends a mouse right click command.
     */
    suspend fun sendMouseRightClick(device: StbDevice): RemoteControlResult {
        return sendMouseClick(device, "RightClick")
    }
    
    /**
     * Sends a mouse middle click command.
     */
    suspend fun sendMouseMiddleClick(device: StbDevice): RemoteControlResult {
        return sendMouseClick(device, "MiddleClick")
    }
    
    /**
     * Sends a mouse double click command.
     */
    suspend fun sendMouseDoubleClick(device: StbDevice): RemoteControlResult {
        return sendMouseClick(device, "DoubleClick")
    }
    
    /**
     * Generic mouse click sender.
     */
    private suspend fun sendMouseClick(
        device: StbDevice,
        clickType: String
    ): RemoteControlResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Sending mouse click: $clickType to ${device.name}")
                
                val soapBody = """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
        <u:SendMouseEvent xmlns:u="urn:schemas-upnp-org:service:VInput:1">
            <EventType>$clickType</EventType>
        </u:SendMouseEvent>
    </s:Body>
</s:Envelope>"""
                
                val controlUrl = device.getControlEndpoint()
                if (controlUrl.isEmpty()) {
                    return@withContext RemoteControlResult.Error("Device control URL is not available")
                }
                
                val request = Request.Builder()
                    .url(controlUrl)
                    .addHeader("SOAPACTION", "\"urn:schemas-upnp-org:service:VInput:1#SendMouseEvent\"")
                    .addHeader("Content-Type", CONTENT_TYPE_SOAP)
                    .post(soapBody.toRequestBody(CONTENT_TYPE_SOAP.toMediaType()))
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Mouse click sent successfully: $clickType")
                    RemoteControlResult.Success
                } else {
                    val errorMsg = "Failed to send mouse click: HTTP ${response.code}"
                    Log.e(TAG, errorMsg)
                    RemoteControlResult.Error(errorMsg)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending mouse click", e)
                RemoteControlResult.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Sends a mouse scroll command.
     * 
     * @param device The target device
     * @param deltaScroll Scroll amount (positive = scroll down, negative = scroll up)
     * @return RemoteControlResult
     */
    suspend fun sendMouseScroll(
        device: StbDevice,
        deltaScroll: Int
    ): RemoteControlResult {
        return withContext(Dispatchers.IO) {
            try {
                val clampedScroll = deltaScroll.coerceIn(-10, 10)
                
                val soapBody = """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
        <u:SendMouseEvent xmlns:u="urn:schemas-upnp-org:service:VInput:1">
            <EventType>Scroll</EventType>
            <DeltaScroll>$clampedScroll</DeltaScroll>
        </u:SendMouseEvent>
    </s:Body>
</s:Envelope>"""
                
                val controlUrl = device.getControlEndpoint()
                if (controlUrl.isEmpty()) {
                    return@withContext RemoteControlResult.Error("Device control URL is not available")
                }
                
                val request = Request.Builder()
                    .url(controlUrl)
                    .addHeader("SOAPACTION", "\"urn:schemas-upnp-org:service:VInput:1#SendMouseEvent\"")
                    .addHeader("Content-Type", CONTENT_TYPE_SOAP)
                    .post(soapBody.toRequestBody(CONTENT_TYPE_SOAP.toMediaType()))
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    RemoteControlResult.Success
                } else {
                    RemoteControlResult.Error("Failed to send mouse scroll: HTTP ${response.code}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending mouse scroll", e)
                RemoteControlResult.Error("Network error: ${e.message}", e)
            }
        }
    }
    
    // ========== Gamepad Commands ==========
    
    /**
     * Sends a gamepad button press command.
     * 
     * @param device The target device
     * @param button The gamepad button to press
     * @return RemoteControlResult
     */
    suspend fun sendGamepadButton(
        device: StbDevice,
        button: RemoteKey
    ): RemoteControlResult {
        // Validate it's a gamepad button
        if (!button.keyName.startsWith("BTN_")) {
            return RemoteControlResult.Error("Invalid gamepad button: ${button.keyName}")
        }
        
        return sendKey(device, button)
    }
    
    /**
     * Convenience methods for gamepad actions
     */
    suspend fun pressGamepadA(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_A)
    suspend fun pressGamepadB(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_B)
    suspend fun pressGamepadX(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_X)
    suspend fun pressGamepadY(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_Y)
    suspend fun pressGamepadL1(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_L1)
    suspend fun pressGamepadL2(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_L2)
    suspend fun pressGamepadR1(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_R1)
    suspend fun pressGamepadR2(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_R2)
    suspend fun pressGamepadSelect(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_SELECT)
    suspend fun pressGamepadStart(device: StbDevice) = sendGamepadButton(device, RemoteKey.GAMEPAD_START)
}
