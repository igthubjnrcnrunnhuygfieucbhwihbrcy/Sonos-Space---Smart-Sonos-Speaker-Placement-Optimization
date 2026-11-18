package com.sonosequencer.alternative.network

import android.content.Context
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
import java.net.SocketTimeoutException
import java.nio.charset.Charset

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
    }
    
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    suspend fun discoverDevices(): List<SonosDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<SonosDevice>()
        
        try {
            // Get local IP address
            val wifiInfo = wifiManager.connectionInfo
            val localIp = wifiInfo.ipAddress
            val localIpAddress = InetAddress.getByAddress(
                byteArrayOf(
                    (localIp and 0xff).toByte(),
                    (localIp shr 8 and 0xff).toByte(),
                    (localIp shr 16 and 0xff).toByte(),
                    (localIp shr 24 and 0xff).toByte()
                )
            )
            
            // Create multicast socket
            val socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.broadcast = true
            socket.bind(InetSocketAddress(localIpAddress, 0))
            
            // Join multicast group
            val multicastAddress = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
            socket.joinGroup(multicastAddress)
            
            // Send M-SEARCH message
            val searchMessage = SSDP_SEARCH_MESSAGE.toByteArray(Charset.forName("UTF-8"))
            val packet = DatagramPacket(
                searchMessage,
                searchMessage.size,
                multicastAddress,
                SSDP_PORT
            )
            socket.send(packet)
            
            // Receive responses
            val buffer = ByteArray(BUFFER_SIZE)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.soTimeout = TIMEOUT_MS
            
            while (true) {
                try {
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length, Charset.forName("UTF-8"))
                    
                    // Parse response to extract device information
                    val device = parseDeviceResponse(response, receivePacket.address.hostAddress)
                    if (device != null) {
                        devices.add(device)
                    }
                } catch (e: SocketTimeoutException) {
                    // Expected when no more responses
                    break
                }
            }
            
            socket.leaveGroup(multicastAddress)
            socket.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering Sonos devices", e)
        }
        
        return@withContext devices.distinctBy { it.udn }
    }
    
    private fun parseDeviceResponse(response: String, ipAddress: String): SonosDevice? {
        try {
            val lines = response.split("\r\n")
            var location: String? = null
            var st: String? = null
            var usn: String? = null
            
            for (line in lines) {
                if (line.startsWith("LOCATION:", ignoreCase = true)) {
                    location = line.substringAfter(":").trim()
                } else if (line.startsWith("ST:", ignoreCase = true)) {
                    st = line.substringAfter(":").trim()
                } else if (line.startsWith("USN:", ignoreCase = true)) {
                    usn = line.substringAfter(":").trim()
                }
            }
            
            if (location != null && usn != null) {
                // Extract device info from USN
                val udn = usn.substringAfter("uuid:").substringBefore("::")
                
                // Fetch device description
                val deviceInfo = fetchDeviceDescription(location)
                
                if (deviceInfo != null) {
                    return SonosDevice(
                        ipAddress = ipAddress,
                        port = if (location.contains(":")) location.substringAfterLast(":").toInt() else 1400,
                        deviceDescriptionUrl = location,
                        modelName = deviceInfo.modelName,
                        modelNumber = deviceInfo.modelNumber,
                        friendlyName = deviceInfo.friendlyName,
                        udn = "uuid:$udn"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device response", e)
        }
        
        return null
    }
    
    private fun fetchDeviceDescription(url: String): DeviceDescription? {
        try {
            val httpUrl = if (url.startsWith("http://")) url else "http://$url"
            val connection = java.net.URL(httpUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                return parseDeviceDescription(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device description", e)
        }
        
        return null
    }
    
    private fun parseDeviceDescription(xml: String): DeviceDescription? {
        try {
            var modelName: String? = null
            var modelNumber: String? = null
            var friendlyName: String? = null
            
            // Simple XML parsing (for demo purposes)
            // In production, use proper XML parser
            val modelNameStart = xml.indexOf("<modelName>")
            if (modelNameStart >= 0) {
                val start = xml.indexOf(">", modelNameStart) + 1
                val end = xml.indexOf("</modelName>", start)
                if (end > start) {
                    modelName = xml.substring(start, end)
                }
            }
            
            val modelNumberStart = xml.indexOf("<modelNumber>")
            if (modelNumberStart >= 0) {
                val start = xml.indexOf(">", modelNumberStart) + 1
                val end = xml.indexOf("</modelNumber>", start)
                if (end > start) {
                    modelNumber = xml.substring(start, end)
                }
            }
            
            val friendlyNameStart = xml.indexOf("<friendlyName>")
            if (friendlyNameStart >= 0) {
                val start = xml.indexOf(">", friendlyNameStart) + 1
                val end = xml.indexOf("</friendlyName>", start)
                if (end > start) {
                    friendlyName = xml.substring(start, end)
                }
            }
            
            if (modelName != null && friendlyName != null) {
                return DeviceDescription(
                    modelName = modelName,
                    modelNumber = modelNumber ?: "",
                    friendlyName = friendlyName
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device description", e)
        }
        
        return null
    }
    
    private data class DeviceDescription(
        val modelName: String,
        val modelNumber: String,
        val friendlyName: String
    )
}
