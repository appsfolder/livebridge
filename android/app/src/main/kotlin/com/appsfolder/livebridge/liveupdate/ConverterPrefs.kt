package com.appsfolder.livebridge.liveupdate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ConverterPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPackageRulesRaw(): String {
        val current = prefs.getString(KEY_PACKAGE_RULES, "") ?: ""
        if (current.isNotBlank()) {
            return current
        }

        return prefs.getString(KEY_PACKAGE_FILTER_LEGACY, "") ?: ""
    }

    fun setPackageRulesRaw(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit()
            .putString(KEY_PACKAGE_RULES, normalized)
            .putString(KEY_PACKAGE_FILTER_LEGACY, normalized)
            .apply()
    }

    fun getPackageMode(): String {
        val raw = prefs.getString(KEY_PACKAGE_MODE, PackageMode.ALL.id) ?: PackageMode.ALL.id
        return PackageMode.from(raw).id
    }

    fun setPackageMode(value: String?) {
        val mode = PackageMode.from(value)
        prefs.edit().putString(KEY_PACKAGE_MODE, mode.id).apply()
    }

    fun getBypassPackageRulesRaw(): String {
        return prefs.getString(KEY_BYPASS_PACKAGE_RULES, "") ?: ""
    }

    fun setBypassPackageRulesRaw(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit()
            .putString(KEY_BYPASS_PACKAGE_RULES, normalized)
            .apply()
    }

    fun getOnlyWithProgress(): Boolean {
        return prefs.getBoolean(KEY_ONLY_WITH_PROGRESS, true)
    }

    fun setOnlyWithProgress(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONLY_WITH_PROGRESS, value).apply()
    }

    fun getTextProgressEnabled(): Boolean {
        return prefs.getBoolean(KEY_TEXT_PROGRESS_ENABLED, true)
    }

    fun setTextProgressEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_TEXT_PROGRESS_ENABLED, value).apply()
    }

    fun getConverterEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONVERTER_ENABLED, true)
    }

    fun setConverterEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_CONVERTER_ENABLED, value).apply()
    }

    fun getKeepAliveForegroundEnabled(): Boolean {
        return prefs.getBoolean(KEY_KEEP_ALIVE_FOREGROUND_ENABLED, false)
    }

    fun getSpringTransitionsEnabled(): Boolean {
        return prefs.getBoolean(KEY_SPRING_TRANSITIONS_ENABLED, true)
    }

    fun setSpringTransitionsEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SPRING_TRANSITIONS_ENABLED, value).apply()
    }

    fun getPreventMirrorDismissEnabled(): Boolean {
        return prefs.getBoolean(KEY_PREVENT_MIRROR_DISMISS_ENABLED, false)
    }

    fun setPreventMirrorDismissEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREVENT_MIRROR_DISMISS_ENABLED, value).apply()
    }

    fun getHideLockscreenContentEnabled(): Boolean {
        return prefs.getBoolean(KEY_HIDE_LOCKSCREEN_CONTENT_ENABLED, false)
    }

    fun setHideLockscreenContentEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_LOCKSCREEN_CONTENT_ENABLED, value).apply()
    }

    fun getConvertedNotificationSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONVERTED_NOTIFICATION_SOUND_ENABLED, false)
    }

    fun setConvertedNotificationSoundEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_CONVERTED_NOTIFICATION_SOUND_ENABLED, value).apply()
    }

    fun getHintsDisabled(): Boolean {
        return prefs.getBoolean(KEY_HINTS_DISABLED, false)
    }

    fun setHintsDisabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_HINTS_DISABLED, value).apply()
    }

    fun getConversionLogEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONVERSION_LOG_ENABLED, false)
    }

    fun setConversionLogEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_CONVERSION_LOG_ENABLED, value).apply()
    }

    fun getBugReportAutoCopyEnabled(): Boolean {
        return prefs.getBoolean(KEY_BUG_REPORT_AUTO_COPY_ENABLED, false)
    }

    fun setBugReportAutoCopyEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_BUG_REPORT_AUTO_COPY_ENABLED, value).apply()
    }

    fun getAppLanguageTag(): String {
        return prefs.getString(KEY_APP_LANGUAGE_TAG, "system") ?: "system"
    }

    fun setAppLanguageTag(value: String?) {
        val normalized = value?.trim().orEmpty().ifBlank { "system" }
        prefs.edit().putString(KEY_APP_LANGUAGE_TAG, normalized).apply()
    }

    fun getConversionLogMaxBytes(): Int {
        return prefs.getInt(
            KEY_CONVERSION_LOG_MAX_BYTES,
            DEFAULT_CONVERSION_LOG_MAX_BYTES
        ).coerceIn(MIN_CONVERSION_LOG_MAX_BYTES, MAX_CONVERSION_LOG_MAX_BYTES)
    }

    fun setConversionLogMaxBytes(value: Int) {
        prefs.edit()
            .putInt(
                KEY_CONVERSION_LOG_MAX_BYTES,
                value.coerceIn(
                    MIN_CONVERSION_LOG_MAX_BYTES,
                    MAX_CONVERSION_LOG_MAX_BYTES
                )
            )
            .apply()
    }

    fun getNetworkSpeedEnabled(): Boolean {
        return prefs.getBoolean(KEY_NETWORK_SPEED_ENABLED, false)
    }

    fun hasKeepAliveForegroundPreference(): Boolean {
        return prefs.contains(KEY_KEEP_ALIVE_FOREGROUND_ENABLED)
    }

    fun setKeepAliveForegroundEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_ALIVE_FOREGROUND_ENABLED, value).apply()
    }

    fun setNetworkSpeedEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_NETWORK_SPEED_ENABLED, value).apply()
    }

    fun getNetworkSpeedMinThresholdBytesPerSecond(): Long {
        return prefs.getLong(KEY_NETWORK_SPEED_MIN_THRESHOLD_BYTES_PER_SECOND, 0L)
            .coerceAtLeast(0L)
    }

    fun setNetworkSpeedMinThresholdBytesPerSecond(value: Long) {
        prefs.edit()
            .putLong(
                KEY_NETWORK_SPEED_MIN_THRESHOLD_BYTES_PER_SECOND,
                value.coerceAtLeast(0L)
            )
            .apply()
    }

    fun getSyncDndEnabled(): Boolean {
        return prefs.getBoolean(KEY_SYNC_DND_ENABLED, true)
    }

    fun setSyncDndEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SYNC_DND_ENABLED, value).apply()
    }

    fun getUpdateChecksEnabled(): Boolean {
        return prefs.getBoolean(KEY_UPDATE_CHECKS_ENABLED, true)
    }

    fun setUpdateChecksEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_UPDATE_CHECKS_ENABLED, value).apply()
    }

    fun getUpdateLastCheckAtMs(): Long {
        return prefs.getLong(KEY_UPDATE_LAST_CHECK_AT_MS, 0L)
    }

    fun setUpdateLastCheckAtMs(value: Long) {
        prefs.edit().putLong(KEY_UPDATE_LAST_CHECK_AT_MS, value).apply()
    }

    fun getUpdateCachedLatestVersion(): String {
        return prefs.getString(KEY_UPDATE_CACHED_LATEST_VERSION, "") ?: ""
    }

    fun setUpdateCachedLatestVersion(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit().putString(KEY_UPDATE_CACHED_LATEST_VERSION, normalized).apply()
    }

    fun getUpdateCachedAvailable(): Boolean {
        return prefs.getBoolean(KEY_UPDATE_CACHED_AVAILABLE, false)
    }

    fun setUpdateCachedAvailable(value: Boolean) {
        prefs.edit().putBoolean(KEY_UPDATE_CACHED_AVAILABLE, value).apply()
    }

    fun getUpdateLastNotifiedVersion(): String {
        return prefs.getString(KEY_UPDATE_LAST_NOTIFIED_VERSION, "") ?: ""
    }

    fun setUpdateLastNotifiedVersion(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit().putString(KEY_UPDATE_LAST_NOTIFIED_VERSION, normalized).apply()
    }

    fun getAospCuttingEnabled(): Boolean {
        return prefs.getBoolean(KEY_AOSP_CUTTING_ENABLED, false)
    }

    fun setAospCuttingEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_AOSP_CUTTING_ENABLED, value).apply()
    }

    fun getAospCuttingLength(): Int {
        return prefs.getInt(
            KEY_AOSP_CUTTING_LENGTH,
            DEFAULT_AOSP_CUTTING_LENGTH
        ).coerceIn(
            MIN_AOSP_CUTTING_LENGTH,
            MAX_AOSP_CUTTING_LENGTH
        )
    }

    fun setAospCuttingLength(value: Int) {
        prefs.edit()
            .putInt(
                KEY_AOSP_CUTTING_LENGTH,
                value.coerceIn(
                    MIN_AOSP_CUTTING_LENGTH,
                    MAX_AOSP_CUTTING_LENGTH
                )
            )
            .apply()
    }

    fun getAnimatedIslandEnabled(): Boolean {
        return prefs.getBoolean(KEY_ANIMATED_ISLAND_ENABLED, false)
    }

    fun setAnimatedIslandEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ANIMATED_ISLAND_ENABLED, value).apply()
    }

    fun getAnimatedIslandUpdateFrequencyMs(): Int {
        return prefs.getInt(
            KEY_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS,
            DEFAULT_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS
        ).coerceIn(
            MIN_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS,
            MAX_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS
        )
    }

    fun setAnimatedIslandUpdateFrequencyMs(value: Int) {
        prefs.edit()
            .putInt(
                KEY_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS,
                value.coerceIn(
                    MIN_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS,
                    MAX_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS
                )
            )
            .apply()
    }

    fun getHyperBridgeEnabled(): Boolean {
        return prefs.getBoolean(KEY_HYPERBRIDGE_ENABLED, false)
    }

    fun setHyperBridgeEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_HYPERBRIDGE_ENABLED, value).apply()
    }

    fun getNotificationDedupEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_DEDUP_ENABLED, false)
    }

    fun setNotificationDedupEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_DEDUP_ENABLED, value).apply()
    }

    fun getNotificationDedupMode(): String {
        val raw = prefs.getString(
            KEY_NOTIFICATION_DEDUP_MODE,
            NotificationDedupMode.OTP_STATUS.id
        ) ?: NotificationDedupMode.OTP_STATUS.id
        return NotificationDedupMode.from(raw).id
    }

    fun setNotificationDedupMode(value: String?) {
        val mode = NotificationDedupMode.from(value)
        prefs.edit().putString(KEY_NOTIFICATION_DEDUP_MODE, mode.id).apply()
    }

    fun getNotificationDedupPackageRulesRaw(): String {
        return prefs.getString(KEY_NOTIFICATION_DEDUP_PACKAGE_RULES, "") ?: ""
    }

    fun setNotificationDedupPackageRulesRaw(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit()
            .putString(KEY_NOTIFICATION_DEDUP_PACKAGE_RULES, normalized)
            .apply()
    }

    fun getNotificationDedupPackageMode(): String {
        val raw = prefs.getString(
            KEY_NOTIFICATION_DEDUP_PACKAGE_MODE,
            PackageMode.ALL.id
        ) ?: PackageMode.ALL.id
        return PackageMode.from(raw).id
    }

    fun setNotificationDedupPackageMode(value: String?) {
        val mode = PackageMode.from(value)
        prefs.edit().putString(KEY_NOTIFICATION_DEDUP_PACKAGE_MODE, mode.id).apply()
    }

    fun getOtpRemoveOriginalMessageEnabled(): Boolean {
        return if (prefs.contains(KEY_OTP_REMOVE_ORIGINAL_MESSAGE_ENABLED)) {
            prefs.getBoolean(KEY_OTP_REMOVE_ORIGINAL_MESSAGE_ENABLED, false)
        } else {
            getLegacyNotificationDedupEnabled()
        }
    }

    fun setOtpRemoveOriginalMessageEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_OTP_REMOVE_ORIGINAL_MESSAGE_ENABLED, value).apply()
    }

    fun getSmartRemoveOriginalMessageEnabled(): Boolean {
        return if (prefs.contains(KEY_SMART_REMOVE_ORIGINAL_MESSAGE_ENABLED)) {
            prefs.getBoolean(KEY_SMART_REMOVE_ORIGINAL_MESSAGE_ENABLED, false)
        } else {
            getLegacyNotificationDedupEnabled() &&
                getLegacyNotificationDedupMode() == NotificationDedupMode.OTP_STATUS.id
        }
    }

    fun setSmartRemoveOriginalMessageEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_REMOVE_ORIGINAL_MESSAGE_ENABLED, value).apply()
    }

    fun getSmartPackageRulesRaw(): String {
        if (prefs.contains(KEY_SMART_PACKAGE_RULES)) {
            return prefs.getString(KEY_SMART_PACKAGE_RULES, "") ?: ""
        }
        return getLegacyNotificationDedupPackageRulesRaw()
    }

    fun setSmartPackageRulesRaw(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit()
            .putString(KEY_SMART_PACKAGE_RULES, normalized)
            .apply()
    }

    fun getSmartPackageMode(): String {
        val raw = if (prefs.contains(KEY_SMART_PACKAGE_MODE)) {
            prefs.getString(KEY_SMART_PACKAGE_MODE, PackageMode.ALL.id)
        } else {
            getLegacyNotificationDedupPackageMode()
        } ?: PackageMode.ALL.id
        return PackageMode.from(raw).id
    }

    fun setSmartPackageMode(value: String?) {
        val mode = PackageMode.from(value)
        prefs.edit().putString(KEY_SMART_PACKAGE_MODE, mode.id).apply()
    }

    fun getSmartStatusDetectionEnabled(): Boolean {
        return getSmartTaxiEnabled() ||
            getSmartDeliveryEnabled() ||
            getSmartCallsEnabled() ||
            getSmartNavigationEnabled() ||
            getSmartWeatherEnabled() ||
            getSmartExternalDevicesEnabled() ||
            getSmartVpnEnabled()
    }

    fun setSmartStatusDetectionEnabled(value: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SMART_STATUS_ENABLED, value)
            .putBoolean(KEY_SMART_TAXI_ENABLED, value)
            .putBoolean(KEY_SMART_DELIVERY_ENABLED, value)
            .putBoolean(KEY_SMART_CALLS_ENABLED, value)
            .putBoolean(KEY_SMART_NAVIGATION_ENABLED, value)
            .putBoolean(KEY_SMART_WEATHER_ENABLED, value)
            .putBoolean(KEY_SMART_EXTERNAL_DEVICES_ENABLED, value)
            .putBoolean(KEY_SMART_VPN_ENABLED, value)
            .apply()
    }

    fun getSmartTaxiEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_TAXI_ENABLED, getLegacySmartStatusDefault())
    }

    fun setSmartTaxiEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_TAXI_ENABLED, value).apply()
    }

    fun getSmartDeliveryEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_DELIVERY_ENABLED, getLegacySmartStatusDefault())
    }

    fun setSmartDeliveryEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_DELIVERY_ENABLED, value).apply()
    }

    fun getSmartCallsEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_CALLS_ENABLED, getLegacySmartStatusDefault())
    }

    fun setSmartCallsEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_CALLS_ENABLED, value).apply()
    }

    fun getSmartMediaPlaybackEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_MEDIA_PLAYBACK_ENABLED, false)
    }

    fun setSmartMediaPlaybackEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_MEDIA_PLAYBACK_ENABLED, value).apply()
    }

    fun getSmartMediaPlaybackShowOnLockScreen(): Boolean {
        return prefs.getBoolean(KEY_SMART_MEDIA_PLAYBACK_SHOW_ON_LOCK_SCREEN, false)
    }

    fun setSmartMediaPlaybackShowOnLockScreen(value: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SMART_MEDIA_PLAYBACK_SHOW_ON_LOCK_SCREEN, value)
            .apply()
    }

    fun getSmartMediaPlaybackUseSymbolsInPlayer(): Boolean {
        return prefs.getBoolean(KEY_SMART_MEDIA_PLAYBACK_USE_SYMBOLS_IN_PLAYER, false)
    }

    fun setSmartMediaPlaybackUseSymbolsInPlayer(value: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SMART_MEDIA_PLAYBACK_USE_SYMBOLS_IN_PLAYER, value)
            .apply()
    }

    fun getSmartNavigationEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_NAVIGATION_ENABLED, true)
    }

    fun setSmartNavigationEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_NAVIGATION_ENABLED, value).apply()
    }

    fun getSmartWeatherEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_WEATHER_ENABLED, true)
    }

    fun setSmartWeatherEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_WEATHER_ENABLED, value).apply()
    }

    fun getSmartExternalDevicesEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_EXTERNAL_DEVICES_ENABLED, true)
    }

    fun setSmartExternalDevicesEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_EXTERNAL_DEVICES_ENABLED, value).apply()
    }

    fun getSmartExternalDevicesIgnoreDebugging(): Boolean {
        return prefs.getBoolean(KEY_SMART_EXTERNAL_DEVICES_IGNORE_DEBUGGING, true)
    }

    fun setSmartExternalDevicesIgnoreDebugging(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_EXTERNAL_DEVICES_IGNORE_DEBUGGING, value).apply()
    }

    fun getSmartVpnEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_VPN_ENABLED, true)
    }

    fun setSmartVpnEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_VPN_ENABLED, value).apply()
    }

    fun getOtpDetectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_OTP_DETECTION_ENABLED, true)
    }

    fun setOtpDetectionEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_OTP_DETECTION_ENABLED, value).apply()
    }

    fun getOtpAutoCopyEnabled(): Boolean {
        return prefs.getBoolean(KEY_OTP_AUTO_COPY_ENABLED, false)
    }

    fun setOtpAutoCopyEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_OTP_AUTO_COPY_ENABLED, value).apply()
    }

    fun getOtpPackageRulesRaw(): String {
        return prefs.getString(KEY_OTP_PACKAGE_RULES, "") ?: ""
    }

    fun setOtpPackageRulesRaw(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit()
            .putString(KEY_OTP_PACKAGE_RULES, normalized)
            .apply()
    }

    fun getOtpPackageMode(): String {
        val raw = prefs.getString(KEY_OTP_PACKAGE_MODE, PackageMode.ALL.id) ?: PackageMode.ALL.id
        return PackageMode.from(raw).id
    }

    fun setOtpPackageMode(value: String?) {
        val mode = PackageMode.from(value)
        prefs.edit().putString(KEY_OTP_PACKAGE_MODE, mode.id).apply()
    }

    fun getAppListAccessGranted(): Boolean {
        return prefs.getBoolean(KEY_APP_LIST_ACCESS_GRANTED, false)
    }

    fun setAppListAccessGranted(value: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LIST_ACCESS_GRANTED, value).apply()
    }

    fun getBackgroundWarningDismissed(): Boolean {
        return prefs.getBoolean(KEY_BACKGROUND_WARNING_DISMISSED, false)
    }

    fun setBackgroundWarningDismissed(value: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_WARNING_DISMISSED, value).apply()
    }

    fun getSamsungWarningDismissed(): Boolean {
        return prefs.getBoolean(KEY_SAMSUNG_WARNING_DISMISSED, false)
    }

    fun setSamsungWarningDismissed(value: Boolean) {
        prefs.edit().putBoolean(KEY_SAMSUNG_WARNING_DISMISSED, value).apply()
    }

    fun hasExpandedSectionsState(): Boolean {
        return prefs.getBoolean(KEY_EXPANDED_SECTIONS_SET, false)
    }

    fun getExpandedSectionsRaw(): String {
        return prefs.getString(KEY_EXPANDED_SECTIONS, "") ?: ""
    }

    fun setExpandedSectionsRaw(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit()
            .putString(KEY_EXPANDED_SECTIONS, normalized)
            .putBoolean(KEY_EXPANDED_SECTIONS_SET, true)
            .apply()
    }

    fun getAppPresentationOverridesRaw(): String {
        return prefs.getString(KEY_APP_PRESENTATION_OVERRIDES, "") ?: ""
    }

    fun setAppPresentationOverridesRaw(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit().putString(KEY_APP_PRESENTATION_OVERRIDES, normalized).apply()
    }

    fun getCustomParserDictionaryRaw(): String? {
        val value = (
                prefs.getString(KEY_USER_PARSER_DICTIONARY, null)
                    ?: prefs.getString(KEY_CUSTOM_PARSER_DICTIONARY_LEGACY, null)
                )?.trim().orEmpty()
        return value.ifBlank { null }
    }

    fun setCustomParserDictionaryRaw(value: String?) {
        val normalized = value?.trim().orEmpty()
        prefs.edit()
            .putString(KEY_USER_PARSER_DICTIONARY, normalized.ifBlank { null })
            .remove(KEY_CUSTOM_PARSER_DICTIONARY_LEGACY)
            .remove(KEY_PARSER_DICTIONARY_EN_OVERRIDE)
            .remove(KEY_PARSER_DICTIONARY_PT_BR_OVERRIDE)
            .remove(KEY_PARSER_DICTIONARY_RU_OVERRIDE)
            .remove(KEY_PARSER_DICTIONARY_ZH_OVERRIDE)
            .remove(KEY_PARSER_DICTIONARY_KO_OVERRIDE)
            .apply()
    }

    fun getParserDictionaryEnabledLanguageIds(): Set<String> {
        val stored = prefs.getString(KEY_PARSER_DICTIONARY_ENABLED_LANGUAGES, null)
            ?: return DEFAULT_PARSER_DICTIONARY_LANGUAGE_IDS.toSet()
        return stored
            .split(',', ';', '\n', '\r', '\t', ' ')
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it in SUPPORTED_PARSER_DICTIONARY_LANGUAGE_IDS }
            .toCollection(linkedSetOf())
    }

    fun setParserDictionaryEnabledLanguageIds(values: Set<String>) {
        val normalized = values
            .asSequence()
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it in SUPPORTED_PARSER_DICTIONARY_LANGUAGE_IDS }
            .distinct()
            .sorted()
            .joinToString(",")
        prefs.edit()
            .putString(KEY_PARSER_DICTIONARY_ENABLED_LANGUAGES, normalized)
            .apply()
    }

    fun getParserDictionaryLanguageOverrideRaw(languageId: String): String? {
        val key = parserDictionaryOverrideKey(languageId) ?: return null
        val value = prefs.getString(key, null)?.trim().orEmpty()
        return value.ifBlank { null }
    }

    fun setParserDictionaryLanguageOverrideRaw(languageId: String, value: String?): Boolean {
        val key = parserDictionaryOverrideKey(languageId) ?: return false
        val normalized = value?.trim().orEmpty()
        prefs.edit()
            .putString(key, normalized.ifBlank { null })
            .remove(KEY_USER_PARSER_DICTIONARY)
            .remove(KEY_CUSTOM_PARSER_DICTIONARY_LEGACY)
            .apply()
        return true
    }

    fun clearCustomParserDictionary() {
        prefs.edit()
            .remove(KEY_USER_PARSER_DICTIONARY)
            .remove(KEY_CUSTOM_PARSER_DICTIONARY_LEGACY)
            .remove(KEY_PARSER_DICTIONARY_EN_OVERRIDE)
            .remove(KEY_PARSER_DICTIONARY_PT_BR_OVERRIDE)
            .remove(KEY_PARSER_DICTIONARY_RU_OVERRIDE)
            .remove(KEY_PARSER_DICTIONARY_ZH_OVERRIDE)
            .remove(KEY_PARSER_DICTIONARY_KO_OVERRIDE)
            .apply()
    }

    fun hasCustomParserDictionary(): Boolean {
        return !getCustomParserDictionaryRaw().isNullOrBlank() ||
            SUPPORTED_PARSER_DICTIONARY_LANGUAGE_IDS.any { languageId ->
                !getParserDictionaryLanguageOverrideRaw(languageId).isNullOrBlank()
            }
    }

    fun exportSettingsBackupJson(): String {
        val root = JSONObject()
            .put("schema", "livebridge_settings_backup_v1")
            .put("exported_at_ms", System.currentTimeMillis())
            .put("settings", buildSettingsJson())
            .put("rules", buildRulesJson())
            .put("dictionary", buildDictionaryJson())
            .put("app_presentation", buildAppPresentationJson())

        return root.toString(2)
    }

    fun importSettingsBackupJson(raw: String): Boolean {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        val hasImportableSections =
            root.has("settings") ||
                root.has("rules") ||
                root.has("dictionary") ||
                root.has("app_presentation") ||
                root.has("additional_state")
        if (!hasImportableSections) {
            return false
        }

        optObject(root, "settings")?.let(::applySettingsJson)
        optObject(root, "rules")?.let(::applyRulesJson)
        optObject(root, "dictionary")?.let(::applyDictionaryJson)
        optObject(root, "app_presentation")?.let(::applyAppPresentationJson)
        optObject(root, "additional_state")?.let(::applyAdditionalStateJson)
        return true
    }

    fun isPackageAllowed(packageName: String): Boolean {
        val mode = PackageMode.from(getPackageMode())
        val packages = parsePackageRules(getPackageRulesRaw())
        val normalizedPackageName = normalizePackageName(packageName)

        return when (mode) {
            PackageMode.ALL -> true
            PackageMode.INCLUDE -> packages.isNotEmpty() && normalizedPackageName in packages
            PackageMode.EXCLUDE -> normalizedPackageName !in packages
        }
    }

    fun isOtpPackageAllowed(packageName: String): Boolean {
        val mode = PackageMode.from(getOtpPackageMode())
        val packages = parsePackageRules(getOtpPackageRulesRaw())
        val normalizedPackageName = normalizePackageName(packageName)

        return when (mode) {
            PackageMode.ALL -> true
            PackageMode.INCLUDE -> packages.isNotEmpty() && normalizedPackageName in packages
            PackageMode.EXCLUDE -> normalizedPackageName !in packages
        }
    }

    fun shouldBypassAllRulesForPackage(packageName: String): Boolean {
        val packages = parsePackageRules(getBypassPackageRulesRaw())
        return normalizePackageName(packageName) in packages
    }

    fun isNotificationDedupPackageAllowed(packageName: String): Boolean {
        val mode = PackageMode.from(getNotificationDedupPackageMode())
        val packages = parsePackageRules(getNotificationDedupPackageRulesRaw())
        val normalizedPackageName = normalizePackageName(packageName)

        return when (mode) {
            PackageMode.ALL -> true
            PackageMode.INCLUDE -> packages.isNotEmpty() && normalizedPackageName in packages
            PackageMode.EXCLUDE -> normalizedPackageName !in packages
        }
    }

    fun isSmartPackageAllowed(packageName: String): Boolean {
        val mode = PackageMode.from(getSmartPackageMode())
        val packages = parsePackageRules(getSmartPackageRulesRaw())
        val normalizedPackageName = normalizePackageName(packageName)

        return when (mode) {
            PackageMode.ALL -> true
            PackageMode.INCLUDE -> packages.isNotEmpty() && normalizedPackageName in packages
            PackageMode.EXCLUDE -> normalizedPackageName !in packages
        }
    }

    private fun normalizePackageName(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }

    private fun parsePackageRules(raw: String): Set<String> {
        return raw
            .split(',', ';', '\n', '\r', '\t', ' ')
            .map(::normalizePackageName)
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun buildSettingsJson(): JSONObject {
        return JSONObject()
            .put("converter_enabled", getConverterEnabled())
            .put("keep_alive_foreground_enabled", getKeepAliveForegroundEnabled())
            .put("spring_transitions_enabled", getSpringTransitionsEnabled())
            .put("prevent_mirror_dismiss_enabled", getPreventMirrorDismissEnabled())
            .put("hide_lockscreen_content_enabled", getHideLockscreenContentEnabled())
            .put("converted_notification_sound_enabled", getConvertedNotificationSoundEnabled())
            .put("hints_disabled", getHintsDisabled())
            .put("conversion_log_enabled", getConversionLogEnabled())
            .put("conversion_log_max_bytes", getConversionLogMaxBytes())
            .put("bug_report_auto_copy_enabled", getBugReportAutoCopyEnabled())
            .put("app_language", getAppLanguageTag())
            .put("network_speed_enabled", getNetworkSpeedEnabled())
            .put(
                "network_speed_min_threshold_bytes_per_second",
                getNetworkSpeedMinThresholdBytesPerSecond()
            )
            .put("sync_dnd_enabled", getSyncDndEnabled())
            .put("update_checks_enabled", getUpdateChecksEnabled())
            .put("only_with_progress", getOnlyWithProgress())
            .put("text_progress_enabled", getTextProgressEnabled())
            .put("aosp_cutting_enabled", getAospCuttingEnabled())
            .put("aosp_cutting_length", getAospCuttingLength())
            .put("animated_island_enabled", getAnimatedIslandEnabled())
            .put(
                "animated_island_update_frequency_ms",
                getAnimatedIslandUpdateFrequencyMs()
            )
            .put("hyper_bridge_enabled", getHyperBridgeEnabled())
            .put("notification_dedup_enabled", getNotificationDedupEnabled())
            .put("notification_dedup_mode", getNotificationDedupMode())
            .put("otp_detection_enabled", getOtpDetectionEnabled())
            .put("otp_auto_copy_enabled", getOtpAutoCopyEnabled())
            .put(
                "otp_remove_original_message_enabled",
                getOtpRemoveOriginalMessageEnabled()
            )
            .put("smart_detection_enabled", getSmartStatusDetectionEnabled())
            .put("smart_taxi_enabled", getSmartTaxiEnabled())
            .put("smart_delivery_enabled", getSmartDeliveryEnabled())
            .put("smart_calls_enabled", getSmartCallsEnabled())
            .put("smart_media_playback_enabled", getSmartMediaPlaybackEnabled())
            .put(
                "smart_media_playback_show_on_lock_screen",
                getSmartMediaPlaybackShowOnLockScreen()
            )
            .put(
                "smart_media_playback_use_symbols_in_player",
                getSmartMediaPlaybackUseSymbolsInPlayer()
            )
            .put("smart_navigation_enabled", getSmartNavigationEnabled())
            .put("smart_weather_enabled", getSmartWeatherEnabled())
            .put("smart_external_devices_enabled", getSmartExternalDevicesEnabled())
            .put(
                "smart_external_devices_ignore_debugging",
                getSmartExternalDevicesIgnoreDebugging()
            )
            .put("smart_vpn_enabled", getSmartVpnEnabled())
            .put(
                "smart_remove_original_message_enabled",
                getSmartRemoveOriginalMessageEnabled()
            )
            .put("app_list_access_granted", getAppListAccessGranted())
            .put("background_warning_dismissed", getBackgroundWarningDismissed())
            .put("samsung_warning_dismissed", getSamsungWarningDismissed())
    }

    private fun buildRulesJson(): JSONObject {
        return JSONObject()
            .put("package_mode", getPackageMode())
            .put("package_rules", jsonArrayFromRules(getPackageRulesRaw()))
            .put("bypass_package_rules", jsonArrayFromRules(getBypassPackageRulesRaw()))
            .put("notification_dedup_package_mode", getNotificationDedupPackageMode())
            .put(
                "notification_dedup_package_rules",
                jsonArrayFromRules(getNotificationDedupPackageRulesRaw())
            )
            .put("otp_package_mode", getOtpPackageMode())
            .put("otp_package_rules", jsonArrayFromRules(getOtpPackageRulesRaw()))
            .put("smart_package_mode", getSmartPackageMode())
            .put("smart_package_rules", jsonArrayFromRules(getSmartPackageRulesRaw()))
    }

    private fun buildDictionaryJson(): JSONObject {
        val dictionary = JSONObject()
            .put(
                "parser_dictionary_enabled_languages",
                JSONArray(getParserDictionaryEnabledLanguageIds().sorted())
            )
        getCustomParserDictionaryRaw()?.let { dictionary.put("custom_parser_dictionary", it) }
        SUPPORTED_PARSER_DICTIONARY_LANGUAGE_IDS.forEach { languageId ->
            getParserDictionaryLanguageOverrideRaw(languageId)?.let { raw ->
                dictionary.put("parser_dictionary_${languageId}_override", raw)
            }
        }
        return dictionary
    }

    private fun buildAppPresentationJson(): JSONObject {
        return JSONObject()
            .put("app_presentation_overrides", getAppPresentationOverridesRaw())
    }

    private fun applySettingsJson(settings: JSONObject) {
        bool(settings, "converter_enabled")?.let(::setConverterEnabled)
        bool(settings, "keep_alive_foreground_enabled")?.let(::setKeepAliveForegroundEnabled)
        bool(settings, "spring_transitions_enabled")?.let(::setSpringTransitionsEnabled)
        bool(settings, "prevent_mirror_dismiss_enabled")?.let(::setPreventMirrorDismissEnabled)
        bool(settings, "hide_lockscreen_content_enabled")?.let(::setHideLockscreenContentEnabled)
        bool(settings, "converted_notification_sound_enabled")
            ?.let(::setConvertedNotificationSoundEnabled)
        bool(settings, "hints_disabled")?.let(::setHintsDisabled)
        bool(settings, "conversion_log_enabled")?.let(::setConversionLogEnabled)
        int(settings, "conversion_log_max_bytes")?.let(::setConversionLogMaxBytes)
        bool(settings, "bug_report_auto_copy_enabled")?.let(::setBugReportAutoCopyEnabled)
        string(settings, "app_language")?.let(::setAppLanguageTag)
        string(settings, "app_language_tag")?.let(::setAppLanguageTag)
        bool(settings, "network_speed_enabled")?.let(::setNetworkSpeedEnabled)
        long(settings, "network_speed_min_threshold_bytes_per_second")
            ?.let(::setNetworkSpeedMinThresholdBytesPerSecond)
        bool(settings, "sync_dnd_enabled")?.let(::setSyncDndEnabled)
        bool(settings, "update_checks_enabled")?.let(::setUpdateChecksEnabled)
        bool(settings, "only_with_progress")?.let(::setOnlyWithProgress)
        bool(settings, "text_progress_enabled")?.let(::setTextProgressEnabled)
        bool(settings, "aosp_cutting_enabled")?.let(::setAospCuttingEnabled)
        int(settings, "aosp_cutting_length")?.let(::setAospCuttingLength)
        bool(settings, "animated_island_enabled")?.let(::setAnimatedIslandEnabled)
        int(settings, "animated_island_update_frequency_ms")
            ?.let(::setAnimatedIslandUpdateFrequencyMs)
        bool(settings, "hyper_bridge_enabled")?.let(::setHyperBridgeEnabled)
        bool(settings, "hyperbridge_enabled")?.let(::setHyperBridgeEnabled)
        bool(settings, "notification_dedup_enabled")?.let(::setNotificationDedupEnabled)
        string(settings, "notification_dedup_mode")?.let(::setNotificationDedupMode)
        bool(settings, "otp_detection_enabled")?.let(::setOtpDetectionEnabled)
        bool(settings, "otp_auto_copy_enabled")?.let(::setOtpAutoCopyEnabled)
        bool(settings, "otp_remove_original_message_enabled")
            ?.let(::setOtpRemoveOriginalMessageEnabled)

        bool(settings, "smart_detection_enabled")?.let(::setSmartStatusDetectionEnabled)
        bool(settings, "smart_taxi_enabled")?.let(::setSmartTaxiEnabled)
        bool(settings, "smart_delivery_enabled")?.let(::setSmartDeliveryEnabled)
        bool(settings, "smart_calls_enabled")?.let(::setSmartCallsEnabled)
        bool(settings, "smart_media_playback_enabled")?.let(::setSmartMediaPlaybackEnabled)
        bool(settings, "smart_media_playback_show_on_lock_screen")
            ?.let(::setSmartMediaPlaybackShowOnLockScreen)
        bool(settings, "smart_media_playback_use_symbols_in_player")
            ?.let(::setSmartMediaPlaybackUseSymbolsInPlayer)
        bool(settings, "smart_navigation_enabled")?.let(::setSmartNavigationEnabled)
        bool(settings, "smart_weather_enabled")?.let(::setSmartWeatherEnabled)
        bool(settings, "smart_external_devices_enabled")
            ?.let(::setSmartExternalDevicesEnabled)
        bool(settings, "smart_external_devices_ignore_debugging")
            ?.let(::setSmartExternalDevicesIgnoreDebugging)
        bool(settings, "smart_vpn_enabled")?.let(::setSmartVpnEnabled)
        bool(settings, "smart_remove_original_message_enabled")
            ?.let(::setSmartRemoveOriginalMessageEnabled)

        bool(settings, "app_list_access_granted")?.let(::setAppListAccessGranted)
        bool(settings, "background_warning_dismissed")?.let(::setBackgroundWarningDismissed)
        bool(settings, "samsung_warning_dismissed")?.let(::setSamsungWarningDismissed)
    }

    private fun applyRulesJson(rules: JSONObject) {
        string(rules, "package_mode")?.let(::setPackageMode)
        rulesValue(rules, "package_rules")?.let(::setPackageRulesRaw)
        rulesValue(rules, "bypass_package_rules")?.let(::setBypassPackageRulesRaw)
        string(rules, "notification_dedup_package_mode")
            ?.let(::setNotificationDedupPackageMode)
        rulesValue(rules, "notification_dedup_package_rules")
            ?.let(::setNotificationDedupPackageRulesRaw)
        string(rules, "otp_package_mode")?.let(::setOtpPackageMode)
        rulesValue(rules, "otp_package_rules")?.let(::setOtpPackageRulesRaw)
        string(rules, "smart_package_mode")?.let(::setSmartPackageMode)
        rulesValue(rules, "smart_package_rules")?.let(::setSmartPackageRulesRaw)
    }

    private fun applyDictionaryJson(dictionary: JSONObject) {
        stringSet(dictionary, "parser_dictionary_enabled_languages")
            ?.let(::setParserDictionaryEnabledLanguageIds)
        SUPPORTED_PARSER_DICTIONARY_LANGUAGE_IDS.forEach { languageId ->
            string(dictionary, "parser_dictionary_${languageId}_override")?.let { raw ->
                setParserDictionaryLanguageOverrideRaw(languageId, raw)
            }
        }
        string(dictionary, "custom_parser_dictionary")?.let(::setCustomParserDictionaryRaw)
    }

    private fun applyAppPresentationJson(appPresentation: JSONObject) {
        string(appPresentation, "app_presentation_overrides")?.let(::setAppPresentationOverridesRaw)
    }

    private fun applyAdditionalStateJson(additionalState: JSONObject) {
        stringSet(additionalState, "parser_dictionary_enabled_languages")
            ?.let(::setParserDictionaryEnabledLanguageIds)
    }

    private fun jsonArrayFromRules(raw: String): JSONArray {
        return JSONArray(parsePackageRules(raw).sorted())
    }

    private fun optObject(parent: JSONObject, key: String): JSONObject? {
        return parent.opt(key) as? JSONObject
    }

    private fun bool(parent: JSONObject, key: String): Boolean? {
        if (!parent.has(key) || parent.isNull(key)) return null
        return when (val value = parent.opt(key)) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> when (value.trim().lowercase(Locale.ROOT)) {
                "true", "1", "yes", "enabled", "on" -> true
                "false", "0", "no", "disabled", "off" -> false
                else -> null
            }
            else -> null
        }
    }

    private fun int(parent: JSONObject, key: String): Int? {
        if (!parent.has(key) || parent.isNull(key)) return null
        return when (val value = parent.opt(key)) {
            is Number -> value.toInt()
            is String -> value.trim().toIntOrNull()
            else -> null
        }
    }

    private fun long(parent: JSONObject, key: String): Long? {
        if (!parent.has(key) || parent.isNull(key)) return null
        return when (val value = parent.opt(key)) {
            is Number -> value.toLong()
            is String -> value.trim().toLongOrNull()
            else -> null
        }
    }

    private fun string(parent: JSONObject, key: String): String? {
        if (!parent.has(key) || parent.isNull(key)) return null
        return parent.optString(key, "").trim()
    }

    private fun rulesValue(parent: JSONObject, key: String): String? {
        if (!parent.has(key) || parent.isNull(key)) return null
        return when (val value = parent.opt(key)) {
            is JSONArray -> (0 until value.length())
                .mapNotNull { index -> value.optString(index, "").trim().ifBlank { null } }
                .joinToString("\n")
            is String -> value.trim()
            else -> null
        }
    }

    private fun stringSet(parent: JSONObject, key: String): Set<String>? {
        if (!parent.has(key) || parent.isNull(key)) return null
        return when (val value = parent.opt(key)) {
            is JSONArray -> (0 until value.length())
                .mapNotNull { index -> value.optString(index, "").trim().ifBlank { null } }
                .toSet()
            is String -> value
                .split(',', ';', '\n', '\r', '\t', ' ')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            else -> null
        }
    }

    private fun getLegacySmartStatusDefault(): Boolean {
        return prefs.getBoolean(KEY_SMART_STATUS_ENABLED, true)
    }

    private fun getLegacyNotificationDedupEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_DEDUP_ENABLED, false)
    }

    private fun getLegacyNotificationDedupMode(): String {
        val raw = prefs.getString(
            KEY_NOTIFICATION_DEDUP_MODE,
            NotificationDedupMode.OTP_STATUS.id
        ) ?: NotificationDedupMode.OTP_STATUS.id
        return NotificationDedupMode.from(raw).id
    }

    private fun getLegacyNotificationDedupPackageRulesRaw(): String {
        return prefs.getString(KEY_NOTIFICATION_DEDUP_PACKAGE_RULES, "") ?: ""
    }

    private fun getLegacyNotificationDedupPackageMode(): String {
        val raw = prefs.getString(
            KEY_NOTIFICATION_DEDUP_PACKAGE_MODE,
            PackageMode.ALL.id
        ) ?: PackageMode.ALL.id
        return PackageMode.from(raw).id
    }

    private fun parserDictionaryOverrideKey(languageId: String): String? {
        return when (languageId.trim().lowercase(Locale.ROOT)) {
            "en" -> KEY_PARSER_DICTIONARY_EN_OVERRIDE
            "pt-br" -> KEY_PARSER_DICTIONARY_PT_BR_OVERRIDE
            "ru" -> KEY_PARSER_DICTIONARY_RU_OVERRIDE
            "zh" -> KEY_PARSER_DICTIONARY_ZH_OVERRIDE
            "ko" -> KEY_PARSER_DICTIONARY_KO_OVERRIDE
            else -> null
        }
    }

    private enum class PackageMode(val id: String) {
        ALL("all"),
        INCLUDE("include"),
        EXCLUDE("exclude");

        companion object {
            fun from(raw: String?): PackageMode {
                return entries.firstOrNull { it.id == raw } ?: ALL
            }
        }
    }

    private enum class NotificationDedupMode(val id: String) {
        OTP_STATUS("otp_status"),
        OTP_ONLY("otp_only");

        companion object {
            fun from(raw: String?): NotificationDedupMode {
                return entries.firstOrNull { it.id == raw } ?: OTP_STATUS
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "live_bridge_prefs"
        private const val KEY_PACKAGE_RULES = "package_rules"
        private const val KEY_PACKAGE_MODE = "package_mode"
        private const val KEY_BYPASS_PACKAGE_RULES = "bypass_package_rules"
        private const val KEY_ONLY_WITH_PROGRESS = "only_with_progress"
        private const val KEY_TEXT_PROGRESS_ENABLED = "text_progress_enabled"
        private const val KEY_CONVERTER_ENABLED = "converter_enabled"
        private const val KEY_KEEP_ALIVE_FOREGROUND_ENABLED = "keep_alive_foreground_enabled"
        private const val KEY_NETWORK_SPEED_ENABLED = "network_speed_enabled"
        private const val KEY_NETWORK_SPEED_MIN_THRESHOLD_BYTES_PER_SECOND =
            "network_speed_min_threshold_bytes_per_second"
        private const val KEY_SPRING_TRANSITIONS_ENABLED = "spring_transitions_enabled"
        private const val KEY_PREVENT_MIRROR_DISMISS_ENABLED =
            "prevent_mirror_dismiss_enabled"
        private const val KEY_HIDE_LOCKSCREEN_CONTENT_ENABLED =
            "hide_lockscreen_content_enabled"
        private const val KEY_CONVERTED_NOTIFICATION_SOUND_ENABLED =
            "converted_notification_sound_enabled"
        private const val KEY_HINTS_DISABLED = "hints_disabled"
        private const val KEY_CONVERSION_LOG_ENABLED = "conversion_log_enabled"
        private const val KEY_BUG_REPORT_AUTO_COPY_ENABLED = "bug_report_auto_copy_enabled"
        private const val KEY_APP_LANGUAGE_TAG = "app_language_tag"
        private const val KEY_CONVERSION_LOG_MAX_BYTES = "conversion_log_max_bytes"
        private const val KEY_SYNC_DND_ENABLED = "sync_dnd_enabled"
        private const val KEY_UPDATE_CHECKS_ENABLED = "update_checks_enabled"
        private const val KEY_UPDATE_LAST_CHECK_AT_MS = "update_last_check_at_ms"
        private const val KEY_UPDATE_CACHED_LATEST_VERSION = "update_cached_latest_version"
        private const val KEY_UPDATE_CACHED_AVAILABLE = "update_cached_available"
        private const val KEY_UPDATE_LAST_NOTIFIED_VERSION = "update_last_notified_version"
        private const val KEY_AOSP_CUTTING_ENABLED = "aosp_cutting_enabled"
        private const val KEY_AOSP_CUTTING_LENGTH = "aosp_cutting_length"
        private const val KEY_ANIMATED_ISLAND_ENABLED = "animated_island_enabled"
        private const val KEY_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS =
            "animated_island_update_frequency_ms"
        private const val KEY_HYPERBRIDGE_ENABLED = "hyperbridge_enabled"
        private const val KEY_NOTIFICATION_DEDUP_ENABLED = "notification_dedup_enabled"
        private const val KEY_NOTIFICATION_DEDUP_MODE = "notification_dedup_mode"
        private const val KEY_NOTIFICATION_DEDUP_PACKAGE_RULES = "notification_dedup_package_rules"
        private const val KEY_NOTIFICATION_DEDUP_PACKAGE_MODE = "notification_dedup_package_mode"
        private const val KEY_SMART_STATUS_ENABLED = "smart_status_enabled"
        private const val KEY_SMART_PACKAGE_RULES = "smart_package_rules"
        private const val KEY_SMART_PACKAGE_MODE = "smart_package_mode"
        private const val KEY_SMART_TAXI_ENABLED = "smart_taxi_enabled"
        private const val KEY_SMART_DELIVERY_ENABLED = "smart_delivery_enabled"
        private const val KEY_SMART_CALLS_ENABLED = "smart_calls_enabled"
        private const val KEY_SMART_MEDIA_PLAYBACK_ENABLED = "smart_media_playback_enabled"
        private const val KEY_SMART_MEDIA_PLAYBACK_SHOW_ON_LOCK_SCREEN =
            "smart_media_playback_show_on_lock_screen"
        private const val KEY_SMART_MEDIA_PLAYBACK_USE_SYMBOLS_IN_PLAYER =
            "smart_media_playback_use_symbols_in_player"
        private const val KEY_SMART_NAVIGATION_ENABLED = "smart_navigation_enabled"
        private const val KEY_SMART_WEATHER_ENABLED = "smart_weather_enabled"
        private const val KEY_SMART_EXTERNAL_DEVICES_ENABLED = "smart_external_devices_enabled"
        private const val KEY_SMART_EXTERNAL_DEVICES_IGNORE_DEBUGGING =
            "smart_external_devices_ignore_debugging"
        private const val KEY_SMART_VPN_ENABLED = "smart_vpn_enabled"
        private const val KEY_SMART_REMOVE_ORIGINAL_MESSAGE_ENABLED =
            "smart_remove_original_message_enabled"
        private const val KEY_OTP_DETECTION_ENABLED = "otp_detection_enabled"
        private const val KEY_OTP_AUTO_COPY_ENABLED = "otp_auto_copy_enabled"
        private const val KEY_OTP_REMOVE_ORIGINAL_MESSAGE_ENABLED =
            "otp_remove_original_message_enabled"
        private const val KEY_OTP_PACKAGE_RULES = "otp_package_rules"
        private const val KEY_OTP_PACKAGE_MODE = "otp_package_mode"
        private const val KEY_APP_LIST_ACCESS_GRANTED = "app_list_access_granted"
        private const val KEY_BACKGROUND_WARNING_DISMISSED = "background_warning_dismissed"
        private const val KEY_SAMSUNG_WARNING_DISMISSED = "samsung_warning_dismissed"
        private const val KEY_EXPANDED_SECTIONS = "expanded_sections"
        private const val KEY_EXPANDED_SECTIONS_SET = "expanded_sections_set"
        private const val KEY_APP_PRESENTATION_OVERRIDES = "app_presentation_overrides"
        private const val KEY_USER_PARSER_DICTIONARY = "user_parser_dictionary"
        private const val KEY_CUSTOM_PARSER_DICTIONARY_LEGACY = "custom_parser_dictionary"
        private const val KEY_PARSER_DICTIONARY_ENABLED_LANGUAGES =
            "parser_dictionary_enabled_languages"
        private const val KEY_PARSER_DICTIONARY_EN_OVERRIDE =
            "parser_dictionary_en_override"
        private const val KEY_PARSER_DICTIONARY_PT_BR_OVERRIDE =
            "parser_dictionary_pt_br_override"
        private const val KEY_PARSER_DICTIONARY_RU_OVERRIDE =
            "parser_dictionary_ru_override"
        private const val KEY_PARSER_DICTIONARY_ZH_OVERRIDE =
            "parser_dictionary_zh_override"
        private const val KEY_PARSER_DICTIONARY_KO_OVERRIDE =
            "parser_dictionary_ko_override"

        private const val KEY_PACKAGE_FILTER_LEGACY = "package_filter"
        private const val MIN_AOSP_CUTTING_LENGTH = 7
        private const val MAX_AOSP_CUTTING_LENGTH = 12
        private const val DEFAULT_AOSP_CUTTING_LENGTH = 7
        private const val MIN_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS = 750
        private const val MAX_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS = 3000
        private const val DEFAULT_ANIMATED_ISLAND_UPDATE_FREQUENCY_MS = 2250
        private const val MIN_CONVERSION_LOG_MAX_BYTES = 1 * 1024 * 1024
        private const val MAX_CONVERSION_LOG_MAX_BYTES = 25 * 1024 * 1024
        private const val DEFAULT_CONVERSION_LOG_MAX_BYTES = 5 * 1024 * 1024
        private val SUPPORTED_PARSER_DICTIONARY_LANGUAGE_IDS =
            setOf("en", "pt-br", "ru", "zh", "ko")
        private val DEFAULT_PARSER_DICTIONARY_LANGUAGE_IDS =
            SUPPORTED_PARSER_DICTIONARY_LANGUAGE_IDS
    }
}
