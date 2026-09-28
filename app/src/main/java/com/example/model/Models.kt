package com.example.model

import android.graphics.drawable.Drawable

enum class RunningMode(val displayName: String) {
    ADB("ADB (Wireless / USB)"),
    ROOT("Root (su)"),
    STOPPED("Not running")
}

data class AmInoSStatus(
    val isRunning: Boolean = false,
    val version: String = "13.5.4",
    val versionCode: Int = 1048,
    val apiVersion: Int = 16,
    val runningMode: RunningMode = RunningMode.STOPPED,
    val isRootAvailable: Boolean = false,
    val selinuxContext: String = "u:r:shell:s0",
    val serverPort: Int = 52140,
    val serverPid: Int = 0,
    val uid: Int = 2000,
    val permissionsGranted: Int = 0,
    val lastStartedTime: Long = 0L
)

data class AuthorizedApp(
    val packageName: String,
    val appName: String,
    val isAuthorized: Boolean = false,
    val isSystemApp: Boolean = false,
    val icon: Drawable? = null,
    val lastAccessed: Long = 0L,
    val allowedPermissions: List<String> = listOf(
        "android.permission.WRITE_SECURE_SETTINGS",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.INTERACT_ACROSS_USERS"
    )
)

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val tag: String = "AmInoSDaemon",
    val message: String
)

data class TerminalEntry(
    val command: String,
    val output: String,
    val exitCode: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isRoot: Boolean = false
)

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AuthorizedApps : Screen("authorized_apps")
    object Terminal : Screen("terminal")
    object Logs : Screen("logs")
    object Settings : Screen("settings")
    object WirelessPairing : Screen("wireless_pairing")
}
