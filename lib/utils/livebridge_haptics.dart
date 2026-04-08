import 'dart:async';

import 'package:flutter/services.dart';

final class LiveBridgeHaptics {
  const LiveBridgeHaptics._();

  static Future<void> selection() => HapticFeedback.selectionClick();

  static Future<void> toggle(bool enabled) {
    return enabled
        ? HapticFeedback.lightImpact()
        : HapticFeedback.selectionClick();
  }

  static Future<void> openSurface() => HapticFeedback.lightImpact();

  static Future<void> confirm() => HapticFeedback.mediumImpact();

  static Future<void> expand(bool opening) {
    return opening ? HapticFeedback.lightImpact() : selection();
  }

  static Future<void> warning() => HapticFeedback.heavyImpact();

  static Future<void> blockedPulse() async {
    await HapticFeedback.heavyImpact();
    await Future<void>.delayed(const Duration(milliseconds: 70));
    await HapticFeedback.selectionClick();
    await Future<void>.delayed(const Duration(milliseconds: 60));
    await HapticFeedback.selectionClick();
  }
}
