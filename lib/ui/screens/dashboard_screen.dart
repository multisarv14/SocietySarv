// Port of ui/screens/DashboardScreen.kt.

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../state/society_view_model.dart';
import '../../util/format.dart';
import '../components/common_components.dart';
import '../components/dashboard_components.dart';
import '../theme/app_colors.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  Future<void> _showExportDialog(BuildContext context, SocietyViewModel viewModel, AppLanguage language) {
    final summaryText = viewModel.generateExportSummary();
    return showDialog<void>(
      context: context,
      builder: (dialogContext) {
        final textTheme = Theme.of(dialogContext).textTheme;
        return AlertDialog(
          insetPadding: dialogInsetPadding(dialogContext),
          title: Text(
            tr('export_pdf_text', language),
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Card(
                margin: EdgeInsets.zero,
                elevation: 0,
                color: AppColors.surfaceVariant,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                child: Padding(
                  padding: const EdgeInsets.all(10),
                  child: Text(summaryText, style: textTheme.bodySmall),
                ),
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(dialogContext).pop(), child: Text(tr('close', language))),
            FilledButton(
              key: const Key('confirm_export_btn'),
              style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
              onPressed: () async {
                Navigator.of(dialogContext).pop();
                try {
                  await SharePlus.instance.share(ShareParams(text: summaryText));
                } catch (_) {
                  // Nothing to share with; the summary was still shown.
                }
                viewModel.showMessage(switch (language) {
                  AppLanguage.marathi => 'वित्तीय विवरण प्रत तयार केली!',
                  AppLanguage.hindi => 'वित्तीय विवरण सारांश तैयार!',
                  AppLanguage.english => 'Statement summary exported!',
                });
              },
              child: Text(switch (language) {
                AppLanguage.marathi => 'प्रत सामायिक करा',
                AppLanguage.hindi => 'साझा करें',
                AppLanguage.english => 'Share Statement',
              }),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<SocietyViewModel>();
    final language = viewModel.language;
    final society = viewModel.societyInfo;
    final currentUser = viewModel.currentUser;
    final financial = viewModel.financialOverview;
    final notices = viewModel.notices;
    final visitors = viewModel.visitors;
    final bills = viewModel.bills;
    final expenses = viewModel.expenses;
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final colorScheme = theme.colorScheme;

    /*
     * The member's home is a quieter screen than the committee's.
     *
     * The gate feed is the guard's and the committee's business, the quick
     * actions all lead to screens a member cannot open, and the maintenance
     * card's "Details" button would send them to the Bills screen that is no
     * longer part of their menu. None of it is shown to a member.
     */
    final isMember = currentUser?.role == UserRole.RESIDENT || currentUser?.role == UserRole.TENANT;

    // A member's maintenance card is about the unit they have selected in My
    // Unit, so a member who owns several sees the one they are looking at.
    final myUnits = viewModel.myUnits;

    final pinnedNotices = notices.where((n) => n.isPinned).toList();

    final List<MaintenanceBill> dashboardBills;
    if (isMember) {
      /*
       * Every unit the member holds, added together.
       *
       * A member with two flats owes what both of them owe; a home screen
       * showing one of them understates it. The unit-by-unit breakdown stays
       * in My Unit.
       */
      var myUnitNumbers = myUnits.map((u) => u.flatNumber.toUpperCase()).toList();
      if (myUnitNumbers.isEmpty && currentUser != null) {
        myUnitNumbers = [currentUser.flatNumber.toUpperCase()];
      }
      dashboardBills = bills.where((b) => myUnitNumbers.contains(b.flatNumber.toUpperCase())).toList();
    } else {
      dashboardBills = bills;
    }

    String langPick(String mr, String hi, String en) => switch (language) {
      AppLanguage.marathi => mr,
      AppLanguage.hindi => hi,
      AppLanguage.english => en,
    };

    final subtitleParts = <String>[
      if (society != null && society.billingStartMonth.trim().isNotEmpty) 'Billing from ${society.billingStartMonth}',
      if (society != null && society.name.trim().isNotEmpty) society.name,
    ];

    final items = <Widget>[
      // 1. Pinned Notices Banner (Animated)
      if (pinnedNotices.isNotEmpty)
        AnimatedSectionCard(
          delayMillis: 50,
          child: Card(
            key: const Key('pinned_notice_card'),
            margin: EdgeInsets.zero,
            elevation: 0,
            clipBehavior: Clip.antiAlias,
            color: const Color(0xFFFFFBEB),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
              side: const BorderSide(color: Color(0xFFFDE68A)),
            ),
            child: InkWell(
              onTap: () => viewModel.selectTab(NavDestination.COMMUNITY),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    Container(
                      width: 36,
                      height: 36,
                      decoration: const BoxDecoration(shape: BoxShape.circle, color: Color(0xFFFEF3C7)),
                      alignment: Alignment.center,
                      child: const Icon(Icons.campaign, color: AppColors.warningAmber, size: 20),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            pinnedNotices.first.title,
                            style: textTheme.titleSmall?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: const Color(0xFF92400E),
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          Text(
                            pinnedNotices.first.content,
                            style: textTheme.bodySmall?.copyWith(color: const Color(0xFFB45309)),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),

      // 2. MAINTENANCE STATUS DASHBOARD COMPONENT
      AnimatedSectionCard(
        delayMillis: 100,
        child: MaintenanceStatusDashboard(
          bills: dashboardBills,
          language: language,
          showActions: !isMember,
          onNavigateToMaintenance: () => viewModel.selectTab(NavDestination.MAINTENANCE),
        ),
      ),

      // 3. GATE ACTIVITY FEED COMPONENT - committee and staff only.
      if (!isMember)
        AnimatedSectionCard(
          delayMillis: 150,
          child: GateActivityFeed(
            visitors: visitors,
            language: language,
            onNavigateToGate: () => viewModel.selectTab(NavDestination.GATE),
          ),
        ),

      // 4. Financial Overview Top KPI Cards
      AnimatedSectionCard(
        delayMillis: 200,
        child: Column(
          children: [
            SectionHeader(
              title: tr('society_financial_overview', language),
              subtitle: subtitleParts.join(' • '),
              // Exports belong to the committee, not to a member.
              actionLabel: isMember ? null : tr('export_pdf_text', language),
              onActionClick: isMember ? null : () => _showExportDialog(context, viewModel, language),
              actionTestTag: 'dashboard_export_btn',
            ),
            IntrinsicHeight(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(
                    child: FinancialKpiCard(
                      title: tr('kpi_total_collection', language),
                      amount: financial.totalMaintenanceCollected,
                      subtitle: '${financial.paidFlatsCount} ${langPick('सदनिका जमा', 'फ्लैट जमा', 'flats paid')}',
                      icon: Icons.payments,
                      accentColor: AppColors.statusPaid,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FinancialKpiCard(
                      title: tr('kpi_operating_expenses', language),
                      amount: financial.totalOperatingExpenses,
                      subtitle: '${expenses.length} ${langPick('नोंदवलेले खर्च', 'व्यय प्रविष्टियां', 'vouchers')}',
                      icon: Icons.trending_down,
                      accentColor: AppColors.statusOverdue,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 10),
            IntrinsicHeight(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(
                    child: FinancialKpiCard(
                      title: tr('kpi_net_treasury', language),
                      amount: financial.netTreasuryBalance,
                      subtitle: 'Bank + Cash',
                      icon: Icons.account_balance_wallet,
                      accentColor: AppColors.tealPrimary,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FinancialKpiCard(
                      title: tr('kpi_outstanding_dues', language),
                      amount: financial.totalOutstandingDues,
                      subtitle: '${financial.pendingFlatsCount} ${langPick('थकीत सदनिका', 'बकाया फ्लैट', 'overdue')}',
                      icon: Icons.warning,
                      accentColor: AppColors.warningAmber,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),

      // 5. Quick Action Hub - every action here opens a committee screen.
      if (!isMember)
        AnimatedSectionCard(
          delayMillis: 250,
          child: Card(
            margin: EdgeInsets.zero,
            elevation: 0,
            color: colorScheme.surface,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
              side: const BorderSide(color: AppColors.borderLight),
            ),
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    langPick('जलद कामकाज (Quick Actions)', 'त्वरित कार्य (Quick Actions)', 'Operations & Actions'),
                    style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold, color: colorScheme.onSurface),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Flexible(
                        child: QuickActionItem(
                          label: tr('record_payment', language),
                          icon: Icons.receipt_long,
                          color: AppColors.tealPrimary,
                          onClick: () => viewModel.selectTab(NavDestination.MAINTENANCE),
                          testTag: 'quick_record_payment_btn',
                        ),
                      ),
                      Flexible(
                        child: QuickActionItem(
                          label: tr('record_expense', language),
                          icon: Icons.money_off,
                          color: AppColors.statusOverdue,
                          onClick: () => viewModel.selectTab(NavDestination.ACCOUNTS),
                          testTag: 'quick_record_expense_btn',
                        ),
                      ),
                      Flexible(
                        child: QuickActionItem(
                          label: tr('gate_terminal_title', language),
                          icon: Icons.security,
                          color: AppColors.tealLight,
                          onClick: () => viewModel.selectTab(NavDestination.GATE),
                          testTag: 'quick_gate_btn',
                        ),
                      ),
                      Flexible(
                        child: QuickActionItem(
                          label: tr('post_notice', language),
                          icon: Icons.campaign,
                          color: AppColors.warningAmber,
                          onClick: () => viewModel.selectTab(NavDestination.COMMUNITY),
                          testTag: 'quick_notice_btn',
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),

      // 6. Treasury Account Split Details
      AnimatedSectionCard(
        delayMillis: 300,
        child: Card(
          margin: EdgeInsets.zero,
          elevation: 0,
          color: colorScheme.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: AppColors.borderLight),
          ),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text(
                        langPick('संस्था बँक व रोख निधी', 'सोसायटी बैंक एवं नकद शेष', 'Bank & Cash Balances'),
                        style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                      ),
                    ),
                    Text(
                      'Net: ${rupees(financial.netTreasuryBalance)}',
                      style: textTheme.labelMedium?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                IntrinsicHeight(
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Expanded(
                        child: _BalanceTile(
                          label: tr('kpi_bank_balance', language),
                          amount: financial.bankBalance,
                          amountColor: AppColors.tealPrimary,
                          caption: society?.bankAccountName ?? 'HDFC Bank A/C',
                          singleLineCaption: true,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _BalanceTile(
                          label: tr('kpi_cash_balance', language),
                          amount: financial.cashBalance,
                          amountColor: AppColors.warningAmber,
                          caption: langPick('पेटी कॅश (हँड कॅश)', 'पेटी नकद', 'Petty Cash in Safe'),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    ];

    return ColoredBox(
      color: theme.scaffoldBackgroundColor,
      child: ListView.separated(
        padding: const EdgeInsets.only(left: 16, right: 16, top: 16, bottom: 80),
        itemCount: items.length,
        separatorBuilder: (_, _) => const SizedBox(height: 14),
        itemBuilder: (_, index) => items[index],
      ),
    );
  }
}

/// One of the two bank / cash tiles in the treasury card.
class _BalanceTile extends StatelessWidget {
  const _BalanceTile({
    required this.label,
    required this.amount,
    required this.amountColor,
    required this.caption,
    this.singleLineCaption = false,
  });

  final String label;
  final double amount;
  final Color amountColor;
  final String caption;
  final bool singleLineCaption;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    return Material(
      color: AppColors.surfaceVariant,
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: textTheme.labelSmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
            Text(
              rupees(amount),
              style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold, color: amountColor),
            ),
            Text(
              caption,
              style: textTheme.bodySmall,
              maxLines: singleLineCaption ? 1 : null,
              overflow: singleLineCaption ? TextOverflow.ellipsis : null,
            ),
          ],
        ),
      ),
    );
  }
}

class QuickActionItem extends StatelessWidget {
  const QuickActionItem({
    super.key,
    required this.label,
    required this.icon,
    required this.color,
    required this.onClick,
    required this.testTag,
  });

  final String label;
  final IconData icon;
  final Color color;
  final VoidCallback onClick;
  final String testTag;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return InkWell(
      key: Key(testTag),
      onTap: onClick,
      child: Padding(
        padding: const EdgeInsets.all(4),
        child: SizedBox(
          width: 72,
          child: Column(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  color: color.withValues(alpha: 0.12),
                ),
                alignment: Alignment.center,
                child: Icon(icon, color: color, size: 22, semanticLabel: label),
              ),
              const SizedBox(height: 4),
              Text(
                label,
                style: theme.textTheme.labelSmall?.copyWith(color: theme.colorScheme.onSurface),
                maxLines: 2,
                textAlign: TextAlign.center,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
