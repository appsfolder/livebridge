package com.appsfolder.livebridge.liveupdate.networkspeed

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.appsfolder.livebridge.liveupdate.ConverterPrefs
import com.appsfolder.livebridge.liveupdate.DeviceBlocker
import com.appsfolder.livebridge.liveupdate.LiveUpdateNotificationListenerService

object NetworkSpeedController {
    fun sync(
        context: Context,
        prefs: ConverterPrefs = ConverterPrefs(context),
    ) {
        if (shouldRun(context, prefs)) {
            start(context)
        } else {
            stop(context)
        }
    }

    fun start(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, NetworkSpeedForegroundService::class.java),
        )
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, NetworkSpeedForegroundService::class.java))
    }

    fun shouldRun(
        context: Context,
        prefs: ConverterPrefs = ConverterPrefs(context),
    ): Boolean {
        if (!prefs.getConverterEnabled() || !prefs.getNetworkSpeedEnabled()) {
            return false
        }
        if (!isNotificationListenerEnabled(context)) {
            return false
        }
        if (DeviceBlocker.isBlockedDevice() && !prefs.getPixelJokeBypassEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val service = ComponentName(context, LiveUpdateNotificationListenerService::class.java)
        return enabled.split(":")
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == service }
    }
}
