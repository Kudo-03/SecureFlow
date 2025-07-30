package com.example.secureflow

import android.util.Log
import java.nio.ByteBuffer

fun parsePacket(buffer: ByteBuffer) {
    val version = (buffer.get(0).toInt() shr 4) and 0x0F
    if (version == 4) {
        val protocol = buffer.get(9).toInt() and 0xFF
        val sourceIp = "${buffer.get(12).toInt() and 0xFF}.${buffer.get(13).toInt() and 0xFF}." +
                "${buffer.get(14).toInt() and 0xFF}.${buffer.get(15).toInt() and 0xFF}"
        val destIp = "${buffer.get(16).toInt() and 0xFF}.${buffer.get(17).toInt() and 0xFF}." +
                "${buffer.get(18).toInt() and 0xFF}.${buffer.get(19).toInt() and 0xFF}"

        Log.d("PacketInfo", "Protocol: $protocol, Source: $sourceIp, Dest: $destIp")

        // Now parse ports if it's TCP (6) or UDP (17)
        if (protocol == 6 || protocol == 17) {
            val sourcePort = ((buffer.get(20).toInt() and 0xFF) shl 8) or (buffer.get(21).toInt() and 0xFF)
            val destPort = ((buffer.get(22).toInt() and 0xFF) shl 8) or (buffer.get(23).toInt() and 0xFF)
            Log.d("PacketInfo", "SrcPort: $sourcePort, DstPort: $destPort")
        }
    }
}
