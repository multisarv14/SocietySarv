// Loading feedback used across the app: buttons that turn into a spinner
// while their action runs, shimmering skeletons while a page's data arrives,
// and the thin progress bar under the top bar.

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../state/society_view_model.dart';
import '../theme/app_colors.dart';
import 'app_motion.dart';

// ---------------------------------------------------------------- buttons

enum _Kind { filled, elevated }

/// A [FilledButton] / [ElevatedButton] that shows a spinner from the moment
/// it is pressed until the view model's operation it started has finished.
/// Taps are ignored meanwhile, so an action can never be sent twice.
class AppButton extends StatefulWidget {
  const AppButton.filled({super.key, required this.onPressed, this.style, required Widget this.child})
    : _kind = _Kind.filled,
      icon = null,
      label = null;

  const AppButton.filledIcon({
    super.key,
    required this.onPressed,
    this.style,
    required Widget this.icon,
    required Widget this.label,
  }) : _kind = _Kind.filled,
       child = null;

  const AppButton.elevated({super.key, required this.onPressed, this.style, required Widget this.child})
    : _kind = _Kind.elevated,
      icon = null,
      label = null;

  const AppButton.elevatedIcon({
    super.key,
    required this.onPressed,
    this.style,
    required Widget this.icon,
    required Widget this.label,
  }) : _kind = _Kind.elevated,
       child = null;

  final _Kind _kind;
  final VoidCallback? onPressed;
  final ButtonStyle? style;
  final Widget? child;
  final Widget? icon;
  final Widget? label;

  @override
  State<AppButton> createState() => _AppButtonState();
}

class _AppButtonState extends State<AppButton> {
  /// This button started the operation currently running.
  bool _armed = false;

  void _press() {
    final onPressed = widget.onPressed;
    if (onPressed == null) return;
    setState(() => _armed = true);
    onPressed();
    // Nothing asynchronous was started (it only opened a dialog, say): let go.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final busy = context.read<SocietyViewModel?>()?.isBusy ?? false;
      if (!busy && _armed) setState(() => _armed = false);
    });
  }

  @override
  Widget build(BuildContext context) {
    final busy = context.select<SocietyViewModel?, bool>((vm) => vm?.isBusy ?? false);
    if (!busy) _armed = false;
    final loading = _armed && busy;

    Widget content(Widget idle) => AnimatedSwitcher(
      duration: AppMotion.fast,
      transitionBuilder: (c, a) => FadeTransition(
        opacity: a,
        child: ScaleTransition(scale: a, child: c),
      ),
      child: loading
          ? const _ButtonSpinner(key: ValueKey('spinner'))
          : KeyedSubtree(key: const ValueKey('idle'), child: idle),
    );

    final onPressed = widget.onPressed == null ? null : (loading ? () {} : _press);
    final Widget button;
    if (widget.child != null) {
      final child = content(widget.child!);
      button = widget._kind == _Kind.filled
          ? FilledButton(onPressed: onPressed, style: widget.style, child: child)
          : ElevatedButton(onPressed: onPressed, style: widget.style, child: child);
    } else {
      final Widget icon = loading ? const SizedBox.shrink() : widget.icon!;
      final label = content(widget.label!);
      button = widget._kind == _Kind.filled
          ? FilledButton.icon(onPressed: onPressed, style: widget.style, icon: icon, label: label)
          : ElevatedButton.icon(onPressed: onPressed, style: widget.style, icon: icon, label: label);
    }
    return PressableScale(onTap: widget.onPressed, scale: 0.97, child: button);
  }
}

class _ButtonSpinner extends StatelessWidget {
  const _ButtonSpinner({super.key});

  @override
  Widget build(BuildContext context) {
    // The button's own foreground colour, whatever its style.
    final color = IconTheme.of(context).color ?? DefaultTextStyle.of(context).style.color ?? Colors.white;
    return SizedBox(
      width: 20,
      height: 20,
      child: CircularProgressIndicator(strokeWidth: 2.4, color: color, backgroundColor: color.withValues(alpha: 0.25)),
    );
  }
}

// --------------------------------------------------------- top progress bar

/// Thin bar under the top bar while any operation is in flight.
class BusyProgressBar extends StatelessWidget {
  const BusyProgressBar({super.key});

  @override
  Widget build(BuildContext context) {
    final busy = context.select<SocietyViewModel, bool>((vm) => vm.isBusy);
    return AnimatedSwitcher(
      duration: AppMotion.medium,
      child: busy
          ? const LinearProgressIndicator(
              key: ValueKey('busy'),
              minHeight: 3,
              color: AppColors.tealLight,
              backgroundColor: AppColors.tealContainer,
            )
          : const SizedBox(key: ValueKey('idle'), height: 3),
    );
  }
}

// ---------------------------------------------------------------- shimmer

/// Sweeps a soft highlight across its child, for skeleton placeholders.
class Shimmer extends StatefulWidget {
  const Shimmer({super.key, required this.child});

  final Widget child;

  @override
  State<Shimmer> createState() => _ShimmerState();
}

class _ShimmerState extends State<Shimmer> with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1400),
  )..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => _ShimmerScope(animation: _controller, child: widget.child);
}

class _ShimmerScope extends InheritedWidget {
  const _ShimmerScope({required this.animation, required super.child});

  final Animation<double> animation;

  @override
  bool updateShouldNotify(_ShimmerScope old) => old.animation != animation;
}

/// A grey rounded block standing in for text or an image while loading.
class SkeletonBox extends StatelessWidget {
  const SkeletonBox({super.key, this.width, required this.height, this.radius = 8});

  final double? width;
  final double height;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final animation = context.dependOnInheritedWidgetOfExactType<_ShimmerScope>()?.animation;
    final shape = BorderRadius.circular(radius);
    if (animation == null) {
      return Container(
        width: width,
        height: height,
        decoration: BoxDecoration(color: AppColors.borderLight, borderRadius: shape),
      );
    }
    return AnimatedBuilder(
      animation: animation,
      builder: (_, _) {
        final t = animation.value * 3 - 1; // -1 .. 2: the highlight sweeps across
        return Container(
          width: width,
          height: height,
          decoration: BoxDecoration(
            borderRadius: shape,
            gradient: LinearGradient(
              begin: Alignment(t - 2, 0),
              end: Alignment(t, 0),
              colors: const [AppColors.borderLight, Color(0xFFF1F5F9), AppColors.borderLight],
              stops: const [0.2, 0.5, 0.8],
            ),
          ),
        );
      },
    );
  }
}

/// Card-shaped skeleton: an icon, two lines of text and a trailing pill.
class SkeletonCard extends StatelessWidget {
  const SkeletonCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surfaceCard,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderLight),
      ),
      child: const Row(
        children: [
          SkeletonBox(width: 44, height: 44, radius: 14),
          SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SkeletonBox(width: 160, height: 14),
                SizedBox(height: 10),
                SkeletonBox(height: 10),
                SizedBox(height: 6),
                SkeletonBox(width: 110, height: 10),
              ],
            ),
          ),
          SizedBox(width: 14),
          SkeletonBox(width: 56, height: 22, radius: 12),
        ],
      ),
    );
  }
}

/// Whole-page skeleton shown while a society's data is first arriving.
class SkeletonPage extends StatelessWidget {
  const SkeletonPage({super.key});

  @override
  Widget build(BuildContext context) {
    Widget tile() => Expanded(
      child: Container(
        height: 110,
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppColors.surfaceCard,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: AppColors.borderLight),
        ),
        child: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SkeletonBox(width: 38, height: 38, radius: 12),
            Spacer(),
            SkeletonBox(width: 70, height: 10),
            SizedBox(height: 8),
            SkeletonBox(width: 100, height: 16),
          ],
        ),
      ),
    );

    return Shimmer(
      child: ListView(
        key: const Key('page_skeleton'),
        physics: const NeverScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          const SkeletonBox(height: 150, radius: 24),
          const SizedBox(height: 14),
          Row(children: [tile(), const SizedBox(width: 10), tile()]),
          const SizedBox(height: 10),
          Row(children: [tile(), const SizedBox(width: 10), tile()]),
          const SizedBox(height: 14),
          for (var i = 0; i < 4; i++) ...[const SkeletonCard(), const SizedBox(height: 12)],
        ],
      ),
    );
  }
}

// ------------------------------------------------------------- empty state

/// Friendly "nothing here yet" panel: a floating tinted icon and a message.
class EmptyState extends StatelessWidget {
  const EmptyState({super.key, required this.icon, required this.message, this.color = AppColors.tealPrimary});

  final IconData icon;
  final String message;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 28),
      decoration: BoxDecoration(
        color: AppColors.surfaceCard,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.borderLight),
      ),
      child: Column(
        children: [
          TweenAnimationBuilder<double>(
            tween: Tween<double>(begin: 0.6, end: 1),
            duration: const Duration(milliseconds: 700),
            curve: Curves.elasticOut,
            builder: (_, scale, child) => Transform.scale(scale: scale, child: child),
            child: Container(
              width: 72,
              height: 72,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [color.withValues(alpha: 0.18), color.withValues(alpha: 0.06)],
                ),
              ),
              child: Icon(icon, color: color, size: 34),
            ),
          ),
          const SizedBox(height: 14),
          Text(
            message,
            textAlign: TextAlign.center,
            style: theme.textTheme.bodyMedium?.copyWith(color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }
}
