package com.example.secureflow


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("About SecureFlow", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Divider()
        Text("Version: 1.0.0")
        Text("SecureFlow demonstrates an on-device VPN service with anomaly visualization and a Firebase-backed auth flow.")
        Text("Made for class demo. 🙂")
    }
}
