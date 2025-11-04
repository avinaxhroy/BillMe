package com.billme.app.core.printing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.billme.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for printer connections and communication
 */
@Singleton
class PrinterManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "PrinterManager"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
    
    private val connectedPrinters = ConcurrentHashMap<String, PrinterConnection>()
    private val bluetoothAdapter: BluetoothAdapter? = 
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    
    private val _printerStatus = MutableSharedFlow<PrinterStatus>()
    val printerStatus: SharedFlow<PrinterStatus> = _printerStatus.asSharedFlow()
    
    /**
     * Discover available printers
     */
    suspend fun discoverPrinters(): List<DiscoveredPrinter> = withContext(Dispatchers.IO) {
        val printers = mutableListOf<DiscoveredPrinter>()
        
        // Bluetooth printers
        bluetoothAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                adapter.bondedDevices?.forEach { device ->
                    if (isPrinterDevice(device)) {
                        printers.add(
                            DiscoveredPrinter(
                                id = device.address,
                                name = device.name ?: "Unknown Printer",
                                type = PrinterType.BLUETOOTH,
                                address = device.address,
                                isConnected = connectedPrinters.containsKey(device.address)
                            )
                        )
                    }
                }
            }
        }
        
        printers
    }
    
    /**
     * Connect to a printer
     */
    suspend fun connectToPrinter(
        printerId: String,
        printerConfig: PrinterConfig
    ): ConnectionResult = withContext(Dispatchers.IO) {
        try {
            if (connectedPrinters.containsKey(printerId)) {
                return@withContext ConnectionResult.Success("Already connected to printer")
            }
            
            val connection = when (printerConfig.connectionType) {
                ConnectionType.BLUETOOTH -> {
                    if (printerConfig.connectionDetails is ConnectionDetails.Bluetooth) {
                        connectBluetoothPrinter(printerConfig.connectionDetails.deviceAddress)
                    } else null
                }
                ConnectionType.WIFI -> {
                    if (printerConfig.connectionDetails is ConnectionDetails.WiFi) {
                        connectNetworkPrinter(printerConfig.connectionDetails.ipAddress, printerConfig.connectionDetails.port)
                    } else null
                }
                ConnectionType.ETHERNET -> {
                    if (printerConfig.connectionDetails is ConnectionDetails.Ethernet) {
                        connectNetworkPrinter(printerConfig.connectionDetails.ipAddress, printerConfig.connectionDetails.port)
                    } else null
                }
                ConnectionType.USB -> {
                    if (printerConfig.connectionDetails is ConnectionDetails.USB) {
                        connectUSBPrinter(printerId)
                    } else null
                }
                ConnectionType.SERIAL -> {
                    if (printerConfig.connectionDetails is ConnectionDetails.Serial) {
                        connectSerialPrinter(printerConfig.connectionDetails.portName, printerConfig.connectionDetails.baudRate)
                    } else null
                }
                ConnectionType.VIRTUAL -> null // Virtual printer doesn't need connection
            }
            
            if (connection != null) {
                connectedPrinters[printerId] = connection
                _printerStatus.emit(
                    PrinterStatus(
                        printerId = printerId,
                        status = PrinterConnectionStatus.CONNECTED,
                        message = "Connected successfully"
                    )
                )
                ConnectionResult.Success("Connected to printer successfully")
            } else {
                ConnectionResult.Error("Failed to establish connection")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to printer", e)
            ConnectionResult.Error(e.message ?: "Connection failed")
        }
    }
    
    /**
     * Disconnect from a printer
     */
    suspend fun disconnectFromPrinter(printerId: String): Boolean {
        return try {
            connectedPrinters[printerId]?.let { connection ->
                connection.disconnect()
                connectedPrinters.remove(printerId)
                _printerStatus.emit(
                    PrinterStatus(
                        printerId = printerId,
                        status = PrinterConnectionStatus.DISCONNECTED,
                        message = "Disconnected"
                    )
                )
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect from printer", e)
            false
        }
    }
    
    /**
     * Send data to printer
     */
    suspend fun sendData(printerId: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            connectedPrinters[printerId]?.let { connection ->
                connection.send(data)
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data to printer", e)
            _printerStatus.emit(
                PrinterStatus(
                    printerId = printerId,
                    status = PrinterConnectionStatus.ERROR,
                    message = "Failed to send data: ${e.message}"
                )
            )
            false
        }
    }
    
    /**
     * Get printer status
     */
    suspend fun getPrinterStatus(printerId: String): PrinterConnectionStatus {
        return connectedPrinters[printerId]?.let { connection ->
            if (connection.isConnected()) {
                PrinterConnectionStatus.CONNECTED
            } else {
                PrinterConnectionStatus.DISCONNECTED
            }
        } ?: PrinterConnectionStatus.DISCONNECTED
    }
    
    // Private helper methods
    
    private suspend fun connectBluetoothPrinter(address: String): PrinterConnection? {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                return BluetoothPrinterConnection(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Bluetooth printer", e)
            }
        }
        return null
    }
    
    private suspend fun connectNetworkPrinter(address: String, port: Int): PrinterConnection? {
        return try {
            val socket = Socket(address, port)
            NetworkPrinterConnection(socket)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to network printer", e)
            null
        }
    }
    
    private suspend fun connectUSBPrinter(printerId: String): PrinterConnection? {
        // USB printer connection would be implemented here
        return null
    }
    
    private suspend fun connectSerialPrinter(portName: String, baudRate: Int): PrinterConnection? {
        // Serial printer connection would be implemented here
        return null
    }
    
    private fun isPrinterDevice(device: BluetoothDevice): Boolean {
        val deviceName = device.name?.lowercase() ?: ""
        val printerKeywords = listOf("printer", "print", "pos", "receipt", "thermal")
        return printerKeywords.any { deviceName.contains(it) }
    }
}

/**
 * Printer connection interface
 */
interface PrinterConnection {
    suspend fun send(data: ByteArray): Boolean
    fun isConnected(): Boolean
    fun disconnect()
}

/**
 * Bluetooth printer connection implementation
 */
class BluetoothPrinterConnection(
    private val socket: BluetoothSocket
) : PrinterConnection {
    
    override suspend fun send(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            socket.outputStream.write(data)
            socket.outputStream.flush()
            true
        } catch (e: IOException) {
            false
        }
    }
    
    override fun isConnected(): Boolean = socket.isConnected
    
    override fun disconnect() {
        try {
            socket.close()
        } catch (e: IOException) {
            Log.e("BluetoothConnection", "Error closing socket", e)
        }
    }
}

/**
 * Network printer connection implementation
 */
class NetworkPrinterConnection(
    private val socket: Socket
) : PrinterConnection {
    
    override suspend fun send(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            socket.outputStream.write(data)
            socket.outputStream.flush()
            true
        } catch (e: IOException) {
            false
        }
    }
    
    override fun isConnected(): Boolean = socket.isConnected && !socket.isClosed
    
    override fun disconnect() {
        try {
            socket.close()
        } catch (e: IOException) {
            Log.e("NetworkConnection", "Error closing socket", e)
        }
    }
}

/**
 * Data classes for printer management
 */
data class DiscoveredPrinter(
    val id: String,
    val name: String,
    val type: PrinterType,
    val address: String,
    val isConnected: Boolean = false
)

data class PrinterStatus(
    val printerId: String,
    val status: PrinterConnectionStatus,
    val message: String
)

sealed class ConnectionResult {
    data class Success(val message: String) : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
}

enum class PrinterType {
    BLUETOOTH,
    NETWORK,
    USB
}

enum class PrinterConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    ERROR
}