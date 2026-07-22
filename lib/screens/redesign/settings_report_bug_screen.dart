import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_toast.dart';

class SettingsReportBugScreen extends StatefulWidget {
  const SettingsReportBugScreen({super.key});

  @override
  State<SettingsReportBugScreen> createState() =>
      _SettingsReportBugScreenState();
}

class _SettingsReportBugScreenState extends State<SettingsReportBugScreen> {
  static const String _projectGithubBugReportUrl =
      'https://github.com/appsfolder/livebridge/issues/new/choose?template=bug_report.yml';

  bool _autoCopyDebugJson = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadState());
    });
  }

  Future<void> _loadState() async {
    try {
      final bool autoCopyDebugJson =
          await LiveBridgePlatform.getBugReportAutoCopyEnabled();
      if (!mounted) {
        return;
      }
      setState(() => _autoCopyDebugJson = autoCopyDebugJson);
    } catch (_) {}
  }

  Future<void> _setAutoCopyDebugJson(bool value) async {
    if (value == _autoCopyDebugJson) {
      return;
    }
    setState(() => _autoCopyDebugJson = value);
    await LiveBridgePlatform.setBugReportAutoCopyEnabled(value);
  }

  List<String> _parseRulesText(String raw) {
    final List<String> values =
        raw
            .split(RegExp(r'[\s,\n\r\t;]+'))
            .map((String item) => item.trim())
            .where((String item) => item.isNotEmpty)
            .toSet()
            .toList()
          ..sort();
    return values;
  }

  Future<String> _buildDebugJson() async {
    final DateTime now = DateTime.now();
    final String localeTag = Localizations.localeOf(context).toLanguageTag();

    final Future<DeviceInfo> deviceInfoFuture =
        LiveBridgePlatform.getDeviceInfo();
    final Future<String> appVersionFuture =
        LiveBridgePlatform.getAppVersionName();
    final Future<String> latestVersionFuture =
        LiveBridgePlatform.getUpdateCachedLatestVersion();
    final Future<bool> updateAvailableFuture =
        LiveBridgePlatform.getUpdateCachedAvailable();
    final Future<bool> listenerEnabledFuture =
        LiveBridgePlatform.isNotificationListenerEnabled();
    final Future<bool> notificationsGrantedFuture =
        LiveBridgePlatform.isNotificationPermissionGranted();
    final Future<bool> canPostPromotedFuture =
        LiveBridgePlatform.canPostPromotedNotifications();
    final Future<bool> converterEnabledFuture =
        LiveBridgePlatform.getConverterEnabled();
    final Future<bool> keepAliveFuture =
        LiveBridgePlatform.getKeepAliveForegroundEnabled();
    final Future<bool> conversionLogEnabledFuture =
        LiveBridgePlatform.getConversionLogEnabled();
    final Future<int> conversionLogMaxBytesFuture =
        LiveBridgePlatform.getConversionLogMaxBytes();
    final Future<bool> networkSpeedEnabledFuture =
        LiveBridgePlatform.getNetworkSpeedEnabled();
    final Future<int> networkSpeedThresholdFuture =
        LiveBridgePlatform.getNetworkSpeedMinThresholdBytesPerSecond();
    final Future<bool> syncDndFuture = LiveBridgePlatform.getSyncDndEnabled();
    final Future<bool> preventDismissingFuture =
        LiveBridgePlatform.getPreventMirrorDismissEnabled();
    final Future<bool> hideLockscreenContentFuture =
        LiveBridgePlatform.getHideLockscreenContentEnabled();
    final Future<bool> convertedNotificationSoundFuture =
        LiveBridgePlatform.getConvertedNotificationSoundEnabled();
    final Future<bool> hintsDisabledFuture =
        LiveBridgePlatform.getHintsDisabled();
    final Future<bool> updateChecksFuture =
        LiveBridgePlatform.getUpdateChecksEnabled();
    final Future<String> appLanguageFuture =
        LiveBridgePlatform.getAppLanguageTag();
    final Future<bool> onlyWithProgressFuture =
        LiveBridgePlatform.getOnlyWithProgress();
    final Future<bool> textProgressFuture =
        LiveBridgePlatform.getTextProgressEnabled();
    final Future<bool> smartStatusFuture =
        LiveBridgePlatform.getSmartStatusDetectionEnabled();
    final Future<bool> smartMediaFuture =
        LiveBridgePlatform.getSmartMediaPlaybackEnabled();
    final Future<bool> smartMediaShowOnLockFuture =
        LiveBridgePlatform.getSmartMediaPlaybackShowOnLockScreen();
    final Future<bool> smartMediaUseSymbolsFuture =
        LiveBridgePlatform.getSmartMediaPlaybackUseSymbolsInPlayer();
    final Future<bool> smartCallsFuture =
        LiveBridgePlatform.getSmartCallsEnabled();
    final Future<bool> smartNavigationFuture =
        LiveBridgePlatform.getSmartNavigationEnabled();
    final Future<bool> smartWeatherFuture =
        LiveBridgePlatform.getSmartWeatherEnabled();
    final Future<bool> smartExternalDevicesFuture =
        LiveBridgePlatform.getSmartExternalDevicesEnabled();
    final Future<bool> smartExternalDevicesIgnoreDebuggingFuture =
        LiveBridgePlatform.getSmartExternalDevicesIgnoreDebugging();
    final Future<bool> smartVpnFuture = LiveBridgePlatform.getSmartVpnEnabled();
    final Future<bool> otpDetectionFuture =
        LiveBridgePlatform.getOtpDetectionEnabled();
    final Future<bool> otpAutoCopyFuture =
        LiveBridgePlatform.getOtpAutoCopyEnabled();
    final Future<bool> aospCuttingEnabledFuture =
        LiveBridgePlatform.getAospCuttingEnabled();
    final Future<int> aospCuttingLengthFuture =
        LiveBridgePlatform.getAospCuttingLength();
    final Future<bool> animatedIslandEnabledFuture =
        LiveBridgePlatform.getAnimatedIslandEnabled();
    final Future<int> animatedIslandFrequencyFuture =
        LiveBridgePlatform.getAnimatedIslandUpdateFrequencyMs();
    final Future<bool> hyperBridgeEnabledFuture =
        LiveBridgePlatform.getHyperBridgeEnabled();
    final Future<bool> notificationDedupEnabledFuture =
        LiveBridgePlatform.getNotificationDedupEnabled();
    final Future<String> notificationDedupModeFuture =
        LiveBridgePlatform.getNotificationDedupMode();
    final Future<String> packageModeFuture =
        LiveBridgePlatform.getPackageMode();
    final Future<String> packageRulesFuture =
        LiveBridgePlatform.getPackageRules();
    final Future<String> bypassPackageRulesFuture =
        LiveBridgePlatform.getBypassPackageRules();
    final Future<String> otpPackageModeFuture =
        LiveBridgePlatform.getOtpPackageMode();
    final Future<String> otpPackageRulesFuture =
        LiveBridgePlatform.getOtpPackageRules();
    final Future<String> smartPackageModeFuture =
        LiveBridgePlatform.getSmartPackageMode();
    final Future<String> smartPackageRulesFuture =
        LiveBridgePlatform.getSmartPackageRules();
    final Future<String> appPresentationOverridesFuture =
        LiveBridgePlatform.getAppPresentationOverrides();
    final Future<bool> hasCustomParserDictionaryFuture =
        LiveBridgePlatform.hasCustomParserDictionary();
    final Future<List<String>> parserDictionaryEnabledLanguagesFuture =
        LiveBridgePlatform.getParserDictionaryEnabledLanguages();

    final DeviceInfo deviceInfo = await deviceInfoFuture;
    final Map<String, dynamic> payload = <String, dynamic>{
      'schema': 'livebridge_bug_report_v2',
      'generated_at_utc': now.toUtc().toIso8601String(),
      'generated_at_local': now.toIso8601String(),
      'timezone_name': now.timeZoneName,
      'timezone_offset_minutes': now.timeZoneOffset.inMinutes,
      'locale': localeTag,
      'platform': <String, dynamic>{
        'os': Platform.operatingSystem,
        'os_version': Platform.operatingSystemVersion,
      },
      'app': <String, dynamic>{
        'version': await appVersionFuture,
        'latest_release_version': await latestVersionFuture,
        'update_available': await updateAvailableFuture,
      },
      'device': <String, dynamic>{
        'label': deviceInfo.label,
        'manufacturer': deviceInfo.manufacturer,
        'brand': deviceInfo.brand,
        'market_name': deviceInfo.marketName,
        'model': deviceInfo.model,
        'raw_model': deviceInfo.rawModel,
        'product': deviceInfo.product,
        'device': deviceInfo.device,
        'board': deviceInfo.board,
        'hardware': deviceInfo.hardware,
        'bootloader': deviceInfo.bootloader,
        'host': deviceInfo.host,
        'id': deviceInfo.id,
        'tags': deviceInfo.tags,
        'type': deviceInfo.type,
        'user': deviceInfo.user,
        'display': deviceInfo.display,
        'fingerprint': deviceInfo.fingerprint,
        'is_pixel': deviceInfo.isPixel,
        'is_samsung': deviceInfo.isSamsung,
        'is_aosp_device': deviceInfo.isAospDevice,
        'hide_live_updates_promotion':
            deviceInfo.shouldHideLiveUpdatesPromotion,
      },
      'permissions': <String, dynamic>{
        'listener_enabled': await listenerEnabledFuture,
        'notifications_granted': await notificationsGrantedFuture,
        'can_post_promoted': await canPostPromotedFuture,
      },
      'settings': <String, dynamic>{
        'converter_enabled': await converterEnabledFuture,
        'keep_alive_foreground_enabled': await keepAliveFuture,
        'conversion_log_enabled': await conversionLogEnabledFuture,
        'conversion_log_max_bytes': await conversionLogMaxBytesFuture,
        'network_speed_enabled': await networkSpeedEnabledFuture,
        'network_speed_min_threshold_bytes_per_second':
            await networkSpeedThresholdFuture,
        'sync_dnd_enabled': await syncDndFuture,
        'prevent_mirror_dismiss_enabled': await preventDismissingFuture,
        'hide_lockscreen_content_enabled': await hideLockscreenContentFuture,
        'converted_notification_sound_enabled':
            await convertedNotificationSoundFuture,
        'hints_disabled': await hintsDisabledFuture,
        'update_checks_enabled': await updateChecksFuture,
        'app_language': await appLanguageFuture,
        'only_with_progress': await onlyWithProgressFuture,
        'text_progress_enabled': await textProgressFuture,
        'smart_detection_enabled': await smartStatusFuture,
        'smart_media_playback_enabled': await smartMediaFuture,
        'smart_media_playback_show_on_lock_screen':
            await smartMediaShowOnLockFuture,
        'smart_media_playback_use_symbols_in_player':
            await smartMediaUseSymbolsFuture,
        'smart_calls_enabled': await smartCallsFuture,
        'smart_navigation_enabled': await smartNavigationFuture,
        'smart_weather_enabled': await smartWeatherFuture,
        'smart_external_devices_enabled': await smartExternalDevicesFuture,
        'smart_external_devices_ignore_debugging':
            await smartExternalDevicesIgnoreDebuggingFuture,
        'smart_vpn_enabled': await smartVpnFuture,
        'otp_detection_enabled': await otpDetectionFuture,
        'otp_auto_copy_enabled': await otpAutoCopyFuture,
        'aosp_cutting_enabled': await aospCuttingEnabledFuture,
        'aosp_cutting_length': await aospCuttingLengthFuture,
        'animated_island_enabled': await animatedIslandEnabledFuture,
        'animated_island_update_frequency_ms':
            await animatedIslandFrequencyFuture,
        'hyper_bridge_enabled': await hyperBridgeEnabledFuture,
        'notification_dedup_enabled': await notificationDedupEnabledFuture,
        'notification_dedup_mode': await notificationDedupModeFuture,
        'bug_report_auto_copy_enabled': _autoCopyDebugJson,
      },
      'rules': <String, dynamic>{
        'package_mode': await packageModeFuture,
        'package_rules': _parseRulesText(await packageRulesFuture),
        'bypass_package_rules': _parseRulesText(await bypassPackageRulesFuture),
        'otp_package_mode': await otpPackageModeFuture,
        'otp_package_rules': _parseRulesText(await otpPackageRulesFuture),
        'smart_package_mode': await smartPackageModeFuture,
        'smart_package_rules': _parseRulesText(await smartPackageRulesFuture),
      },
      'additional_state': <String, dynamic>{
        'has_custom_parser_dictionary': await hasCustomParserDictionaryFuture,
        'parser_dictionary_enabled_languages':
            await parserDictionaryEnabledLanguagesFuture,
        'app_presentation_overrides_length':
            (await appPresentationOverridesFuture).length,
      },
    };

    return const JsonEncoder.withIndent('  ').convert(payload);
  }

  Future<bool> _copyDebugJsonToClipboard() async {
    try {
      final String payload = await _buildDebugJson();
      await Clipboard.setData(ClipboardData(text: payload));
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<bool> _launchGithubUrl(Uri uri) async {
    final bool openedInBrowserView = await launchUrl(
      uri,
      mode: LaunchMode.inAppBrowserView,
    );
    if (openedInBrowserView) {
      return true;
    }
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  Future<void> _copyDebugJson() async {
    unawaited(LiveBridgeHaptics.confirm());
    final AppStrings strings = AppStrings.of(context);
    final bool copied = await _copyDebugJsonToClipboard();
    if (!mounted) {
      return;
    }
    showLbToast(
      context,
      message: copied ? strings.bugReportCopied : strings.bugReportCopyFailed,
      icon: copied ? LbIconSymbol.copyThree : null,
    );
  }

  Future<void> _openGithubBugPage() async {
    unawaited(LiveBridgeHaptics.openSurface());
    final AppStrings strings = AppStrings.of(context);
    if (_autoCopyDebugJson) {
      final bool copied = await _copyDebugJsonToClipboard();
      if (mounted) {
        showLbToast(
          context,
          message: copied
              ? strings.bugReportCopied
              : strings.bugReportCopyFailed,
          icon: copied ? LbIconSymbol.copyThree : null,
        );
      }
    }
    final bool opened = await _launchGithubUrl(
      Uri.parse(_projectGithubBugReportUrl),
    );
    if (!opened && mounted) {
      showLbToast(context, message: strings.githubOpenFailed);
    }
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);

    return LbDetailScreen(
      title: strings.reportBug,
      children: <Widget>[
        LbListComponent(
          items: <LbListItemData>[
            LbListItemData(
              title: strings.copyDebugJsonTitle,
              description: strings.copyDebugJsonDescription,
              showChevron: false,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.copy,
                size: 24,
                color: palette.textPrimary,
              ),
              onTap: () {
                unawaited(_copyDebugJson());
              },
            ),
            LbListItemData(
              title: strings.openGithubPageTitle,
              description: strings.openGithubPageDescription,
              showChevron: false,
              trailingWidget: LbIcon(
                symbol: LbIconSymbol.externalLink,
                size: 24,
                color: palette.textPrimary,
              ),
              onTap: () {
                unawaited(_openGithubBugPage());
              },
            ),
            LbListItemData(
              title: strings.autoCopyDebugJsonTitle,
              description: strings.autoCopyDebugJsonDescription,
              showChevron: false,
              toggleValue: _autoCopyDebugJson,
              onToggle: (bool value) {
                unawaited(_setAutoCopyDebugJson(value));
              },
              onTap: () {
                final bool nextValue = !_autoCopyDebugJson;
                unawaited(LiveBridgeHaptics.toggle(nextValue));
                unawaited(_setAutoCopyDebugJson(nextValue));
              },
            ),
          ],
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
      ],
    );
  }
}
