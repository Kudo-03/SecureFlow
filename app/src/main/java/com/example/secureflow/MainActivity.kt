package com.example.secureflow

<<<<<<< HEAD

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.secureflow.ui.theme.SecureFlowTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            startVPN() // Already granted
        }

        setContent {
            SecureFlowTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
=======
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.secureflow.FireBase.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    SecureFlowApp()
>>>>>>> try5
                }
            }
        }
    }
<<<<<<< HEAD

    // ✅ This must be outside of onCreate
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVPN()
        }
    }

    private fun startVPN() {
        val vpnIntent = Intent(this, MyVpnService::class.java)
        startService(vpnIntent)
    }

    companion object {
        const val VPN_REQUEST_CODE = 100
    }
=======
}

@Composable
fun SecureFlowApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    SecureFlowNavGraph(navController = navController, authViewModel = authViewModel)
>>>>>>> try5
}
