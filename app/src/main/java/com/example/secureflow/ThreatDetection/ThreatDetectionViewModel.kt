package com.example.secureflow.ThreatDetection

import com.example.secureflow.ThreatDetection.ThreatDetector
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.secureflow.Cohere.CohereExample
import com.example.secureflow.Cohere.CohereRequest
import com.example.secureflow.Cohere.CohereResponse
import com.example.secureflow.Cohere.CohereService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ThreatDetectionViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext
    private val detector = ThreatDetector(context)

    private val _threatScore = MutableStateFlow<Float?>(null)
    val threatScore: StateFlow<Float?> = _threatScore

    private val _coherePrediction = MutableStateFlow<String>("")
    val coherePrediction: StateFlow<String> = _coherePrediction

    /**
     * Analyze traffic using TFLite.
     */
    fun analyzeTraffic(features: FloatArray) {
        viewModelScope.launch {
            val result = detector.predict(features)
            _threatScore.value = result
        }
    }

    /**
     * Analyze domain using Cohere API.
     */
    fun classifyDomain(domain: String) {
        val examples = listOf(
            CohereExample("google.com", "benign"),
            CohereExample("ads.example.com", "tracking"),
            CohereExample("phishingsite.net", "malicious"),
            CohereExample("secure.bank.com", "benign"),
            CohereExample("clickbait.io", "malicious")
        )

        val request = CohereRequest(
            inputs = listOf(domain),
            examples = examples
        )

        CohereService.api.classifyDomain(request).enqueue(object : Callback<CohereResponse> {
            override fun onResponse(
                call: Call<CohereResponse>,
                response: Response<CohereResponse>
            ) {
                val prediction = response.body()?.classifications?.firstOrNull()
                _coherePrediction.value = prediction?.prediction ?: "Unknown"
            }

            override fun onFailure(call: Call<CohereResponse>, t: Throwable) {
                _coherePrediction.value = "Error: ${t.localizedMessage}"
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}