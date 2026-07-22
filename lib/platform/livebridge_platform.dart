import 'package:flutter/services.dart';
import '../models/app_models.dart';

class LiveBridgePlatform {
  static const MethodChannel _channel = MethodChannel('livebridge/platform');
  static const Duration _appsTtl = Duration(minutes: 10);
  static List<InstalledApp>? _appsBox;
  static DateTime? _appsTs;

  static Future<bool> _askBool(
    String method, [
    Map<String, dynamic>? args,
  ]) async {
    final bool? res = await _channel.invokeMethod<bool>(method, args);
    return res ?? false;
  }

  static Future<String> _askStr(
    String method, [
    Map<String, dynamic>? args,
  ]) async {
    final String? res = await _channel.invokeMethod<String>(method, args);
    return res ?? '';
  }

  static Future<int> _askInt(
    String method, [
    Map<String, dynamic>? args,
  ]) async {
    final num? res = await _channel.invokeMethod<num>(method, args);
    return res?.toInt() ?? 0;
  }

  static Future<bool> isNotificationListenerEnabled() =>
      _askBool('isNotificationListenerEnabled');
  static Future<bool> openNotificationListenerSettings() =>
      _askBool('openNotificationListenerSettings');
  static Future<bool> isNotificationPermissionGranted() =>
      _askBool('isNotificationPermissionGranted');
  static Future<bool> requestNotificationPermission() =>
      _askBool('requestNotificationPermission');
  static Future<bool> canPostPromotedNotifications() =>
      _askBool('canPostPromotedNotifications');
  static Future<bool> openPromotedNotificationSettings() =>
      _askBool('openPromotedNotificationSettings');
  static Future<bool> openAppNotificationSettings() =>
      _askBool('openAppNotificationSettings');
  static Future<String> exportLiveBridgeSettingsBackup() =>
      _askStr('exportLiveBridgeSettingsBackup');
  static Future<String> saveLiveBridgeSettingsBackupToDownloads() =>
      _askStr('saveLiveBridgeSettingsBackupToDownloads');
  static Future<bool> importLiveBridgeSettingsBackup(String value) =>
      _askBool('importLiveBridgeSettingsBackup', {'value': value});

  static Future<String> getPackageRules() => _askStr('getPackageRules');
  static Future<bool> setPackageRules(String value) =>
      _askBool('setPackageRules', {'value': value});
  static Future<String> getPackageMode() => _askStr('getPackageMode');
  static Future<bool> setPackageMode(String value) =>
      _askBool('setPackageMode', {'value': value});
  static Future<String> getBypassPackageRules() =>
      _askStr('getBypassPackageRules');
  static Future<bool> setBypassPackageRules(String value) =>
      _askBool('setBypassPackageRules', {'value': value});

  static Future<bool> getOnlyWithProgress() => _askBool('getOnlyWithProgress');
  static Future<bool> setOnlyWithProgress(bool value) =>
      _askBool('setOnlyWithProgress', {'value': value});
  static Future<bool> getTextProgressEnabled() =>
      _askBool('getTextProgressEnabled');
  static Future<bool> setTextProgressEnabled(bool value) =>
      _askBool('setTextProgressEnabled', {'value': value});
  static Future<bool> getConverterEnabled() => _askBool('getConverterEnabled');
  static Future<bool> setConverterEnabled(bool value) =>
      _askBool('setConverterEnabled', {'value': value});
  static Future<bool> getKeepAliveForegroundEnabled() =>
      _askBool('getKeepAliveForegroundEnabled');
  static Future<bool> setKeepAliveForegroundEnabled(bool value) =>
      _askBool('setKeepAliveForegroundEnabled', {'value': value});
  static Future<bool> getSpringTransitionsEnabled() =>
      _askBool('getSpringTransitionsEnabled');
  static Future<bool> setSpringTransitionsEnabled(bool value) =>
      _askBool('setSpringTransitionsEnabled', {'value': value});
  static Future<bool> getPreventMirrorDismissEnabled() =>
      _askBool('getPreventMirrorDismissEnabled');
  static Future<bool> setPreventMirrorDismissEnabled(bool value) =>
      _askBool('setPreventMirrorDismissEnabled', {'value': value});
  static Future<bool> getHideLockscreenContentEnabled() =>
      _askBool('getHideLockscreenContentEnabled');
  static Future<bool> setHideLockscreenContentEnabled(bool value) =>
      _askBool('setHideLockscreenContentEnabled', {'value': value});
  static Future<bool> getConvertedNotificationSoundEnabled() =>
      _askBool('getConvertedNotificationSoundEnabled');
  static Future<bool> setConvertedNotificationSoundEnabled(bool value) =>
      _askBool('setConvertedNotificationSoundEnabled', {'value': value});
  static Future<bool> getHintsDisabled() => _askBool('getHintsDisabled');
  static Future<bool> setHintsDisabled(bool value) =>
      _askBool('setHintsDisabled', {'value': value});
  static Future<bool> getConversionLogEnabled() =>
      _askBool('getConversionLogEnabled');
  static Future<bool> setConversionLogEnabled(bool value) =>
      _askBool('setConversionLogEnabled', {'value': value});
  static Future<bool> getBugReportAutoCopyEnabled() =>
      _askBool('getBugReportAutoCopyEnabled');
  static Future<bool> setBugReportAutoCopyEnabled(bool value) =>
      _askBool('setBugReportAutoCopyEnabled', {'value': value});
  static Future<String> getAppLanguageTag() => _askStr('getAppLanguageTag');
  static Future<bool> setAppLanguageTag(String value) =>
      _askBool('setAppLanguageTag', {'value': value});
  static Future<int> getConversionLogMaxBytes() =>
      _askInt('getConversionLogMaxBytes');
  static Future<bool> setConversionLogMaxBytes(int value) =>
      _askBool('setConversionLogMaxBytes', {'value': value});
  static Future<String> getConversionLogEntries() =>
      _askStr('getConversionLogEntries');
  static Future<String> getConversionLogEntriesPage({
    required int offset,
    required int limit,
  }) => _askStr('getConversionLogEntriesPage', {
    'offset': offset,
    'limit': limit,
  });
  static Future<bool> getNetworkSpeedEnabled() =>
      _askBool('getNetworkSpeedEnabled');
  static Future<bool> setNetworkSpeedEnabled(bool value) =>
      _askBool('setNetworkSpeedEnabled', {'value': value});
  static Future<int> getNetworkSpeedMinThresholdBytesPerSecond() =>
      _askInt('getNetworkSpeedMinThresholdBytesPerSecond');
  static Future<bool> setNetworkSpeedMinThresholdBytesPerSecond(int value) =>
      _askBool('setNetworkSpeedMinThresholdBytesPerSecond', {'value': value});
  static Future<bool> getSyncDndEnabled() => _askBool('getSyncDndEnabled');
  static Future<bool> setSyncDndEnabled(bool value) =>
      _askBool('setSyncDndEnabled', {'value': value});
  static Future<bool> getUpdateChecksEnabled() =>
      _askBool('getUpdateChecksEnabled');
  static Future<bool> setUpdateChecksEnabled(bool value) =>
      _askBool('setUpdateChecksEnabled', {'value': value});
  static Future<int> getUpdateLastCheckAtMs() async {
    final num? value = await _channel.invokeMethod<num>(
      'getUpdateLastCheckAtMs',
    );
    return value?.toInt() ?? 0;
  }

  static Future<bool> setUpdateLastCheckAtMs(int value) =>
      _askBool('setUpdateLastCheckAtMs', {'value': value});
  static Future<String> getUpdateCachedLatestVersion() =>
      _askStr('getUpdateCachedLatestVersion');
  static Future<bool> setUpdateCachedLatestVersion(String value) =>
      _askBool('setUpdateCachedLatestVersion', {'value': value});
  static Future<bool> getUpdateCachedAvailable() =>
      _askBool('getUpdateCachedAvailable');
  static Future<bool> setUpdateCachedAvailable(bool value) =>
      _askBool('setUpdateCachedAvailable', {'value': value});
  static Future<String> getUpdateLastNotifiedVersion() =>
      _askStr('getUpdateLastNotifiedVersion');
  static Future<bool> setUpdateLastNotifiedVersion(String value) =>
      _askBool('setUpdateLastNotifiedVersion', {'value': value});
  static Future<String> getAppVersionName() => _askStr('getAppVersionName');
  static Future<bool> showUpdateAvailableNotification({
    required String version,
    required String releaseUrl,
  }) => _askBool('showUpdateAvailableNotification', {
    'version': version,
    'releaseUrl': releaseUrl,
  });
  static Future<bool> getAospCuttingEnabled() =>
      _askBool('getAospCuttingEnabled');
  static Future<bool> setAospCuttingEnabled(bool value) =>
      _askBool('setAospCuttingEnabled', {'value': value});
  static Future<int> getAospCuttingLength() => _askInt('getAospCuttingLength');
  static Future<bool> setAospCuttingLength(int value) =>
      _askBool('setAospCuttingLength', {'value': value});
  static Future<bool> getAnimatedIslandEnabled() =>
      _askBool('getAnimatedIslandEnabled');
  static Future<bool> setAnimatedIslandEnabled(bool value) =>
      _askBool('setAnimatedIslandEnabled', {'value': value});
  static Future<int> getAnimatedIslandUpdateFrequencyMs() =>
      _askInt('getAnimatedIslandUpdateFrequencyMs');
  static Future<bool> setAnimatedIslandUpdateFrequencyMs(int value) =>
      _askBool('setAnimatedIslandUpdateFrequencyMs', {'value': value});
  static Future<bool> getHyperBridgeEnabled() =>
      _askBool('getHyperBridgeEnabled');
  static Future<bool> setHyperBridgeEnabled(bool value) =>
      _askBool('setHyperBridgeEnabled', {'value': value});
  static Future<bool> getNotificationDedupEnabled() =>
      _askBool('getNotificationDedupEnabled');
  static Future<bool> setNotificationDedupEnabled(bool value) =>
      _askBool('setNotificationDedupEnabled', {'value': value});
  static Future<String> getNotificationDedupMode() =>
      _askStr('getNotificationDedupMode');
  static Future<bool> setNotificationDedupMode(String value) =>
      _askBool('setNotificationDedupMode', {'value': value});
  static Future<String> getNotificationDedupPackageRules() =>
      _askStr('getNotificationDedupPackageRules');
  static Future<bool> setNotificationDedupPackageRules(String value) =>
      _askBool('setNotificationDedupPackageRules', {'value': value});
  static Future<String> getNotificationDedupPackageMode() =>
      _askStr('getNotificationDedupPackageMode');
  static Future<bool> setNotificationDedupPackageMode(String value) =>
      _askBool('setNotificationDedupPackageMode', {'value': value});
  static Future<bool> getOtpRemoveOriginalMessageEnabled() =>
      _askBool('getOtpRemoveOriginalMessageEnabled');
  static Future<bool> setOtpRemoveOriginalMessageEnabled(bool value) =>
      _askBool('setOtpRemoveOriginalMessageEnabled', {'value': value});
  static Future<bool> getSmartRemoveOriginalMessageEnabled() =>
      _askBool('getSmartRemoveOriginalMessageEnabled');
  static Future<bool> setSmartRemoveOriginalMessageEnabled(bool value) =>
      _askBool('setSmartRemoveOriginalMessageEnabled', {'value': value});
  static Future<String> getSmartPackageRules() =>
      _askStr('getSmartPackageRules');
  static Future<bool> setSmartPackageRules(String value) =>
      _askBool('setSmartPackageRules', {'value': value});
  static Future<String> getSmartPackageMode() => _askStr('getSmartPackageMode');
  static Future<bool> setSmartPackageMode(String value) =>
      _askBool('setSmartPackageMode', {'value': value});
  static Future<bool> getSmartStatusDetectionEnabled() =>
      _askBool('getSmartStatusDetectionEnabled');
  static Future<bool> setSmartStatusDetectionEnabled(bool value) =>
      _askBool('setSmartStatusDetectionEnabled', {'value': value});
  static Future<bool> getSmartTaxiEnabled() => _askBool('getSmartTaxiEnabled');
  static Future<bool> setSmartTaxiEnabled(bool value) =>
      _askBool('setSmartTaxiEnabled', {'value': value});
  static Future<bool> getSmartDeliveryEnabled() =>
      _askBool('getSmartDeliveryEnabled');
  static Future<bool> setSmartDeliveryEnabled(bool value) =>
      _askBool('setSmartDeliveryEnabled', {'value': value});
  static Future<bool> getSmartCallsEnabled() =>
      _askBool('getSmartCallsEnabled');
  static Future<bool> setSmartCallsEnabled(bool value) =>
      _askBool('setSmartCallsEnabled', {'value': value});
  static Future<bool> getSmartMediaPlaybackEnabled() =>
      _askBool('getSmartMediaPlaybackEnabled');
  static Future<bool> setSmartMediaPlaybackEnabled(bool value) =>
      _askBool('setSmartMediaPlaybackEnabled', {'value': value});
  static Future<bool> getSmartMediaPlaybackShowOnLockScreen() =>
      _askBool('getSmartMediaPlaybackShowOnLockScreen');
  static Future<bool> setSmartMediaPlaybackShowOnLockScreen(bool value) =>
      _askBool('setSmartMediaPlaybackShowOnLockScreen', {'value': value});
  static Future<bool> getSmartMediaPlaybackUseSymbolsInPlayer() =>
      _askBool('getSmartMediaPlaybackUseSymbolsInPlayer');
  static Future<bool> setSmartMediaPlaybackUseSymbolsInPlayer(bool value) =>
      _askBool('setSmartMediaPlaybackUseSymbolsInPlayer', {'value': value});
  static Future<bool> getSmartNavigationEnabled() =>
      _askBool('getSmartNavigationEnabled');
  static Future<bool> setSmartNavigationEnabled(bool value) =>
      _askBool('setSmartNavigationEnabled', {'value': value});
  static Future<bool> getSmartWeatherEnabled() =>
      _askBool('getSmartWeatherEnabled');
  static Future<bool> setSmartWeatherEnabled(bool value) =>
      _askBool('setSmartWeatherEnabled', {'value': value});
  static Future<bool> getSmartExternalDevicesEnabled() =>
      _askBool('getSmartExternalDevicesEnabled');
  static Future<bool> setSmartExternalDevicesEnabled(bool value) =>
      _askBool('setSmartExternalDevicesEnabled', {'value': value});
  static Future<bool> getSmartExternalDevicesIgnoreDebugging() =>
      _askBool('getSmartExternalDevicesIgnoreDebugging');
  static Future<bool> setSmartExternalDevicesIgnoreDebugging(bool value) =>
      _askBool('setSmartExternalDevicesIgnoreDebugging', {'value': value});
  static Future<bool> getSmartVpnEnabled() => _askBool('getSmartVpnEnabled');
  static Future<bool> setSmartVpnEnabled(bool value) =>
      _askBool('setSmartVpnEnabled', {'value': value});
  static Future<bool> getOtpDetectionEnabled() =>
      _askBool('getOtpDetectionEnabled');
  static Future<bool> setOtpDetectionEnabled(bool value) =>
      _askBool('setOtpDetectionEnabled', {'value': value});
  static Future<bool> getOtpAutoCopyEnabled() =>
      _askBool('getOtpAutoCopyEnabled');
  static Future<bool> setOtpAutoCopyEnabled(bool value) =>
      _askBool('setOtpAutoCopyEnabled', {'value': value});

  static Future<String> getOtpPackageRules() => _askStr('getOtpPackageRules');
  static Future<bool> setOtpPackageRules(String value) =>
      _askBool('setOtpPackageRules', {'value': value});
  static Future<String> getOtpPackageMode() => _askStr('getOtpPackageMode');
  static Future<bool> setOtpPackageMode(String value) =>
      _askBool('setOtpPackageMode', {'value': value});

  static Future<List<InstalledApp>> getInstalledApps({
    bool forceRefresh = false,
  }) async {
    final DateTime ts = DateTime.now();
    if (!forceRefresh &&
        _appsBox != null &&
        _appsTs != null &&
        ts.difference(_appsTs!) <= _appsTtl) {
      return _appsBox!;
    }
    final List<dynamic>? res = await _channel.invokeMethod<List<dynamic>>(
      'getInstalledApps',
    );
    if (res == null) return <InstalledApp>[];

    final List<InstalledApp> apps = res
        .whereType<Map>()
        .map((Map e) {
          final Map<String, dynamic> m = Map<String, dynamic>.from(e);
          final String pkg = (m['packageName'] as String?) ?? '';
          return InstalledApp(
            packageName: pkg,
            label: (m['label'] as String?) ?? pkg,
            icon: m['icon'] is Uint8List ? m['icon'] as Uint8List : null,
            isSystem: m['isSystem'] == true,
          );
        })
        .where((app) => app.packageName.isNotEmpty)
        .toList();

    _appsBox = apps;
    _appsTs = ts;
    return apps;
  }

  static Future<bool> getAppListAccessGranted() =>
      _askBool('getAppListAccessGranted');
  static Future<bool> setAppListAccessGranted(bool value) =>
      _askBool('setAppListAccessGranted', {'value': value});
  static Future<bool> getBackgroundWarningDismissed() =>
      _askBool('getBackgroundWarningDismissed');
  static Future<bool> setBackgroundWarningDismissed(bool value) =>
      _askBool('setBackgroundWarningDismissed', {'value': value});
  static Future<bool> getSamsungWarningDismissed() =>
      _askBool('getSamsungWarningDismissed');
  static Future<bool> setSamsungWarningDismissed(bool value) =>
      _askBool('setSamsungWarningDismissed', {'value': value});
  static Future<bool> hasExpandedSectionsState() =>
      _askBool('hasExpandedSectionsState');
  static Future<String> getExpandedSections() => _askStr('getExpandedSections');
  static Future<bool> setExpandedSections(String value) =>
      _askBool('setExpandedSections', {'value': value});
  static Future<String> getAppPresentationOverrides() =>
      _askStr('getAppPresentationOverrides');
  static Future<bool> setAppPresentationOverrides(String value) =>
      _askBool('setAppPresentationOverrides', {'value': value});
  static Future<String> saveAppPresentationOverridesToDownloads() =>
      _askStr('saveAppPresentationOverridesToDownloads');
  static Future<bool> hasCustomParserDictionary() =>
      _askBool('hasCustomParserDictionary');
  static Future<String> getParserDictionaryJson() =>
      _askStr('getParserDictionaryJson');
  static Future<String> saveParserDictionaryToDownloads() =>
      _askStr('saveParserDictionaryToDownloads');
  static Future<bool> setCustomParserDictionary(String value) =>
      _askBool('setCustomParserDictionary', {'value': value});
  static Future<bool> clearCustomParserDictionary() =>
      _askBool('clearCustomParserDictionary');
  static Future<List<String>> getParserDictionaryEnabledLanguages() async {
    final List<dynamic>? res = await _channel.invokeMethod<List<dynamic>>(
      'getParserDictionaryEnabledLanguages',
    );
    return res
            ?.whereType<String>()
            .map((String value) => value.trim().toLowerCase())
            .where((String value) => value.isNotEmpty)
            .toList() ??
        const <String>[];
  }

  static Future<bool> setParserDictionaryEnabledLanguages(List<String> value) =>
      _askBool('setParserDictionaryEnabledLanguages', {'value': value});

  static Future<bool> setParserDictionaryLanguageOverride({
    required String languageId,
    required String value,
  }) => _askBool('setParserDictionaryLanguageOverride', {
    'languageId': languageId,
    'value': value,
  });

  static Future<DeviceInfo> getDeviceInfo() async {
    final Map<dynamic, dynamic>? res = await _channel
        .invokeMethod<Map<dynamic, dynamic>>('getDeviceInfo');
    final Map<String, dynamic> m = res == null
        ? const <String, dynamic>{}
        : Map<String, dynamic>.from(res);
    return DeviceInfo(
      manufacturer: (m['manufacturer'] as String?) ?? '',
      brand: (m['brand'] as String?) ?? '',
      marketName:
          (m['marketName'] as String?) ?? ((m['model'] as String?) ?? ''),
      model: (m['model'] as String?) ?? '',
      rawModel: (m['rawModel'] as String?) ?? '',
      product: (m['product'] as String?) ?? '',
      device: (m['device'] as String?) ?? '',
      board: (m['board'] as String?) ?? '',
      hardware: (m['hardware'] as String?) ?? '',
      bootloader: (m['bootloader'] as String?) ?? '',
      host: (m['host'] as String?) ?? '',
      id: (m['id'] as String?) ?? '',
      tags: (m['tags'] as String?) ?? '',
      type: (m['type'] as String?) ?? '',
      user: (m['user'] as String?) ?? '',
      fingerprint: (m['fingerprint'] as String?) ?? '',
      display: (m['display'] as String?) ?? '',
    );
  }
}
