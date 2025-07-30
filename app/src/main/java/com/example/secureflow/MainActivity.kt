package com.example.secureflow

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.example.secureflow.api.ApiClient
import com.example.secureflow.api.SecureFlowApi
import com.example.secureflow.net.SimpleVpnService
import com.example.secureflow.ui.theme.SecureFlowTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var api: SecureFlowApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Init API client with your backend URL
        api = ApiClient.build(this, "http://172.104.236.190:80")


        // First, check if backend is alive
        checkApiAndStartVpn()

        // Load UI
        setContent {
            SecureFlowTheme {
                MainScreen(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    private fun checkApiAndStartVpn() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = api.ping()
                if (resp["status"] == "alive") {
                    Log.d("SecureFlow", "✅ API works: ${resp["status"]}")
                    runOnUiThread { checkVpnPermissionAndStart() }
                } else {
                    Log.e("SecureFlow", "❌ API ping failed: $resp")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "API unavailable", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("SecureFlow", "❌ API failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "API unavailable", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkVpnPermissionAndStart() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        val vpnIntent = Intent(this, SimpleVpnService::class.java)
        startService(vpnIntent)
        Toast.makeText(this, "VPN started", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startVpn()
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val VPN_REQUEST_CODE = 100
    }
}
