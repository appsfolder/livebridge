package com.appsfolder.livebridge.liveupdate.networkspeed

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.appsfolder.livebridge.MainActivity
import com.appsfolder.livebridge.R

class NetworkSpeedNotificationBuilder(
    private val context: Context,
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val group = NotificationChannelGroup(CHANNEL_ID, localizedText().channelName)
        notificationManager.createNotificationChannelGroup(group)

        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            localizedText().channelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = localizedText().channelDescription
            setShowBadge(false)
            setGroup(CHANNEL_ID)
            setSound(null, null)
        }

        notificationManager.createNotificationChannel(channel)
    }

    fun build(
        sample: NetworkSpeedSample,
        settings: NetworkSpeedSettings,
    ): Notification {
        ensureChannel()

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = buildContentText(sample, settings)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        val statusText = NetworkSpeedFormatter.formatChipText(
            sample.totalBytesPerSecond,
            settings.unitSelection,
        )

        return builder
            .setContentTitle(localizedText().notificationTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_speed)
            .setColor(PROGRESS_COLOR)
            .setShortCriticalText(statusText)
            .setRequestPromotedOngoing(true)
            .build()
    }

    private fun buildContentText(
        sample: NetworkSpeedSample,
        settings: NetworkSpeedSettings,
    ): String {
        val uploadText =
            "${settings.uploadPrefix}${NetworkSpeedFormatter.formatLine(sample.uploadBytesPerSecond, settings.unitSelection)}"
        val downloadText =
            "${settings.downloadPrefix}${NetworkSpeedFormatter.formatLine(sample.downloadBytesPerSecond, settings.unitSelection)}"

        return when (settings.displayMode) {
            NetworkSpeedDisplayMode.UPLOAD_ONLY -> uploadText
            NetworkSpeedDisplayMode.DOWNLOAD_ONLY -> downloadText
            NetworkSpeedDisplayMode.TOTAL ->
                if (settings.prioritizeUploadSpeed) {
                    "$uploadText  $downloadText"
                } else {
                    "$downloadText  $uploadText"
                }
        }
    }

    private fun localizedText(): LocalizedText {
        return if (isRussianLocale()) {
            LocalizedText(
                notificationTitle = "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0430",
                channelName = "\u041c\u043e\u043d\u0438\u0442\u043e\u0440 \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u0438 \u0441\u0435\u0442\u0438",
                channelDescription = "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0441\u0435\u0442\u0438 \u0432 \u0441\u0442\u0430\u0442\u0443\u0441 \u0431\u0430\u0440\u0435",
            )
        } else {
            LocalizedText(
                notificationTitle = "Network speed",
                channelName = "Network Monitor Service",
                channelDescription = "Shows real-time network speed in status bar",
            )
        }
    }

    private fun isRussianLocale(): Boolean {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return locale?.language?.startsWith("ru", ignoreCase = true) == true
    }

    private data class LocalizedText(
        val notificationTitle: String,
        val channelName: String,
        val channelDescription: String,
    )

    companion object {
        const val CHANNEL_ID = "net_monitor_silent"
        private const val PROGRESS_COLOR = 0xFF0F766E.toInt()
    }
}
