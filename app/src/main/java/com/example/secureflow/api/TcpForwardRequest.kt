package com.example.secureflow.api

data class TcpForwardRequest(
    val src_ip: String,
    val src_port: Int,
    val dst_ip: String,
    val dst_port: Int,
    val payload_b64: String,
    val syn: Boolean,
    val fin: Boolean,
    val rst: Boolean
)
