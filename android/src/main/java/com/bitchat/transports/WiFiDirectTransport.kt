//
// WiFiDirectTransport.kt
// bitchat Android
//
// This is free and unencumbered software released into the public domain.
// For more information, see <https://unlicense.org>
//

package com.bitchat.transports

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.bitchat.model.BitchatPacket
import com.bitchat.model.BinaryProtocol
import com.bitchat.model.PeerInfo
import java.net.ServerSocket
import java.net.Socket

class WiFiDirectTransport(private val context: Context) : TransportProtocol {
    override val transportType = TransportType.WIFI_DIRECT

    private val PORT = 8988

    private val manager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel = manager?.initialize(context, context.mainLooper, null)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val peers = mutableMapOf<String, WifiP2pDevice>()
    private var delegate: TransportDelegate? = null
    private var receiverRegistered = false
    private var serverJob: Job? = null

    override val isAvailable: Boolean
        get() = manager != null

    override val currentPeers: List<PeerInfo>
        get() = peers.values.map { PeerInfo(it.deviceAddress, it.deviceName) }

    private val peerListListener = WifiP2pManager.PeerListListener { list ->
        val newPeers = list.deviceList.associateBy { it.deviceAddress }
        val lost = peers.keys - newPeers.keys
        val gained = newPeers.keys - peers.keys
        lost.forEach { delegate?.onPeerLost(PeerInfo(it)) }
        gained.forEach { id ->
            newPeers[id]?.let { dev ->
                delegate?.onPeerDiscovered(PeerInfo(dev.deviceAddress, dev.deviceName))
            }
        }
        peers.clear()
        peers.putAll(newPeers)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.requestPeers(channel, peerListListener)
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    manager?.requestPeers(channel, peerListListener)
                }
            }
        }
    }

    override fun startDiscovery() {
        if (!isAvailable || receiverRegistered) return

        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
        receiverRegistered = true

        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })

        serverJob = scope.launch {
            try {
                val server = ServerSocket(PORT)
                while (true) {
                    val socket = server.accept()
                    val data = socket.getInputStream().readBytes()
                    val packet = BinaryProtocol.decode(data)
                    if (packet != null) {
                        delegate?.onPacketReceived(packet, PeerInfo(socket.inetAddress.hostAddress ?: ""))
                    }
                    socket.close()
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun stopDiscovery() {
        if (!isAvailable) return

        if (receiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
            }
            receiverRegistered = false
        }

        manager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })

        serverJob?.cancel()
        serverJob = null
        peers.clear()
    }

    override fun send(packet: BitchatPacket, toPeer: String?) {
        val targets = if (toPeer == null) peers.keys.toList() else listOf(toPeer)
        val data = BinaryProtocol.encode(packet)

        for (id in targets) {
            val device = peers[id] ?: continue
            val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
            manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    manager?.requestConnectionInfo(channel) { info ->
                        val host = info.groupOwnerAddress
                        if (host != null) {
                            scope.launch {
                                try {
                                    Socket(host, PORT).use { socket ->
                                        socket.getOutputStream().write(data)
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                }

                override fun onFailure(reason: Int) {}
            })
        }
    }

    override fun setDelegate(delegate: TransportDelegate) {
        this.delegate = delegate
    }
}
