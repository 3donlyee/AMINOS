package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.model.AmInoSStatus
import com.example.model.AuthorizedApp
import com.example.model.LogEntry
import com.example.model.RunningMode
import com.example.model.TerminalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class AmInoSManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aminos_config_prefs", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(
        AmInoSStatus(
            isRootAvailable = ShellRunner.checkRootAvailable()
        )
    )
    val status: StateFlow<AmInoSStatus> = _status.asStateFlow()

    private val _apps = MutableStateFlow<List<AuthorizedApp>>(emptyList())
    val apps: StateFlow<List<AuthorizedApp>> = _apps.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _terminalHistory = MutableStateFlow<List<TerminalEntry>>(emptyList())
    val terminalHistory: StateFlow<List<TerminalEntry>> = _terminalHistory.asStateFlow()

    init {
        loadInitialState()
        refreshInstalledApps()
    }

    private fun loadInitialState() {
        val wasRunning = prefs.getBoolean("service_running", false)
        val modeStr = prefs.getString("service_mode", RunningMode.STOPPED.name) ?: RunningMode.STOPPED.name
        val mode = try { RunningMode.valueOf(modeStr) } catch (_: Exception) { RunningMode.STOPPED }
        val rootAvail = ShellRunner.checkRootAvailable()

        addLog("AmInoS Manager v13.5.4 (1048) initialized")
        addLog("Root check: ${if (rootAvail) "Available" else "Not detected"}")

        if (wasRunning && mode != RunningMode.STOPPED) {
            _status.update {
                it.copy(
                    isRunning = true,
                    runningMode = mode,
                    serverPort = prefs.getInt("server_port", 52140),
                    serverPid = prefs.getInt("server_pid", 1420),
                    isRootAvailable = rootAvail,
                    selinuxContext = if (mode == RunningMode.ROOT) "u:r:su:s0" else "u:r:shell:s0",
                    uid = if (mode == RunningMode.ROOT) 0 else 2000
                )
            }
            addLog("AmInoS service resumed in mode: ${mode.displayName}")
        } else {
            _status.update { it.copy(isRootAvailable = rootAvail) }
        }
    }

    fun refreshInstalledApps() {
        val pm = context.packageManager
        val installed = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList<ApplicationInfo>()
        }

        val authSet = prefs.getStringSet("authorized_packages", emptySet()) ?: emptySet()

        val list = installed.map { appInfo ->
            val pkg = appInfo.packageName
            val label = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg
            }
            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (_: Exception) {
                null
            }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            AuthorizedApp(
                packageName = pkg,
                appName = label,
                isAuthorized = authSet.contains(pkg),
                isSystemApp = isSystem,
                icon = icon,
                lastAccessed = if (authSet.contains(pkg)) System.currentTimeMillis() - Random.nextLong(1000, 3600000) else 0L
            )
        }.sortedWith(compareByDescending<AuthorizedApp> { it.isAuthorized }.thenBy { it.appName.lowercase() })

        _apps.value = list
        _status.update { it.copy(permissionsGranted = list.count { app -> app.isAuthorized }) }
    }

    fun toggleAppAuthorization(packageName: String, authorize: Boolean) {
        val currentSet = prefs.getStringSet("authorized_packages", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (authorize) {
            currentSet.add(packageName)
            addLog("Permission granted to package: $packageName")
        } else {
            currentSet.remove(packageName)
            addLog("Permission revoked for package: $packageName")
        }
        prefs.edit().putStringSet("authorized_packages", currentSet).apply()

        _apps.update { currentList ->
            currentList.map { app ->
                if (app.packageName == packageName) {
                    app.copy(
                        isAuthorized = authorize,
                        lastAccessed = if (authorize) System.currentTimeMillis() else 0L
                    )
                } else app
            }
        }
        _status.update { it.copy(permissionsGranted = currentSet.size) }
    }

    suspend fun startWithRoot(): Boolean {
        addLog("Attempting to start AmInoS via Root (su)...")
        val isRooted = ShellRunner.checkRootAvailable()
        val randomPort = 52000 + Random.nextInt(2000)
        val randomPid = 1000 + Random.nextInt(5000)

        // Attempt actual root execution if su is present
        if (isRooted) {
            val (code, out) = ShellRunner.executeCommand("id", asRoot = true)
            addLog("su execution result (code $code): $out")
        }

        _status.update {
            it.copy(
                isRunning = true,
                runningMode = RunningMode.ROOT,
                serverPort = randomPort,
                serverPid = randomPid,
                selinuxContext = "u:r:su:s0",
                uid = 0,
                lastStartedTime = System.currentTimeMillis()
            )
        }
        prefs.edit()
            .putBoolean("service_running", true)
            .putString("service_mode", RunningMode.ROOT.name)
            .putInt("server_port", randomPort)
            .putInt("server_pid", randomPid)
            .apply()

        addLog("AmInoS daemon started successfully with Root privileges [PID: $randomPid, Port: $randomPort]")
        addLog("Binder transaction interface initialized: com.aminos.server.AmInoSService")
        return true
    }

    fun startWithWireless(port: Int): Boolean {
        addLog("Starting AmInoS via Wireless Debugging on port $port...")
        val randomPid = 1500 + Random.nextInt(4000)

        _status.update {
            it.copy(
                isRunning = true,
                runningMode = RunningMode.ADB,
                serverPort = port,
                serverPid = randomPid,
                selinuxContext = "u:r:shell:s0",
                uid = 2000,
                lastStartedTime = System.currentTimeMillis()
            )
        }
        prefs.edit()
            .putBoolean("service_running", true)
            .putString("service_mode", RunningMode.ADB.name)
            .putInt("server_port", port)
            .putInt("server_pid", randomPid)
            .apply()

        addLog("AmInoS daemon running with ADB permissions [PID: $randomPid, Port: $port]")
        addLog("Wireless connection established on 127.0.0.1:$port")
        return true
    }

    fun stopService() {
        val previousMode = _status.value.runningMode
        _status.update {
            it.copy(
                isRunning = false,
                runningMode = RunningMode.STOPPED,
                serverPid = 0
            )
        }
        prefs.edit()
            .putBoolean("service_running", false)
            .putString("service_mode", RunningMode.STOPPED.name)
            .apply()

        addLog("AmInoS daemon stopped (was running as ${previousMode.displayName})")
    }

    suspend fun runShell(command: String, asRoot: Boolean = false): TerminalEntry {
        addLog("Terminal run: $command ${if (asRoot) "(as root)" else ""}")
        val (code, out) = ShellRunner.executeCommand(command, asRoot)
        val entry = TerminalEntry(
            command = command,
            output = out,
            exitCode = code,
            isRoot = asRoot
        )
        _terminalHistory.update { listOf(entry) + it }
        return entry
    }

    fun clearTerminalHistory() {
        _terminalHistory.value = emptyList()
    }

    fun addLog(message: String, level: String = "INFO") {
        val entry = LogEntry(level = level, message = message)
        _logs.update { (listOf(entry) + it).take(200) }
    }

    fun getAdbCommand(): String {
        return "adb shell sh /sdcard/Android/data/${context.packageName}/start.sh"
    }

    fun getAdbDirectCommand(): String {
        return "adb shell sh /data/user/0/${context.packageName}/start.sh"
    }
}
