package com.appsfolder.livebridge.liveupdate

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.NotificationManagerCompat
import com.appsfolder.livebridge.liveupdate.networkspeed.NetworkSpeedController

class LiveBridgeTileService : TileService() {
    private val prefs by lazy { ConverterPrefs(applicationContext) }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { toggleConverter() }
        } else {
            toggleConverter()
        }
    }

    private fun toggleConverter() {
        val newValue = !prefs.getConverterEnabled()
        prefs.setConverterEnabled(newValue)
        NetworkSpeedController.sync(applicationContext, prefs)
        if (!newValue) {
            LiveUpdateNotifier.clearRuntimeState()
            NotificationManagerCompat.from(applicationContext).cancelAll()
        } else {
            requestNotificationListenerRebindIfPossible()
        }
        syncKeepAliveForegroundService()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val enabled = prefs.getConverterEnabled()
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "LiveBridge"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (enabled) {
                if (isRussianLocale()) "Включено" else "Enabled"
            } else {
                if (isRussianLocale()) "Выключено" else "Disabled"
            }
        }
        tile.updateTile()
    }

    private fun syncKeepAliveForegroundService() {
        val shouldRun =
            prefs.getConverterEnabled() &&
                    prefs.getKeepAliveForegroundEnabled() &&
                    isNotificationListenerEnabled() &&
                    (!DeviceBlocker.isBlockedDevice() || prefs.getPixelJokeBypassEnabled())
        if (shouldRun) {
            KeepAliveForegroundService.start(applicationContext)
        } else {
            KeepAliveForegroundService.stop(applicationContext)
        }
    }

    private fun requestNotificationListenerRebindIfPossible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return
        }
        if (!isNotificationListenerEnabled()) {
            return
        }
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(applicationContext, LiveUpdateNotificationListenerService::class.java)
            )
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val service = ComponentName(applicationContext, LiveUpdateNotificationListenerService::class.java)
        return enabled.split(":")
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == service }
    }

    private fun isRussianLocale(): Boolean {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
        return locale?.language?.startsWith("ru", ignoreCase = true) == true
    }

    companion object {
        fun requestStateSync(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return
            }
            runCatching {
                requestListeningState(
                    context,
                    ComponentName(context, LiveBridgeTileService::class.java)
                )
            }
        }
    }
}
