// SocietySarv subscription - which plan the society is on, until when, and
// the plans MultiSarv offers.
//
// Plans and the society's current plan come from Firestore and are managed in
// MultiSarv Core; the app only reads them. The Chairman can ask for a plan
// (new, renewal or change) here, and MultiSarv switches it on once paid.
//
// Public API:
//   class SubscriptionStatusCard ({Key? key, VoidCallback? onTap})
//   Future<void> showSubscriptionScreen(BuildContext context)

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../state/society_view_model.dart';
import '../../util/format.dart';
import '../components/common_components.dart';
import '../theme/app_colors.dart';

/// "06 Mar 2027" from the stored `yyyy-MM-dd`, or the raw text if unreadable.
String _dateLabel(String iso) {
  final d = DateTime.tryParse(iso);
  return d == null ? iso : formatDate(d, 'dd MMM yyyy');
}

String _durationLabel(int months, SocietyViewModel vm) {
  if (months > 0 && months % 12 == 0) {
    final years = months ~/ 12;
    return years == 1 ? vm.msg('१ वर्ष', '1 वर्ष', '1 year') : vm.msg('$years वर्षे', '$years वर्ष', '$years years');
  }
  return months == 1
      ? vm.msg('१ महिना', '1 महीना', '1 month')
      : vm.msg('$months महिने', '$months महीने', '$months months');
}

(String, Color, Color) _statusLook(SubscriptionStatus status, SocietyViewModel vm) {
  switch (status) {
    case SubscriptionStatus.ACTIVE:
      return (vm.msg('सक्रिय', 'सक्रिय', 'Active'), AppColors.statusPaid, AppColors.statusPaidBg);
    case SubscriptionStatus.EXPIRING_SOON:
      return (
        vm.msg('लवकरच संपणार', 'जल्द समाप्त', 'Expiring soon'),
        AppColors.statusPending,
        AppColors.statusPendingBg
      );
    case SubscriptionStatus.EXPIRED:
      return (vm.msg('संपला', 'समाप्त', 'Expired'), AppColors.statusOverdue, AppColors.statusOverdueBg);
    case SubscriptionStatus.NONE:
      return (vm.msg('प्लॅन नाही', 'कोई प्लान नहीं', 'No plan'), AppColors.textSecondary, AppColors.surfaceVariant);
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final SubscriptionStatus status;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final (label, fg, bg) = _statusLook(status, vm);
    return Container(
      key: Key('subscription_status_${status.name}'),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(20)),
      child:
          Text(label, style: Theme.of(context).textTheme.labelSmall?.copyWith(color: fg, fontWeight: FontWeight.bold)),
    );
  }
}

/// One-line summary of the plan's state: expiry, days left, or why there is none.
String _summary(SocietyViewModel vm) {
  final sub = vm.subscription;
  final status = vm.subscriptionStatus;
  if (sub == null || status == SubscriptionStatus.NONE) {
    return vm.msg('सोसायटीसाठी अजून प्लॅन सुरू केलेला नाही.', 'सोसायटी के लिए अभी कोई प्लान शुरू नहीं हुआ है।',
        'No plan has been started for this society yet.');
  }
  final expiry = _dateLabel(sub.expiryDate);
  final left = subscriptionDaysLeft(sub, DateTime.now());
  if (status == SubscriptionStatus.EXPIRED) {
    return vm.msg('$expiry रोजी संपला.', '$expiry को समाप्त हुआ।', 'Expired on $expiry.');
  }
  return vm.msg('$expiry पर्यंत वैध ($left दिवस बाकी).', '$expiry तक वैध ($left दिन शेष)।',
      'Valid till $expiry ($left days left).');
}

/// The society's plan at a glance, for the committee's home screen and the
/// Society Profile. Tapping it opens the full subscription screen.
class SubscriptionStatusCard extends StatelessWidget {
  const SubscriptionStatusCard({super.key, this.onTap});

  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final theme = Theme.of(context);
    final sub = vm.subscription;
    final status = vm.subscriptionStatus;
    final (_, accent, _) = _statusLook(status, vm);

    return Card(
      key: const Key('subscription_status_card'),
      margin: EdgeInsets.zero,
      color: theme.colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(
            color: status == SubscriptionStatus.ACTIVE ? AppColors.borderLight : accent.withValues(alpha: 0.5)),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap ?? () => showSubscriptionScreen(context),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(color: AppColors.tealContainer, borderRadius: BorderRadius.circular(12)),
                alignment: Alignment.center,
                child: const Icon(Icons.workspace_premium_rounded, color: AppColors.tealPrimary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Flexible(
                          child: Text(
                            sub == null || sub.planName.trim().isEmpty
                                ? vm.msg('सोसायटीसर्व प्लॅन', 'सोसायटीसर्व प्लान', 'SocietySarv plan')
                                : sub.planName,
                            style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        const SizedBox(width: 8),
                        _StatusChip(status: status),
                      ],
                    ),
                    const SizedBox(height: 2),
                    Text(_summary(vm),
                        style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: AppColors.textMuted),
            ],
          ),
        ),
      ),
    );
  }
}

Future<void> showSubscriptionScreen(BuildContext context) => Navigator.of(context).push(
      MaterialPageRoute<void>(fullscreenDialog: true, builder: (_) => const _SubscriptionScreen()),
    );

class _SubscriptionScreen extends StatelessWidget {
  const _SubscriptionScreen();

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final theme = Theme.of(context);
    final sub = vm.subscription;
    final plans = vm.subscriptionPlans;
    final pending = vm.pendingSubscriptionRequest;
    final muted = theme.colorScheme.onSurfaceVariant;
    final closed = vm.subscriptionRequests.where((r) => r.status != SubscriptionRequestStatus.PENDING).toList();

    final children = <Widget>[
      _CurrentPlanCard(subscription: sub),
      if (pending != null)
        Card(
          key: const Key('subscription_pending_request'),
          margin: EdgeInsets.zero,
          color: AppColors.statusInfoBg,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                const Icon(Icons.hourglass_top_rounded, color: AppColors.statusInfo),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    vm.msg(
                      '"${pending.planName}" साठी विनंती पाठवली आहे. MultiSarv टीम संपर्क करेल.',
                      '"${pending.planName}" के लिए अनुरोध भेजा गया है। MultiSarv टीम संपर्क करेगी।',
                      'Request for "${pending.planName}" sent. The MultiSarv team will contact you.',
                    ),
                    style: theme.textTheme.bodySmall?.copyWith(color: AppColors.statusInfo),
                  ),
                ),
              ],
            ),
          ),
        ),
      Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Text(
          vm.msg('उपलब्ध प्लॅन', 'उपलब्ध प्लान', 'Available plans'),
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
        ),
      ),
      if (plans.isEmpty)
        Text(
          vm.msg(
              'सध्या कोणतेही प्लॅन उपलब्ध नाहीत. MultiSarv शी संपर्क साधा.',
              'अभी कोई प्लान उपलब्ध नहीं है। MultiSarv से संपर्क करें।',
              'No plans are on offer right now. Contact MultiSarv.'),
          style: theme.textTheme.bodySmall?.copyWith(color: muted),
        ),
      for (final plan in plans) _PlanCard(plan: plan, isCurrent: sub?.planId == plan.id),
      if (closed.isNotEmpty) ...[
        Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Text(
            vm.msg('मागील विनंत्या', 'पिछले अनुरोध', 'Past requests'),
            style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
          ),
        ),
        for (final r in closed)
          Text(
            '${r.planName} • ${r.status == SubscriptionRequestStatus.DONE ? vm.msg('पूर्ण', 'पूर्ण', 'Done') : vm.msg('नाकारली', 'अस्वीकृत', 'Declined')}'
            '${r.adminNote.trim().isEmpty ? '' : ' • ${r.adminNote}'}',
            style: theme.textTheme.bodySmall?.copyWith(color: muted),
          ),
      ],
    ];

    return Scaffold(
      appBar: AppBar(
        title: Text(vm.msg('सबस्क्रिप्शन', 'सब्सक्रिप्शन', 'Subscription')),
        backgroundColor: AppColors.tealPrimary,
        foregroundColor: Colors.white,
      ),
      body: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: children.length,
        separatorBuilder: (_, _) => const SizedBox(height: 12),
        itemBuilder: (_, i) => AppEntrance(index: i, child: children[i]),
      ),
    );
  }
}

class _CurrentPlanCard extends StatelessWidget {
  const _CurrentPlanCard({required this.subscription});

  final SocietySubscription? subscription;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final theme = Theme.of(context);
    final sub = subscription;
    final status = vm.subscriptionStatus;
    final onDark = theme.textTheme.bodySmall?.copyWith(color: Colors.white.withValues(alpha: 0.85));

    return Container(
      key: const Key('subscription_current_plan'),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: const LinearGradient(colors: [AppColors.tealDark, AppColors.tealPrimary]),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  vm.msg('सध्याचा प्लॅन', 'वर्तमान प्लान', 'Current plan'),
                  style: theme.textTheme.labelMedium?.copyWith(color: Colors.white70),
                ),
              ),
              _StatusChip(status: status),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            sub == null || status == SubscriptionStatus.NONE
                ? vm.msg('प्लॅन नाही', 'कोई प्लान नहीं', 'No plan')
                : sub.planName,
            style: theme.textTheme.headlineSmall?.copyWith(color: Colors.white, fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 4),
          Text(_summary(vm), style: onDark),
          if (sub != null && status != SubscriptionStatus.NONE) ...[
            const SizedBox(height: 8),
            Text(
              '${vm.msg('सुरुवात', 'प्रारंभ', 'Started')}: ${_dateLabel(sub.startDate)}  •  '
              '${rupees0(sub.price)} / ${_durationLabel(sub.durationMonths, vm)}',
              style: onDark,
            ),
            for (final f in sub.features)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Row(
                  children: [
                    const Icon(Icons.check_circle, size: 14, color: Colors.white70),
                    const SizedBox(width: 6),
                    Expanded(child: Text(f, style: onDark)),
                  ],
                ),
              ),
          ],
        ],
      ),
    );
  }
}

class _PlanCard extends StatelessWidget {
  const _PlanCard({required this.plan, required this.isCurrent});

  final SubscriptionPlan plan;
  final bool isCurrent;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final theme = Theme.of(context);
    final muted = theme.colorScheme.onSurfaceVariant;
    final status = vm.subscriptionStatus;
    // The plan the society is already on is renewed rather than requested.
    final renew = isCurrent && status != SubscriptionStatus.NONE;
    final canAsk = vm.canRequestSubscription && vm.pendingSubscriptionRequest == null;

    return Card(
      key: Key('plan_card_${plan.id}'),
      margin: EdgeInsets.zero,
      color: theme.colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: isCurrent ? AppColors.tealPrimary : AppColors.borderLight, width: isCurrent ? 1.5 : 1),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(plan.name, style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                      if (plan.description.trim().isNotEmpty)
                        Text(plan.description, style: theme.textTheme.bodySmall?.copyWith(color: muted)),
                    ],
                  ),
                ),
                if (isCurrent)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(color: AppColors.tealContainer, borderRadius: BorderRadius.circular(20)),
                    child: Text(
                      vm.msg('सध्याचा', 'वर्तमान', 'Current'),
                      style: theme.textTheme.labelSmall
                          ?.copyWith(color: AppColors.tealPrimary, fontWeight: FontWeight.bold),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            Text.rich(
              TextSpan(children: [
                TextSpan(
                  text: rupees0(plan.price),
                  style:
                      theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800, color: AppColors.tealPrimary),
                ),
                TextSpan(
                    text: ' / ${_durationLabel(plan.durationMonths, vm)}',
                    style: theme.textTheme.bodySmall?.copyWith(color: muted)),
              ]),
            ),
            for (final f in plan.features)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.check_circle, size: 16, color: AppColors.statusPaid),
                    const SizedBox(width: 6),
                    Expanded(child: Text(f, style: theme.textTheme.bodySmall)),
                  ],
                ),
              ),
            if (vm.canRequestSubscription) ...[
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: AppButton.filled(
                  key: Key('request_plan_${plan.id}'),
                  style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
                  onPressed: canAsk ? () => _confirmRequest(context, vm, plan, renew) : null,
                  child: Text(
                    renew
                        ? vm.msg('नूतनीकरणाची विनंती', 'नवीनीकरण का अनुरोध', 'Request renewal')
                        : vm.msg('हा प्लॅन निवडा', 'यह प्लान चुनें', 'Choose this plan'),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Future<void> _confirmRequest(BuildContext context, SocietyViewModel vm, SubscriptionPlan plan, bool renew) async {
    final note = TextEditingController();
    final send = await showAppDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        insetPadding: dialogInsetPadding(dialogContext),
        title: Text(renew ? vm.msg('नूतनीकरण', 'नवीनीकरण', 'Renew plan') : plan.name),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              vm.msg(
                '${plan.name} (${rupees0(plan.price)} / ${_durationLabel(plan.durationMonths, vm)}) साठी MultiSarv ला विनंती पाठवली जाईल. पेमेंट झाल्यावर प्लॅन सुरू होईल.',
                '${plan.name} (${rupees0(plan.price)} / ${_durationLabel(plan.durationMonths, vm)}) के लिए MultiSarv को अनुरोध भेजा जाएगा। भुगतान के बाद प्लान शुरू होगा।',
                'MultiSarv will be asked to start ${plan.name} (${rupees0(plan.price)} / ${_durationLabel(plan.durationMonths, vm)}). The plan starts once payment is received.',
              ),
            ),
            const SizedBox(height: 10),
            TextField(
              key: const Key('plan_request_note'),
              controller: note,
              maxLines: 2,
              decoration: InputDecoration(labelText: vm.msg('टीप (ऐच्छिक)', 'टिप्पणी (वैकल्पिक)', 'Note (optional)')),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: Text(tr('cancel', vm.language))),
          FilledButton(
            key: const Key('plan_request_send'),
            style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text(vm.msg('विनंती पाठवा', 'अनुरोध भेजें', 'Send request')),
          ),
        ],
      ),
    );
    final text = note.text;
    note.dispose();
    if (send != true || !context.mounted) return;
    // This screen sits over the shell, so the message is shown here rather
    // than on the shell's own snackbar underneath.
    final messenger = ScaffoldMessenger.of(context);
    vm.requestSubscription(plan, text,
        onResult: (_, message) => messenger.showSnackBar(SnackBar(content: Text(message))));
  }
}
