import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../l10n/app_strings.dart';
import '../../models/app_models.dart';
import '../../platform/livebridge_platform.dart';
import '../../theme/livebridge_tokens.dart';
import '../../utils/livebridge_haptics.dart';
import '../../widgets/redesign/lb_app_presentation_editor_sheet.dart';
import '../../widgets/redesign/lb_apps_loading_state.dart';
import '../../widgets/redesign/lb_defaults_card.dart';
import '../../widgets/redesign/lb_detail_screen.dart';
import '../../widgets/redesign/lb_icon.dart';
import '../../widgets/redesign/lb_installed_app_avatar.dart';
import '../../widgets/redesign/lb_incremental_list_component.dart';
import '../../widgets/redesign/lb_list_component.dart';
import '../../widgets/redesign/lb_modal_bottom_sheet.dart';
import '../../widgets/redesign/lb_search_pill.dart';
import '../../widgets/redesign/lb_toast.dart';
import 'app_presentation_runtime.dart';
import 'rules_runtime.dart';

class RulesPerAppBehaviorScreen extends StatefulWidget {
  const RulesPerAppBehaviorScreen({super.key});

  @override
  State<RulesPerAppBehaviorScreen> createState() =>
      _RulesPerAppBehaviorScreenState();
}

class _RulesPerAppBehaviorScreenState extends State<RulesPerAppBehaviorScreen> {
  bool _busy = false;
  bool _showSystemApps = false;
  bool _isSearchExpanded = false;
  bool _isAppsLoading = true;
  String _searchQuery = '';
  List<InstalledApp> _apps = const <InstalledApp>[];
  int _appsLoadRequestId = 0;
  late final TextEditingController _searchController;
  late final FocusNode _searchFocusNode;
  AppPresentationOverride _defaultOverride = const AppPresentationOverride();
  Map<String, AppPresentationOverride> _overrides =
      <String, AppPresentationOverride>{};

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
    unawaited(_loadOverrides());
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

  List<InstalledApp> get _visibleApps {
    return lbBuildVisibleApps(
      apps: _apps,
      showSystemApps: _showSystemApps,
      searchQuery: _searchQuery,
    );
  }

  void _snack(String value) {
    if (!mounted) {
      return;
    }
    showLbToast(context, message: value);
  }

  bool _isSameOverride(
    AppPresentationOverride left,
    AppPresentationOverride right,
  ) {
    return left.compactTextSource == right.compactTextSource &&
        left.iconSource == right.iconSource &&
        left.effectiveTitleSource == right.effectiveTitleSource &&
        left.effectiveContentSource == right.effectiveContentSource &&
        left.removeOriginalMessage == right.removeOriginalMessage;
  }

  Future<void> _loadOverrides() async {
    try {
      final String raw = await LiveBridgePlatform.getAppPresentationOverrides();
      final LbAppPresentationOverridesState parsed =
          lbParseAppPresentationOverrides(raw);
      final AppPresentationOverride normalizedDefault =
          parsed.defaultOverride.removeOriginalMessage
          ? parsed.defaultOverride.copyWith(removeOriginalMessage: false)
          : parsed.defaultOverride;
      final Map<String, AppPresentationOverride> normalizedOverrides =
          <String, AppPresentationOverride>{};
      for (final MapEntry<String, AppPresentationOverride> entry
          in parsed.packageOverrides.entries) {
        if (!_isSameOverride(entry.value, normalizedDefault)) {
          normalizedOverrides[entry.key] = entry.value;
        }
      }
      final bool shouldPersistNormalizedState =
          !_isSameOverride(parsed.defaultOverride, normalizedDefault) ||
          normalizedOverrides.length != parsed.packageOverrides.length;
      if (!mounted) {
        return;
      }
      setState(() {
        _defaultOverride = normalizedDefault;
        _overrides = normalizedOverrides;
      });
      if (shouldPersistNormalizedState) {
        unawaited(
          _persistOverrides(
            defaultOverride: normalizedDefault,
            overrides: normalizedOverrides,
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        _snack(AppStrings.of(context).appPresentationLoadFailed);
      }
    }
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
        apps = await LiveBridgePlatform.getInstalledApps();
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

  Future<void> _persistOverrides({
    required AppPresentationOverride defaultOverride,
    required Map<String, AppPresentationOverride> overrides,
  }) async {
    final bool saved = await LiveBridgePlatform.setAppPresentationOverrides(
      lbEncodeAppPresentationOverrides(
        LbAppPresentationOverridesState(
          defaultOverride: defaultOverride,
          packageOverrides: overrides,
        ),
      ),
    );
    if (!saved && mounted) {
      _snack(AppStrings.of(context).appPresentationSaveFailed);
    }
  }

  Future<void> _openDefaultsEditor() async {
    _searchFocusNode.unfocus();
    unawaited(LiveBridgeHaptics.openSurface());
    final AppStrings strings = AppStrings.of(context);
    await showLbModalBottomSheet<void>(
      context: context,
      builder: (BuildContext context) {
        return LbAppPresentationEditorSheet(
          title: strings.defaultsTitle,
          value: _defaultOverride,
          onChanged: (AppPresentationOverride updated) {
            final Map<String, AppPresentationOverride> nextOverrides =
                <String, AppPresentationOverride>{};
            for (final MapEntry<String, AppPresentationOverride> entry
                in _overrides.entries) {
              if (!_isSameOverride(entry.value, updated)) {
                nextOverrides[entry.key] = entry.value;
              }
            }
            setState(() {
              _defaultOverride = updated;
              _overrides = nextOverrides;
            });
            unawaited(
              _persistOverrides(
                defaultOverride: updated,
                overrides: nextOverrides,
              ),
            );
          },
        );
      },
    );
  }

  Future<void> _openAppEditor(InstalledApp app) async {
    _searchFocusNode.unfocus();
    final String key = lbNormalizePackageName(app.packageName);
    final AppPresentationOverride current = _overrides[key] ?? _defaultOverride;
    unawaited(LiveBridgeHaptics.openSurface());
    await showLbModalBottomSheet<void>(
      context: context,
      builder: (BuildContext context) {
        return LbAppPresentationEditorSheet(
          title: app.label,
          app: app,
          value: current,
          onChanged: (AppPresentationOverride updated) {
            final Map<String, AppPresentationOverride> nextOverrides =
                Map<String, AppPresentationOverride>.from(_overrides);
            if (_isSameOverride(updated, _defaultOverride)) {
              nextOverrides.remove(key);
            } else {
              nextOverrides[key] = updated;
            }
            setState(() => _overrides = nextOverrides);
            unawaited(
              _persistOverrides(
                defaultOverride: _defaultOverride,
                overrides: nextOverrides,
              ),
            );
          },
        );
      },
    );
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

  Future<void> _openOverflowMenu() async {
    if (_busy) {
      return;
    }
    _searchFocusNode.unfocus();
    unawaited(LiveBridgeHaptics.openSurface());
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);
    await showLbModalBottomSheet<void>(
      context: context,
      builder: (BuildContext sheetContext) {
        return LbListComponent(
          items: <LbListItemData>[
            LbListItemData(
              title: strings.exportLabel,
              showChevron: false,
              trailingIcon: LbIconSymbol.downloadThree,
              trailingIconColor: palette.textPrimary,
              onTap: () {
                Navigator.of(sheetContext).maybePop();
                unawaited(_downloadOverrides());
              },
            ),
            LbListItemData(
              title: strings.importLabel,
              showChevron: false,
              trailingIcon: LbIconSymbol.upload,
              trailingIconColor: palette.textPrimary,
              onTap: () {
                Navigator.of(sheetContext).maybePop();
                unawaited(_uploadOverrides());
              },
            ),
          ],
          rowHeight: LbSpacing.recentRowHeight,
          extendDividersToEnd: true,
        );
      },
    );
  }

  Future<void> _downloadOverrides() async {
    if (_busy) {
      return;
    }
    setState(() => _busy = true);
    unawaited(LiveBridgeHaptics.confirm());
    try {
      final String result =
          await LiveBridgePlatform.saveAppPresentationOverridesToDownloads();
      if (!mounted) {
        return;
      }
      _snack(
        result.trim().isEmpty
            ? AppStrings.of(context).appPresentationDownloadFailed
            : AppStrings.of(context).appPresentationSaved,
      );
    } catch (_) {
      if (mounted) {
        _snack(AppStrings.of(context).appPresentationDownloadFailed);
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  Future<void> _uploadOverrides() async {
    if (_busy) {
      return;
    }
    final AppStrings strings = AppStrings.of(context);
    setState(() => _busy = true);
    unawaited(LiveBridgeHaptics.confirm());
    try {
      final FilePickerResult? result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: const <String>['json'],
        withData: true,
      );
      if (result == null || result.files.isEmpty) {
        return;
      }

      final PlatformFile selected = result.files.first;
      final String raw = selected.bytes != null
          ? utf8.decode(selected.bytes!)
          : await File(selected.path!).readAsString();

      final bool saved = await LiveBridgePlatform.setAppPresentationOverrides(
        raw,
      );
      if (!mounted) {
        return;
      }
      if (!saved) {
        _snack(strings.appPresentationSaveFailed);
        return;
      }

      await _loadOverrides();
      if (!mounted) {
        return;
      }
      _snack(strings.appPresentationUploadDone);
    } on PlatformException catch (error) {
      if (!mounted) {
        return;
      }
      _snack(
        error.code == 'invalid_app_overrides'
            ? strings.appPresentationInvalidJson
            : strings.appPresentationUploadFailed,
      );
    } catch (_) {
      if (mounted) {
        _snack(strings.appPresentationUploadFailed);
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  List<LbListItemData> _buildAppRows() {
    return _visibleApps.map((InstalledApp app) {
      final String key = lbNormalizePackageName(app.packageName);
      final bool hasCustomOverride = _overrides.containsKey(key);
      return LbListItemData(
        title: app.label,
        leadingChild: LbInstalledAppAvatar(
          app: app,
          size: LbSpacing.recentAvatarSize,
        ),
        trailingIcon: hasCustomOverride ? LbIconSymbol.filterTwo : null,
        trailingIconColor: hasCustomOverride
            ? LbPalette.of(context).chevron
            : null,
        onTap: () {
          unawaited(_openAppEditor(app));
        },
      );
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final AppStrings strings = AppStrings.of(context);
    final LbPalette palette = LbPalette.of(context);

    return LbDetailScreen(
      title: strings.perAppSettingsTitle,
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
      trailing: GestureDetector(
        onTap: _busy ? null : () => unawaited(_openOverflowMenu()),
        behavior: HitTestBehavior.opaque,
        child: SizedBox.square(
          dimension: LbSpacing.headerIconSize + LbSpacing.sm,
          child: Center(
            child: LbIcon(
              symbol: LbIconSymbol.overflow,
              size: LbSpacing.headerIconSize,
              color: palette.textPrimary,
            ),
          ),
        ),
      ),
      children: <Widget>[
        LbDefaultsCard(
          title: strings.defaultsTitle,
          subtitle: strings.defaultsSubtitle,
          onTap: () {
            unawaited(_openDefaultsEditor());
          },
        ),
        const SizedBox(height: LbSpacing.detailSectionGap),
        Row(
          children: <Widget>[
            Expanded(
              child: Text(
                strings.appsListTitle,
                style: LbTextStyles.title.copyWith(
                  color: palette.textSecondary,
                ),
              ),
            ),
            GestureDetector(
              onTap: () {
                unawaited(LiveBridgeHaptics.selection());
                setState(() => _showSystemApps = !_showSystemApps);
              },
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
        if (_isAppsLoading)
          const LbAppsLoadingState()
        else
          LbIncrementalListComponent(
            key: ValueKey<Object>((_showSystemApps, _searchQuery)),
            items: _buildAppRows(),
            rowHeight: LbSpacing.recentRowHeight,
            leadingSize: LbSpacing.recentAvatarSize,
            leadingGap: LbSpacing.recentAvatarGap,
          ),
      ],
    );
  }
}
