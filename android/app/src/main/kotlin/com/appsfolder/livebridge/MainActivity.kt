package com.kakao.taxi

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.kakao.taxi.liveupdate.AppPresentationOverridesCodec
import com.kakao.taxi.liveupdate.AppPresentationOverridesLoader
import com.kakao.taxi.liveupdate.ConverterPrefs
import com.kakao.taxi.liveupdate.DeviceBlocker
import com.kakao.taxi.liveupdate.DeviceProps
import com.kakao.taxi.liveupdate.InstalledAppsRepository
import com.kakao.taxi.liveupdate.KeepAliveForegroundService
import com.kakao.taxi.liveupdate.LiveBridgeTileService
import com.kakao.taxi.liveupdate.LiveParserDictionary
import com.kakao.taxi.liveupdate.LiveParserDictionaryLoader
import com.kakao.taxi.liveupdate.LiveUpdateNotifier
import com.kakao.taxi.liveupdate.LiveUpdateNotificationListenerService
import com.kakao.taxi.liveupdate.NetworkSpeedForegroundService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : FlutterActivity() {
    private var notificationPermissionResult: MethodChannel.Result? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            METHOD_CHANNEL
        ).setMethodCallHandler { call, result ->
            handleMethodCall(call, result)
        }

        val prefs = ConverterPrefs(applicationContext)
        initializeKeepAliveDefaultIfNeeded(prefs)
        syncKeepAliveForegroundService(prefs)
        syncNetworkSpeedService(prefs)
        clearDynamicLauncherShortcuts()
        LiveBridgeTileService.requestStateSync(applicationContext)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            notificationPermissionResult?.success(granted)
            notificationPermissionResult = null
        }
    }

    private fun handleMethodCall(call: MethodCall, res: MethodChannel.Result) {
        val prefs = ConverterPrefs(applicationContext)

        when (call.method) {
            "isDeviceBlocked" -> res.success(
                DeviceBlocker.isBlockedDevice() && !prefs.getPixelJokeBypassEnabled()
            )

            "getPixelJokeBypassEnabled" -> res.success(prefs.getPixelJokeBypassEnabled())
            "setPixelJokeBypassEnabled" -> {
                prefs.setPixelJokeBypassEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "isNotificationListenerEnabled" -> res.success(isNotificationListenerEnabled())
            "requestNotificationListenerRebind" -> res.success(requestNotificationListenerRebind())
            "openNotificationListenerSettings" -> res.success(openNotificationListenerSettings())
            "isNotificationPermissionGranted" -> res.success(isNotificationPermissionGranted())
            "requestNotificationPermission" -> requestNotificationPermission(res)
            "canPostPromotedNotifications" -> res.success(canPostPromotedNotifications())
            "openPromotedNotificationSettings" -> res.success(openPromotedNotificationSettings())
            "openAppNotificationSettings" -> res.success(openAppNotificationSettings())
            "getInstalledApps" -> loadInstalledAppsAsync(
                forceRefresh = call.argument<Boolean>("forceRefresh") ?: false,
                res = res
            )
            "getDeviceInfo" -> res.success(getDeviceInfo())
            "getAppListAccessGranted" -> res.success(prefs.getAppListAccessGranted())
            "setAppListAccessGranted" -> {
                prefs.setAppListAccessGranted(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getBackgroundWarningDismissed" -> res.success(prefs.getBackgroundWarningDismissed())
            "setBackgroundWarningDismissed" -> {
                prefs.setBackgroundWarningDismissed(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getSamsungWarningDismissed" -> res.success(prefs.getSamsungWarningDismissed())
            "setSamsungWarningDismissed" -> {
                prefs.setSamsungWarningDismissed(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getSamsungRemoteReparserEnabled" -> res.success(prefs.getSamsungRemoteReparserEnabled())
            "setSamsungRemoteReparserEnabled" -> {
                prefs.setSamsungRemoteReparserEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "hasExpandedSectionsState" -> res.success(prefs.hasExpandedSectionsState())
            "getExpandedSections" -> res.success(prefs.getExpandedSectionsRaw())
            "setExpandedSections" -> {
                prefs.setExpandedSectionsRaw(call.argument<String>("value"))
                res.success(true)
            }

            "getAppPresentationOverrides" -> res.success(prefs.getAppPresentationOverridesRaw())
            "setAppPresentationOverrides" -> {
                val raw = call.argument<String>("value")
                val normalized = AppPresentationOverridesCodec.normalizeForStorage(raw)
                if (normalized == null) {
                    res.error("invalid_app_overrides", "App overrides JSON is invalid", null)
                    return
                }
                prefs.setAppPresentationOverridesRaw(normalized)
                AppPresentationOverridesLoader.invalidate()
                res.success(true)
            }

            "saveAppPresentationOverridesToDownloads" -> {
                val raw = AppPresentationOverridesCodec.normalizeForDownload(
                    prefs.getAppPresentationOverridesRaw()
                ) ?: run {
                    res.error("invalid_app_overrides", "App overrides JSON is invalid", null)
                    return
                }
                val savedUri = saveJsonToDownloads(
                    raw = raw,
                    filePrefix = "livebridge_app_overrides"
                )
                if (savedUri == null) {
                    res.error("save_failed", "Unable to save app overrides to Downloads", null)
                } else {
                    res.success(savedUri)
                }
            }

            "hasCustomParserDictionary" -> res.success(prefs.hasCustomParserDictionary())
            "getParserDictionaryJson" -> res.success(
                prefs.getCustomParserDictionaryRaw() ?: loadBundledParserDictionaryJson().orEmpty()
            )

            "saveParserDictionaryToDownloads" -> {
                val userRaw = prefs.getCustomParserDictionaryRaw()
                val raw = userRaw ?: loadBundledParserDictionaryJson().orEmpty()
                if (raw.isBlank()) {
                    res.error("dictionary_empty", "Dictionary payload is empty", null)
                    return
                }
                val filePrefix = if (userRaw.isNullOrBlank()) {
                    "livebridge_dictionary"
                } else {
                    "livebridge_user_dictionary"
                }
                val savedUri = saveJsonToDownloads(raw = raw, filePrefix = filePrefix)
                if (savedUri == null) {
                    res.error("save_failed", "Unable to save dictionary to Downloads", null)
                } else {
                    res.success(savedUri)
                }
            }

            "setCustomParserDictionary" -> {
                val raw = call.argument<String>("value")?.trim().orEmpty()
                if (raw.isBlank()) {
                    res.error("invalid_dictionary", "Dictionary payload is empty", null)
                    return
                }
                if (!isValidJsonObject(raw)) {
                    res.error("invalid_dictionary", "Dictionary JSON is invalid", null)
                    return
                }
                prefs.setCustomParserDictionaryRaw(raw)
                LiveParserDictionaryLoader.invalidate()
                res.success(true)
            }

            "clearCustomParserDictionary" -> {
                prefs.clearCustomParserDictionary()
                LiveParserDictionaryLoader.invalidate()
                res.success(true)
            }

            "getPackageRules" -> res.success(prefs.getPackageRulesRaw())
            "setPackageRules" -> {
                prefs.setPackageRulesRaw(call.argument<String>("value"))
                res.success(true)
            }

            "getPackageMode" -> res.success(prefs.getPackageMode())
            "setPackageMode" -> {
                prefs.setPackageMode(call.argument<String>("value"))
                res.success(true)
            }

            "getBypassPackageRules" -> res.success(prefs.getBypassPackageRulesRaw())
            "setBypassPackageRules" -> {
                prefs.setBypassPackageRulesRaw(call.argument<String>("value"))
                res.success(true)
            }

            "getOnlyWithProgress" -> res.success(prefs.getOnlyWithProgress())
            "setOnlyWithProgress" -> {
                prefs.setOnlyWithProgress(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getTextProgressEnabled" -> res.success(prefs.getTextProgressEnabled())
            "setTextProgressEnabled" -> {
                prefs.setTextProgressEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getNetworkSpeedEnabled" -> res.success(prefs.getNetworkSpeedEnabled())
            "setNetworkSpeedEnabled" -> {
                prefs.setNetworkSpeedEnabled(call.argument<Boolean>("value") ?: false)
                syncNetworkSpeedService(prefs)
                res.success(true)
            }

            "getNetworkSpeedDisplayMode" -> res.success(prefs.getNetworkSpeedDisplayMode())
            "setNetworkSpeedDisplayMode" -> {
                prefs.setNetworkSpeedDisplayMode(call.argument<String>("value"))
                syncNetworkSpeedService(prefs)
                res.success(true)
            }

            "getNetworkSpeedUploadPrefix" -> res.success(prefs.getNetworkSpeedUploadPrefix())
            "setNetworkSpeedUploadPrefix" -> {
                prefs.setNetworkSpeedUploadPrefix(call.argument<String>("value"))
                syncNetworkSpeedService(prefs)
                res.success(true)
            }

            "getNetworkSpeedDownloadPrefix" -> res.success(prefs.getNetworkSpeedDownloadPrefix())
            "setNetworkSpeedDownloadPrefix" -> {
                prefs.setNetworkSpeedDownloadPrefix(call.argument<String>("value"))
                syncNetworkSpeedService(prefs)
                res.success(true)
            }

            "getNetworkSpeedUnit" -> res.success(prefs.getNetworkSpeedUnit())
            "setNetworkSpeedUnit" -> {
                prefs.setNetworkSpeedUnit(call.argument<String>("value"))
                syncNetworkSpeedService(prefs)
                res.success(true)
            }

            "getNetworkSpeedPrioritizeUpload" -> res.success(prefs.getNetworkSpeedPrioritizeUpload())
            "setNetworkSpeedPrioritizeUpload" -> {
                prefs.setNetworkSpeedPrioritizeUpload(call.argument<Boolean>("value") ?: false)
                syncNetworkSpeedService(prefs)
                res.success(true)
            }

            "getNetworkSpeedLockscreenOnly" -> res.success(prefs.getNetworkSpeedLockscreenOnly())
            "setNetworkSpeedLockscreenOnly" -> {
                prefs.setNetworkSpeedLockscreenOnly(call.argument<Boolean>("value") ?: false)
                syncNetworkSpeedService(prefs)
                res.success(true)
            }

            "getNetworkSpeedChipBackgroundDisabled" ->
                res.success(prefs.getNetworkSpeedChipBackgroundDisabled())
            "setNetworkSpeedChipBackgroundDisabled" -> {
                prefs.setNetworkSpeedChipBackgroundDisabled(call.argument<Boolean>("value") ?: false)
                syncNetworkSpeedService(prefs)
                res.success(true)
            }

            "getConverterEnabled" -> res.success(prefs.getConverterEnabled())
            "setConverterEnabled" -> {
                val value = call.argument<Boolean>("value") ?: true
                applyConverterEnabled(prefs, value)
                res.success(true)
            }

            "getKeepAliveForegroundEnabled" -> {
                syncKeepAliveForegroundService(prefs)
                res.success(prefs.getKeepAliveForegroundEnabled())
            }

            "setKeepAliveForegroundEnabled" -> {
                val value = call.argument<Boolean>("value") ?: false
                prefs.setKeepAliveForegroundEnabled(value)
                syncKeepAliveForegroundService(prefs)
                res.success(true)
            }

            "getSyncDndEnabled" -> res.success(prefs.getSyncDndEnabled())
            "setSyncDndEnabled" -> {
                prefs.setSyncDndEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getUpdateChecksEnabled" -> res.success(prefs.getUpdateChecksEnabled())
            "setUpdateChecksEnabled" -> {
                prefs.setUpdateChecksEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getUpdateLastCheckAtMs" -> res.success(prefs.getUpdateLastCheckAtMs())
            "setUpdateLastCheckAtMs" -> {
                prefs.setUpdateLastCheckAtMs(call.argument<Number>("value")?.toLong() ?: 0L)
                res.success(true)
            }

            "getUpdateCachedLatestVersion" -> res.success(prefs.getUpdateCachedLatestVersion())
            "setUpdateCachedLatestVersion" -> {
                prefs.setUpdateCachedLatestVersion(call.argument<String>("value"))
                res.success(true)
            }

            "getUpdateCachedAvailable" -> res.success(prefs.getUpdateCachedAvailable())
            "setUpdateCachedAvailable" -> {
                prefs.setUpdateCachedAvailable(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getUpdateLastNotifiedVersion" -> res.success(prefs.getUpdateLastNotifiedVersion())
            "setUpdateLastNotifiedVersion" -> {
                prefs.setUpdateLastNotifiedVersion(call.argument<String>("value"))
                res.success(true)
            }

            "getAppVersionName" -> res.success(getAppVersionName())
            "showUpdateAvailableNotification" -> {
                val version = call.argument<String>("version")?.trim().orEmpty()
                val releaseUrl = call.argument<String>("releaseUrl")?.trim().orEmpty()
                if (version.isEmpty()) {
                    res.success(false)
                } else {
                    res.success(showUpdateAvailableNotification(version, releaseUrl))
                }
            }

            "getAospCuttingEnabled" -> res.success(prefs.getAospCuttingEnabled())
            "setAospCuttingEnabled" -> {
                prefs.setAospCuttingEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getAnimatedIslandEnabled" -> res.success(prefs.getAnimatedIslandEnabled())
            "setAnimatedIslandEnabled" -> {
                prefs.setAnimatedIslandEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getHyperBridgeEnabled" -> res.success(prefs.getHyperBridgeEnabled())
            "setHyperBridgeEnabled" -> {
                prefs.setHyperBridgeEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getSmartStatusDetectionEnabled" -> res.success(prefs.getSmartStatusDetectionEnabled())
            "setSmartStatusDetectionEnabled" -> {
                prefs.setSmartStatusDetectionEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getSmartMediaPlaybackEnabled" -> res.success(prefs.getSmartMediaPlaybackEnabled())
            "setSmartMediaPlaybackEnabled" -> {
                prefs.setSmartMediaPlaybackEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getSmartNavigationEnabled" -> res.success(prefs.getSmartNavigationEnabled())
            "setSmartNavigationEnabled" -> {
                prefs.setSmartNavigationEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getSmartWeatherEnabled" -> res.success(prefs.getSmartWeatherEnabled())
            "setSmartWeatherEnabled" -> {
                prefs.setSmartWeatherEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getSmartExternalDevicesEnabled" -> res.success(prefs.getSmartExternalDevicesEnabled())
            "setSmartExternalDevicesEnabled" -> {
                prefs.setSmartExternalDevicesEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getSmartVpnEnabled" -> res.success(prefs.getSmartVpnEnabled())
            "setSmartVpnEnabled" -> {
                prefs.setSmartVpnEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getOtpDetectionEnabled" -> res.success(prefs.getOtpDetectionEnabled())
            "setOtpDetectionEnabled" -> {
                prefs.setOtpDetectionEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getOtpAutoCopyEnabled" -> res.success(prefs.getOtpAutoCopyEnabled())
            "setOtpAutoCopyEnabled" -> {
                prefs.setOtpAutoCopyEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getOtpPackageRules" -> res.success(prefs.getOtpPackageRulesRaw())
            "setOtpPackageRules" -> {
                prefs.setOtpPackageRulesRaw(call.argument<String>("value"))
                res.success(true)
            }

            "getOtpPackageMode" -> res.success(prefs.getOtpPackageMode())
            "setOtpPackageMode" -> {
                prefs.setOtpPackageMode(call.argument<String>("value"))
                res.success(true)
            }

            else -> res.notImplemented()
        }
    }

    private fun loadBundledParserDictionaryJson(): String? {
        return try {
            assets.open("liveupdate_dictionary.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to read bundled parser dictionary asset", error)
            null
        }
    }

    private fun isValidJsonObject(raw: String): Boolean {
        return runCatching { JSONObject(raw) }.isSuccess
    }

    private fun saveJsonToDownloads(raw: String, filePrefix: String): String? {
        val resolver = contentResolver
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.json"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null

        return try {
            resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
                if (writer == null) {
                    throw IllegalStateException("Unable to open output stream for $uri")
                }
                writer.write(raw)
                writer.flush()
            }

            val publishValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, publishValues, null, null)
            uri.toString()
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to save JSON to Downloads for $filePrefix", error)
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    private fun syncKeepAliveForegroundService(prefs: ConverterPrefs) {
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

    private fun syncNetworkSpeedService(prefs: ConverterPrefs) {
        if (prefs.getNetworkSpeedEnabled()) {
            NetworkSpeedForegroundService.sync(applicationContext)
        } else {
            NetworkSpeedForegroundService.stop(applicationContext)
        }
    }

    private fun initializeKeepAliveDefaultIfNeeded(prefs: ConverterPrefs) {
        if (prefs.hasKeepAliveForegroundPreference()) {
            return
        }
        if (isLikelyChineseDevice()) {
            prefs.setKeepAliveForegroundEnabled(true)
        }
    }

    private fun applyConverterEnabled(prefs: ConverterPrefs, value: Boolean) {
        prefs.setConverterEnabled(value)
        if (!value) {
            LiveUpdateNotifier.cancelAllMirrored(applicationContext)
        } else {
            requestNotificationListenerRebind()
        }
        syncKeepAliveForegroundService(prefs)
        syncNetworkSpeedService(prefs)
        LiveBridgeTileService.requestStateSync(applicationContext)
    }

    private fun clearDynamicLauncherShortcuts() {
        runCatching { ShortcutManagerCompat.removeAllDynamicShortcuts(applicationContext) }
    }

    private fun getAppVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName?.trim().orEmpty()
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to resolve app version", error)
            ""
        }
    }

    private fun showUpdateAvailableNotification(version: String, releaseUrl: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationPermissionGranted()) {
            return false
        }

        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) {
            return false
        }

        ensureUpdateNotificationChannel()

        val normalizedReleaseUrl = releaseUrl.ifBlank { DEFAULT_RELEASES_URL }
        val openReleaseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedReleaseUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            UPDATE_NOTIFICATION_ID,
            openReleaseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isRuLocale = isRussianLocale()
        val title = if (isRuLocale) {
            "Доступно обновление LiveBridge"
        } else {
            "LiveBridge update available"
        }
        val content = if (isRuLocale) {
            "Новая версия: $version"
        } else {
            "New version: $version"
        }

        val notification = NotificationCompat.Builder(applicationContext, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_liveupdate)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(UPDATE_NOTIFICATION_ID, notification)
        return true
    }

    private fun ensureUpdateNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(UPDATE_CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            UPDATE_CHANNEL_ID,
            UPDATE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "LiveBridge app update notifications"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun isRussianLocale(): Boolean {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
        val language = locale?.language?.lowercase(Locale.ROOT).orEmpty()
        return language.startsWith("ru")
    }

    private fun isLikelyChineseDevice(): Boolean {
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase(Locale.ROOT)
        val brand = (Build.BRAND ?: "").lowercase(Locale.ROOT)
        val fingerprint = (Build.FINGERPRINT ?: "").lowercase(Locale.ROOT)
        val display = (Build.DISPLAY ?: "").lowercase(Locale.ROOT)
        val product = (Build.PRODUCT ?: "").lowercase(Locale.ROOT)
        val combined = "$manufacturer $brand $fingerprint $display $product"

        if (CHINESE_DEVICE_MARKERS.any(combined::contains)) {
            return true
        }
        return CHINESE_ROM_MARKERS.any(combined::contains)
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?: return false
        val service = ComponentName(this, LiveUpdateNotificationListenerService::class.java)

        return enabled.split(":")
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == service }
    }

    private fun requestNotificationListenerRebind(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }
        if (!isNotificationListenerEnabled()) {
            return false
        }

        return try {
            NotificationListenerService.requestRebind(
                ComponentName(this, LiveUpdateNotificationListenerService::class.java)
            )
            true
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to request listener rebind", error)
            false
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission(res: MethodChannel.Result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || isNotificationPermissionGranted()) {
            res.success(true)
            return
        }

        if (notificationPermissionResult != null) {
            res.error(
                "permission_in_progress",
                "Notification permission request is already in progress",
                null
            )
            return
        }

        notificationPermissionResult = res
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS
        )
    }

    private fun canPostPromotedNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < 36) {
            return false
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        return try {
            val method = notificationManager.javaClass.getMethod("canPostPromotedNotifications")
            method.invoke(notificationManager) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun openNotificationListenerSettings(): Boolean {
        if (launchSettingsIntent(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))) {
            return true
        }

        return launchSettingsIntent(appDetailsIntent())
    }

    private fun openAppNotificationSettings(): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }

        if (launchSettingsIntent(intent)) {
            return true
        }

        return launchSettingsIntent(appDetailsIntent())
    }

    private fun openPromotedNotificationSettings(): Boolean {
        val intent = Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS").apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }

        if (launchSettingsIntent(intent)) {
            return true
        }

        return openAppNotificationSettings()
    }

    private fun appDetailsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    }

    private fun loadInstalledAppsAsync(
        forceRefresh: Boolean,
        res: MethodChannel.Result
    ) {
        appsLoaderExecutor.execute {
            try {
                val apps = InstalledAppsRepository.loadInstalledApps(
                    context = applicationContext,
                    selfPackageName = packageName,
                    forceRefresh = forceRefresh
                )
                runOnUiThread {
                    res.success(apps)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to load installed apps", error)
                runOnUiThread {
                    res.error(
                        "installed_apps_failed",
                        "Failed to load installed apps",
                        error.message
                    )
                }
            }
        }
    }

    private fun getDeviceInfo(): Map<String, String> {
        val mn = DeviceProps.marketName()
        return mapOf(
            "manufacturer" to (Build.MANUFACTURER ?: ""),
            "brand" to (Build.BRAND ?: ""),
            "model" to mn,
            "marketName" to mn,
            "rawModel" to (Build.MODEL ?: ""),
            "product" to (Build.PRODUCT ?: ""),
            "fingerprint" to (Build.FINGERPRINT ?: ""),
            "display" to (Build.DISPLAY ?: "")
        )
    }

    private fun launchSettingsIntent(intent: Intent): Boolean {
        return try {
            if (intent.resolveActivity(packageManager) == null) {
                false
            } else {
                startActivity(intent)
                true
            }
        } catch (_: ActivityNotFoundException) {
            false
        } catch (error: SecurityException) {
            Log.e(TAG, "Unable to open settings with intent: ${intent.action}", error)
            false
        }
    }

    companion object {
        private const val METHOD_CHANNEL = "livebridge/platform"
        private const val REQUEST_POST_NOTIFICATIONS = 2406
        private const val TAG = "MainActivity"
        private const val UPDATE_CHANNEL_ID = "livebridge_update_checks"
        private const val UPDATE_CHANNEL_NAME = "LiveBridge Updates"
        private const val UPDATE_NOTIFICATION_ID = 32001
        private const val DEFAULT_RELEASES_URL = "https://github.com/Spottq/livebridge-for-samsung/releases"

        private val appsLoaderExecutor = Executors.newSingleThreadExecutor()
        private val CHINESE_DEVICE_MARKERS = setOf(
            "xiaomi",
            "redmi",
            "poco",
            "realme",
            "oppo",
            "oneplus",
            "vivo",
            "iqoo",
            "huawei",
            "honor",
            "zte",
            "nubia",
            "meizu",
            "lenovo"
        )
        private val CHINESE_ROM_MARKERS = setOf(
            "miui",
            "hyperos",
            "coloros",
            "originos",
            "funtouch",
            "harmony",
            "emui"
        )
    }
}
