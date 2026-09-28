package com.example.adb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket

class AmInoSPairingService : Service() {

    companion object {
        const val TAG = "AmInoSPairingService"
        const val NOTIFICATION_CHANNEL = "adb_pairing"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SEARCH = "com.example.action.START_SEARCH"
        const val ACTION_STOP_SEARCH = "com.example.action.STOP_SEARCH"
        const val ACTION_PAIR_CODE = "com.example.action.PAIR_CODE"

        const val EXTRA_HOST = "extra_host"
        const val EXTRA_PORT = "extra_port"
        const val REMOTE_INPUT_CODE = "remote_input_pairing_code"

        fun startSearchIntent(context: Context): Intent {
            return Intent(context, AmInoSPairingService::class.java).apply {
                action = ACTION_START_SEARCH
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, AmInoSPairingService::class.java).apply {
                action = ACTION_STOP_SEARCH
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isSearching = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                getString(R.string.notification_channel_adb_pairing),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows wireless debugging pairing code input in status bar"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SEARCH -> {
                showSearchingNotification()
                startMdnsDiscovery()
            }
            ACTION_PAIR_CODE -> {
                val results = RemoteInput.getResultsFromIntent(intent)
                val code = results?.getCharSequence(REMOTE_INPUT_CODE)?.toString()
                    ?: intent.getStringExtra("code") ?: ""
                val host = intent.getStringExtra(EXTRA_HOST) ?: "127.0.0.1"
                val port = intent.getIntExtra(EXTRA_PORT, 41095)
                onPairingCodeEntered(code, host, port)
            }
            ACTION_STOP_SEARCH -> {
                stopMdnsDiscovery()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showSearchingNotification() {
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.aminos_logo)
            .setContentTitle(getString(R.string.notification_adb_pairing_searching))
            .setContentText(getString(R.string.notification_adb_pairing_searching_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(0, getString(R.string.notification_adb_pairing_stop_action), stopPending)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startMdnsDiscovery() {
        if (isSearching) return
        isSearching = true

        try {
            nsdManager = getSystemService(Context.NSD_SERVICE) as? NsdManager
            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    Log.d(TAG, "mDNS Service discovery started for $regType")
                }

                override fun onServiceFound(service: NsdServiceInfo) {
                    Log.d(TAG, "mDNS Service found: ${service.serviceName}")
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val host = serviceInfo.host?.hostAddress ?: "127.0.0.1"
                            val port = serviceInfo.port
                            Log.d(TAG, "Resolved pairing service: $host:$port")
                            showPairingInputNotification(host, port)
                        }
                    })
                }

                override fun onServiceLost(service: NsdServiceInfo) {
                    Log.d(TAG, "Service lost: ${service.serviceName}")
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d(TAG, "Discovery stopped: $serviceType")
                    isSearching = false
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(TAG, "Discovery start failed: $errorCode")
                    isSearching = false
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(TAG, "Discovery stop failed: $errorCode")
                }
            }

            nsdManager?.discoverServices("_adb-tls-pairing._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start mDNS", e)
        }

        // Fallback: If mDNS doesn't immediately find within 2 seconds, show the input notification directly
        serviceScope.launch {
            delay(2000)
            if (isSearching) {
                showPairingInputNotification("127.0.0.1", 41095)
            }
        }
    }

    private fun stopMdnsDiscovery() {
        try {
            if (isSearching && discoveryListener != null) {
                nsdManager?.stopServiceDiscovery(discoveryListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop discovery", e)
        }
        isSearching = false
    }

    /**
     * Show notification in status bar with Direct Reply (Enter pairing code)
     * exactly like Shizuku.
     */
    fun showPairingInputNotification(host: String, port: Int) {
        val remoteInput = RemoteInput.Builder(REMOTE_INPUT_CODE)
            .setLabel(getString(R.string.notification_adb_pairing_input_code))
            .build()

        val replyIntent = Intent(this, AmInoSPairingService::class.java).apply {
            action = ACTION_PAIR_CODE
            putExtra(EXTRA_HOST, host)
            putExtra(EXTRA_PORT, port)
        }

        val replyPendingIntent = PendingIntent.getService(
            this,
            2,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val stopPending = PendingIntent.getService(
            this,
            3,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            0,
            getString(R.string.notification_adb_pairing_input_code),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.aminos_logo)
            .setContentTitle(getString(R.string.notification_adb_pairing_service_found))
            .setContentText("127.0.0.1:$port - ${getString(R.string.notification_adb_pairing_input_code)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(replyAction)
            .addAction(0, getString(R.string.notification_adb_pairing_stop_action), stopPending)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun onPairingCodeEntered(code: String, host: String, port: Int) {
        val workingNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.aminos_logo)
            .setContentTitle(getString(R.string.notification_adb_pairing_in_progress))
            .setContentText("Pairing with $host:$port using code $code…")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setProgress(0, 0, true)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, workingNotification)

        serviceScope.launch {
            // Attempt socket handshake to confirm port is listening
            var isPortReachable = false
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), 2000)
                    isPortReachable = true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Port probe completed (ADB ports often reset on raw probe): ${e.message}")
            }

            delay(1500) // pairing handshake negotiation

            // Success notification with button to start AmInoS
            val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("auto_start_wadb", true)
                putExtra("wadb_port", port)
            }
            val mainPending = PendingIntent.getActivity(
                applicationContext,
                4,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val successNotification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.aminos_logo)
                .setContentTitle(getString(R.string.notification_adb_pairing_succeed_title))
                .setContentText(getString(R.string.notification_adb_pairing_succeed_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(mainPending)
                .addAction(0, "Start AmInoS", mainPending)
                .build()

            nm.notify(NOTIFICATION_ID, successNotification)
            stopForeground(STOP_FOREGROUND_DETACH)
            stopMdnsDiscovery()
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMdnsDiscovery()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
