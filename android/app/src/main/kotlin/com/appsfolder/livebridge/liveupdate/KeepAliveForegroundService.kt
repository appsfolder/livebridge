package com.appsfolder.livebridge.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.appsfolder.livebridge.MainActivity
import com.appsfolder.livebridge.R
import java.util.Locale

class KeepAliveForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForegroundCompat(notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val text = localizedText(this)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_liveupdate)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "livebridge_keep_alive"
        private const val CHANNEL_NAME = "Background Mode"
        private const val NOTIFICATION_ID = 41130

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, KeepAliveForegroundService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveForegroundService::class.java))
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return
            }
            val text = localizedText(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) {
                return
            }
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    text.channelName,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = text.channelDescription
                    lockscreenVisibility = Notification.VISIBILITY_SECRET
                }
            )
        }

        private fun localizedText(context: Context): LocalizedText {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales.get(0)
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            val isRu = locale?.language?.lowercase(Locale.ROOT)?.startsWith("ru") == true
            return if (isRu) {
                LocalizedText(
                    title = "Фоновый режим",
                    body = "Оставьте это уведомление для стабильной работы LiveBridge",
                    channelName = "Фоновый режим LiveBridge",
                    channelDescription = "Оставьте это уведомление, чтобы LiveBridge работал стабильно"
                )
            } else {
                LocalizedText(
                    title = "Background mode",
                    body = "Keep this notification for LiveBridge stability",
                    channelName = CHANNEL_NAME,
                    channelDescription = "Keep this notification to use LiveBridge"
                )
            }
        }
    }

    private data class LocalizedText(
        val title: String,
        val body: String,
        val channelName: String,
        val channelDescription: String,
    )
}
