package com.example.secureflow.Proxy

import Anomaly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProxyRepository {

    private val api = RetrofitClient.api

    suspend fun getHealth(): HealthResponse? = withContext(Dispatchers.IO) {
        runCatching { api.getHealth() }.getOrNull()
    }

    suspend fun getUserAnomalies(uid: String): List<Anomaly> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getLatestAnomalies(uid)
            println("✅ Received ${response.data.size} anomalies from API")
            response.data
        }.getOrElse {
            it.printStackTrace()
            emptyList()
        }
    }



}