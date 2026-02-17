package com.example.secureflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class MyVpnService : VpnService(), CoroutineScope by MainScope() {

    companion object {
        private const val TAG = "MyVpnService"
        private const val CHANNEL_ID = "custom_vpn"
        private const val NOTIF_ID = 1001

        // Set your backend server info
        private const val SERVER_HOST = "172.105.95.168"
        private const val SERVER_PORT = 1194

        private const val KEEPALIVE_INTERVAL_S = 20L
        private const val MAX_PACKET = 65535
    }

    private var tunFd: ParcelFileDescriptor? = null
    private var input: FileInputStream? = null
    private var output: FileOutputStream? = null
    private var udpSocket: DatagramSocket? = null

    private var jobTunToNet: Job? = null
    private var jobNetToTun: Job? = null
    private var jobKeepAlive: Job? = null

    private var currentUserId: String = "anonymous"

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Extract UID safely from the received Intent
        currentUserId = intent?.getStringExtra("USER_ID") ?: run {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            } catch (e: Exception) {
                "anonymous"
            }
        }

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
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            notifManager.getNotificationChannel(CHANNEL_ID) == null
        ) {
            notifManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "SecureFlow VPN",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("SecureFlow VPN active")
                .setContentText("Monitoring traffic securely...")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("SecureFlow VPN active")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
        }

        startForeground(NOTIF_ID, notif)
    }

    private suspend fun performHandshakeAndStart() = withContext(Dispatchers.IO) {
        val sock = DatagramSocket()
        try {
            protect(sock) // Avoid routing this socket inside VPN
            sock.soTimeout = 5000
            sock.connect(InetSocketAddress(SERVER_HOST, SERVER_PORT))

            // 🔹 Send JSON HELLO with UID
            val helloJson = """{"uid": "$currentUserId"}"""
            val helloBytes = helloJson.toByteArray(Charsets.UTF_8)
            sock.send(DatagramPacket(helloBytes, helloBytes.size))
            Log.i(TAG, "Sent HELLO JSON with UID: $currentUserId")

            // 🔹 Receive CONFIG
            val buf = ByteArray(1024)
            val rcv = DatagramPacket(buf, buf.size)
            sock.receive(rcv)
            val resp = String(rcv.data, 0, rcv.length).trim()

            if (!resp.startsWith("CONFIG|"))
                throw IllegalStateException("Bad handshake response: $resp")

            val parts = resp.split("|")
            val assignedIp = parts.getOrNull(1)?.trim() ?: "10.0.0.2"
            val mask = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 24
            val dns = parts.getOrNull(3)?.trim() ?: "1.1.1.1"

            Log.i(TAG, "Assigned IP: $assignedIp/$mask DNS=$dns")

            // 🔹 Build TUN interface
            val builder = Builder()
                .setSession("SecureFlow VPN")
                .setMtu(1500)
                .addAddress(assignedIp, mask)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(dns)
                .allowFamily(OsConstants.AF_INET)

            tunFd = builder.establish() ?: throw IllegalStateException("Failed to establish VPN")
            input = FileInputStream(tunFd!!.fileDescriptor)
            output = FileOutputStream(tunFd!!.fileDescriptor)
            udpSocket = sock

            startLoops()

        } catch (t: Throwable) {
            Log.e(TAG, "Handshake error", t)
            try { sock.close() } catch (_: Throwable) {}
            throw t
        }
    }

    private fun startLoops() {
        // Launch anomaly tester if needed
        VpnAnomalyTester(this, SERVER_HOST, SERVER_PORT).startAllTests()

        // 🔹 TUN → NET
        jobTunToNet = launch(Dispatchers.IO) {
            val readBuf = ByteArray(MAX_PACKET)
            while (isActive) {
                try {
                    val n = input?.read(readBuf) ?: break
                    if (n > 0) {
                        val frame = ByteArray(n + 2)
                        frame[0] = ((n ushr 8) and 0xFF).toByte()
                        frame[1] = (n and 0xFF).toByte()
                        System.arraycopy(readBuf, 0, frame, 2, n)
                        udpSocket?.send(DatagramPacket(frame, frame.size))
                    } else delay(5)
                } catch (t: Throwable) {
                    Log.w(TAG, "Tun→Net error: ${t.message}")
                    delay(100)
                }
            }
        }

        // 🔹 NET → TUN
        jobNetToTun = launch(Dispatchers.IO) {
            val buf = ByteArray(MAX_PACKET)
            val dp = DatagramPacket(buf, buf.size)
            while (isActive) {
                try {
                    udpSocket?.receive(dp)
                    if (dp.length < 2) continue
                    val hi = dp.data[0].toInt() and 0xFF
                    val lo = dp.data[1].toInt() and 0xFF
                    val len = (hi shl 8) or lo
                    if (len > 0 && len <= dp.length - 2) {
                        output?.write(dp.data, 2, len)
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Net→Tun error: ${t.message}")
                    delay(10)
                }
            }
        }

        // 🔹 Keepalive
        jobKeepAlive = launch(Dispatchers.IO) {
            val ka = "KEEPALIVE".toByteArray(Charsets.UTF_8)
            while (isActive) {
                try {
                    udpSocket?.send(DatagramPacket(ka, ka.size))
                } catch (t: Throwable) {
                    Log.w(TAG, "Keepalive failed: ${t.message}")
                }
                delay(KEEPALIVE_INTERVAL_S * 1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownEverything()
        cancel()
    }

    private fun shutdownEverything() {
        listOf(jobTunToNet, jobNetToTun, jobKeepAlive).forEach {
            try { it?.cancel() } catch (_: Throwable) {}
        }
        try { udpSocket?.close() } catch (_: Throwable) {}
        try { input?.close() } catch (_: Throwable) {}
        try { output?.close() } catch (_: Throwable) {}
        try { tunFd?.close() } catch (_: Throwable) {}
    }
}
