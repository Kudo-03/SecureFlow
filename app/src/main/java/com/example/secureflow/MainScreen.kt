package com.example.secureflow

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.secureflow.vpn.SimpleVpnService


@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("SecureFlow: stopped") }
    var busy by remember { mutableStateOf(false) }

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        busy = false
        if (res.resultCode == Activity.RESULT_OK) {
            val svc = Intent(context, SimpleVpnService::class.java)
            ContextCompat.startForegroundService(context, svc)
            status = "SecureFlow: running"
        } else {
            status = "SecureFlow: permission denied"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(status)

        Button(
            enabled = !busy,
            onClick = {
                busy = true
                val prep = VpnService.prepare(context)
                if (prep != null) {
                    vpnLauncher.launch(prep)
                } else {
                    val svc = Intent(context, SimpleVpnService::class.java)
                    ContextCompat.startForegroundService(context, svc)
                    status = "SecureFlow: running"
                    busy = false
                }
            }
        ) { Text(if (busy) "Starting..." else "Start VPN") }

        Button(
            onClick = {
                context.stopService(Intent(context, SimpleVpnService::class.java))
                status = "SecureFlow: stopped"
            }
        ) { Text("Stop VPN") }
    }
}

