// Shared motion for the whole app: dialog entrance, tab / page switches and
// the staggered fade-up of list items. Every screen uses these so the app
// moves the same way everywhere.

import 'dart:async';

import 'package:flutter/material.dart';

import '../theme/app_colors.dart';

/// Durations and curves used across the app.
class AppMotion {
  AppMotion._();

  static const fast = Duration(milliseconds: 180);
  static const medium = Duration(milliseconds: 320);
  static const slow = Duration(milliseconds: 520);

  static const Curve emphasized = Curves.easeOutCubic;
  static const Curve spring = Cubic(0.34, 1.36, 0.64, 1);
}

/// Drop-in replacement for [showDialog]: the dialog scales up from 92% while
/// fading in over a blurred-dark barrier, and settles with a slight spring.
Future<T?> showAppDialog<T>({
  required BuildContext context,
  required WidgetBuilder builder,
  bool barrierDismissible = true,
}) {
  return showGeneralDialog<T>(
    context: context,
    barrierDismissible: barrierDismissible,
    barrierLabel: MaterialLocalizations.of(context).modalBarrierDismissLabel,
    barrierColor: const Color(0x8A0F172A),
    transitionDuration: AppMotion.medium,
    pageBuilder: (dialogContext, _, _) => SafeArea(child: Builder(builder: builder)),
    transitionBuilder: (_, animation, _, child) {
      final curved = CurvedAnimation(parent: animation, curve: AppMotion.spring, reverseCurve: Curves.easeInCubic);
      final fade = CurvedAnimation(parent: animation, curve: Curves.easeOut, reverseCurve: Curves.easeIn);
      return FadeTransition(
        opacity: fade,
        child: ScaleTransition(scale: Tween<double>(begin: 0.92, end: 1).animate(curved), child: child),
      );
    },
  );
}

/// Material "fade through" switch used for bottom-navigation tabs and
/// sub-tabs: the old page fades out, the new one fades in while rising and
/// growing slightly into place.
class FadeThroughSwitcher extends StatelessWidget {
  const FadeThroughSwitcher({super.key, required this.child, this.duration = AppMotion.medium});

  /// Give the child a distinct key per page so the switch animates.
  final Widget child;
  final Duration duration;

  @override
  Widget build(BuildContext context) {
    return AnimatedSwitcher(
      duration: duration,
      reverseDuration: AppMotion.fast,
      switchInCurve: AppMotion.emphasized,
      switchOutCurve: Curves.easeIn,
      layoutBuilder: (current, previous) => Stack(fit: StackFit.expand, children: [...previous, ?current]),
      transitionBuilder: (child, animation) {
        return FadeTransition(
          opacity: animation,
          child: ScaleTransition(
            scale: Tween<double>(begin: 0.97, end: 1).animate(animation),
            child: SlideTransition(
              position: Tween<Offset>(begin: const Offset(0, 0.015), end: Offset.zero).animate(animation),
              child: child,
            ),
          ),
        );
      },
      child: child,
    );
  }
}

/// Horizontal shared-axis switch for step-by-step flows (wizards): moving
/// forward slides the new step in from the right, moving back from the left.
class SharedAxisSwitcher extends StatelessWidget {
  const SharedAxisSwitcher({super.key, required this.child, this.forward = true});

  final Widget child;
  final bool forward;

  @override
  Widget build(BuildContext context) {
    return AnimatedSwitcher(
      duration: AppMotion.medium,
      switchInCurve: AppMotion.emphasized,
      switchOutCurve: Curves.easeIn,
      layoutBuilder: (current, previous) => Stack(alignment: Alignment.topCenter, children: [...previous, ?current]),
      transitionBuilder: (child, animation) {
        final incoming = child.key == this.child.key;
        final dx = (incoming ? 0.12 : -0.12) * (forward ? 1 : -1);
        return FadeTransition(
          opacity: animation,
          child: SlideTransition(
            position: Tween<Offset>(begin: Offset(dx, 0), end: Offset.zero).animate(animation),
            child: child,
          ),
        );
      },
      child: child,
    );
  }
}

/// Fades and lifts a list item into place, staggered by its [index]. Only
/// the first screenful animates, and only while the list is still at the top,
/// so scrolling back up never replays it.
class AppEntrance extends StatefulWidget {
  const AppEntrance({super.key, required this.index, required this.child});

  final int index;
  final Widget child;

  static const _maxAnimated = 10;

  @override
  State<AppEntrance> createState() => _AppEntranceState();
}

class _AppEntranceState extends State<AppEntrance> with SingleTickerProviderStateMixin {
  AnimationController? _controller;
  Timer? _delay;

  @override
  void initState() {
    super.initState();
    if (widget.index >= AppEntrance._maxAnimated) return;
    final controller = AnimationController(vsync: this, duration: AppMotion.slow);
    _controller = controller;
    _delay = Timer(Duration(milliseconds: 40 * widget.index), () {
      if (mounted) controller.forward();
    });
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Item rebuilt after the user has scrolled: show it as-is.
    final position = Scrollable.maybeOf(context)?.position;
    if (_controller != null &&
        !_controller!.isAnimating &&
        position != null &&
        position.hasPixels &&
        position.pixels > 0) {
      _delay?.cancel();
      _controller!.value = 1;
    }
  }

  @override
  void dispose() {
    _delay?.cancel();
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final controller = _controller;
    if (controller == null) return widget.child;
    final curved = CurvedAnimation(parent: controller, curve: AppMotion.emphasized);
    return FadeTransition(
      opacity: curved,
      child: SlideTransition(
        position: Tween<Offset>(begin: const Offset(0, 0.08), end: Offset.zero).animate(curved),
        child: widget.child,
      ),
    );
  }
}

/// Wraps a list item builder so each item enters with [AppEntrance].
IndexedWidgetBuilder appEntrance(IndexedWidgetBuilder builder) =>
    (context, index) => AppEntrance(index: index, child: builder(context, index));

/// Shrinks slightly while pressed - used on tappable tiles and quick actions.
class PressableScale extends StatefulWidget {
  const PressableScale({super.key, required this.child, this.onTap, this.scale = 0.95});

  final Widget child;
  final VoidCallback? onTap;
  final double scale;

  @override
  State<PressableScale> createState() => _PressableScaleState();
}

class _PressableScaleState extends State<PressableScale> {
  bool _pressed = false;

  void _set(bool v) {
    if (_pressed != v) setState(() => _pressed = v);
  }

  @override
  Widget build(BuildContext context) {
    return Listener(
      onPointerDown: widget.onTap == null ? null : (_) => _set(true),
      onPointerUp: (_) => _set(false),
      onPointerCancel: (_) => _set(false),
      child: AnimatedScale(
        scale: _pressed ? widget.scale : 1,
        duration: AppMotion.fast,
        curve: Curves.easeOut,
        child: widget.child,
      ),
    );
  }
}

/// Counts a rupee amount up from zero the first time it is shown, then
/// animates between values when the data changes.
class AnimatedAmount extends StatelessWidget {
  const AnimatedAmount({super.key, required this.amount, required this.format, this.style, this.maxLines});

  final double amount;
  final String Function(double) format;
  final TextStyle? style;
  final int? maxLines;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween<double>(begin: 0, end: amount),
      duration: const Duration(milliseconds: 900),
      curve: Curves.easeOutCubic,
      builder: (_, value, _) => Text(
        format(value),
        style: style,
        maxLines: maxLines,
        overflow: maxLines == null ? null : TextOverflow.ellipsis,
      ),
    );
  }
}

/// A rounded progress bar that fills smoothly to [value].
class AnimatedProgressBar extends StatelessWidget {
  const AnimatedProgressBar({
    super.key,
    required this.value,
    required this.color,
    this.background = AppColors.surfaceVariant,
    this.height = 8,
  });

  final double value;
  final Color color;
  final Color background;
  final double height;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween<double>(begin: 0, end: value.clamp(0.0, 1.0)),
      duration: const Duration(milliseconds: 1000),
      curve: Curves.easeOutCubic,
      builder: (_, v, _) => ClipRRect(
        borderRadius: BorderRadius.circular(height),
        child: LinearProgressIndicator(value: v, minHeight: height, color: color, backgroundColor: background),
      ),
    );
  }
}
