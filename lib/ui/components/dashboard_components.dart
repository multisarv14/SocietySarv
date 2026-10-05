// Port of ui/components/DashboardComponents.kt.
//
// Public API:
//   class AnimatedSectionCard ({Key? key, int delayMillis = 0, required Widget child})
//   class MaintenanceStatusDashboard ({Key? key, required List<MaintenanceBill> bills,
//       required AppLanguage language, required VoidCallback onNavigateToMaintenance, bool showActions = true})
//   class GateActivityFeed ({Key? key, required List<VisitorLog> visitors, required AppLanguage language,
//       required VoidCallback onNavigateToGate})
//   class SmallVisitorBadge ({Key? key, required VisitorStatus status, required AppLanguage language})

import 'dart:async';

import 'package:flutter/material.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../util/format.dart';
import '../theme/app_colors.dart';

/// Animated card container that provides a subtle slide-in and fade-in
/// animation when navigating between sections.
class AnimatedSectionCard extends StatefulWidget {
  const AnimatedSectionCard({super.key, this.delayMillis = 0, required this.child});

  final int delayMillis;
  final Widget child;

  @override
  State<AnimatedSectionCard> createState() => _AnimatedSectionCardState();
}

class _AnimatedSectionCardState extends State<AnimatedSectionCard> with SingleTickerProviderStateMixin {
  // Fade runs 350ms and the slide 400ms, both after the same delay.
  late final AnimationController _controller =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 400));
  late final Animation<double> _fade = CurvedAnimation(
    parent: _controller,
    curve: const Interval(0, 350 / 400, curve: Curves.easeInOut),
  );
  late final Animation<Offset> _slide = Tween<Offset>(begin: const Offset(0, 1 / 6), end: Offset.zero)
      .animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
  Timer? _delay;

  @override
  void initState() {
    super.initState();
    if (widget.delayMillis <= 0) {
      _controller.forward();
    } else {
      _delay = Timer(Duration(milliseconds: widget.delayMillis), () {
        if (mounted) _controller.forward();
      });
    }
  }

  @override
  void dispose() {
    _delay?.cancel();
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _fade,
      child: SlideTransition(position: _slide, child: widget.child),
    );
  }
}

/// The small "Details ->" / "Terminal ->" pill in a card header.
class _HeaderAction extends StatelessWidget {
  const _HeaderAction({
    required this.testTag,
    required this.label,
    required this.background,
    required this.foreground,
    required this.onTap,
  });

  final String testTag;
  final String label;
  final Color background;
  final Color foreground;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      key: Key(testTag),
      color: background,
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                label,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: foreground,
                    ),
              ),
              const SizedBox(width: 4),
              Icon(Icons.arrow_forward, color: foreground, size: 12),
            ],
          ),
        ),
      ),
    );
  }
}

class _CardHeader extends StatelessWidget {
  const _CardHeader({
    required this.icon,
    required this.iconColor,
    required this.iconBackground,
    required this.title,
    required this.subtitle,
    this.action,
  });

  final IconData icon;
  final Color iconColor;
  final Color iconBackground;
  final String title;
  final String subtitle;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(color: iconBackground, borderRadius: BorderRadius.circular(10)),
          alignment: Alignment.center,
          child: Icon(icon, color: iconColor, size: 20),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: theme.colorScheme.onSurface,
                ),
              ),
              Text(
                subtitle,
                style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
        ?action,
      ],
    );
  }
}

class _DashboardCard extends StatelessWidget {
  const _DashboardCard({required this.testTag, required this.child});

  final String testTag;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      child: Card(
        key: Key(testTag),
        margin: EdgeInsets.zero,
        color: Theme.of(context).colorScheme.surface,
        elevation: 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: AppColors.borderLight),
        ),
        child: Padding(padding: const EdgeInsets.all(16), child: child),
      ),
    );
  }
}

/// Maintenance Status Dashboard Component.
/// Displays monthly dues, payment status, collection rate, and total pending
/// amounts for society members.
class MaintenanceStatusDashboard extends StatelessWidget {
  const MaintenanceStatusDashboard({
    super.key,
    required this.bills,
    required this.language,
    required this.onNavigateToMaintenance,
    this.showActions = true,
  });

  final List<MaintenanceBill> bills;
  final AppLanguage language;
  final VoidCallback onNavigateToMaintenance;

  /// False on a member's home: the Details button opens the committee's Bills
  /// screen, which is not part of a member's menu, so it is left off.
  final bool showActions;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;

    // Derived from the line items, so an edited bill is reflected here without
    // depending on the total that happened to be stored on the document.
    final totalBilled = bills.fold(0.0, (s, b) => s + b.payableTotal);
    final totalCollected = bills.fold(0.0, (s, b) => s + b.amountPaid);
    final totalPending = bills.fold(0.0, (s, b) => s + b.pendingAmount);
    final paidCount = bills.where((b) => b.derivedStatus == PaymentStatus.PAID).length;
    final pendingCount = bills.where((b) => b.derivedStatus != PaymentStatus.PAID).length;
    final progress = totalBilled > 0 ? (totalCollected / totalBilled).clamp(0.0, 1.0) : 0.0;

    final String subtitle;
    if (showActions) {
      final word = language == AppLanguage.marathi
          ? 'नोंदणीकृत सदनिका'
          : language == AppLanguage.hindi
              ? 'पंजीकृत फ्लैट'
              : 'registered units';
      subtitle = '${bills.length} $word';
    } else {
      // A member's card covers every unit they hold, so it says so rather
      // than looking like one.
      final units = bills.map((b) => b.flatNumber.toUpperCase()).toSet().length;
      final word = language == AppLanguage.marathi
          ? 'बिले'
          : language == AppLanguage.hindi
              ? 'बिल'
              : 'bills';
      subtitle = '${bills.length} $word${units > 1 ? ' • $units units' : ''}';
    }

    Widget metric(String label, double amount, Color bg, Color labelColor, Color valueColor) => Expanded(
          child: Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(12)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: textTheme.labelSmall?.copyWith(color: labelColor)),
                const SizedBox(height: 2),
                Text(
                  rupees0(amount),
                  style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold, color: valueColor),
                ),
              ],
            ),
          ),
        );

    return _DashboardCard(
      testTag: 'maintenance_status_dashboard_card',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header row with Title & Action
          _CardHeader(
            icon: Icons.pending_actions,
            iconColor: AppColors.tealPrimary,
            iconBackground: AppColors.tealContainer,
            title: AppStrings.get('maintenance_status_title', language),
            subtitle: subtitle,
            action: showActions
                ? _HeaderAction(
                    testTag: 'view_maintenance_details_btn',
                    label: language == AppLanguage.marathi
                        ? 'सविस्तर'
                        : language == AppLanguage.hindi
                            ? 'विवरण'
                            : 'Details',
                    background: AppColors.tealContainer,
                    foreground: AppColors.tealPrimary,
                    onTap: onNavigateToMaintenance,
                  )
                : null,
          ),
          const SizedBox(height: 14),

          // Metrics Summary Grid (3 Pillars: Total Dues, Collected, Total Pending)
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              spacing: 8,
              children: [
                metric(
                  AppStrings.get('monthly_dues', language),
                  totalBilled,
                  AppColors.surfaceVariant,
                  theme.colorScheme.onSurfaceVariant,
                  theme.colorScheme.onSurface,
                ),
                metric(
                  AppStrings.get('collected_amount', language),
                  totalCollected,
                  AppColors.successContainer,
                  AppColors.successGreen,
                  AppColors.successGreen,
                ),
                metric(
                  AppStrings.get('total_pending_amount', language),
                  totalPending,
                  AppColors.dangerContainer,
                  AppColors.dangerRed,
                  AppColors.dangerRed,
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // Progress Bar for Collection Status
          Row(
            children: [
              Expanded(
                child: Text(
                  AppStrings.get('collection_progress', language),
                  style: textTheme.labelSmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ),
              Text(
                '${(progress * 100).toInt()}% • $paidCount ${AppStrings.get('status_paid', language)} / '
                '$pendingCount ${AppStrings.get('status_unpaid', language)}',
                style: textTheme.labelSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: progress >= 0.75 ? AppColors.successGreen : AppColors.warningAmber,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 8,
              color: progress >= 0.75 ? AppColors.successGreen : AppColors.tealPrimary,
              backgroundColor: AppColors.surfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

/// Gate Activity Feed Component.
/// Displays visitor entries with timestamp and a small status badge.
class GateActivityFeed extends StatelessWidget {
  const GateActivityFeed({
    super.key,
    required this.visitors,
    required this.language,
    required this.onNavigateToGate,
  });

  final List<VisitorLog> visitors;
  final AppLanguage language;
  final VoidCallback onNavigateToGate;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final String summary;
    if (visitors.isEmpty) {
      summary = switch (language) {
        AppLanguage.marathi => 'कोणतीही अभ्यागत नोंद उपलब्ध नाही',
        AppLanguage.hindi => 'कोई आगंतुक प्रविष्टि नहीं',
        AppLanguage.english => 'No visitor logs recorded today',
      };
    } else {
      summary = switch (language) {
        AppLanguage.marathi => '${visitors.length} नोंदी - पाहण्यासाठी Terminal उघडा',
        AppLanguage.hindi => '${visitors.length} प्रविष्टियां - देखने के लिए Terminal खोलें',
        AppLanguage.english => '${visitors.length} entries - open Terminal to view them',
      };
    }

    return _DashboardCard(
      testTag: 'gate_activity_feed_card',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _CardHeader(
            icon: Icons.security,
            iconColor: AppColors.accentBlue,
            iconBackground: AppColors.accentBlueContainer,
            title: AppStrings.get('gate_activity_feed', language),
            subtitle: AppStrings.get('recent_visitor_entries', language),
            action: _HeaderAction(
              testTag: 'view_gate_terminal_btn',
              label: language == AppLanguage.marathi
                  ? 'सुरक्षा केंद्र'
                  : language == AppLanguage.hindi
                      ? 'गेट केंद्र'
                      : 'Terminal',
              background: AppColors.accentBlueContainer,
              foreground: AppColors.accentBlue,
              onTap: onNavigateToGate,
            ),
          ),
          const SizedBox(height: 12),
          // The home screen carries a count, not the log itself. The entries
          // live behind Terminal, so the dashboard stays a summary and the
          // activity is read where it can be acted on.
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: Center(
              child: Text(
                summary,
                key: const Key('gate_feed_summary'),
                style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class SmallVisitorBadge extends StatelessWidget {
  const SmallVisitorBadge({super.key, required this.status, required this.language});

  final VisitorStatus status;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final isInside = status == VisitorStatus.INSIDE;
    final bgColor = isInside ? AppColors.successContainer : AppColors.surfaceVariant;
    final textColor = isInside ? AppColors.successGreen : Theme.of(context).colorScheme.onSurfaceVariant;
    final label = switch (status) {
      VisitorStatus.INSIDE => switch (language) {
          AppLanguage.english => 'Inside',
          AppLanguage.hindi => 'परिसर में',
          AppLanguage.marathi => 'परिसरात',
        },
      VisitorStatus.EXITED => switch (language) {
          AppLanguage.english => 'Exited',
          AppLanguage.hindi => 'बाहर',
          AppLanguage.marathi => 'बाहेर',
        },
      VisitorStatus.PRE_APPROVED => switch (language) {
          AppLanguage.english => 'Pre-Approved',
          AppLanguage.hindi => 'स्वीकृत',
          AppLanguage.marathi => 'मंजूर',
        },
      VisitorStatus.REJECTED => switch (language) {
          AppLanguage.english => 'Rejected',
          AppLanguage.hindi => 'अस्वीकृत',
          AppLanguage.marathi => 'नामंजूर',
        },
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(color: bgColor, borderRadius: BorderRadius.circular(12)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(color: textColor, shape: BoxShape.circle),
          ),
          const SizedBox(width: 4),
          Text(
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                  fontSize: 10,
                  color: textColor,
                ),
          ),
        ],
      ),
    );
  }
}
