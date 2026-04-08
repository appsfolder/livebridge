import 'package:flutter/material.dart';
import '../models/app_models.dart';
import '../utils/livebridge_haptics.dart';

class InstalledAppAvatar extends StatelessWidget {
  const InstalledAppAvatar({super.key, required this.app});
  final InstalledApp app;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final Color fallbackIconBackground = colorScheme.primary.withValues(
      alpha: colorScheme.brightness == Brightness.dark ? 0.22 : 0.12,
    );
    if (app.icon != null) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: Image.memory(
          app.icon!,
          width: 44,
          height: 44,
          fit: BoxFit.cover,
        ),
      );
    }
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        color: fallbackIconBackground,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Center(
        child: Text(
          app.label.isNotEmpty ? app.label[0].toUpperCase() : '?',
          style: TextStyle(
            color: colorScheme.primary,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
      ),
    );
  }
}

class PackagePickerSheet extends StatefulWidget {
  const PackagePickerSheet({
    super.key,
    required this.title,
    required this.apps,
    required this.initialSelected,
    required this.applyLabel,
    required this.searchHint,
    required this.showSystemAppsLabel,
    required this.hideSystemAppsLabel,
    this.showSystemAppsInitially = false,
  });

  final String title;
  final List<InstalledApp> apps;
  final Set<String> initialSelected;
  final String applyLabel;
  final String searchHint;
  final String showSystemAppsLabel;
  final String hideSystemAppsLabel;
  final bool showSystemAppsInitially;

  @override
  State<PackagePickerSheet> createState() => _PackagePickerSheetState();
}

class _PackagePickerSheetState extends State<PackagePickerSheet> {
  late final Set<String> _selected = Set<String>.from(widget.initialSelected);
  late bool _showSystemApps = widget.showSystemAppsInitially;
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final Color popupMenuColor = colorScheme.brightness == Brightness.light
        ? Colors.white
        : colorScheme.surfaceContainer;
    final String q = _query.toLowerCase();
    final List<InstalledApp> selectedApps = <InstalledApp>[];
    final List<InstalledApp> regularApps = <InstalledApp>[];
    for (final app in widget.apps) {
      final bool selected = _selected.contains(app.packageName);
      if (!_showSystemApps && app.isSystem && !selected) {
        continue;
      }
      if (_query.isNotEmpty &&
          !app.label.toLowerCase().contains(q) &&
          !app.packageName.toLowerCase().contains(q)) {
        continue;
      }
      if (selected) {
        selectedApps.add(app);
      } else {
        regularApps.add(app);
      }
    }
    final List<InstalledApp> filtered = <InstalledApp>[
      ...selectedApps,
      ...regularApps,
    ];

    return SafeArea(
      top: false,
      child: Padding(
        padding: EdgeInsets.only(
          left: 16,
          right: 16,
          top: 8,
          bottom: 16 + MediaQuery.of(context).viewInsets.bottom,
        ),
        child: SizedBox(
          height: MediaQuery.of(context).size.height * 0.8,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: Row(
                  children: <Widget>[
                    Expanded(
                      child: Text(
                        widget.title,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    PopupMenuButton<bool>(
                      icon: const Icon(Icons.more_vert_rounded),
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
                      tooltip: _showSystemApps
                          ? widget.hideSystemAppsLabel
                          : widget.showSystemAppsLabel,
                      onSelected: (bool next) {
                        LiveBridgeHaptics.toggle(next);
                        setState(() => _showSystemApps = next);
                      },
                      itemBuilder: (BuildContext context) =>
                          <PopupMenuEntry<bool>>[
                            PopupMenuItem<bool>(
                              value: !_showSystemApps,
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
                                          ? widget.hideSystemAppsLabel
                                          : widget.showSystemAppsLabel,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                onChanged: (val) => setState(() => _query = val.trim()),
                decoration: InputDecoration(
                  prefixIcon: const Icon(Icons.search_rounded),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(16),
                    borderSide: BorderSide.none,
                  ),
                  filled: true,
                  fillColor: colorScheme.surfaceContainerHighest.withValues(
                    alpha: 0.5,
                  ),
                  hintText: widget.searchHint,
                  contentPadding: const EdgeInsets.symmetric(vertical: 0),
                ),
              ),
              const SizedBox(height: 12),
              Expanded(
                child: ListView.builder(
                  physics: const BouncingScrollPhysics(),
                  itemCount: filtered.length,
                  itemBuilder: (context, index) {
                    final app = filtered[index];
                    final checked = _selected.contains(app.packageName);
                    return ListTile(
                      onTap: () {
                        LiveBridgeHaptics.toggle(!checked);
                        setState(() {
                          checked
                              ? _selected.remove(app.packageName)
                              : _selected.add(app.packageName);
                        });
                      },
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                      leading: InstalledAppAvatar(app: app),
                      title: Text(
                        app.label,
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                      subtitle: Text(
                        app.packageName,
                        style: TextStyle(
                          fontSize: 12,
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                      trailing: Checkbox(
                        value: checked,
                        fillColor: WidgetStateProperty.resolveWith<Color?>(
                          (states) => states.contains(WidgetState.selected)
                              ? colorScheme.primary
                              : null,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(4),
                        ),
                        onChanged: (val) {
                          LiveBridgeHaptics.toggle(val == true);
                          setState(() {
                            val == true
                                ? _selected.add(app.packageName)
                                : _selected.remove(app.packageName);
                          });
                        },
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  style: FilledButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  onPressed: () {
                    LiveBridgeHaptics.confirm();
                    Navigator.of(context).pop(_selected);
                  },
                  icon: const Icon(Icons.check_rounded),
                  label: Text(
                    widget.applyLabel,
                    style: const TextStyle(fontSize: 16),
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

class SelectorOption<T> {
  const SelectorOption({
    required this.value,
    required this.title,
    this.subtitle,
    this.icon,
  });

  final T value;
  final String title;
  final String? subtitle;
  final IconData? icon;
}

class LiveBridgeChoiceSelector<T> extends StatelessWidget {
  const LiveBridgeChoiceSelector({
    super.key,
    required this.value,
    required this.options,
    required this.onChanged,
  });

  final T value;
  final List<SelectorOption<T>> options;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        final bool horizontal =
            options.length == 2 && constraints.maxWidth >= 320;
        if (horizontal) {
          return Row(
            children: <Widget>[
              for (int index = 0; index < options.length; index++) ...<Widget>[
                Expanded(
                  child: _ChoiceCard<T>(
                    option: options[index],
                    selected: options[index].value == value,
                    onTap: () => onChanged(options[index].value),
                  ),
                ),
                if (index != options.length - 1) const SizedBox(width: 10),
              ],
            ],
          );
        }

        return Column(
          children: <Widget>[
            for (int index = 0; index < options.length; index++) ...<Widget>[
              _ChoiceCard<T>(
                option: options[index],
                selected: options[index].value == value,
                onTap: () => onChanged(options[index].value),
              ),
              if (index != options.length - 1) const SizedBox(height: 10),
            ],
          ],
        );
      },
    );
  }
}

class LiveBridgeMultiChoiceSelector<T> extends StatelessWidget {
  const LiveBridgeMultiChoiceSelector({
    super.key,
    required this.values,
    required this.options,
    required this.onToggle,
  });

  final Set<T> values;
  final List<SelectorOption<T>> options;
  final ValueChanged<T> onToggle;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        for (int index = 0; index < options.length; index++) ...<Widget>[
          _ChoiceCard<T>(
            option: options[index],
            selected: values.contains(options[index].value),
            onTap: () => onToggle(options[index].value),
          ),
          if (index != options.length - 1) const SizedBox(height: 10),
        ],
      ],
    );
  }
}

class LiveBridgeToggleSelector<T> extends StatelessWidget {
  const LiveBridgeToggleSelector({
    super.key,
    required this.value,
    required this.options,
    required this.onChanged,
  });

  final T value;
  final List<SelectorOption<T>> options;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.45),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: colorScheme.outlineVariant.withValues(alpha: 0.45),
        ),
      ),
      child: Row(
        children: <Widget>[
          for (final SelectorOption<T> option in options)
            Expanded(
              child: _ToggleSegment<T>(
                option: option,
                selected: option.value == value,
                onTap: () => onChanged(option.value),
              ),
            ),
        ],
      ),
    );
  }
}

class _ToggleSegment<T> extends StatelessWidget {
  const _ToggleSegment({
    required this.option,
    required this.selected,
    required this.onTap,
  });

  final SelectorOption<T> option;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final Color selectedColor = colorScheme.primary;
    final Color textColor = selected
        ? colorScheme.onPrimary
        : colorScheme.onSurface;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 170),
      curve: Curves.easeOutCubic,
      margin: const EdgeInsets.symmetric(horizontal: 2),
      decoration: BoxDecoration(
        color: selected ? selectedColor : Colors.transparent,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: () {
            LiveBridgeHaptics.selection();
            onTap();
          },
          child: SizedBox(
            height: 44,
            child: Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10),
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      if (option.icon != null) ...<Widget>[
                        Icon(option.icon, size: 16, color: textColor),
                        const SizedBox(width: 6),
                      ],
                      Text(
                        option.title,
                        maxLines: 1,
                        softWrap: false,
                        overflow: TextOverflow.fade,
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          color: textColor,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ChoiceCard<T> extends StatelessWidget {
  const _ChoiceCard({
    required this.option,
    required this.selected,
    required this.onTap,
  });

  final SelectorOption<T> option;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final ColorScheme colorScheme = Theme.of(context).colorScheme;
    final Color backgroundColor = selected
        ? colorScheme.primaryContainer.withValues(alpha: 0.65)
        : colorScheme.surfaceContainerHighest.withValues(alpha: 0.45);
    final Color borderColor = selected
        ? colorScheme.primary.withValues(alpha: 0.55)
        : colorScheme.outlineVariant.withValues(alpha: 0.45);
    final Color titleColor = selected
        ? colorScheme.primary
        : colorScheme.onSurface.withValues(alpha: 0.9);

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () {
          LiveBridgeHaptics.selection();
          onTap();
        },
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          curve: Curves.easeOutCubic,
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          decoration: BoxDecoration(
            color: backgroundColor,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: borderColor, width: selected ? 1.4 : 1),
          ),
          child: Row(
            children: <Widget>[
              if (option.icon != null) ...<Widget>[
                Icon(option.icon, size: 18, color: titleColor),
                const SizedBox(width: 10),
              ],
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Text(
                      option.title,
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        color: titleColor,
                      ),
                    ),
                    if ((option.subtitle ?? '').isNotEmpty) ...<Widget>[
                      const SizedBox(height: 2),
                      Text(
                        option.subtitle!,
                        style: TextStyle(
                          fontSize: 12,
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              if (selected)
                Icon(
                  Icons.check_circle_rounded,
                  size: 18,
                  color: colorScheme.primary,
                ),
            ],
          ),
        ),
      ),
    );
  }
}
