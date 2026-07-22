import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';
import 'lb_list_component.dart';

class LbIncrementalListComponent extends StatefulWidget {
  const LbIncrementalListComponent({
    super.key,
    required this.items,
    this.backgroundColor,
    this.rowHeight = LbSpacing.listRowHeight,
    this.leadingSize = LbSpacing.listLeadingSize,
    this.leadingIconSize = LbSpacing.listLeadingIconSize,
    this.leadingGap = LbSpacing.listLeadingGap,
    this.extendDividersToEnd = false,
    this.pageSize = 32,
  });

  final List<LbListItemData> items;
  final Color? backgroundColor;
  final double rowHeight;
  final double leadingSize;
  final double leadingIconSize;
  final double leadingGap;
  final bool extendDividersToEnd;
  final int pageSize;

  @override
  State<LbIncrementalListComponent> createState() =>
      _LbIncrementalListComponentState();
}

class _LbIncrementalListComponentState
    extends State<LbIncrementalListComponent> {
  ScrollController? _scrollController;
  late int _visibleItemCount = _initialVisibleItemCount;

  int get _initialVisibleItemCount =>
      widget.items.length.clamp(0, widget.pageSize);

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final ScrollController? nextController = PrimaryScrollController.maybeOf(
      context,
    );
    if (identical(nextController, _scrollController)) {
      return;
    }
    _scrollController?.removeListener(_handleScroll);
    _scrollController = nextController;
    _scrollController?.addListener(_handleScroll);
  }

  @override
  void didUpdateWidget(covariant LbIncrementalListComponent oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.items.length < _visibleItemCount) {
      _visibleItemCount = widget.items.length;
    }
  }

  @override
  void dispose() {
    _scrollController?.removeListener(_handleScroll);
    super.dispose();
  }

  void _handleScroll() {
    final ScrollController? controller = _scrollController;
    if (controller == null ||
        !controller.hasClients ||
        _visibleItemCount >= widget.items.length ||
        controller.position.extentAfter > widget.rowHeight * 6) {
      return;
    }
    setState(() {
      _visibleItemCount = (_visibleItemCount + widget.pageSize).clamp(
        0,
        widget.items.length,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return LbListComponent(
      items: widget.items.take(_visibleItemCount).toList(growable: false),
      backgroundColor: widget.backgroundColor,
      rowHeight: widget.rowHeight,
      leadingSize: widget.leadingSize,
      leadingIconSize: widget.leadingIconSize,
      leadingGap: widget.leadingGap,
      extendDividersToEnd: widget.extendDividersToEnd,
    );
  }
}
