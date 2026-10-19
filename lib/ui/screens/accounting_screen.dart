// Port of ui/screens/AccountingScreen.kt.

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';

import '../../data/models.dart';
import '../../l10n/app_strings.dart';
import '../../state/society_view_model.dart';
import '../../util/format.dart';
import '../../util/ledger_pdf.dart';
import '../components/common_components.dart';
import '../theme/app_colors.dart';

class AccountingScreen extends StatefulWidget {
  const AccountingScreen({super.key});

  @override
  State<AccountingScreen> createState() => _AccountingScreenState();
}

class _AccountingScreenState extends State<AccountingScreen> with SingleTickerProviderStateMixin {
  late final TabController _tabController = TabController(length: 4, vsync: this);
  int _selectedSubTab = 0;

  @override
  void initState() {
    super.initState();
    _tabController.addListener(() {
      if (_tabController.index != _selectedSubTab) {
        setState(() => _selectedSubTab = _tabController.index);
      }
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<SocietyViewModel>();
    final language = viewModel.language;
    final society = viewModel.societyInfo;
    final financial = viewModel.financialOverview;
    final currentUser = viewModel.currentUser;
    final theme = Theme.of(context);

    // Chairman and Treasurer keep the write actions; everyone else sees the
    // same books, read-only.
    final canEdit = viewModel.canEditAccounts;

    // A member opens Accounts to see what the society holds - nothing else.
    // No sub-tabs, no ledgers, no journals and no export.
    final isMember = currentUser?.role == UserRole.RESIDENT || currentUser?.role == UserRole.TENANT;
    if (isMember) {
      return MemberBalanceView(financial: financial, society: society, language: language);
    }

    final subTabs = [
      tr('balance_sheet_title', language),
      tr('member_ledger', language),
      tr('expense_journal', language),
      tr('non_fee_revenue', language),
    ];

    final Widget body = switch (_selectedSubTab) {
      0 => BalanceSheetView(financial: financial, society: society, language: language),
      1 => MemberLedgerView(society: society, language: language),
      2 => ExpenseJournalView(language: language, canEdit: canEdit),
      _ => NonFeeRevenueView(language: language, canEdit: canEdit),
    };

    return ColoredBox(
      color: theme.scaffoldBackgroundColor,
      child: Column(
        children: [
          Material(
            color: theme.colorScheme.surface,
            child: TabBar(
              controller: _tabController,
              isScrollable: true,
              tabAlignment: TabAlignment.start,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              labelColor: AppColors.tealPrimary,
              unselectedLabelColor: AppColors.textSecondary,
              labelStyle: theme.textTheme.labelLarge?.copyWith(fontWeight: FontWeight.bold),
              unselectedLabelStyle: theme.textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w500),
              tabs: [
                for (var index = 0; index < subTabs.length; index++)
                  Tab(key: Key('accounting_subtab_$index'), child: Text(subTabs[index], maxLines: 1)),
              ],
            ),
          ),
          Expanded(
            child: FadeThroughSwitcher(child: KeyedSubtree(key: ValueKey('acc_$_selectedSubTab'), child: body)),
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------- helpers

/// White card with the light slate outline every accounts card uses.
class _OutlinedCard extends StatelessWidget {
  const _OutlinedCard({required this.child, this.radius = 16, this.onTap, this.cardKey});

  final Widget child;
  final double radius;
  final VoidCallback? onTap;
  final Key? cardKey;

  @override
  Widget build(BuildContext context) {
    return Card(
      key: cardKey,
      margin: EdgeInsets.zero,
      elevation: 0,
      clipBehavior: Clip.antiAlias,
      color: Theme.of(context).colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(radius),
        side: const BorderSide(color: AppColors.borderLight),
      ),
      child: onTap == null ? child : InkWell(onTap: onTap, child: child),
    );
  }
}

/// A label on the left and an amount on the right.
class _AmountLine extends StatelessWidget {
  const _AmountLine({required this.label, required this.value, this.labelStyle, this.valueStyle, this.valueKey});

  final String label;
  final String value;
  final TextStyle? labelStyle;
  final TextStyle? valueStyle;
  final Key? valueKey;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Flexible(child: Text(label, style: labelStyle)),
        const SizedBox(width: 8),
        Text(value, key: valueKey, style: valueStyle),
      ],
    );
  }
}

InputDecoration _outlined(String label, {String? hint, Widget? prefixIcon, String? errorText}) => InputDecoration(
  labelText: label,
  hintText: hint,
  prefixIcon: prefixIcon,
  errorText: errorText,
);

// ------------------------------------------------------- member balance

/// Accounts as a member sees it: the society's balances, and nothing else.
///
/// The ledgers, the expense journal, the revenue register and every export
/// belong to the committee, so none of them are reachable from here.
class MemberBalanceView extends StatelessWidget {
  const MemberBalanceView({super.key, required this.financial, required this.society, required this.language});

  final FinancialOverview financial;
  final SocietyInfo? society;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final bankAccountName = society?.bankAccountName ?? '';
    return ColoredBox(
      color: theme.scaffoldBackgroundColor,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Card(
              key: const Key('member_balance_card'),
              margin: EdgeInsets.zero,
              elevation: 0,
              color: AppColors.tealContainer,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      tr('kpi_net_treasury', language),
                      style: textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w500, color: AppColors.tealPrimary),
                    ),
                    Text(
                      rupees(financial.netTreasuryBalance),
                      key: const Key('member_net_treasury_amount'),
                      style: textTheme.headlineMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: AppColors.tealPrimary,
                      ),
                    ),
                    Text(
                      society?.name ?? '',
                      style: textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 14),
            _OutlinedCard(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _AmountLine(
                      label: tr('kpi_bank_balance', language),
                      value: rupees(financial.bankBalance),
                      labelStyle: textTheme.bodyMedium,
                      valueStyle: textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: AppColors.tealPrimary,
                      ),
                    ),
                    const Divider(height: 20),
                    _AmountLine(
                      label: tr('kpi_cash_balance', language),
                      value: rupees(financial.cashBalance),
                      labelStyle: textTheme.bodyMedium,
                      valueStyle: textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: AppColors.warningAmber,
                      ),
                    ),
                    if (bankAccountName.trim().isNotEmpty) ...[
                      const SizedBox(height: 10),
                      Text(
                        bankAccountName,
                        style: textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ------------------------------------------------------- 1. balance sheet

class BalanceSheetView extends StatelessWidget {
  const BalanceSheetView({super.key, required this.financial, required this.society, required this.language});

  final FinancialOverview financial;
  final SocietyInfo? society;
  final AppLanguage language;

  Future<void> _showExportDialog(BuildContext context) {
    final viewModel = context.read<SocietyViewModel>();
    final exportText = viewModel.generateExportSummary();
    return showAppDialog<void>(
      context: context,
      builder: (dialogContext) {
        final textTheme = Theme.of(dialogContext).textTheme;
        return AlertDialog(
          insetPadding: dialogInsetPadding(dialogContext),
          title: Text(
            tr('balance_sheet_title', language),
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          content: SizedBox(
            width: double.maxFinite,
            child: SingleChildScrollView(
              child: Card(
                margin: EdgeInsets.zero,
                elevation: 0,
                color: AppColors.surfaceVariant,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                child: Padding(
                  padding: const EdgeInsets.all(10),
                  child: Text(exportText, style: textTheme.bodySmall),
                ),
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(dialogContext).pop(), child: Text(tr('close', language))),
            AppButton.filled(
              style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
              onPressed: () async {
                Navigator.of(dialogContext).pop();
                try {
                  await SharePlus.instance.share(
                    ShareParams(text: exportText, subject: tr('balance_sheet_title', language)),
                  );
                } catch (_) {
                  // Nothing to share with; the summary was still shown.
                }
                viewModel.showMessage(
                  language == AppLanguage.marathi ? 'ताळेबंद प्रत डाऊनलोड केली!' : 'Balance Sheet exported!',
                );
              },
              child: Text(language == AppLanguage.marathi ? 'प्रत सामायिक करा' : 'Export Document'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final onSurfaceVariant = theme.colorScheme.onSurfaceVariant;

    // The period the sheet covers: the society's billing window, not a date
    // hard-coded into the screen.
    final months = billingMonths(society);
    final selectedMonth = months.isEmpty ? '' : months.first;
    final from = months.isEmpty ? '' : months.last.split(' ').last;
    final to = months.isEmpty ? '' : months.first.split(' ').last;
    final String selectedFY;
    if (from.trim().isEmpty || to.trim().isEmpty) {
      selectedFY = '';
    } else if (from == to) {
      selectedFY = 'FY $from';
    } else {
      selectedFY = 'FY $from-$to';
    }

    final bodyMedium = textTheme.bodyMedium;
    final bodyMediumBold = bodyMedium?.copyWith(fontWeight: FontWeight.bold);
    final bodySmall = textTheme.bodySmall;
    final bodySmallBold = bodySmall?.copyWith(fontWeight: FontWeight.bold);
    final isMr = language == AppLanguage.marathi;

    final items = <Widget>[
      // Summary Net Surplus Card
      Card(
        margin: EdgeInsets.zero,
        elevation: 0,
        color: AppColors.tealContainer,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                tr('net_surplus', language),
                style: textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w500, color: AppColors.tealPrimary),
              ),
              Text(
                rupees(financial.netSurplus),
                style: textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
              ),
              const SizedBox(height: 4),
              Text(
                'Total Gross Income: ${rupees(financial.totalGrossIncome)} | Total Expenses: ${rupees(financial.totalOperatingExpenses)}',
                style: bodySmall?.copyWith(color: onSurfaceVariant),
              ),
            ],
          ),
        ),
      ),

      // Statement: Income vs Expenditure
      _OutlinedCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                tr('income_expenditure', language),
                style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold, color: theme.colorScheme.onSurface),
              ),
              const Divider(height: 20),
              // Income Section
              _AmountLine(
                label: '1. ${tr('kpi_total_collection', language)}',
                value: rupees(financial.totalMaintenanceCollected),
                labelStyle: bodyMedium,
                valueStyle: bodyMediumBold?.copyWith(color: AppColors.statusPaid),
              ),
              const SizedBox(height: 6),
              _AmountLine(
                label: '2. ${tr('non_fee_revenue', language)}',
                value: rupees(financial.totalNonFeeRevenue),
                labelStyle: bodyMedium,
                valueStyle: bodyMediumBold?.copyWith(color: AppColors.statusPaid),
              ),
              const SizedBox(height: 6),
              _AmountLine(
                label: tr('total_income', language),
                value: rupees(financial.totalGrossIncome),
                labelStyle: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                valueStyle: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold, color: AppColors.statusPaid),
              ),
              const Divider(height: 20),
              // Expense Section
              _AmountLine(
                label: tr('kpi_operating_expenses', language),
                value: rupees(financial.totalOperatingExpenses),
                labelStyle: bodyMedium,
                valueStyle: bodyMediumBold?.copyWith(color: AppColors.statusOverdue),
              ),
            ],
          ),
        ),
      ),

      // Statement: Assets vs Liabilities / Funds
      _OutlinedCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                isMr ? 'संस्थेची मालमत्ता व देणी (Assets & Liabilities)' : 'Assets & Liabilities Summary',
                style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
              ),
              const Divider(height: 20),
              Text(
                isMr ? 'मालमत्ता व निधी (Assets):' : 'Current Assets & Funds:',
                style: textTheme.labelLarge?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
              ),
              const SizedBox(height: 4),
              _AmountLine(
                label: '• ${tr('kpi_bank_balance', language)}',
                value: rupees(financial.bankBalance),
                labelStyle: bodySmall,
                valueStyle: bodySmallBold,
              ),
              _AmountLine(
                label: '• ${tr('kpi_cash_balance', language)}',
                value: rupees(financial.cashBalance),
                labelStyle: bodySmall,
                valueStyle: bodySmallBold,
              ),
              _AmountLine(
                label: '• ${tr('kpi_outstanding_dues', language)}',
                value: rupees(financial.totalOutstandingDues),
                labelStyle: bodySmall,
                valueStyle: bodySmallBold?.copyWith(color: AppColors.warningAmber),
              ),
              const SizedBox(height: 10),
              Text(
                isMr ? 'राखीव निधी व दायित्व (Liabilities & Reserves):' : 'Statutory Reserves & Funds:',
                style: textTheme.labelLarge?.copyWith(fontWeight: FontWeight.bold, color: AppColors.warningAmber),
              ),
              const SizedBox(height: 4),
              _AmountLine(
                label: '• ${tr('fund_sinking', language)}',
                value: rupees(financial.totalGrossIncome * 0.15),
                labelStyle: bodySmall,
                valueStyle: bodySmallBold,
              ),
              _AmountLine(
                label: '• ${tr('fund_major_repair', language)}',
                value: rupees(financial.totalGrossIncome * 0.10),
                labelStyle: bodySmall,
                valueStyle: bodySmallBold,
              ),
              _AmountLine(
                label: '• ${tr('fund_csr', language)}',
                value: rupees(financial.totalGrossIncome * 0.02),
                labelStyle: bodySmall,
                valueStyle: bodySmallBold,
                valueKey: const Key('fund_csr_amount'),
              ),
            ],
          ),
        ),
      ),

      // Filter row & Export Action
      Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: AppColors.surfaceVariant,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.borderLight),
              ),
              child: Text(
                '📅 ${selectedFY.trim().isEmpty ? 'Billing' : selectedFY} • ${selectedMonth.trim().isEmpty ? '-' : selectedMonth}',
                style: textTheme.labelMedium?.copyWith(fontWeight: FontWeight.bold),
              ),
            ),
          ),
          const SizedBox(width: 8),
          OutlinedButton.icon(
            key: const Key('export_balance_sheet_btn'),
            onPressed: () => _showExportDialog(context),
            style: OutlinedButton.styleFrom(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              side: const BorderSide(color: AppColors.tealPrimary),
              foregroundColor: AppColors.tealPrimary,
            ),
            icon: const Icon(Icons.download, color: AppColors.tealPrimary, size: 16),
            label: Text(
              tr('export_pdf_text', language),
              style: textTheme.labelMedium?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
            ),
          ),
        ],
      ),
    ];

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      separatorBuilder: (_, _) => const SizedBox(height: 14),
      itemBuilder: (_, index) => AppEntrance(index: index, child: items[index]),
    );
  }
}

// ------------------------------------------------------- 2. member ledger

/// The three states the member list can be filtered to.
enum LedgerFilter {
  UNPAID('status_unpaid'),
  PAID('status_paid'),
  ALL('filter_all');

  const LedgerFilter(this.labelKey);
  final String labelKey;
}

/// Member Ledger.
///
/// Opens on the members who still owe something - that is what a chairman came
/// here for - with a Paid / All toggle to widen it. Tapping a member card opens
/// their month-by-month ledger: what was billed, what was paid, what is left,
/// and the payment details behind each row.
class MemberLedgerView extends StatefulWidget {
  const MemberLedgerView({super.key, required this.society, required this.language});

  final SocietyInfo? society;
  final AppLanguage language;

  @override
  State<MemberLedgerView> createState() => _MemberLedgerViewState();
}

class _MemberLedgerViewState extends State<MemberLedgerView> {
  String _searchQuery = '';
  LedgerFilter _ledgerFilter = LedgerFilter.UNPAID;

  Future<void> _openMember(Resident member) async {
    MaintenanceBill? receiptBill;
    await showAppDialog<void>(
      context: context,
      builder: (_) => MemberLedgerDetailDialog(
        member: member,
        society: widget.society,
        language: widget.language,
        onOpenReceipt: (bill) => receiptBill = bill,
      ),
    );
    final bill = receiptBill;
    if (bill != null && mounted) {
      await showReceiptDialog(context, bill: bill, society: widget.society, language: widget.language);
    }
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<SocietyViewModel>();
    final residents = viewModel.members;
    final bills = viewModel.bills;
    final language = widget.language;
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final onSurfaceVariant = theme.colorScheme.onSurfaceVariant;

    // Pending is taken from the bills themselves, so the list can never
    // disagree with what the member's own ledger shows.
    final pendingByFlat = <String, double>{};
    // How many bills each unit has. A unit that was never billed owes nothing,
    // which is not the same thing as having paid - see the filter below.
    final billCountByFlat = <String, int>{};
    final billedByFlat = <String, double>{};
    final paidByFlat = <String, double>{};
    for (final b in bills) {
      final key = b.flatNumber.toUpperCase();
      pendingByFlat[key] = (pendingByFlat[key] ?? 0.0) + b.pendingAmount;
      billCountByFlat[key] = (billCountByFlat[key] ?? 0) + 1;
      billedByFlat[key] = (billedByFlat[key] ?? 0.0) + b.payableTotal;
      paidByFlat[key] = (paidByFlat[key] ?? 0.0) + b.amountPaid;
    }

    final query = _searchQuery.toLowerCase();
    final visibleMembers = residents
        .where((r) => r.ownerName.trim().isNotEmpty)
        .where((r) => r.flatNumber.toLowerCase().contains(query) || r.ownerName.toLowerCase().contains(query))
        .where((member) {
          final pending = pendingByFlat[member.flatNumber.toUpperCase()] ?? 0.0;
          final billCount = billCountByFlat[member.flatNumber.toUpperCase()] ?? 0;
          switch (_ledgerFilter) {
            case LedgerFilter.UNPAID:
              return pending > 0.009;
            /*
         * Paid means "billed and settled". A unit that has never been
         * billed also has nothing pending, and used to be listed here
         * as paid ₹0 - which read as a collection that never happened.
         */
            case LedgerFilter.PAID:
              return billCount > 0 && pending <= 0.009;
            case LedgerFilter.ALL:
              return true;
          }
        })
        .toList();

    final header = <Widget>[
      TextField(
        key: const Key('ledger_search_input'),
        onChanged: (v) => setState(() => _searchQuery = v),
        decoration: _outlined(tr('search_flat', language), prefixIcon: const Icon(Icons.search)),
        maxLines: 1,
      ),
      // Unpaid / Paid / All - the list follows the selection.
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            tr('filter_label', language),
            style: textTheme.labelMedium?.copyWith(fontWeight: FontWeight.bold, color: onSurfaceVariant),
          ),
          const SizedBox(height: 4),
          RadioGroup<LedgerFilter>(
            groupValue: _ledgerFilter,
            onChanged: (v) {
              if (v != null) setState(() => _ledgerFilter = v);
            },
            child: Row(
              children: [
                for (final option in LedgerFilter.values)
                  Expanded(
                    child: Row(
                      key: Key('ledger_filter_${option.name.toLowerCase()}'),
                      children: [
                        Radio<LedgerFilter>(value: option, activeColor: AppColors.tealPrimary),
                        Flexible(
                          child: Text(
                            tr(option.labelKey, language),
                            style: textTheme.bodySmall?.copyWith(
                              fontWeight: _ledgerFilter == option ? FontWeight.bold : FontWeight.normal,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
          Text('${visibleMembers.length} member(s)', style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant)),
        ],
      ),
    ];

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: header.length + visibleMembers.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: appEntrance((context, index) {
        if (index < header.length) return header[index];
        final resident = visibleMembers[index - header.length];
        final key = resident.flatNumber.toUpperCase();
        final pending = pendingByFlat[key] ?? 0.0;
        final billCount = billCountByFlat[key] ?? 0;
        final billed = billedByFlat[key] ?? 0.0;
        final paid = paidByFlat[key] ?? 0.0;

        final Color pillColor;
        final Color pillTextColor;
        final String pillText;
        if (billCount == 0) {
          pillColor = AppColors.surfaceVariant;
          pillTextColor = onSurfaceVariant;
          pillText = 'No bills raised yet';
        } else if (pending > 0.009) {
          pillColor = AppColors.statusOverdueBg;
          pillTextColor = AppColors.statusOverdue;
          pillText = tr('status_unpaid', language);
        } else {
          pillColor = AppColors.statusPaidBg;
          pillTextColor = AppColors.statusPaid;
          pillText = tr('status_paid', language);
        }

        // Wing and unit type, the same two facts the member list shows, so
        // the ledger reads the same way.
        final details = [
          if (resident.wing.trim().isNotEmpty) 'Wing ${resident.wing}',
          resident.unitType.trim().isNotEmpty ? resident.unitType : resident.flatType.displayName,
        ].join(' • ');

        return _OutlinedCard(
          cardKey: Key('ledger_member_${resident.flatNumber}'),
          onTap: () => _openMember(resident),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${resident.flatNumber} • ${resident.ownerName}',
                        style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      Text(details, style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant)),
                      const SizedBox(height: 4),
                      // In the All list the rows are a mix, so each one says
                      // where it stands rather than leaving it to be worked
                      // out from the numbers on the right.
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(color: pillColor, borderRadius: BorderRadius.circular(8)),
                        child: Text(
                          pillText,
                          style: textTheme.labelSmall?.copyWith(fontWeight: FontWeight.bold, color: pillTextColor),
                        ),
                      ),
                    ],
                  ),
                ),
                // Billed and paid alongside pending, so "All" says what
                // actually happened rather than only what is left.
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text('Billed ${rupees0(billed)}', style: textTheme.labelSmall?.copyWith(color: onSurfaceVariant)),
                    Text('Paid ${rupees0(paid)}', style: textTheme.labelSmall?.copyWith(color: AppColors.statusPaid)),
                    Text(
                      'Pending ${rupees(pending)}',
                      style: textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: pending > 0.009 ? AppColors.statusOverdue : AppColors.statusPaid,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      }),
    );
  }
}

/// Chronological ordering for "August 2026" style month labels.
int _ledgerMonthKey(String billMonth) => parseMonthYear(billMonth)?.millisecondsSinceEpoch ?? 0;

/// One member's ledger, laid out as a plain month-wise table - Month, Bill, Late
/// Fee, Total, Paid, Pending - with a running outstanding under it and a button
/// that writes the same table out as a PDF to save or share.
class MemberLedgerDetailDialog extends StatefulWidget {
  const MemberLedgerDetailDialog({
    super.key,
    required this.member,
    required this.society,
    required this.language,
    required this.onOpenReceipt,
  });

  final Resident member;
  final SocietyInfo? society;
  final AppLanguage language;

  /// Called (after this dialog has closed itself) with the bill whose receipt
  /// should be opened.
  final ValueChanged<MaintenanceBill> onOpenReceipt;

  @override
  State<MemberLedgerDetailDialog> createState() => _MemberLedgerDetailDialogState();
}

class _MemberLedgerDetailDialogState extends State<MemberLedgerDetailDialog> {
  String? _pdfStatus;
  bool _isPdfError = false;

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<SocietyViewModel>();
    final member = widget.member;
    final language = widget.language;
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final onSurfaceVariant = theme.colorScheme.onSurfaceVariant;

    // Stable sort by month, oldest first.
    final indexed =
        viewModel.bills
            .where((b) => b.flatNumber.toLowerCase() == member.flatNumber.toLowerCase())
            .toList()
            .asMap()
            .entries
            .toList()
          ..sort((a, b) {
            final c = _ledgerMonthKey(a.value.billMonth).compareTo(_ledgerMonthKey(b.value.billMonth));
            return c != 0 ? c : a.key.compareTo(b.key);
          });
    final ordered = indexed.map((e) => e.value).toList();
    double sum(double Function(MaintenanceBill) f) => ordered.fold(0.0, (a, b) => a + f(b));
    final totalBilled = sum((b) => b.payableTotal);
    final totalPaid = sum((b) => b.amountPaid);
    final totalPending = sum((b) => b.pendingAmount);

    final List<Widget> content;
    if (ordered.isEmpty) {
      content = [
        Text(
          'No bills have been raised for this unit yet.',
          style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
        ),
      ];
    } else {
      content = [
        // Header row.
        Container(
          decoration: const BoxDecoration(
            color: AppColors.tealContainer,
            borderRadius: BorderRadius.only(topLeft: Radius.circular(8), topRight: Radius.circular(8)),
          ),
          padding: const EdgeInsets.all(6),
          child: const Row(
            children: [
              _LedgerCell('Month', flex: 20, bold: true),
              _LedgerCell('Bill', flex: 13, bold: true, end: true),
              _LedgerCell('Late', flex: 10, bold: true, end: true),
              _LedgerCell('Total', flex: 13, bold: true, end: true),
              _LedgerCell('Paid', flex: 13, bold: true, end: true),
              _LedgerCell('Pending', flex: 13, bold: true, end: true),
            ],
          ),
        ),
        for (final bill in ordered) ...[
          InkWell(
            key: Key('ledger_row_${bill.billMonth}'),
            onTap: () {
              if (bill.amountPaid > 0) {
                Navigator.of(context).pop();
                widget.onOpenReceipt(bill);
              }
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 7),
              child: Row(
                children: [
                  _LedgerCell(bill.billMonth, flex: 20),
                  _LedgerCell(fmt0(bill.chargesSubtotal), flex: 13, end: true),
                  _LedgerCell(fmt0(bill.lateFeeAmount), flex: 10, end: true),
                  _LedgerCell(fmt0(bill.payableTotal), flex: 13, end: true, bold: true),
                  _LedgerCell(fmt0(bill.amountPaid), flex: 13, end: true, colour: AppColors.statusPaid),
                  _LedgerCell(
                    fmt0(bill.pendingAmount),
                    flex: 13,
                    end: true,
                    bold: true,
                    colour: bill.pendingAmount > 0.009 ? AppColors.statusOverdue : AppColors.statusPaid,
                  ),
                ],
              ),
            ),
          ),
          const Divider(height: 1, color: AppColors.borderLight),
        ],
        // Closing totals.
        Container(
          color: AppColors.surfaceVariant,
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 8),
          child: Row(
            children: [
              const _LedgerCell('Total', flex: 20, bold: true),
              _LedgerCell(fmt0(sum((b) => b.chargesSubtotal)), flex: 13, bold: true, end: true),
              _LedgerCell(fmt0(sum((b) => b.lateFeeAmount)), flex: 10, bold: true, end: true),
              _LedgerCell(fmt0(totalBilled), flex: 13, bold: true, end: true),
              _LedgerCell(fmt0(totalPaid), flex: 13, bold: true, end: true),
              _LedgerCell(
                fmt0(totalPending),
                flex: 13,
                bold: true,
                end: true,
                colour: totalPending > 0.009 ? AppColors.statusOverdue : AppColors.statusPaid,
              ),
            ],
          ),
        ),
        const SizedBox(height: 6),
        Text('Tap a paid month to open its receipt.', style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant)),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                key: const Key('ledger_pdf_btn'),
                onPressed: () async {
                  final file = await LedgerPdf.generate(member, ordered, widget.society);
                  if (!mounted) return;
                  setState(() {
                    _isPdfError = file == null;
                    _pdfStatus = file == null
                        ? 'Could not create the ledger PDF.'
                        : 'Saved as ${file.uri.pathSegments.last}';
                  });
                },
                style: OutlinedButton.styleFrom(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  side: const BorderSide(color: AppColors.tealPrimary),
                  foregroundColor: AppColors.tealPrimary,
                ),
                icon: const Icon(Icons.picture_as_pdf, color: AppColors.tealPrimary, size: 16),
                label: Text(
                  tr('download_pdf', language),
                  style: textTheme.labelMedium?.copyWith(color: AppColors.tealPrimary),
                ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: AppButton.filledIcon(
                key: const Key('ledger_share_pdf_btn'),
                onPressed: () async {
                  final failure = await LedgerPdf.share(member, ordered, widget.society);
                  if (!mounted) return;
                  setState(() {
                    _isPdfError = failure != null;
                    _pdfStatus = failure;
                  });
                },
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.tealPrimary,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                ),
                icon: const Icon(Icons.share, size: 16),
                label: Text(tr('share_pdf', language), style: textTheme.labelMedium?.copyWith(color: Colors.white)),
              ),
            ),
          ],
        ),
        if (_pdfStatus != null) ...[
          const SizedBox(height: 8),
          Text(
            _pdfStatus!,
            style: textTheme.bodySmall?.copyWith(color: _isPdfError ? theme.colorScheme.error : AppColors.statusPaid),
          ),
        ],
      ];
    }

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${member.flatNumber} • ${member.ownerName}',
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          Text(tr('member_ledger', language), style: textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary)),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: content),
        ),
      ),
      actions: [
        AppButton.filled(
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: () => Navigator.of(context).pop(),
          child: Text(tr('close', language)),
        ),
      ],
    );
  }
}

/// One cell of the month-wise ledger table.
class _LedgerCell extends StatelessWidget {
  const _LedgerCell(this.text, {required this.flex, this.bold = false, this.end = false, this.colour});

  final String text;
  final int flex;
  final bool bold;
  final bool end;
  final Color? colour;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      flex: flex,
      child: Text(
        text,
        style: Theme.of(
          context,
        ).textTheme.labelSmall?.copyWith(fontWeight: bold ? FontWeight.bold : FontWeight.normal, color: colour),
        maxLines: 1,
        overflow: TextOverflow.clip,
        softWrap: false,
        textAlign: end ? TextAlign.end : TextAlign.start,
      ),
    );
  }
}

// ------------------------------------------------------- 3. expense journal

class ExpenseJournalView extends StatelessWidget {
  const ExpenseJournalView({super.key, required this.language, required this.canEdit});

  final AppLanguage language;
  final bool canEdit;

  void _openExpenseDialog(BuildContext context, ExpenseRecord? editing) {
    final viewModel = context.read<SocietyViewModel>();
    showAppDialog<void>(
      context: context,
      builder: (_) => AddExpenseDialog(
        language: language,
        existing: editing,
        onSubmit: (title, cat, amount, date, vendor, phone, mode, notes) {
          if (editing == null) {
            viewModel.addExpense(
              title,
              cat,
              amount,
              date,
              vendor,
              phone,
              mode,
              notes,
              onResult: (ok, message) {
                if (!ok) viewModel.showMessage(message);
              },
            );
          } else {
            viewModel.updateExpense(
              editing,
              title,
              cat,
              amount,
              date,
              vendor,
              phone,
              mode,
              notes,
              onResult: (_, message) {
                viewModel.showMessage(message);
              },
            );
          }
        },
      ),
    );
  }

  void _confirmDelete(BuildContext context, ExpenseRecord expense) {
    final viewModel = context.read<SocietyViewModel>();
    showAppDialog<void>(
      context: context,
      builder: (dialogContext) {
        final textTheme = Theme.of(dialogContext).textTheme;
        return AlertDialog(
          insetPadding: dialogInsetPadding(dialogContext),
          title: Text(
            language == AppLanguage.marathi ? 'खर्च हटवायचा?' : 'Delete this expense?',
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          content: Text(
            '${expense.title} • ${rupees(expense.amount)}'
            '\n\nThe amount goes back into the '
            '${expense.paymentMode == PaymentMode.CASH ? 'cash' : 'bank'}'
            ' balance.',
            style: textTheme.bodySmall,
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(dialogContext).pop(), child: Text(tr('cancel', language))),
            AppButton.filled(
              key: const Key('confirm_delete_expense_btn'),
              style: FilledButton.styleFrom(backgroundColor: AppColors.statusOverdue),
              onPressed: () {
                viewModel.deleteExpense(expense, onResult: (_, message) => viewModel.showMessage(message));
                Navigator.of(dialogContext).pop();
              },
              child: Text(language == AppLanguage.marathi ? 'हटवा' : 'Delete'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<SocietyViewModel>();
    final expenses = viewModel.expenses;
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final onSurfaceVariant = theme.colorScheme.onSurfaceVariant;

    final header = Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Flexible(
          child: Text(
            tr('expense_journal', language),
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
        ),
        const SizedBox(width: 8),
        if (canEdit)
          AppButton.filledIcon(
            key: const Key('add_expense_modal_btn'),
            onPressed: () => _openExpenseDialog(context, null),
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.statusOverdue,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            icon: const Icon(Icons.add, size: 18),
            label: Text(tr('record_expense', language), style: textTheme.labelMedium?.copyWith(color: Colors.white)),
          )
        else
          _ViewOnlyChip(language: language),
      ],
    );

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: expenses.length + 1,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: appEntrance((context, index) {
        if (index == 0) return header;
        final expense = expenses[index - 1];
        return _OutlinedCard(
          radius: 14,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            expense.title,
                            style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          Text(
                            '${language == AppLanguage.marathi ? expense.category.labelMr : expense.category.labelEn} • ${expense.date}',
                            style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                    Text(
                      rupees(expense.amount),
                      style: textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: AppColors.statusOverdue,
                      ),
                    ),
                  ],
                ),
                const Divider(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text(
                        'Vendor: ${expense.vendorName} (${expense.vendorContact.trim().isEmpty ? 'Direct' : expense.vendorContact})',
                        style: textTheme.bodySmall,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'Mode: ${expense.paymentMode.name}',
                      style: textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary),
                    ),
                  ],
                ),
                if (expense.notes.trim().isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text('Notes: ${expense.notes}', style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant)),
                ],
                // A voucher entered wrong can be corrected or withdrawn -
                // both put the treasury back where it should be.
                if (canEdit)
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      TextButton.icon(
                        key: Key('edit_expense_btn_${expense.receiptNumber}'),
                        onPressed: () => _openExpenseDialog(context, expense),
                        icon: const Icon(Icons.edit, color: AppColors.tealPrimary, size: 16),
                        label: Text(
                          tr('edit_profile', language),
                          style: textTheme.labelMedium?.copyWith(color: AppColors.tealPrimary),
                        ),
                      ),
                      TextButton.icon(
                        key: Key('delete_expense_btn_${expense.receiptNumber}'),
                        onPressed: () => _confirmDelete(context, expense),
                        icon: const Icon(Icons.delete, color: AppColors.statusOverdue, size: 16),
                        label: Text(
                          language == AppLanguage.marathi ? 'हटवा' : 'Delete',
                          style: textTheme.labelMedium?.copyWith(color: AppColors.statusOverdue),
                        ),
                      ),
                    ],
                  ),
              ],
            ),
          ),
        );
      }),
    );
  }
}

// ------------------------------------------------------- 4. non-fee revenue

class NonFeeRevenueView extends StatelessWidget {
  const NonFeeRevenueView({super.key, required this.language, required this.canEdit});

  final AppLanguage language;
  final bool canEdit;

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<SocietyViewModel>();
    final revenues = viewModel.revenues;
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;

    final header = Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Flexible(
          child: Text(
            tr('non_fee_revenue', language),
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
        ),
        const SizedBox(width: 8),
        if (canEdit)
          AppButton.filledIcon(
            key: const Key('add_non_fee_modal_btn'),
            onPressed: () {
              showAppDialog<void>(
                context: context,
                builder: (_) => AddNonFeeRevenueDialog(
                  language: language,
                  onSubmit: (title, cat, amount, date, payer, flat, mode, notes) =>
                      viewModel.addNonFeeRevenue(title, cat, amount, date, payer, flat, mode, notes),
                ),
              );
            },
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.tealPrimary,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            icon: const Icon(Icons.add, size: 18),
            label: Text(tr('record_non_fee', language), style: textTheme.labelMedium?.copyWith(color: Colors.white)),
          )
        else
          _ViewOnlyChip(language: language),
      ],
    );

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: revenues.length + 1,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: appEntrance((context, index) {
        if (index == 0) return header;
        final revenue = revenues[index - 1];
        return _OutlinedCard(
          radius: 14,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(revenue.title, style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                          Text(
                            '${language == AppLanguage.marathi ? revenue.sourceCategory.labelMr : revenue.sourceCategory.labelEn} • ${revenue.date}',
                            style: textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                    Text(
                      rupees(revenue.amount),
                      style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold, color: AppColors.statusPaid),
                    ),
                  ],
                ),
                const Divider(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(child: Text('Payer: ${revenue.payerName}', style: textTheme.bodySmall)),
                    const SizedBox(width: 8),
                    Text(
                      '${revenue.paymentMode.name} • ${revenue.receiptNumber}',
                      style: textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      }),
    );
  }
}

// ------------------------------------------------------- add expense dialog

typedef ExpenseSubmit =
    void Function(
      String title,
      ExpenseCategory category,
      double amount,
      String date,
      String vendor,
      String phone,
      PaymentMode mode,
      String notes,
    );

/// Record Expense.
///
/// Category comes first because it is what decides the rest of the form; the
/// title is optional and falls back to the category name. The society's own cash
/// and bank balances are shown against the amount, and an expense larger than
/// what it holds cannot be submitted at all.
///
/// Closes itself after calling [onSubmit].
class AddExpenseDialog extends StatefulWidget {
  const AddExpenseDialog({super.key, required this.language, this.existing, required this.onSubmit});

  final AppLanguage language;

  /// The expense being edited, or null when recording a new one.
  final ExpenseRecord? existing;
  final ExpenseSubmit onSubmit;

  @override
  State<AddExpenseDialog> createState() => _AddExpenseDialogState();
}

class _AddExpenseDialogState extends State<AddExpenseDialog> {
  late ExpenseCategory _selectedCategory = widget.existing?.category ?? ExpenseCategory.WATER_TANKER;
  late final TextEditingController _title = TextEditingController(text: widget.existing?.title ?? '');
  late final TextEditingController _amount = TextEditingController(
    text: (widget.existing != null && widget.existing!.amount > 0) ? widget.existing!.amount.toInt().toString() : '',
  );
  late final TextEditingController _vendor = TextEditingController(text: widget.existing?.vendorName ?? '');
  late final TextEditingController _phone = TextEditingController(text: widget.existing?.vendorContact ?? '');
  late final TextEditingController _notes = TextEditingController(text: widget.existing?.notes ?? '');
  late PaymentMode _selectedMode = widget.existing?.paymentMode ?? PaymentMode.UPI;
  String? _error;

  @override
  void dispose() {
    _title.dispose();
    _amount.dispose();
    _vendor.dispose();
    _phone.dispose();
    _notes.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final financial = context.watch<SocietyViewModel>().financialOverview;
    final availableBank = financial.bankBalance;
    final availableCash = financial.cashBalance;
    final language = widget.language;
    final existing = widget.existing;
    final isMr = language == AppLanguage.marathi;
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;

    // Editing an expense that is already recorded: the money it took out of the
    // treasury is available to it again, so an edit is not blocked by its own
    // spending.
    final totalAvailable = availableBank + availableCash + (existing?.amount ?? 0.0);
    final amount = double.tryParse(_amount.text) ?? 0.0;
    final isOverBudget = amount > totalAvailable;

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        existing == null ? tr('record_expense', language) : tr('edit_expense', language),
        style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // What the society can actually spend, before anything is typed.
              Card(
                margin: EdgeInsets.zero,
                elevation: 0,
                color: AppColors.surfaceVariant,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                child: Padding(
                  padding: const EdgeInsets.all(10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        tr('available_balance', language),
                        style: textTheme.labelMedium?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      Text(
                        'Bank ${rupees(availableBank)}  •  Cash ${rupees(availableCash)}',
                        style: textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                      Text(
                        'Total ${rupees(totalAvailable)}',
                        key: const Key('expense_available_balance'),
                        style: textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: AppColors.tealPrimary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),

              // 1. Category
              DropdownButtonFormField<ExpenseCategory>(
                key: const Key('expense_category_dropdown'),
                initialValue: _selectedCategory,
                isExpanded: true,
                decoration: _outlined('Category (वर्गवारी)'),
                items: [
                  for (final cat in ExpenseCategory.values)
                    DropdownMenuItem(
                      value: cat,
                      child: Text(isMr ? cat.labelMr : cat.labelEn, overflow: TextOverflow.ellipsis),
                    ),
                ],
                onChanged: (cat) {
                  if (cat != null) setState(() => _selectedCategory = cat);
                },
              ),
              const SizedBox(height: 10),

              // 2. Title - optional, defaults to the category name.
              TextField(
                key: const Key('expense_title_input'),
                controller: _title,
                maxLines: 1,
                decoration: _outlined('Expense Title (optional)', hint: _selectedCategory.labelEn),
              ),
              const SizedBox(height: 10),

              TextField(
                key: const Key('expense_amount_input'),
                controller: _amount,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
                onChanged: (_) => setState(() => _error = null),
                decoration: _outlined(
                  tr('amount', language),
                  errorText: isOverBudget ? tr('insufficient_balance', language) : null,
                ),
              ),
              const SizedBox(height: 10),

              DropdownButtonFormField<PaymentMode>(
                initialValue: _selectedMode,
                isExpanded: true,
                decoration: _outlined(tr('payment_mode', language)),
                items: [
                  for (final mode in PaymentMode.values)
                    DropdownMenuItem(
                      value: mode,
                      child: Text(isMr ? mode.labelMr : mode.labelEn, overflow: TextOverflow.ellipsis),
                    ),
                ],
                onChanged: (mode) {
                  if (mode != null) setState(() => _selectedMode = mode);
                },
              ),
              const SizedBox(height: 10),

              TextField(controller: _vendor, maxLines: 1, decoration: _outlined(tr('vendor_name', language))),
              const SizedBox(height: 10),

              TextField(
                controller: _phone,
                maxLines: 1,
                keyboardType: TextInputType.phone,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(10)],
                decoration: _outlined('Vendor Contact'),
              ),
              const SizedBox(height: 10),

              TextField(controller: _notes, minLines: 1, maxLines: null, decoration: _outlined(tr('notes', language))),

              if (_error != null) ...[
                const SizedBox(height: 10),
                Text(
                  _error!,
                  key: const Key('expense_error_text'),
                  style: textTheme.bodySmall?.copyWith(color: theme.colorScheme.error),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: Text(tr('cancel', language))),
        AppButton.filled(
          key: const Key('submit_expense_btn'),
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          // Nothing can be submitted that the society cannot pay for.
          onPressed: (amount > 0 && !isOverBudget)
              ? () {
                  if (amount <= 0) {
                    setState(() => _error = 'Enter an amount greater than zero.');
                    return;
                  }
                  if (amount > totalAvailable) {
                    setState(() => _error = tr('insufficient_balance', language));
                    return;
                  }
                  widget.onSubmit(
                    _title.text.trim().isEmpty ? _selectedCategory.labelEn : _title.text,
                    _selectedCategory,
                    amount,
                    existing?.date ?? '',
                    _vendor.text,
                    _phone.text,
                    _selectedMode,
                    _notes.text,
                  );
                  Navigator.of(context).pop();
                }
              : null,
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}

// ------------------------------------------------------- add non-fee revenue dialog

typedef RevenueSubmit =
    void Function(
      String title,
      RevenueSource category,
      double amount,
      String date,
      String payer,
      String flatNumber,
      PaymentMode mode,
      String notes,
    );

/// Closes itself after calling [onSubmit].
class AddNonFeeRevenueDialog extends StatefulWidget {
  const AddNonFeeRevenueDialog({super.key, required this.language, required this.onSubmit});

  final AppLanguage language;
  final RevenueSubmit onSubmit;

  @override
  State<AddNonFeeRevenueDialog> createState() => _AddNonFeeRevenueDialogState();
}

class _AddNonFeeRevenueDialogState extends State<AddNonFeeRevenueDialog> {
  final _title = TextEditingController();
  RevenueSource _selectedCategory = RevenueSource.CLUBHOUSE_BOOKING;
  final _amount = TextEditingController();
  final _payer = TextEditingController();
  final _flatNumber = TextEditingController();
  // The form has no mode picker; revenue is recorded as UPI.
  final PaymentMode _selectedMode = PaymentMode.UPI;
  final _notes = TextEditingController();

  @override
  void dispose() {
    _title.dispose();
    _amount.dispose();
    _payer.dispose();
    _flatNumber.dispose();
    _notes.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final language = widget.language;
    final isMr = language == AppLanguage.marathi;
    final textTheme = Theme.of(context).textTheme;

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(tr('record_non_fee', language), style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(
                controller: _title,
                decoration: _outlined('Revenue Title (तपशील)', hint: 'e.g. Clubhouse birthday booking'),
              ),
              const SizedBox(height: 10),

              // Category Selector
              DropdownButtonFormField<RevenueSource>(
                initialValue: _selectedCategory,
                isExpanded: true,
                decoration: _outlined('Source (स्त्रोत)'),
                items: [
                  for (final cat in RevenueSource.values)
                    DropdownMenuItem(
                      value: cat,
                      child: Text(isMr ? cat.labelMr : cat.labelEn, overflow: TextOverflow.ellipsis),
                    ),
                ],
                onChanged: (cat) {
                  if (cat != null) setState(() => _selectedCategory = cat);
                },
              ),
              const SizedBox(height: 10),

              TextField(
                controller: _amount,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: _outlined(tr('amount', language)),
              ),
              const SizedBox(height: 10),

              TextField(controller: _payer, decoration: _outlined('Payer Name (व्यक्तीचे नाव)')),
              const SizedBox(height: 10),

              TextField(controller: _flatNumber, decoration: _outlined(tr('unit_number', language))),
              const SizedBox(height: 10),

              TextField(controller: _notes, minLines: 1, maxLines: null, decoration: _outlined(tr('notes', language))),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: Text(tr('cancel', language))),
        AppButton.filled(
          key: const Key('submit_non_fee_btn'),
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: () {
            final amount = double.tryParse(_amount.text.trim()) ?? 0.0;
            if (_title.text.trim().isNotEmpty && amount > 0) {
              widget.onSubmit(
                _title.text,
                _selectedCategory,
                amount,
                '',
                _payer.text,
                _flatNumber.text,
                _selectedMode,
                _notes.text,
              );
              Navigator.of(context).pop();
            }
          },
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}

/// Badge shown where a write button would be, for read-only viewers.
class _ViewOnlyChip extends StatelessWidget {
  const _ViewOnlyChip({required this.language});

  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      key: const Key('accounts_view_only_chip'),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppColors.surfaceVariant,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.borderLight),
      ),
      child: Text(
        tr('view_only', language),
        style: theme.textTheme.labelMedium?.copyWith(
          fontWeight: FontWeight.bold,
          color: theme.colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }
}
