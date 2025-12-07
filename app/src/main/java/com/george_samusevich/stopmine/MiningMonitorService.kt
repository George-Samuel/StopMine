package com.george_samusevich.stopmine

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class MiningMonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null

    companion object {
        const val ACTION_START_MONITORING = "START_MONITORING"
        const val ACTION_STOP_MONITORING = "STOP_MONITORING"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "mining_monitor_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startForegroundService()
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
            }
            else -> {
                startForegroundService()
                startMonitoring()
            }
        }
        return START_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Для Android 10+ используем startForeground с notification
            startForeground(NOTIFICATION_ID, notification)
        } else {
            // Для старых версий
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mining Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for cryptocurrency mining activities"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mining Monitor")
            .setContentText("Monitoring for mining activities")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Базовая логика мониторинга
                    val suspiciousActivities = detectSuspiciousActivity()

                    if (suspiciousActivities.isNotEmpty()) {
                        sendAlert(suspiciousActivities)
                    }

                    updateNotification(suspiciousActivities.size)
                    delay(30000)

                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(60000)
                }
            }
        }
    }

    private fun detectSuspiciousActivity(): List<String> {
        val suspiciousActivities = mutableListOf<String>()

        // Простая логика детектирования
        if (isPotentialMiningActivity()) {
            suspiciousActivities.add("Potential mining activity detected")
        }

        return suspiciousActivities
    }

    private fun isPotentialMiningActivity(): Boolean {
        // Заглушка для вашей логики детектирования
        return false
    }

    private fun updateNotification(suspiciousCount: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mining Monitor")
            .setContentText(
                if (suspiciousCount > 0)
                    "Found $suspiciousCount suspicious activities"
                else
                    "No threats detected"
            )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setSilent(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendAlert(suspiciousApps: List<String>) {
        val alertIntent = Intent("MINING_ACTIVITY_DETECTED").apply {
            putStringArrayListExtra("suspicious_apps", ArrayList(suspiciousApps))
        }
        sendBroadcast(alertIntent)
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        stopForeground(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        serviceScope.cancel()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}