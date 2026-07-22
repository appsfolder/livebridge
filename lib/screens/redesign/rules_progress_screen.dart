import 'dart:async';

import 'package:flutter/material.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_apps_loading_state.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_installed_app_avatar.dart';
import '../../widgets/redesign/lb_incremental_list_component.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_search_pill.dart';
import '../../widgets/redesign/lb_selection_indicator.dart';
import '../../widgets/redesign/lb_toast.dart';
import 'rules_runtime.dart';

class RulesProgressScreen extends StatefulWidget {
  const RulesProgressScreen({super.key});

  @override
  State<RulesProgressScreen> createState() => _RulesProgressScreenState();
}

class _RulesProgressScreenState extends State<RulesProgressScreen> {
  bool _nativeProgressEnabled = true;
  bool _textProgressEnabled = true;
  bool _showSystemApps = false;
  bool _isSearchExpanded = false;
  bool _isAppsLoading = true;
  String _searchQuery = '';
  LbRulesConversionMode _conversionMode = LbRulesConversionMode.allApps;
  Set<String> _selectedPackages = <String>{};
  List<InstalledApp> _apps = const <InstalledApp>[];
  int _appsLoadRequestId = 0;
  late final TextEditingController _searchController;
  late final FocusNode _searchFocusNode;

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController()
      ..addListener(() {
        final String nextQuery = _searchController.text;
        if (nextQuery == _searchQuery || !mounted) {
          return;
        }
        setState(() => _searchQuery = nextQuery);
      });
    _searchFocusNode = FocusNode();
    unawaited(_loadPrimaryState());
    WidgetsBinding.instance.addPostFrameCallback((_) {
      unawaited(_loadApps());
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    _searchFocusNode.dispose();
    super.dispose();
  }

  void _snack(String value) {
    if (!mounted) {
      return;
    }
    showLbToast(context, message: value);
  }

  Future<void> _loadPrimaryState() async {
    try {
      final Future<bool> nativeProgressEnabledFuture =
          LiveBridgePlatform.getOnlyWithProgress();
      final Future<bool> textProgressEnabledFuture =
          LiveBridgePlatform.getTextProgressEnabled();
      final Future<dynamic> packageModeFuture =
          LiveBridgePlatform.getPackageMode();
      final Future<String> packageRulesFuture =
          LiveBridgePlatform.getPackageRules();

      final bool nativeProgressEnabled = await nativeProgressEnabledFuture;
      final bool textProgressEnabled = await textProgressEnabledFuture;
      final PackageMode packageMode = PackageModeId.from(
        await packageModeFuture,
      );
      final Set<String> selectedPackages = lbParsePackageRules(
        await packageRulesFuture,
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _nativeProgressEnabled = nativeProgressEnabled;
        _textProgressEnabled = textProgressEnabled;
        _conversionMode = lbConversionModeFromPackageMode(packageMode);
        _selectedPackages = selectedPackages;
      });
    } catch (_) {}
  }

  Future<void> _loadApps() async {
    final int requestId = ++_appsLoadRequestId;
    final DateTime startedAt = DateTime.now();
    if (mounted) {
      setState(() => _isAppsLoading = true);
    }

    try {
      List<InstalledApp> apps = const <InstalledApp>[];
      if (mounted && await lbEnsureAppListAccess(context)) {
        apps = lbSortInstalledApps(await LiveBridgePlatform.getInstalledApps());
      }

      if (!mounted || requestId != _appsLoadRequestId) {
        return;
      }

      await lbWaitForMinimumAppsLoading(startedAt: startedAt);

      if (!mounted || requestId != _appsLoadRequestId) {
        return;
      }

      setState(() {
        _apps = apps;
        _isAppsLoading = false;
      });
    } catch (_) {
      if (!mounted || requestId != _appsLoadRequestId) {
        return;
      }
      await lbWaitForMinimumAppsLoading(startedAt: startedAt);
      if (!mounted || requestId != _appsLoadRequestId) {
        return;
      }
      setState(() => _isAppsLoading = false);
      _snack(AppStrings.of(context).appsLoadFailed);
    }
  }

  Future<void> _setNativeProgress(bool value) async {
    if (value == _nativeProgressEnabled) {
      return;
    }
    setState(() => _nativeProgressEnabled = value);
    await LiveBridgePlatform.setOnlyWithProgress(value);
  }

  Future<void> _setTextProgress(bool value) async {
    if (value == _textProgressEnabled) {
      return;
    }
    setState(() => _textProgressEnabled = value);
    await LiveBridgePlatform.setTextProgressEnabled(value);
  }

  Future<void> _setConversionMode(LbRulesConversionMode mode) async {
    if (mode == _conversionMode) {
      return;
    }
    setState(() => _conversionMode = mode);
    await LiveBridgePlatform.setPackageMode(
      lbPackageModeFromConversionMode(mode).id,
    );
  }

  void _toggleSystemApps() {
    unawaited(LiveBridgeHaptics.selection());
    setState(() => _showSystemApps = !_showSystemApps);
  }

  void _openSearch() {
    if (_isSearchExpanded) {
      return;
    }
    unawaited(LiveBridgeHaptics.selection());
    setState(() => _isSearchExpanded = true);
    Future<void>.delayed(const Duration(milliseconds: 250), () {
      if (!mounted || !_isSearchExpanded) {
        return;
      }
      _searchFocusNode.requestFocus();
    });
  }

  void _closeSearch() {
    if (!_isSearchExpanded && _searchQuery.isEmpty) {
      return;
    }
    unawaited(LiveBridgeHaptics.selection());
    _searchFocusNode.unfocus();
    _searchController.clear();
    setState(() {
      _isSearchExpanded = false;
      _searchQuery = '';
    });
  }

  Future<void> _setAppEnabled(String packageName, bool value) async {
    final String normalized = lbNormalizePackageName(packageName);
    final Set<String> next = Set<String>.from(_selectedPackages);
    if (value) {
      next.add(normalized);
    } else {
      next.remove(normalized);
    }
    setState(() => _selectedPackages = next);
    await LiveBridgePlatform.setPackageRules(lbEncodePackageRules(next));
  }

  List<LbListItemData> _buildAppItems() {
    final List<InstalledApp> visibleApps = lbBuildVisibleApps(
      apps: _apps,
      selectedPackages: _selectedPackages,
      showSystemApps: _showSystemApps,
      searchQuery: _searchQuery,
    );

    return visibleApps.map((InstalledApp app) {
      final bool isEnabled = _selectedPackages.contains(
        lbNormalizePackageName(app.packageName),
      );
      return LbListItemData(
        title: app.label,
        leadingChild: LbInstalledAppAvatar(
          app: app,
          size: LbSpacing.recentAvatarSize,
        ),
        showChevron: false,
        toggleValue: isEnabled,
        onToggle: (bool value) {
          unawaited(_setAppEnabled(app.packageName, value));
        },
        onTap: () {
          final bool nextValue = !isEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setAppEnabled(app.packageName, nextValue));
        },
      );
    }).toList();
  }

  Widget _buildAppsSection(LbPalette palette) {
    if (_isAppsLoading) {
      return const LbAppsLoadingState();
    }
    if (_apps.isEmpty) {
      return Padding(
        padding: const EdgeInsets.only(top: LbSpacing.lg),
        child: Text(
          AppStrings.of(context).appsLoadFailed,
          style: LbTextStyles.body.copyWith(color: palette.textSecondary),
        ),
      );
    }
    return LbIncrementalListComponent(
      key: ValueKey<Object>((_showSystemApps, _searchQuery)),
      items: _buildAppItems(),
      rowHeight: LbSpacing.recentRowHeight,
      leadingSize: LbSpacing.recentAvatarSize,
      leadingGap: LbSpacing.recentAvatarGap,
      extendDividersToEnd: true,
    );
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);

    final List<LbListItemData> progressItems = <LbListItemData>[
      LbListItemData(
        title: strings.nativeProgressTitle,
        description: strings.nativeProgressDescription,
        showChevron: false,
        toggleValue: _nativeProgressEnabled,
        onToggle: (bool value) {
          unawaited(_setNativeProgress(value));
        },
        onTap: () {
          final bool nextValue = !_nativeProgressEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setNativeProgress(nextValue));
        },
      ),
      LbListItemData(
        title: strings.textProgressTitle,
        description: strings.textProgressDescription,
        showChevron: false,
        toggleValue: _textProgressEnabled,
        onToggle: (bool value) {
          unawaited(_setTextProgress(value));
        },
        onTap: () {
          final bool nextValue = !_textProgressEnabled;
          unawaited(LiveBridgeHaptics.toggle(nextValue));
          unawaited(_setTextProgress(nextValue));
        },
      ),
    ];

    final List<LbListItemData> conversionItems = <LbListItemData>[
      LbListItemData(
        title: strings.allAppsTitle,
        description: strings.allAppsDescription,
        showChevron: false,
        trailingWidget: LbSelectionIndicator(
          selected: _conversionMode == LbRulesConversionMode.allApps,
        ),
        trailingWidgetWidth: LbSpacing.listAccessoryWidth,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_setConversionMode(LbRulesConversionMode.allApps));
        },
      ),
      LbListItemData(
        title: strings.onlySelectedTitle,
        description: strings.onlySelectedDescription,
        showChevron: false,
        trailingWidget: LbSelectionIndicator(
          selected: _conversionMode == LbRulesConversionMode.onlySelected,
        ),
        trailingWidgetWidth: LbSpacing.listAccessoryWidth,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_setConversionMode(LbRulesConversionMode.onlySelected));
        },
      ),
      LbListItemData(
        title: strings.excludeSelectedTitle,
        description: strings.excludeSelectedDescription,
        showChevron: false,
        trailingWidget: LbSelectionIndicator(
          selected: _conversionMode == LbRulesConversionMode.excludeSelected,
        ),
        trailingWidgetWidth: LbSpacing.listAccessoryWidth,
        onTap: () {
          unawaited(LiveBridgeHaptics.selection());
          unawaited(_setConversionMode(LbRulesConversionMode.excludeSelected));
        },
      ),
    ];

    return LbDetailScreen(
      title: strings.progressTitle,
      floatingBottom: _isAppsLoading
          ? null
          : LbSearchPill(
              expanded: _isSearchExpanded,
              controller: _searchController,
              focusNode: _searchFocusNode,
              onOpen: _openSearch,
              onClose: _closeSearch,
            ),
      floatingBottomReservedHeight: _isAppsLoading
          ? 0
          : LbSpacing.searchPillReservedHeight,
      children: <Widget>[
        LbListComponent(
          items: progressItems,
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
        const SizedBox(height: LbSpacing.detailSectionGap),
        Text(
          strings.conversionModeTitle,
          style: LbTextStyles.title.copyWith(color: palette.textSecondary),
        ),
        const SizedBox(height: LbSpacing.recentSectionGap),
        LbListComponent(
          items: conversionItems,
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        ),
        const SizedBox(height: LbSpacing.detailSectionGap),
        Row(
          children: <Widget>[
            Expanded(
              child: Text(
                strings.selectedAppsTitle,
                style: LbTextStyles.title.copyWith(
                  color: palette.textSecondary,
                ),
              ),
            ),
            GestureDetector(
              onTap: _toggleSystemApps,
              behavior: HitTestBehavior.opaque,
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: LbSpacing.xs,
                  vertical: LbSpacing.xs,
                ),
                child: Text(
                  _showSystemApps ? strings.hideSystem : strings.showSystem,
                  style: LbTextStyles.body.copyWith(color: palette.link),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: LbSpacing.recentSectionGap),
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 220),
          switchInCurve: Curves.easeOutCubic,
          switchOutCurve: Curves.easeInCubic,
          layoutBuilder: (Widget? currentChild, List<Widget> previousChildren) {
            return Stack(
              alignment: Alignment.topCenter,
              children: <Widget>[
                ...previousChildren,
                if (currentChild != null) currentChild,
              ],
            );
          },
          child: _buildAppsSection(palette),
        ),
      ],
    );
  }
}
