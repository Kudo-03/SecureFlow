package com.example.secureflow.Proxy

data class HealthResponse(    val status: String,
                              val ml_last_err: Double?,
                              val ml_last_thr: Double?,
                              val ema_mu: Double?,
                              val ema_sigma: Double?,
                              val anomalies: Int,
                              val vpn_client: String?,
                              val tun_iface: String?)