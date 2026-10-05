// Port of ui/screens/MaintenanceScreen.kt.
//
// The Bills tab: maintenance billing & payment logging, the receipt list,
// tariff configuration, the maintenance charge split, late fee rules and a
// read-only member directory, plus the dialogs they open.
// ignore_for_file: constant_identifier_names

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../data/models.dart';
import '../../data/payment_allocation.dart';
import '../../l10n/app_strings.dart';
import '../../state/society_view_model.dart';
import '../../util/format.dart';
import '../../util/image_blob.dart';
import '../components/common_components.dart';
import '../theme/app_colors.dart';

// Kotlin theme aliases used by this screen.
const Color _tealPrimaryContainer = AppColors.tealContainer;
const Color _slateOutlineVariant = AppColors.borderLight;
const Color _slateSurfaceVariant = AppColors.surfaceVariant;

bool _isMr(AppLanguage language) => language == AppLanguage.marathi;

bool _containsIgnoreCase(String haystack, String needle) =>
    haystack.toLowerCase().contains(needle.toLowerCase());

bool _equalsIgnoreCase(String a, String b) => a.toLowerCase() == b.toLowerCase();

/// Kotlin "%.2f%%".
String _pct(double value) => '${plain2(value)}%';

double? _toDoubleOrNull(String raw) => double.tryParse(raw);

final List<TextInputFormatter> _amountFormatters = [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))];
final List<TextInputFormatter> _digitFormatters = [FilteringTextInputFormatter.digitsOnly];

ButtonStyle _tealButtonStyle({double radius = 20, EdgeInsetsGeometry? padding, Size? minimumSize}) =>
    FilledButton.styleFrom(
      backgroundColor: AppColors.tealPrimary,
      foregroundColor: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radius)),
      padding: padding,
      minimumSize: minimumSize,
      tapTargetSize: minimumSize != null ? MaterialTapTargetSize.shrinkWrap : null,
    );

ButtonStyle _dangerButtonStyle() => FilledButton.styleFrom(
      backgroundColor: AppColors.statusOverdue,
      foregroundColor: Colors.white,
    );

/// A Card with a thin outline, the way every list row on this screen is drawn.
Widget _outlinedCard(
  BuildContext context, {
  Key? key,
  double radius = 14,
  Color? color,
  bool bordered = true,
  required Widget child,
}) {
  return Card(
    key: key,
    margin: EdgeInsets.zero,
    elevation: 0,
    color: color ?? Theme.of(context).colorScheme.surface,
    shape: RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(radius),
      side: bordered ? const BorderSide(color: _slateOutlineVariant) : BorderSide.none,
    ),
    child: child,
  );
}

/// Dialog content that takes the dialog's full width and scrolls.
Widget _dialogBody({required List<Widget> children, double spacing = 8, double? maxHeight}) {
  Widget body = SingleChildScrollView(
    child: Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: spacing,
      children: children,
    ),
  );
  if (maxHeight != null) {
    body = ConstrainedBox(constraints: BoxConstraints(maxHeight: maxHeight), child: body);
  }
  return SizedBox(width: double.maxFinite, child: body);
}

TextStyle? _titleBold(BuildContext context) =>
    Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold);

// ============================================================================
// Screen
// ============================================================================

class MaintenanceScreen extends StatefulWidget {
  const MaintenanceScreen({super.key});

  @override
  State<MaintenanceScreen> createState() => _MaintenanceScreenState();
}

class _MaintenanceScreenState extends State<MaintenanceScreen> with SingleTickerProviderStateMixin {
  late final TabController _tabs = TabController(length: 6, vsync: this);
  int _selectedSubTab = 0;

  @override
  void initState() {
    super.initState();
    _tabs.addListener(() {
      if (_tabs.index != _selectedSubTab) setState(() => _selectedSubTab = _tabs.index);
    });
  }

  @override
  void dispose() {
    _tabs.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final role = vm.currentUser?.role;
    final canEditCharges = role == UserRole.CHAIRMAN || role == UserRole.TREASURER;
    final subTabs = [
      tr('nav_maintenance', language),
      tr('receipt_list', language),
      tr('tariff_configuration', language),
      tr('charge_setup', language),
      tr('late_fee_rules', language),
      tr('member_directory', language),
    ];
    final labelStyle = Theme.of(context).textTheme.labelLarge;

    final Widget body = switch (_selectedSubTab) {
      0 => const MaintenanceBillingView(),
      1 => ReceiptListView(canEdit: canEditCharges),
      2 => const TariffConfigurationView(),
      3 => MaintenanceChargeSetupView(canEdit: canEditCharges),
      4 => const LateFeeRulesView(),
      _ => const ResidentDirectoryView(),
    };

    return ColoredBox(
      color: AppColors.surfaceBg,
      child: Column(
        children: [
          Material(
            color: Theme.of(context).colorScheme.surface,
            child: TabBar(
              controller: _tabs,
              isScrollable: true,
              tabAlignment: TabAlignment.start,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              labelColor: AppColors.tealPrimary,
              unselectedLabelColor: AppColors.tealPrimary,
              indicatorColor: AppColors.tealPrimary,
              labelStyle: labelStyle?.copyWith(fontWeight: FontWeight.bold),
              unselectedLabelStyle: labelStyle?.copyWith(fontWeight: FontWeight.w500),
              tabs: [
                for (var i = 0; i < subTabs.length; i++)
                  Tab(key: Key('maint_subtab_$i'), child: Text(subTabs[i], maxLines: 1)),
              ],
            ),
          ),
          Expanded(child: body),
        ],
      ),
    );
  }
}

// ============================================================================
// 1. Maintenance Billing & Payment Logger View
// ============================================================================

class MaintenanceBillingView extends StatefulWidget {
  const MaintenanceBillingView({super.key});

  @override
  State<MaintenanceBillingView> createState() => _MaintenanceBillingViewState();
}

class _MaintenanceBillingViewState extends State<MaintenanceBillingView> {
  // Bills open on what still needs collecting.
  PaymentStatus _statusFilter = PaymentStatus.UNPAID;
  final TextEditingController _search = TextEditingController();

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  void _openEditBill(SocietyViewModel vm, MaintenanceBill bill) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => EditBillDialog(
        bill: bill,
        society: vm.societyInfo,
        language: vm.language,
        onDismiss: () => Navigator.of(dialogContext).pop(),
        onSave: (month, base, water, sinking, repair, lateFee, dueDate, onError) {
          vm.updateBill(bill, month, base, water, sinking, repair, lateFee, dueDate, onResult: (ok, message) {
            if (ok) {
              vm.showMessage(message);
              if (dialogContext.mounted) Navigator.of(dialogContext).pop();
            } else {
              onError(message);
            }
          });
        },
      ),
    );
  }

  void _confirmDeleteBill(SocietyViewModel vm, MaintenanceBill bill) {
    final language = vm.language;
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        insetPadding: dialogInsetPadding(dialogContext),
        title: Text(
          _isMr(language) ? 'बिल हटवायचे?' : 'Delete this bill?',
          style: _titleBold(dialogContext),
        ),
        content: Text(
          '${bill.flatNumber} • ${bill.billMonth} • ${rupees(bill.payableTotal)}'
          '${bill.amountPaid > 0 ? '\n\n${rupees(bill.amountPaid)} has already been collected against this bill.' : ''}',
          style: Theme.of(dialogContext).textTheme.bodySmall,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(tr('cancel', language)),
          ),
          FilledButton(
            key: const Key('confirm_delete_bill_btn'),
            style: _dangerButtonStyle(),
            onPressed: () {
              vm.deleteBill(bill, onResult: (_, message) => vm.showMessage(message));
              Navigator.of(dialogContext).pop();
            },
            child: Text(_isMr(language) ? 'हटवा' : 'Delete'),
          ),
        ],
      ),
    );
  }

  void _openRecordPayment(SocietyViewModel vm, MaintenanceBill bill) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => RecordPaymentDialog(
        bill: bill,
        language: vm.language,
        onDismiss: () => Navigator.of(dialogContext).pop(),
        onSubmit: (amount, mode, ref, date, screenshot) {
          vm.recordMaintenancePayment(bill, amount, mode, ref, date, screenshotBase64: screenshot);
          Navigator.of(dialogContext).pop();
        },
      ),
    );
  }

  void _openGenerateBill() {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => GenerateBillDialog(onDismiss: () => Navigator.of(dialogContext).pop()),
    );
  }

  void _openBulkCollect() {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => BulkCollectionDialog(onDismiss: () => Navigator.of(dialogContext).pop()),
    );
  }

  void _confirmBulkBills(SocietyViewModel vm, List<MissingBill> outstandingRun) {
    final language = vm.language;
    final months = outstandingRun.map((m) => m.month).toSet().length;
    final units = outstandingRun.map((m) => m.unit.flatNumber).toSet().length;
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        insetPadding: dialogInsetPadding(dialogContext),
        title: Text(
          _isMr(language) ? 'सर्व युनिट्सची बिले तयार करायची?' : 'Generate bills for all units?',
          style: _titleBold(dialogContext),
        ),
        content: Text(
          '${outstandingRun.length} bill(s) will be raised - $units unit(s) '
          'across $months month(s) that have no bill yet.\n\n'
          "Each one uses the unit's own tariff, split by the society's "
          'charge setup. Bills that already exist are left alone.',
          style: Theme.of(dialogContext).textTheme.bodySmall,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(tr('cancel', language)),
          ),
          FilledButton(
            key: const Key('confirm_bulk_bills_btn'),
            style: _tealButtonStyle(),
            onPressed: () {
              Navigator.of(dialogContext).pop();
              vm.generateMissingBills(onResult: (_, message) => vm.showMessage(message));
            },
            child: Text(_isMr(language) ? 'तयार करा' : 'Generate'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final society = vm.societyInfo;
    final bills = vm.bills;
    final role = vm.currentUser?.role;
    final canEdit = role == UserRole.CHAIRMAN || role == UserRole.TREASURER;
    final textTheme = Theme.of(context).textTheme;
    final onSurfaceVariant = Theme.of(context).colorScheme.onSurfaceVariant;
    final searchQuery = _search.text;

    /*
     * The same search the Receipts list has, because a committee looking for
     * one unit's bill among several hundred was left scrolling for it. Unit
     * number, member name, month and bill number are all matched, so whichever
     * of them the chairman has to hand will find the bill.
     */
    final term = searchQuery.trim();
    final filteredBills = bills.where((bill) {
      return bill.derivedStatus == _statusFilter &&
          (term.isEmpty ||
              _containsIgnoreCase(bill.flatNumber, term) ||
              _containsIgnoreCase(bill.residentName, term) ||
              _containsIgnoreCase(bill.billMonth, term) ||
              _containsIgnoreCase(bill.receiptNumber, term));
    }).toList();

    /*
     * How much catching up there is: every billable unit crossed with every
     * month from the society's billing start that it has no bill for. Recomputed
     * whenever the bills or the members move, so it disappears once the run is
     * done.
     */
    final outstandingRun = vm.pendingBillRun();
    final isReadyToBill =
        (society?.hasBalancedMaintenancePercents ?? false) && (society?.billingStartMonth.trim().isNotEmpty ?? false);

    final header = <Widget>[
      BillsFilterAndActionsRow(
        statusFilter: _statusFilter,
        onSelectStatus: (s) => setState(() => _statusFilter = s),
        canEdit: canEdit,
        language: language,
        onBulkCollect: _openBulkCollect,
        onGenerateBill: _openGenerateBill,
      ),
      TextField(
        key: const Key('bill_search_input'),
        controller: _search,
        onChanged: (_) => setState(() {}),
        maxLines: 1,
        decoration: InputDecoration(
          labelText: _isMr(language)
              ? 'युनिट, सभासद, महिना किंवा बिल क्रमांकाने शोधा'
              : 'Search by unit, member, month or bill no.',
          prefixIcon: const Icon(Icons.search, color: AppColors.tealPrimary),
          suffixIcon: searchQuery.trim().isEmpty
              ? null
              : IconButton(
                  key: const Key('bill_search_clear_btn'),
                  tooltip: 'Clear search',
                  icon: const Icon(Icons.close),
                  onPressed: () => setState(_search.clear),
                ),
        ),
      ),
      Text(
        '${filteredBills.length} bill(s) • ${rupees(filteredBills.fold(0.0, (s, b) => s + b.pendingAmount))} pending',
        key: const Key('bill_search_summary'),
        style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
      ),
      if (filteredBills.isEmpty && searchQuery.trim().isNotEmpty)
        Text(
          _isMr(language) ? '"$searchQuery" साठी कोणतेही बिल सापडले नाही.' : 'No bills match "$searchQuery".',
          key: const Key('bill_search_empty'),
          style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
        ),
      // Catch-up banner: only worth showing once the society can actually
      // bill, and only while something is still unbilled.
      if (canEdit && isReadyToBill && outstandingRun.isNotEmpty)
        _outlinedCard(
          context,
          key: const Key('bulk_bill_card'),
          color: _tealPrimaryContainer,
          bordered: false,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  _isMr(language)
                      ? '${outstandingRun.length} बिले अजून तयार व्हायची आहेत'
                      : '${outstandingRun.length} bill(s) still to be raised',
                  style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
                ),
                Text(
                  '${outstandingRun.map((m) => m.unit.flatNumber).toSet().length} unit(s) • '
                  '${outstandingRun.map((m) => m.month).toSet().length} month(s) pending',
                  style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
                ),
                const SizedBox(height: 10),
                FilledButton.icon(
                  key: const Key('bulk_generate_bills_btn'),
                  style: _tealButtonStyle(radius: 8),
                  onPressed: () => _confirmBulkBills(vm, outstandingRun),
                  icon: const Icon(Icons.add, size: 16),
                  label: Text(
                    _isMr(language) ? 'सर्व युनिट्सची बिले तयार करा' : 'Generate Bills for All Units',
                    style: textTheme.labelMedium?.copyWith(color: Colors.white),
                  ),
                ),
              ],
            ),
          ),
        ),
    ];

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: header.length + filteredBills.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        if (index < header.length) return header[index];
        return _billCard(context, vm, filteredBills[index - header.length], canEdit);
      },
    );
  }

  Widget _billCard(BuildContext context, SocietyViewModel vm, MaintenanceBill bill, bool canEdit) {
    final language = vm.language;
    final textTheme = Theme.of(context).textTheme;
    final onSurfaceVariant = Theme.of(context).colorScheme.onSurfaceVariant;
    final status = bill.derivedStatus;

    return _outlinedCard(
      context,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${bill.flatNumber} • ${bill.residentName}',
                        style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      Text(
                        '${bill.billMonth} • Due: ${bill.dueDate}',
                        style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      rupees(bill.payableTotal),
                      style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    StatusBadge(status: status, language: language),
                  ],
                ),
              ],
            ),
            const Divider(height: 20),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Bill ${rupees0(bill.chargesSubtotal)} + Late ${rupees0(bill.lateFeeAmount)}',
                        style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
                      ),
                      Text(
                        'Paid ${rupees(bill.amountPaid)} • Pending ${rupees(bill.pendingAmount)}',
                        style: textTheme.bodySmall?.copyWith(
                          fontWeight: FontWeight.w600,
                          color: bill.pendingAmount <= 0.009 ? AppColors.statusPaid : AppColors.statusOverdue,
                        ),
                      ),
                    ],
                  ),
                ),
                Wrap(
                  spacing: 6,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    // An existing bill can be corrected - a wrong tariff,
                    // a missed water charge - without deleting it and
                    // starting again. Payments already taken stay put.
                    if (canEdit)
                      _SmallIconButton(
                        key: Key('edit_bill_btn_${bill.flatNumber}'),
                        icon: Icons.edit,
                        tooltip: 'Edit bill',
                        color: AppColors.tealPrimary,
                        onPressed: () => _openEditBill(vm, bill),
                      ),
                    // A settled bill is a receipted record. Undoing
                    // it goes through the receipt, not by deleting
                    // the bill out from under the payment.
                    if (canEdit && status != PaymentStatus.PAID)
                      _SmallIconButton(
                        key: Key('delete_bill_btn_${bill.flatNumber}'),
                        icon: Icons.delete,
                        tooltip: 'Delete bill',
                        color: AppColors.statusOverdue,
                        onPressed: () => _confirmDeleteBill(vm, bill),
                      ),
                    if (status != PaymentStatus.PAID)
                      // Compact: it sits beside the edit and delete
                      // icons, so it takes only the room it needs.
                      FilledButton.icon(
                        key: Key('collect_payment_btn_${bill.flatNumber}'),
                        style: _tealButtonStyle(
                          radius: 8,
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                          minimumSize: const Size(0, 32),
                        ),
                        onPressed: () => _openRecordPayment(vm, bill),
                        icon: const Icon(Icons.add, size: 14),
                        label: Text(tr('pay', language), style: textTheme.labelSmall?.copyWith(color: Colors.white)),
                      ),
                    if (bill.amountPaid > 0.0)
                      OutlinedButton.icon(
                        key: Key('view_receipt_btn_${bill.flatNumber}'),
                        style: OutlinedButton.styleFrom(
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                          side: const BorderSide(color: AppColors.tealPrimary),
                        ),
                        onPressed: () => showReceiptDialog(
                          context,
                          bill: bill,
                          society: vm.societyInfo,
                          language: language,
                        ),
                        icon: const Icon(Icons.receipt, color: AppColors.tealPrimary, size: 16),
                        label: Text(
                          _isMr(language) ? 'पावती पहा' : 'Receipt',
                          style: textTheme.labelSmall?.copyWith(color: AppColors.tealPrimary),
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _SmallIconButton extends StatelessWidget {
  const _SmallIconButton({
    super.key,
    required this.icon,
    required this.tooltip,
    required this.color,
    required this.onPressed,
  });

  final IconData icon;
  final String tooltip;
  final Color color;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 32,
      height: 32,
      child: IconButton(
        padding: EdgeInsets.zero,
        tooltip: tooltip,
        iconSize: 18,
        icon: Icon(icon, color: color),
        onPressed: onPressed,
      ),
    );
  }
}

/// The filter chips and the two actions that sit above the bill list.
///
/// These used to share one `SpaceBetween` row. Three chips and two buttons do
/// not fit across a 360dp phone, so the row overflowed. They get a line each
/// instead. The buttons stay right-aligned, their labels are held to one line
/// so no amount of squeezing can wrap them, and the chips scroll sideways if a
/// longer language or a fourth filter makes them wider than the screen.
class BillsFilterAndActionsRow extends StatelessWidget {
  const BillsFilterAndActionsRow({
    super.key,
    required this.statusFilter,
    required this.onSelectStatus,
    required this.canEdit,
    required this.language,
    required this.onBulkCollect,
    required this.onGenerateBill,
  });

  final PaymentStatus statusFilter;
  final ValueChanged<PaymentStatus> onSelectStatus;
  final bool canEdit;
  final AppLanguage language;
  final VoidCallback onBulkCollect;
  final VoidCallback onGenerateBill;

  @override
  Widget build(BuildContext context) {
    final labelSmall = Theme.of(context).textTheme.labelSmall;
    const buttonPadding = EdgeInsets.symmetric(horizontal: 12, vertical: 8);

    Widget chip(PaymentStatus status, String key, Color selectedColor, String tag) => FilterChip(
          key: Key(tag),
          selected: statusFilter == status,
          onSelected: (_) => onSelectStatus(status),
          label: Text(tr(key, language), maxLines: 1),
          selectedColor: selectedColor,
        );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 10,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          spacing: 8,
          children: [
            // One receipt for however many months it covers. Punching five
            // separate ones for a single payment was the committee's most
            // common piece of busywork.
            if (canEdit)
              OutlinedButton.icon(
                key: const Key('bulk_collect_btn'),
                style: OutlinedButton.styleFrom(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  side: const BorderSide(color: AppColors.tealPrimary),
                  padding: buttonPadding,
                ),
                onPressed: onBulkCollect,
                icon: const Icon(Icons.payments, color: AppColors.tealPrimary, size: 16),
                label: Text(
                  _isMr(language) ? 'एकत्रित पावती' : 'Bulk Receipt',
                  style: labelSmall?.copyWith(color: AppColors.tealPrimary),
                  maxLines: 1,
                  softWrap: false,
                ),
              ),
            FilledButton.icon(
              key: const Key('generate_bill_btn'),
              style: _tealButtonStyle(radius: 8, padding: buttonPadding),
              onPressed: onGenerateBill,
              icon: const Icon(Icons.add, size: 16),
              label: Text(
                _isMr(language) ? 'बिल तयार करा' : 'New Bill',
                style: labelSmall?.copyWith(color: Colors.white),
                maxLines: 1,
                softWrap: false,
              ),
            ),
          ],
        ),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            spacing: 8,
            children: [
              chip(PaymentStatus.UNPAID, 'status_unpaid', AppColors.statusOverdueBg, 'bills_filter_unpaid'),
              chip(PaymentStatus.PARTIAL, 'status_partial', AppColors.statusPendingBg, 'bills_filter_partial'),
              chip(PaymentStatus.PAID, 'status_paid', AppColors.statusPaidBg, 'bills_filter_paid'),
            ],
          ),
        ),
      ],
    );
  }
}

// ============================================================================
// Receipt list
// ============================================================================

/// Every receipt the society has issued.
///
/// A receipt lives on the bill it settled, so this is the bills that carry a
/// payment - receipt number, unit, member, date, amount, mode and the proof that
/// was attached. Correcting one restates that payment; deleting one puts the
/// bill back to unpaid and the money back on the member's dues.
class ReceiptListView extends StatefulWidget {
  const ReceiptListView({super.key, required this.canEdit});

  final bool canEdit;

  @override
  State<ReceiptListView> createState() => _ReceiptListViewState();
}

class _ReceiptListViewState extends State<ReceiptListView> {
  final TextEditingController _search = TextEditingController();

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  void _editReceipt(SocietyViewModel vm, MaintenanceBill receipt) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => RecordPaymentDialog(
        bill: receipt,
        language: vm.language,
        isEditingReceipt: true,
        onDismiss: () => Navigator.of(dialogContext).pop(),
        onSubmit: (amount, mode, ref, date, screenshot) {
          vm.updateRecordedPayment(receipt, amount, mode, ref, date, screenshot, onResult: (ok, message) {
            vm.showMessage(message);
            if (ok && dialogContext.mounted) Navigator.of(dialogContext).pop();
          });
        },
      ),
    );
  }

  void _confirmDelete(SocietyViewModel vm, MaintenanceBill receipt) {
    final language = vm.language;
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        insetPadding: dialogInsetPadding(dialogContext),
        title: Text(
          _isMr(language) ? 'पावती रद्द करायची?' : 'Delete this receipt?',
          style: _titleBold(dialogContext),
        ),
        content: Text(
          '${receipt.receiptNumber} • ${receipt.flatNumber} • ${rupees(receipt.amountPaid)}\n\n'
          'The payment is reversed: the bill for ${receipt.billMonth} goes back to '
          "unpaid and ${rupees(receipt.amountPaid)} returns to the member's dues.",
          style: Theme.of(dialogContext).textTheme.bodySmall,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(tr('cancel', language)),
          ),
          FilledButton(
            key: const Key('confirm_delete_receipt_btn'),
            style: _dangerButtonStyle(),
            onPressed: () {
              vm.deleteReceipt(receipt, onResult: (_, message) => vm.showMessage(message));
              Navigator.of(dialogContext).pop();
            },
            child: Text(_isMr(language) ? 'रद्द करा' : 'Delete'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final textTheme = Theme.of(context).textTheme;
    final onSurfaceVariant = Theme.of(context).colorScheme.onSurfaceVariant;
    final searchQuery = _search.text;

    final receipts = vm.bills
        .where((b) => b.amountPaid > 0.0)
        .where((r) =>
            searchQuery.trim().isEmpty ||
            _containsIgnoreCase(r.receiptNumber, searchQuery) ||
            _containsIgnoreCase(r.flatNumber, searchQuery) ||
            _containsIgnoreCase(r.residentName, searchQuery))
        .toList()
      ..sort((a, b) => billMonthKey(b.billMonth).compareTo(billMonthKey(a.billMonth)));

    final header = <Widget>[
      TextField(
        key: const Key('receipt_search_input'),
        controller: _search,
        onChanged: (_) => setState(() {}),
        maxLines: 1,
        decoration: const InputDecoration(labelText: 'Search by receipt no., unit or member'),
      ),
      Text(
        '${receipts.length} receipt(s) • ${rupees(receipts.fold(0.0, (s, r) => s + r.amountPaid))} collected',
        style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
      ),
      if (receipts.isEmpty)
        Text(
          _isMr(language) ? 'अजून एकही पावती तयार झालेली नाही.' : 'No receipts have been issued yet.',
          style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
        ),
    ];

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: header.length + receipts.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        if (index < header.length) return header[index];
        return _receiptCard(context, vm, receipts[index - header.length]);
      },
    );
  }

  Widget _receiptCard(BuildContext context, SocietyViewModel vm, MaintenanceBill receipt) {
    final language = vm.language;
    final textTheme = Theme.of(context).textTheme;
    final onSurfaceVariant = Theme.of(context).colorScheme.onSurfaceVariant;
    final hasProof = blobImageProvider(receipt.receiptImageBase64) != null;
    final mode = receipt.paymentMode;

    return _outlinedCard(
      context,
      key: Key('receipt_card_${receipt.receiptNumber}'),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        receipt.receiptNumber.trim().isEmpty ? '(no receipt number)' : receipt.receiptNumber,
                        style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      Text(
                        '${receipt.flatNumber} • ${receipt.residentName}',
                        style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      rupees(receipt.amountPaid),
                      style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold, color: AppColors.statusPaid),
                    ),
                    StatusBadge(status: receipt.derivedStatus, language: language),
                  ],
                ),
              ],
            ),
            const Divider(height: 16),
            _ReceiptField(label: 'Bill', value: '${receipt.billMonth} • ${rupees(receipt.payableTotal)}'),
            _ReceiptField(
              label: tr('payment_date', language),
              value: receipt.paidDate.trim().isEmpty ? '-' : receipt.paidDate,
            ),
            _ReceiptField(
              label: tr('payment_mode', language),
              value: mode == null ? '-' : (_isMr(language) ? mode.labelMr : mode.labelEn),
            ),
            _ReceiptField(
              label: 'Reference',
              value: receipt.transactionRef.trim().isEmpty ? '-' : receipt.transactionRef,
            ),
            _ReceiptField(
              label: 'Proof',
              value: receipt.receiptImageBase64.trim().isEmpty ? 'Not attached' : 'Screenshot attached',
            ),
            // The proof itself, so it can be checked without opening
            // anything - it is the whole point of having asked for it.
            if (hasProof) ...[
              const SizedBox(height: 8),
              ClipRRect(
                key: Key('receipt_proof_${receipt.receiptNumber}'),
                borderRadius: BorderRadius.circular(10),
                child: Semantics(
                  label: 'Payment screenshot',
                  child: BlobImage(
                    base64: receipt.receiptImageBase64,
                    width: double.infinity,
                    height: 160,
                    fit: BoxFit.contain,
                  ),
                ),
              ),
            ],
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Flexible(
                  child: Wrap(
                    alignment: WrapAlignment.end,
                    children: [
                      TextButton.icon(
                        onPressed: () => showReceiptDialog(
                          context,
                          bill: receipt,
                          society: vm.societyInfo,
                          language: language,
                        ),
                        icon: const Icon(Icons.receipt, color: AppColors.tealPrimary, size: 16),
                        label: Text(
                          'View',
                          style: textTheme.labelMedium?.copyWith(color: AppColors.tealPrimary),
                        ),
                      ),
                      if (widget.canEdit) ...[
                        TextButton.icon(
                          key: Key('edit_receipt_btn_${receipt.receiptNumber}'),
                          onPressed: () => _editReceipt(vm, receipt),
                          icon: const Icon(Icons.edit, color: AppColors.tealPrimary, size: 16),
                          label: Text(
                            tr('edit_profile', language),
                            style: textTheme.labelMedium?.copyWith(color: AppColors.tealPrimary),
                          ),
                        ),
                        TextButton.icon(
                          key: Key('delete_receipt_btn_${receipt.receiptNumber}'),
                          onPressed: () => _confirmDelete(vm, receipt),
                          icon: const Icon(Icons.delete, color: AppColors.statusOverdue, size: 16),
                          label: Text(
                            _isMr(language) ? 'रद्द' : 'Delete',
                            style: textTheme.labelMedium?.copyWith(color: AppColors.statusOverdue),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// One labelled line on a receipt card.
class _ReceiptField extends StatelessWidget {
  const _ReceiptField({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final bodySmall = Theme.of(context).textTheme.bodySmall;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant)),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              value,
              style: bodySmall?.copyWith(fontWeight: FontWeight.w600),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.end,
            ),
          ),
        ],
      ),
    );
  }
}

// ============================================================================
// 2. Tariff Configuration
// ============================================================================

/// Tariff Configuration.
///
/// Shows the society wing by wing: what each wing actually holds - its floors,
/// its units and the unit type they were created as - and the monthly tariff
/// charged for that type. Only unit types a wing actually uses are listed, so a
/// society of 1 BHKs is never asked to price a penthouse.
class TariffConfigurationView extends StatelessWidget {
  const TariffConfigurationView({super.key});

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final society = vm.societyInfo;

    // Unit types this society genuinely has: whatever its wings were built with,
    // plus anything else configured against a tariff.
    final fromWings = (society?.wingPlans ?? const <WingPlan>[])
        .expand((plan) => plan.unitCounts.map((c) => c.unitType))
        .where((t) => t.trim().isNotEmpty);
    final configured = society?.unitTypes ?? const <UnitTypeConfig>[];
    final names = <String>[];
    for (final n in [...fromWings, ...configured.map((t) => t.name)]) {
      if (n.trim().isNotEmpty && !names.contains(n)) names.add(n);
    }
    final liveTypes = [
      for (final name in names)
        configured.firstWhere(
          (t) => _equalsIgnoreCase(t.name, name),
          orElse: () => UnitTypeConfig(name: name, monthlyTariff: 0.0),
        ),
    ];

    // The edited amounts start again whenever the configured types change
    // (Kotlin: remember(liveTypes)).
    final signature = liveTypes.map((t) => '${t.name}=${t.monthlyTariff}').join('|');
    return _TariffEditor(key: ValueKey(signature), vm: vm, society: society, liveTypes: liveTypes);
  }
}

class _TariffEditor extends StatefulWidget {
  const _TariffEditor({super.key, required this.vm, required this.society, required this.liveTypes});

  final SocietyViewModel vm;
  final SocietyInfo? society;
  final List<UnitTypeConfig> liveTypes;

  @override
  State<_TariffEditor> createState() => _TariffEditorState();
}

class _TariffEditorState extends State<_TariffEditor> {
  // Edited amounts, keyed by unit type name.
  final Map<String, TextEditingController> _tariffs = {};

  @override
  void initState() {
    super.initState();
    for (final t in widget.liveTypes) {
      _tariffs[t.name] = TextEditingController(text: t.monthlyTariff > 0 ? t.monthlyTariff.toInt().toString() : '');
    }
  }

  @override
  void dispose() {
    for (final c in _tariffs.values) {
      c.dispose();
    }
    super.dispose();
  }

  TextEditingController _controllerFor(String name) => _tariffs.putIfAbsent(name, TextEditingController.new);

  @override
  Widget build(BuildContext context) {
    final vm = widget.vm;
    final language = vm.language;
    final society = widget.society;
    final textTheme = Theme.of(context).textTheme;
    final onSurfaceVariant = Theme.of(context).colorScheme.onSurfaceVariant;
    final residents = vm.members;
    final wingPlans = society?.wingPlans ?? const <WingPlan>[];

    // Any unit type the society charges for that no wing was built with.
    final extraTypes = widget.liveTypes
        .where((type) => !wingPlans.any(
              (plan) => plan.unitCounts.any((c) => _equalsIgnoreCase(c.unitType, type.name)),
            ))
        .toList();

    final children = <Widget>[
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            tr('tariff_configuration', language),
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          Text(
            _isMr(language)
                ? 'विंगनिहाय सदनिका व त्यांचे मासिक देखभाल दर.'
                : 'Units wing by wing, with the monthly maintenance charged for each type.',
            style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
          ),
        ],
      ),
      // One card per wing: what it holds and what its units are charged.
      for (final plan in wingPlans) _wingCard(context, plan, residents),
      if (extraTypes.isNotEmpty)
        _outlinedCard(
          context,
          radius: 16,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  _isMr(language) ? 'इतर सदनिका प्रकार' : 'Other Unit Types',
                  style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                for (final type in extraTypes)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: TextField(
                      key: Key('tariff_input_type_${type.name}'),
                      controller: _controllerFor(type.name),
                      keyboardType: TextInputType.number,
                      inputFormatters: _digitFormatters,
                      maxLines: 1,
                      decoration: InputDecoration(labelText: '${type.name} - Monthly Tariff (₹)'),
                    ),
                  ),
              ],
            ),
          ),
        ),
      if (wingPlans.isEmpty && widget.liveTypes.isEmpty)
        Text(
          'No wings are configured yet. Open the Society Profile and add the '
          'floor inventory to set tariffs wing by wing.',
          style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
        ),
      FilledButton.icon(
        key: const Key('save_tariffs_btn'),
        style: _tealButtonStyle(radius: 10),
        onPressed: () {
          vm.updateUnitTypeTariffs(
            [
              for (final type in widget.liveTypes)
                type.copyWith(
                  monthlyTariff: _toDoubleOrNull(_tariffs[type.name]?.text ?? '') ?? type.monthlyTariff,
                ),
            ],
            onResult: (_, message) => vm.showMessage(message),
          );
        },
        icon: const Icon(Icons.save),
        label: Text(tr('save', language)),
      ),
    ];

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: children.length,
      separatorBuilder: (_, _) => const SizedBox(height: 14),
      itemBuilder: (_, i) => children[i],
    );
  }

  Widget _wingCard(BuildContext context, WingPlan plan, List<Resident> residents) {
    final textTheme = Theme.of(context).textTheme;
    final onSurfaceVariant = Theme.of(context).colorScheme.onSurfaceVariant;
    final wingUnits = residents.where((r) => _equalsIgnoreCase(r.wing, plan.name)).toList();
    final countsText = plan.unitCounts.map((c) => '${c.unitType} × ${c.quantity}').join(', ');

    return _outlinedCard(
      context,
      key: Key('tariff_wing_card_${plan.name}'),
      radius: 16,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Wing ${plan.name}',
                        style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      Text(
                        '${plan.totalFloors} floors • '
                        '${countsText.trim().isEmpty ? 'no unit types set' : countsText}',
                        style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                _Pill(text: '${plan.totalUnits} units'),
              ],
            ),
            if (wingUnits.isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(
                '${wingUnits.where((u) => u.ownerName.trim().isNotEmpty).length} occupied • '
                '${wingUnits.where((u) => u.ownerName.trim().isEmpty).length} vacant',
                style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
              ),
            ],
            const Divider(height: 20),
            // One tariff per unit type this wing actually holds.
            for (final count in plan.unitCounts.where((c) => c.unitType.trim().isNotEmpty))
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: TextField(
                  key: Key('tariff_input_${plan.name}_${count.unitType}'),
                  controller: _controllerFor(count.unitType),
                  keyboardType: TextInputType.number,
                  inputFormatters: _digitFormatters,
                  maxLines: 1,
                  decoration: InputDecoration(
                    labelText: '${count.unitType} (${count.quantity} units) - ₹ / month',
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(color: _tealPrimaryContainer, borderRadius: BorderRadius.circular(8)),
      child: Text(
        text,
        style: Theme.of(context)
            .textTheme
            .labelSmall
            ?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
      ),
    );
  }
}

// ============================================================================
// Maintenance Charge Configuration
// ============================================================================

/// The amount the preview opens on when the society has no tariff set yet.
const String DEFAULT_PREVIEW_AMOUNT = '1100';

String _percentText(double? value) {
  if (value == null) return '';
  if (value == value.toInt().toDouble()) return value.toInt().toString();
  return plain2(value);
}

/// Maintenance Charge Configuration.
///
/// A month's maintenance is divided between the society's funds by percentage
/// rather than by charges added on top of it, so the four shares have to add up
/// to 100%. The preview below shows what those shares come to for a given
/// maintenance amount, and saving is refused until they balance.
class MaintenanceChargeSetupView extends StatelessWidget {
  const MaintenanceChargeSetupView({super.key, required this.canEdit});

  final bool canEdit;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final society = vm.societyInfo;
    // Fields start again from the saved values whenever the society changes
    // (Kotlin: remember(society)).
    final firstTariff = (society?.unitTypes ?? const <UnitTypeConfig>[])
        .where((t) => t.monthlyTariff > 0)
        .map((t) => t.monthlyTariff)
        .firstOrNull;
    final signature = society == null
        ? 'none'
        : '${society.waterChargesPercent}|${society.sinkingFundPercent}|${society.repairFundPercent}|'
            '${society.otherMaintenancePercent}|$firstTariff';
    return _ChargeSetupEditor(key: ValueKey(signature), vm: vm, society: society, canEdit: canEdit);
  }
}

class _ChargeSetupEditor extends StatefulWidget {
  const _ChargeSetupEditor({super.key, required this.vm, required this.society, required this.canEdit});

  final SocietyViewModel vm;
  final SocietyInfo? society;
  final bool canEdit;

  @override
  State<_ChargeSetupEditor> createState() => _ChargeSetupEditorState();
}

class _ChargeSetupEditorState extends State<_ChargeSetupEditor> {
  late final TextEditingController _water = TextEditingController(text: _percentText(widget.society?.waterChargesPercent));
  late final TextEditingController _sinking =
      TextEditingController(text: _percentText(widget.society?.sinkingFundPercent));
  late final TextEditingController _repair = TextEditingController(text: _percentText(widget.society?.repairFundPercent));
  late final TextEditingController _other =
      TextEditingController(text: _percentText(widget.society?.otherMaintenancePercent));

  // What the preview is worked out against: the society's own tariff when it
  // has one, so the numbers on screen are the ones it actually bills.
  late final TextEditingController _previewAmount = TextEditingController(
    text: (widget.society?.unitTypes ?? const <UnitTypeConfig>[])
            .where((t) => t.monthlyTariff > 0)
            .map((t) => t.monthlyTariff.toInt().toString())
            .firstOrNull ??
        DEFAULT_PREVIEW_AMOUNT,
  );
  String? _error;

  @override
  void dispose() {
    _water.dispose();
    _sinking.dispose();
    _repair.dispose();
    _other.dispose();
    _previewAmount.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final vm = widget.vm;
    final language = vm.language;
    final society = widget.society;
    final canEdit = widget.canEdit;
    final textTheme = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;

    final waterPct = _toDoubleOrNull(_water.text) ?? 0.0;
    final sinkingPct = _toDoubleOrNull(_sinking.text) ?? 0.0;
    final repairPct = _toDoubleOrNull(_repair.text) ?? 0.0;
    final otherPct = _toDoubleOrNull(_other.text) ?? 0.0;
    final percentTotal = waterPct + sinkingPct + repairPct + otherPct;
    final isBalanced = (percentTotal - 100.0).abs() < 0.01;

    // The preview follows what is typed, not what is saved, so the amounts move
    // as soon as a percentage changes.
    final previewValue = _toDoubleOrNull(_previewAmount.text) ?? 0.0;
    final preview = society == null
        ? const MaintenanceSplit(water: 0, sinking: 0, repair: 0, other: 0)
        : society
            .copyWith(
              waterChargesPercent: waterPct,
              sinkingFundPercent: sinkingPct,
              repairFundPercent: repairPct,
              otherMaintenancePercent: otherPct,
            )
            .splitMaintenance(previewValue);

    Widget percentField(String label, TextEditingController controller, String tag) => _PercentField(
          label: label,
          controller: controller,
          testTag: tag,
          enabled: canEdit,
          onChanged: () => setState(() => _error = null),
        );

    final boldMedium = textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        spacing: 14,
        children: [
          _outlinedCard(
            context,
            radius: 16,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(tr('charge_setup', language), style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                  Text(
                    _isMr(language)
                        ? 'देखभाल रक्कम विविध निधींमध्ये किती टक्के विभागायची.'
                        : 'What share of the maintenance amount goes to each fund.',
                    style: textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
                  ),
                  const Divider(height: 24),
                  percentField('Water Charges (%)', _water, 'charge_water_percent'),
                  const SizedBox(height: 10),
                  percentField('Sinking Fund (%)', _sinking, 'charge_sinking_percent'),
                  const SizedBox(height: 10),
                  percentField('Repair & Maintenance Fund (%)', _repair, 'charge_repair_percent'),
                  const SizedBox(height: 10),
                  percentField('Other / Common Maintenance (%)', _other, 'charge_other_percent'),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('Total', style: boldMedium),
                      Text(
                        _pct(percentTotal),
                        key: const Key('charge_percent_total'),
                        style: boldMedium?.copyWith(color: isBalanced ? AppColors.statusPaid : AppColors.statusOverdue),
                      ),
                    ],
                  ),
                  if (!isBalanced)
                    Text(
                      'The percentages must add up to 100%. They currently add up to ${_pct(percentTotal)}.',
                      key: const Key('charge_percent_error'),
                      style: textTheme.bodySmall?.copyWith(color: scheme.error),
                    ),
                ],
              ),
            ),
          ),

          // Live preview.
          _outlinedCard(
            context,
            key: const Key('charge_preview_card'),
            radius: 16,
            color: _slateSurfaceVariant,
            bordered: false,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    _isMr(language) ? 'उदाहरण (Preview)' : 'Live Preview',
                    style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    key: const Key('charge_preview_amount'),
                    controller: _previewAmount,
                    onChanged: (_) => setState(() {}),
                    keyboardType: TextInputType.number,
                    inputFormatters: _amountFormatters,
                    maxLines: 1,
                    decoration: const InputDecoration(labelText: 'Maintenance Amount (₹)'),
                  ),
                  const SizedBox(height: 10),
                  _ChargePreviewRow(label: 'Water Charges', percent: waterPct, amount: preview.water),
                  _ChargePreviewRow(label: 'Sinking Fund', percent: sinkingPct, amount: preview.sinking),
                  _ChargePreviewRow(label: 'Repair Fund', percent: repairPct, amount: preview.repair),
                  _ChargePreviewRow(
                    label: 'Other Maintenance',
                    percent: previewValue > 0 ? preview.other / previewValue * 100.0 : otherPct,
                    amount: preview.other,
                  ),
                  const Divider(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('Total', style: boldMedium),
                      Text(
                        rupees(preview.total),
                        key: const Key('charge_preview_total'),
                        style: boldMedium?.copyWith(color: AppColors.tealPrimary),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),

          if (_error != null)
            Text(
              _error!,
              key: const Key('charge_save_error'),
              style: textTheme.bodySmall?.copyWith(color: scheme.error),
            ),

          if (canEdit) ...[
            FilledButton.icon(
              key: const Key('save_charge_setup_btn'),
              style: _tealButtonStyle(radius: 10),
              onPressed: isBalanced
                  ? () {
                      vm.updateMaintenanceCharges(waterPct, sinkingPct, repairPct, otherPct, onResult: (ok, message) {
                        if (!mounted) return;
                        if (ok) {
                          setState(() => _error = null);
                          vm.showMessage(message);
                        } else {
                          setState(() => _error = message);
                        }
                      });
                    }
                  : null,
              icon: const Icon(Icons.save),
              label: Text(tr('save', language)),
            ),
            Text(
              _isMr(language)
                  ? 'हे फक्त यापुढे तयार होणाऱ्या बिलांना लागू होईल. आधीची बिले जशी आहेत तशीच राहतील.'
                  : 'This applies to bills raised from now on. Bills already generated are left as they are.',
              style: textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
            ),
          ],
        ],
      ),
    );
  }
}

class _PercentField extends StatelessWidget {
  const _PercentField({
    required this.label,
    required this.controller,
    required this.testTag,
    required this.enabled,
    required this.onChanged,
  });

  final String label;
  final TextEditingController controller;
  final String testTag;
  final bool enabled;
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context) {
    return TextField(
      key: Key(testTag),
      controller: controller,
      enabled: enabled,
      onChanged: (_) => onChanged(),
      keyboardType: TextInputType.number,
      inputFormatters: [..._amountFormatters, LengthLimitingTextInputFormatter(6)],
      maxLines: 1,
      decoration: InputDecoration(labelText: label),
    );
  }
}

/// One share of a bill's maintenance, as configured under Charge Setup.
class _SplitRow extends StatelessWidget {
  const _SplitRow({required this.label, required this.percent, required this.amount});

  final String label;
  final double percent;
  final double amount;

  @override
  Widget build(BuildContext context) {
    final bodySmall = Theme.of(context).textTheme.bodySmall;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            '$label (${_pct(percent)})',
            style: bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
          ),
          Text(rupees(amount), style: bodySmall?.copyWith(fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

/// "Water Charges: 10.00% -> ₹110.00"
class _ChargePreviewRow extends StatelessWidget {
  const _ChargePreviewRow({required this.label, required this.percent, required this.amount});

  final String label;
  final double percent;
  final double amount;

  @override
  Widget build(BuildContext context) {
    final bodySmall = Theme.of(context).textTheme.bodySmall;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            '$label: ${_pct(percent)}',
            style: bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
          ),
          Text(rupees(amount), style: bodySmall?.copyWith(fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

// ============================================================================
// 3. Late Fee Rules View
// ============================================================================

class LateFeeRulesView extends StatefulWidget {
  const LateFeeRulesView({super.key});

  @override
  State<LateFeeRulesView> createState() => _LateFeeRulesViewState();
}

class _LateFeeRulesViewState extends State<LateFeeRulesView> {
  late final TextEditingController _lateFeePercent;
  late final TextEditingController _graceDays;

  @override
  void initState() {
    super.initState();
    final society = context.read<SocietyViewModel>().societyInfo;
    _lateFeePercent = TextEditingController(text: (society?.lateFeePercentPerMonth ?? 2.0).toString());
    _graceDays = TextEditingController(text: (society?.lateFeeGraceDays ?? 10).toString());
  }

  @override
  void dispose() {
    _lateFeePercent.dispose();
    _graceDays.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final textTheme = Theme.of(context).textTheme;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: _outlinedCard(
        context,
        radius: 16,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(tr('late_fee_rules', language), style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
              Text(
                _isMr(language)
                    ? 'मुदतीनंतर थकीत बिलावर दंड व व्याजाची आकारणी.'
                    : 'Configure monthly overdue interest & grace periods.',
                style: textTheme.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
              ),
              const Divider(height: 24),
              TextField(
                controller: _lateFeePercent,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Interest Rate per Month (% प्रति महिना व्याज)'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _graceDays,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Grace Period Days (सवलतीचे दिवस)'),
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                key: const Key('save_late_fee_btn'),
                style: _tealButtonStyle(radius: 10),
                onPressed: () {
                  final pct = _toDoubleOrNull(_lateFeePercent.text) ?? 2.0;
                  final days = int.tryParse(_graceDays.text) ?? 10;
                  vm.updateLateFeeRules(pct, days);
                },
                icon: const Icon(Icons.save),
                label: Text(tr('save', language)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ============================================================================
// 4. Resident Directory View (read-only here - members are added and edited
// under Residents > Members, which only the Chairman can open)
// ============================================================================

class ResidentDirectoryView extends StatelessWidget {
  const ResidentDirectoryView({super.key});

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final residents = vm.members;
    final textTheme = Theme.of(context).textTheme;
    final onSurfaceVariant = Theme.of(context).colorScheme.onSurfaceVariant;

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: residents.length + 1,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        if (index == 0) {
          return Text(
            tr('member_directory', language),
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          );
        }
        final resident = residents[index - 1];
        final wingAndType = [
          if (resident.wing.trim().isNotEmpty) 'Wing ${resident.wing}',
          resident.unitType.trim().isNotEmpty ? resident.unitType : 'Unit type not set',
        ].join(' • ');
        return _outlinedCard(
          context,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${resident.flatNumber} • ${resident.ownerName}',
                            style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                          ),
                          if (resident.tenantName.trim().isNotEmpty)
                            Text(
                              'Tenant: ${resident.tenantName}',
                              style: textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary),
                            ),
                          // Wing and unit type - the two things that decide
                          // which tariff this unit is billed.
                          Text(
                            wingAndType,
                            style: textTheme.bodySmall?.copyWith(
                              color: resident.unitType.trim().isEmpty ? AppColors.statusOverdue : AppColors.tealPrimary,
                            ),
                          ),
                          Text(
                            '📞 ${resident.contactNumber} • Slot: '
                            '${resident.parkingSlot.trim().isEmpty ? 'N/A' : resident.parkingSlot}',
                            style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                    _Pill(text: resident.role.name),
                  ],
                ),
                const Divider(height: 16),
                // Read-only: members are added and edited under
                // Residents > Members, and are never deleted from here.
                Text(
                  'Family: ${resident.familyMemberCount} • 2W: ${resident.vehicleTwoWheelerCount} • '
                  '4W: ${resident.vehicleFourWheelerCount}',
                  style: textTheme.bodySmall,
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

// ============================================================================
// Dialog: Record Payment
// ============================================================================

typedef PaymentSubmit = void Function(
  double amount,
  PaymentMode mode,
  String ref,
  String date,
  String screenshotBase64,
);

class RecordPaymentDialog extends StatefulWidget {
  const RecordPaymentDialog({
    super.key,
    required this.bill,
    required this.language,
    this.isEditingReceipt = false,
    required this.onDismiss,
    required this.onSubmit,
  });

  final MaintenanceBill bill;
  final AppLanguage language;

  /// True when an existing receipt is being corrected rather than a new one taken.
  final bool isEditingReceipt;
  final VoidCallback onDismiss;
  final PaymentSubmit onSubmit;

  @override
  State<RecordPaymentDialog> createState() => _RecordPaymentDialogState();
}

class _RecordPaymentDialogState extends State<RecordPaymentDialog> {
  late final TextEditingController _amount = TextEditingController(
    text: widget.isEditingReceipt
        ? widget.bill.amountPaid.toInt().toString()
        : widget.bill.pendingAmount.toInt().toString(),
  );
  late PaymentMode _selectedMode = widget.bill.paymentMode ?? PaymentMode.UPI;
  late final TextEditingController _refNo = TextEditingController(text: widget.bill.transactionRef);

  // The day the money actually moved, which is not always the day it is
  // entered - a cheque banked last week is recorded today.
  late final TextEditingController _paidDate = TextEditingController(
    text: widget.bill.paidDate.trim().isEmpty ? todayLabel() : widget.bill.paidDate,
  );
  late String _screenshot = widget.bill.receiptImageBase64;

  @override
  void dispose() {
    _amount.dispose();
    _refNo.dispose();
    _paidDate.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bill = widget.bill;
    final language = widget.language;
    final textTheme = Theme.of(context).textTheme;

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        '${tr('record_payment', language)} (${bill.flatNumber})',
        style: _titleBold(context),
      ),
      // Scrolls, so the amount, mode, reference, date and the receipt
      // screenshot together can never push Save off the bottom on a
      // short screen or with the keyboard up.
      content: _dialogBody(
        spacing: 10,
        children: [
          Text('Member: ${bill.residentName} (${bill.billMonth})'),
          Text(
            'Bill ${rupees(bill.chargesSubtotal)}  +  Late fee ${rupees(bill.lateFeeAmount)}  =  '
            'Total ${rupees(bill.payableTotal)}',
            style: textTheme.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
          ),
          Text(
            'Already paid ${rupees(bill.amountPaid)}  •  Pending ${rupees(bill.pendingAmount)}',
            style: textTheme.bodySmall?.copyWith(
              fontWeight: FontWeight.bold,
              color: bill.pendingAmount > 0 ? AppColors.statusOverdue : AppColors.statusPaid,
            ),
          ),
          TextField(
            controller: _amount,
            keyboardType: TextInputType.number,
            decoration: InputDecoration(labelText: tr('amount', language)),
          ),
          _PaymentModeField(
            value: _selectedMode,
            language: language,
            onChanged: (mode) => setState(() => _selectedMode = mode),
          ),
          TextField(
            controller: _refNo,
            decoration: InputDecoration(
              labelText: tr('transaction_ref', language),
              hintText: 'e.g. UPI/623401992384 or Cheque No.',
            ),
          ),
          TextField(
            key: const Key('payment_date_input'),
            controller: _paidDate,
            maxLines: 1,
            decoration: InputDecoration(labelText: tr('payment_date', language), hintText: 'dd MMM yyyy'),
          ),
          _PaymentScreenshotPicker(
            screenshotBase64: _screenshot,
            onScreenshot: (s) => setState(() => _screenshot = s),
            language: language,
          ),
        ],
      ),
      actions: [
        TextButton(onPressed: widget.onDismiss, child: Text(tr('cancel', language))),
        FilledButton(
          key: const Key('submit_record_payment_btn'),
          style: _tealButtonStyle(),
          onPressed: () {
            final amount = _toDoubleOrNull(_amount.text) ?? bill.totalAmount;
            widget.onSubmit(amount, _selectedMode, _refNo.text, _paidDate.text, _screenshot);
          },
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}

class _PaymentModeField extends StatelessWidget {
  const _PaymentModeField({super.key, required this.value, required this.language, required this.onChanged});

  final PaymentMode value;
  final AppLanguage language;
  final ValueChanged<PaymentMode> onChanged;

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<PaymentMode>(
      initialValue: value,
      isExpanded: true,
      decoration: InputDecoration(labelText: tr('payment_mode', language)),
      items: [
        for (final mode in PaymentMode.values)
          DropdownMenuItem(
            value: mode,
            child: Text(_isMr(language) ? mode.labelMr : mode.labelEn),
          ),
      ],
      onChanged: (mode) {
        if (mode != null) onChanged(mode);
      },
    );
  }
}

/// The payment proof attached to a receipt.
///
/// Downscaled through [ImageBlob] before it is stored, the same as every other
/// picture the app keeps, so a full-size screenshot cannot overrun the document.
class _PaymentScreenshotPicker extends StatefulWidget {
  const _PaymentScreenshotPicker({
    required this.screenshotBase64,
    required this.onScreenshot,
    required this.language,
  });

  final String screenshotBase64;
  final ValueChanged<String> onScreenshot;
  final AppLanguage language;

  @override
  State<_PaymentScreenshotPicker> createState() => _PaymentScreenshotPickerState();
}

class _PaymentScreenshotPickerState extends State<_PaymentScreenshotPicker> {
  String? _error;

  Future<void> _pick() async {
    try {
      final picked = await pickImageAsBase64(context, source: ImageSource.gallery);
      if (picked == null || !mounted) return;
      widget.onScreenshot(picked);
      setState(() => _error = null);
    } catch (_) {
      if (mounted) setState(() => _error = 'Could not read that image. Pick a JPG or PNG.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final labelMedium = Theme.of(context).textTheme.labelMedium;
    final hasShot = widget.screenshotBase64.trim().isNotEmpty;
    final hasPreview = blobImageProvider(widget.screenshotBase64) != null;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            OutlinedButton.icon(
              key: const Key('payment_screenshot_btn'),
              style: OutlinedButton.styleFrom(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                side: const BorderSide(color: AppColors.tealPrimary),
              ),
              onPressed: _pick,
              icon: const Icon(Icons.add_photo_alternate, color: AppColors.tealPrimary, size: 16),
              label: Text(
                hasShot ? tr('replace_screenshot', widget.language) : tr('upload_screenshot', widget.language),
                style: labelMedium?.copyWith(color: AppColors.tealPrimary),
              ),
            ),
            if (hasShot)
              TextButton(
                onPressed: () {
                  widget.onScreenshot('');
                  setState(() => _error = null);
                },
                child: Text('Remove', style: labelMedium?.copyWith(color: AppColors.statusOverdue)),
              ),
          ],
        ),
        if (hasPreview) ...[
          const SizedBox(height: 6),
          ClipRRect(
            key: const Key('payment_screenshot_preview'),
            borderRadius: BorderRadius.circular(10),
            child: Semantics(
              label: 'Payment screenshot',
              child: BlobImage(
                base64: widget.screenshotBase64,
                width: double.infinity,
                height: 160,
                fit: BoxFit.contain,
              ),
            ),
          ),
        ],
        if (_error != null)
          Text(
            _error!,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Theme.of(context).colorScheme.error),
          ),
      ],
    );
  }
}

// ============================================================================
// Dialog: Generate Bill
// ============================================================================

/// How many months of history the Generate Bill sheet shows for a unit.
const int RECENT_BILL_MONTHS = 6;

/// "101 (Raju Anbhule)", or just the unit number when nobody lives there.
String _unitLabel(Resident unit) =>
    unit.ownerName.trim().isEmpty ? unit.flatNumber : '${unit.flatNumber} (${unit.ownerName})';

/// The unit number back out of a label - everything before the bracket.
String _flatNumberFromLabel(String label) {
  final i = label.indexOf(' (');
  return (i < 0 ? label : label.substring(0, i)).trim();
}

/// The 10th of the billed month, the usual society due date.
String _defaultDueDate(String billMonth) {
  final parsed = parseMonthYear(billMonth);
  if (parsed == null) return billMonth;
  return formatDate(DateTime(parsed.year, parsed.month, 10), 'dd MMM yyyy');
}

/// Raises the next month's bill for one unit.
///
/// The unit is picked from a dropdown, which fills in the resident's name (read
/// only - members are edited in the Resident Directory, not here), and the sheet
/// then shows exactly what that unit already owes: every past bill month by
/// month with its late fee, total and what was paid against it, and the pending
/// amount they add up to.
class GenerateBillDialog extends StatefulWidget {
  const GenerateBillDialog({super.key, required this.onDismiss});

  final VoidCallback onDismiss;

  @override
  State<GenerateBillDialog> createState() => _GenerateBillDialogState();
}

class _GenerateBillDialogState extends State<GenerateBillDialog> {
  String? _selectedFlat;
  String? _month;
  List<String> _lastMonths = const [];

  // The whole month's maintenance. The society's configured percentages
  // divide it up; they are not added on top of it.
  final TextEditingController _maintenance = TextEditingController();
  final TextEditingController _lateFee = TextEditingController(text: '0');
  final TextEditingController _dueDate = TextEditingController();
  String? _error;

  String? _tariffEffectKey;
  String? _lateFeeEffectKey;

  @override
  void dispose() {
    _maintenance.dispose();
    _lateFee.dispose();
    _dueDate.dispose();
    super.dispose();
  }

  void _setMonth(String month) {
    _month = month;
    _dueDate.text = _defaultDueDate(month);
  }

  /// Compose's LaunchedEffect: runs after the frame, whenever [key] changes.
  void _effect(String key, String? last, void Function(String) remember, VoidCallback body) {
    if (key == last) return;
    remember(key);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) setState(body);
    });
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final society = vm.societyInfo;
    final residents = vm.residents;
    final textTheme = Theme.of(context).textTheme;
    final onSurfaceVariant = Theme.of(context).colorScheme.onSurfaceVariant;

    // Only units that actually have a member behind them can be billed.
    final billableUnits = residents.where((r) => r.ownerName.trim().isNotEmpty).toList()
      ..sort((a, b) => a.flatNumber.compareTo(b.flatNumber));
    if (_selectedFlat == null ||
        (billableUnits.isNotEmpty && !billableUnits.any((u) => _equalsIgnoreCase(u.flatNumber, _selectedFlat!)))) {
      _selectedFlat = billableUnits.isEmpty ? '' : billableUnits.first.flatNumber;
    }
    final selectedFlat = _selectedFlat!;
    final resident = billableUnits.where((u) => _equalsIgnoreCase(u.flatNumber, selectedFlat)).firstOrNull;

    // Only this unit's bills - never another unit's - so the history below the
    // picker always belongs to the unit that is selected.
    final unitBills = vm.bills.where((b) => _equalsIgnoreCase(b.flatNumber, selectedFlat)).toList();
    final totalPending = unitBills.fold(0.0, (s, b) => s + b.pendingAmount);
    final unitLabels = {for (final u in billableUnits) u.flatNumber.toUpperCase(): _unitLabel(u)};

    // Billing runs from the society's configured start month up to today.
    final months = billingMonths(society);
    if (_month == null || !listEquals(months, _lastMonths)) {
      _lastMonths = months;
      _setMonth(months.isEmpty ? '' : months.first);
    }
    final month = _month!;

    // The maintenance for this unit comes from its unit type's tariff, and the
    // committee can still override it for this one bill. Charged against the
    // unit's own type, so two unit types in one wing are billed differently.
    final tariff = society?.monthlyTariffFor(resident) ?? 0.0;
    _effect('${resident?.flatNumber}|$tariff', _tariffEffectKey, (k) => _tariffEffectKey = k, () {
      _maintenance.text = tariff.toInt().toString();
    });

    // A unit already carrying dues past its due date attracts the society's
    // late fee. Suggested, not forced - the committee can still override it.
    final latePercent = society?.lateFeePercentPerMonth ?? 0.0;
    _effect('$selectedFlat|$totalPending|$latePercent', _lateFeeEffectKey, (k) => _lateFeeEffectKey = k, () {
      _lateFee.text = (totalPending > 0 && latePercent > 0) ? (totalPending * latePercent / 100).toInt().toString() : '0';
    });

    final maintenanceValue = _toDoubleOrNull(_maintenance.text) ?? 0.0;
    final split = society == null
        ? const MaintenanceSplit(water: 0, sinking: 0, repair: 0, other: 0)
        : society.splitMaintenance(maintenanceValue);
    final charges = split.total;
    final lateFeeValue = _toDoubleOrNull(_lateFee.text) ?? 0.0;
    final newBillTotal = charges + lateFeeValue;

    // Vacant units cannot be billed - there is nobody to bill - so say how
    // many are being left out rather than silently hiding them from a list
    // that is meant to hold every unit.
    final vacantCount = residents.where((r) => r.ownerName.trim().isEmpty && !r.role.isStaff).length;

    final selectedLabel = unitLabels[selectedFlat.toUpperCase()] ?? '';

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        _isMr(language) ? 'मासिक बिल तयार करा' : 'Generate Maintenance Bill',
        style: _titleBold(context),
      ),
      content: _dialogBody(
        children: [
          // Listed as "101 (Raju Anbhule)" so the committee can pick by
          // the member's name as easily as by the unit number.
          SearchableDropdownField(
            label: tr('unit_number_label', language),
            value: selectedLabel.trim().isEmpty ? selectedFlat : selectedLabel,
            options: billableUnits.map(_unitLabel).toList(),
            onValueChange: (picked) => setState(() {
              _selectedFlat = _flatNumberFromLabel(picked);
              _error = null;
            }),
            allowFreeText: false,
            testTag: 'bill_unit_dropdown',
          ),
          Text(
            '${billableUnits.length} unit(s) can be billed'
            '${vacantCount > 0 ? ' • $vacantCount vacant unit(s) have no member yet' : ''}',
            key: const Key('bill_unit_count'),
            style: textTheme.bodySmall?.copyWith(color: onSurfaceVariant),
          ),
          // Filled in from the unit; changed in the Resident Directory.
          // Re-created when the unit changes so the read-only value follows it.
          KeyedSubtree(
            key: ValueKey('resident_name_${resident?.flatNumber}_${resident?.ownerName}'),
            child: TextFormField(
              key: const Key('bill_resident_name'),
              initialValue: resident?.ownerName ?? '',
              readOnly: true,
              enabled: false,
              decoration: const InputDecoration(labelText: 'Resident Name'),
            ),
          ),
          // Total Pending across every bill raised for this unit.
          _outlinedCard(
            context,
            radius: 12,
            bordered: false,
            color: totalPending > 0 ? AppColors.statusOverdueBg : AppColors.statusPaidBg,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Flexible(
                    child: Text(
                      tr('total_pending', language),
                      style: textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold),
                    ),
                  ),
                  Text(
                    rupees(totalPending),
                    key: const Key('bill_total_pending'),
                    style: textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: totalPending > 0 ? AppColors.statusOverdue : AppColors.statusPaid,
                    ),
                  ),
                ],
              ),
            ),
          ),
          // The last six months for this unit alone.
          if (unitBills.isNotEmpty)
            MonthWiseBillTable(
              bills: unitBills,
              language: language,
              maxMonths: RECENT_BILL_MONTHS,
              heading: _isMr(language)
                  ? '$selectedFlat - मागील ६ महिन्यांची बिले'
                  : "$selectedFlat - Last 6 Months' Bills",
            ),
          const Divider(height: 8),
          Text(
            _isMr(language) ? 'नवीन बिल' : 'New Bill',
            style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
          ),
          SearchableDropdownField(
            label: 'Billing Month',
            value: month,
            options: months,
            onValueChange: (m) => setState(() => _setMonth(m)),
            allowFreeText: false,
            testTag: 'bill_month_dropdown',
          ),
          _AmountField(
            label: 'Total Maintenance (₹)',
            controller: _maintenance,
            testTag: 'bill_maintenance_input',
            onChanged: () => setState(() {}),
          ),
          // Split by the society's configured percentages, shown so the
          // committee can see where the money goes. Change the split
          // under Maintenance > Charge Setup, not here.
          _outlinedCard(
            context,
            radius: 12,
            bordered: false,
            color: _slateSurfaceVariant,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                children: [
                  _SplitRow(label: 'Water Charges', percent: society?.waterChargesPercent ?? 0.0, amount: split.water),
                  _SplitRow(label: 'Sinking Fund', percent: society?.sinkingFundPercent ?? 0.0, amount: split.sinking),
                  _SplitRow(label: 'Repair Fund', percent: society?.repairFundPercent ?? 0.0, amount: split.repair),
                  // Other carries whatever the rounding leaves over, so its
                  // share is read back off the amount rather than from the
                  // config - otherwise a society whose percentages do not yet
                  // add up to 100% would show a percentage that does not
                  // match its own rupees.
                  _SplitRow(
                    label: 'Other Maintenance',
                    percent: maintenanceValue > 0 ? split.other / maintenanceValue * 100.0 : 0.0,
                    amount: split.other,
                  ),
                ],
              ),
            ),
          ),
          _AmountField(
            label: 'Late Fee (₹)',
            controller: _lateFee,
            testTag: 'bill_late_fee_input',
            onChanged: () => setState(() {}),
          ),
          TextField(
            controller: _dueDate,
            maxLines: 1,
            decoration: const InputDecoration(labelText: 'Due Date'),
          ),
          // What the member will be asked to pay for this month.
          _outlinedCard(
            context,
            radius: 12,
            bordered: false,
            color: _slateSurfaceVariant,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                children: [
                  _TotalRow(label: 'Bill Amount', amount: charges),
                  _TotalRow(label: 'Late Fee', amount: lateFeeValue),
                  const Divider(height: 12),
                  _TotalRow(label: 'Total Payable', amount: newBillTotal, bold: true, testTag: 'bill_total_payable'),
                  if (totalPending > 0) ...[
                    _TotalRow(label: 'Previous Pending', amount: totalPending),
                    _TotalRow(
                      label: 'Grand Outstanding',
                      amount: newBillTotal + totalPending,
                      bold: true,
                      testTag: 'bill_grand_outstanding',
                    ),
                  ],
                ],
              ),
            ),
          ),
          if (_error != null)
            Text(_error!, style: textTheme.bodySmall?.copyWith(color: Theme.of(context).colorScheme.error)),
        ],
      ),
      actions: [
        TextButton(onPressed: widget.onDismiss, child: Text(tr('cancel', language))),
        FilledButton(
          key: const Key('bill_submit_btn'),
          style: _tealButtonStyle(),
          onPressed: resident == null
              ? null
              : () {
                  final member = resident;
                  if (charges <= 0.0) {
                    setState(() => _error = 'The bill amount must be more than zero.');
                    return;
                  }
                  if (unitBills.any((b) => _equalsIgnoreCase(b.billMonth, month))) {
                    setState(() => _error = 'A bill for $month already exists for ${member.flatNumber}.');
                    return;
                  }
                  // Other/Common Maintenance is carried in the bill's base
                  // amount; the four shares add back up to the maintenance.
                  vm.generateBill(
                    member.flatNumber,
                    member.ownerName,
                    month,
                    split.other,
                    split.water,
                    split.sinking,
                    split.repair,
                    lateFeeValue,
                    _dueDate.text,
                  );
                  widget.onDismiss();
                },
          child: Text(tr('submit', language)),
        ),
      ],
    );
  }
}

// ============================================================================
// Dialog: Edit Bill
// ============================================================================

typedef BillSave = void Function(
  String month,
  double base,
  double water,
  double sinking,
  double repair,
  double lateFee,
  String dueDate,
  void Function(String message) onError,
);

/// Edit an existing maintenance bill.
///
/// Everything that was charged can be corrected - the month, each line item, the
/// late fee and the due date. What has already been collected against the bill
/// is shown but never touched here; that is what Record Payment is for.
class EditBillDialog extends StatefulWidget {
  const EditBillDialog({
    super.key,
    required this.bill,
    required this.society,
    required this.language,
    required this.onDismiss,
    required this.onSave,
  });

  final MaintenanceBill bill;
  final SocietyInfo? society;
  final AppLanguage language;
  final VoidCallback onDismiss;
  final BillSave onSave;

  @override
  State<EditBillDialog> createState() => _EditBillDialogState();
}

class _EditBillDialogState extends State<EditBillDialog> {
  // The bill's own month is always offered, even for a bill raised before
  // the society's billing start month was set.
  late final List<String> _months = {...billingMonths(widget.society), widget.bill.billMonth}.toList();
  late String _month = widget.bill.billMonth;
  late final TextEditingController _base = TextEditingController(text: widget.bill.baseAmount.toInt().toString());
  late final TextEditingController _water = TextEditingController(text: widget.bill.waterCharges.toInt().toString());
  late final TextEditingController _sinking = TextEditingController(text: widget.bill.sinkingFund.toInt().toString());
  late final TextEditingController _repair = TextEditingController(text: widget.bill.repairFund.toInt().toString());
  late final TextEditingController _lateFee = TextEditingController(text: widget.bill.lateFeeAmount.toInt().toString());
  late final TextEditingController _dueDate = TextEditingController(text: widget.bill.dueDate);
  String? _error;

  @override
  void dispose() {
    for (final c in [_base, _water, _sinking, _repair, _lateFee, _dueDate]) {
      c.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bill = widget.bill;
    final language = widget.language;
    final textTheme = Theme.of(context).textTheme;

    final charges = [_base, _water, _sinking, _repair].fold(0.0, (s, c) => s + (_toDoubleOrNull(c.text) ?? 0.0));
    final lateFeeValue = _toDoubleOrNull(_lateFee.text) ?? 0.0;
    final payable = charges + lateFeeValue;
    final pendingAfter = payable - bill.amountPaid;
    void refresh() => setState(() {});

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(_isMr(language) ? 'बिल संपादित करा' : 'Edit Maintenance Bill', style: _titleBold(context)),
          Text(
            '${bill.flatNumber} • ${bill.residentName}',
            style: textTheme.bodySmall?.copyWith(color: AppColors.tealPrimary),
          ),
        ],
      ),
      content: _dialogBody(
        children: [
          SearchableDropdownField(
            label: 'Billing Month',
            value: _month,
            options: _months,
            onValueChange: (m) => setState(() {
              _month = m;
              _error = null;
            }),
            allowFreeText: false,
            testTag: 'edit_bill_month_dropdown',
          ),
          _AmountField(
            label: 'Other / Common Maintenance (₹)',
            controller: _base,
            testTag: 'edit_bill_base_input',
            onChanged: refresh,
          ),
          _AmountField(label: 'Water Charges (₹)', controller: _water, onChanged: refresh),
          _AmountField(label: 'Sinking Fund (₹)', controller: _sinking, onChanged: refresh),
          _AmountField(label: 'Repair Fund (₹)', controller: _repair, onChanged: refresh),
          _AmountField(
            label: 'Late Fee (₹)',
            controller: _lateFee,
            testTag: 'edit_bill_late_fee_input',
            onChanged: refresh,
          ),
          TextField(
            controller: _dueDate,
            maxLines: 1,
            decoration: const InputDecoration(labelText: 'Due Date'),
          ),
          _outlinedCard(
            context,
            radius: 12,
            bordered: false,
            color: _slateSurfaceVariant,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                children: [
                  _TotalRow(label: 'Bill Amount', amount: charges),
                  _TotalRow(label: 'Late Fee', amount: lateFeeValue),
                  const Divider(height: 12),
                  _TotalRow(label: 'Total Payable', amount: payable, bold: true, testTag: 'edit_bill_total_payable'),
                  _TotalRow(label: 'Already Paid', amount: bill.amountPaid),
                  _TotalRow(
                    label: 'Pending After Edit',
                    amount: pendingAfter < 0 ? 0.0 : pendingAfter,
                    bold: true,
                    testTag: 'edit_bill_pending',
                  ),
                ],
              ),
            ),
          ),
          if (_error != null)
            Text(_error!, style: textTheme.bodySmall?.copyWith(color: Theme.of(context).colorScheme.error)),
        ],
      ),
      actions: [
        TextButton(onPressed: widget.onDismiss, child: Text(tr('cancel', language))),
        FilledButton(
          key: const Key('edit_bill_save_btn'),
          style: _tealButtonStyle(),
          onPressed: () {
            if (charges <= 0.0) {
              setState(() => _error = 'The bill amount must be more than zero.');
              return;
            }
            widget.onSave(
              _month,
              _toDoubleOrNull(_base.text) ?? 0.0,
              _toDoubleOrNull(_water.text) ?? 0.0,
              _toDoubleOrNull(_sinking.text) ?? 0.0,
              _toDoubleOrNull(_repair.text) ?? 0.0,
              lateFeeValue,
              _dueDate.text,
              (message) {
                if (mounted) setState(() => _error = message);
              },
            );
          },
          child: Text(tr('save', language)),
        ),
      ],
    );
  }
}

// ============================================================================
// Month-wise bill table & small helpers
// ============================================================================

/// Month | Bill Amount | Late Fee | Total | Paid, one row per billed month.
///
/// [maxMonths] keeps the table to the most recent months - the Generate Bill
/// sheet shows the last six for the selected unit, not its whole history.
class MonthWiseBillTable extends StatelessWidget {
  const MonthWiseBillTable({
    super.key,
    required this.bills,
    required this.language,
    this.maxMonths = 1 << 30,
    this.heading,
  });

  final List<MaintenanceBill> bills;
  final AppLanguage language;
  final int maxMonths;
  final String? heading;

  @override
  Widget build(BuildContext context) {
    final sorted = [...bills]..sort((a, b) => billMonthKey(a.billMonth).compareTo(billMonthKey(b.billMonth)));
    final ordered = sorted.length > maxMonths ? sorted.sublist(sorted.length - maxMonths) : sorted;
    double sum(double Function(MaintenanceBill) f) => ordered.fold(0.0, (s, b) => s + f(b));
    const rowPadding = EdgeInsets.symmetric(horizontal: 8, vertical: 6);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 6),
          child: Text(
            heading ?? (_isMr(language) ? 'महिनानिहाय बिले' : 'Month-wise Bills'),
            style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
          ),
        ),
        Container(
          padding: rowPadding,
          decoration: const BoxDecoration(
            color: _tealPrimaryContainer,
            borderRadius: BorderRadius.only(topLeft: Radius.circular(8), topRight: Radius.circular(8)),
          ),
          child: const Row(
            children: [
              _TableCell('Month', flex: 22, bold: true),
              _TableCell('Bill', flex: 14, bold: true, end: true),
              _TableCell('Late Fee', flex: 13, bold: true, end: true),
              _TableCell('Total', flex: 14, bold: true, end: true),
              _TableCell('Paid', flex: 14, bold: true, end: true),
            ],
          ),
        ),
        for (final bill in ordered) ...[
          Padding(
            padding: rowPadding,
            child: Row(
              children: [
                _TableCell(bill.billMonth, flex: 22),
                _TableCell(fmt0(bill.chargesSubtotal), flex: 14, end: true),
                _TableCell(fmt0(bill.lateFeeAmount), flex: 13, end: true),
                _TableCell(fmt0(bill.payableTotal), flex: 14, end: true, bold: true),
                _TableCell(
                  fmt0(bill.amountPaid),
                  flex: 14,
                  end: true,
                  colour: bill.pendingAmount <= 0.009 ? AppColors.statusPaid : AppColors.statusOverdue,
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: _slateOutlineVariant),
        ],
        Padding(
          padding: rowPadding,
          child: Row(
            children: [
              const _TableCell('Total', flex: 22, bold: true),
              _TableCell(fmt0(sum((b) => b.chargesSubtotal)), flex: 14, bold: true, end: true),
              _TableCell(fmt0(sum((b) => b.lateFeeAmount)), flex: 13, bold: true, end: true),
              _TableCell(fmt0(sum((b) => b.payableTotal)), flex: 14, bold: true, end: true),
              _TableCell(fmt0(sum((b) => b.amountPaid)), flex: 14, bold: true, end: true),
            ],
          ),
        ),
      ],
    );
  }
}

class _TableCell extends StatelessWidget {
  const _TableCell(this.text, {required this.flex, this.bold = false, this.end = false, this.colour});

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
        maxLines: 1,
        overflow: TextOverflow.clip,
        softWrap: false,
        textAlign: end ? TextAlign.end : TextAlign.start,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
              fontWeight: bold ? FontWeight.bold : FontWeight.normal,
              color: colour,
            ),
      ),
    );
  }
}

class _TotalRow extends StatelessWidget {
  const _TotalRow({required this.label, required this.amount, this.bold = false, this.testTag = ''});

  final String label;
  final double amount;
  final bool bold;
  final String testTag;

  @override
  Widget build(BuildContext context) {
    final style = Theme.of(context).textTheme.bodySmall?.copyWith(
          fontWeight: bold ? FontWeight.bold : FontWeight.normal,
        );
    return Padding(
      key: testTag.trim().isEmpty ? null : Key(testTag),
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: style),
          Text(rupees(amount), style: style),
        ],
      ),
    );
  }
}

class _AmountField extends StatelessWidget {
  const _AmountField({
    required this.label,
    required this.controller,
    this.testTag = '',
    required this.onChanged,
  });

  final String label;
  final TextEditingController controller;
  final String testTag;
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context) {
    return TextField(
      key: testTag.trim().isEmpty ? null : Key(testTag),
      controller: controller,
      onChanged: (_) => onChanged(),
      keyboardType: TextInputType.number,
      inputFormatters: _amountFormatters,
      maxLines: 1,
      decoration: InputDecoration(labelText: label),
    );
  }
}

// ============================================================================
// Dialog: Bulk Collection
// ============================================================================

/// One receipt against however many months a payment covers.
///
/// A unit that owes five months and pays part of it should cost the chairman a
/// single entry, not five. Pick the unit, see what it owes month by month, type
/// what was handed over, and the sheet shows exactly which months that clears
/// before anything is written. Whatever the payment does not reach stays
/// pending; anything over what is owed is carried to the next bill.
class BulkCollectionDialog extends StatefulWidget {
  const BulkCollectionDialog({super.key, required this.onDismiss});

  final VoidCallback onDismiss;

  @override
  State<BulkCollectionDialog> createState() => _BulkCollectionDialogState();
}

class _BulkCollectionDialogState extends State<BulkCollectionDialog> {
  String? _flatNumber;
  final TextEditingController _amount = TextEditingController();
  PaymentMode _mode = PaymentMode.UPI;
  final TextEditingController _reference = TextEditingController();
  final TextEditingController _paidOn = TextEditingController(text: todayLabel());
  String? _errorMessage;

  @override
  void dispose() {
    _amount.dispose();
    _reference.dispose();
    _paidOn.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final bills = vm.bills;
    final textTheme = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;

    // Only units that actually owe something can be collected against.
    final unitsWithDues = bills.where((b) => b.pendingAmount > 0.009).map((b) => b.flatNumber).toSet().toList()
      ..sort();
    if (_flatNumber == null || (unitsWithDues.isNotEmpty && !unitsWithDues.contains(_flatNumber))) {
      _flatNumber = unitsWithDues.isEmpty ? '' : unitsWithDues.first;
    }
    final flatNumber = _flatNumber!;

    final pending = oldestMonthFirst(
      bills.where((b) => _equalsIgnoreCase(b.flatNumber, flatNumber) && b.pendingAmount > 0.009).toList(),
    );
    final totalPending = pending.fold(0.0, (s, b) => s + b.pendingAmount);
    final memberName =
        vm.residents.where((r) => _equalsIgnoreCase(r.flatNumber, flatNumber)).firstOrNull?.ownerName ?? '';

    final amount = _toDoubleOrNull(_amount.text) ?? 0.0;
    // Worked out as the chairman types, so the split is seen before it is saved.
    final preview = allocatePayment(pending, amount);

    final List<Widget> body;
    if (unitsWithDues.isEmpty) {
      body = [
        Text(
          _isMr(language) ? 'कोणत्याही युनिटची थकबाकी नाही.' : 'No unit has anything pending.',
          key: const Key('bulk_collect_nothing_pending'),
          style: textTheme.bodyMedium,
        ),
      ];
    } else {
      body = [
        SearchableDropdownField(
          label: tr('unit_number_label', language),
          value: flatNumber,
          options: unitsWithDues,
          onValueChange: (v) => setState(() {
            _flatNumber = v;
            _errorMessage = null;
          }),
          allowFreeText: false,
          testTag: 'bulk_collect_unit_dropdown',
        ),
        _outlinedCard(
          context,
          radius: 12,
          bordered: false,
          color: AppColors.statusOverdueBg,
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  memberName.trim().isEmpty ? flatNumber : '$flatNumber • $memberName',
                  style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                ),
                Text(
                  _isMr(language)
                      ? '${pending.length} महिने थकीत • एकूण ${rupees(totalPending)}'
                      : '${pending.length} month(s) pending • ${rupees(totalPending)} total',
                  key: const Key('bulk_collect_total_pending'),
                  style: textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w600, color: AppColors.statusOverdue),
                ),
              ],
            ),
          ),
        ),
        TextField(
          key: const Key('bulk_collect_amount_input'),
          controller: _amount,
          onChanged: (_) => setState(() => _errorMessage = null),
          keyboardType: TextInputType.number,
          inputFormatters: _amountFormatters,
          maxLines: 1,
          decoration: InputDecoration(
            labelText: tr('amount_paid', language),
            hintText: totalPending.toStringAsFixed(0),
            helperText: _isMr(language)
                ? 'पूर्ण भरणा: ${rupees(totalPending)}'
                : 'Pay in full: ${rupees(totalPending)}',
          ),
        ),
        _PaymentModeField(
          key: const Key('bulk_collect_mode_input'),
          value: _mode,
          language: language,
          onChanged: (m) => setState(() => _mode = m),
        ),
        TextField(
          key: const Key('bulk_collect_ref_input'),
          controller: _reference,
          maxLines: 1,
          decoration: InputDecoration(labelText: tr('transaction_ref', language)),
        ),
        TextField(
          key: const Key('bulk_collect_date_input'),
          controller: _paidOn,
          maxLines: 1,
          decoration: InputDecoration(labelText: tr('payment_date', language)),
        ),
        // What this payment would do, month by month, before it is made.
        if (amount > 0.0) ...[
          const Divider(),
          Text(
            _isMr(language) ? 'ही रक्कम अशी वळती होईल' : 'This payment will be adjusted as follows',
            style: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
          ),
          for (final line in preview.allocations)
            Row(
              key: Key('bulk_preview_${line.bill.billMonth.replaceAll(' ', '_')}'),
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(line.bill.billMonth, style: textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w600)),
                      Text(
                        line.applied <= 0.009
                            ? (_isMr(language)
                                ? 'थकीत राहील ${rupees(line.pendingAfter)}'
                                : 'Stays pending ${rupees(line.pendingAfter)}')
                            : line.isSettled
                                ? (_isMr(language) ? 'पूर्ण भरले' : 'Settled in full')
                                : (_isMr(language)
                                    ? 'अंशतः • ${rupees(line.pendingAfter)} थकीत'
                                    : 'Part paid • ${rupees(line.pendingAfter)} still pending'),
                        style: textTheme.labelSmall?.copyWith(
                          color: line.isSettled ? AppColors.statusPaid : AppColors.statusOverdue,
                        ),
                      ),
                    ],
                  ),
                ),
                Text(rupees(line.applied), style: textTheme.bodySmall?.copyWith(fontWeight: FontWeight.bold)),
              ],
            ),
          if (preview.excess > 0.009)
            Container(
              key: const Key('bulk_collect_excess_note'),
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(color: AppColors.statusPaidBg, borderRadius: BorderRadius.circular(10)),
              child: Text(
                _isMr(language)
                    ? '${rupees(preview.excess)} जास्तीची रक्कम पुढील महिन्याच्या बिलात वजा होईल.'
                    : "${rupees(preview.excess)} extra will be carried forward to the next month's bill.",
                style: textTheme.labelSmall?.copyWith(color: AppColors.statusPaid),
              ),
            ),
        ],
        if (_errorMessage != null)
          Text(
            _errorMessage!,
            key: const Key('bulk_collect_error'),
            style: textTheme.bodySmall?.copyWith(color: scheme.error),
          ),
      ];
    }

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            _isMr(language) ? 'एकत्रित पावती (अनेक महिने)' : 'Bulk Receipt - Multiple Months',
            style: _titleBold(context),
          ),
          Text(
            _isMr(language)
                ? 'एका पावतीत अनेक महिन्यांची रक्कम नोंदवा'
                : 'Record one payment against several pending months',
            style: textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ],
      ),
      content: _dialogBody(spacing: 10, maxHeight: 460, children: body),
      actions: [
        TextButton(onPressed: widget.onDismiss, child: Text(tr('cancel', language))),
        FilledButton(
          key: const Key('bulk_collect_save_btn'),
          style: _tealButtonStyle(radius: 8),
          onPressed: unitsWithDues.isNotEmpty && amount > 0.0
              ? () {
                  vm.recordBulkPayment(
                    flatNumber,
                    amount,
                    _mode,
                    _reference.text.trim(),
                    _paidOn.text.trim(),
                    onResult: (ok, message) {
                      if (ok) {
                        vm.showMessage(message);
                        widget.onDismiss();
                      } else if (mounted) {
                        setState(() => _errorMessage = message);
                      }
                    },
                  );
                }
              : null,
          child: Text(
            _isMr(language) ? 'पावती नोंदवा' : 'Record Receipt',
            style: textTheme.labelMedium?.copyWith(color: Colors.white),
          ),
        ),
      ],
    );
  }
}
