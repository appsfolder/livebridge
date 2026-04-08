import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../l10n/app_strings.dart';
import '../models/app_models.dart';
import '../platform/livebridge_platform.dart';
import '../utils/livebridge_haptics.dart';
import '../widgets/shared_widgets.dart';

const String _defaultAppPresentationKey = '__default__';

enum _PerAppMenuAction {
  defaultBehavior,
  toggleSystemApps,
  downloadSettings,
  uploadSettings,
}

class _ParsedAppPresentationOverrides {
  const _ParsedAppPresentationOverrides({
    required this.defaultOverride,
    required this.packageOverrides,
  });

  final AppPresentationOverride defaultOverride;
  final Map<String, AppPresentationOverride> packageOverrides;
}

Future<void> showDefaultAppPresentationBehaviorEditor(
  BuildContext context,
) async {
  LiveBridgeHaptics.openSurface();

  try {
    final String raw = await LiveBridgePlatform.getAppPresentationOverrides();
    final _ParsedAppPresentationOverrides parsed =
        _parseStandaloneAppPresentationOverrides(raw);
    if (!context.mounted) {
      return;
    }

    final AppStrings s = AppStrings.of(context);
    AppPresentationOverride currentDefault = parsed.defaultOverride;
    Map<String, AppPresentationOverride> currentOverrides =
        Map<String, AppPresentationOverride>.from(parsed.packageOverrides);

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (BuildContext context) => _AppPresentationEditorSheet(
        app: InstalledApp(
          packageName: '',
          label: s.appPresentationDefaultSummary,
        ),
        initialValue: currentDefault,
        resetValue: const AppPresentationOverride(),
        showResetAction: false,
        onChanged: (AppPresentationOverride updated) {
          final Map<String, AppPresentationOverride> next =
              <String, AppPresentationOverride>{};
          for (final MapEntry<String, AppPresentationOverride> entry
              in currentOverrides.entries) {
            if (!_isSameStandaloneAppPresentationOverride(
              entry.value,
              updated,
            )) {
              next[entry.key] = entry.value;
            }
          }
          currentDefault = updated;
          currentOverrides = next;
          unawaited(
            _persistStandaloneAppPresentationOverrides(
              context,
              defaultOverride: updated,
              overrides: next,
            ),
          );
        },
      ),
    );
  } catch (_) {
    if (!context.mounted) {
      return;
    }
    _showStandaloneAppPresentationSnack(
      context,
      AppStrings.of(context).appPresentationLoadFailed,
    );
  }
}

bool _isSameStandaloneAppPresentationOverride(
  AppPresentationOverride a,
  AppPresentationOverride b,
) {
  return a.compactTextSource == b.compactTextSource &&
      a.iconSource == b.iconSource;
}

_ParsedAppPresentationOverrides _parseStandaloneAppPresentationOverrides(
  String raw,
) {
  final String normalized = raw.trim();
  if (normalized.isEmpty) {
    return const _ParsedAppPresentationOverrides(
      defaultOverride: AppPresentationOverride(),
      packageOverrides: <String, AppPresentationOverride>{},
    );
  }

  final dynamic decoded = jsonDecode(normalized);
  if (decoded is! Map) {
    return const _ParsedAppPresentationOverrides(
      defaultOverride: AppPresentationOverride(),
      packageOverrides: <String, AppPresentationOverride>{},
    );
  }

  AppPresentationOverride defaultOverride = const AppPresentationOverride();
  if (decoded[_defaultAppPresentationKey] is Map) {
    defaultOverride = AppPresentationOverride.fromJsonEntry(
      Map<String, dynamic>.from(decoded[_defaultAppPresentationKey] as Map),
    );
  }

  final Map<String, AppPresentationOverride> values =
      <String, AppPresentationOverride>{};
  for (final MapEntry<dynamic, dynamic> entry in decoded.entries) {
    final String packageName = (entry.key as String? ?? '')
        .trim()
        .toLowerCase();
    if (packageName.isEmpty ||
        packageName == _defaultAppPresentationKey ||
        entry.value is! Map) {
      continue;
    }
    final AppPresentationOverride parsed =
        AppPresentationOverride.fromJsonEntry(
          Map<String, dynamic>.from(entry.value as Map),
        );
    if (!_isSameStandaloneAppPresentationOverride(parsed, defaultOverride)) {
      values[packageName] = parsed;
    }
  }

  return _ParsedAppPresentationOverrides(
    defaultOverride: defaultOverride,
    packageOverrides: values,
  );
}

String _encodeStandaloneAppPresentationOverrides(
  AppPresentationOverride defaultOverride,
  Map<String, AppPresentationOverride> values,
) {
  final Map<String, dynamic> payload = <String, dynamic>{};
  if (!defaultOverride.isDefault) {
    payload[_defaultAppPresentationKey] = defaultOverride.toJsonEntry();
  }
  for (final String packageName in values.keys.toList()..sort()) {
    final AppPresentationOverride? value = values[packageName];
    if (value != null &&
        !_isSameStandaloneAppPresentationOverride(value, defaultOverride)) {
      payload[packageName] = value.toJsonEntry();
    }
  }
  return jsonEncode(payload);
}

Future<void> _persistStandaloneAppPresentationOverrides(
  BuildContext context, {
  required AppPresentationOverride defaultOverride,
  required Map<String, AppPresentationOverride> overrides,
}) async {
  final bool saved = await LiveBridgePlatform.setAppPresentationOverrides(
    _encodeStandaloneAppPresentationOverrides(defaultOverride, overrides),
  );
  if (!saved && context.mounted) {
    _showStandaloneAppPresentationSnack(
      context,
      AppStrings.of(context).appPresentationSaveFailed,
    );
  }
}

void _showStandaloneAppPresentationSnack(BuildContext context, String value) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(value)));
}

class AppPresentationSettingsPage extends StatefulWidget {
  const AppPresentationSettingsPage({super.key});

  @override
  State<AppPresentationSettingsPage> createState() =>
      _AppPresentationSettingsPageState();
}

class _AppPresentationSettingsPageState
    extends State<AppPresentationSettingsPage> {
  bool _isLoading = true;
  bool _busy = false;
  List<InstalledApp> _apps = const [];
  AppPresentationOverride _defaultOverride = const AppPresentationOverride();
  Map<String, AppPresentationOverride> _overrides = {};
  String _q = '';
  bool _showSystemApps = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  bool _isSameOverride(AppPresentationOverride a, AppPresentationOverride b) {
    return a.compactTextSource == b.compactTextSource &&
        a.iconSource == b.iconSource;
  }

  _ParsedAppPresentationOverrides _parseOverrides(String raw) {
    final normalized = raw.trim();
    if (normalized.isEmpty) {
      return const _ParsedAppPresentationOverrides(
        defaultOverride: AppPresentationOverride(),
        packageOverrides: <String, AppPresentationOverride>{},
      );
    }
    final decoded = jsonDecode(normalized);
    if (decoded is! Map) {
      return const _ParsedAppPresentationOverrides(
        defaultOverride: AppPresentationOverride(),
        packageOverrides: <String, AppPresentationOverride>{},
      );
    }

    AppPresentationOverride defaultOverride = const AppPresentationOverride();
    if (decoded[_defaultAppPresentationKey] is Map) {
      defaultOverride = AppPresentationOverride.fromJsonEntry(
        Map<String, dynamic>.from(decoded[_defaultAppPresentationKey] as Map),
      );
    }
    final Map<String, AppPresentationOverride> values = {};
    for (final entry in decoded.entries) {
      final packageName = (entry.key as String? ?? '').trim().toLowerCase();
      if (packageName.isEmpty ||
          packageName == _defaultAppPresentationKey ||
          entry.value is! Map) {
        continue;
      }
      final parsed = AppPresentationOverride.fromJsonEntry(
        Map<String, dynamic>.from(entry.value as Map),
      );
      if (!_isSameOverride(parsed, defaultOverride)) {
        values[packageName] = parsed;
      }
    }
    return _ParsedAppPresentationOverrides(
      defaultOverride: defaultOverride,
      packageOverrides: values,
    );
  }

  String _encodeOverrides(
    AppPresentationOverride defaultOverride,
    Map<String, AppPresentationOverride> values,
  ) {
    final payload = <String, dynamic>{};
    if (!defaultOverride.isDefault) {
      payload[_defaultAppPresentationKey] = defaultOverride.toJsonEntry();
    }
    for (final packageName in values.keys.toList()..sort()) {
      final value = values[packageName];
      if (value != null && !_isSameOverride(value, defaultOverride)) {
        payload[packageName] = value.toJsonEntry();
      }
    }
    return jsonEncode(payload);
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final apps = await LiveBridgePlatform.getInstalledApps();
      final raw = await LiveBridgePlatform.getAppPresentationOverrides();
      final parsed = _parseOverrides(raw);
      if (mounted) {
        setState(() {
          _apps = apps;
          _defaultOverride = parsed.defaultOverride;
          _overrides = parsed.packageOverrides;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() => _isLoading = false);
        _snack(AppStrings.of(context).appPresentationLoadFailed);
      }
    }
  }

  Future<void> _persistOverrides({
    required AppPresentationOverride defaultOverride,
    required Map<String, AppPresentationOverride> overrides,
  }) async {
    final saved = await LiveBridgePlatform.setAppPresentationOverrides(
      _encodeOverrides(defaultOverride, overrides),
    );
    if (!saved && mounted) {
      _snack(AppStrings.of(context).appPresentationSaveFailed);
    }
  }

  Future<void> _openEditor(InstalledApp app) async {
    LiveBridgeHaptics.openSurface();
    final String key = app.packageName.toLowerCase();
    final AppPresentationOverride current = _overrides[key] ?? _defaultOverride;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (context) => _AppPresentationEditorSheet(
        app: app,
        initialValue: current,
        resetValue: _defaultOverride,
        onChanged: (AppPresentationOverride updated) {
          final Map<String, AppPresentationOverride> next =
              Map<String, AppPresentationOverride>.from(_overrides);
          _isSameOverride(updated, _defaultOverride)
              ? next.remove(key)
              : next[key] = updated;
          if (mounted) {
            setState(() => _overrides = next);
          }
          unawaited(
            _persistOverrides(
              defaultOverride: _defaultOverride,
              overrides: next,
            ),
          );
        },
      ),
    );
  }

  Future<void> _openDefaultEditor() async {
    LiveBridgeHaptics.openSurface();
    final AppStrings s = AppStrings.of(context);
    final InstalledApp virtualApp = InstalledApp(
      packageName: '',
      label: s.appPresentationDefaultSummary,
    );

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (context) => _AppPresentationEditorSheet(
        app: virtualApp,
        initialValue: _defaultOverride,
        resetValue: const AppPresentationOverride(),
        showResetAction: false,
        onChanged: (AppPresentationOverride updated) {
          final Map<String, AppPresentationOverride> next =
              <String, AppPresentationOverride>{};
          for (final entry in _overrides.entries) {
            if (!_isSameOverride(entry.value, updated)) {
              next[entry.key] = entry.value;
            }
          }
          if (mounted) {
            setState(() {
              _defaultOverride = updated;
              _overrides = next;
            });
          }
          unawaited(
            _persistOverrides(defaultOverride: updated, overrides: next),
          );
        },
      ),
    );
  }

  Future<void> _downloadOverrides() async {
    if (_busy) return;
    LiveBridgeHaptics.confirm();
    setState(() => _busy = true);
    try {
      final res =
          await LiveBridgePlatform.saveAppPresentationOverridesToDownloads();
      if (!mounted) return;
      _snack(
        res.trim().isEmpty
            ? AppStrings.of(context).appPresentationDownloadFailed
            : AppStrings.of(context).appPresentationSaved,
      );
    } catch (_) {
      if (mounted) {
        _snack(AppStrings.of(context).appPresentationDownloadFailed);
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _uploadOverrides() async {
    if (_busy) return;
    LiveBridgeHaptics.confirm();
    setState(() => _busy = true);
    try {
      final res = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: const ['json'],
        withData: true,
      );
      if (res == null || res.files.isEmpty) return;

      final selected = res.files.first;
      String raw = selected.bytes != null
          ? utf8.decode(selected.bytes!)
          : await File(selected.path!).readAsString();

      final saved = await LiveBridgePlatform.setAppPresentationOverrides(raw);
      if (!mounted) return;
      if (!saved) {
        _snack(AppStrings.of(context).appPresentationSaveFailed);
        return;
      }

      _snack(AppStrings.of(context).appPresentationUploadDone);
      await _load();
    } on PlatformException catch (e) {
      if (mounted) {
        _snack(
          e.code == 'invalid_app_overrides'
              ? AppStrings.of(context).appPresentationInvalidJson
              : AppStrings.of(context).appPresentationUploadFailed,
        );
      }
    } catch (_) {
      if (mounted) {
        _snack(AppStrings.of(context).appPresentationUploadFailed);
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  String _summaryFor(InstalledApp app) {
    final s = AppStrings.of(context);
    final String key = app.packageName.toLowerCase();
    final value = _overrides[key] ?? _defaultOverride;

    if (_isSameOverride(value, _defaultOverride)) {
      return s.appPresentationDefaultSummary;
    }

    final textSummary = value.compactTextSource == AppCompactTextSource.title
        ? s.appPresentationTextTitle
        : s.appPresentationTextNotification;
    final iconSummary =
        value.iconSource == AppNotificationIconSource.notification
        ? s.appPresentationIconNotification
        : s.appPresentationIconApp;
    return '$textSummary • $iconSummary';
  }

  void _snack(String value) {
    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(value)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);
    final colorScheme = Theme.of(context).colorScheme;
    final Color popupMenuColor = colorScheme.brightness == Brightness.light
        ? Colors.white
        : colorScheme.surfaceContainer;

    final paddingTop = MediaQuery.paddingOf(context).top;
    final paddingBottom = MediaQuery.paddingOf(context).bottom;

    final filtered = _apps.where((app) {
      if (!_showSystemApps && app.isSystem) {
        return false;
      }
      if (_q.isEmpty) return true;
      final q = _q.toLowerCase();
      return app.label.toLowerCase().contains(q) ||
          app.packageName.toLowerCase().contains(q);
    }).toList();

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      extendBodyBehindAppBar: true,
      extendBody: true,
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Stack(
              children: [
                ListView.builder(
                  padding: EdgeInsets.only(
                    top: paddingTop + 88,
                    bottom: paddingBottom + 24,
                    left: 16,
                    right: 16,
                  ),
                  physics: const AlwaysScrollableScrollPhysics(
                    parent: BouncingScrollPhysics(),
                  ),
                  itemCount: filtered.length,
                  itemBuilder: (context, index) {
                    final app = filtered[index];
                    final hasCustom = _overrides.containsKey(
                      app.packageName.toLowerCase(),
                    );
                    final bool isDark =
                        colorScheme.brightness == Brightness.dark;
                    final shadowOpacity =
                        colorScheme.brightness == Brightness.dark ? 0.28 : 0.03;

                    return Container(
                      margin: const EdgeInsets.only(bottom: 12),
                      decoration: BoxDecoration(
                        color: isDark
                            ? colorScheme.surfaceContainerLow
                            : Colors.white,
                        borderRadius: BorderRadius.circular(24),
                        boxShadow: [
                          BoxShadow(
                            color: isDark
                                ? colorScheme.shadow.withValues(
                                    alpha: shadowOpacity,
                                  )
                                : Colors.black.withValues(alpha: shadowOpacity),
                            blurRadius: 16,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: Material(
                        color: Colors.transparent,
                        child: InkWell(
                          borderRadius: BorderRadius.circular(24),
                          onTap: () => _openEditor(app),
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Row(
                              children: [
                                InstalledAppAvatar(app: app),
                                const SizedBox(width: 16),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        app.label,
                                        style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 16,
                                        ),
                                      ),
                                      const SizedBox(height: 2),
                                      Text(
                                        app.packageName,
                                        style: TextStyle(
                                          color: isDark
                                              ? colorScheme.onSurfaceVariant
                                              : Colors.grey[500],
                                          fontSize: 12,
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        _summaryFor(app),
                                        style: TextStyle(
                                          color: colorScheme.primary.withValues(
                                            alpha: 0.8,
                                          ),
                                          fontSize: 13,
                                          fontWeight: FontWeight.w500,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                if (hasCustom)
                                  Container(
                                    padding: const EdgeInsets.all(8),
                                    decoration: BoxDecoration(
                                      color: colorScheme.primary.withValues(
                                        alpha:
                                            colorScheme.brightness ==
                                                Brightness.dark
                                            ? 0.2
                                            : 0.1,
                                      ),
                                      shape: BoxShape.circle,
                                    ),
                                    child: Icon(
                                      Icons.tune_rounded,
                                      color: colorScheme.primary,
                                      size: 20,
                                    ),
                                  ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),

                Positioned(
                  top: paddingTop + 12,
                  left: 16,
                  right: 16,
                  child: Container(
                    height: 56,
                    decoration: BoxDecoration(
                      color: colorScheme.brightness == Brightness.dark
                          ? colorScheme.surfaceContainerHigh
                          : Colors.white,
                      borderRadius: BorderRadius.circular(28),
                      boxShadow: [
                        BoxShadow(
                          color: colorScheme.brightness == Brightness.dark
                              ? colorScheme.shadow.withValues(alpha: 0.34)
                              : Colors.black.withValues(alpha: 0.08),
                          blurRadius: 24,
                          offset: const Offset(0, 8),
                        ),
                      ],
                    ),
                    child: Row(
                      children: [
                        const SizedBox(width: 4),
                        IconButton(
                          icon: const Icon(Icons.arrow_back_rounded),
                          color: colorScheme.brightness == Brightness.dark
                              ? colorScheme.onSurface
                              : Colors.grey[800],
                          onPressed: () {
                            LiveBridgeHaptics.selection();
                            Navigator.maybePop(context);
                          },
                        ),
                        Expanded(
                          child: TextField(
                            onChanged: (val) => setState(() => _q = val.trim()),
                            decoration: InputDecoration(
                              hintText: s.searchAppHint,
                              border: InputBorder.none,
                              hintStyle: TextStyle(
                                color: colorScheme.brightness == Brightness.dark
                                    ? colorScheme.onSurfaceVariant.withValues(
                                        alpha: 0.8,
                                      )
                                    : Colors.grey[400],
                                fontSize: 16,
                              ),
                            ),
                            style: const TextStyle(fontSize: 16),
                          ),
                        ),
                        PopupMenuButton<_PerAppMenuAction>(
                          icon: const Icon(Icons.more_vert_rounded),
                          tooltip: s.appPresentationSettings,
                          enabled: !_busy,
                          color: popupMenuColor,
                          elevation: 10,
                          position: PopupMenuPosition.under,
                          menuPadding: const EdgeInsets.all(6),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(18),
                            side: BorderSide(
                              color: colorScheme.outlineVariant.withValues(
                                alpha: 0.45,
                              ),
                            ),
                          ),
                          onSelected: (_PerAppMenuAction action) {
                            switch (action) {
                              case _PerAppMenuAction.defaultBehavior:
                                unawaited(_openDefaultEditor());
                                break;
                              case _PerAppMenuAction.toggleSystemApps:
                                LiveBridgeHaptics.toggle(!_showSystemApps);
                                setState(
                                  () => _showSystemApps = !_showSystemApps,
                                );
                                break;
                              case _PerAppMenuAction.downloadSettings:
                                unawaited(_downloadOverrides());
                                break;
                              case _PerAppMenuAction.uploadSettings:
                                unawaited(_uploadOverrides());
                                break;
                            }
                          },
                          itemBuilder: (BuildContext context) =>
                              <PopupMenuEntry<_PerAppMenuAction>>[
                                PopupMenuItem<_PerAppMenuAction>(
                                  value: _PerAppMenuAction.defaultBehavior,
                                  child: Row(
                                    children: <Widget>[
                                      const Icon(Icons.tune_rounded, size: 18),
                                      const SizedBox(width: 10),
                                      Expanded(
                                        child: Text(
                                          s.appPresentationDefaultSummary,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                PopupMenuItem<_PerAppMenuAction>(
                                  value: _PerAppMenuAction.toggleSystemApps,
                                  child: Row(
                                    children: <Widget>[
                                      Icon(
                                        _showSystemApps
                                            ? Icons.visibility_off_rounded
                                            : Icons.visibility_rounded,
                                        size: 18,
                                      ),
                                      const SizedBox(width: 10),
                                      Expanded(
                                        child: Text(
                                          _showSystemApps
                                              ? s.hideSystemApps
                                              : s.showSystemApps,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                PopupMenuItem<_PerAppMenuAction>(
                                  value: _PerAppMenuAction.downloadSettings,
                                  child: Row(
                                    children: <Widget>[
                                      const Icon(
                                        Icons.download_rounded,
                                        size: 18,
                                      ),
                                      const SizedBox(width: 10),
                                      Expanded(child: Text(s.downloadSettings)),
                                    ],
                                  ),
                                ),
                                PopupMenuItem<_PerAppMenuAction>(
                                  value: _PerAppMenuAction.uploadSettings,
                                  child: Row(
                                    children: <Widget>[
                                      const Icon(
                                        Icons.upload_file_rounded,
                                        size: 18,
                                      ),
                                      const SizedBox(width: 10),
                                      Expanded(child: Text(s.uploadSettings)),
                                    ],
                                  ),
                                ),
                              ],
                        ),
                        const SizedBox(width: 4),
                      ],
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}

class _AppPresentationEditorSheet extends StatefulWidget {
  const _AppPresentationEditorSheet({
    required this.app,
    required this.initialValue,
    required this.resetValue,
    this.showResetAction = true,
    required this.onChanged,
  });
  final InstalledApp app;
  final AppPresentationOverride initialValue;
  final AppPresentationOverride resetValue;
  final bool showResetAction;
  final ValueChanged<AppPresentationOverride> onChanged;

  @override
  State<_AppPresentationEditorSheet> createState() =>
      _AppPresentationEditorSheetState();
}

class _AppPresentationEditorSheetState
    extends State<_AppPresentationEditorSheet> {
  late AppCompactTextSource _compactTextSource =
      widget.initialValue.compactTextSource;
  late AppNotificationIconSource _iconSource = widget.initialValue.iconSource;

  AppPresentationOverride _currentValue() {
    return AppPresentationOverride(
      compactTextSource: _compactTextSource,
      iconSource: _iconSource,
    );
  }

  void _emitChanged() {
    widget.onChanged(_currentValue());
  }

  @override
  Widget build(BuildContext context) {
    final s = AppStrings.of(context);

    return SafeArea(
      top: false,
      child: Padding(
        padding: EdgeInsets.only(
          left: 24,
          right: 24,
          top: 8,
          bottom: 24 + MediaQuery.of(context).viewInsets.bottom,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                InstalledAppAvatar(app: widget.app),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.app.label,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      if (widget.app.packageName.trim().isNotEmpty)
                        Text(
                          widget.app.packageName,
                          style: Theme.of(context).textTheme.bodyMedium
                              ?.copyWith(
                                color: Theme.of(
                                  context,
                                ).colorScheme.onSurfaceVariant,
                              ),
                        ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 32),
            Text(
              s.appPresentationTextSourceLabel,
              style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
            ),
            const SizedBox(height: 12),
            LiveBridgeToggleSelector<AppCompactTextSource>(
              value: _compactTextSource,
              options: <SelectorOption<AppCompactTextSource>>[
                SelectorOption<AppCompactTextSource>(
                  value: AppCompactTextSource.title,
                  title: s.appPresentationTextTitle,
                ),
                SelectorOption<AppCompactTextSource>(
                  value: AppCompactTextSource.text,
                  title: s.appPresentationTextNotification,
                ),
              ],
              onChanged: (AppCompactTextSource next) {
                if (_compactTextSource == next) return;
                setState(() => _compactTextSource = next);
                _emitChanged();
              },
            ),
            const SizedBox(height: 24),
            Text(
              s.appPresentationIconSourceLabel,
              style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
            ),
            const SizedBox(height: 12),
            LiveBridgeToggleSelector<AppNotificationIconSource>(
              value: _iconSource,
              options: <SelectorOption<AppNotificationIconSource>>[
                SelectorOption<AppNotificationIconSource>(
                  value: AppNotificationIconSource.notification,
                  title: s.appPresentationIconNotification,
                ),
                SelectorOption<AppNotificationIconSource>(
                  value: AppNotificationIconSource.app,
                  title: s.appPresentationIconApp,
                ),
              ],
              onChanged: (AppNotificationIconSource next) {
                if (_iconSource == next) return;
                setState(() => _iconSource = next);
                _emitChanged();
              },
            ),
            const SizedBox(height: 32),
            Row(
              children: [
                if (widget.showResetAction)
                  TextButton(
                    onPressed: () {
                      LiveBridgeHaptics.warning();
                      setState(() {
                        _compactTextSource =
                            widget.resetValue.compactTextSource;
                        _iconSource = widget.resetValue.iconSource;
                      });
                      _emitChanged();
                    },
                    child: Text(s.resetToDefault),
                  ),
                const Spacer(),
                FilledButton.icon(
                  onPressed: () {
                    LiveBridgeHaptics.confirm();
                    Navigator.of(context).maybePop();
                  },
                  icon: const Icon(Icons.check_rounded),
                  label: Text(s.save),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
