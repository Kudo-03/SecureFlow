package com.example.secureflow.api




data class TcpForwardResponse(
    val payload_b64: String,
    val closed: Boolean
)

data class FeaturesRequest(val features: List<Float>)
data class FeaturesResponse(val threat: Boolean, val confidence: Float)
