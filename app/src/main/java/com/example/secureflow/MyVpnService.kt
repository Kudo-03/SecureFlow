package com.example.secureflow

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.nio.ByteBuffer

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()
        vpnInterface = builder
            .setSession("MyVPN")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)
            .establish()

        vpnInterface?.let { tun ->
            val input = FileInputStream(tun.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)

            running = true

            Thread {
                while (running) {
                    val length = input.read(buffer.array())
                    if (length > 0) {
                        buffer.limit(length)
                        buffer.position(0)

                        // ✅ This is where you call your packet parser
                        parsePacket(buffer)

                        buffer.clear()
                    }
                }
            }.start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        vpnInterface?.close()
    }
}
