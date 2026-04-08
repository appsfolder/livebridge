package com.kakao.taxi.liveupdate

import android.content.Context
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

    fun getNetworkSpeedEnabled(): Boolean {
        return prefs.getBoolean(KEY_NETWORK_SPEED_ENABLED, false)
    }

    fun setNetworkSpeedEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_NETWORK_SPEED_ENABLED, value).apply()
    }

    fun getNetworkSpeedDisplayMode(): String {
        val raw = prefs.getString(KEY_NETWORK_SPEED_DISPLAY_MODE, NetworkSpeedDisplayMode.TOTAL.id)
            ?: NetworkSpeedDisplayMode.TOTAL.id
        return NetworkSpeedDisplayMode.from(raw).id
    }

    fun setNetworkSpeedDisplayMode(value: String?) {
        val mode = NetworkSpeedDisplayMode.from(value)
        prefs.edit().putString(KEY_NETWORK_SPEED_DISPLAY_MODE, mode.id).apply()
    }

    fun getNetworkSpeedUploadPrefix(): String {
        return prefs.getString(KEY_NETWORK_SPEED_UPLOAD_PREFIX, DEFAULT_NETWORK_SPEED_UPLOAD_PREFIX)
            ?: DEFAULT_NETWORK_SPEED_UPLOAD_PREFIX
    }

    fun setNetworkSpeedUploadPrefix(value: String?) {
        prefs.edit()
            .putString(
                KEY_NETWORK_SPEED_UPLOAD_PREFIX,
                value ?: DEFAULT_NETWORK_SPEED_UPLOAD_PREFIX
            )
            .apply()
    }

    fun getNetworkSpeedDownloadPrefix(): String {
        return prefs.getString(
            KEY_NETWORK_SPEED_DOWNLOAD_PREFIX,
            DEFAULT_NETWORK_SPEED_DOWNLOAD_PREFIX
        ) ?: DEFAULT_NETWORK_SPEED_DOWNLOAD_PREFIX
    }

    fun setNetworkSpeedDownloadPrefix(value: String?) {
        prefs.edit()
            .putString(
                KEY_NETWORK_SPEED_DOWNLOAD_PREFIX,
                value ?: DEFAULT_NETWORK_SPEED_DOWNLOAD_PREFIX
            )
            .apply()
    }

    fun getNetworkSpeedUnit(): String {
        val raw = prefs.getString(KEY_NETWORK_SPEED_UNIT, null)
        return if (raw == null) {
            NetworkSpeedUnit.AUTO.id
        } else {
            NetworkSpeedUnit.normalizeSelection(raw)
        }
    }

    fun setNetworkSpeedUnit(value: String?) {
        val normalized = NetworkSpeedUnit.normalizeSelection(value)
        prefs.edit().putString(KEY_NETWORK_SPEED_UNIT, normalized).apply()
    }

    fun getNetworkSpeedPrioritizeUpload(): Boolean {
        return prefs.getBoolean(KEY_NETWORK_SPEED_PRIORITIZE_UPLOAD, false)
    }

    fun setNetworkSpeedPrioritizeUpload(value: Boolean) {
        prefs.edit().putBoolean(KEY_NETWORK_SPEED_PRIORITIZE_UPLOAD, value).apply()
    }

    fun getNetworkSpeedLockscreenOnly(): Boolean {
        return prefs.getBoolean(KEY_NETWORK_SPEED_LOCKSCREEN_ONLY, false)
    }

    fun setNetworkSpeedLockscreenOnly(value: Boolean) {
        prefs.edit().putBoolean(KEY_NETWORK_SPEED_LOCKSCREEN_ONLY, value).apply()
    }

    fun getNetworkSpeedChipBackgroundDisabled(): Boolean {
        return prefs.getBoolean(KEY_NETWORK_SPEED_DISABLE_CHIP_BACKGROUND, false)
    }

    fun setNetworkSpeedChipBackgroundDisabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_NETWORK_SPEED_DISABLE_CHIP_BACKGROUND, value).apply()
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

    fun hasKeepAliveForegroundPreference(): Boolean {
        return prefs.contains(KEY_KEEP_ALIVE_FOREGROUND_ENABLED)
    }

    fun setKeepAliveForegroundEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_ALIVE_FOREGROUND_ENABLED, value).apply()
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

    fun getAnimatedIslandEnabled(): Boolean {
        return prefs.getBoolean(KEY_ANIMATED_ISLAND_ENABLED, false)
    }

    fun setAnimatedIslandEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ANIMATED_ISLAND_ENABLED, value).apply()
    }

    fun getHyperBridgeEnabled(): Boolean {
        return prefs.getBoolean(KEY_HYPERBRIDGE_ENABLED, false)
    }

    fun setHyperBridgeEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_HYPERBRIDGE_ENABLED, value).apply()
    }

    fun getSmartStatusDetectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_STATUS_ENABLED, true)
    }

    fun setSmartStatusDetectionEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_STATUS_ENABLED, value).apply()
    }

    fun getSmartMediaPlaybackEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_MEDIA_PLAYBACK_ENABLED, false)
    }

    fun setSmartMediaPlaybackEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_MEDIA_PLAYBACK_ENABLED, value).apply()
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

    fun getPixelJokeBypassEnabled(): Boolean {
        return prefs.getBoolean(KEY_PIXEL_JOKE_BYPASS_ENABLED, false)
    }

    fun setPixelJokeBypassEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_PIXEL_JOKE_BYPASS_ENABLED, value).apply()
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

    fun getSamsungRemoteReparserEnabled(): Boolean {
        return prefs.getBoolean(KEY_SAMSUNG_REMOTE_REPARSER_ENABLED, true)
    }

    fun setSamsungRemoteReparserEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SAMSUNG_REMOTE_REPARSER_ENABLED, value).apply()
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
            .apply()
    }

    fun clearCustomParserDictionary() {
        prefs.edit()
            .remove(KEY_USER_PARSER_DICTIONARY)
            .remove(KEY_CUSTOM_PARSER_DICTIONARY_LEGACY)
            .apply()
    }

    fun hasCustomParserDictionary(): Boolean {
        return !getCustomParserDictionaryRaw().isNullOrBlank()
    }

    fun isPackageAllowed(packageName: String): Boolean {
        val mode = PackageMode.from(getPackageMode())
        val packages = parsePackageRules(getPackageRulesRaw())

        return when (mode) {
            PackageMode.ALL -> true
            PackageMode.INCLUDE -> packages.isNotEmpty() && packageName in packages
            PackageMode.EXCLUDE -> packageName !in packages
        }
    }

    fun isOtpPackageAllowed(packageName: String): Boolean {
        val mode = PackageMode.from(getOtpPackageMode())
        val packages = parsePackageRules(getOtpPackageRulesRaw())

        return when (mode) {
            PackageMode.ALL -> true
            PackageMode.INCLUDE -> packages.isNotEmpty() && packageName in packages
            PackageMode.EXCLUDE -> packageName !in packages
        }
    }

    fun shouldBypassAllRulesForPackage(packageName: String): Boolean {
        val packages = parsePackageRules(getBypassPackageRulesRaw())
        return packageName.lowercase(Locale.ROOT) in packages
    }

    private fun parsePackageRules(raw: String): Set<String> {
        return raw
            .split(',', ';', '\n', '\r', '\t', ' ')
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .toSet()
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

    companion object {
        private const val PREFS_NAME = "live_bridge_prefs"
        private const val KEY_PACKAGE_RULES = "package_rules"
        private const val KEY_PACKAGE_MODE = "package_mode"
        private const val KEY_BYPASS_PACKAGE_RULES = "bypass_package_rules"
        private const val KEY_ONLY_WITH_PROGRESS = "only_with_progress"
        private const val KEY_TEXT_PROGRESS_ENABLED = "text_progress_enabled"
        private const val KEY_NETWORK_SPEED_ENABLED = "network_speed_enabled"
        private const val KEY_NETWORK_SPEED_DISPLAY_MODE = "network_speed_display_mode"
        private const val KEY_NETWORK_SPEED_UPLOAD_PREFIX = "network_speed_upload_prefix"
        private const val KEY_NETWORK_SPEED_DOWNLOAD_PREFIX = "network_speed_download_prefix"
        private const val KEY_NETWORK_SPEED_UNIT = "network_speed_unit"
        private const val KEY_NETWORK_SPEED_PRIORITIZE_UPLOAD =
            "network_speed_prioritize_upload"
        private const val KEY_NETWORK_SPEED_LOCKSCREEN_ONLY = "network_speed_lockscreen_only"
        private const val KEY_NETWORK_SPEED_DISABLE_CHIP_BACKGROUND =
            "network_speed_disable_chip_background"
        private const val KEY_CONVERTER_ENABLED = "converter_enabled"
        private const val KEY_KEEP_ALIVE_FOREGROUND_ENABLED = "keep_alive_foreground_enabled"
        private const val KEY_SYNC_DND_ENABLED = "sync_dnd_enabled"
        private const val KEY_UPDATE_CHECKS_ENABLED = "update_checks_enabled"
        private const val KEY_UPDATE_LAST_CHECK_AT_MS = "update_last_check_at_ms"
        private const val KEY_UPDATE_CACHED_LATEST_VERSION = "update_cached_latest_version"
        private const val KEY_UPDATE_CACHED_AVAILABLE = "update_cached_available"
        private const val KEY_UPDATE_LAST_NOTIFIED_VERSION = "update_last_notified_version"
        private const val KEY_AOSP_CUTTING_ENABLED = "aosp_cutting_enabled"
        private const val KEY_ANIMATED_ISLAND_ENABLED = "animated_island_enabled"
        private const val KEY_HYPERBRIDGE_ENABLED = "hyperbridge_enabled"
        private const val KEY_SMART_STATUS_ENABLED = "smart_status_enabled"
        private const val KEY_SMART_MEDIA_PLAYBACK_ENABLED = "smart_media_playback_enabled"
        private const val KEY_SMART_NAVIGATION_ENABLED = "smart_navigation_enabled"
        private const val KEY_SMART_WEATHER_ENABLED = "smart_weather_enabled"
        private const val KEY_SMART_EXTERNAL_DEVICES_ENABLED = "smart_external_devices_enabled"
        private const val KEY_SMART_VPN_ENABLED = "smart_vpn_enabled"
        private const val KEY_OTP_DETECTION_ENABLED = "otp_detection_enabled"
        private const val KEY_OTP_AUTO_COPY_ENABLED = "otp_auto_copy_enabled"
        private const val KEY_OTP_PACKAGE_RULES = "otp_package_rules"
        private const val KEY_OTP_PACKAGE_MODE = "otp_package_mode"
        private const val KEY_PIXEL_JOKE_BYPASS_ENABLED = "pixel_joke_bypass_enabled"
        private const val KEY_APP_LIST_ACCESS_GRANTED = "app_list_access_granted"
        private const val KEY_BACKGROUND_WARNING_DISMISSED = "background_warning_dismissed"
        private const val KEY_SAMSUNG_WARNING_DISMISSED = "samsung_warning_dismissed"
        private const val KEY_SAMSUNG_REMOTE_REPARSER_ENABLED = "samsung_remote_reparser_enabled"
        private const val KEY_EXPANDED_SECTIONS = "expanded_sections"
        private const val KEY_EXPANDED_SECTIONS_SET = "expanded_sections_set"
        private const val KEY_APP_PRESENTATION_OVERRIDES = "app_presentation_overrides"
        private const val KEY_USER_PARSER_DICTIONARY = "user_parser_dictionary"
        private const val KEY_CUSTOM_PARSER_DICTIONARY_LEGACY = "custom_parser_dictionary"

        private const val KEY_PACKAGE_FILTER_LEGACY = "package_filter"
        private const val DEFAULT_NETWORK_SPEED_UPLOAD_PREFIX = "▲ "
        private const val DEFAULT_NETWORK_SPEED_DOWNLOAD_PREFIX = "▼ "
    }
}
