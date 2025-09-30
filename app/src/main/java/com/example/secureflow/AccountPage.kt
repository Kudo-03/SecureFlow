package com.example.secureflow

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.secureflow.FireBase.AuthViewModel

@Composable
fun AccountPage(
    authViewModel: AuthViewModel = viewModel(),
) {
    val uid = authViewModel.getUserId().orEmpty()
    var name by remember { mutableStateOf<String?>(null) }
    val email = authViewModel.currentUser?.email ?: "—"

    LaunchedEffect(uid) {
        authViewModel.getUserName { fetched -> name = fetched }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Divider()
        Labeled("Name", name ?: "—")
        Labeled("Email", email)
        Labeled("UID", uid)
    }
}

@Composable
private fun Labeled(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
