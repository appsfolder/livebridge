package com.appsfolder.livebridge

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.MATCH_ALL
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
import com.appsfolder.livebridge.liveupdate.AppPresentationOverridesCodec
import com.appsfolder.livebridge.liveupdate.AppPresentationOverridesLoader
import com.appsfolder.livebridge.liveupdate.ConverterPrefs
import com.appsfolder.livebridge.liveupdate.ConversionLogStore
import com.appsfolder.livebridge.liveupdate.DeviceProps
import com.appsfolder.livebridge.liveupdate.KeepAliveForegroundService
import com.appsfolder.livebridge.liveupdate.LiveBridgeTileService
import com.appsfolder.livebridge.liveupdate.LiveParserDictionary
import com.appsfolder.livebridge.liveupdate.LiveParserDictionaryLoader
import com.appsfolder.livebridge.liveupdate.LiveUpdateNotifier
import com.appsfolder.livebridge.liveupdate.LiveUpdateNotificationListenerService
import com.appsfolder.livebridge.liveupdate.networkspeed.NetworkSpeedController
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.io.ByteArrayOutputStream
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
        syncNetworkSpeedForegroundService(prefs)
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
            "isNotificationListenerEnabled" -> res.success(isNotificationListenerEnabled())
            "requestNotificationListenerRebind" -> res.success(requestNotificationListenerRebind())
            "openNotificationListenerSettings" -> res.success(openNotificationListenerSettings())
            "isNotificationPermissionGranted" -> res.success(isNotificationPermissionGranted())
            "requestNotificationPermission" -> requestNotificationPermission(res)
            "canPostPromotedNotifications" -> res.success(canPostPromotedNotifications())
            "openPromotedNotificationSettings" -> res.success(openPromotedNotificationSettings())
            "openAppNotificationSettings" -> res.success(openAppNotificationSettings())
            "getInstalledApps" -> loadInstalledAppsAsync(res)
            "getDeviceInfo" -> res.success(getDeviceInfo())
            "exportLiveBridgeSettingsBackup" -> res.success(prefs.exportSettingsBackupJson())
            "saveLiveBridgeSettingsBackupToDownloads" -> {
                val savedUri = saveJsonToDownloads(
                    raw = prefs.exportSettingsBackupJson(),
                    filePrefix = "livebridge_settings",
                    extension = "lbst"
                )
                if (savedUri == null) {
                    res.error("save_failed", "Unable to save LiveBridge settings backup", null)
                } else {
                    res.success(savedUri)
                }
            }
            "importLiveBridgeSettingsBackup" -> {
                val raw = call.argument<String>("value")?.trim().orEmpty()
                if (raw.isBlank()) {
                    res.success(false)
                    return
                }
                val imported = prefs.importSettingsBackupJson(raw)
                if (imported) {
                    afterSettingsBackupImported(prefs)
                }
                res.success(imported)
            }
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

            "getParserDictionaryEnabledLanguages" -> {
                res.success(prefs.getParserDictionaryEnabledLanguageIds().toList())
            }

            "setParserDictionaryEnabledLanguages" -> {
                val values = call.argument<List<String>>("value").orEmpty().toSet()
                prefs.setParserDictionaryEnabledLanguageIds(values)
                LiveParserDictionaryLoader.invalidate()
                res.success(true)
            }

            "setParserDictionaryLanguageOverride" -> {
                val languageId = call.argument<String>("languageId")?.trim().orEmpty()
                if (languageId.isBlank()) {
                    res.error("invalid_language", "Language id is required", null)
                    return
                }
                val raw = call.argument<String>("value")?.trim().orEmpty()
                if (raw.isNotBlank() && !isValidJsonObject(raw)) {
                    res.error("invalid_dictionary", "Dictionary JSON is invalid", null)
                    return
                }
                val saved = prefs.setParserDictionaryLanguageOverrideRaw(languageId, raw)
                if (saved) {
                    LiveParserDictionaryLoader.invalidate()
                }
                res.success(saved)
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

            "getSpringTransitionsEnabled" -> res.success(prefs.getSpringTransitionsEnabled())
            "setSpringTransitionsEnabled" -> {
                prefs.setSpringTransitionsEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getPreventMirrorDismissEnabled" -> res.success(prefs.getPreventMirrorDismissEnabled())
            "setPreventMirrorDismissEnabled" -> {
                prefs.setPreventMirrorDismissEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getHideLockscreenContentEnabled" -> res.success(prefs.getHideLockscreenContentEnabled())
            "setHideLockscreenContentEnabled" -> {
                prefs.setHideLockscreenContentEnabled(call.argument<Boolean>("value") ?: false)
                LiveUpdateNotifier.ensureChannel(applicationContext)
                res.success(true)
            }

            "getConvertedNotificationSoundEnabled" -> {
                res.success(prefs.getConvertedNotificationSoundEnabled())
            }
            "setConvertedNotificationSoundEnabled" -> {
                prefs.setConvertedNotificationSoundEnabled(
                    call.argument<Boolean>("value") ?: false
                )
                LiveUpdateNotifier.ensureChannel(applicationContext)
                res.success(true)
            }

            "getHintsDisabled" -> res.success(prefs.getHintsDisabled())
            "setHintsDisabled" -> {
                prefs.setHintsDisabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getConversionLogEnabled" -> res.success(prefs.getConversionLogEnabled())
            "setConversionLogEnabled" -> {
                prefs.setConversionLogEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getBugReportAutoCopyEnabled" -> res.success(prefs.getBugReportAutoCopyEnabled())
            "setBugReportAutoCopyEnabled" -> {
                prefs.setBugReportAutoCopyEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getAppLanguageTag" -> res.success(prefs.getAppLanguageTag())
            "setAppLanguageTag" -> {
                prefs.setAppLanguageTag(call.argument<String>("value"))
                res.success(true)
            }

            "getConversionLogMaxBytes" -> res.success(prefs.getConversionLogMaxBytes())
            "setConversionLogMaxBytes" -> {
                prefs.setConversionLogMaxBytes(call.argument<Number>("value")?.toInt() ?: 0)
                ConversionLogStore.trimToPrefs(applicationContext, prefs)
                res.success(true)
            }

            "getConversionLogEntries" -> {
                res.success(ConversionLogStore.getEntriesRaw(applicationContext))
            }

            "getConversionLogEntriesPage" -> {
                loadConversionLogEntriesPageAsync(call, res)
            }

            "getNetworkSpeedEnabled" -> {
                syncNetworkSpeedForegroundService(prefs)
                res.success(prefs.getNetworkSpeedEnabled())
            }

            "setNetworkSpeedEnabled" -> {
                val value = call.argument<Boolean>("value") ?: false
                prefs.setNetworkSpeedEnabled(value)
                syncNetworkSpeedForegroundService(prefs)
                res.success(true)
            }

            "getNetworkSpeedMinThresholdBytesPerSecond" -> {
                syncNetworkSpeedForegroundService(prefs)
                res.success(prefs.getNetworkSpeedMinThresholdBytesPerSecond())
            }

            "setNetworkSpeedMinThresholdBytesPerSecond" -> {
                val value = call.argument<Number>("value")?.toLong() ?: 0L
                prefs.setNetworkSpeedMinThresholdBytesPerSecond(value)
                syncNetworkSpeedForegroundService(prefs)
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
            "getAospCuttingLength" ->
                res.success(prefs.getAospCuttingLength())
            "setAospCuttingLength" -> {
                prefs.setAospCuttingLength(
                    call.argument<Number>("value")?.toInt() ?: 7
                )
                res.success(true)
            }

            "getAnimatedIslandEnabled" -> res.success(prefs.getAnimatedIslandEnabled())
            "setAnimatedIslandEnabled" -> {
                prefs.setAnimatedIslandEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }
            "getAnimatedIslandUpdateFrequencyMs" ->
                res.success(prefs.getAnimatedIslandUpdateFrequencyMs())
            "setAnimatedIslandUpdateFrequencyMs" -> {
                prefs.setAnimatedIslandUpdateFrequencyMs(
                    call.argument<Number>("value")?.toInt() ?: 2250
                )
                res.success(true)
            }

            "getHyperBridgeEnabled" -> res.success(prefs.getHyperBridgeEnabled())
            "setHyperBridgeEnabled" -> {
                prefs.setHyperBridgeEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getNotificationDedupEnabled" -> res.success(prefs.getNotificationDedupEnabled())
            "setNotificationDedupEnabled" -> {
                prefs.setNotificationDedupEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getNotificationDedupMode" -> res.success(prefs.getNotificationDedupMode())
            "setNotificationDedupMode" -> {
                prefs.setNotificationDedupMode(call.argument<String>("value"))
                res.success(true)
            }

            "getNotificationDedupPackageRules" -> {
                res.success(prefs.getNotificationDedupPackageRulesRaw())
            }
            "setNotificationDedupPackageRules" -> {
                prefs.setNotificationDedupPackageRulesRaw(call.argument<String>("value"))
                res.success(true)
            }

            "getNotificationDedupPackageMode" -> {
                res.success(prefs.getNotificationDedupPackageMode())
            }
            "setNotificationDedupPackageMode" -> {
                prefs.setNotificationDedupPackageMode(call.argument<String>("value"))
                res.success(true)
            }

            "getOtpRemoveOriginalMessageEnabled" -> {
                res.success(prefs.getOtpRemoveOriginalMessageEnabled())
            }
            "setOtpRemoveOriginalMessageEnabled" -> {
                prefs.setOtpRemoveOriginalMessageEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getSmartRemoveOriginalMessageEnabled" -> {
                res.success(prefs.getSmartRemoveOriginalMessageEnabled())
            }
            "setSmartRemoveOriginalMessageEnabled" -> {
                prefs.setSmartRemoveOriginalMessageEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }

            "getSmartStatusDetectionEnabled" -> res.success(prefs.getSmartStatusDetectionEnabled())
            "setSmartStatusDetectionEnabled" -> {
                val value = call.argument<Boolean>("value") ?: true
                prefs.setSmartStatusDetectionEnabled(value)
                if (!value) {
                    LiveUpdateNotifier.cancelCallMirrors(applicationContext)
                }
                res.success(true)
            }

            "getSmartPackageRules" -> res.success(prefs.getSmartPackageRulesRaw())
            "setSmartPackageRules" -> {
                prefs.setSmartPackageRulesRaw(call.argument<String>("value"))
                res.success(true)
            }

            "getSmartPackageMode" -> res.success(prefs.getSmartPackageMode())
            "setSmartPackageMode" -> {
                prefs.setSmartPackageMode(call.argument<String>("value"))
                res.success(true)
            }

            "getSmartTaxiEnabled" -> res.success(prefs.getSmartTaxiEnabled())
            "setSmartTaxiEnabled" -> {
                prefs.setSmartTaxiEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getSmartDeliveryEnabled" -> res.success(prefs.getSmartDeliveryEnabled())
            "setSmartDeliveryEnabled" -> {
                prefs.setSmartDeliveryEnabled(call.argument<Boolean>("value") ?: true)
                res.success(true)
            }

            "getSmartCallsEnabled" -> res.success(prefs.getSmartCallsEnabled())
            "setSmartCallsEnabled" -> {
                val value = call.argument<Boolean>("value") ?: true
                prefs.setSmartCallsEnabled(value)
                if (!value) {
                    LiveUpdateNotifier.cancelCallMirrors(applicationContext)
                }
                res.success(true)
            }

            "getSmartMediaPlaybackEnabled" -> res.success(prefs.getSmartMediaPlaybackEnabled())
            "setSmartMediaPlaybackEnabled" -> {
                prefs.setSmartMediaPlaybackEnabled(call.argument<Boolean>("value") ?: false)
                res.success(true)
            }
            "getSmartMediaPlaybackShowOnLockScreen" -> {
                res.success(prefs.getSmartMediaPlaybackShowOnLockScreen())
            }
            "setSmartMediaPlaybackShowOnLockScreen" -> {
                prefs.setSmartMediaPlaybackShowOnLockScreen(
                    call.argument<Boolean>("value") ?: false
                )
                res.success(true)
            }
            "getSmartMediaPlaybackUseSymbolsInPlayer" -> {
                res.success(prefs.getSmartMediaPlaybackUseSymbolsInPlayer())
            }
            "setSmartMediaPlaybackUseSymbolsInPlayer" -> {
                prefs.setSmartMediaPlaybackUseSymbolsInPlayer(
                    call.argument<Boolean>("value") ?: false
                )
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

            "getSmartExternalDevicesIgnoreDebugging" -> res.success(
                prefs.getSmartExternalDevicesIgnoreDebugging()
            )
            "setSmartExternalDevicesIgnoreDebugging" -> {
                prefs.setSmartExternalDevicesIgnoreDebugging(call.argument<Boolean>("value") ?: true)
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

    private fun saveJsonToDownloads(
        raw: String,
        filePrefix: String,
        extension: String = "json",
        mimeType: String = "application/json"
    ): String? {
        val resolver = contentResolver
        val normalizedExtension = extension.trim().trimStart('.').ifBlank { "json" }
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.$normalizedExtension"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
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

    private fun afterSettingsBackupImported(prefs: ConverterPrefs) {
        AppPresentationOverridesLoader.invalidate()
        LiveParserDictionaryLoader.invalidate()
        ConversionLogStore.trimToPrefs(applicationContext, prefs)
        LiveUpdateNotifier.ensureChannel(applicationContext)
        applyConverterEnabled(prefs, prefs.getConverterEnabled())
        LiveBridgeTileService.requestStateSync(applicationContext)
    }

    private fun syncKeepAliveForegroundService(prefs: ConverterPrefs) {
        val shouldRun =
            prefs.getConverterEnabled() &&
                    prefs.getKeepAliveForegroundEnabled() &&
                    isNotificationListenerEnabled()
        if (shouldRun) {
            KeepAliveForegroundService.start(applicationContext)
        } else {
            KeepAliveForegroundService.stop(applicationContext)
        }
    }

    private fun syncNetworkSpeedForegroundService(prefs: ConverterPrefs) {
        NetworkSpeedController.sync(applicationContext, prefs)
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
            LiveUpdateNotifier.clearRuntimeState()
            NotificationManagerCompat.from(applicationContext).cancelAll()
        } else {
            requestNotificationListenerRebind()
        }
        syncKeepAliveForegroundService(prefs)
        syncNetworkSpeedForegroundService(prefs)
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

    private fun loadInstalledAppsAsync(res: MethodChannel.Result) {
        appsLoaderExecutor.execute {
            try {
                val apps = getInstalledApps()
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

    private fun loadConversionLogEntriesPageAsync(call: MethodCall, res: MethodChannel.Result) {
        val offset = call.argument<Number>("offset")?.toInt() ?: 0
        val limit = call.argument<Number>("limit")?.toInt() ?: 10

        appsLoaderExecutor.execute {
            try {
                val page = ConversionLogStore.getEntriesPageRaw(
                    context = applicationContext,
                    offset = offset,
                    limit = limit
                )
                runOnUiThread {
                    res.success(page)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to load conversion log page", error)
                runOnUiThread {
                    res.error(
                        "conversion_log_page_failed",
                        "Failed to load conversion log page",
                        error.message
                    )
                }
            }
        }
    }

    private fun getInstalledApps(): List<Map<String, Any>> {
        val now = System.currentTimeMillis()
        synchronized(installedAppsCacheLock) {
            val cached = installedAppsCache
            if (cached != null && now - installedAppsCacheAtMs <= INSTALLED_APPS_CACHE_TTL_MS) {
                return cached
            }
        }

        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, MATCH_ALL)
        }

        val entriesByPackage = linkedMapOf<String, MutableMap<String, Any>>()

        resolved.forEach { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@forEach
            val appPackage = activityInfo.packageName
            if (appPackage == packageName) {
                return@forEach
            }
            val resolvedLabel = resolveInfo.loadLabel(pm)?.toString()?.trim().orEmpty()
            val label = if (resolvedLabel.isNotEmpty()) resolvedLabel else appPackage
            val iconBytes = resolveCachedIconBytes(appPackage) ?: drawableToPngBytes(resolveInfo.loadIcon(pm))
            val isSystemApp = isSystemApp(activityInfo.applicationInfo)
            val entry = mutableMapOf<String, Any>(
                "packageName" to appPackage,
                "label" to label,
                "isSystem" to isSystemApp
            )
            if (iconBytes != null) {
                entry["icon"] = iconBytes
                cacheIconBytes(appPackage, iconBytes)
            }
            entriesByPackage[appPackage] = entry
        }

        getInstalledPackagesCompat(pm).forEach { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@forEach
            val appPackage = packageInfo.packageName.orEmpty()
            if (appPackage.isEmpty() || appPackage == packageName) {
                return@forEach
            }
            if (!isSystemApp(appInfo) || entriesByPackage.containsKey(appPackage)) {
                return@forEach
            }
            val label = appInfo.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { appPackage }
            val iconBytes = resolveCachedIconBytes(appPackage) ?: drawableToPngBytes(appInfo.loadIcon(pm))
            val entry = mutableMapOf<String, Any>(
                "packageName" to appPackage,
                "label" to label,
                "isSystem" to true
            )
            if (iconBytes != null) {
                entry["icon"] = iconBytes
                cacheIconBytes(appPackage, iconBytes)
            }
            entriesByPackage[appPackage] = entry
        }

        val entries = entriesByPackage.values
            .sortedBy { (it["label"] as? String)?.lowercase(Locale.getDefault()) ?: "" }
            .toList()

        synchronized(installedAppsCacheLock) {
            installedAppsCache = entries
            installedAppsCacheAtMs = now
        }
        return entries
    }

    private fun isSystemApp(applicationInfo: ApplicationInfo?): Boolean {
        if (applicationInfo == null) {
            return false
        }
        val flags = applicationInfo.flags
        return (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun getInstalledPackagesCompat(pm: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
    }

    private fun resolveCachedIconBytes(packageName: String): ByteArray? {
        synchronized(installedAppsCacheLock) {
            return appIconBytesCache[packageName]
        }
    }

    private fun cacheIconBytes(packageName: String, bytes: ByteArray) {
        synchronized(installedAppsCacheLock) {
            if (appIconBytesCache.size >= MAX_ICON_CACHE_SIZE && !appIconBytesCache.containsKey(packageName)) {
                appIconBytesCache.clear()
            }
            appIconBytesCache[packageName] = bytes
        }
    }

    private fun drawableToPngBytes(drawable: Drawable?): ByteArray? {
        drawable ?: return null
        return try {
            val sizePx = 96
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.toByteArray()
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to extract app icon for package picker", error)
            null
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
            "device" to (Build.DEVICE ?: ""),
            "board" to (Build.BOARD ?: ""),
            "hardware" to (Build.HARDWARE ?: ""),
            "bootloader" to (Build.BOOTLOADER ?: ""),
            "host" to (Build.HOST ?: ""),
            "id" to (Build.ID ?: ""),
            "tags" to (Build.TAGS ?: ""),
            "type" to (Build.TYPE ?: ""),
            "user" to (Build.USER ?: ""),
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
        private const val INSTALLED_APPS_CACHE_TTL_MS = 10 * 60 * 1000L
        private const val MAX_ICON_CACHE_SIZE = 512
        private const val UPDATE_CHANNEL_ID = "livebridge_update_checks"
        private const val UPDATE_CHANNEL_NAME = "LiveBridge Updates"
        private const val UPDATE_NOTIFICATION_ID = 32001
        private const val DEFAULT_RELEASES_URL = "https://appsfolder.github.io/livebridge/"

        private val installedAppsCacheLock = Any()
        private var installedAppsCache: List<Map<String, Any>>? = null
        private var installedAppsCacheAtMs: Long = 0L
        private val appIconBytesCache: MutableMap<String, ByteArray> = mutableMapOf()
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
