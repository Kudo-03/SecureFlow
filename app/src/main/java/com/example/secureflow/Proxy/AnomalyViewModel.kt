package com.example.secureflow.Proxy

import Anomaly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AnomalyViewModel : ViewModel() {

    private val repository = ProxyRepository()

    private val _anomalies = MutableStateFlow<List<Anomaly>>(emptyList())
    val anomalies: StateFlow<List<Anomaly>> = _anomalies

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentUserId: String? = null

    /** Called once from UI when user changes */
    fun loadUserAnomalies(userId: String) {
        currentUserId = userId
        startLiveUpdates()
    }

    /** Continuously pulls anomalies every 1 s */
    private fun startLiveUpdates() {
        viewModelScope.launch {
            _isLoading.value = true
            while (isActive && currentUserId != null) {
                try {
                    val list = repository.getUserAnomalies(currentUserId!!)
                    _anomalies.value = list.takeLast(100) // keep history small
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                _isLoading.value = false
                delay(1000) // refresh every second
            }
        }
    }

    fun refreshHealth(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val health = repository.getHealth()
            onResult(health?.status ?: "error")
        }
    }
}
