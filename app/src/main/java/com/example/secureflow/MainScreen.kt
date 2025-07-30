package com.example.secureflow


import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.secureflow.ThreatDetection.ThreatDetectionViewModel

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val viewModel: ThreatDetectionViewModel = viewModel()
    val context = LocalContext.current
    val threatScore by viewModel.threatScore.collectAsState()
    val cohereResult by viewModel.coherePrediction.collectAsState()

    // Show Toast when score updates
    LaunchedEffect(threatScore) {
        threatScore?.let {
            Toast.makeText(
                context,
                if (it >= 0.5f) "🚨 Suspicious Traffic Detected!" else "✅ Traffic is Safe",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Show Toast when cohere updates
    LaunchedEffect(cohereResult) {
        if (cohereResult.isNotEmpty()) {
            Toast.makeText(
                context,
                "Cohere says: $cohereResult",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("Threat Score (TFLite): ${threatScore?.toString() ?: "Not evaluated"}")
        Text("Cohere Classification: $cohereResult")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            viewModel.analyzeTraffic(floatArrayOf(443f, 1f, 520f, 2.1f, 0.95f, 0.03f))
            viewModel.classifyDomain("ads.tracker-site.com")
        }) {
            Text("Analyze Traffic + Domain")
        }
    }
}
