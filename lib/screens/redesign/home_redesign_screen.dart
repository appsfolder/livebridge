import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../models/conversion_log_entry.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../utils/version_compare.dart';
import '../../widgets/redesign/lb_bottom_floating_nav.dart';
import '../../widgets/redesign/lb_bottom_nav_item.dart';
import '../../widgets/redesign/lb_conversion_log_entry_sheet.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_installed_app_avatar.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_modal_bottom_sheet.dart';
import '../../widgets/redesign/lb_profile_card.dart';
import '../../widgets/redesign/lb_recent_empty_state.dart';
import '../../widgets/redesign/lb_stat_card.dart';
import '../../widgets/redesign/lb_status_card.dart';
import '../../widgets/redesign/lb_toast.dart';
import 'rules_dictionary_screen.dart';
import 'settings_app_config_screen.dart';
import 'settings_backup_restore_screen.dart';
import 'rules_otp_codes_screen.dart';
import 'rules_network_connections_screen.dart';
import 'rules_miscellaneous_screen.dart';
import 'rules_bypass_screen.dart';
import 'rules_per_app_behavior_screen.dart';
import 'rules_progress_screen.dart';
import 'rules_runtime.dart';
import 'rules_smart_conversion_screen.dart';
import 'settings_report_bug_screen.dart';
import 'settings_brand_specific_screen.dart';
import 'settings_experimental_screen.dart';
import 'settings_permissions_screen.dart';
import 'settings_support_screen.dart';
import 'settings_update_screen.dart';

class HomeRedesignScreen extends StatefulWidget {
  const HomeRedesignScreen({super.key});

  @override
  State<HomeRedesignScreen> createState() => _HomeRedesignScreenState();
}

class _HomeRedesignScreenState extends State<HomeRedesignScreen>
    with TickerProviderStateMixin {
  bool _isLiveBridgeRunning = true;
  bool _isConversionLogEnabled = false;
  bool _listenerEnabled = false;
  bool _notificationsGranted = false;
  bool _canPostPromoted = false;
  bool _hidePromotedAccess = false;
  bool _updateAvailable = false;
  String _currentAppVersion = 'v1.3.4';
  String _latestReleaseVersion = '';
  int _currentTabIndex = 0;
  bool _isDraggingNavSelector = false;
  int? _navMotionTargetIndex;
  int? _programmaticTargetIndex;
  List<ConversionLogEntry> _conversionLogEntries = const <ConversionLogEntry>[];
  bool _isConversionLogLoading = false;
  bool _hasMoreConversionLogEntries = false;
  int _conversionLogLoadGeneration = 0;
  Map<String, InstalledApp> _installedAppsByPackage =
      const <String, InstalledApp>{};
  late final ScrollController _homeScrollController;
  late final PageController _pageController;
  late final AnimationController _navMotionController;
  late final AnimationController _masterToggleWobbleController;
  late final Animation<double> _masterToggleWobbleOffset;

  static const int _navItemsCount = 3;
  static const int _conversionLogPageSize = 10;
  static const double _conversionLogLoadMoreExtent = 420;

  List<LbBottomNavData> _buildNavItems(AppStrings strings) {
    return <LbBottomNavData>[
      LbBottomNavData(
        label: strings.navHome,
        icon: LbIconSymbol.home,
        motion: LbBottomNavMotion.sway,
        activeIcon: LbIconSymbol.homeFilled,
      ),
      LbBottomNavData(
        label: strings.navRules,
        icon: LbIconSymbol.taskTwo,
        motion: LbBottomNavMotion.pop,
        activeIcon: LbIconSymbol.taskTwoFilled,
      ),
      LbBottomNavData(
        label: strings.navSettings,
        icon: LbIconSymbol.settings,
        motion: LbBottomNavMotion.spin,
        activeIcon: LbIconSymbol.settingsFilled,
      ),
    ];
  }

  @override
  void initState() {
    super.initState();
    _homeScrollController = ScrollController()..addListener(_handleHomeScroll);
    _pageController = PageController(initialPage: _currentTabIndex);
    _navMotionController =
        AnimationController(
          vsync: this,
          duration: const Duration(milliseconds: 480),
        )..addStatusListener((AnimationStatus status) {
          if (status == AnimationStatus.completed && mounted) {
            setState(() => _navMotionTargetIndex = null);
          }
        });
    _masterToggleWobbleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 280),
    );
    _masterToggleWobbleOffset =
        TweenSequence<double>(<TweenSequenceItem<double>>[
          TweenSequenceItem(tween: Tween<double>(begin: 0, end: 4), weight: 18),
          TweenSequenceItem(
            tween: Tween<double>(begin: 4, end: -4),
            weight: 24,
          ),
          TweenSequenceItem(
            tween: Tween<double>(begin: -4, end: 2.5),
            weight: 22,
          ),
          TweenSequenceItem(
            tween: Tween<double>(begin: 2.5, end: -2),
            weight: 18,
          ),
          TweenSequenceItem(
            tween: Tween<double>(begin: -2, end: 0),
            weight: 18,
          ),
        ]).animate(
          CurvedAnimation(
            parent: _masterToggleWobbleController,
            curve: Curves.easeOut,
          ),
        );
    unawaited(_loadLiveState());
  }

  @override
  void dispose() {
    _homeScrollController
      ..removeListener(_handleHomeScroll)
      ..dispose();
    _pageController.dispose();
    _navMotionController.dispose();
    _masterToggleWobbleController.dispose();
    super.dispose();
  }

  double get _pageProgress {
    if (!_pageController.hasClients) {
      return _currentTabIndex.toDouble();
    }
    return _pageController.page ?? _currentTabIndex.toDouble();
  }

  void _playNavMotion(int index) {
    setState(() => _navMotionTargetIndex = index);
    _navMotionController.forward(from: 0);
  }

  Future<void> _handleTabSelected(int index) async {
    final int baseIndex = _pageProgress.round();
    if (baseIndex == index) {
      return;
    }
    if (baseIndex == 0 && index != 0) {
      _collapseHomeConversionLog();
    }
    _programmaticTargetIndex = index;
    _playNavMotion(index);
    unawaited(LiveBridgeHaptics.selection());
    await _pageController.animateToPage(
      index,
      duration: Duration(milliseconds: 320 + ((index - baseIndex).abs() * 120)),
      curve: Curves.easeOutCubic,
    );
    _programmaticTargetIndex = null;
  }

  void _handleHomeScroll() {
    if (_currentTabIndex != 0 ||
        !_homeScrollController.hasClients ||
        !_isConversionLogEnabled ||
        !_hasMoreConversionLogEntries ||
        _isConversionLogLoading) {
      return;
    }

    final ScrollPosition position = _homeScrollController.position;
    if (position.extentAfter <= _conversionLogLoadMoreExtent) {
      unawaited(_loadMoreConversionLogEntries());
    }
  }

  void _collapseHomeConversionLog() {
    if (_homeScrollController.hasClients) {
      _homeScrollController.jumpTo(0);
    }

    if (_conversionLogEntries.length <= _conversionLogPageSize &&
        !_hasMoreConversionLogEntries) {
      return;
    }

    setState(() {
      _conversionLogEntries = _conversionLogEntries
          .take(_conversionLogPageSize)
          .toList(growable: false);
      _hasMoreConversionLogEntries = _isConversionLogEnabled;
      _isConversionLogLoading = false;
      _conversionLogLoadGeneration += 1;
    });
  }

  Future<void> _pushRulesDetailScreen(Widget screen) async {
    await Navigator.of(context).push<void>(
      PageRouteBuilder<void>(
        opaque: false,
        transitionDuration: const Duration(milliseconds: 340),
        reverseTransitionDuration: const Duration(milliseconds: 260),
        pageBuilder: (BuildContext context, _, __) => screen,
        transitionsBuilder:
            (
              BuildContext context,
              Animation<double> animation,
              Animation<double> secondaryAnimation,
              Widget child,
            ) {
              final Animation<double> curved = CurvedAnimation(
                parent: animation,
                curve: Curves.easeOutCubic,
                reverseCurve: Curves.easeInCubic,
              );

              return SlideTransition(
                position: Tween<Offset>(
                  begin: const Offset(1, 0),
                  end: Offset.zero,
                ).animate(curved),
                child: child,
              );
            },
      ),
    );
    if (mounted) {
      unawaited(_loadLiveState());
    }
  }

  bool get _canToggleMaster => _listenerEnabled && _notificationsGranted;
  bool get _displayMasterSwitchValue =>
      _canToggleMaster && _isLiveBridgeRunning;

  bool get _hasPermissionIssues {
    return !(_listenerEnabled &&
        _notificationsGranted &&
        (_hidePromotedAccess || _canPostPromoted));
  }

  String get _displayVersion {
    final String trimmed = _currentAppVersion.trim();
    if (trimmed.isEmpty) {
      return 'v1.3.4';
    }
    return trimmed.startsWith(RegExp(r'[vV]')) ? trimmed : 'v$trimmed';
  }

  String get _displayLatestReleaseVersion {
    final String trimmed = _latestReleaseVersion.trim();
    if (trimmed.isEmpty) {
      return '';
    }
    return trimmed.startsWith(RegExp(r'[vV]')) ? trimmed : 'v$trimmed';
  }

  String _statusCardTitle(AppStrings strings) {
    return _displayMasterSwitchValue
        ? strings.statusRunning
        : strings.statusDisabled;
  }

  Future<void> _loadLiveState() async {
    try {
      final Future<bool> listenerEnabledFuture =
          LiveBridgePlatform.isNotificationListenerEnabled();
      final Future<bool> notificationsGrantedFuture =
          LiveBridgePlatform.isNotificationPermissionGranted();
      final Future<bool> canPostPromotedFuture =
          LiveBridgePlatform.canPostPromotedNotifications();
      final Future<bool> converterEnabledFuture =
          LiveBridgePlatform.getConverterEnabled();
      final Future<bool> updateAvailableFuture =
          LiveBridgePlatform.getUpdateCachedAvailable();
      final Future<String> latestReleaseVersionFuture =
          LiveBridgePlatform.getUpdateCachedLatestVersion();
      final Future<String> currentAppVersionFuture =
          LiveBridgePlatform.getAppVersionName();
      final Future<DeviceInfo> deviceInfoFuture =
          LiveBridgePlatform.getDeviceInfo();
      final Future<bool> conversionLogEnabledFuture =
          LiveBridgePlatform.getConversionLogEnabled();

      final bool listenerEnabled = await listenerEnabledFuture;
      final bool notificationsGranted = await notificationsGrantedFuture;
      final bool canPostPromoted = await canPostPromotedFuture;
      final bool converterEnabled = await converterEnabledFuture;
      final bool updateAvailable = await updateAvailableFuture;
      final String latestReleaseVersion = await latestReleaseVersionFuture;
      final String currentAppVersion = await currentAppVersionFuture;
      final bool sanitizedUpdateAvailable =
          updateAvailable &&
          lbIsReleaseNewer(
            currentVersion: currentAppVersion,
            latestVersion: latestReleaseVersion,
          );
      final String sanitizedLatestReleaseVersion = sanitizedUpdateAvailable
          ? latestReleaseVersion
          : '';

      if (updateAvailable && !sanitizedUpdateAvailable) {
        unawaited(LiveBridgePlatform.setUpdateCachedAvailable(false));
        unawaited(LiveBridgePlatform.setUpdateCachedLatestVersion(''));
      }

      final DeviceInfo deviceInfo = await deviceInfoFuture;
      final bool conversionLogEnabled = await conversionLogEnabledFuture;

      if (!mounted) {
        return;
      }

      setState(() {
        _listenerEnabled = listenerEnabled;
        _notificationsGranted = notificationsGranted;
        _canPostPromoted = canPostPromoted;
        _hidePromotedAccess = deviceInfo.shouldHideLiveUpdatesPromotion;
        _isLiveBridgeRunning = converterEnabled;
        _updateAvailable = sanitizedUpdateAvailable;
        _latestReleaseVersion = sanitizedLatestReleaseVersion;
        _currentAppVersion = currentAppVersion;
        _isConversionLogEnabled = conversionLogEnabled;
      });
      unawaited(_loadInstalledAppsLookup());
      unawaited(
        _loadConversionLogEntries(enabledOverride: conversionLogEnabled),
      );
    } catch (_) {}
  }

  Future<void> _loadInstalledAppsLookup() async {
    try {
      final bool appListGranted =
          await LiveBridgePlatform.getAppListAccessGranted();
      if (!appListGranted) {
        if (!mounted) {
          return;
        }
        setState(
          () => _installedAppsByPackage = const <String, InstalledApp>{},
        );
        return;
      }

      final List<InstalledApp> apps =
          await LiveBridgePlatform.getInstalledApps();
      if (!mounted) {
        return;
      }

      setState(() {
        _installedAppsByPackage = <String, InstalledApp>{
          for (final InstalledApp app in apps)
            lbNormalizePackageName(app.packageName): app,
        };
      });
    } catch (_) {}
  }

  Future<void> _loadConversionLogEntries({bool? enabledOverride}) async {
    try {
      final bool enabled =
          enabledOverride ?? await LiveBridgePlatform.getConversionLogEnabled();
      if (!enabled) {
        if (!mounted) {
          return;
        }
        setState(() {
          _isConversionLogEnabled = false;
          _conversionLogEntries = const <ConversionLogEntry>[];
          _hasMoreConversionLogEntries = false;
          _isConversionLogLoading = false;
          _conversionLogLoadGeneration += 1;
        });
        return;
      }

      if (!mounted) {
        return;
      }
      final int generation = _conversionLogLoadGeneration + 1;
      setState(() {
        _isConversionLogEnabled = true;
        _conversionLogEntries = const <ConversionLogEntry>[];
        _hasMoreConversionLogEntries = false;
        _isConversionLogLoading = true;
        _conversionLogLoadGeneration = generation;
      });
      await _loadConversionLogPage(
        offset: 0,
        generation: generation,
        replaceEntries: true,
      );
    } catch (_) {}
  }

  Future<void> _loadMoreConversionLogEntries() async {
    if (_isConversionLogLoading ||
        !_isConversionLogEnabled ||
        !_hasMoreConversionLogEntries) {
      return;
    }

    await _loadConversionLogPage(
      offset: _conversionLogEntries.length,
      generation: _conversionLogLoadGeneration,
      replaceEntries: false,
    );
  }

  Future<void> _loadConversionLogPage({
    required int offset,
    required int generation,
    required bool replaceEntries,
  }) async {
    if (!mounted || generation != _conversionLogLoadGeneration) {
      return;
    }

    setState(() => _isConversionLogLoading = true);

    try {
      final String rawPage =
          await LiveBridgePlatform.getConversionLogEntriesPage(
            offset: offset,
            limit: _conversionLogPageSize,
          );
      final ConversionLogPage page = ConversionLogPage.decode(rawPage);

      if (!mounted || generation != _conversionLogLoadGeneration) {
        return;
      }

      setState(() {
        _isConversionLogEnabled = true;
        _conversionLogEntries = replaceEntries
            ? page.entries
            : <ConversionLogEntry>[..._conversionLogEntries, ...page.entries];
        _hasMoreConversionLogEntries = page.hasMore;
        _isConversionLogLoading = false;
      });
    } catch (_) {
      if (!mounted || generation != _conversionLogLoadGeneration) {
        return;
      }
      setState(() => _isConversionLogLoading = false);
    }
  }

  Future<void> _setConverterEnabled(bool value) async {
    if (!_canToggleMaster) {
      _masterToggleWobbleController.forward(from: 0);
      unawaited(LiveBridgeHaptics.blockedPulse());
      return;
    }
    unawaited(LiveBridgeHaptics.toggle(value));
    setState(() => _isLiveBridgeRunning = value);
    await LiveBridgePlatform.setConverterEnabled(value);
  }

  Future<bool> _launchExternalUrl(Uri uri) async {
    final bool openedInBrowserView = await launchUrl(
      uri,
      mode: LaunchMode.inAppBrowserView,
    );
    if (openedInBrowserView) {
      return true;
    }
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  Future<void> _openGithub() async {
    unawaited(LiveBridgeHaptics.openSurface());
    final bool opened = await _launchExternalUrl(
      Uri.parse('https://github.com/appsfolder/livebridge'),
    );
    if (!opened && mounted) {
      showLbToast(context, message: AppStrings.of(context).githubOpenFailed);
    }
  }

  Future<void> _openTelegramDiscussions() async {
    unawaited(LiveBridgeHaptics.openSurface());
    final bool opened = await _launchExternalUrl(
      Uri.parse('https://t.me/livebridge_dev'),
    );
    if (!opened && mounted) {
      showLbToast(context, message: AppStrings.of(context).linkOpenFailed);
    }
  }

  Future<void> _openRulesProgressScreen() {
    return _pushRulesDetailScreen(const RulesProgressScreen());
  }

  Future<void> _openRulesDictionaryScreen() {
    return _pushRulesDetailScreen(const RulesDictionaryScreen());
  }

  Future<void> _openRulesOtpCodesScreen() {
    return _pushRulesDetailScreen(const RulesOtpCodesScreen());
  }

  Future<void> _openRulesSmartConversionScreen() {
    return _pushRulesDetailScreen(const RulesSmartConversionScreen());
  }

  Future<void> _openRulesNetworkConnectionsScreen() {
    return _pushRulesDetailScreen(const RulesNetworkConnectionsScreen());
  }

  Future<void> _openRulesMiscellaneousScreen() {
    return _pushRulesDetailScreen(const RulesMiscellaneousScreen());
  }

  Future<void> _openRulesPerAppBehaviorScreen() {
    return _pushRulesDetailScreen(const RulesPerAppBehaviorScreen());
  }

  Future<void> _openRulesBypassScreen() {
    return _pushRulesDetailScreen(const RulesBypassScreen());
  }

  Future<void> _openPermissionsScreen() {
    return _pushRulesDetailScreen(const SettingsPermissionsScreen());
  }

  Future<void> _openExperimentalScreen() {
    return _pushRulesDetailScreen(const SettingsExperimentalScreen());
  }

  Future<void> _openBrandSpecificScreen() {
    return _pushRulesDetailScreen(const SettingsBrandSpecificScreen());
  }

  Future<void> _openAppConfigScreen() {
    return _pushRulesDetailScreen(const SettingsAppConfigScreen());
  }

  Future<void> _openReportBugScreen() {
    return _pushRulesDetailScreen(const SettingsReportBugScreen());
  }

  Future<void> _openBackupRestoreScreen() {
    return _pushRulesDetailScreen(const SettingsBackupRestoreScreen());
  }

  Future<void> _openSupportScreen() {
    return _pushRulesDetailScreen(const SettingsSupportScreen());
  }

  Future<void> _openUpdateSettingsScreen() {
    return _pushRulesDetailScreen(
      SettingsUpdateScreen(
        initialCurrentVersion: _currentAppVersion,
        initialUpdateAvailable: _updateAvailable,
      ),
    );
  }

  InstalledApp? _installedAppForPackage(String packageName) {
    return _installedAppsByPackage[lbNormalizePackageName(packageName)];
  }

  String _formatLogTime(int postedAtMs) {
    final DateTime time = DateTime.fromMillisecondsSinceEpoch(postedAtMs);
    final String hh = time.hour.toString().padLeft(2, '0');
    final String mm = time.minute.toString().padLeft(2, '0');
    final String ss = time.second.toString().padLeft(2, '0');
    return '$hh:$mm:$ss';
  }

  Future<void> _enableConversionLog() async {
    unawaited(LiveBridgeHaptics.selection());
    setState(() => _isConversionLogEnabled = true);
    await LiveBridgePlatform.setConversionLogEnabled(true);
    await _loadConversionLogEntries(enabledOverride: true);
  }

  Future<void> _copyPayload(String payload) async {
    await Clipboard.setData(ClipboardData(text: payload));
    if (!mounted) {
      return;
    }
    unawaited(LiveBridgeHaptics.confirm());
    showLbToast(
      context,
      message: AppStrings.of(context).payloadCopied,
      icon: LbIconSymbol.copyThree,
    );
  }

  Future<void> _openConversionLogEntry(ConversionLogEntry entry) async {
    unawaited(LiveBridgeHaptics.selection());
    await showLbModalBottomSheet<void>(
      context: context,
      builder: (BuildContext context) => LbConversionLogEntrySheet(
        entry: entry,
        app: _installedAppForPackage(entry.packageName),
        formattedTime: _formatLogTime(entry.postedAtMs),
        onCopyPayload: () {
          unawaited(_copyPayload(entry.payloadJson));
        },
      ),
    );
  }

  List<LbListItemData> _buildConversionLogItems() {
    return _conversionLogEntries
        .map((ConversionLogEntry entry) {
          return LbListItemData(
            title: entry.appLabel,
            leadingChild: LbInstalledAppAvatar(
              app:
                  _installedAppForPackage(entry.packageName) ??
                  InstalledApp(
                    packageName: entry.packageName,
                    label: entry.appLabel,
                  ),
              size: LbSpacing.recentAvatarSize,
            ),
            onTap: () {
              unawaited(_openConversionLogEntry(entry));
            },
          );
        })
        .toList(growable: false);
  }

  void _handleNavSelectorDragStart() {
    _navMotionController.stop();
    setState(() {
      _isDraggingNavSelector = true;
      _navMotionTargetIndex = null;
    });
  }

  void _handleNavSelectorDragUpdate(double progress) {
    if (!_pageController.hasClients) {
      return;
    }
    final ScrollPosition position = _pageController.position;
    final double targetPixels =
        (progress.clamp(0.0, (_navItemsCount - 1).toDouble()) *
                position.viewportDimension)
            .clamp(position.minScrollExtent, position.maxScrollExtent)
            .toDouble();
    position.jumpTo(targetPixels);
  }

  Future<void> _handleNavSelectorDragEnd(double progress) async {
    if (!mounted) {
      return;
    }
    final int targetIndex = progress.round().clamp(0, _navItemsCount - 1);
    setState(() => _isDraggingNavSelector = false);

    _programmaticTargetIndex = targetIndex;
    if (_currentTabIndex != targetIndex) {
      _playNavMotion(targetIndex);
      unawaited(LiveBridgeHaptics.selection());
    }
    await _pageController.animateToPage(
      targetIndex,
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
    );
    _programmaticTargetIndex = null;
  }

  Widget _buildTabScaffold({
    required double topInset,
    required double bottomInset,
    required LbPalette palette,
    required String title,
    required List<Widget> children,
    bool showOverflow = true,
    ScrollController? scrollController,
  }) {
    return RepaintBoundary(
      child: SingleChildScrollView(
        key: ValueKey<String>(title),
        controller: scrollController,
        physics: const BouncingScrollPhysics(),
        padding: EdgeInsets.fromLTRB(
          LbSpacing.screenHorizontal,
          0,
          LbSpacing.screenHorizontal,
          LbSpacing.screenBottom + bottomInset,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            SizedBox(height: topInset + LbSpacing.heroTopInset),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Expanded(
                  child: Text(
                    title,
                    style: LbTextStyles.display.copyWith(
                      color: palette.textPrimary,
                    ),
                  ),
                ),
                if (showOverflow)
                  Transform.translate(
                    offset: const Offset(0, -6),
                    child: Padding(
                      padding: EdgeInsets.only(
                        top: LbSpacing.headerIconTopInset,
                      ),
                      child: LbIcon(
                        symbol: LbIconSymbol.overflow,
                        size: LbSpacing.headerIconSize,
                        color: palette.textMuted,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: LbSpacing.titleToCardGap),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _buildHomeTab(double topInset, double bottomInset, LbPalette palette) {
    final AppStrings strings = AppStrings.of(context);
    return _buildTabScaffold(
      topInset: topInset,
      bottomInset: bottomInset,
      palette: palette,
      title: strings.heroTitle,
      showOverflow: false,
      scrollController: _homeScrollController,
      children: <Widget>[
        AnimatedBuilder(
          animation: _masterToggleWobbleController,
          builder: (BuildContext context, _) {
            return LbStatusCard(
              title: _statusCardTitle(strings),
              isActive: _displayMasterSwitchValue,
              secondaryText: strings.statusByPrefix,
              secondaryAccentText: 'appsfolder',
              secondaryAccentOnTap: () {
                unawaited(_openGithub());
              },
              toggleOffset: _masterToggleWobbleOffset.value,
              toggleHapticsEnabled: false,
              onToggle: (bool value) {
                unawaited(_setConverterEnabled(value));
              },
            );
          },
        ),
        const SizedBox(height: LbSpacing.statCardGap),
        Row(
          children: <Widget>[
            Expanded(
              child: LbStatCard(
                icon: LbIconSymbol.telegramFilled,
                title: strings.discussTitle,
                subtitle: strings.discussSubtitle,
                onTap: () {
                  unawaited(_openTelegramDiscussions());
                },
              ),
            ),
            const SizedBox(width: LbSpacing.statCardGap),
            Expanded(
              child: LbStatCard(
                icon: _hasPermissionIssues
                    ? LbIconSymbol.sad
                    : LbIconSymbol.happy,
                title: strings.accessTitle,
                subtitle: _hasPermissionIssues
                    ? strings.permissionCheckRequired
                    : strings.permissionsAllSet,
                isWarning: _hasPermissionIssues,
                onTap: () {
                  unawaited(LiveBridgeHaptics.selection());
                  unawaited(_openPermissionsScreen());
                },
              ),
            ),
            const SizedBox(width: LbSpacing.statCardGap),
            Expanded(
              child: LbStatCard(
                icon: LbIconSymbol.cloudArrowDownFilled,
                title:
                    _updateAvailable && _displayLatestReleaseVersion.isNotEmpty
                    ? _displayLatestReleaseVersion
                    : _displayVersion,
                subtitle: _updateAvailable
                    ? strings.versionTapToUpdate
                    : strings.versionLatestVersion,
                isWarning: _updateAvailable,
                onTap: () {
                  unawaited(LiveBridgeHaptics.selection());
                  unawaited(_openUpdateSettingsScreen());
                },
              ),
            ),
          ],
        ),
        const SizedBox(height: LbSpacing.md),
        if (_isConversionLogEnabled) ...<Widget>[
          Text(
            strings.recentConversions,
            style: LbTextStyles.title.copyWith(color: palette.textSecondary),
          ),
          const SizedBox(height: LbSpacing.recentSectionGap),
          if (_conversionLogEntries.isEmpty && !_isConversionLogLoading)
            SizedBox(
              width: double.infinity,
              height: LbSpacing.recentEmptyStateHeight,
              child: Center(
                child: Text(
                  strings.noConversionsYet,
                  style: LbTextStyles.title.copyWith(
                    color: palette.textSecondary,
                  ),
                ),
              ),
            )
          else if (_conversionLogEntries.isNotEmpty) ...<Widget>[
            LbListComponent(
              items: _buildConversionLogItems(),
              rowHeight: LbSpacing.recentRowHeight,
              leadingSize: LbSpacing.recentAvatarSize,
              leadingGap: LbSpacing.recentAvatarGap,
            ),
            if (_isConversionLogLoading)
              const SizedBox(height: LbSpacing.recentSectionGap),
          ],
        ] else ...<Widget>[
          LbRecentEmptyState(
            title: strings.conversionLogDisabled,
            actionLabel: strings.enable,
            onAction: () {
              unawaited(_enableConversionLog());
            },
          ),
        ],
      ],
    );
  }

  Widget _buildRulesTab(
    double topInset,
    double bottomInset,
    LbPalette palette,
  ) {
    final AppStrings strings = AppStrings.of(context);
    final List<LbListItemData> primaryItems = <LbListItemData>[
      LbListItemData(
        title: strings.progressTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openRulesProgressScreen());
        },
      ),
      LbListItemData(
        title: strings.otpCodesTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openRulesOtpCodesScreen());
        },
      ),
      LbListItemData(
        title: strings.smartConversionTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openRulesSmartConversionScreen());
        },
      ),
      LbListItemData(
        title: strings.networkConnectionsTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openRulesNetworkConnectionsScreen());
        },
      ),
      LbListItemData(
        title: strings.miscellaneousTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openRulesMiscellaneousScreen());
        },
      ),
    ];

    final List<LbListItemData> secondaryItems = <LbListItemData>[
      LbListItemData(
        title: strings.appPresentationSettings,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openRulesPerAppBehaviorScreen());
        },
      ),
      LbListItemData(
        title: strings.bypassTitle,
        description: strings.bypassDescription,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openRulesBypassScreen());
        },
      ),
    ];

    return _buildTabScaffold(
      topInset: topInset,
      bottomInset: bottomInset,
      palette: palette,
      title: strings.redesignRulesTitle,
      showOverflow: false,
      children: <Widget>[
        LbProfileCard(
          title: strings.dictionaryTitle,
          subtitle: strings.dictionaryManageSubtitle,
          icon: LbIconSymbol.translate,
          onTap: () {
            unawaited(LiveBridgeHaptics.selection());
            unawaited(_openRulesDictionaryScreen());
          },
        ),
        const SizedBox(height: LbSpacing.md),
        LbListComponent(
          items: primaryItems,
          rowHeight: LbSpacing.recentRowHeight,
        ),
        const SizedBox(height: LbSpacing.statCardGap),
        LbListComponent(
          items: secondaryItems,
          rowHeight: LbSpacing.recentRowHeight,
        ),
      ],
    );
  }

  Widget _buildSettingsTab(
    double topInset,
    double bottomInset,
    LbPalette palette,
  ) {
    final AppStrings strings = AppStrings.of(context);
    final List<LbListItemData> settingsItems = <LbListItemData>[
      LbListItemData(
        title: strings.experimentalTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openExperimentalScreen());
        },
      ),
      LbListItemData(
        title: strings.brandSpecificTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openBrandSpecificScreen());
        },
      ),
      LbListItemData(
        title: strings.accessTitle,
        trailingIcon: _hasPermissionIssues
            ? LbIconSymbol.alertOctagonFilled
            : null,
        trailingIconColor: _hasPermissionIssues ? palette.warning : null,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openPermissionsScreen());
        },
      ),
      LbListItemData(
        title: strings.appConfigTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openAppConfigScreen());
        },
      ),
      LbListItemData(
        title: strings.supportLiveBridgeTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openSupportScreen());
        },
      ),
      LbListItemData(
        title: strings.reportBug,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openReportBugScreen());
        },
      ),
      LbListItemData(
        title: strings.backupRestoreTitle,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_openBackupRestoreScreen());
        },
      ),
    ];

    return _buildTabScaffold(
      topInset: topInset,
      bottomInset: bottomInset,
      palette: palette,
      title: strings.settingsTitle,
      showOverflow: false,
      children: <Widget>[
        LbProfileCard(
          title: _updateAvailable
              ? strings.updateProfileNewVersionTitle
              : 'LiveBridge $_displayVersion',
          subtitle: _updateAvailable
              ? _displayLatestReleaseVersion.isNotEmpty
                    ? strings.updateProfileVersionSubtitle(
                        _displayVersion,
                        _displayLatestReleaseVersion,
                      )
                    : strings.updateProfileAvailableSubtitle
              : strings.updateProfileOpenSubtitle,
          backgroundColor: _updateAvailable ? palette.warningSurface : null,
          avatarChild: Transform.scale(
            scale: 1.12,
            child: Image.asset(
              'assets/icons/icon-master.png',
              width: LbSpacing.profileCardAvatarSize,
              height: LbSpacing.profileCardAvatarSize,
              fit: BoxFit.cover,
              filterQuality: FilterQuality.high,
            ),
          ),
          onTap: () {
            unawaited(LiveBridgeHaptics.selection());
            unawaited(_openUpdateSettingsScreen());
          },
        ),
        const SizedBox(height: LbSpacing.statCardGap),
        LbListComponent(
          items: settingsItems,
          rowHeight: LbSpacing.recentRowHeight,
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final double topInset = MediaQuery.paddingOf(context).top;
    final double bottomInset = MediaQuery.paddingOf(context).bottom;
    final Brightness brightness = Theme.of(context).brightness;
    final LbPalette palette = LbPalette.of(context);
    final AppStrings strings = AppStrings.of(context);
    final List<LbBottomNavData> navItems = _buildNavItems(strings);

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: LbAppTheme.overlayStyle(brightness),
      child: Scaffold(
        extendBody: true,
        backgroundColor: palette.background,
        body: Stack(
          children: <Widget>[
            PageView(
              controller: _pageController,
              allowImplicitScrolling: true,
              physics: const BouncingScrollPhysics(),
              onPageChanged: (int index) {
                if (_currentTabIndex == 0 && index != 0) {
                  _collapseHomeConversionLog();
                }
                if (_programmaticTargetIndex == null &&
                    !_isDraggingNavSelector) {
                  _playNavMotion(index);
                }
                if (_currentTabIndex != index) {
                  setState(() => _currentTabIndex = index);
                }
              },
              children: <Widget>[
                _buildHomeTab(topInset, bottomInset, palette),
                _buildRulesTab(topInset, bottomInset, palette),
                _buildSettingsTab(topInset, bottomInset, palette),
              ],
            ),
            Align(
              alignment: Alignment.bottomCenter,
              child: Padding(
                padding: EdgeInsets.fromLTRB(
                  LbSpacing.bottomNavHorizontalInset,
                  0,
                  LbSpacing.bottomNavHorizontalInset,
                  LbSpacing.bottomNavMarginBottom + bottomInset,
                ),
                child: AnimatedBuilder(
                  animation: Listenable.merge(<Listenable>[
                    _pageController,
                    _navMotionController,
                  ]),
                  builder: (BuildContext context, _) {
                    return LbBottomFloatingNav(
                      items: navItems,
                      currentIndex: _currentTabIndex,
                      selectionProgress: _pageProgress,
                      motionTargetIndex: _navMotionTargetIndex,
                      motionProgress: _navMotionController.value,
                      onItemSelected: (int index) {
                        unawaited(_handleTabSelected(index));
                      },
                      onSelectionDragStart: _handleNavSelectorDragStart,
                      onSelectionDragUpdate: _handleNavSelectorDragUpdate,
                      onSelectionDragEnd: (double progress) {
                        unawaited(_handleNavSelectorDragEnd(progress));
                      },
                    );
                  },
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
