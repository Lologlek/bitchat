//
// BluetoothTransport.kt
// bitchat Android
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

package com.bitchat.transports

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.bitchat.model.BitchatPacket
import com.bitchat.model.PeerInfo
import com.bitchat.model.BinaryProtocol
import java.util.UUID

/**
 * Minimal Bluetooth LE transport implementation using the same service and
 * characteristic UUIDs as the iOS BluetoothMeshService.
 */
class BluetoothTransport(private val context: Context) : TransportProtocol {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("F47B5E2D-4A9E-4C5A-9B3F-8E1D2C3A4B5C")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("A1B2C3D4-E5F6-4A5B-8C9D-0E1F2A3B4C5D")
    }

    override val transportType = TransportType.BLUETOOTH

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
    private var gattServer: BluetoothGattServer? = null

    private val peers = mutableMapOf<String, BluetoothDevice>()
    private val gattConnections = mutableMapOf<String, BluetoothGatt>()
    private var delegate: TransportDelegate? = null

    override val isAvailable: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    override val currentPeers: List<PeerInfo>
        get() = peers.map { PeerInfo(it.key) }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                peers[device.address] = device
                delegate?.onPeerDiscovered(PeerInfo(device.address))
                gattConnections[device.address]?.close()
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                peers.remove(device.address)?.let { delegate?.onPeerLost(PeerInfo(it.address)) }
                gattConnections.remove(device.address)?.close()
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid == CHARACTERISTIC_UUID) {
                handleIncoming(value, device)
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            if (!peers.containsKey(device.address)) {
                peers[device.address] = device
                delegate?.onPeerDiscovered(PeerInfo(device.address, rssi = result.rssi))
                device.connectGatt(context, false, gattCallback)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                gattConnections[gatt.device.address] = gatt
                gatt.discoverServices()
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                gattConnections.remove(gatt.device.address)?.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID) ?: return
            service.getCharacteristic(CHARACTERISTIC_UUID)?.let {
                gatt.setCharacteristicNotification(it, true)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == CHARACTERISTIC_UUID) {
                handleIncoming(characteristic.value, gatt.device)
            }
        }
    }

    override fun startDiscovery() {
        if (!isAvailable) return

        // Set up GATT server
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback).apply {
            val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val characteristic = BluetoothGattCharacteristic(
                CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            service.addCharacteristic(characteristic)
            addService(service)
        }

        // Start advertising
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(false)
            .build()
        advertiser?.startAdvertising(settings, data, object : AdvertiseCallback() {})

        // Start scanning
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner?.startScan(listOf(filter), scanSettings, scanCallback)
    }

    override fun stopDiscovery() {
        advertiser?.stopAdvertising(object : AdvertiseCallback() {})
        scanner?.stopScan(scanCallback)
        gattServer?.close()
        peers.clear()
    }

    override fun send(packet: BitchatPacket, toPeer: String?) {
        val data = BinaryProtocol.encode(packet)
        if (toPeer == null) {
            peers.values.forEach { sendToDevice(it, data) }
        } else {
            peers[toPeer]?.let { sendToDevice(it, data) }
        }
    }

    private fun sendToDevice(device: BluetoothDevice, data: ByteArray) {
        val gatt = gattConnections[device.address] ?: device.connectGatt(context, false, gattCallback).also {
            gattConnections[device.address] = it
        }
        val service = gatt.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
        if (service == null || characteristic == null) {
            gatt.discoverServices()
            return
        }
        characteristic.value = data
        gatt.writeCharacteristic(characteristic)
    }

    private fun forwardPacket(packet: BitchatPacket, exclude: String) {
        val data = BinaryProtocol.encode(packet)
        peers.forEach { (addr, device) ->
            if (addr != exclude) {
                sendToDevice(device, data)
            }
        }
    }

    override fun setDelegate(delegate: TransportDelegate) {
        this.delegate = delegate
    }

    private fun handleIncoming(data: ByteArray, fromDevice: BluetoothDevice) {
        val packet = BinaryProtocol.decode(data) ?: return
        delegate?.onPacketReceived(packet, PeerInfo(fromDevice.address))
        if (packet.ttl > 1) {
            val relay = packet.copy(ttl = (packet.ttl - 1).toByte())
            forwardPacket(relay, fromDevice.address)
        }
    }
}
