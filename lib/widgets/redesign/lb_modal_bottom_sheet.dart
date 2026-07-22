import 'dart:ui';

import 'package:flutter/material.dart';

import '../../theme/livebridge_tokens.dart';

Future<T?> showLbModalBottomSheet<T>({
  required BuildContext context,
  required WidgetBuilder builder,
}) {
  return showGeneralDialog<T>(
    context: context,
    barrierDismissible: true,
    barrierLabel: MaterialLocalizations.of(context).modalBarrierDismissLabel,
    barrierColor: Colors.transparent,
    transitionDuration: const Duration(milliseconds: 220),
    pageBuilder:
        (
          BuildContext dialogContext,
          Animation<double> animation,
          Animation<double> secondaryAnimation,
        ) => builder(dialogContext),
    transitionBuilder:
        (
          BuildContext dialogContext,
          Animation<double> animation,
          Animation<double> secondaryAnimation,
          Widget child,
        ) {
          return _LbModalBottomSheetScaffold(
            routeAnimation: animation,
            child: child,
          );
        },
  );
}

class LbModalBottomSheetSurface extends StatelessWidget {
  const LbModalBottomSheetSurface({
    super.key,
    required this.child,
    this.onHandleDragStart,
    this.onHandleDragUpdate,
    this.onHandleDragEnd,
  });

  final Widget child;
  final GestureDragStartCallback? onHandleDragStart;
  final GestureDragUpdateCallback? onHandleDragUpdate;
  final GestureDragEndCallback? onHandleDragEnd;

  @override
  Widget build(BuildContext context) {
    final LbPalette palette = LbPalette.of(context);
    final double bottomInset = MediaQuery.paddingOf(context).bottom;

    return Material(
      color: Colors.transparent,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * 0.88,
        ),
        child: Container(
          width: double.infinity,
          padding: EdgeInsets.fromLTRB(
            LbSpacing.screenHorizontal,
            LbSpacing.modalSheetTopPadding,
            LbSpacing.screenHorizontal,
            LbSpacing.md + bottomInset,
          ),
          decoration: BoxDecoration(
            color: palette.surfaceRaised,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(25)),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              GestureDetector(
                onVerticalDragStart: onHandleDragStart,
                onVerticalDragUpdate: onHandleDragUpdate,
                onVerticalDragEnd: onHandleDragEnd,
                behavior: HitTestBehavior.opaque,
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: LbSpacing.xl,
                    vertical: LbSpacing.xs,
                  ),
                  child: Container(
                    width: LbSpacing.modalSheetHandleWidth,
                    height: LbSpacing.modalSheetHandleHeight,
                    decoration: BoxDecoration(
                      color: palette.textMuted.withValues(alpha: 0.55),
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: LbSpacing.modalSheetContentTopGap),
              Flexible(
                child: SingleChildScrollView(
                  physics: const BouncingScrollPhysics(),
                  child: child,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _LbModalBottomSheetScaffold extends StatefulWidget {
  const _LbModalBottomSheetScaffold({
    required this.routeAnimation,
    required this.child,
  });

  final Animation<double> routeAnimation;
  final Widget child;

  @override
  State<_LbModalBottomSheetScaffold> createState() =>
      _LbModalBottomSheetScaffoldState();
}

class _LbModalBottomSheetScaffoldState
    extends State<_LbModalBottomSheetScaffold>
    with SingleTickerProviderStateMixin {
  late final AnimationController _settleController;
  Animation<double>? _settleAnimation;
  double _dragOffset = 0;
  double _viewportHeight = 1;

  @override
  void initState() {
    super.initState();
    _settleController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 220),
    );
  }

  @override
  void dispose() {
    _settleController.dispose();
    super.dispose();
  }

  double get _dismissProgress {
    final double threshold = (_viewportHeight * 0.72).clamp(1, double.infinity);
    return (_dragOffset / threshold).clamp(0.0, 1.0);
  }

  void _stopSettleAnimation() {
    _settleAnimation?.removeListener(_handleSettleTick);
    _settleAnimation = null;
    _settleController.stop();
  }

  void _handleSettleTick() {
    if (!mounted || _settleAnimation == null) {
      return;
    }
    setState(() => _dragOffset = _settleAnimation!.value);
  }

  void _animateDragOffsetTo(double target) {
    _stopSettleAnimation();
    _settleAnimation = Tween<double>(begin: _dragOffset, end: target).animate(
      CurvedAnimation(parent: _settleController, curve: Curves.easeOutCubic),
    )..addListener(_handleSettleTick);
    _settleController.forward(from: 0);
  }

  void _handleDragStart(DragStartDetails details) {
    _stopSettleAnimation();
  }

  void _handleDragUpdate(DragUpdateDetails details) {
    final double nextOffset = (_dragOffset + details.delta.dy).clamp(
      0.0,
      _viewportHeight,
    );
    if (nextOffset == _dragOffset) {
      return;
    }
    setState(() => _dragOffset = nextOffset);
  }

  void _handleDragEnd(DragEndDetails details) {
    final double dismissDistance =
        _viewportHeight * LbMotion.modalSheetDismissThreshold;
    final bool shouldDismiss =
        _dragOffset >= dismissDistance ||
        details.primaryVelocity != null &&
            details.primaryVelocity! >= LbMotion.modalSheetDismissVelocity;

    if (shouldDismiss) {
      Navigator.of(context).maybePop();
      return;
    }
    _animateDragOffsetTo(0);
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        _viewportHeight = constraints.maxHeight;
        return AnimatedBuilder(
          animation: Listenable.merge(<Listenable>[
            widget.routeAnimation,
            _settleController,
          ]),
          builder: (BuildContext context, _) {
            final CurvedAnimation sheetCurve = CurvedAnimation(
              parent: widget.routeAnimation,
              curve: Curves.easeOutCubic,
              reverseCurve: Curves.easeInCubic,
            );
            final CurvedAnimation backdropCurve = CurvedAnimation(
              parent: widget.routeAnimation,
              curve: Curves.easeOut,
              reverseCurve: Curves.easeIn,
            );
            final double backdropProgress =
                (backdropCurve.value * (1 - _dismissProgress)).clamp(0.0, 1.0);
            return Material(
              color: Colors.transparent,
              child: Stack(
                children: <Widget>[
                  Positioned.fill(
                    child: GestureDetector(
                      onTap: () => Navigator.of(context).maybePop(),
                      behavior: HitTestBehavior.opaque,
                      child: BackdropFilter(
                        filter: ImageFilter.blur(
                          sigmaX:
                              LbEffects.modalSheetBackdropBlur *
                              backdropProgress,
                          sigmaY:
                              LbEffects.modalSheetBackdropBlur *
                              backdropProgress,
                        ),
                        child: ColoredBox(
                          color: Colors.black.withValues(
                            alpha:
                                LbEffects.modalSheetBackdropTint *
                                backdropProgress,
                          ),
                        ),
                      ),
                    ),
                  ),
                  Align(
                    alignment: Alignment.bottomCenter,
                    child: Transform.translate(
                      offset: Offset(0, _dragOffset),
                      child: SlideTransition(
                        position: Tween<Offset>(
                          begin: const Offset(0, 1),
                          end: Offset.zero,
                        ).animate(sheetCurve),
                        child: LbModalBottomSheetSurface(
                          onHandleDragStart: _handleDragStart,
                          onHandleDragUpdate: _handleDragUpdate,
                          onHandleDragEnd: _handleDragEnd,
                          child: widget.child,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }
}
