package com.example.secureflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.math.min

class MyVpnService : VpnService(), CoroutineScope by MainScope() {

    companion object {
        private const val TAG = "MyVpnService"
        private const val CHANNEL_ID = "custom_vpn"
        private const val NOTIF_ID = 1001

        // CHANGE THESE
        private const val SERVER_HOST = "172.105.95.168"   // <- set this
        private const val SERVER_PORT = 1194

        // keepalive interval seconds
        private const val KEEPALIVE_INTERVAL_S = 20L

        // max packet sizes
        private const val MAX_PACKET = 65535
    }

    private var tunFd: ParcelFileDescriptor? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null

    private var udpSocket: DatagramSocket? = null

    private var jobTunToNet: Job? = null
    private var jobNetToTun: Job? = null
    private var jobKeepAlive: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        launch(Dispatchers.Main) {
            try {
                performHandshakeAndStart()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to start VPN", t)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Custom VPN", NotificationManager.IMPORTANCE_LOW)
                )
            }
            val notif = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Custom VPN running")
                .setContentText("Tap to manage")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
            startForeground(NOTIF_ID, notif)
        } else {
            val notif = Notification.Builder(this)
                .setContentTitle("Custom VPN running")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
            startForeground(NOTIF_ID, notif)
        }
    }

    private suspend fun performHandshakeAndStart() = withContext(Dispatchers.IO) {
        // 1) create UDP socket and protect it BEFORE connecting
        val sock = DatagramSocket()
        try {
            // protect the socket so Android doesn't route it via the VPN
            // VpnService.protect has overloads for both Socket and DatagramSocket on modern Android.
            // If your platform doesn't have protect(DatagramSocket), you can call protect(sock.socket) or protect(fd) if needed.
            try {
                protect(sock)
            } catch (ex: Throwable) {
                // Some devices/SDK combos may not accept protect(DatagramSocket) directly.
                // Attempt the socket's underlying Socket if available, otherwise rethrow.
                Log.w(TAG, "protect(datagramSocket) failed: ${ex.message}. Continuing (you may need to adapt).")
            }

            sock.soTimeout = 5000
            sock.connect(InetSocketAddress(SERVER_HOST, SERVER_PORT))

            // 2) send HELLO
            val hello = "HELLO".toByteArray(Charsets.UTF_8)
            sock.send(DatagramPacket(hello, hello.size))

            // 3) receive CONFIG|<ip>|<mask>|<dns>
            val buf = ByteArray(1024)
            val rcv = DatagramPacket(buf, buf.size)
            sock.receive(rcv)
            val resp = String(rcv.data, 0, rcv.length, Charsets.UTF_8).trim()
            if (!resp.startsWith("CONFIG|")) {
                throw IllegalStateException("Bad handshake response: $resp")
            }
            val parts = resp.split("|")
            if (parts.size < 3) throw IllegalStateException("Bad CONFIG format")
            val assignedIp = parts[1].trim()
            val mask = parts[2].trim().toIntOrNull() ?: 32
            val dns = parts.getOrNull(3)?.trim() ?: "1.1.1.1"

            Log.i(TAG, "Assigned IP: $assignedIp/$mask DNS=$dns")

            // 4) build TUN with assigned IP
            val builder = Builder()
                .setSession("CustomVPN")
                .setMtu(1500)
                .addAddress(assignedIp, mask)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(dns)

            tunFd = builder.establish() ?: throw IllegalStateException("Failed to establish VPN")
            input = FileInputStream(tunFd!!.fileDescriptor)
            output = FileOutputStream(tunFd!!.fileDescriptor)

            // 5) store socket and start loops
            udpSocket = sock
            startLoops()
        } catch (t: Throwable) {
            try { sock.close() } catch (_: Throwable) {}
            throw t
        }
    }

    private fun startLoops() {
        // TUN -> NET
        jobTunToNet = launch(Dispatchers.IO) {
            val readBuf = ByteArray(MAX_PACKET)
            while (isActive) {
                try {
                    val n = input!!.read(readBuf)
                    if (n > 0) {
                        // prefix with 2-byte big-endian length
                        val out = ByteArray(n + 2)
                        out[0] = ((n ushr 8) and 0xFF).toByte()
                        out[1] = (n and 0xFF).toByte()
                        System.arraycopy(readBuf, 0, out, 2, n)

                        val dp = DatagramPacket(out, out.size)
                        udpSocket!!.send(dp)
                    } else {
                        // maybe EOF – break
                        delay(1)
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Tun->Net error: ${t.message}")
                    delay(100)
                }
            }
        }

        // NET -> TUN
        jobNetToTun = launch(Dispatchers.IO) {
            val buf = ByteArray(MAX_PACKET)
            val dp = DatagramPacket(buf, buf.size)
            while (isActive) {
                try {
                    udpSocket!!.receive(dp)
                    if (dp.length < 2) continue
                    val hi = dp.data[0].toInt() and 0xFF
                    val lo = dp.data[1].toInt() and 0xFF
                    val len = (hi shl 8) or lo
                    if (len <= 0 || len > dp.length - 2) continue
                    // write raw IP packet into TUN
                    output!!.write(dp.data, 2, len)
                } catch (t: Throwable) {
                    Log.w(TAG, "Net->Tun error: ${t.message}")
                    delay(10)
                }
            }
        }

        // Keepalive
        jobKeepAlive = launch(Dispatchers.IO) {
            val ka = "KEEPALIVE".toByteArray(Charsets.UTF_8)
            while (isActive) {
                try {
                    val dp = DatagramPacket(ka, ka.size)
                    udpSocket!!.send(dp)
                } catch (t: Throwable) {
                    Log.w(TAG, "Keepalive send failed: ${t.message}")
                }
                delay(KEEPALIVE_INTERVAL_S * 1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownEverything()
        cancel() // cancel coroutines
    }

    private fun shutdownEverything() {
        try { jobTunToNet?.cancel() } catch (_: Throwable) {}
        try { jobNetToTun?.cancel() } catch (_: Throwable) {}
        try { jobKeepAlive?.cancel() } catch (_: Throwable) {}
        try { udpSocket?.close() } catch (_: Throwable) {}
        try { input?.close() } catch (_: Throwable) {}
        try { output?.close() } catch (_: Throwable) {}
        try { tunFd?.close() } catch (_: Throwable) {}
    }
}