package com.sonosequencer.alternative.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.Charset

/**
 * Discovers Sonos speakers on the local network using UPnP/SSDP protocol.
 * 
 * This class sends M-SEARCH messages to the SSDP multicast address and
 * listens for responses from Sonos ZonePlayer devices.
 */
class SonosDiscovery(private val context: Context) {
    
    companion object {
        private const val TAG = "SonosDiscovery"
        private const val SSDP_PORT = 1900
        private const val SSDP_MULTICAST_ADDRESS = "239.255.255.250"
        private const val SSDP_SEARCH_MESSAGE = "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: 3\r\n" +
                "ST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n" +
                "\r\n"
        
        private const val TIMEOUT_MS = 5000
        private const val BUFFER_SIZE = 8192
        private const val MAX_RETRIES = 2
    }
    
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    private val connectivityManager: ConnectivityManager by lazy {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    /**
     * Checks if the device is connected to a WiFi network.
     */
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Discovers Sonos devices on the local network.
     * 
     * @return List of discovered Sonos devices
     */
    suspend fun discoverDevices(): List<SonosDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<SonosDevice>()
        var socket: DatagramSocket? = null
        
        try {
            // Check if WiFi is connected
            if (!isWifiConnected()) {
                Log.w(TAG, "WiFi not connected, cannot discover devices")
                return@withContext devices
            }
            
            // Get local IP address
            val localIpAddress = getLocalIpAddress() ?: run {
                Log.w(TAG, "Could not determine local IP address")
                return@withContext devices
            }
            
            // Create and configure multicast socket
            socket = createMulticastSocket(localIpAddress)
            
            // Send M-SEARCH message and collect responses
            for (attempt in 1..MAX_RETRIES) {
                try {
                    val foundDevices = sendSearchAndCollectResponses(socket, localIpAddress)
                    devices.addAll(foundDevices)
                    
                    // If we found devices, break out of retry loop
                    if (foundDevices.isNotEmpty()) {
                        break
                    }
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "Search attempt $attempt timed out", e)
                    if (attempt == MAX_RETRIES) {
                        Log.w(TAG, "All search attempts timed out")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error during search attempt $attempt", e)
                }
            }
            
        } catch (e: SocketException) {
            Log.e(TAG, "Socket error during discovery", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for network operations", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during discovery", e)
        } finally {
            // Always close the socket
            try {
                socket?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing socket", e)
            }
        }
        
        return@withContext devices.distinctBy { it.udn }
    }
    
    /**
     * Gets the local IP address of the device.
     */
    private fun getLocalIpAddress(): InetAddress? {
        return try {
            val wifiInfo = wifiManager.connectionInfo
            val localIp = wifiInfo.ipAddress
            InetAddress.getByAddress(
                byteArrayOf(
                    (localIp and 0xff).toByte(),
                    (localIp shr 8 and 0xff).toByte(),
                    (localIp shr 16 and 0xff).toByte(),
                    (localIp shr 24 and 0xff).toByte()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address", e)
            null
        }
    }
    
    /**
     * Creates and configures a multicast socket for SSDP discovery.
     */
    private fun createMulticastSocket(localAddress: InetAddress): DatagramSocket {
        val socket = DatagramSocket(null)
        socket.reuseAddress = true
        socket.broadcast = true
        socket.bind(InetSocketAddress(localAddress, 0))
        
        // Join multicast group
        val multicastAddress = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
        socket.joinGroup(multicastAddress)
        
        // Set timeout
        socket.soTimeout = TIMEOUT_MS
        
        return socket
    }
    
    /**
     * Sends SSDP M-SEARCH message and collects responses.
     */
    private fun sendSearchAndCollectResponses(
        socket: DatagramSocket,
        localAddress: InetAddress
    ): List<SonosDevice> {
        val devices = mutableListOf<SonosDevice>()
        
        try {
            // Send M-SEARCH message
            val searchMessage = SSDP_SEARCH_MESSAGE.toByteArray(Charset.forName("UTF-8"))
            val multicastAddress = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
            val packet = DatagramPacket(
                searchMessage,
                searchMessage.size,
                multicastAddress,
                SSDP_PORT
            )
            socket.send(packet)
            Log.d(TAG, "Sent M-SEARCH message to $multicastAddress:$SSDP_PORT")
            
            // Receive responses
            val buffer = ByteArray(BUFFER_SIZE)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            
            while (true) {
                try {
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length, Charset.forName("UTF-8"))
                    
                    // Parse response to extract device information
                    val device = parseDeviceResponse(response, receivePacket.address.hostAddress)
                    if (device != null) {
                        Log.d(TAG, "Discovered device: ${device.friendlyName} at ${device.ipAddress}")
                        devices.add(device)
                    }
                } catch (e: SocketTimeoutException) {
                    // Expected when no more responses
                    Log.d(TAG, "No more responses, discovery complete")
                    break
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during receive", e)
        }
        
        return devices
    }
    
    /**
     * Parses SSDP response to extract device information.
     */
    private fun parseDeviceResponse(response: String, ipAddress: String): SonosDevice? {
        return try {
            val lines = response.split("\r\n")
            var location: String? = null
            var st: String? = null
            var usn: String? = null
            
            for (line in lines) {
                when {
                    line.startsWith("LOCATION:", ignoreCase = true) -> {
                        location = line.substringAfter(":").trim()
                    }
                    line.startsWith("ST:", ignoreCase = true) -> {
                        st = line.substringAfter(":").trim()
                    }
                    line.startsWith("USN:", ignoreCase = true) -> {
                        usn = line.substringAfter(":").trim()
                    }
                }
            }
            
            if (location != null && usn != null) {
                // Extract device info from USN
                val udn = usn.substringAfter("uuid:").substringBefore("::")
                
                // Fetch device description
                val deviceInfo = fetchDeviceDescription(location, ipAddress)
                
                if (deviceInfo != null) {
                    return SonosDevice(
                        ipAddress = ipAddress,
                        port = if (location.contains(":")) 
                            location.substringAfterLast(":").toIntOrNull() ?: 1400 
                        else 1400,
                        deviceDescriptionUrl = location,
                        modelName = deviceInfo.modelName,
                        modelNumber = deviceInfo.modelNumber,
                        friendlyName = deviceInfo.friendlyName,
                        udn = "uuid:$udn"
                    )
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device response", e)
            null
        }
    }
    
    /**
     * Fetches device description from the LOCATION URL.
     */
    private fun fetchDeviceDescription(url: String, fallbackIp: String): DeviceDescription? {
        return try {
            // Ensure URL has proper scheme
            val httpUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else {
                "http://$url"
            }
            
            val connection = java.net.URL(httpUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                connection.disconnect()
                
                return parseDeviceDescription(response)
            } else {
                Log.w(TAG, "HTTP ${connection.responseCode} when fetching device description from $httpUrl")
            }
        } catch (e: UnknownHostException) {
            // Try with fallback IP if URL doesn't resolve
            Log.w(TAG, "Unknown host, trying with fallback IP: $fallbackIp")
            tryWithFallbackIp(url, fallbackIp)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device description from $url", e)
        }
        
        return null
    }
    
    /**
     * Tries to fetch device description using fallback IP.
     */
    private fun tryWithFallbackIp(url: String, fallbackIp: String): DeviceDescription? {
        return try {
            // Extract path from URL
            val path = if (url.startsWith("http://") || url.startsWith("https://")) {
                java.net.URI(url).path
            } else if (url.startsWith("/")) {
                url
            } else {
                "/$url"
            }
            
            val httpUrl = "http://$fallbackIp$path"
            val connection = java.net.URL(httpUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                connection.disconnect()
                
                return parseDeviceDescription(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device description with fallback IP", e)
        }
        
        return null
    }
    
    /**
     * Parses device description XML to extract device information.
     */
    private fun parseDeviceDescription(xml: String): DeviceDescription? {
        return try {
            var modelName: String? = null
            var modelNumber: String? = null
            var friendlyName: String? = null
            
            // Simple XML parsing - extract values between tags
            fun extractTagValue(tagName: String): String? {
                val startTag = "<$tagName>"
                val endTag = "</$tagName>"
                val start = xml.indexOf(startTag)
                if (start >= 0) {
                    val valueStart = start + startTag.length
                    val end = xml.indexOf(endTag, valueStart)
                    if (end > valueStart) {
                        return xml.substring(valueStart, end)
                    }
                }
                return null
            }
            
            modelName = extractTagValue("modelName")
            modelNumber = extractTagValue("modelNumber")
            friendlyName = extractTagValue("friendlyName")
            
            // Also try with device/ prefix for some Sonos devices
            if (modelName == null) modelName = extractTagValue("device")?.let { device ->
                extractTagValueFromDevice(device, "modelName")
            }
            
            if (modelName != null && friendlyName != null) {
                return DeviceDescription(
                    modelName = modelName,
                    modelNumber = modelNumber ?: "",
                    friendlyName = friendlyName
                )
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device description XML", e)
            null
        }
    }
    
    private fun extractTagValueFromDevice(deviceXml: String, tagName: String): String? {
        val startTag = "<$tagName>"
        val endTag = "</$tagName>"
        val start = deviceXml.indexOf(startTag)
        if (start >= 0) {
            val valueStart = start + startTag.length
            val end = deviceXml.indexOf(endTag, valueStart)
            if (end > valueStart) {
                return deviceXml.substring(valueStart, end)
            }
        }
        return null
    }
    
    private data class DeviceDescription(
        val modelName: String,
        val modelNumber: String,
        val friendlyName: String
    )
}

/**
 * Data class representing a Sonos device.
 */
data class SonosDevice(
    val ipAddress: String,
    val port: Int,
    val deviceDescriptionUrl: String,
    val modelName: String,
    val modelNumber: String,
    val friendlyName: String,
    val udn: String
)
