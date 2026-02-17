package com.example.secureflow

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.secureflow.FireBase.AuthViewModel
import com.example.secureflow.FireBase.LoginPage
import com.example.secureflow.FireBase.SignupPage
import com.example.secureflow.Proxy.AnomalyViewModel

@Composable
fun SecureFlowNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val anomalyViewModel: AnomalyViewModel = viewModel()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = modifier.padding(innerPadding) // ✅ Use Scaffold padding here
        ) {
            // LOGIN PAGE
            composable("login") {
                LoginPage(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }

            // SIGNUP PAGE
            composable("signup") {
                SignupPage(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }

            // HOME / DASHBOARD
            composable("home") {
                SecureFlowDashboard(
                    authViewModel = authViewModel,
                    anomalyViewModel = anomalyViewModel,
                    onAccountClick = { navController.navigate("account") },
                    onInfoClick = { navController.navigate("info") },
                    onLogoutClick = {
                        authViewModel.signout()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }

            // ACCOUNT PAGE
            composable("account") {
                AccountPage(authViewModel = authViewModel)
            }

            // INFO PAGE
            composable("info") {
                AboutPage()
            }
        }
    }
}
