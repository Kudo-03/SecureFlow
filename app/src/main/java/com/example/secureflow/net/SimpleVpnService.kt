package com.example.secureflow.net

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.secureflow.api.ApiClient
import com.example.secureflow.api.SecureFlowApi
import com.example.secureflow.api.TcpForwardRequest
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class SimpleVpnService : VpnService(), CoroutineScope by MainScope() {

    companion object {
        private const val TAG = "SimpleVpn"
        private const val NOTIF_ID = 1
        private const val NOTIF_CHANNEL_ID = "vpn_channel"
    }

    private var vpnJob: Job? = null
    private lateinit var api: SecureFlowApi
    private val tcpStates = mutableMapOf<String, Long>()
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    @Volatile private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) {
            Log.w(TAG, "VPN already running — ignoring duplicate start")
            return START_NOT_STICKY
        }
        running = true

        startForeground(NOTIF_ID, buildNotification())
        api = ApiClient.build(this, "http://172.104.236.190:80/")
        startVpn()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "SecureFlow VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("SecureFlow VPN")
            .setContentText("Monitoring traffic…")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setOngoing(true)
            .build()
    }

    private fun startVpn() {
        val fd = Builder()
            .setSession("SecureFlow VPN")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addRoute("0.0.0.0", 0)
            .establish() ?: return

        inputStream = FileInputStream(fd.fileDescriptor)
        outputStream = FileOutputStream(fd.fileDescriptor)

        vpnJob = launch(Dispatchers.IO) {
            readPackets()
        }
    }

    private suspend fun readPackets() {
        val buffer = ByteBuffer.allocate(32767)
        while (running && isActive) {
            try {
                buffer.clear()
                val length = inputStream?.read(buffer.array())
                if (length == null || length <= 0) {
                    delay(5)
                    continue
                }
                buffer.limit(length)
                handlePacket(buffer, length)
            } catch (e: IOException) {
                if (!running) break // we are stopping service
                Log.w(TAG, "Read error, retrying: ${e.message}")
                delay(5) // small delay to avoid CPU loop
                continue
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected read error", e)
                delay(5)
            }
        }
    }

    private fun handlePacket(buffer: ByteBuffer, length: Int) {
        val output = outputStream ?: return
        try {
            val ip = PacketUtils.parseIpv4(buffer) ?: return
            if (ip.proto != 6) return

            val tcp = PacketUtils.parseTcp(buffer, ip) ?: return
            val payload = PacketUtils.tcpPayload(buffer, ip, tcp)
            val key = "${ip.src}:${tcp.srcPort}-${ip.dst}:${tcp.dstPort}"

            val syn = tcp.flags and 0x02 != 0
            val ack = tcp.flags and 0x10 != 0
            val fin = tcp.flags and 0x01 != 0
            val rst = tcp.flags and 0x04 != 0

            when {
                syn && !ack -> {
                    Log.d(TAG, "🔹 SYN from $key")
                    val serverSeq = (System.currentTimeMillis() and 0xFFFF).toLong()
                    tcpStates[key] = serverSeq
                    safeWrite(PacketUtils.buildSynAckResponse(ip, tcp, serverSeq).array())
                }
                ack && tcpStates.containsKey(key) && payload.isEmpty() -> {
                    Log.d(TAG, "🔹 Handshake ACK from $key -> ESTABLISHED")
                }
                payload.isNotEmpty() -> {
                    Log.d(TAG, "📤 Forwarding ${payload.size} bytes to API for $key")

                    val request = TcpForwardRequest(
                        src_ip = ip.src,
                        src_port = tcp.srcPort,
                        dst_ip = ip.dst,
                        dst_port = tcp.dstPort,
                        payload_b64 = Base64.encodeToString(payload, Base64.NO_WRAP),
                        syn = syn, fin = fin, rst = rst
                    )

                    launch(Dispatchers.IO) {
                        try {
                            val resp = api.forwardTcp(request)
                            if (resp.isSuccessful) {
                                val body = resp.body()
                                if (body != null && body.payload_b64.isNotEmpty()) {
                                    val respBytes = Base64.decode(body.payload_b64, Base64.NO_WRAP)
                                    safeWrite(PacketUtils.buildTcpResponse(buffer, ip, tcp, respBytes).array())
                                    Log.d(TAG, "✅ Wrote ${respBytes.size} bytes back to TUN")
                                }
                            } else {
                                Log.e(TAG, "❌ API error: ${resp.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ API forward failed", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Packet handling failed", e)
        }
    }

    private fun safeWrite(data: ByteArray) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: IOException) {
            if (running) Log.e(TAG, "Write failed", e)
        }
    }

    override fun onDestroy() {
        running = false
        vpnJob?.cancel()
        cancel()

        try { inputStream?.close() } catch (_: IOException) {}
        try { outputStream?.close() } catch (_: IOException) {}

        tcpStates.clear()
        super.onDestroy()
    }
}
