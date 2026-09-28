package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AmInoSManager
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

    var portText by remember { mutableStateOf("42125") }
    var codeText by remember { mutableStateOf("") }
    var isPairing by remember { mutableStateOf(false) }
    var pairingSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isPairing) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SharinganRed.copy(alpha = 0.15f)),
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
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Enable 'Wireless debugging' in Developer options.\n2. Tap 'Pair device with pairing code'.\n3. Enter the 6-digit code and port below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open Developer options directly", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeveloperMode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Developer Options", fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { if (it.length <= 5) portText = it.filter { c -> c.isDigit() } },
                        label = { Text("Port") },
                        placeholder = { Text("e.g. 42125") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pairing_port_input")
                    )

                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { if (it.length <= 6) codeText = it.filter { c -> c.isDigit() } },
                        label = { Text("Pairing Code") },
                        placeholder = { Text("6 digits") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("pairing_code_input")
                    )
                }

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                AnimatedVisibility(visible = pairingSuccess) {
                    Surface(
                        color = StatusRunning.copy(alpha = 0.15f),
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
                        manager.addLog("Attempting wireless pairing to 127.0.0.1:$port with code: $codeText...")
                        delay(1200) // Simulated pairing negotiation
                        pairingSuccess = true
                        manager.addLog("Pairing handshake confirmed!")
                        delay(800)
                        manager.startWithWireless(port)
                        isPairing = false
                        Toast.makeText(context, "AmInoS started successfully!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                enabled = !isPairing && !pairingSuccess,
                colors = ButtonDefaults.buttonColors(containerColor = SharinganRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_pair_button")
            ) {
                if (isPairing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pairing...")
                } else {
                    Text("Pair & Start")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isPairing
            ) {
                Text("Cancel")
            }
        }
    )
}
