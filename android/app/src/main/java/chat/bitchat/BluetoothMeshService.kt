package chat.bitchat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import java.nio.charset.Charset
import java.util.UUID

class BluetoothMeshService(private val viewModel: ChatViewModel) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("F47B5E2D-4A9E-4C5A-9B3F-8E1D2C3A4B5C")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("A1B2C3D4-E5F6-4A5B-8C9D-0E1F2A3B4C5D")
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var gattServer: BluetoothGattServer? = null

    fun start(context: Context) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = manager.openGattServer(context, object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                // Simple peer tracking
                device?.address?.let { addr ->
                    val peers = viewModel.connectedPeers.value.toMutableList()
                    if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                        if (!peers.contains(addr)) peers.add(addr)
                    } else {
                        peers.remove(addr)
                    }
                    viewModel.updatePeers(peers)
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                if (characteristic?.uuid == CHARACTERISTIC_UUID && value != null) {
                    BinaryProtocol.decode(value)?.let { packet ->
                        val messageStr = packet.payload.toString(Charset.defaultCharset())
                        val msg = BitchatMessage(
                            sender = device?.address ?: "peer",
                            content = messageStr,
                            timestamp = java.util.Date(),
                            isRelay = false
                        )
                        viewModel.receive(msg)
                    }
                }
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            }
        })

        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
    }

    fun send(message: BitchatMessage) {
        val packet = BitchatPacket(
            type = MessageType.message.value,
            senderID = ("local".toByteArray()),
            recipientID = null,
            timestamp = System.currentTimeMillis(),
            payload = message.content.toByteArray(),
            signature = null,
            ttl = 1
        )
        val data = BinaryProtocol.encode(packet)
        // In a real implementation we'd iterate connected devices and write
    }
}

enum class MessageType(val value: Byte) {
    announce(0x01),
    keyExchange(0x02),
    leave(0x03),
    message(0x04)
}
