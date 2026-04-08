import 'package:flutter/material.dart';

import '../l10n/app_strings.dart';
import '../models/network_speed_models.dart';
import '../utils/livebridge_haptics.dart';

class NetworkSpeedSection extends StatelessWidget {
  const NetworkSpeedSection({
    super.key,
    required this.settings,
    required this.masterEnabled,
    required this.onChanged,
  });

  final NetworkSpeedSettings settings;
  final bool masterEnabled;
  final ValueChanged<NetworkSpeedSettings> onChanged;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    final ColorScheme colorScheme = theme.colorScheme;
    final AppStrings s = AppStrings.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          s.networkSpeedSubtitle,
          style: theme.textTheme.bodySmall?.copyWith(
            color: colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 16),
        SwitchListTile.adaptive(
          value: settings.enabled,
          onChanged: (bool value) {
            LiveBridgeHaptics.toggle(value);
            onChanged(settings.copyWith(enabled: value));
          },
          title: Text(
            s.networkSpeedEnabledTitle,
            style: const TextStyle(fontWeight: FontWeight.w600),
          ),
          subtitle: Text(
            masterEnabled
                ? s.networkSpeedEnabledSubtitle
                : s.networkSpeedEnabledMasterOffSubtitle,
            style: TextStyle(color: colorScheme.onSurfaceVariant, fontSize: 13),
          ),
          contentPadding: EdgeInsets.zero,
          activeThumbColor: colorScheme.primary,
        ),
        const Padding(
          padding: EdgeInsets.symmetric(vertical: 16),
          child: Divider(height: 1),
        ),
        AnimatedOpacity(
          duration: const Duration(milliseconds: 200),
          opacity: settings.enabled ? 1 : 0.4,
          child: IgnorePointer(
            ignoring: !settings.enabled,
            child: Column(
              children: <Widget>[
                _NetworkSpeedDisplayModeDropdown(
                  label: s.networkSpeedDisplayContentTitle,
                  value: settings.displayMode,
                  optionLabelBuilder: (NetworkSpeedDisplayMode mode) {
                    switch (mode) {
                      case NetworkSpeedDisplayMode.total:
                        return s.networkSpeedDisplayModeTotal;
                      case NetworkSpeedDisplayMode.uploadOnly:
                        return s.networkSpeedDisplayModeUploadOnly;
                      case NetworkSpeedDisplayMode.downloadOnly:
                        return s.networkSpeedDisplayModeDownloadOnly;
                    }
                  },
                  onChanged: (NetworkSpeedDisplayMode next) {
                    if (next == settings.displayMode) {
                      return;
                    }
                    LiveBridgeHaptics.selection();
                    onChanged(settings.copyWith(displayMode: next));
                  },
                ),
                const SizedBox(height: 12),
                _NetworkSpeedActionTile(
                  title: s.networkSpeedUploadPrefixLabel,
                  value: s.networkSpeedCurrentValue(
                    settings.uploadPrefix.replaceAll('\n', r'\n'),
                  ),
                  icon: Icons.north_rounded,
                  onTap: () => _openPrefixSheet(
                    context,
                    title: s.networkSpeedUploadPrefixLabel,
                    currentValue: settings.uploadPrefix,
                    defaultValue: NetworkSpeedSettings.defaultUploadPrefix,
                    hintText: s.networkSpeedPrefixEditorHint,
                    onSaved: (String value) {
                      onChanged(settings.copyWith(uploadPrefix: value));
                    },
                  ),
                ),
                const SizedBox(height: 12),
                _NetworkSpeedActionTile(
                  title: s.networkSpeedDownloadPrefixLabel,
                  value: s.networkSpeedCurrentValue(
                    settings.downloadPrefix.replaceAll('\n', r'\n'),
                  ),
                  icon: Icons.south_rounded,
                  onTap: () => _openPrefixSheet(
                    context,
                    title: s.networkSpeedDownloadPrefixLabel,
                    currentValue: settings.downloadPrefix,
                    defaultValue: NetworkSpeedSettings.defaultDownloadPrefix,
                    hintText: s.networkSpeedPrefixEditorHint,
                    onSaved: (String value) {
                      onChanged(settings.copyWith(downloadPrefix: value));
                    },
                  ),
                ),
                const SizedBox(height: 12),
                _NetworkSpeedUnitDropdown(
                  label: s.networkSpeedUnitTitle,
                  values: settings.units,
                  optionLabelBuilder: (NetworkSpeedUnit unit) {
                    switch (unit) {
                      case NetworkSpeedUnit.auto:
                        return s.networkSpeedUnitAuto;
                      case NetworkSpeedUnit.bytes:
                        return 'B/s';
                      case NetworkSpeedUnit.kilobytes:
                        return 'KB/s';
                      case NetworkSpeedUnit.megabytes:
                        return 'MB/s';
                      case NetworkSpeedUnit.gigabytes:
                        return 'GB/s';
                    }
                  },
                  onChanged: (Set<NetworkSpeedUnit> values) {
                    if (NetworkSpeedUnitSelection.equals(
                      values,
                      settings.units,
                    )) {
                      return;
                    }
                    onChanged(settings.copyWith(units: values));
                  },
                ),
                const SizedBox(height: 12),
                SwitchListTile.adaptive(
                  value: settings.prioritizeUploadSpeed,
                  onChanged: (bool value) {
                    LiveBridgeHaptics.toggle(value);
                    onChanged(settings.copyWith(prioritizeUploadSpeed: value));
                  },
                  title: Text(
                    s.networkSpeedPrioritizeUploadTitle,
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                  subtitle: Text(
                    s.networkSpeedPrioritizeUploadSubtitle,
                    style: TextStyle(
                      color: colorScheme.onSurfaceVariant,
                      fontSize: 13,
                    ),
                  ),
                  contentPadding: EdgeInsets.zero,
                  activeThumbColor: colorScheme.primary,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _openPrefixSheet(
    BuildContext context, {
    required String title,
    required String currentValue,
    required String defaultValue,
    required String hintText,
    required ValueChanged<String> onSaved,
  }) async {
    final String? selected = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (BuildContext context) => _NetworkSpeedPrefixSheet(
        title: title,
        initialValue: currentValue,
        defaultValue: defaultValue,
        hintText: hintText,
        resetLabel: AppStrings.of(context).resetToDefault,
        saveLabel: AppStrings.of(context).save,
      ),
    );

    if (selected == null || selected == currentValue) {
      return;
    }

    onSaved(selected);
  }
}

class _NetworkSpeedDisplayModeDropdown extends StatelessWidget {
  const _NetworkSpeedDisplayModeDropdown({
    required this.label,
    required this.value,
    required this.optionLabelBuilder,
    required this.onChanged,
  });

  final String label;
  final NetworkSpeedDisplayMode value;
  final String Function(NetworkSpeedDisplayMode mode) optionLabelBuilder;
  final ValueChanged<NetworkSpeedDisplayMode> onChanged;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final bool isLight = colorScheme.brightness == Brightness.light;
    final Color fieldColor = isLight
        ? Colors.white
        : colorScheme.surfaceContainerLow;
    final Color menuColor = isLight
        ? Colors.white
        : colorScheme.surfaceContainer;
    final Color borderColor = colorScheme.primary.withValues(
      alpha: isLight ? 0.5 : 0.65,
    );

    return DropdownButtonFormField<NetworkSpeedDisplayMode>(
      initialValue: value,
      onTap: LiveBridgeHaptics.openSurface,
      onChanged: (NetworkSpeedDisplayMode? next) {
        if (next == null) {
          return;
        }
        onChanged(next);
      },
      isExpanded: true,
      icon: Icon(
        Icons.keyboard_arrow_down_rounded,
        color: colorScheme.onSurfaceVariant,
      ),
      dropdownColor: menuColor,
      borderRadius: BorderRadius.circular(24),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w500,
          color: colorScheme.onSurfaceVariant,
        ),
        filled: true,
        fillColor: fieldColor,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: borderColor, width: 1.2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: borderColor, width: 1.2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: colorScheme.primary, width: 1.8),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 20,
          vertical: 16,
        ),
      ),
      items: NetworkSpeedDisplayMode.values.map((NetworkSpeedDisplayMode mode) {
        return DropdownMenuItem<NetworkSpeedDisplayMode>(
          value: mode,
          child: Text(
            optionLabelBuilder(mode),
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          ),
        );
      }).toList(),
    );
  }
}

class _NetworkSpeedActionTile extends StatelessWidget {
  const _NetworkSpeedActionTile({
    required this.title,
    required this.value,
    required this.icon,
    required this.onTap,
  });

  final String title;
  final String value;
  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () {
          LiveBridgeHaptics.openSurface();
          onTap();
        },
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          decoration: BoxDecoration(
            color: colorScheme.primaryContainer.withValues(alpha: 0.38),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Row(
            children: <Widget>[
              Container(
                width: 34,
                height: 34,
                decoration: BoxDecoration(
                  color: colorScheme.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, size: 18, color: colorScheme.primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      title,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 15,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      value,
                      style: TextStyle(
                        color: colorScheme.onSurfaceVariant,
                        fontSize: 13,
                        height: 1.2,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Icon(
                Icons.keyboard_arrow_right_rounded,
                color: colorScheme.onSurfaceVariant,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NetworkSpeedUnitDropdown extends StatefulWidget {
  const _NetworkSpeedUnitDropdown({
    required this.label,
    required this.values,
    required this.optionLabelBuilder,
    required this.onChanged,
  });

  final String label;
  final Set<NetworkSpeedUnit> values;
  final String Function(NetworkSpeedUnit unit) optionLabelBuilder;
  final ValueChanged<Set<NetworkSpeedUnit>> onChanged;

  @override
  State<_NetworkSpeedUnitDropdown> createState() =>
      _NetworkSpeedUnitDropdownState();
}

class _NetworkSpeedUnitDropdownState extends State<_NetworkSpeedUnitDropdown> {
  final MenuController _menuController = MenuController();
  late Set<NetworkSpeedUnit> _draftValues;
  late Set<NetworkSpeedUnit> _committedValues;
  bool _menuOpen = false;

  @override
  void initState() {
    super.initState();
    _draftValues = _normalize(widget.values);
    _committedValues = _normalize(widget.values);
  }

  @override
  void didUpdateWidget(covariant _NetworkSpeedUnitDropdown oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (_menuOpen) {
      return;
    }

    final Set<NetworkSpeedUnit> normalized = _normalize(widget.values);
    if (!_sameSelection(normalized, _draftValues)) {
      _draftValues = normalized;
      _committedValues = normalized;
    }
  }

  Set<NetworkSpeedUnit> _normalize(Set<NetworkSpeedUnit> values) {
    return Set<NetworkSpeedUnit>.from(
      NetworkSpeedUnitSelection.parse(
        values.map((NetworkSpeedUnit unit) => unit.id).join(','),
      ),
    );
  }

  bool _sameSelection(Set<NetworkSpeedUnit> left, Set<NetworkSpeedUnit> right) {
    return NetworkSpeedUnitSelection.equals(left, right);
  }

  void _toggleUnit(NetworkSpeedUnit unit) {
    final bool checked = _draftValues.contains(unit);
    final Set<NetworkSpeedUnit> next;
    if (checked) {
      next = Set<NetworkSpeedUnit>.from(_draftValues)..remove(unit);
    } else if (unit == NetworkSpeedUnit.auto) {
      next = <NetworkSpeedUnit>{NetworkSpeedUnit.auto};
    } else {
      next = Set<NetworkSpeedUnit>.from(_draftValues)
        ..remove(NetworkSpeedUnit.auto)
        ..add(unit);
    }
    setState(() => _draftValues = _normalize(next));
  }

  void _commitSelection() {
    final Set<NetworkSpeedUnit> next = _normalize(_draftValues);
    if (_sameSelection(next, _committedValues)) {
      return;
    }
    _committedValues = next;
    widget.onChanged(next);
  }

  void _toggleMenu() {
    if (_menuController.isOpen) {
      _menuController.close();
      return;
    }
    LiveBridgeHaptics.openSurface();
    _menuController.open();
  }

  String _summaryLabel() {
    if (_draftValues.contains(NetworkSpeedUnit.auto)) {
      return widget.optionLabelBuilder(NetworkSpeedUnit.auto);
    }

    return kNetworkSpeedUnitValues
        .where(
          (NetworkSpeedUnit unit) =>
              unit != NetworkSpeedUnit.auto && _draftValues.contains(unit),
        )
        .map(widget.optionLabelBuilder)
        .join(', ');
  }

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final bool isLight = colorScheme.brightness == Brightness.light;
    final Color fieldColor = isLight
        ? Colors.white
        : colorScheme.surfaceContainerLow;
    final Color menuColor = isLight
        ? Colors.white
        : colorScheme.surfaceContainer;
    final Color borderColor = colorScheme.primary.withValues(
      alpha: isLight ? 0.5 : 0.65,
    );

    return MenuAnchor(
      controller: _menuController,
      consumeOutsideTap: true,
      crossAxisUnconstrained: false,
      style: MenuStyle(
        backgroundColor: WidgetStatePropertyAll<Color>(menuColor),
        shape: WidgetStatePropertyAll<OutlinedBorder>(
          RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        ),
        side: WidgetStatePropertyAll<BorderSide>(
          BorderSide(color: borderColor, width: 1.2),
        ),
        padding: const WidgetStatePropertyAll<EdgeInsets>(EdgeInsets.zero),
      ),
      onOpen: () => setState(() => _menuOpen = true),
      onClose: () {
        if (mounted) {
          setState(() => _menuOpen = false);
        }
        _commitSelection();
      },
      menuChildren: <Widget>[
        SizedBox(
          width: 320,
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                for (final NetworkSpeedUnit unit in kNetworkSpeedUnitValues)
                  _NetworkSpeedUnitMenuRow(
                    title: widget.optionLabelBuilder(unit),
                    checked: _draftValues.contains(unit),
                    onTap: () {
                      LiveBridgeHaptics.selection();
                      _toggleUnit(unit);
                    },
                  ),
                const SizedBox(height: 8),
                Align(
                  alignment: Alignment.centerRight,
                  child: FilledButton.tonalIcon(
                    onPressed: _menuController.close,
                    icon: const Icon(Icons.check_rounded, size: 18),
                    label: Text(
                      MaterialLocalizations.of(context).okButtonLabel,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
      builder:
          (BuildContext context, MenuController controller, Widget? child) {
            return Material(
              color: Colors.transparent,
              child: InkWell(
                borderRadius: BorderRadius.circular(20),
                onTap: _toggleMenu,
                child: InputDecorator(
                  isEmpty: false,
                  decoration: InputDecoration(
                    labelText: widget.label,
                    labelStyle: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                      color: colorScheme.onSurfaceVariant,
                    ),
                    filled: true,
                    fillColor: fieldColor,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: BorderSide(color: borderColor, width: 1.2),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: BorderSide(color: borderColor, width: 1.2),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: BorderSide(
                        color: colorScheme.primary,
                        width: 1.8,
                      ),
                    ),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 16,
                    ),
                    suffixIcon: Icon(
                      _menuOpen
                          ? Icons.keyboard_arrow_up_rounded
                          : Icons.keyboard_arrow_down_rounded,
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ),
                  child: Text(
                    _summaryLabel(),
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ),
            );
          },
    );
  }
}

class _NetworkSpeedUnitMenuRow extends StatelessWidget {
  const _NetworkSpeedUnitMenuRow({
    required this.title,
    required this.checked,
    required this.onTap,
  });

  final String title;
  final bool checked;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
          child: Row(
            children: <Widget>[
              Checkbox(
                value: checked,
                onChanged: (_) => onTap(),
                visualDensity: VisualDensity.compact,
                materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  title,
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: checked ? FontWeight.w700 : FontWeight.w500,
                    color: checked
                        ? colorScheme.onSurface
                        : colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _NetworkSpeedPrefixSheet extends StatefulWidget {
  const _NetworkSpeedPrefixSheet({
    required this.title,
    required this.initialValue,
    required this.defaultValue,
    required this.hintText,
    required this.resetLabel,
    required this.saveLabel,
  });

  final String title;
  final String initialValue;
  final String defaultValue;
  final String hintText;
  final String resetLabel;
  final String saveLabel;

  @override
  State<_NetworkSpeedPrefixSheet> createState() =>
      _NetworkSpeedPrefixSheetState();
}

class _NetworkSpeedPrefixSheetState extends State<_NetworkSpeedPrefixSheet> {
  late final TextEditingController _controller = TextEditingController(
    text: widget.initialValue,
  );

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
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
          children: <Widget>[
            Text(
              widget.title,
              style: Theme.of(
                context,
              ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 20),
            TextField(
              controller: _controller,
              autofocus: true,
              decoration: InputDecoration(
                hintText: widget.hintText,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(18),
                ),
              ),
            ),
            const SizedBox(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                OutlinedButton.icon(
                  onPressed: () {
                    LiveBridgeHaptics.warning();
                    Navigator.of(context).pop(widget.defaultValue);
                  },
                  icon: const Icon(Icons.refresh_rounded),
                  label: Text(widget.resetLabel),
                ),
                const SizedBox(width: 12),
                FilledButton.icon(
                  onPressed: () {
                    LiveBridgeHaptics.confirm();
                    Navigator.of(context).pop(_controller.text);
                  },
                  icon: const Icon(Icons.check_rounded),
                  label: Text(widget.saveLabel),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
