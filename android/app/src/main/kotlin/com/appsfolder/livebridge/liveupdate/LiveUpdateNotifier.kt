package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.icu.text.BreakIterator
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.kakao.taxi.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

object LiveUpdateNotifier {
    const val CHANNEL_ID = "livebridge_promoted_updates"
    private const val YANDEX_MAPS_PACKAGE = "ru.yandex.yandexmaps"

    private const val CHANNEL_NAME = "LiveBridge Updates"
    private const val TAG = "LiveUpdateNotifier"
    private const val MAX_MIRRORED_ACTIONS = 3
    private const val OTP_REPEAT_SUPPRESS_MS = 60_000L
    private const val OTP_AUTOCOPY_COPIED_SHOW_DELAY_MS = 1_000L
    private const val OTP_AUTOCOPY_COPIED_SHOW_DURATION_MS = 1_500L
    private const val AOSP_ISLAND_TEXT_LIMIT = 7
    private val BLOCKED_SOURCE_PACKAGES = setOf(
        "ru.dublgis.dgismobile"
    )
    private val KNOWN_NAVIGATION_PACKAGES = setOf(
        YANDEX_MAPS_PACKAGE,
        "com.google.android.apps.maps",
        "com.waze"
    )
    private val NAVIGATION_DISTANCE_PATTERN = Regex(
        "(?<!\\d)\\d{1,4}(?:[\\s.,]\\d{1,2})?\\s*(?:км|km|м|m|mi|ft|миль|фут)\\b",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val TEXT_PROGRESS_PERCENT_PATTERN = Regex("(?<!\\d)(\\d{1,3})\\s*%")
    private val TEXT_PROGRESS_DISCOUNT_CONTEXT_PATTERN = Regex(
        "(скид|акци|промокод|промо|купон|распрод|кэшб[еэ]к|кешб[еэ]к|discount|promo|coupon|sale|cashback|off\\b|выгод|bonus|бонус|save|deal|special\\s+offer|limited\\s+time|дарим|подар)",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val TEXT_PROGRESS_OFFER_CONTEXT_PATTERN = Regex(
        "(при\\s+заказ|при\\s+покуп|minimum\\s+order|order\\s+from|в\\s+приложени\\S*\\s+акци)",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val TEXT_PROGRESS_MONEY_CONTEXT_PATTERN = Regex(
        "(\\d{2,7}\\s*(?:₽|руб\\.?|рубл(?:ей|я|ь)?|rur|usd|eur|\\$|€|kzt|тенге))",
        setOf(RegexOption.IGNORE_CASE)
    )
    private const val SMART_ISLAND_ANIMATION_MIN_DELAY_MS = 2_000L
    private const val SMART_ISLAND_ANIMATION_MAX_DELAY_MS = 3_000L
    private const val SMART_ISLAND_TOKEN_MAX_LENGTH = 20

    private val OTP_CODE_LENGTH = 4..8
    private val transparentActionIcon by lazy {
        IconCompat.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }
    private val progressColor = Color.valueOf(15f / 255f, 118f / 255f, 110f / 255f, 1f).toArgb()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val appIconCacheLock = Any()
    private val appIconCache = mutableMapOf<String, AppIconAssets>()
    private val missingAppIconPackages = mutableSetOf<String>()

    private val stateLock = Any()
    private val sbnToAggregateKey = mutableMapOf<String, String>()
    private val aggregateStates = mutableMapOf<String, AggregateState>()
    private val sbnToOtpAggregateKey = mutableMapOf<String, String>()
    private val sbnToOtpSourceKey = mutableMapOf<String, String>()
    private val otpSourceStates = mutableMapOf<String, OtpSourceState>()
    private val otpAggregateStates = mutableMapOf<String, OtpAggregateState>()
    private val otpAnimationGenerations = mutableMapOf<String, Long>()
    private val smartAnimationGenerations = mutableMapOf<String, Long>()
    private val smartAnimationStates = mutableMapOf<String, SmartAnimationState>()

    private data class AppIconAssets(
        val smallIcon: IconCompat?,
        val largeIconBitmap: Bitmap?
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val current = manager.getNotificationChannel(CHANNEL_ID)

        if (current == null) {
            manager.createNotificationChannel(createChannel())
            return
        }

        if (current.lockscreenVisibility != Notification.VISIBILITY_PUBLIC) {
            current.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            manager.createNotificationChannel(current)
        }
    }

    private fun createChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Converted promoted ongoing notifications"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
    }

    fun clearRuntimeState() {
        synchronized(stateLock) {
            sbnToAggregateKey.clear()
            aggregateStates.clear()
            sbnToOtpAggregateKey.clear()
            sbnToOtpSourceKey.clear()
            otpSourceStates.clear()
            otpAggregateStates.clear()
            otpAnimationGenerations.clear()
            smartAnimationGenerations.clear()
            smartAnimationStates.clear()
        }
        synchronized(appIconCacheLock) {
            appIconCache.clear()
            missingAppIconPackages.clear()
        }
    }

    fun cancelAllMirrored(context: Context) {
        clearRuntimeState()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val manager = NotificationManagerCompat.from(context)
        notificationManager.activeNotifications
            .filter { it.notification.channelId == CHANNEL_ID }
            .forEach { statusBarNotification ->
                manager.cancel(statusBarNotification.id)
            }
    }

    fun maybeMirror(context: Context, prefs: ConverterPrefs, sbn: StatusBarNotification): Boolean {
        ensureChannel(context)

        val manager = NotificationManagerCompat.from(context)
        if (!prefs.getConverterEnabled()) {
            val staleAggregateIds = synchronized(stateLock) {
                clearAggregateTrackingForSbnKeyLocked(sbn.key)
            }
            staleAggregateIds.forEach(manager::cancel)
            manager.cancel(mirrorIdForKey(sbn.key))
            return false
        }
        if (prefs.getSyncDndEnabled() && isDoNotDisturbActive(context)) {
            val staleAggregateIds = synchronized(stateLock) {
                clearAggregateTrackingForSbnKeyLocked(sbn.key)
            }
            staleAggregateIds.forEach(manager::cancel)
            manager.cancel(mirrorIdForKey(sbn.key))
            return false
        }

        return try {
            if (!passesCoreFilters(context.packageName, sbn)) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach(manager::cancel)
                manager.cancel(mirrorIdForKey(sbn.key))
                return false
            }
            val appPresentationOverride = AppPresentationOverridesLoader
                .get(prefs)
                .resolve(sbn.packageName.lowercase(Locale.ROOT))
            if (prefs.shouldBypassAllRulesForPackage(sbn.packageName)) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach(manager::cancel)
                val bypassSamsungBridge = SamsungBridgePreprocessor.build(
                    context = context,
                    prefs = prefs,
                    sbn = sbn,
                    sourceHasNativeProgress = hasProgress(sbn.notification)
                )

                val notification = buildMirroredNotification(
                    context = context,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    progressOverride = null,
                    otpOverride = null,
                    smartShortTextOverride = null,
                    requestPromoted = true,
                    samsungBridge = bypassSamsungBridge,
                    allowNavigationIconHeuristics = false
                )
                notifyWithPromotionFallback(
                    context = context,
                    manager = manager,
                    notificationId = mirrorIdForKey(sbn.key),
                    promotedNotification = notification,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    progressOverride = null,
                    otpOverride = null,
                    smartShortTextOverride = null,
                    samsungBridge = bypassSamsungBridge,
                    allowNavigationIconHeuristics = false
                )
                return true
            }
            val parserDictionary = LiveParserDictionaryLoader.get(context, prefs)
            val mediaPlaybackSmartEnabled = prefs.getSmartMediaPlaybackEnabled()
            if (!passesBaseFilters(prefs, sbn, parserDictionary, mediaPlaybackSmartEnabled)) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach(manager::cancel)
                manager.cancel(mirrorIdForKey(sbn.key))
                return false
            }
            val source = sbn.notification
            val samsungBridge = SamsungBridgePreprocessor.build(
                context = context,
                prefs = prefs,
                sbn = sbn,
                sourceHasNativeProgress = hasProgress(source)
            )
            val hasNativeProgress = samsungBridge.hasNativeOrSamsungProgress
            val animatedIslandEnabled = prefs.getAnimatedIslandEnabled()
            val isMediaPlaybackNotification = mediaPlaybackSmartEnabled &&
                    isLikelyMediaPlaybackNotification(source)
            val mediaPlaybackSnapshot = if (isMediaPlaybackNotification) {
                extractMediaPlaybackSnapshot(
                    context = context,
                    notification = source,
                    sourcePackageName = sbn.packageName
                )
            } else {
                null
            }

            val otpMatch = if (!isMediaPlaybackNotification &&
                !hasNativeProgress &&
                prefs.getOtpDetectionEnabled() &&
                prefs.isOtpPackageAllowed(sbn.packageName)
            ) {
                detectOtpCode(sbn.packageName, source, parserDictionary)
            } else {
                null
            }

            val smartMatch = if (!isMediaPlaybackNotification &&
                otpMatch == null &&
                prefs.getSmartStatusDetectionEnabled()
            ) {
                detectSmartStage(
                    packageName = sbn.packageName,
                    source = source,
                    parserDictionary = parserDictionary,
                    navigationEnabled = prefs.getSmartNavigationEnabled(),
                    weatherEnabled = prefs.getSmartWeatherEnabled(),
                    externalDevicesEnabled = prefs.getSmartExternalDevicesEnabled(),
                    vpnEnabled = prefs.getSmartVpnEnabled(),
                    hasNativeProgress = hasNativeProgress
                )
            } else {
                null
            }

            val textProgressMatch = if (!isMediaPlaybackNotification &&
                !hasNativeProgress &&
                otpMatch == null &&
                prefs.getTextProgressEnabled()
            ) {
                detectTextProgress(
                    packageName = sbn.packageName,
                    source = source,
                    parserDictionary = parserDictionary
                )
            } else {
                null
            }

            val shouldSuppressNonTrafficVpn = !isMediaPlaybackNotification &&
                    otpMatch == null &&
                    smartMatch == null &&
                    textProgressMatch == null &&
                    prefs.getSmartVpnEnabled() &&
                    shouldSuppressVpnWithoutTraffic(
                        packageName = sbn.packageName,
                        source = source,
                        parserDictionary = parserDictionary
                    )
            if (shouldSuppressNonTrafficVpn) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach(manager::cancel)
                manager.cancel(mirrorIdForKey(sbn.key))
                return false
            }

            if (!hasNativeProgress &&
                !isMediaPlaybackNotification &&
                otpMatch == null &&
                smartMatch == null &&
                textProgressMatch == null &&
                prefs.getOnlyWithProgress()
            ) {
                val staleAggregateIds = synchronized(stateLock) {
                    clearAggregateTrackingForSbnKeyLocked(sbn.key)
                }
                staleAggregateIds.forEach(manager::cancel)
                manager.cancel(mirrorIdForKey(sbn.key))
                return false
            }

            when {
                isMediaPlaybackNotification -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach(manager::cancel)

                    val mediaProgressOverride = mediaPlaybackSnapshot?.toProgressOverride()
                    val mediaShortText = mediaPlaybackSnapshot?.let(::buildMediaPlaybackShortText)
                    val mediaTitle = mediaPlaybackSnapshot?.title
                    val mediaText = mediaPlaybackSnapshot?.artist
                    val mediaLargeIcon = mediaPlaybackSnapshot?.albumArt
                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = mediaProgressOverride,
                        otpOverride = null,
                        smartShortTextOverride = mediaShortText,
                        requestPromoted = true,
                        allowNavigationIconHeuristics = false,
                        preferMediaControls = true,
                        mediaPlaybackIsPlaying = mediaPlaybackSnapshot?.isPlaying,
                        titleOverride = mediaTitle,
                        textOverride = mediaText,
                        largeIconOverride = mediaLargeIcon
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(sbn.key),
                        promotedNotification = notification,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = mediaProgressOverride,
                        otpOverride = null,
                        smartShortTextOverride = mediaShortText,
                        allowNavigationIconHeuristics = false,
                        preferMediaControls = true,
                        mediaPlaybackIsPlaying = mediaPlaybackSnapshot?.isPlaying,
                        titleOverride = mediaTitle,
                        textOverride = mediaText,
                        largeIconOverride = mediaLargeIcon
                    )
                    true
                }

                otpMatch != null -> {
                    val routeState = synchronized(stateLock) {
                        val staleAggregateIds = mutableListOf<Int>()
                        staleAggregateIds.addAll(clearSmartTrackingForSbnKeyLocked(sbn.key))

                        val sourceKey = otpSourceKeyForPackage(sbn.packageName)
                        val sourceState = otpSourceStates[sourceKey]
                        if (sourceState != null &&
                            sourceState.sbnKey != sbn.key &&
                            sbn.postTime < sourceState.postTimeMs
                        ) {
                            staleAggregateIds.addAll(clearOtpTrackingForSbnKeyLocked(sbn.key))
                            OtpRouteState(
                                staleAggregateIds = staleAggregateIds,
                                shouldPublish = false,
                                shouldAutoCopy = false,
                                otpCode = otpMatch.code
                            )
                        } else {
                            staleAggregateIds.addAll(clearOtpTrackingForSourceLocked(sourceKey, sbn.key))

                            val existingOtpAggregateKey = sbnToOtpAggregateKey[sbn.key]
                            if (existingOtpAggregateKey != null && existingOtpAggregateKey != otpMatch.aggregateKey) {
                                staleAggregateIds.addAll(clearOtpTrackingForSbnKeyLocked(sbn.key))
                            }

                            val state = otpAggregateStates.getOrPut(otpMatch.aggregateKey) { OtpAggregateState() }
                            state.activeSbnKeys.add(sbn.key)
                            sbnToOtpAggregateKey[sbn.key] = otpMatch.aggregateKey
                            sbnToOtpSourceKey[sbn.key] = sourceKey
                            otpSourceStates[sourceKey] = OtpSourceState(
                                sbnKey = sbn.key,
                                aggregateKey = otpMatch.aggregateKey,
                                postTimeMs = sbn.postTime
                            )

                            val now = System.currentTimeMillis()
                            val shouldPublish =
                                state.lastRenderedAtMs == 0L ||
                                        now - state.lastRenderedAtMs >= OTP_REPEAT_SUPPRESS_MS
                            if (shouldPublish) {
                                state.lastRenderedAtMs = now
                            }
                            val shouldAutoCopy =
                                prefs.getOtpAutoCopyEnabled() &&
                                        shouldAutoCopyOtpLocked(state, otpMatch.code, now)
                            OtpRouteState(
                                staleAggregateIds = staleAggregateIds,
                                shouldPublish = shouldPublish,
                                shouldAutoCopy = shouldAutoCopy,
                                otpCode = otpMatch.code
                            )
                        }
                    }
                    routeState.staleAggregateIds.forEach(manager::cancel)

                    if (routeState.shouldPublish) {
                        val notification = buildMirroredNotification(
                            context = context,
                            sbn = sbn,
                            appPresentationOverride = appPresentationOverride,
                            progressOverride = null,
                            otpOverride = otpMatch,
                            smartShortTextOverride = null,
                            requestPromoted = true,
                            samsungBridge = samsungBridge
                        )
                        notifyWithPromotionFallback(
                            context = context,
                            manager = manager,
                            notificationId = mirrorIdForKey(otpMatch.aggregateKey),
                            promotedNotification = notification,
                            sbn = sbn,
                            appPresentationOverride = appPresentationOverride,
                            progressOverride = null,
                            otpOverride = otpMatch,
                            smartShortTextOverride = null,
                            samsungBridge = samsungBridge
                        )
                    }
                    if (routeState.shouldAutoCopy) {
                        copyOtpToClipboard(context, routeState.otpCode)
                        if (routeState.shouldPublish) {
                            startOtpAutoCopyAnimation(
                                context = context,
                                manager = manager,
                                sbn = sbn,
                                appPresentationOverride = appPresentationOverride,
                                otpMatch = otpMatch,
                                samsungBridge = samsungBridge
                            )
                        }
                    }
                    true
                }

                textProgressMatch != null -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach(manager::cancel)

                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = ProgressOverride(
                            value = textProgressMatch.percent,
                            max = 100
                        ),
                        otpOverride = null,
                        smartShortTextOverride = textProgressMatch.shortText,
                        requestPromoted = true,
                        samsungBridge = samsungBridge
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(sbn.key),
                        promotedNotification = notification,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = ProgressOverride(
                            value = textProgressMatch.percent,
                            max = 100
                        ),
                        otpOverride = null,
                        smartShortTextOverride = textProgressMatch.shortText,
                        samsungBridge = samsungBridge
                    )
                    true
                }

                smartMatch != null -> {
                    val routeState = synchronized(stateLock) {
                        val staleAggregateIds = mutableListOf<Int>()
                        staleAggregateIds.addAll(clearOtpTrackingForSbnKeyLocked(sbn.key))

                        val existingSmartAggregateKey = sbnToAggregateKey[sbn.key]
                        if (existingSmartAggregateKey != null &&
                            existingSmartAggregateKey != smartMatch.aggregateKey
                        ) {
                            staleAggregateIds.addAll(clearSmartTrackingForSbnKeyLocked(sbn.key))
                        }

                        val state = aggregateStates.getOrPut(smartMatch.aggregateKey) {
                            AggregateState(smartMatch.stageValue, smartMatch.maxStage)
                        }
                        state.activeSbnKeys.add(sbn.key)
                        state.sourcesBySbnKey[sbn.key] = SmartSourceEntry(
                            stageValue = smartMatch.stageValue,
                            postTimeMs = sbn.postTime,
                            sbn = sbn,
                            compactOrderCode = smartMatch.compactOrderCode
                        )
                        state.maxStageSeen = if (smartMatch.keepHighestStage) {
                            maxOf(state.maxStageSeen, smartMatch.stageValue)
                        } else {
                            smartMatch.stageValue
                        }
                        sbnToAggregateKey[sbn.key] = smartMatch.aggregateKey
                        val sourceEntry = selectSmartSourceEntryLocked(
                            aggregateState = state,
                            keepHighestStage = smartMatch.keepHighestStage
                        )

                        SmartRouteState(
                            staleAggregateIds = staleAggregateIds,
                            stageValue = state.maxStageSeen,
                            stageMax = state.maxStage,
                            compactOrderCode = sourceEntry?.compactOrderCode ?: smartMatch.compactOrderCode,
                            sourceSbn = sourceEntry?.sbn ?: sbn
                        )
                    }
                    routeState.staleAggregateIds.forEach(manager::cancel)
                    val sourceSbn = routeState.sourceSbn
                    val sourceNotification = sourceSbn.notification
                    val smartRuleId = smartRuleIdFromAggregateKey(smartMatch.aggregateKey)
                    val defaultSmartStatus = smartShortStatusText(
                        context = context,
                        ruleId = smartRuleId,
                        stageValue = routeState.stageValue,
                        parserDictionary = parserDictionary
                    )
                    val vpnTraffic = if (smartRuleId == "vpn") {
                        extractVpnTrafficSpeeds(
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            parserDictionary = parserDictionary
                        )
                    } else {
                        null
                    }
                    val smartStatusText = when (smartRuleId) {
                        "navigation" -> extractNavigationDistanceText(
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            parserDictionary = parserDictionary
                        ) ?: defaultSmartStatus

                        "weather" -> extractWeatherTemperatureText(
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            parserDictionary = parserDictionary
                        ) ?: defaultSmartStatus

                        "external_device" -> extractExternalDeviceStatusText(
                            context = context,
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            stageValue = routeState.stageValue,
                            parserDictionary = parserDictionary
                        ) ?: defaultSmartStatus

                        "vpn" -> formatDominantVpnTrafficText(vpnTraffic) ?: defaultSmartStatus

                        else -> defaultSmartStatus
                    } ?: routeState.compactOrderCode
                    val smartProgressOverride = if (
                        smartRuleId == "weather" ||
                        smartRuleId == "external_device" ||
                        smartRuleId == "vpn"
                    ) {
                        null
                    } else {
                        ProgressOverride(routeState.stageValue, routeState.stageMax)
                    }

                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sourceSbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = smartProgressOverride,
                        otpOverride = null,
                        smartShortTextOverride = smartStatusText,
                        compactCodeOverride = routeState.compactOrderCode,
                        smartRuleId = smartRuleId,
                        requestPromoted = true,
                        samsungBridge = samsungBridge
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(smartMatch.aggregateKey),
                        promotedNotification = notification,
                        sbn = sourceSbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = smartProgressOverride,
                        otpOverride = null,
                        smartShortTextOverride = smartStatusText,
                        compactCodeOverride = routeState.compactOrderCode,
                        smartRuleId = smartRuleId,
                        samsungBridge = samsungBridge
                    )
                    if (animatedIslandEnabled) {
                        val animatedTokens = buildSmartAnimatedIslandTokens(
                            ruleId = smartRuleId,
                            notification = sourceNotification,
                            fallbackTitle = sourceSbn.packageName,
                            primaryStatus = smartStatusText,
                            compactOrderCode = routeState.compactOrderCode,
                            parserDictionary = parserDictionary
                        )
                        startSmartIslandAnimation(
                            context = context,
                            manager = manager,
                            aggregateKey = smartMatch.aggregateKey,
                            sbn = sourceSbn,
                            appPresentationOverride = appPresentationOverride,
                            progressOverride = smartProgressOverride,
                            smartRuleId = smartRuleId,
                            tokens = animatedTokens,
                            initialToken = smartStatusText,
                            samsungBridge = samsungBridge
                        )
                    }
                    true
                }

                else -> {
                    val staleAggregateIds = synchronized(stateLock) {
                        clearAggregateTrackingForSbnKeyLocked(sbn.key)
                    }
                    staleAggregateIds.forEach(manager::cancel)

                    val notification = buildMirroredNotification(
                        context = context,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = null,
                        otpOverride = null,
                        smartShortTextOverride = null,
                        requestPromoted = true,
                        samsungBridge = samsungBridge
                    )
                    notifyWithPromotionFallback(
                        context = context,
                        manager = manager,
                        notificationId = mirrorIdForKey(sbn.key),
                        promotedNotification = notification,
                        sbn = sbn,
                        appPresentationOverride = appPresentationOverride,
                        progressOverride = null,
                        otpOverride = null,
                        smartShortTextOverride = null,
                        samsungBridge = samsungBridge
                    )
                    true
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to mirror notification: ${sbn.key}", error)
            false
        }
    }

    private fun isDoNotDisturbActive(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        return try {
            when (notificationManager.currentInterruptionFilter) {
                NotificationManager.INTERRUPTION_FILTER_NONE,
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                NotificationManager.INTERRUPTION_FILTER_ALARMS -> true

                else -> false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun cancelMirrored(context: Context, sbn: StatusBarNotification) {
        try {
            val manager = NotificationManagerCompat.from(context)
            val staleAggregateIds = synchronized(stateLock) {
                clearAggregateTrackingForSbnKeyLocked(sbn.key)
            }
            staleAggregateIds.forEach(manager::cancel)
            manager.cancel(mirrorIdForKey(sbn.key))
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to cancel mirrored notification: ${sbn.key}", error)
        }
    }

    private fun passesBaseFilters(
        prefs: ConverterPrefs,
        sbn: StatusBarNotification,
        parserDictionary: LiveParserDictionary,
        mediaPlaybackSmartEnabled: Boolean
    ): Boolean {
        val source = sbn.notification

        if (isLikelyMediaPlaybackNotification(source) && !mediaPlaybackSmartEnabled) {
            return false
        }

        if (parserDictionary.blockedSourcePackages.contains(sbn.packageName.lowercase(Locale.ROOT))) {
            return false
        }

        return prefs.isPackageAllowed(sbn.packageName)
    }

    private fun passesCoreFilters(
        appPackageName: String,
        sbn: StatusBarNotification
    ): Boolean {
        if (appPackageName.isNotEmpty() && sbn.packageName == appPackageName) {
            return false
        }
        val source = sbn.notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.channelId == CHANNEL_ID) {
            return false
        }
        if (Build.VERSION.SDK_INT >= 36 && source.flags and 0x40000 != 0) {
            return false
        }
        if (source.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            return false
        }
        return true
    }

    private fun buildMirroredNotification(
        context: Context,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        progressOverride: ProgressOverride?,
        otpOverride: OtpMatch?,
        smartShortTextOverride: String?,
        compactCodeOverride: String? = null,
        smartRuleId: String? = null,
        requestPromoted: Boolean,
        otpShortTextOverride: String? = null,
        samsungBridge: SamsungBridgeContext = SamsungBridgeContext.disabled(
            sourceHasNativeProgress = false
        ),
        allowNavigationIconHeuristics: Boolean = true,
        preferMediaControls: Boolean = false,
        mediaPlaybackIsPlaying: Boolean? = null,
        titleOverride: String? = null,
        textOverride: String? = null,
        largeIconOverride: Bitmap? = null
    ): Notification {
        val runtimePrefs = ConverterPrefs(context)
        val parserDictionary = LiveParserDictionaryLoader.get(context, runtimePrefs)
        val source = sbn.notification
        val samsungReparse = samsungBridge.reparsePayload
        val sourceSmallIcon = resolveSourceSmallIcon(context, sbn)
        val appIconAssets = resolveAppIconAssets(context, sbn.packageName)
        val appSmallIcon = appIconAssets?.smallIcon
        val samsungSmallIcon = samsungReparse?.icon
        val samsungLargeIcon = samsungReparse?.largeIconBitmap
        val remoteDrawableAssets = resolveRemoteDrawableAssets(context, sbn)
        val shouldTryNavigationArrowIcon =
            appPresentationOverride.iconSource == NotificationIconSource.NOTIFICATION &&
                    (smartRuleId == "navigation" ||
                            (allowNavigationIconHeuristics &&
                                    isLikelyNavigationPackage(sbn.packageName, parserDictionary)))
        val navigationDrawable =
            if (shouldTryNavigationArrowIcon) {
                remoteDrawableAssets
            } else {
                null
            }
        val sourceLargeIcon = resolveSourceLargeIconBitmap(context, source)
        val preferredLargeIcon = when {
            shouldTryNavigationArrowIcon ->
                navigationDrawable?.bitmap ?: samsungLargeIcon ?: sourceLargeIcon
            else ->
                samsungLargeIcon ?: sourceLargeIcon
        }
        val nowBarAppIcon = appSmallIcon ?: sourceSmallIcon ?: samsungSmallIcon
        val nowBarRightIcon = if (samsungBridge.hasCustomRemoteCard) {
            null
        } else {
            samsungReparse?.rightIcon
                ?: remoteDrawableAssets?.icon
                ?: preferredLargeIcon?.let { bitmap ->
                    runCatching { IconCompat.createWithBitmap(bitmap) }.getOrNull()
                }
        }

        val appName = resolveAppName(context, sbn.packageName)
        val allowRemoteViewTextFallback = shouldTryNavigationArrowIcon
        val title = samsungReparse?.title?.takeIf { it.isNotBlank() }
            ?: extractTitle(source, appName, allowRemoteViewTextFallback)
        val text = samsungReparse?.text?.takeIf { it.isNotBlank() }
            ?: extractText(source, allowRemoteViewTextFallback)
        val displayTitle = when (appPresentationOverride.compactTextSource) {
            CompactTextSource.TEXT -> text.ifBlank { title }
            CompactTextSource.TITLE -> title
        }
        val displayText = if (
            appPresentationOverride.compactTextSource == CompactTextSource.TEXT &&
            title.isNotBlank() &&
            title != displayTitle
        ) {
            title
        } else {
            text
        }
        val compactPrimaryText = sequenceOf(
            otpShortTextOverride?.trim(),
            otpOverride?.code?.trim(),
            compactCodeOverride?.trim(),
            displayTitle.trim()
        ).firstOrNull { !it.isNullOrEmpty() } ?: displayTitle
        val aospCuttingEnabled = runtimePrefs.getAospCuttingEnabled()
        val hyperBridgeEnabled = runtimePrefs.getHyperBridgeEnabled()

        val sourceHasProgress = hasProgress(source)
        val samsungProgressMax = samsungReparse?.progressMax ?: 0
        val samsungProgressValue = samsungReparse?.progressValue ?: 0
        val progressMax = when {
            sourceHasProgress -> source.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            progressOverride != null -> progressOverride.max
            else -> samsungProgressMax
        }
        val progressValue = when {
            sourceHasProgress -> source.extras.getInt(Notification.EXTRA_PROGRESS, 0)
            progressOverride != null -> progressOverride.value
            else -> samsungProgressValue
        }
        val indeterminate = when {
            sourceHasProgress ->
                source.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
            progressOverride != null -> false
            else -> false
        }
        val hasProgress = sourceHasProgress || progressOverride != null || samsungProgressMax > 0
        var resolvedProgressChipText: String? = null
        val determinateProgressPercent = if (hasProgress && !indeterminate && progressMax > 0) {
            val safeMax = progressMax.coerceAtLeast(1)
            val safeProgress = progressValue.coerceIn(0, safeMax)
            ((safeProgress.toFloat() / safeMax.toFloat()) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        } else {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(compactPrimaryText)
            .setContentText(displayText)
            .setSubText(appName)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(resolveStableWhen(source, sbn.postTime))
            .setShowWhen(false)
            .setColor(progressColor)
            .setCategory(if (hasProgress) Notification.CATEGORY_PROGRESS else Notification.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val preferredSmallIcon = when (appPresentationOverride.iconSource) {
            NotificationIconSource.NOTIFICATION -> when {
                shouldTryNavigationArrowIcon ->
                    appSmallIcon ?: navigationDrawable?.icon ?: samsungSmallIcon ?: sourceSmallIcon
                samsungBridge.hasCustomRemoteCard ->
                    appSmallIcon ?: samsungSmallIcon ?: sourceSmallIcon
                else ->
                    appSmallIcon ?: sourceSmallIcon ?: samsungSmallIcon
            }
            NotificationIconSource.APP -> appSmallIcon ?: sourceSmallIcon
        }
        val preferredChipIcon = when {
            shouldTryNavigationArrowIcon ->
                navigationDrawable?.icon ?: sourceSmallIcon ?: samsungSmallIcon ?: appSmallIcon
            samsungBridge.hasCustomRemoteCard ->
                samsungSmallIcon ?: sourceSmallIcon ?: appSmallIcon
            else ->
                sourceSmallIcon ?: samsungSmallIcon ?: appSmallIcon
        }
        applySmallIcon(context, builder, preferredSmallIcon)
        preferredLargeIcon?.let(builder::setLargeIcon)

        if (requestPromoted) {
            builder.setRequestPromotedOngoing(true)
        }

        if (otpOverride != null) {
            builder.addAction(buildCopyOtpAction(context, sbn, otpOverride.code))
        }

        source.contentIntent?.let(builder::setContentIntent)
        source.deleteIntent?.let(builder::setDeleteIntent)

        copySourceActions(
            source = source,
            builder = builder,
            maxActions = if (otpOverride != null) MAX_MIRRORED_ACTIONS - 1 else MAX_MIRRORED_ACTIONS,
            preferMediaControls = preferMediaControls,
            mediaPlaybackIsPlaying = mediaPlaybackIsPlaying
        )

        if (hasProgress) {
            if (indeterminate || progressMax <= 0) {
                builder.setProgress(0, 0, true)
                builder.setStyle(
                    NotificationCompat.ProgressStyle()
                        .setProgressIndeterminate(true)
                        .setStyledByProgress(true)
                )
            } else {
                val safeMax = progressMax.coerceAtLeast(1)
                val safeProgress = progressValue.coerceIn(0, safeMax)
                val percent = determinateProgressPercent ?: 0

                builder.setProgress(safeMax, safeProgress, false)
                builder.setStyle(
                    NotificationCompat.ProgressStyle()
                        .setProgress(percent)
                        .setStyledByProgress(true)
                )
                resolvedProgressChipText = smartShortTextOverride ?: "$percent%"
                builder.setShortCriticalText(
                    limitIslandText(resolvedProgressChipText, aospCuttingEnabled)
                )
            }
        } else if (otpOverride != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
            builder.setShortCriticalText(
                limitIslandText(otpShortTextOverride ?: otpOverride.code, aospCuttingEnabled)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
        if (smartShortTextOverride != null && !hasProgress) {
            builder.setContentText(smartShortTextOverride)
            builder.setShortCriticalText(limitIslandText(smartShortTextOverride, aospCuttingEnabled))
        }

        if (samsungBridge.enabled) {
            val samsungTexts = SamsungBridgeContentPolicy.resolve(
                sourcePackageName = sbn.packageName,
                hasCustomRemoteCard = samsungBridge.hasCustomRemoteCard,
                hasProgress = hasProgress,
                smartRuleId = smartRuleId,
                smartShortTextOverride = smartShortTextOverride,
                displayText = displayText,
                compactPrimaryText = compactPrimaryText,
                resolvedProgressChipText = resolvedProgressChipText,
                otpShortTextOverride = otpShortTextOverride,
                otpCode = otpOverride?.code,
                compactCodeOverride = compactCodeOverride,
                samsungReparseChipText = samsungReparse?.chipText,
                remoteViewMiniTextPair = null
            )
            SamsungNowBarApplier.apply(
                context = context,
                builder = builder,
                source = source,
                sourcePackageName = sbn.packageName,
                primaryText = compactPrimaryText,
                texts = samsungTexts,
                chipIcon = preferredChipIcon,
                nowBarIcon = nowBarAppIcon,
                rightIcon = nowBarRightIcon,
                hasProgress = hasProgress,
                progressValue = progressValue,
                progressMax = progressMax
            )
        }

        if (hyperBridgeEnabled) {
            val hyperTicker = when {
                otpOverride != null -> otpShortTextOverride ?: otpOverride.code
                !smartShortTextOverride.isNullOrBlank() -> smartShortTextOverride
                determinateProgressPercent != null -> "$determinateProgressPercent%"
                else -> displayTitle
            }
            HyperBridgeAdapter.apply(
                context = context,
                builder = builder,
                sourcePackageName = sbn.packageName,
                appName = appName,
                title = displayTitle,
                content = displayText,
                ticker = hyperTicker,
                progressPercent = determinateProgressPercent,
                largeIcon = preferredLargeIcon,
                fallbackSmallIcon = preferredSmallIcon,
                sourceActions = source.actions
            )
        }

        return builder.build()
    }

    private fun notifyWithPromotionFallback(
        context: Context,
        manager: NotificationManagerCompat,
        notificationId: Int,
        promotedNotification: Notification,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        progressOverride: ProgressOverride?,
        otpOverride: OtpMatch?,
        smartShortTextOverride: String?,
        compactCodeOverride: String? = null,
        smartRuleId: String? = null,
        otpShortTextOverride: String? = null,
        samsungBridge: SamsungBridgeContext = SamsungBridgeContext.disabled(
            sourceHasNativeProgress = false
        ),
        allowNavigationIconHeuristics: Boolean = true,
        preferMediaControls: Boolean = false,
        mediaPlaybackIsPlaying: Boolean? = null,
        titleOverride: String? = null,
        textOverride: String? = null,
        largeIconOverride: Bitmap? = null
    ) {
        try {
            manager.notify(notificationId, promotedNotification)
        } catch (error: Throwable) {
            val fallback = buildMirroredNotification(
                context = context,
                sbn = sbn,
                appPresentationOverride = appPresentationOverride,
                progressOverride = progressOverride,
                otpOverride = otpOverride,
                smartShortTextOverride = smartShortTextOverride,
                compactCodeOverride = compactCodeOverride,
                smartRuleId = smartRuleId,
                requestPromoted = false,
                otpShortTextOverride = otpShortTextOverride,
                samsungBridge = samsungBridge,
                allowNavigationIconHeuristics = allowNavigationIconHeuristics,
                preferMediaControls = preferMediaControls,
                mediaPlaybackIsPlaying = mediaPlaybackIsPlaying,
                titleOverride = titleOverride,
                textOverride = textOverride,
                largeIconOverride = largeIconOverride
            )
            manager.notify(notificationId, fallback)
        }
    }

    private fun detectSmartStage(
        packageName: String,
        source: Notification,
        parserDictionary: LiveParserDictionary,
        navigationEnabled: Boolean,
        weatherEnabled: Boolean,
        externalDevicesEnabled: Boolean,
        vpnEnabled: Boolean,
        hasNativeProgress: Boolean
    ): SmartStageMatch? {
        val isNavigationPackage = isLikelyNavigationPackage(packageName, parserDictionary)
        val packageLower = packageName.lowercase(Locale.ROOT)
        val isWeatherPackage = isLikelyWeatherPackage(packageLower, parserDictionary)
        val isExternalDevicePackage = isLikelySmartRulePackage(
            packageNameLower = packageLower,
            ruleId = "external_device",
            parserDictionary = parserDictionary
        )
        val isVpnPackage = isLikelyVpnPackage(
            packageNameLower = packageLower,
            parserDictionary = parserDictionary
        )
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = isNavigationPackage ||
                    isWeatherPackage ||
                    isExternalDevicePackage ||
                    isVpnPackage
        ).lowercase(Locale.ROOT)

        for (rule in parserDictionary.smartRules) {
            if (hasNativeProgress && rule.id != "weather") {
                continue
            }
            if (rule.id == "navigation" && !navigationEnabled) {
                continue
            }
            if (rule.id == "weather" && !weatherEnabled) {
                continue
            }
            if (rule.id == "external_device" && !externalDevicesEnabled) {
                continue
            }
            if (rule.id == "vpn" && !vpnEnabled) {
                continue
            }
            if (rule.id == "vpn" && !hasVpnSpeedPattern(combinedText, parserDictionary)) {
                continue
            }
            if (!rule.isRelevant(packageLower, combinedText)) {
                continue
            }
            if (rule.isExcluded(combinedText)) {
                continue
            }
            if (rule.id == "external_device" &&
                extractConnectedDeviceName(
                    text = combinedText,
                    parserDictionary = parserDictionary
                ).isNullOrBlank()
            ) {
                continue
            }

            val matchedSignal = rule.signals.firstOrNull { it.pattern.containsMatchIn(combinedText) } ?: continue
            val entityToken = when (rule.id) {
                "navigation" -> "route"
                "weather" -> "weather"
                "external_device" -> "device"
                "vpn" -> "vpn"
                else -> extractEntityToken(combinedText, parserDictionary)
            }
            val compactOrderCode = if (rule.id == "food") {
                extractCompactOrderCode(entityToken)
            } else {
                null
            }

            return SmartStageMatch(
                aggregateKey = "$packageLower:${rule.id}:$entityToken",
                stageValue = matchedSignal.stage,
                maxStage = rule.maxStage,
                compactOrderCode = compactOrderCode,
                keepHighestStage = rule.id != "navigation" &&
                        rule.id != "weather" &&
                        rule.id != "external_device" &&
                        rule.id != "vpn"
            )
        }

        if (weatherEnabled) {
            detectWeatherSmartStage(
                packageNameLower = packageLower,
                source = source,
                parserDictionary = parserDictionary
            )?.let { return it }
        }

        if (vpnEnabled) {
            detectVpnTrafficSmartStage(
                packageNameLower = packageLower,
                source = source,
                parserDictionary = parserDictionary
            )?.let { return it }
        }

        return null
    }

    private fun detectWeatherSmartStage(
        packageNameLower: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): SmartStageMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageNameLower,
            includeRemoteViewTexts = true
        )
        if (combinedText.isBlank()) {
            return null
        }
        val likelyWeatherPackage = isLikelyWeatherPackage(packageNameLower, parserDictionary)
        val hasWeatherContext = parserDictionary.weatherContextPattern.containsMatchIn(combinedText)
        if (!likelyWeatherPackage && !hasWeatherContext) {
            return null
        }

        val temperature = extractWeatherTemperatureFromText(combinedText, parserDictionary) ?: return null
        if (temperature.isBlank()) {
            return null
        }

        return SmartStageMatch(
            aggregateKey = "$packageNameLower:weather:weather",
            stageValue = 1,
            maxStage = 1,
            compactOrderCode = null,
            keepHighestStage = false
        )
    }

    private fun detectVpnTrafficSmartStage(
        packageNameLower: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): SmartStageMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageNameLower,
            includeRemoteViewTexts = true
        )
        if (combinedText.isBlank()) {
            return null
        }
        if (!hasVpnSpeedPattern(combinedText, parserDictionary)) {
            return null
        }

        val likelyVpnPackage = isLikelyVpnPackage(packageNameLower, parserDictionary)
        val hasVpnContext = parserDictionary.vpnContextPattern.containsMatchIn(combinedText)
        if (!likelyVpnPackage && !hasVpnContext) {
            return null
        }

        return SmartStageMatch(
            aggregateKey = "$packageNameLower:vpn:vpn",
            stageValue = 1,
            maxStage = 1,
            compactOrderCode = null,
            keepHighestStage = false
        )
    }

    private fun detectTextProgress(
        packageName: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): TextProgressMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = true
        )
        if (combinedText.isBlank()) {
            return null
        }

        val percentPattern = parserDictionary.textProgressPercentPattern
        val combinedLower = combinedText.lowercase(Locale.ROOT)
        val matches = percentPattern.findAll(combinedText)
        for (match in matches) {
            val percentValue = match.groupValues.getOrNull(1)?.toIntOrNull() ?: continue
            if (percentValue !in 0..100) {
                continue
            }
            if (!hasTextProgressContextHint(
                    textLower = combinedLower,
                    start = match.range.first,
                    endExclusive = match.range.last + 1,
                    parserDictionary = parserDictionary
                )
            ) {
                continue
            }
            if (isExcludedTextProgressContext(
                    textLower = combinedLower,
                    start = match.range.first,
                    endExclusive = match.range.last + 1,
                    parserDictionary = parserDictionary
                )
            ) {
                continue
            }
            return TextProgressMatch(
                percent = percentValue,
                shortText = "$percentValue%"
            )
        }
        return null
    }

    private fun hasTextProgressContextHint(
        textLower: String,
        start: Int,
        endExclusive: Int,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val contextWindow = parserDictionary.textProgressContextWindow
        val windowStart = (start - contextWindow).coerceAtLeast(0)
        val windowEnd = (endExclusive + contextWindow).coerceAtMost(textLower.length)
        val context = textLower.substring(windowStart, windowEnd)
        return parserDictionary.textProgressIncludeContextPattern.containsMatchIn(context)
    }

    private fun isExcludedTextProgressContext(
        textLower: String,
        start: Int,
        endExclusive: Int,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val contextWindow = parserDictionary.textProgressContextWindow
        val windowStart = (start - contextWindow).coerceAtLeast(0)
        val windowEnd = (endExclusive + contextWindow).coerceAtMost(textLower.length)
        val context = textLower.substring(windowStart, windowEnd)
        return parserDictionary.textProgressExcludeContextPattern.containsMatchIn(context)
    }

    private fun detectOtpCode(
        packageName: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): OtpMatch? {
        val combinedText = collectNotificationText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = false
        )
        if (combinedText.isBlank()) {
            return null
        }

        val combinedLower = combinedText.lowercase(Locale.ROOT)
        val hasStrongTrigger = parserDictionary.otpStrongTriggers.any(combinedLower::contains)
        val hasLooseTrigger = parserDictionary.otpLooseTriggerPattern.containsMatchIn(combinedLower)
        if (!hasStrongTrigger && !hasLooseTrigger) {
            return null
        }
        if (!hasStrongTrigger && looksLikeOrderContext(combinedLower, parserDictionary)) {
            return null
        }

        for (pattern in parserDictionary.otpCodePatterns) {
            for (match in pattern.findAll(combinedText)) {
                val rawValue = match.groupValues.getOrNull(1)?.ifBlank { match.value } ?: match.value
                val digits = rawValue.filter(Char::isDigit)
                if (digits.length !in OTP_CODE_LENGTH) {
                    continue
                }
                if (!hasOtpTokenBoundaries(combinedText, match.range.first, match.range.last + 1)) {
                    continue
                }
                if (isLikelyMoneyCandidate(combinedLower, match.range.first, match.range.last + 1, parserDictionary)) {
                    continue
                }
                if (looksLikeOrderContextAroundMatch(
                        combinedLower,
                        match.range.first,
                        match.range.last + 1,
                        parserDictionary
                    ) &&
                    !hasStrongTrigger
                ) {
                    continue
                }
                if (digits.length in OTP_CODE_LENGTH) {
                    return OtpMatch(
                        code = digits,
                        aggregateKey = otpAggregateKeyForCode(packageName, digits)
                    )
                }
            }
        }

        return null
    }

    private fun otpAggregateKeyForCode(packageName: String, code: String): String {
        return "otp:${packageName.lowercase(Locale.ROOT)}:$code"
    }

    private fun otpSourceKeyForPackage(packageName: String): String {
        return packageName.lowercase(Locale.ROOT)
    }

    private fun hasOtpTokenBoundaries(
        text: String,
        start: Int,
        endExclusive: Int
    ): Boolean {
        val left = if (start > 0) text[start - 1] else null
        val right = if (endExclusive < text.length) text[endExclusive] else null
        val leftOk = left == null || !left.isLetterOrDigit()
        val rightOk = right == null || !right.isLetterOrDigit()
        return leftOk && rightOk
    }

    private fun extractEntityToken(combinedText: String, parserDictionary: LiveParserDictionary): String {
        for (pattern in parserDictionary.entityTokenPatterns) {
            val match = pattern.find(combinedText) ?: continue
            val token = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (token.isNotEmpty()) {
                return token
            }
        }

        return "default"
    }

    private fun extractCompactOrderCode(token: String): String? {
        if (token == "default" || token.isBlank()) {
            return null
        }
        if (!token.any(Char::isDigit)) {
            return null
        }

        val compact = token
            .filter { it.isLetterOrDigit() || it == '-' }
            .uppercase(Locale.ROOT)
            .take(12)

        return compact.ifBlank { null }
    }

    private fun smartRuleIdFromAggregateKey(aggregateKey: String): String {
        val firstSeparator = aggregateKey.indexOf(':')
        if (firstSeparator < 0) {
            return ""
        }
        val secondSeparator = aggregateKey.indexOf(':', firstSeparator + 1)
        if (secondSeparator < 0) {
            return ""
        }
        return aggregateKey.substring(firstSeparator + 1, secondSeparator)
    }

    private fun smartShortStatusText(
        context: Context,
        ruleId: String,
        stageValue: Int,
        parserDictionary: LiveParserDictionary
    ): String? {
        return parserDictionary.resolveStatusText(
            ruleId = ruleId,
            stageValue = stageValue,
            isRussianLocale = isRussianLocale(context)
        )
    }

    private fun extractExternalDeviceStatusText(
        context: Context,
        notification: Notification,
        fallbackTitle: String,
        stageValue: Int,
        parserDictionary: LiveParserDictionary
    ): String? {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        val deviceName = extractConnectedDeviceName(
            text = combinedText,
            parserDictionary = parserDictionary
        )
        val statusText = parserDictionary.resolveStatusText(
            ruleId = "external_device",
            stageValue = stageValue,
            isRussianLocale = isRussianLocale(context)
        )

        return when {
            !deviceName.isNullOrBlank() && !statusText.isNullOrBlank() -> "$deviceName · $statusText"
            !deviceName.isNullOrBlank() -> deviceName
            else -> statusText
        }
    }

    private fun extractConnectedDeviceName(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        for (pattern in parserDictionary.externalDeviceNamePatterns) {
            val match = pattern.find(text) ?: continue
            val candidate = normalizeExternalDeviceName(
                raw = match.groupValues.getOrNull(1),
                parserDictionary = parserDictionary
            )
            if (!candidate.isNullOrBlank()) {
                return candidate
            }
        }
        return null
    }

    private fun normalizeExternalDeviceName(
        raw: String?,
        parserDictionary: LiveParserDictionary
    ): String? {
        val normalized = raw.orEmpty()
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('"', '\'', '«', '»', '.', ',', ':', ';')
        if (normalized.length < 2) {
            return null
        }
        val lower = normalized.lowercase(Locale.ROOT)
        if (lower in parserDictionary.externalDeviceGenericNames) {
            return null
        }
        return normalized
    }

    private fun extractVpnTrafficSpeeds(
        notification: Notification,
        fallbackTitle: String,
        parserDictionary: LiveParserDictionary
    ): VpnTrafficSpeeds? {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        return extractVpnTrafficSpeedsFromText(combinedText, parserDictionary)
    }

    private fun extractVpnTrafficSpeedsFromText(
        combinedText: String,
        parserDictionary: LiveParserDictionary
    ): VpnTrafficSpeeds? {
        if (combinedText.isBlank()) {
            return null
        }

        val fallbackSpeeds = parserDictionary.vpnSpeedPattern.findAll(combinedText)
            .map { normalizeVpnSpeedToken(it.value) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(2)
            .toList()

        var incoming = extractDirectionalVpnSpeed(
            text = combinedText,
            speedPattern = parserDictionary.vpnSpeedPattern,
            markers = parserDictionary.vpnDownloadMarkers
        )
        var outgoing = extractDirectionalVpnSpeed(
            text = combinedText,
            speedPattern = parserDictionary.vpnSpeedPattern,
            markers = parserDictionary.vpnUploadMarkers
        )
        if (!incoming.isNullOrBlank() || !outgoing.isNullOrBlank()) {
            if (outgoing.isNullOrBlank()) {
                outgoing = pickFallbackVpnSpeed(
                    candidates = fallbackSpeeds,
                    exclude = incoming
                )
            }
            if (incoming.isNullOrBlank()) {
                incoming = pickFallbackVpnSpeed(
                    candidates = fallbackSpeeds,
                    exclude = outgoing
                )
            }
            return VpnTrafficSpeeds(
                outgoingSpeed = outgoing,
                incomingSpeed = incoming
            )
        }

        if (fallbackSpeeds.isEmpty()) {
            return null
        }
        if (fallbackSpeeds.size == 1) {
            return VpnTrafficSpeeds(
                outgoingSpeed = fallbackSpeeds.first(),
                incomingSpeed = null
            )
        }
        return VpnTrafficSpeeds(
            outgoingSpeed = fallbackSpeeds[0],
            incomingSpeed = fallbackSpeeds[1]
        )
    }

    private fun pickFallbackVpnSpeed(
        candidates: List<String>,
        exclude: String?
    ): String? {
        if (candidates.isEmpty()) {
            return null
        }
        if (exclude.isNullOrBlank()) {
            return candidates.first()
        }
        val different = candidates.firstOrNull { !it.equals(exclude, ignoreCase = true) }
        return different ?: candidates.firstOrNull()
    }

    private fun formatDominantVpnTrafficText(vpnTraffic: VpnTrafficSpeeds?): String? {
        vpnTraffic ?: return null
        val outgoing = vpnTraffic.outgoingSpeed
        val incoming = vpnTraffic.incomingSpeed
        if (outgoing.isNullOrBlank() && incoming.isNullOrBlank()) {
            return null
        }
        if (outgoing.isNullOrBlank()) {
            return formatVpnIncomingToken(incoming)
        }
        if (incoming.isNullOrBlank()) {
            return formatVpnOutgoingToken(outgoing)
        }

        val outgoingMagnitude = parseVpnSpeedMagnitude(outgoing)
        val incomingMagnitude = parseVpnSpeedMagnitude(incoming)

        return when {
            outgoingMagnitude == null && incomingMagnitude == null ->
                formatVpnOutgoingToken(outgoing)

            outgoingMagnitude == null ->
                formatVpnIncomingToken(incoming)

            incomingMagnitude == null ->
                formatVpnOutgoingToken(outgoing)

            outgoingMagnitude > incomingMagnitude ->
                formatVpnOutgoingToken(outgoing)

            else ->
                formatVpnIncomingToken(incoming)
        }
    }

    private fun parseVpnSpeedMagnitude(speed: String?): Double? {
        val normalized = speed.orEmpty()
            .replace(" ", "")
            .replace(',', '.')
            .lowercase(Locale.ROOT)
        if (normalized.isBlank()) {
            return null
        }

        var numberEnd = 0
        while (numberEnd < normalized.length) {
            val ch = normalized[numberEnd]
            if (!ch.isDigit() && ch != '.') {
                break
            }
            numberEnd += 1
        }
        if (numberEnd == 0) {
            return null
        }

        val numericValue = normalized.substring(0, numberEnd).toDoubleOrNull() ?: return null
        val unitChar = normalized.drop(numberEnd).firstOrNull()
        val multiplier = when (unitChar) {
            'k', 'к' -> 1_000.0
            'm', 'м' -> 1_000_000.0
            'g', 'г' -> 1_000_000_000.0
            't', 'т' -> 1_000_000_000_000.0
            else -> 1.0
        }
        return numericValue * multiplier
    }

    private fun formatVpnOutgoingToken(speed: String?): String? {
        if (speed.isNullOrBlank()) {
            return null
        }
        return "↑$speed"
    }

    private fun formatVpnIncomingToken(speed: String?): String? {
        if (speed.isNullOrBlank()) {
            return null
        }
        return "↓$speed"
    }

    private fun extractDirectionalVpnSpeed(
        text: String,
        speedPattern: Regex,
        markers: Set<String>
    ): String? {
        var bestSpeed: String? = null
        var bestDistance: Int? = null
        for (match in speedPattern.findAll(text)) {
            val distance = nearestMarkerDistance(
                text = text,
                start = match.range.first,
                endExclusive = match.range.last + 1,
                markers = markers
            ) ?: continue
            val normalizedSpeed = normalizeVpnSpeedToken(match.value)
            if (normalizedSpeed.isBlank()) {
                continue
            }
            if (bestDistance == null || distance < bestDistance) {
                bestDistance = distance
                bestSpeed = normalizedSpeed
            }
        }
        return bestSpeed
    }

    private fun nearestMarkerDistance(
        text: String,
        start: Int,
        endExclusive: Int,
        markers: Set<String>
    ): Int? {
        if (markers.isEmpty() || text.isEmpty()) {
            return null
        }
        val windowStart = (start - 24).coerceAtLeast(0)
        val windowEnd = (endExclusive + 24).coerceAtMost(text.length)
        val context = text.substring(windowStart, windowEnd)

        var bestDistance: Int? = null
        for (marker in markers) {
            val ranges = markerRangesInContext(context, marker)
            for (range in ranges) {
                val markerStart = windowStart + range.first
                val markerEndExclusive = windowStart + range.last + 1
                val distance = when {
                    markerEndExclusive <= start -> start - markerEndExclusive
                    markerStart >= endExclusive -> markerStart - endExclusive
                    else -> 0
                }
                if (bestDistance == null || distance < bestDistance) {
                    bestDistance = distance
                }
            }
        }
        return bestDistance
    }

    private fun markerRangesInContext(context: String, marker: String): List<IntRange> {
        val normalized = marker.trim()
        if (normalized.isEmpty()) {
            return emptyList()
        }

        val hasWordChars = normalized.any { it.isLetterOrDigit() }
        if (hasWordChars) {
            return Regex("\\b${Regex.escape(normalized)}\\b", setOf(RegexOption.IGNORE_CASE))
                .findAll(context)
                .map { it.range }
                .toList()
        }

        val ranges = mutableListOf<IntRange>()
        var fromIndex = 0
        while (fromIndex < context.length) {
            val index = context.indexOf(normalized, fromIndex)
            if (index < 0) {
                break
            }
            ranges += index until (index + normalized.length)
            fromIndex = index + normalized.length
        }
        return ranges
    }

    private fun normalizeVpnSpeedToken(raw: String): String {
        return raw
            .replace(Regex("\\s+"), "")
            .replace("/с", "/s")
            .replace("/С", "/s")
            .replace("сек", "s", ignoreCase = true)
    }

    private fun extractNavigationDistanceText(
        notification: Notification,
        fallbackTitle: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        val match = parserDictionary.navigationDistancePattern.find(combinedText) ?: return null
        return match.value
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { null }
    }

    private fun extractWeatherTemperatureText(
        notification: Notification,
        fallbackTitle: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        return extractWeatherTemperatureFromText(combinedText, parserDictionary)
    }

    private fun extractWeatherTemperatureFromText(
        combinedText: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val match = parserDictionary.weatherTemperaturePattern.find(combinedText) ?: return null
        val rawNumber = match.groupValues.getOrNull(1).orEmpty()
            .replace('−', '-')
            .trim()
        if (rawNumber.isBlank()) {
            return null
        }
        val baseTemperature = "${rawNumber}°"
        val conditionEmoji = extractWeatherConditionEmoji(combinedText, parserDictionary)
        return if (conditionEmoji != null) {
            "$baseTemperature · $conditionEmoji"
        } else {
            baseTemperature
        }
    }

    private fun isRussianLocale(context: Context): Boolean {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        val language = locale?.language?.lowercase(Locale.ROOT).orEmpty()
        return language.startsWith("ru")
    }

    private fun isLikelyMoneyCandidate(
        textLower: String,
        start: Int,
        endExclusive: Int,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val windowStart = (start - 18).coerceAtLeast(0)
        val windowEnd = (endExclusive + 18).coerceAtMost(textLower.length)
        val context = textLower.substring(windowStart, windowEnd)
        return parserDictionary.moneyContextPattern.containsMatchIn(context)
    }

    private fun looksLikeOrderContext(textLower: String, parserDictionary: LiveParserDictionary): Boolean {
        return parserDictionary.orderContextHints.any(textLower::contains)
    }

    private fun looksLikeOrderContextAroundMatch(
        textLower: String,
        start: Int,
        endExclusive: Int,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val windowStart = (start - 24).coerceAtLeast(0)
        val windowEnd = (endExclusive + 24).coerceAtMost(textLower.length)
        val context = textLower.substring(windowStart, windowEnd)
        return looksLikeOrderContext(context, parserDictionary)
    }

    private fun shouldAutoCopyOtpLocked(
        state: OtpAggregateState,
        code: String,
        nowMs: Long
    ): Boolean {
        if (state.lastAutoCopiedCode != code) {
            state.lastAutoCopiedCode = code
            state.lastAutoCopiedAtMs = nowMs
            return true
        }

        if (nowMs - state.lastAutoCopiedAtMs >= OTP_REPEAT_SUPPRESS_MS) {
            state.lastAutoCopiedAtMs = nowMs
            return true
        }

        return false
    }

    private fun copyOtpToClipboard(context: Context, code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
    }

    private fun startOtpAutoCopyAnimation(
        context: Context,
        manager: NotificationManagerCompat,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        otpMatch: OtpMatch,
        samsungBridge: SamsungBridgeContext
    ) {
        val generation = synchronized(stateLock) {
            val nextGeneration = (otpAnimationGenerations[otpMatch.aggregateKey] ?: 0L) + 1L
            otpAnimationGenerations[otpMatch.aggregateKey] = nextGeneration
            nextGeneration
        }

        val copiedLabel = if (isRussianLocale(context)) "Скопировано" else "Copied"

        scheduleOtpAnimationStep(
            context = context,
            manager = manager,
            sbn = sbn,
            appPresentationOverride = appPresentationOverride,
            otpMatch = otpMatch,
            samsungBridge = samsungBridge,
            generation = generation,
            delayMs = OTP_AUTOCOPY_COPIED_SHOW_DELAY_MS,
            otpShortTextOverride = copiedLabel
        )

        scheduleOtpAnimationStep(
            context = context,
            manager = manager,
            sbn = sbn,
            appPresentationOverride = appPresentationOverride,
            otpMatch = otpMatch,
            samsungBridge = samsungBridge,
            generation = generation,
            delayMs = OTP_AUTOCOPY_COPIED_SHOW_DELAY_MS + OTP_AUTOCOPY_COPIED_SHOW_DURATION_MS,
            otpShortTextOverride = null
        )
    }

    private fun scheduleOtpAnimationStep(
        context: Context,
        manager: NotificationManagerCompat,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        otpMatch: OtpMatch,
        samsungBridge: SamsungBridgeContext,
        generation: Long,
        delayMs: Long,
        otpShortTextOverride: String?
    ) {
        mainHandler.postDelayed({
            if (!isOtpAnimationGenerationCurrent(otpMatch.aggregateKey, generation)) {
                return@postDelayed
            }
            try {
                val notification = buildMirroredNotification(
                    context = context,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    progressOverride = null,
                    otpOverride = otpMatch,
                    smartShortTextOverride = null,
                    requestPromoted = true,
                    otpShortTextOverride = otpShortTextOverride,
                    samsungBridge = samsungBridge
                )
                notifyWithPromotionFallback(
                    context = context,
                    manager = manager,
                    notificationId = mirrorIdForKey(otpMatch.aggregateKey),
                    promotedNotification = notification,
                    sbn = sbn,
                    appPresentationOverride = appPresentationOverride,
                    progressOverride = null,
                    otpOverride = otpMatch,
                    smartShortTextOverride = null,
                    otpShortTextOverride = otpShortTextOverride,
                    samsungBridge = samsungBridge
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed OTP auto-copy animation update: ${otpMatch.aggregateKey}", error)
            }
        }, delayMs)
    }

    private fun isOtpAnimationGenerationCurrent(aggregateKey: String, generation: Long): Boolean {
        return synchronized(stateLock) {
            val state = otpAggregateStates[aggregateKey] ?: return@synchronized false
            if (state.activeSbnKeys.isEmpty()) {
                return@synchronized false
            }
            otpAnimationGenerations[aggregateKey] == generation
        }
    }

    private fun buildSmartAnimatedIslandTokens(
        ruleId: String,
        notification: Notification,
        fallbackTitle: String,
        primaryStatus: String?,
        compactOrderCode: String?,
        parserDictionary: LiveParserDictionary
    ): List<String?> {
        val combinedText = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = true
        )
        return when (ruleId) {
            "food" -> {
                listOf(
                    primaryStatus,
                    compactOrderCode ?: extractCompactOrderCode(combinedText)
                )
            }

            "navigation" -> {
                listOf(
                    primaryStatus,
                    extractNavigationInstructionToken(combinedText, parserDictionary)
                )
            }

            "weather" -> {
                listOf(
                    extractWeatherDayToken(combinedText, parserDictionary),
                    primaryStatus,
                    extractWeatherConditionToken(combinedText, parserDictionary)
                )
            }

            else -> listOf(primaryStatus)
        }
    }

    private fun startSmartIslandAnimation(
        context: Context,
        manager: NotificationManagerCompat,
        aggregateKey: String,
        sbn: StatusBarNotification,
        appPresentationOverride: AppPresentationOverride,
        progressOverride: ProgressOverride?,
        smartRuleId: String,
        tokens: List<String?>,
        initialToken: String?,
        samsungBridge: SamsungBridgeContext
    ) {
        if (tokens.isEmpty()) {
            return
        }
        val aospCuttingEnabled = ConverterPrefs(context).getAospCuttingEnabled()
        val normalizedTokens = tokens.map { normalizeAnimatedToken(it, aospCuttingEnabled) }
        val normalizedInitial = normalizeAnimatedToken(initialToken, aospCuttingEnabled)
        val uniqueRenderableTokens = normalizedTokens
            .mapNotNull { it }
            .distinctBy { it.lowercase(Locale.ROOT) }
        val generationToStart = synchronized(stateLock) {
            if (uniqueRenderableTokens.size < 2) {
                smartAnimationGenerations.remove(aggregateKey)
                smartAnimationStates.remove(aggregateKey)
                return@synchronized null
            }

            val existingState = smartAnimationStates[aggregateKey]
            if (existingState != null && smartAnimationGenerations.containsKey(aggregateKey)) {
                existingState.sbn = sbn
                existingState.appPresentationOverride = appPresentationOverride
                existingState.progressOverride = progressOverride
                existingState.smartRuleId = smartRuleId
                existingState.tokens = normalizedTokens
                existingState.samsungBridge = samsungBridge
                if (!normalizedInitial.isNullOrBlank() &&
                    normalizedTokens.any { it.equals(normalizedInitial, ignoreCase = true) } &&
                    existingState.lastShownToken.isNullOrBlank()
                ) {
                    existingState.lastShownToken = normalizedInitial
                }
                return@synchronized null
            }

            val nextGeneration = (smartAnimationGenerations[aggregateKey] ?: 0L) + 1L
            smartAnimationGenerations[aggregateKey] = nextGeneration
            smartAnimationStates[aggregateKey] = SmartAnimationState(
                sbn = sbn,
                appPresentationOverride = appPresentationOverride,
                progressOverride = progressOverride,
                smartRuleId = smartRuleId,
                tokens = normalizedTokens,
                nextIndex = 0,
                lastShownToken = normalizedInitial,
                samsungBridge = samsungBridge
            )
            nextGeneration
        } ?: return

        scheduleSmartAnimationStep(
            context = context,
            manager = manager,
            aggregateKey = aggregateKey,
            generation = generationToStart
        )
    }

    private fun scheduleSmartAnimationStep(
        context: Context,
        manager: NotificationManagerCompat,
        aggregateKey: String,
        generation: Long
    ) {
        mainHandler.postDelayed({
            val frame = synchronized(stateLock) {
                if (!isSmartAnimationGenerationCurrentLocked(aggregateKey, generation)) {
                    return@synchronized null
                }
                if (!ConverterPrefs(context).getAnimatedIslandEnabled()) {
                    if (smartAnimationGenerations[aggregateKey] == generation) {
                        smartAnimationGenerations.remove(aggregateKey)
                    }
                    smartAnimationStates.remove(aggregateKey)
                    return@synchronized null
                }
                val animationState = smartAnimationStates[aggregateKey] ?: return@synchronized null
                val nextToken = pickNextSmartAnimationToken(
                    tokens = animationState.tokens,
                    startIndex = animationState.nextIndex,
                    lastShownToken = animationState.lastShownToken
                ) ?: return@synchronized null

                animationState.nextIndex = nextToken.nextIndex
                animationState.lastShownToken = nextToken.token
                SmartAnimationFrame(
                    sbn = animationState.sbn,
                    appPresentationOverride = animationState.appPresentationOverride,
                    progressOverride = animationState.progressOverride,
                    smartRuleId = animationState.smartRuleId,
                    token = nextToken.token,
                    samsungBridge = animationState.samsungBridge
                )
            } ?: return@postDelayed

            try {
                val notification = buildMirroredNotification(
                    context = context,
                    sbn = frame.sbn,
                    appPresentationOverride = frame.appPresentationOverride,
                    progressOverride = frame.progressOverride,
                    otpOverride = null,
                    smartShortTextOverride = frame.token,
                    smartRuleId = frame.smartRuleId,
                    requestPromoted = true,
                    samsungBridge = frame.samsungBridge
                )
                notifyWithPromotionFallback(
                    context = context,
                    manager = manager,
                    notificationId = mirrorIdForKey(aggregateKey),
                    promotedNotification = notification,
                    sbn = frame.sbn,
                    appPresentationOverride = frame.appPresentationOverride,
                    progressOverride = frame.progressOverride,
                    otpOverride = null,
                    smartShortTextOverride = frame.token,
                    smartRuleId = frame.smartRuleId,
                    samsungBridge = frame.samsungBridge
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed smart island animation update: $aggregateKey", error)
            }
            if (!isSmartAnimationGenerationCurrent(aggregateKey, generation)) {
                return@postDelayed
            }
            scheduleSmartAnimationStep(
                context = context,
                manager = manager,
                aggregateKey = aggregateKey,
                generation = generation
            )
        }, nextSmartIslandDelayMs())
    }

    private fun isSmartAnimationGenerationCurrent(aggregateKey: String, generation: Long): Boolean {
        return synchronized(stateLock) {
            isSmartAnimationGenerationCurrentLocked(aggregateKey, generation)
        }
    }

    private fun isSmartAnimationGenerationCurrentLocked(aggregateKey: String, generation: Long): Boolean {
        val state = aggregateStates[aggregateKey] ?: return false
        if (state.activeSbnKeys.isEmpty()) {
            return false
        }
        if (!smartAnimationStates.containsKey(aggregateKey)) {
            return false
        }
        return smartAnimationGenerations[aggregateKey] == generation
    }

    private fun pickNextSmartAnimationToken(
        tokens: List<String?>,
        startIndex: Int,
        lastShownToken: String?
    ): SmartAnimationToken? {
        if (tokens.isEmpty()) {
            return null
        }
        var index = ((startIndex % tokens.size) + tokens.size) % tokens.size
        var attemptsLeft = tokens.size
        while (attemptsLeft > 0) {
            val token = tokens[index]
            if (!token.isNullOrBlank() && !token.equals(lastShownToken, ignoreCase = true)) {
                return SmartAnimationToken(
                    token = token,
                    nextIndex = (index + 1) % tokens.size
                )
            }
            index = (index + 1) % tokens.size
            attemptsLeft -= 1
        }
        return null
    }

    private fun nextSmartIslandDelayMs(): Long {
        return Random.nextLong(
            SMART_ISLAND_ANIMATION_MIN_DELAY_MS,
            SMART_ISLAND_ANIMATION_MAX_DELAY_MS + 1L
        )
    }

    private fun normalizeAnimatedToken(raw: String?, aospCuttingEnabled: Boolean): String? {
        val normalized = raw.orEmpty()
            .replace(Regex("\\s+"), " ")
            .trim()
        val normalizedLengthSafe = safeTakeByGraphemes(normalized, SMART_ISLAND_TOKEN_MAX_LENGTH)
        if (normalizedLengthSafe.isBlank()) {
            return null
        }
        return limitIslandText(normalizedLengthSafe, aospCuttingEnabled)
            .trim()
            .ifBlank { null }
    }

    private fun extractNavigationInstructionToken(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val match = parserDictionary.navigationInstructionPattern.find(text) ?: return null
        return match.value
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { null }
    }

    private fun resolveRemoteViewMiniTextPair(
        notification: Notification,
        fallbackTitle: String,
        parserDictionary: LiveParserDictionary,
        smartRuleId: String?,
        compactPrimaryText: String,
        displayTitle: String,
        displayText: String,
        smartShortTextOverride: String?,
        hasProgress: Boolean
    ): SamsungMiniTextPair? {
        val remoteTexts = extractRemoteViewTexts(notification)
        if (remoteTexts.isEmpty()) {
            return null
        }

        if (smartRuleId == "navigation") {
            val combinedText = collectNotificationText(
                notification = notification,
                fallbackTitle = fallbackTitle,
                includeRemoteViewTexts = true
            )
            val primary = sequenceOf(
                smartShortTextOverride?.trim()?.takeIf {
                    isNavigationDistanceText(it, parserDictionary)
                },
                extractNavigationDistanceText(
                    notification = notification,
                    fallbackTitle = fallbackTitle,
                    parserDictionary = parserDictionary
                ),
                displayText.trim().takeIf { isNavigationDistanceText(it, parserDictionary) },
                compactPrimaryText.trim().takeIf { isNavigationDistanceText(it, parserDictionary) },
                remoteTexts.firstOrNull { candidate ->
                    isNavigationDistanceText(candidate, parserDictionary)
                }?.trim()
            ).firstOrNull { !it.isNullOrEmpty() }

            val secondary = sequenceOf(
                extractNavigationInstructionToken(combinedText, parserDictionary),
                displayTitle.trim(),
                displayText.trim(),
                compactPrimaryText.trim(),
                remoteTexts.firstOrNull { candidate ->
                    !isEquivalentText(candidate, primary) &&
                            !isNavigationDistanceText(candidate, parserDictionary)
                }?.trim()
            ).firstOrNull { candidate ->
                !candidate.isNullOrEmpty() &&
                        !isEquivalentText(candidate, primary) &&
                        !isNavigationDistanceText(candidate, parserDictionary)
            }

            return if (!primary.isNullOrBlank() && !secondary.isNullOrBlank()) {
                SamsungMiniTextPair(primaryText = primary, secondaryText = secondary)
            } else {
                null
            }
        }

        val primary = sequenceOf(
            compactPrimaryText.trim().takeIf { candidate ->
                candidate.isNotEmpty() && !isEquivalentText(candidate, fallbackTitle)
            },
            displayTitle.trim().takeIf { candidate ->
                candidate.isNotEmpty() && !isEquivalentText(candidate, fallbackTitle)
            },
            remoteTexts.firstOrNull()?.trim()
        ).firstOrNull { !it.isNullOrEmpty() } ?: return null

        val secondary = sequenceOf(
            smartShortTextOverride?.trim()?.takeIf { !hasProgress },
            remoteTexts.firstOrNull { candidate ->
                !isEquivalentText(candidate, primary)
            }?.trim(),
            displayText.trim().takeIf {
                it.isNotEmpty() && !isGenericLiveUpdatePlaceholder(it)
            }
        ).firstOrNull { candidate ->
            !candidate.isNullOrEmpty() && !isEquivalentText(candidate, primary)
        }

        return if (!secondary.isNullOrBlank()) {
            SamsungMiniTextPair(primaryText = primary, secondaryText = secondary)
        } else {
            null
        }
    }

    private fun isNavigationDistanceText(
        value: String?,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val normalized = value?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return false
        }
        return parserDictionary.navigationDistancePattern.containsMatchIn(normalized) ||
                NAVIGATION_DISTANCE_PATTERN.containsMatchIn(normalized)
    }

    private fun isEquivalentText(left: String?, right: String?): Boolean {
        val normalizedLeft = normalizeComparableText(left)
        val normalizedRight = normalizeComparableText(right)
        return normalizedLeft.isNotEmpty() &&
                normalizedRight.isNotEmpty() &&
                normalizedLeft == normalizedRight
    }

    private fun normalizeComparableText(value: String?): String {
        return value.orEmpty()
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase(Locale.ROOT)
    }

    private fun isGenericLiveUpdatePlaceholder(value: String?): Boolean {
        return isEquivalentText(value, "Live update in progress")
    }

    private fun extractWeatherDayToken(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val match = parserDictionary.weatherDayPattern.find(text) ?: return null
        return match.value.trim().ifBlank { null }
    }

    private fun extractWeatherConditionToken(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        val match = parserDictionary.weatherConditionPattern.find(text) ?: return null
        return match.value.trim().ifBlank { null }
    }

    private fun extractWeatherConditionEmoji(
        text: String,
        parserDictionary: LiveParserDictionary
    ): String? {
        return when {
            parserDictionary.weatherConditionThunderPattern.containsMatchIn(text) -> "\u26c8\ufe0f"
            parserDictionary.weatherConditionRainPattern.containsMatchIn(text) -> "\ud83c\udf27\ufe0f"
            parserDictionary.weatherConditionSnowPattern.containsMatchIn(text) -> "\u2744\ufe0f"
            parserDictionary.weatherConditionFogPattern.containsMatchIn(text) -> "\ud83c\udf2b\ufe0f"
            parserDictionary.weatherConditionWindPattern.containsMatchIn(text) -> "\ud83c\udf2c\ufe0f"
            parserDictionary.weatherConditionSunPattern.containsMatchIn(text) -> "\u2600\ufe0f"
            parserDictionary.weatherConditionCloudPattern.containsMatchIn(text) -> "\u2601\ufe0f"
            else -> null
        }
    }

    private fun applySmallIcon(
        context: Context,
        builder: NotificationCompat.Builder,
        sourceIcon: IconCompat?
    ) {
        builder.setSmallIcon(
            sourceIcon ?: IconCompat.createWithResource(context, R.drawable.ic_stat_liveupdate)
        )
    }

    private fun resolveSourceSmallIcon(context: Context, sbn: StatusBarNotification): IconCompat? {
        val source = sbn.notification
        val packageContext = runCatching {
            context.createPackageContext(sbn.packageName, 0)
        }.getOrNull()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val frameworkSmallIcon = source.smallIcon
            if (frameworkSmallIcon != null) {
                iconToBitmap(packageContext ?: context, frameworkSmallIcon)?.let { bitmap ->
                    runCatching { IconCompat.createWithBitmap(bitmap) }.getOrNull()?.let { return it }
                }
                try {
                    return IconCompat.createFromIcon(context, frameworkSmallIcon)
                } catch (_: Exception) {
                }
            }
        }

        val legacyIconRes = source.icon
        if (legacyIconRes == 0) {
            return null
        }

        packageContext?.let { packageCtx ->
            runCatching {
                packageCtx.getDrawable(legacyIconRes)?.let(::drawableToBitmap)
            }.getOrNull()?.let { bitmap ->
                runCatching { IconCompat.createWithBitmap(bitmap) }.getOrNull()?.let { return it }
            }
        }

        return try {
            IconCompat.createWithResource(
                (packageContext ?: context).resources,
                sbn.packageName,
                legacyIconRes
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveAppSmallIcon(context: Context, packageName: String): IconCompat? {
        return resolveAppIconAssets(context, packageName)?.smallIcon
    }

    private fun resolveAppLargeIconBitmap(context: Context, packageName: String): Bitmap? {
        return resolveAppIconAssets(context, packageName)?.largeIconBitmap
    }

    private fun resolveAppIconAssets(context: Context, packageName: String): AppIconAssets? {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) {
            return null
        }

        synchronized(appIconCacheLock) {
            appIconCache[normalizedPackage]?.let { return it }
            if (missingAppIconPackages.contains(normalizedPackage)) {
                return null
            }
        }

        val resolved = try {
            val appInfo = context.packageManager.getApplicationInfo(normalizedPackage, 0)
            if (appInfo.icon == 0) {
                null
            } else {
                val packageContext = context.createPackageContext(normalizedPackage, 0)
                val resourceIcon = runCatching {
                    IconCompat.createWithResource(
                        packageContext.resources,
                        normalizedPackage,
                        appInfo.icon
                    )
                }.getOrNull()
                val bitmap = runCatching {
                    packageContext.getDrawable(appInfo.icon)?.let { drawable ->
                        drawableToBitmap(drawable, clipAdaptiveIcon = true)
                    }
                }.getOrNull()
                val smallIcon = resourceIcon
                    ?: bitmap?.let { runCatching { IconCompat.createWithBitmap(it) }.getOrNull() }

                if (smallIcon == null && bitmap == null) {
                    null
                } else {
                    AppIconAssets(
                        smallIcon = smallIcon,
                        largeIconBitmap = bitmap
                    )
                }
            }
        } catch (_: Exception) {
            null
        }

        synchronized(appIconCacheLock) {
            if (resolved != null) {
                appIconCache[normalizedPackage] = resolved
            } else {
                missingAppIconPackages.add(normalizedPackage)
            }
        }

        return resolved
    }

    private fun resolveSourceLargeIconBitmap(context: Context, notification: Notification): Bitmap? {
        val extras = notification.extras
        val fromExtras = extras.get(Notification.EXTRA_LARGE_ICON)
        when (fromExtras) {
            is Bitmap -> return fromExtras
            is android.graphics.drawable.Icon -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    iconToBitmap(context, fromExtras)?.let { return it }
                }
            }
        }

        val fromBigExtras = extras.get(Notification.EXTRA_LARGE_ICON_BIG)
        when (fromBigExtras) {
            is Bitmap -> return fromBigExtras
            is android.graphics.drawable.Icon -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    iconToBitmap(context, fromBigExtras)?.let { return it }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.getLargeIcon()?.let { return iconToBitmap(context, it) }
        }

        @Suppress("DEPRECATION")
        return notification.largeIcon
    }

    private fun resolveRemoteDrawableAssets(
        context: Context,
        sbn: StatusBarNotification
    ): RemoteDrawableAssets? {
        val packageContext = try {
            context.createPackageContext(sbn.packageName, 0)
        } catch (_: Exception) {
            return null
        }

        val resources = packageContext.resources
        val source = sbn.notification
        val drawableResId =
            extractFirstRemoteDrawableResId(source.contentView, resources)
                ?: extractFirstRemoteDrawableResId(source.bigContentView, resources)
                ?: extractFirstRemoteDrawableResId(source.headsUpContentView, resources)
                ?: return null

        val rawBitmap = try {
            packageContext.getDrawable(drawableResId)?.let { drawable ->
                drawableToBitmap(drawable)
            }
        } catch (_: Exception) {
            null
        }
        val bitmap = rawBitmap?.let(::tintBitmapWhite)
        val icon = bitmap?.let {
            runCatching { IconCompat.createWithBitmap(it) }.getOrNull()
        } ?: runCatching {
            IconCompat.createWithResource(resources, sbn.packageName, drawableResId)
        }.getOrNull()

        if (icon == null && bitmap == null) {
            return null
        }
        return RemoteDrawableAssets(
            icon = icon,
            bitmap = bitmap
        )
    }

    private fun iconToBitmap(context: Context, icon: android.graphics.drawable.Icon): Bitmap? {
        return try {
            icon.loadDrawable(context)?.let(::drawableToBitmap)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(
        drawable: Drawable,
        clipAdaptiveIcon: Boolean = false
    ): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        if (clipAdaptiveIcon &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            drawable is AdaptiveIconDrawable
        ) {
            return adaptiveIconToBitmap(drawable)
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(512)
        val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(512)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun adaptiveIconToBitmap(drawable: AdaptiveIconDrawable): Bitmap {
        val size = maxOf(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1)
        ).coerceAtMost(512)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)

        val originalMask = Path(drawable.iconMask)
        val maskBounds = RectF()
        originalMask.computeBounds(maskBounds, true)
        if (maskBounds.width() <= 0f || maskBounds.height() <= 0f) {
            drawable.draw(canvas)
            return bitmap
        }

        val normalizedBounds = RectF(0f, 0f, maskBounds.width(), maskBounds.height())
        val scale = minOf(
            size / normalizedBounds.width(),
            size / normalizedBounds.height()
        )
        val dx = (size - normalizedBounds.width() * scale) / 2f
        val dy = (size - normalizedBounds.height() * scale) / 2f
        val maskMatrix = Matrix().apply {
            postTranslate(-maskBounds.left, -maskBounds.top)
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        val scaledMask = Path()
        originalMask.transform(maskMatrix, scaledMask)

        canvas.save()
        canvas.clipPath(scaledMask)
        drawable.draw(canvas)
        canvas.restore()
        return bitmap
    }

    private fun tintBitmapWhite(source: Bitmap): Bitmap {
        val mutableSource =
            if (source.config == Bitmap.Config.ARGB_8888 && source.isMutable) {
                source
            } else {
                source.copy(Bitmap.Config.ARGB_8888, true)
            } ?: source

        val result = Bitmap.createBitmap(
            mutableSource.width,
            mutableSource.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(mutableSource, 0f, 0f, paint)
        return result
    }

    private fun extractRemoteViewTexts(notification: Notification): List<String> {
        val values = linkedSetOf<String>()
        val remoteViews = listOfNotNull(
            notification.contentView,
            notification.bigContentView,
            notification.headsUpContentView
        )

        for (rv in remoteViews) {
            val actions = getRemoteViewActions(rv)
            for (action in actions) {
                val fields = collectAllDeclaredFields(action.javaClass)
                val methodName = fields.firstNotNullOfOrNull { field ->
                    val normalized = field.name.removePrefix("m").lowercase(Locale.ROOT)
                    if (normalized != "methodname") {
                        null
                    } else {
                        runCatching {
                            field.isAccessible = true
                            field.get(action) as? String
                        }.getOrNull()
                    }
                }?.lowercase(Locale.ROOT).orEmpty()
                val likelyTextAction =
                    methodName.contains("settext") || methodName.contains("setcharsequence")

                for (field in fields) {
                    val value = runCatching {
                        field.isAccessible = true
                        field.get(action)
                    }.getOrNull()
                    when (value) {
                        is CharSequence -> {
                            val normalized = value.toString().trim()
                            if (normalized.isNotEmpty()) {
                                if (likelyTextAction || field.name.contains("text", ignoreCase = true)) {
                                    values.add(normalized)
                                }
                            }
                        }

                        is Array<*> -> {
                            value.filterIsInstance<CharSequence>()
                                .map { it.toString().trim() }
                                .filter { it.isNotEmpty() }
                                .forEach(values::add)
                        }
                    }
                }
            }
        }
        return values.toList()
    }

    private fun extractFirstRemoteDrawableResId(
        rv: android.widget.RemoteViews?,
        resources: android.content.res.Resources
    ): Int? {
        val actions = getRemoteViewActions(rv)
        if (actions.isEmpty()) {
            return null
        }

        for (action in actions) {
            val fields = collectAllDeclaredFields(action.javaClass)
            val actionClassName = action.javaClass.name.lowercase(Locale.ROOT)
            var methodName = ""
            val candidates = mutableListOf<Pair<String, Int>>()

            for (field in fields) {
                val value = runCatching {
                    field.isAccessible = true
                    field.get(action)
                }.getOrNull() ?: continue
                val normalizedName = field.name.removePrefix("m").lowercase(Locale.ROOT)
                if (normalizedName == "methodname" && value is String) {
                    methodName = value.lowercase(Locale.ROOT)
                }
                candidates.addAll(extractDrawableResIdCandidates(value, normalizedName))
            }

            val looksLikeImageAction =
                methodName.contains("icon") ||
                        methodName.contains("image") ||
                        methodName.contains("drawable") ||
                        actionClassName.contains("icon") ||
                        actionClassName.contains("image") ||
                        actionClassName.contains("drawable")
            if (!looksLikeImageAction) {
                continue
            }

            for ((fieldName, resId) in candidates) {
                val isResourceField =
                    fieldName.contains("res") ||
                            fieldName.contains("icon") ||
                            fieldName.contains("drawable") ||
                            fieldName.contains("value")
                if (!isResourceField) {
                    continue
                }
                if (isDrawableResource(resources, resId)) {
                    return resId
                }
            }
        }
        return null
    }

    private fun extractDrawableResIdCandidates(value: Any, fieldName: String): List<Pair<String, Int>> {
        val candidates = mutableListOf<Pair<String, Int>>()
        when (value) {
            is Int -> {
                if (value > 0) {
                    candidates += fieldName to value
                }
            }

            is IntArray -> {
                value.filter { it > 0 }.forEachIndexed { index, item ->
                    candidates += "$fieldName:$index" to item
                }
            }

            is Array<*> -> {
                value.forEachIndexed { index, item ->
                    if (item != null) {
                        candidates += extractDrawableResIdCandidates(item, "$fieldName:$index")
                    }
                }
            }

            is List<*> -> {
                value.forEachIndexed { index, item ->
                    if (item != null) {
                        candidates += extractDrawableResIdCandidates(item, "$fieldName:$index")
                    }
                }
            }

            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    value is android.graphics.drawable.Icon &&
                    value.type == android.graphics.drawable.Icon.TYPE_RESOURCE
                ) {
                    val resId = value.resId
                    if (resId > 0) {
                        candidates += "$fieldName:icon" to resId
                    }
                }
            }
        }
        return candidates
    }

    private fun getRemoteViewActions(rv: android.widget.RemoteViews?): List<Any> {
        rv ?: return emptyList()
        return try {
            val actionsField = rv.javaClass.getDeclaredField("mActions")
            actionsField.isAccessible = true
            (actionsField.get(rv) as? List<*>)?.filterNotNull() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun collectAllDeclaredFields(clazz: Class<*>): List<java.lang.reflect.Field> {
        val fields = mutableListOf<java.lang.reflect.Field>()
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            fields.addAll(current.declaredFields)
            current = current.superclass
        }
        return fields
    }

    private fun isDrawableResource(resources: android.content.res.Resources, resId: Int): Boolean {
        return try {
            val typeName = resources.getResourceTypeName(resId)
            typeName == "drawable" || typeName == "mipmap"
        } catch (_: Exception) {
            false
        }
    }

    private fun buildCopyOtpAction(
        context: Context,
        sbn: StatusBarNotification,
        otpCode: String
    ): NotificationCompat.Action {
        val copyIntent = Intent(context, OtpCopyReceiver::class.java).apply {
            action = OtpCopyReceiver.ACTION_COPY_OTP
            putExtra(OtpCopyReceiver.EXTRA_OTP_CODE, otpCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mirrorIdForKey("${sbn.key}:otp_copy"),
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_content_copy_24),
            otpActionLabel(context),
            pendingIntent
        ).build()
    }

    private fun otpActionLabel(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        val language = locale?.language?.lowercase(Locale.ROOT).orEmpty()
        return if (language.startsWith("ru")) "Скопировать код" else "Copy code"
    }

    private fun copySourceActions(
        source: Notification,
        builder: NotificationCompat.Builder,
        maxActions: Int,
        preferMediaControls: Boolean = false,
        mediaPlaybackIsPlaying: Boolean? = null
    ) {
        val actions = source.actions ?: return
        if (actions.isEmpty()) {
            return
        }

        val safeMaxActions = maxActions.coerceAtLeast(0)
        if (safeMaxActions == 0) {
            return
        }

        if (preferMediaControls) {
            val preferredMediaActions = selectPreferredMediaActions(
                actions = actions.toList(),
                isPlaying = mediaPlaybackIsPlaying
            )
            if (preferredMediaActions.isNotEmpty()) {
                preferredMediaActions
                    .take(safeMaxActions)
                    .forEach { preferredAction ->
                        val compatAction = toCompatAction(
                            frameworkAction = preferredAction.action,
                            titleOverride = preferredAction.shortTitle
                        ) ?: return@forEach
                        builder.addAction(compatAction)
                    }
                return
            }
        }

        actions.take(safeMaxActions).forEach { frameworkAction ->
            val compatAction = toCompatAction(frameworkAction) ?: return@forEach
            builder.addAction(compatAction)
        }
    }

    private fun selectPreferredMediaActions(
        actions: List<Notification.Action>,
        isPlaying: Boolean?
    ): List<MediaPreferredAction> {
        if (actions.isEmpty()) {
            return emptyList()
        }

        val indexed = actions.withIndex()
            .filter { it.value.actionIntent != null }
            .toList()
        if (indexed.isEmpty()) {
            return emptyList()
        }

        val usedIndexes = mutableSetOf<Int>()

        fun pickByKeywords(keywords: List<String>): Notification.Action? {
            val candidate = indexed.firstOrNull { (index, action) ->
                if (index in usedIndexes) {
                    return@firstOrNull false
                }
                val title = action.title?.toString()?.lowercase(Locale.ROOT).orEmpty()
                keywords.any(title::contains)
            } ?: return null
            usedIndexes += candidate.index
            return candidate.value
        }

        val previousAction = pickByKeywords(
            listOf("previous", "prev", "назад", "пред", "rewind", "⏮")
        )
        val pauseAction = pickByKeywords(
            listOf("pause", "пауза", "⏸")
        )
        val playAction = pickByKeywords(
            listOf("play", "играть", "воспроиз", "resume", "▶", "⏯")
        )
        val nextAction = pickByKeywords(
            listOf("next", "след", "skip", "forward", "⏭")
        )

        val centerAction = when (isPlaying) {
            true -> pauseAction ?: playAction
            false -> playAction ?: pauseAction
            null -> pauseAction ?: playAction
        }

        val centerShortTitle = when {
            centerAction != null && centerAction == playAction -> "Play"
            centerAction != null && centerAction == pauseAction -> "Pause"
            isPlaying == false -> "Play"
            else -> "Pause"
        }

        val ordered = listOfNotNull(
            previousAction?.let { MediaPreferredAction(it, "Previous") },
            centerAction?.let {
                MediaPreferredAction(
                    action = it,
                    shortTitle = centerShortTitle
                )
            },
            nextAction?.let { MediaPreferredAction(it, "Next") }
        )
        return if (ordered.size >= 2) ordered else emptyList()
    }

    private fun toCompatAction(
        frameworkAction: Notification.Action,
        titleOverride: String? = null
    ): NotificationCompat.Action? {
        if (frameworkAction.actionIntent == null) {
            return null
        }

        return try {
            val copied = NotificationCompat.Action.Builder.fromAndroidAction(frameworkAction).build()
            NotificationCompat.Action.Builder(
                transparentActionIcon,
                titleOverride?.takeIf { it.isNotBlank() }
                    ?: copied.title?.toString()?.takeIf { it.isNotBlank() }
                    ?: frameworkAction.title?.toString()?.takeIf { it.isNotBlank() }
                    ?: "Action",
                copied.actionIntent ?: frameworkAction.actionIntent
            ).build()
        } catch (_: Exception) {
            val title = titleOverride?.takeIf { it.isNotBlank() }
                ?: frameworkAction.title?.toString()?.takeIf { it.isNotBlank() }
                ?: "Action"
            NotificationCompat.Action.Builder(
                transparentActionIcon,
                title,
                frameworkAction.actionIntent
            ).build()
        }
    }

    private fun hasProgress(notification: Notification): Boolean {
        val extras = notification.extras
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        return max > 0 || indeterminate
    }

    private fun extractTitle(
        notification: Notification,
        fallbackName: String,
        allowRemoteViewFallback: Boolean
    ): String {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        val normalizedTitle = title?.toString()?.takeIf { it.isNotBlank() }
        if (normalizedTitle != null) {
            return normalizedTitle
        }
        if (!allowRemoteViewFallback) {
            return fallbackName
        }
        val remoteTitle = extractRemoteViewTexts(notification).firstOrNull()
        return remoteTitle ?: fallbackName
    }

    private fun extractText(notification: Notification, allowRemoteViewFallback: Boolean): String {
        val extras = notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        val normalized = text?.toString()?.takeIf { it.isNotBlank() }
        if (normalized != null) {
            return normalized
        }
        if (!allowRemoteViewFallback) {
            return "Live update in progress"
        }
        val remoteText = extractRemoteViewTexts(notification).firstOrNull()
        return remoteText ?: "Live update in progress"
    }

    private fun collectNotificationText(
        notification: Notification,
        fallbackTitle: String,
        includeRemoteViewTexts: Boolean
    ): String {
        val extras = notification.extras
        val parts = mutableListOf<String>()

        fun add(value: CharSequence?) {
            val text = value?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
                parts.add(text)
            }
        }

        add(extras.getCharSequence(Notification.EXTRA_TITLE))
        add(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        add(extras.getCharSequence(Notification.EXTRA_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.forEach(::add)
        if (includeRemoteViewTexts) {
            extractRemoteViewTexts(notification).forEach { add(it) }
        }

        if (parts.isEmpty()) {
            parts.add(fallbackTitle)
        }

        return parts
            .distinct()
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isLikelyMediaPlaybackNotification(notification: Notification): Boolean {
        if (notification.category == Notification.CATEGORY_TRANSPORT) {
            return true
        }
        val extras = notification.extras
        if (extras.get(Notification.EXTRA_MEDIA_SESSION) != null) {
            return true
        }
        val template = extras.getString("android.template")
        return template?.contains("MediaStyle", ignoreCase = true) == true
    }

    private fun extractMediaPlaybackSnapshot(
        context: Context,
        notification: Notification,
        sourcePackageName: String
    ): MediaPlaybackSnapshot? {
        val sessionToken = extractMediaSessionToken(notification)
        val mediaController = resolveMediaController(
            context = context,
            sessionToken = sessionToken,
            sourcePackageName = sourcePackageName
        ) ?: return null

        return try {
            val playbackState = mediaController.playbackState ?: return null
            if (playbackState.state == PlaybackState.STATE_STOPPED ||
                playbackState.state == PlaybackState.STATE_NONE
            ) {
                return null
            }

            val metadata = mediaController.metadata
            val durationMs =
                metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
            val rawPositionMs = resolvePlaybackStatePositionMs(playbackState).coerceAtLeast(0L)
            val positionMs = if (durationMs > 0L) {
                rawPositionMs.coerceIn(0L, durationMs)
            } else {
                rawPositionMs
            }
            val title = metadata
                ?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?.trim()
                ?.ifBlank { null }
            val artist = metadata
                ?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?.trim()
                ?.ifBlank { null }
                ?: metadata
                    ?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                    ?.trim()
                    ?.ifBlank { null }
            val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

            MediaPlaybackSnapshot(
                title = title,
                artist = artist,
                albumArt = albumArt,
                durationMs = durationMs,
                positionMs = positionMs,
                isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to resolve media playback snapshot", error)
            null
        }
    }

    private fun resolveMediaController(
        context: Context,
        sessionToken: MediaSession.Token?,
        sourcePackageName: String
    ): MediaController? {
        if (sessionToken != null) {
            try {
                return MediaController(context, sessionToken)
            } catch (_: Throwable) {
            }
        }

        return try {
            val mediaSessionManager =
                context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                    ?: return null
            val componentName = ComponentName(
                context,
                LiveUpdateNotificationListenerService::class.java
            )
            val activeControllers = mediaSessionManager.getActiveSessions(componentName)
            activeControllers.firstOrNull { it.packageName == sourcePackageName }
                ?: activeControllers.firstOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractMediaSessionToken(notification: Notification): MediaSession.Token? {
        val extras = notification.extras
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolvePlaybackStatePositionMs(playbackState: PlaybackState): Long {
        val basePositionMs = playbackState.position.coerceAtLeast(0L)
        if (playbackState.state != PlaybackState.STATE_PLAYING) {
            return basePositionMs
        }

        val lastUpdateElapsedMs = playbackState.lastPositionUpdateTime
        if (lastUpdateElapsedMs <= 0L) {
            return basePositionMs
        }

        val elapsedSinceUpdateMs =
            (SystemClock.elapsedRealtime() - lastUpdateElapsedMs).coerceAtLeast(0L)
        val speed = playbackState.playbackSpeed.takeIf { it > 0f } ?: 1f
        return (basePositionMs + (elapsedSinceUpdateMs * speed)).toLong()
    }

    private fun MediaPlaybackSnapshot.toProgressOverride(): ProgressOverride? {
        if (durationMs <= 0L) {
            return null
        }
        val max = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1)
        val value = positionMs.coerceIn(0L, max.toLong()).toInt()
        return ProgressOverride(value = value, max = max)
    }

    private fun buildMediaPlaybackShortText(snapshot: MediaPlaybackSnapshot): String {
        if (!snapshot.title.isNullOrBlank()) {
            return snapshot.title
        }
        if (!snapshot.artist.isNullOrBlank()) {
            return snapshot.artist
        }
        if (snapshot.durationMs > 0L) {
            return formatMillisecondsAsClock(snapshot.positionMs)
        }
        return if (snapshot.isPlaying) "PLAY" else "PAUSE"
    }

    private fun formatMillisecondsAsClock(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }

    private fun resolveAppName(context: Context, packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun resolveStableWhen(source: Notification, fallbackPostTime: Long): Long {
        val sourceWhen = source.`when`
        return if (sourceWhen > 0L) {
            sourceWhen
        } else {
            fallbackPostTime
        }
    }

    private fun isLikelyNavigationPackage(
        packageName: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val packageLower = packageName.lowercase(Locale.ROOT)
        if (parserDictionary.knownNavigationPackages.contains(packageLower)) {
            return true
        }
        return parserDictionary.navigationPackageMarkers.any(packageLower::contains)
    }

    private fun isLikelyWeatherPackage(
        packageNameLower: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        return parserDictionary.weatherPackageHints.any(packageNameLower::contains)
    }

    private fun isLikelySmartRulePackage(
        packageNameLower: String,
        ruleId: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val targetRule = parserDictionary.smartRules.firstOrNull { it.id == ruleId } ?: return false
        return targetRule.packageHints.any(packageNameLower::contains)
    }

    private fun isLikelyVpnPackage(
        packageNameLower: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        if (isLikelySmartRulePackage(packageNameLower, "vpn", parserDictionary)) {
            return true
        }
        return parserDictionary.vpnPackageMarkers.any(packageNameLower::contains)
    }

    private fun shouldSuppressVpnWithoutTraffic(
        packageName: String,
        source: Notification,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        val packageLower = packageName.lowercase(Locale.ROOT)
        val combinedText = collectVpnDetectionText(
            notification = source,
            fallbackTitle = packageName,
            includeRemoteViewTexts = true
        )
        if (combinedText.isBlank()) {
            return false
        }
        if (hasVpnSpeedPattern(combinedText, parserDictionary)) {
            return false
        }
        val likelyVpnPackage = isLikelyVpnPackage(packageLower, parserDictionary)
        val hasVpnContext = parserDictionary.vpnContextPattern.containsMatchIn(combinedText)
        return likelyVpnPackage || hasVpnContext
    }

    private fun collectVpnDetectionText(
        notification: Notification,
        fallbackTitle: String,
        includeRemoteViewTexts: Boolean
    ): String {
        val base = collectNotificationText(
            notification = notification,
            fallbackTitle = fallbackTitle,
            includeRemoteViewTexts = includeRemoteViewTexts
        )
        val parts = mutableListOf<String>()
        if (base.isNotBlank()) {
            parts += base
        }
        notification.tickerText?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(parts::add)
        notification.channelId?.trim()?.takeIf { it.isNotBlank() }?.let(parts::add)
        notification.group?.trim()?.takeIf { it.isNotBlank() }?.let(parts::add)
        notification.sortKey?.trim()?.takeIf { it.isNotBlank() }?.let(parts::add)
        notification.category?.trim()?.takeIf { it.isNotBlank() }?.let(parts::add)
        notification.actions
            ?.mapNotNull { it.title?.toString()?.trim() }
            ?.filter { it.isNotBlank() }
            ?.forEach(parts::add)

        if (parts.isEmpty()) {
            return ""
        }
        return parts
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun hasVpnSpeedPattern(
        text: String,
        parserDictionary: LiveParserDictionary
    ): Boolean {
        if (text.isBlank()) {
            return false
        }
        return parserDictionary.vpnSpeedPattern.containsMatchIn(text)
    }

    private fun mirrorIdForKey(key: String): Int {
        val value = key.hashCode()
        return if (value == Int.MIN_VALUE) 0 else abs(value)
    }

    private fun limitIslandText(value: String?, enabled: Boolean): String {
        val normalized = value.orEmpty()
        if (!enabled) {
            return normalized
        }
        return safeTakeByGraphemes(normalized, AOSP_ISLAND_TEXT_LIMIT)
    }

    private fun safeTakeByGraphemes(value: String, maxGraphemes: Int): String {
        if (maxGraphemes <= 0 || value.isEmpty()) {
            return ""
        }

        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(value)

        var endIndex = 0
        var consumed = 0
        while (consumed < maxGraphemes) {
            val nextBoundary = iterator.next()
            if (nextBoundary == BreakIterator.DONE) {
                break
            }
            endIndex = nextBoundary
            consumed += 1
        }
        return if (endIndex > 0) value.substring(0, endIndex) else ""
    }

    private fun clearAggregateTrackingForSbnKeyLocked(sbnKey: String): List<Int> {
        val idsToCancel = mutableListOf<Int>()
        idsToCancel.addAll(clearSmartTrackingForSbnKeyLocked(sbnKey))
        idsToCancel.addAll(clearOtpTrackingForSbnKeyLocked(sbnKey))
        return idsToCancel
    }

    private fun clearSmartTrackingForSbnKeyLocked(sbnKey: String): List<Int> {
        val idsToCancel = mutableListOf<Int>()
        val smartAggregateKey = sbnToAggregateKey.remove(sbnKey)
        if (smartAggregateKey != null) {
            val state = aggregateStates[smartAggregateKey]
            if (state != null) {
                state.activeSbnKeys.remove(sbnKey)
                state.sourcesBySbnKey.remove(sbnKey)
                if (state.activeSbnKeys.isEmpty()) {
                    aggregateStates.remove(smartAggregateKey)
                    smartAnimationGenerations.remove(smartAggregateKey)
                    smartAnimationStates.remove(smartAggregateKey)
                    idsToCancel.add(mirrorIdForKey(smartAggregateKey))
                }
            } else {
                smartAnimationGenerations.remove(smartAggregateKey)
                smartAnimationStates.remove(smartAggregateKey)
                idsToCancel.add(mirrorIdForKey(smartAggregateKey))
            }
        }
        return idsToCancel
    }

    private fun selectSmartSourceEntryLocked(
        aggregateState: AggregateState,
        keepHighestStage: Boolean
    ): SmartSourceEntry? {
        if (aggregateState.sourcesBySbnKey.isEmpty()) {
            return null
        }
        val activeEntries = aggregateState.activeSbnKeys
            .mapNotNull(aggregateState.sourcesBySbnKey::get)
        if (activeEntries.isEmpty()) {
            aggregateState.sourcesBySbnKey.clear()
            return null
        }
        return if (keepHighestStage) {
            activeEntries.maxWithOrNull(
                compareBy<SmartSourceEntry>(
                    { it.stageValue },
                    { it.postTimeMs },
                    { it.sbn.key }
                )
            )
        } else {
            activeEntries.maxWithOrNull(
                compareBy<SmartSourceEntry>(
                    { it.postTimeMs },
                    { it.stageValue },
                    { it.sbn.key }
                )
            )
        }
    }

    private fun clearOtpTrackingForSbnKeyLocked(sbnKey: String): List<Int> {
        val idsToCancel = mutableListOf<Int>()
        val sourceKey = sbnToOtpSourceKey.remove(sbnKey)
        val otpAggregateKey = sbnToOtpAggregateKey.remove(sbnKey)
        if (otpAggregateKey != null) {
            val state = otpAggregateStates[otpAggregateKey]
            if (state != null) {
                state.activeSbnKeys.remove(sbnKey)
                if (state.activeSbnKeys.isEmpty()) {
                    otpAggregateStates.remove(otpAggregateKey)
                    otpAnimationGenerations.remove(otpAggregateKey)
                    idsToCancel.add(mirrorIdForKey(otpAggregateKey))
                }
            } else {
                otpAnimationGenerations.remove(otpAggregateKey)
                idsToCancel.add(mirrorIdForKey(otpAggregateKey))
            }
        }
        if (sourceKey != null) {
            val sourceState = otpSourceStates[sourceKey]
            if (sourceState != null && sourceState.sbnKey == sbnKey) {
                otpSourceStates.remove(sourceKey)
            }
        }
        return idsToCancel
    }

    private fun clearOtpTrackingForSourceLocked(sourceKey: String, exceptSbnKey: String): List<Int> {
        val idsToCancel = mutableListOf<Int>()
        val sbnKeysToClear = sbnToOtpSourceKey.entries
            .filter { it.value == sourceKey && it.key != exceptSbnKey }
            .map { it.key }

        for (sbnKey in sbnKeysToClear) {
            idsToCancel.addAll(clearOtpTrackingForSbnKeyLocked(sbnKey))
        }
        return idsToCancel
    }

    private data class ProgressOverride(
        val value: Int,
        val max: Int
    )

    private data class AggregateState(
        var maxStageSeen: Int,
        val maxStage: Int,
        val activeSbnKeys: MutableSet<String> = mutableSetOf(),
        val sourcesBySbnKey: MutableMap<String, SmartSourceEntry> = mutableMapOf()
    )

    private data class SmartSourceEntry(
        val stageValue: Int,
        val postTimeMs: Long,
        val sbn: StatusBarNotification,
        val compactOrderCode: String?
    )

    private data class OtpAggregateState(
        val activeSbnKeys: MutableSet<String> = mutableSetOf(),
        var lastRenderedAtMs: Long = 0L,
        var lastAutoCopiedCode: String = "",
        var lastAutoCopiedAtMs: Long = 0L
    )

    private data class OtpSourceState(
        val sbnKey: String,
        val aggregateKey: String,
        val postTimeMs: Long
    )

    private data class OtpRouteState(
        val staleAggregateIds: List<Int>,
        val shouldPublish: Boolean,
        val shouldAutoCopy: Boolean,
        val otpCode: String
    )

    private data class SmartRouteState(
        val staleAggregateIds: List<Int>,
        val stageValue: Int,
        val stageMax: Int,
        val compactOrderCode: String?,
        val sourceSbn: StatusBarNotification
    )

    private data class RemoteDrawableAssets(
        val icon: IconCompat?,
        val bitmap: Bitmap?
    )

    private data class SmartStageMatch(
        val aggregateKey: String,
        val stageValue: Int,
        val maxStage: Int,
        val compactOrderCode: String?,
        val keepHighestStage: Boolean
    )

    private data class VpnTrafficSpeeds(
        val outgoingSpeed: String?,
        val incomingSpeed: String?
    )

    private data class SmartAnimationState(
        var sbn: StatusBarNotification,
        var appPresentationOverride: AppPresentationOverride,
        var progressOverride: ProgressOverride?,
        var smartRuleId: String,
        var tokens: List<String?>,
        var nextIndex: Int,
        var lastShownToken: String?,
        var samsungBridge: SamsungBridgeContext
    )

    private data class SmartAnimationFrame(
        val sbn: StatusBarNotification,
        val appPresentationOverride: AppPresentationOverride,
        val progressOverride: ProgressOverride?,
        val smartRuleId: String,
        val token: String,
        val samsungBridge: SamsungBridgeContext
    )

    private data class SmartAnimationToken(
        val token: String,
        val nextIndex: Int
    )

    private data class TextProgressMatch(
        val percent: Int,
        val shortText: String
    )

    private data class MediaPlaybackSnapshot(
        val title: String?,
        val artist: String?,
        val albumArt: Bitmap?,
        val durationMs: Long,
        val positionMs: Long,
        val isPlaying: Boolean
    )

    private data class MediaPreferredAction(
        val action: Notification.Action,
        val shortTitle: String
    )

    private data class OtpMatch(
        val code: String,
        val aggregateKey: String
    )
}
