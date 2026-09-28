package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AmInoSManager
import com.example.ui.components.AuthorizedAppsCard
import com.example.ui.components.NarutoSharinganEye
import com.example.ui.components.SharinganLogoImage
import com.example.ui.components.StartPCCard
import com.example.ui.components.StartRootCard
import com.example.ui.components.StartWirelessCard
import com.example.ui.components.StatusCard
import com.example.ui.theme.CardElevated
import com.example.ui.theme.SharinganGlow
import com.example.ui.theme.SharinganRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    manager: AmInoSManager,
    onNavigateToApps: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenPairingDialog: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by manager.status.collectAsState()
    val apps by manager.apps.collectAsState()

    var eyeSpinning by remember { mutableStateOf(false) }
    var tomoeCount by remember { mutableIntStateOf(3) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SharinganLogoImage(
                            size = 36.dp,
                            isRotating = status.isRunning,
                            showGlow = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AmInoS",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = SharinganRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Sharingan",
                                        color = SharinganRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "v${status.version} (${status.versionCode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToTerminal,
                        modifier = Modifier.testTag("action_terminal")
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = "Terminal")
                    }
                    IconButton(
                        onClick = onNavigateToLogs,
                        modifier = Modifier.testTag("action_logs")
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = "Logs")
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Sharingan Interactive Anime Hero
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            eyeSpinning = !eyeSpinning
                            tomoeCount = if (tomoeCount == 3) 1 else tomoeCount + 1
                            Toast.makeText(context, "Sharingan awakened: $tomoeCount Tomoe!", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("sharingan_interactive_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Uchiha Sharingan Core",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "System API inspection & privileged binder execution active. Tap eye to cycle tomoe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        NarutoSharinganEye(
                            size = 64.dp,
                            isSpinning = eyeSpinning || status.isRunning,
                            tomoeCount = tomoeCount
                        )
                    }
                }
            }

            // Main Service Status Card
            item {
                StatusCard(
                    status = status,
                    onStopClick = { manager.stopService() },
                    onRestartClick = {
                        scope.launch {
                            manager.stopService()
                            if (status.isRootAvailable) {
                                manager.startWithRoot()
                            } else {
                                manager.startWithWireless(52140)
                            }
                        }
                    },
                    onViewLogsClick = onNavigateToLogs
                )
            }

            // Authorized Applications Card
            item {
                val authorizedCount = apps.count { it.isAuthorized }
                AuthorizedAppsCard(
                    authorizedCount = authorizedCount,
                    totalCount = apps.size,
                    onClick = onNavigateToApps
                )
            }

            // Start via Wireless Debugging Card
            item {
                StartWirelessCard(
                    onPairClick = onOpenPairingDialog,
                    onStartClick = {
                        manager.startWithWireless(52140)
                        Toast.makeText(context, "AmInoS started via Wireless Debugging!", Toast.LENGTH_SHORT).show()
                    },
                    onDeveloperOptionsClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open Developer Settings directly", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Start with Computer Card
            item {
                StartPCCard(adbCommand = manager.getAdbCommand())
            }

            // Start with Root Card
            item {
                StartRootCard(
                    isRootAvailable = status.isRootAvailable,
                    onStartRootClick = {
                        scope.launch {
                            val started = manager.startWithRoot()
                            if (started) {
                                Toast.makeText(context, "AmInoS started with Root permissions!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
