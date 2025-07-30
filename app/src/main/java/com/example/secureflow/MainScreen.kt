package com.example.secureflow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.secureflow.api.ApiClient
import com.example.secureflow.net.SimpleVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var statusMessage by remember { mutableStateOf("SecureFlow Dashboard") }

    // API client
    val api = remember { ApiClient.build(context, "http://172.104.236.190:80") }


    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = Intent(context, SimpleVpnService::class.java)
            ContextCompat.startForegroundService(context, intent)
            statusMessage = "✅ VPN started"
        } else {
            statusMessage = "❌ VPN permission denied"
            Toast.makeText(context, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(statusMessage)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Ping backend before starting VPN
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val resp = api.ping()
                    if (resp["status"] == "alive") {
                        statusMessage = "✅ Backend alive, requesting VPN permission"
                        val prepareIntent = VpnService.prepare(context)
                        if (prepareIntent != null) {
                            vpnLauncher.launch(prepareIntent)
                        } else {
                            val intent = Intent(context, SimpleVpnService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                            statusMessage = "✅ VPN started"
                        }
                    } else {
                        statusMessage = "❌ API unavailable"
                    }
                } catch (e: Exception) {
                    statusMessage = "❌ API check failed: ${e.message}"
                }
            }
        }) {
            Text("Start Securing Browsing")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("(More features coming soon...)")
    }
}
