package com.example.secureflow.net

import java.nio.ByteBuffer
import kotlin.math.min

object PacketUtils {
    // Parse IPv4 basic
    data class Ipv4(
        val ihlBytes: Int,
        val totalLen: Int,
        val proto: Int,
        val src: String,
        val dst: String
    )

    data class Tcp(
        val srcPort: Int,
        val dstPort: Int,
        val dataOffsetBytes: Int,
        val seq: Long,
        val ack: Long,
        val flags: Int
    )

    fun getU8(b: Byte): Int = b.toInt() and 0xFF
    fun getU16(b: Short): Int = b.toInt() and 0xFFFF
    fun getU32(i: Int): Long = (i.toLong() and 0xFFFFFFFFL)

    fun parseIpv4(buf: ByteBuffer): Ipv4? {
        if (buf.remaining() < 20) return null
        val verIhl = getU8(buf.get(0))
        val version = (verIhl shr 4) and 0xF
        if (version != 4) return null
        val ihlBytes = (verIhl and 0xF) * 4
        val totalLen = getU16(buf.getShort(2))
        val proto = getU8(buf.get(9))
        val src = "${getU8(buf.get(12))}.${getU8(buf.get(13))}.${getU8(buf.get(14))}.${getU8(buf.get(15))}"
        val dst = "${getU8(buf.get(16))}.${getU8(buf.get(17))}.${getU8(buf.get(18))}.${getU8(buf.get(19))}"
        return Ipv4(ihlBytes, totalLen, proto, src, dst)
    }

    fun parseTcp(buf: ByteBuffer, ip: Ipv4): Tcp? {
        val start = ip.ihlBytes
        if (buf.limit() < start + 20) return null
        val srcPort = getU16(buf.getShort(start))
        val dstPort = getU16(buf.getShort(start + 2))
        val seq = getU32(buf.getInt(start + 4))
        val ack = getU32(buf.getInt(start + 8))
        val dataOff = (getU8(buf.get(start + 12)) shr 4) * 4
        val flags = getU8(buf.get(start + 13))
        return Tcp(srcPort, dstPort, dataOff, seq, ack, flags)
    }

    fun tcpPayload(buf: ByteBuffer, ip: Ipv4, tcp: Tcp): ByteArray {
        val off = ip.ihlBytes + tcp.dataOffsetBytes
        val len = ip.totalLen - off
        if (len <= 0) return ByteArray(0)
        val arr = ByteArray(len)
        val oldPos = buf.position()
        buf.position(off)
        buf.get(arr, 0, min(len, buf.limit() - off))
        buf.position(oldPos)
        return arr
    }

    // Build a basic IPv4/TCP packet for server->client (reply)
    fun buildTcpResponse(
        orig: ByteBuffer,
        ip: Ipv4,
        tcp: Tcp,
        payload: ByteArray
    ): ByteBuffer {
        val respIhl = 20
        val respTcph = 20
        val total = respIhl + respTcph + payload.size
        val out = ByteBuffer.allocate(total)

        // IPv4
        out.put(0, ((4 shl 4) or (respIhl / 4)).toByte()) // ver/ihl
        out.put(1, 0.toByte()) // DSCP/ECN
        putU16(out, 2, total)
        putU16(out, 4, 0) // identification
        putU16(out, 6, 0x4000) // flags/frag offset (DF set)
        out.put(8, 64.toByte()) // TTL
        out.put(9, 6.toByte())  // TCP
        // src/dst swap
        val srcOct = ip.dst.split(".").map { it.toInt().toByte() }
        val dstOct = ip.src.split(".").map { it.toInt().toByte() }
        out.put(12, srcOct[0]); out.put(13, srcOct[1]); out.put(14, srcOct[2]); out.put(15, srcOct[3])
        out.put(16, dstOct[0]); out.put(17, dstOct[1]); out.put(18, dstOct[2]); out.put(19, dstOct[3])
        putU16(out, 10, 0) // checksum placeholder

        // TCP
        putU16(out, 20, tcp.dstPort) // srcPort = original dst
        putU16(out, 22, tcp.srcPort) // dstPort = original src
        putU32(out, 24, tcp.ack)     // seq = original ack
        // ack = original seq + payload_len
        val segLen = payload.size
        val newAck = (tcp.seq + segLen) and 0xFFFFFFFFL
        putU32(out, 28, newAck)
        out.put(32, ((respTcph / 4) shl 4).toByte()) // data offset
        // set ACK + PSH when payload exists, else ACK
        val ackFlag = 0x10
        val pshFlag = if (segLen > 0) 0x08 else 0x00
        out.put(33, (ackFlag or pshFlag).toByte())
        putU16(out, 34, 65535) // window
        putU16(out, 36, 0)     // checksum placeholder
        putU16(out, 38, 0)     // urgent ptr

        // copy payload
        val payStart = 40
        for (i in payload.indices) out.put(payStart + i, payload[i])

        // checksums
        putU16(out, 10, ipChecksum(out.array(), 0, respIhl))
        putU16(out, 36, tcpChecksum(out.array(), 12, 16, 20, respTcph, payload))

        return out
    }

    private fun putU16(buf: ByteBuffer, off: Int, v: Int) {
        buf.put(off, ((v ushr 8) and 0xFF).toByte())
        buf.put(off + 1, (v and 0xFF).toByte())
    }
    private fun putU32(buf: ByteBuffer, off: Int, v: Long) {
        buf.put(off, ((v ushr 24) and 0xFF).toByte())
        buf.put(off + 1, ((v ushr 16) and 0xFF).toByte())
        buf.put(off + 2, ((v ushr 8) and 0xFF).toByte())
        buf.put(off + 3, (v and 0xFF).toByte())
    }

    private fun ipChecksum(arr: ByteArray, off: Int, len: Int): Int {
        var sum = 0L
        var i = off
        while (i < off + len) {
            val v = ((arr[i].toInt() and 0xFF) shl 8) or (arr[i + 1].toInt() and 0xFF)
            sum += v
            i += 2
        }
        while ((sum ushr 16) != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    private fun tcpChecksum(
        arr: ByteArray,
        ipSrcOff: Int,
        ipDstOff: Int,
        tcpOff: Int,
        tcpLen: Int,
        payload: ByteArray
    ): Int {
        var sum = 0L
        // pseudo header: src ip
        sum += word(arr, ipSrcOff); sum += word(arr, ipSrcOff + 2)
        // dst ip
        sum += word(arr, ipDstOff); sum += word(arr, ipDstOff + 2)
        sum += 6  // protocol
        sum += tcpLen + payload.size

        // TCP header
        var i = tcpOff
        while (i < tcpOff + tcpLen) { sum += word(arr, i); i += 2 }

        // payload
        i = 0
        while (i + 1 < payload.size) {
            val w = ((payload[i].toInt() and 0xFF) shl 8) or (payload[i + 1].toInt() and 0xFF)
            sum += w
            i += 2
        }
        if (i < payload.size) {
            val w = ((payload[i].toInt() and 0xFF) shl 8)
            sum += w
        }

        while ((sum ushr 16) != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    private fun word(arr: ByteArray, off: Int): Int {
        return ((arr[off].toInt() and 0xFF) shl 8) or (arr[off + 1].toInt() and 0xFF)
    }

    fun buildSynAckResponse(ip: Ipv4, tcp: Tcp, serverSeq: Long): ByteBuffer {
        val respIhl = 20
        val respTcph = 20
        val total = respIhl + respTcph
        val out = ByteBuffer.allocate(total)

        // IPv4 header
        out.put(0, ((4 shl 4) or (respIhl / 4)).toByte()) // version + IHL
        out.put(1, 0) // DSCP/ECN
        putU16(out, 2, total)
        putU16(out, 4, 0) // identification
        putU16(out, 6, 0x4000) // flags + fragment offset (DF set)
        out.put(8, 64.toByte()) // TTL
        out.put(9, 6.toByte()) // TCP protocol

        // swap src/dst IPs
        val srcOct = ip.dst.split(".").map { it.toInt().toByte() }
        val dstOct = ip.src.split(".").map { it.toInt().toByte() }
        out.put(12, srcOct[0]); out.put(13, srcOct[1]); out.put(14, srcOct[2]); out.put(15, srcOct[3])
        out.put(16, dstOct[0]); out.put(17, dstOct[1]); out.put(18, dstOct[2]); out.put(19, dstOct[3])
        putU16(out, 10, 0) // placeholder for IP checksum

        // TCP header
        putU16(out, 20, tcp.dstPort) // src port = original dst
        putU16(out, 22, tcp.srcPort) // dst port = original src

        // Server sequence number (new random)
        putU32(out, 24, serverSeq and 0xFFFFFFFFL)

        // Ack = client's seq + 1 for SYN
        val clientAck = (tcp.seq + 1) and 0xFFFFFFFFL
        putU32(out, 28, clientAck)

        // Data offset (5 words = 20 bytes header)
        out.put(32, ((respTcph / 4) shl 4).toByte())

        // Flags: SYN (0x02) + ACK (0x10)
        out.put(33, (0x12).toByte())

        putU16(out, 34, 65535) // window size
        putU16(out, 36, 0)     // TCP checksum placeholder
        putU16(out, 38, 0)     // urgent pointer

        // Checksums
        putU16(out, 10, ipChecksum(out.array(), 0, respIhl))
        putU16(out, 36, tcpChecksum(out.array(), 12, 16, 20, respTcph, ByteArray(0)))

        return out
    }

}
