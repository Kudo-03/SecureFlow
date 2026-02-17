package com.example.secureflow


import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

/**
 * Sends synthetic traffic through the VPN UDP channel to Smart-Proxy.
 * Run it after the VPN handshake succeeds.
 */
class VpnAnomalyTester(
    private val scope: CoroutineScope,
    private val serverHost: String,
    private val serverPort: Int
) {
    private val tag = "VpnAnomalyTester"

    fun startAllTests() {
        scope.launch(Dispatchers.IO) { sendNormal() }
        scope.launch(Dispatchers.IO) { delay(2000); sendSmallFlood() }
        scope.launch(Dispatchers.IO) { delay(4000); sendLargeFlood() }
        scope.launch(Dispatchers.IO) { delay(6000); sendMixed() }
    }

    private suspend fun sendNormal() {
        Log.i(tag, "Sending normal packets...")
        sendPackets(50) { Random.nextInt(400, 800) }
    }

    private suspend fun sendSmallFlood() {
        Log.i(tag, "Sending small-packet flood...")
        sendPackets(500) { 1 }
    }

    private suspend fun sendLargeFlood() {
        Log.i(tag, "Sending large-packet flood...")
        sendPackets(30) { 60000 }
    }

    private suspend fun sendMixed() {
        Log.i(tag, "Sending mixed random packets...")
        val sizes = listOf(1, 10, 500, 10000, 50000)
        sendPackets(150) { sizes.random() }
    }

    private suspend fun sendPackets(count: Int, sizeProvider: () -> Int) {
        val socket = DatagramSocket()
        try {
            val target = InetAddress.getByName(serverHost)
            repeat(count) {
                val size = sizeProvider()
                val data = ByteArray(size) { Random.nextInt(0, 255).toByte() }
                socket.send(DatagramPacket(data, data.size, target, serverPort))
                delay(25)
            }
        } catch (t: Throwable) {
            Log.e(tag, "Send error: ${t.message}", t)
        } finally {
            socket.close()
        }
    }
}
