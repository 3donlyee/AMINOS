package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.adb.AmInoSPairingService
import com.example.data.AmInoSManager
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SharinganRed
import com.example.ui.theme.StatusRunning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WirelessPairingDialog(
    manager: AmInoSManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var portText by remember { mutableStateOf("41095") }
    var codeText by remember { mutableStateOf("") }
    var isPairing by remember { mutableStateOf(false) }
    var pairingSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notificationSent by remember { mutableStateOf(false) }

    // Launcher for POST_NOTIFICATIONS permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val serviceIntent = AmInoSPairingService.startSearchIntent(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            notificationSent = true
            Toast.makeText(context, "Pairing notification shown in status bar!", Toast.LENGTH_SHORT).show()
        }
    }

    // Automatically trigger notification search service when dialog opens
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                val serviceIntent = AmInoSPairingService.startSearchIntent(context)
                context.startForegroundService(serviceIntent)
                notificationSent = true
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            val serviceIntent = AmInoSPairingService.startSearchIntent(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            notificationSent = true
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isPairing) onDismiss() },
        containerColor = Color(0xFF1E1E26),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFE2E2EA),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SharinganRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = SharinganRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Pair with Wireless Debugging",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Enable 'Wireless debugging' in Developer options.\n2. Tap 'Pair device with pairing code'.\n3. Enter the 6-digit code in notification or directly below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCCCCCC)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open Developer options directly", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.DeveloperMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dev Options", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                val serviceIntent = AmInoSPairingService.startSearchIntent(context)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                                notificationSent = true
                                Toast.makeText(context, "Pairing notification created!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = SharinganRed
                        )
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Notif", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { if (it.length <= 5) portText = it.filter { c -> c.isDigit() } },
                        label = { Text("Port", color = Color(0xFFAAAAAA)) },
                        placeholder = { Text("e.g. 41095", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SharinganRed,
                            unfocusedBorderColor = Color(0xFF444455),
                            focusedContainerColor = Color(0xFF14141A),
                            unfocusedContainerColor = Color(0xFF14141A)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pairing_port_input")
                    )

                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { if (it.length <= 6) codeText = it.filter { c -> c.isDigit() } },
                        label = { Text("Pairing Code", color = Color(0xFFAAAAAA)) },
                        placeholder = { Text("6 digits", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SharinganRed,
                            unfocusedBorderColor = Color(0xFF444455),
                            focusedContainerColor = Color(0xFF14141A),
                            unfocusedContainerColor = Color(0xFF14141A)
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("pairing_code_input")
                    )
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(visible = pairingSuccess) {
                    Surface(
                        color = StatusRunning.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusRunning)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Paired successfully! Starting AmInoS...",
                                color = StatusRunning,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull()
                    if (port == null || port !in 1024..65535) {
                        errorMessage = "Please enter a valid port (1024 - 65535)"
                        return@Button
                    }
                    if (codeText.length < 6) {
                        errorMessage = "Please enter 6-digit pairing code"
                        return@Button
                    }

                    errorMessage = null
                    isPairing = true
                    scope.launch {
                        manager.addLog("Wireless pairing with 127.0.0.1:$port, code: $codeText")

                        // Send intent to PairingService to trigger pairing notification and execution
                        val pairIntent = Intent(context, AmInoSPairingService::class.java).apply {
                            action = AmInoSPairingService.ACTION_PAIR_CODE
                            putExtra(AmInoSPairingService.EXTRA_HOST, "127.0.0.1")
                            putExtra(AmInoSPairingService.EXTRA_PORT, port)
                            putExtra("code", codeText)
                        }
                        context.startService(pairIntent)

                        delay(1200)
                        pairingSuccess = true
                        delay(600)
                        manager.startWithWireless(port)
                        isPairing = false
                        Toast.makeText(context, "AmInoS paired and running!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                enabled = !isPairing && !pairingSuccess,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SharinganRed,
                    disabledContainerColor = SharinganRed.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_pair_button")
            ) {
                if (isPairing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pairing…", color = Color.White)
                } else {
                    Text("Pair & Start", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isPairing
            ) {
                Text("Cancel", color = Color(0xFFAAAAAA))
            }
        }
    )
}
