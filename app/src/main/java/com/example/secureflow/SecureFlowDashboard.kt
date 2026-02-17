package com.example.secureflow

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.secureflow.FireBase.AuthViewModel
import com.example.secureflow.Proxy.AnomalyViewModel
import com.example.secureflow.ui.sections.AnomalySection

@Composable
fun SecureFlowDashboard(
    authViewModel: AuthViewModel = viewModel(),
    anomalyViewModel: AnomalyViewModel = viewModel(),
    onAccountClick: () -> Unit,
    onInfoClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    var vpnRunning by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    // Logged-in user info
    var displayName by remember { mutableStateOf("User") }
    val userId = authViewModel.getUserId() ?: "anonymous"

    // Load display name
    LaunchedEffect(Unit) {
        authViewModel.getUserName { name ->
            displayName = name ?: "User"
        }
    }

    // Launcher for VPN permission
    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        busy = false
        if (res.resultCode == Activity.RESULT_OK) {
            val svc = Intent(context, MyVpnService::class.java).apply {
                putExtra("USER_ID", userId)
            }
            ContextCompat.startForegroundService(context, svc)
            vpnRunning = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(horizontal = 16.dp)
            .padding(top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // --- Top Banner with Dropdown Menu ---
        item {
            var expanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1565C0))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AsyncImage(
                        model = "https://i.pravatar.cc/150?u=${userId}",
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { expanded = true },
                        contentScale = ContentScale.Crop
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("👤 Account Info") },
                            onClick = {
                                expanded = false
                                onAccountClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ℹ️ App Info") },
                            onClick = {
                                expanded = false
                                onInfoClick()
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "🚪 Log Out",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                expanded = false
                                onLogoutClick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(0.8f))
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // --- VPN Control Section ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "VPN Control",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    LargeVpnSwitch(
                        isOn = vpnRunning,
                        onToggle = {
                            if (!busy) {
                                busy = true
                                if (!vpnRunning) {
                                    val prep = VpnService.prepare(context)
                                    if (prep != null) {
                                        vpnLauncher.launch(prep)
                                    } else {
                                        val svc = Intent(context, MyVpnService::class.java).apply {
                                            putExtra("USER_ID", userId)
                                        }
                                        ContextCompat.startForegroundService(context, svc)
                                        vpnRunning = true
                                        busy = false
                                    }
                                } else {
                                    context.stopService(Intent(context, MyVpnService::class.java))
                                    vpnRunning = false
                                    busy = false
                                }
                            }
                        }
                    )

                    Text(
                        text = if (vpnRunning) "SecureFlow is running" else "SecureFlow is stopped",
                        color = if (vpnRunning) Color(0xFF00C853) else Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- Recent Anomalies Section ---
        item {
            AnomalySection(viewModel = anomalyViewModel, userId = userId)
        }
    }
}

// ────────────────────────────────
//  Custom Large VPN Switch
// ────────────────────────────────
@Composable
fun LargeVpnSwitch(isOn: Boolean, onToggle: () -> Unit) {
    val switchWidth = 140.dp
    val switchHeight = 60.dp
    val knobSize = 52.dp

    val backgroundColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFF00C853) else Color.LightGray,
        label = "bgColor"
    )

    val knobOffset by animateDpAsState(
        targetValue = if (isOn) (switchWidth - knobSize - 8.dp) else 8.dp,
        label = "offset"
    )

    Box(
        modifier = Modifier
            .width(switchWidth)
            .height(switchHeight)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable { onToggle() },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFCCCCCC), CircleShape)
        )
    }
}
