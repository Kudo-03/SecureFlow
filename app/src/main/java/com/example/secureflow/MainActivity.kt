package com.example.secureflow

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.example.secureflow.ui.theme.SecureFlowTheme
import com.example.secureflow.vpn.SimpleVpnService


class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Init API client with your backend URL



        // First, check if backend is alive


        // Load UI
        setContent {
            SecureFlowTheme {
                MainScreen(modifier = Modifier.fillMaxWidth())
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