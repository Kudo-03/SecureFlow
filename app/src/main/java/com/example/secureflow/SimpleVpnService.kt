// MyVpnService.kt
package com.example.secureflow

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.*

class MyVpnService : VpnService(), CoroutineScope by MainScope() {

    companion object {
        private const val TAG = "MyVpnService"
        private const val CHANNEL_ID = "vpn_channel"
        private const val NOTIF_ID = 42

        // >>> Replace with your server’s public IP / DNS and port
        private const val SERVER_HOST = "172.104.236.190"
        private const val SERVER_PORT = 1194

        // Virtual network settings for client side
        private const val VPN_MTU = 1500
        private const val VPN_IP = "10.0.0.2"      // client IP
        private const val VPN_MASK = 32
        private const val VPN_DNS = "1.1.1.1"      // or your own DNS
    }

    private var tunFd: ParcelFileDescriptor? = null
    private var jobTunToNet: Job? = null
    private var jobNetToTun: Job? = null
    private var socket: DatagramSocket? = null
    private var output: FileOutputStream? = null
    private var input: FileInputStream? = null

    override fun onCreate() {
        super.onCreate()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        launch {
            try {
                setupTun()
                setupSocketAndLoops()
            } catch (t: Throwable) {
                Log.e(TAG, "Error starting VPN", t)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "VPN", NotificationManager.IMPORTANCE_LOW)
                )
            }
            val notif = Notification.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setContentTitle("Custom VPN is running")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun setupTun() {
        val builder = Builder()
            .setSession("CustomVPN")
            .setMtu(VPN_MTU)
            .addAddress(VPN_IP, VPN_MASK)
            .addDnsServer(VPN_DNS)
            .addRoute("0.0.0.0", 0)     // full tunnel for IPv4

        tunFd = builder.establish() ?: throw IllegalStateException("Failed to establish TUN")
        input = FileInputStream(tunFd!!.fileDescriptor)   // TUN -> app
        output = FileOutputStream(tunFd!!.fileDescriptor) // app -> TUN
    }

    private suspend fun setupSocketAndLoops() {
        withContext(Dispatchers.IO) {
            socket = DatagramSocket().apply {
                soTimeout = 0
                protect(this)
                connect(InetSocketAddress(SERVER_HOST, SERVER_PORT)) // now off main
            }
        }
        // then launch the two loops on Dispatchers.IO as you already do

    // TUN -> NET (send)
        jobTunToNet = launch(Dispatchers.IO) {
            val buf = ByteArray(65535)
            while (isActive) {
                val n = input!!.read(buf)
                if (n > 0) {
                    // wrap: 2-byte length prefix (big-endian)
                    val out = ByteArray(n + 2)
                    out[0] = ((n ushr 8) and 0xFF).toByte()
                    out[1] = (n and 0xFF).toByte()
                    System.arraycopy(buf, 0, out, 2, n)
                    val dp = DatagramPacket(out, out.size)
                    socket!!.send(dp)
                }
            }
        }

        // NET -> TUN (receive)
        jobNetToTun = launch(Dispatchers.IO) {
            val buf = ByteArray(65535)
            while (isActive) {
                val dp = DatagramPacket(buf, buf.size)
                socket!!.receive(dp)
                if (dp.length < 2) continue
                val hi = dp.data[0].toInt() and 0xFF
                val lo = dp.data[1].toInt() and 0xFF
                val len = (hi shl 8) or lo
                if (len <= 0 || len > dp.length - 2) continue
                output!!.write(dp.data, 2, len)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jobTunToNet?.cancel()
        jobNetToTun?.cancel()
        socket?.close()
        input?.close()
        output?.close()
        tunFd?.close()
        cancel()
    }
}
