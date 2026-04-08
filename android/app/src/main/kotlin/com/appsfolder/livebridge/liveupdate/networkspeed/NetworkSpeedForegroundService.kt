package com.appsfolder.livebridge.liveupdate.networkspeed

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.appsfolder.livebridge.liveupdate.ConverterPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkSpeedForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var prefs: ConverterPrefs
    private lateinit var notificationBuilder: NetworkSpeedNotificationBuilder
    private lateinit var notificationManager: NotificationManagerCompat
    private var dataSource: NetworkSpeedDataSource? = null
    private var monitoringJob: Job? = null
    private var lastTotalRxBytes: Long = 0L
    private var lastTotalTxBytes: Long = 0L
    private var lastSampleAtMs: Long = 0L
    private var latestSample = NetworkSpeedSample()

    override fun onCreate() {
        super.onCreate()
        prefs = ConverterPrefs(applicationContext)
        notificationBuilder = NetworkSpeedNotificationBuilder(applicationContext)
        notificationManager = NotificationManagerCompat.from(applicationContext)
        dataSource = NetworkSpeedDataSource(applicationContext)
        notificationBuilder.ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!NetworkSpeedController.shouldRun(applicationContext, prefs)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundCompat(
            notificationBuilder.build(
                sample = latestSample,
                settings = prefs.getNetworkSpeedSettings(),
            ),
        )
        startMonitoringIfNeeded()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitoringJob?.cancel()
        monitoringJob = null
        dataSource?.close()
        dataSource = null
        serviceScope.cancel()
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: Notification) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            }

            else -> {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun startMonitoringIfNeeded() {
        if (monitoringJob?.isActive == true) {
            return
        }

        monitoringJob = serviceScope.launch {
            while (isActive) {
                if (!NetworkSpeedController.shouldRun(applicationContext, prefs)) {
                    withContext(Dispatchers.Main) {
                        stopSelf()
                    }
                    return@launch
                }

                val startedAtMs = SystemClock.elapsedRealtime()
                val trafficData = dataSource?.getTrafficData() ?: NetworkTrafficData(0L, 0L)
                val nowMs = SystemClock.elapsedRealtime()

                if (lastSampleAtMs != 0L) {
                    val deltaMs = nowMs - lastSampleAtMs
                    val downloadDelta = trafficData.rxBytes - lastTotalRxBytes
                    val uploadDelta = trafficData.txBytes - lastTotalTxBytes

                    if (deltaMs > 0L) {
                        latestSample = NetworkSpeedSample(
                            downloadBytesPerSecond = ((downloadDelta * 1000L) / deltaMs)
                                .coerceAtLeast(0L),
                            uploadBytesPerSecond = ((uploadDelta * 1000L) / deltaMs)
                                .coerceAtLeast(0L),
                        )
                        publishCurrentNotification()
                    }
                }

                lastTotalRxBytes = trafficData.rxBytes
                lastTotalTxBytes = trafficData.txBytes
                lastSampleAtMs = nowMs

                val elapsedMs = SystemClock.elapsedRealtime() - startedAtMs
                delay((POLL_INTERVAL_MS - elapsedMs).coerceAtLeast(0L))
            }
        }
    }

    private fun publishCurrentNotification() {
        notificationManager.notify(
            NOTIFICATION_ID,
            notificationBuilder.build(
                sample = latestSample,
                settings = prefs.getNetworkSpeedSettings(),
            ),
        )
    }

    companion object {
        private const val POLL_INTERVAL_MS = 1500L
        const val NOTIFICATION_ID = 45100
    }
}
