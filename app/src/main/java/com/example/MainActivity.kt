package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.AmInoSManager
import com.example.model.Screen
import com.example.ui.screens.AuthorizedAppsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.screens.WirelessPairingDialog
import com.example.ui.theme.AmInoSTheme

class MainActivity : ComponentActivity() {

    private lateinit var manager: AmInoSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        manager = AmInoSManager(applicationContext)

        setContent {
            AmInoSTheme(darkTheme = true) { // Authentic dark anime Naruto Sharingan theme
                Surface(modifier = Modifier.fillMaxSize()) {
                    AmInoSApp(manager = manager)
                }
            }
        }
    }
}

@Composable
fun AmInoSApp(manager: AmInoSManager) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showPairingDialog by remember { mutableStateOf(false) }

    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
        when (screen) {
            is Screen.Home -> {
                HomeScreen(
                    manager = manager,
                    onNavigateToApps = { currentScreen = Screen.AuthorizedApps },
                    onNavigateToTerminal = { currentScreen = Screen.Terminal },
                    onNavigateToLogs = { currentScreen = Screen.Logs },
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onOpenPairingDialog = { showPairingDialog = true }
                )
            }
            is Screen.AuthorizedApps -> {
                AuthorizedAppsScreen(
                    manager = manager,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
            is Screen.Terminal -> {
                TerminalScreen(
                    manager = manager,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
            is Screen.Logs -> {
                LogsScreen(
                    manager = manager,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
            else -> {
                HomeScreen(
                    manager = manager,
                    onNavigateToApps = { currentScreen = Screen.AuthorizedApps },
                    onNavigateToTerminal = { currentScreen = Screen.Terminal },
                    onNavigateToLogs = { currentScreen = Screen.Logs },
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onOpenPairingDialog = { showPairingDialog = true }
                )
            }
        }
    }

    if (showPairingDialog) {
        WirelessPairingDialog(
            manager = manager,
            onDismiss = { showPairingDialog = false }
        )
    }
}
