package com.example.secureflow

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
                }
            }
        }
    }
}

@Composable
fun SecureFlowApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    SecureFlowNavGraph(navController = navController, authViewModel = authViewModel)
}
