// Port of ui/screens/ResidentPortalScreen.kt - the member's own "My Unit" screen.

import 'package:flutter/foundation.dart' show listEquals;
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../data/models.dart';
import '../../data/payment_allocation.dart';
import '../../l10n/app_strings.dart';
import '../../state/society_view_model.dart';
import '../../util/format.dart';
import '../components/common_components.dart';
import '../components/upi_qr_payment_dialog.dart';
import '../theme/app_colors.dart';

class ResidentPortalScreen extends StatefulWidget {
  const ResidentPortalScreen({super.key});

  @override
  State<ResidentPortalScreen> createState() => _ResidentPortalScreenState();
}

class _ResidentPortalScreenState extends State<ResidentPortalScreen> {
  List<String> _yearsSeen = const [];
  String _selectedYear = '';

  void _openPasscodeGenerator(SocietyViewModel vm, String myFlat, AppLanguage language) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => GenerateVisitorPasscodeDialog(
        flatNumber: myFlat,
        language: language,
        onDismiss: () => Navigator.of(dialogContext).pop(),
        onSubmit: (name, phone, pur) {
          vm.generateVisitorPasscode(name, phone, myFlat, pur, onGenerated: (code) {
            if (mounted) _showGeneratedPasscode(code, vm.language);
          });
          Navigator.of(dialogContext).pop();
        },
      ),
    );
  }

  void _showGeneratedPasscode(String code, AppLanguage language) {
    final theme = Theme.of(context);
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        insetPadding: dialogInsetPadding(dialogContext),
        title: Text(
          language == AppLanguage.marathi ? 'अभ्यागत ६-अंकी पासकोड' : 'Visitor Pre-Approved Passcode',
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
        ),
        content: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                language == AppLanguage.marathi
                    ? 'हा पासकोड आपल्या पाहुण्याला / डिलिव्हरी बॉयला द्या:'
                    : 'Share this one-time passcode with your visitor:',
                style: theme.textTheme.bodyMedium,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 14),
              Container(
                decoration: BoxDecoration(
                  color: AppColors.tealContainer,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.tealPrimary, width: 1.5),
                ),
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                child: Text(
                  code,
                  style: theme.textTheme.headlineLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                    letterSpacing: 4,
                    color: AppColors.tealPrimary,
                  ),
                ),
              ),
            ],
          ),
        ),
        actions: [
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(tr('close', language)),
          ),
        ],
      ),
    );
  }

  void _openChangePin(SocietyViewModel vm, AppLanguage language) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => ChangePinDialog(
        language: language,
        onDismiss: () => Navigator.of(dialogContext).pop(),
        onSubmit: (newPin) {
          vm.updatePin(newPin);
          Navigator.of(dialogContext).pop();
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final society = vm.societyInfo;
    final currentUser = vm.currentUser;
    final bills = vm.bills;
    final theme = Theme.of(context);
    final muted = theme.colorScheme.onSurfaceVariant;

    // Everything on this screen belongs to one unit.
    //
    // A member with a single flat never notices; a member who owns several
    // picks one from the selector below and the dues, the bills, the parking
    // and the complaint form all follow that choice.
    final myUnits = vm.myUnits;
    final unit = vm.activeUnit ?? currentUser;

    final myFlat = unit?.flatNumber ?? '';
    final unitBills = bills.where((b) => b.flatNumber.toLowerCase() == myFlat.toLowerCase()).toList();

    // Financial year filter.
    //
    // A member looking for last year's receipts needs a way back to it, so the
    // years the society has billed in are offered and everything below - the
    // month-wise table, the statements, the receipts - follows the choice.
    final financialYearList = <String>[];
    for (final fy in [...financialYears(society), ...unitBills.map((b) => financialYearOf(b.billMonth))]) {
      if (fy.trim().isNotEmpty && !financialYearList.contains(fy)) financialYearList.add(fy);
    }
    if (!listEquals(financialYearList, _yearsSeen)) {
      _yearsSeen = financialYearList;
      _selectedYear = financialYearList.isEmpty ? '' : financialYearList.first;
    }
    final selectedYear = _selectedYear;
    final myBills = selectedYear.trim().isEmpty
        ? unitBills
        : unitBills.where((b) => financialYearOf(b.billMonth) == selectedYear).toList();

    // The gate log for this member's own units and nothing else - a member has
    // no business seeing who called on their neighbours. A member who holds
    // several units sees the callers at all of them.
    var myUnitNumbers = myUnits.map((u) => u.flatNumber.toUpperCase()).toList();
    if (myUnitNumbers.isEmpty && currentUser != null) myUnitNumbers = [currentUser.flatNumber.toUpperCase()];
    final myVisitors = vm.visitors.where((v) => myUnitNumbers.contains(v.flatNumber.toUpperCase())).toList();

    // What this unit owes, in the three parts a member reads separately: the
    // arrears behind them, this month's bill, and any credit they are already
    // in from an overpayment.
    final rawCredit = unit?.advanceBalance ?? 0.0;
    final advanceCredit = rawCredit < 0 ? 0.0 : rawCredit;
    // Every bill the unit has, not just the financial year on screen: what a
    // member owes does not reset because they looked back at last year.
    final dues = unitDues(unitBills, advanceCredit);

    // Every payment this unit has been receipted for, newest month first.
    //
    // A bill that is only part paid carries a receipt just as a settled one
    // does, and the bill card offers the receipt only once the bill is fully
    // paid. This lists them all in one place.
    final myReceipts = myBills.where((b) => b.amountPaid > 0.009).toList()
      ..sort((a, b) => _portalMonthKey(b.billMonth).compareTo(_portalMonthKey(a.billMonth)));

    void openQr(MaintenanceBill bill) => showUpiQrPaymentDialog(
          context,
          bill: bill,
          society: society,
          language: language,
          dues: dues,
          // The committee receipts the payment once it lands; the member just
          // closes the sheet.
          onPaymentRecorded: () {},
        );
    void openReceipt(MaintenanceBill bill) =>
        showReceiptDialog(context, bill: bill, society: society, language: language);

    final cardShape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(14),
      side: const BorderSide(color: AppColors.borderLight),
    );

    final children = <Widget>[
      // A member who owns more than one unit picks which one this screen is
      // about; everything below follows the selection.
      if (myUnits.length > 1)
        Card(
          key: const Key('unit_selector_card'),
          margin: EdgeInsets.zero,
          color: theme.colorScheme.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
            side: const BorderSide(color: AppColors.tealPrimary),
          ),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  tr('select_unit', language),
                  style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                ),
                Text(
                  '${myUnits.length} ${tr('units_you_own', language)}',
                  style: theme.textTheme.bodySmall?.copyWith(color: muted),
                ),
                const SizedBox(height: 8),
                SearchableDropdownField(
                  label: tr('unit_number_label', language),
                  value: myFlat,
                  options: myUnits.map((u) => u.flatNumber).toList(),
                  onValueChange: vm.selectUnit,
                  allowFreeText: false,
                  testTag: 'portal_unit_dropdown',
                ),
              ],
            ),
          ),
        ),

      // Member card.
      //
      // Deliberately compact - a smaller avatar, one line of identity, tight
      // padding - so the dues underneath it are on screen without scrolling.
      _memberCard(theme, unit, dues, language, () => _openChangePin(vm, language)),

      if (financialYearList.length > 1)
        Card(
          key: const Key('portal_financial_year_card'),
          margin: EdgeInsets.zero,
          color: theme.colorScheme.surface,
          shape: cardShape,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                SearchableDropdownField(
                  label: tr('financial_year', language),
                  value: selectedYear,
                  options: financialYearList,
                  onValueChange: (v) => setState(() => _selectedYear = v),
                  allowFreeText: false,
                  testTag: 'portal_financial_year_dropdown',
                ),
                Text(
                  '${myBills.length} bill(s) in $selectedYear',
                  style: theme.textTheme.bodySmall?.copyWith(color: muted),
                ),
              ],
            ),
          ),
        ),

      // Month-wise billing for this unit: what was billed, what was paid, what
      // is still due for the month, and the running outstanding.
      if (myBills.isNotEmpty) UnitMonthlyBillingTable(bills: myBills, unitNumber: myFlat, language: language),

      // Resident quick actions.
      //
      // Raising a complaint lives on the Helpdesk, where the member can also
      // see what came of it, so this screen keeps only the action that has
      // nowhere else to be.
      SizedBox(
        height: 48,
        child: FilledButton(
          key: const Key('gen_passcode_btn'),
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.tealPrimary,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
          onPressed: () => _openPasscodeGenerator(vm, myFlat, language),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.key, size: 18),
              const SizedBox(width: 6),
              Text(
                language == AppLanguage.marathi ? 'अभ्यागत पासकोड तयार करा' : 'Visitor Passcode',
                style: theme.textTheme.labelMedium?.copyWith(color: Colors.white),
              ),
            ],
          ),
        ),
      ),

      // Visitors the gate logged against this unit.
      //
      // The passcode is shown here and nowhere else - it belongs to the member
      // the visitor came to see, not to the whole society - and the member
      // answers for anyone still waiting at the gate.
      if (myVisitors.isNotEmpty) ...[
        Text(
          tr('gate_activity_log', language),
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
        ),
        for (final visitor in myVisitors) _visitorCard(theme, vm, visitor, language, cardShape),
      ],

      // My Vehicles & Parking Slot
      Card(
        margin: EdgeInsets.zero,
        color: theme.colorScheme.surface,
        shape: cardShape,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                language == AppLanguage.marathi ? '$myFlat - वाहने व पार्किंग जागा' : '$myFlat - Parking & Vehicles',
                style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                '🅿️ Allocated Slot: ${(unit?.parkingSlot ?? '').trim().isEmpty ? 'Not allotted' : unit!.parkingSlot}',
                style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
              ),
              if ((unit?.fourWheelerNumber ?? '').trim().isNotEmpty)
                Text('🚗 4W Car: ${unit!.fourWheelerNumber}', style: theme.textTheme.bodySmall),
              if ((unit?.twoWheelerNumber ?? '').trim().isNotEmpty)
                Text('🛵 2W Scooter: ${unit!.twoWheelerNumber}', style: theme.textTheme.bodySmall),
            ],
          ),
        ),
      ),

      // My Maintenance Bills
      Text(
        language == AppLanguage.marathi ? '$myFlat - देखभाल बिले व पावत्या' : '$myFlat - Maintenance Statements',
        style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),

      // Bill cards: a single row - the month and its status on the left, the
      // amount and the one action on the right.
      for (final bill in myBills) _billCard(theme, bill, language, openQr, openReceipt),

      // Every receipt this unit holds, to open, download or share.
      if (myReceipts.isNotEmpty) ...[
        Text(
          tr('my_receipts', language),
          key: const Key('portal_receipts_heading'),
          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
        ),
        for (final receipt in myReceipts) _receiptCard(theme, receipt, language, openReceipt),
      ],
    ];

    return ColoredBox(
      color: theme.scaffoldBackgroundColor,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: children.length,
        separatorBuilder: (_, _) => const SizedBox(height: 14),
        itemBuilder: (_, i) => children[i],
      ),
    );
  }

  Widget _memberCard(ThemeData theme, Resident? unit, UnitDues dues, AppLanguage language, VoidCallback onChangePin) {
    final mr = language == AppLanguage.marathi;
    final settled = dues.isSettled;
    return Card(
      key: const Key('portal_member_card'),
      margin: EdgeInsets.zero,
      color: AppColors.tealContainer,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(
                  child: Row(
                    children: [
                      Container(
                        width: 34,
                        height: 34,
                        decoration: const BoxDecoration(color: AppColors.tealPrimary, shape: BoxShape.circle),
                        alignment: Alignment.center,
                        child: const Icon(Icons.home, color: Colors.white, size: 18),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '${unit?.flatNumber ?? ''} • ${unit?.ownerName ?? 'Member'}',
                              style: theme.textTheme.titleSmall?.copyWith(
                                fontWeight: FontWeight.bold,
                                color: AppColors.tealPrimary,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                            Text(
                              '${unit?.flatType.displayName ?? '2 BHK'} • Wing ${unit?.wing ?? 'A'}',
                              style: theme.textTheme.labelSmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                              maxLines: 1,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                SizedBox(
                  width: 32,
                  height: 32,
                  child: IconButton(
                    padding: EdgeInsets.zero,
                    onPressed: onChangePin,
                    tooltip: 'Change PIN',
                    icon: const Icon(Icons.lock, color: AppColors.tealPrimary, size: 18),
                  ),
                ),
              ],
            ),
            const Padding(padding: EdgeInsets.symmetric(vertical: 8), child: Divider(height: 1)),

            // Every figure here comes off the bills themselves, so this can
            // never disagree with the month-wise table below it. Arrears, this
            // month and any credit are each shown, then the one number the
            // member actually has to pay.
            if (dues.previousPending > 0.009)
              PortalDuesLine(label: mr ? 'मागील थकबाकी' : 'Previous pending', amount: dues.previousPending),
            if (dues.currentPending > 0.009)
              PortalDuesLine(label: mr ? 'या महिन्याचे बिल' : 'Current bill', amount: dues.currentPending),
            if (dues.excessCredit > 0.009)
              PortalDuesLine(
                label: mr ? 'जमा (जास्तीची) रक्कम' : 'Advance credit',
                amount: -dues.excessCredit,
                color: AppColors.statusPaid,
                testTag: 'portal_advance_credit',
              ),
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(mr ? 'एकूण भरायची रक्कम' : 'Total Payable', style: theme.textTheme.labelSmall),
                        Text(
                          rupees(dues.netPayable),
                          key: const Key('portal_outstanding_amount'),
                          style: theme.textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.bold,
                            color: settled ? AppColors.statusPaid : AppColors.statusOverdue,
                          ),
                        ),
                        // Credit beyond everything owed is the member's, and
                        // comes off whatever is billed next.
                        if (dues.creditInHand > 0.009)
                          Text(
                            mr
                                ? '${rupees(dues.creditInHand)} पुढील बिलात वजा होईल'
                                : '${rupees(dues.creditInHand)} credit carries to your next bill',
                            key: const Key('portal_credit_in_hand'),
                            style: theme.textTheme.labelSmall?.copyWith(color: AppColors.statusPaid),
                          ),
                      ],
                    ),
                  ),
                  Container(
                    decoration: BoxDecoration(
                      color: settled ? AppColors.statusPaidBg : AppColors.statusOverdue.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    child: Text(
                      settled ? tr('status_paid', language) : tr('status_unpaid', language),
                      style: theme.textTheme.labelSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: settled ? AppColors.statusPaid : AppColors.statusOverdue,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _visitorCard(
    ThemeData theme,
    SocietyViewModel vm,
    VisitorLog visitor,
    AppLanguage language,
    ShapeBorder shape,
  ) {
    final mr = language == AppLanguage.marathi;
    return Card(
      key: Key('portal_visitor_${visitor.id}'),
      margin: EdgeInsets.zero,
      color: theme.colorScheme.surface,
      shape: shape,
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
                        visitor.visitorName,
                        style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      Text(
                        '${visitor.flatNumber} • ${visitor.purpose.labelEn} • ${visitor.entryTime}',
                        style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                      if (visitor.vehicleNumber.trim().isNotEmpty)
                        Text('🚗 ${visitor.vehicleNumber}', style: theme.textTheme.bodySmall),
                    ],
                  ),
                ),
                VisitorStatusBadge(status: visitor.status, language: language),
              ],
            ),
            if (visitor.passcode.trim().isNotEmpty) ...[
              const SizedBox(height: 10),
              Container(
                decoration: BoxDecoration(
                  color: AppColors.tealContainer,
                  borderRadius: BorderRadius.circular(10),
                ),
                padding: const EdgeInsets.all(10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      mr ? 'पासकोड' : 'Passcode',
                      style: theme.textTheme.labelSmall?.copyWith(color: AppColors.tealPrimary),
                    ),
                    Text(
                      visitor.passcode,
                      key: Key('portal_visitor_passcode_${visitor.id}'),
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        letterSpacing: 3,
                        color: AppColors.tealPrimary,
                      ),
                    ),
                  ],
                ),
              ),
            ],
            // Only somebody still at the gate needs answering.
            if (visitor.status == VisitorStatus.PRE_APPROVED || visitor.status == VisitorStatus.INSIDE) ...[
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: FilledButton(
                      key: Key('portal_approve_visitor_${visitor.id}'),
                      style: FilledButton.styleFrom(
                        backgroundColor: AppColors.tealPrimary,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                      onPressed: () =>
                          vm.answerVisitor(visitor, true, onResult: (_, message) => vm.showMessage(message)),
                      child: Text(
                        mr ? 'परवानगी द्या' : 'Approve',
                        style: theme.textTheme.labelMedium?.copyWith(color: Colors.white),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: OutlinedButton(
                      key: Key('portal_reject_visitor_${visitor.id}'),
                      style: OutlinedButton.styleFrom(
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                        side: const BorderSide(color: AppColors.statusOverdue),
                      ),
                      onPressed: () =>
                          vm.answerVisitor(visitor, false, onResult: (_, message) => vm.showMessage(message)),
                      child: Text(
                        mr ? 'नकार' : 'Deny',
                        style: theme.textTheme.labelMedium?.copyWith(color: AppColors.statusOverdue),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _billCard(
    ThemeData theme,
    MaintenanceBill bill,
    AppLanguage language,
    void Function(MaintenanceBill) openQr,
    void Function(MaintenanceBill) openReceipt,
  ) {
    final mr = language == AppLanguage.marathi;
    final status = bill.derivedStatus;
    // Derived, not stored, so an edited bill never disagrees with the
    // month-wise table above.
    final subtitle = rupees(bill.payableTotal) +
        (bill.pendingAmount > 0.009 && bill.amountPaid > 0.009
            ? ' • ${rupees(bill.pendingAmount)} due'
            : ' • Due ${bill.dueDate}');
    return Card(
      key: Key('portal_bill_card_${bill.billMonth.replaceAll(' ', '_')}'),
      margin: EdgeInsets.zero,
      color: theme.colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.borderLight),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    bill.billMonth,
                    style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                    maxLines: 1,
                  ),
                  Text(
                    subtitle,
                    style: theme.textTheme.labelSmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            StatusBadge(status: status, language: language),
            const SizedBox(width: 8),
            // A member pays, they do not record payments. Marking a bill
            // settled is the committee's entry to make, so the member is
            // offered the QR to pay with and, once the committee has receipted
            // it, the receipt to keep.
            if (status != PaymentStatus.PAID)
              _smallAction(
                theme,
                key: Key('portal_upi_qr_btn_${bill.flatNumber}'),
                icon: Icons.qr_code,
                label: mr ? 'QR' : 'Pay',
                onPressed: () => openQr(bill),
              )
            else
              // Only a settled bill has a receipt behind it.
              _smallAction(
                theme,
                key: Key('portal_receipt_btn_${bill.flatNumber}'),
                icon: Icons.receipt,
                label: mr ? 'पावती' : 'Receipt',
                onPressed: () => openReceipt(bill),
              ),
          ],
        ),
      ),
    );
  }

  Widget _receiptCard(
    ThemeData theme,
    MaintenanceBill receipt,
    AppLanguage language,
    void Function(MaintenanceBill) openReceipt,
  ) {
    final monthTag = receipt.billMonth.replaceAll(' ', '_');
    final paidDate = receipt.paidDate.trim().isEmpty ? '' : ' • ${receipt.paidDate}';
    return Card(
      key: Key('portal_receipt_row_$monthTag'),
      margin: EdgeInsets.zero,
      color: theme.colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.borderLight),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    receipt.receiptNumber.trim().isEmpty ? receipt.billMonth : receipt.receiptNumber,
                    style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(
                    '${receipt.billMonth} • ${rupees(receipt.amountPaid)}$paidDate',
                    style: theme.textTheme.labelSmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            _smallAction(
              theme,
              key: Key('portal_open_receipt_$monthTag'),
              icon: Icons.receipt,
              label: language == AppLanguage.marathi ? 'उघडा' : 'Open',
              onPressed: () => openReceipt(receipt),
            ),
          ],
        ),
      ),
    );
  }

  Widget _smallAction(
    ThemeData theme, {
    required Key key,
    required IconData icon,
    required String label,
    required VoidCallback onPressed,
  }) {
    return OutlinedButton(
      key: key,
      onPressed: onPressed,
      style: OutlinedButton.styleFrom(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        side: const BorderSide(color: AppColors.tealPrimary),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        minimumSize: const Size(0, 32),
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: AppColors.tealPrimary, size: 14),
          const SizedBox(width: 4),
          Text(label, style: theme.textTheme.labelSmall?.copyWith(color: AppColors.tealPrimary)),
        ],
      ),
    );
  }
}

/// One line of the member's dues breakdown: a label left, a rupee figure right.
class PortalDuesLine extends StatelessWidget {
  const PortalDuesLine({
    super.key,
    required this.label,
    required this.amount,
    this.color,
    this.testTag = '',
  });

  final String label;
  final double amount;

  /// Defaults to the theme's onSurface.
  final Color? color;
  final String testTag;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      key: testTag.trim().isEmpty ? null : Key(testTag),
      padding: const EdgeInsets.symmetric(vertical: 1),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: theme.textTheme.labelMedium),
          Text(
            rupees(amount),
            style: theme.textTheme.labelMedium?.copyWith(
              fontWeight: FontWeight.w600,
              color: color ?? theme.colorScheme.onSurface,
            ),
          ),
        ],
      ),
    );
  }
}

/// One unit's billing, month by month.
///
///  - Total Bill    what was charged for that month, late fee included
///  - Paid Amount   what has been collected against it
///  - Due Amount    what is still owed for that month alone
///  - Pending       everything still owed up to and including that month
///
/// Due is the month on its own; Pending is the running arrear, which is the
/// number a member is actually chased for.
class UnitMonthlyBillingTable extends StatelessWidget {
  const UnitMonthlyBillingTable({
    super.key,
    required this.bills,
    required this.unitNumber,
    required this.language,
  });

  final List<MaintenanceBill> bills;
  final String unitNumber;
  final AppLanguage language;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final ordered = [...bills]..sort((a, b) => _portalMonthKey(a.billMonth).compareTo(_portalMonthKey(b.billMonth)));

    // Running arrear: each month carries forward everything left before it.
    var running = 0.0;
    final rows = <(MaintenanceBill, double)>[];
    for (final bill in ordered) {
      running += bill.pendingAmount;
      rows.add((bill, running));
    }
    final lastRunning = rows.isEmpty ? 0.0 : rows.last.$2;
    double sum(double Function(MaintenanceBill) f) => ordered.fold(0.0, (s, b) => s + f(b));

    Widget cell(String text, int flex, {bool bold = false, bool end = false, Color? colour}) => Expanded(
          flex: flex,
          child: Text(
            text,
            style: theme.textTheme.labelSmall?.copyWith(
              fontWeight: bold ? FontWeight.bold : FontWeight.normal,
              color: colour,
            ),
            maxLines: 1,
            softWrap: false,
            overflow: TextOverflow.clip,
            textAlign: end ? TextAlign.end : TextAlign.start,
          ),
        );

    return Card(
      key: const Key('unit_monthly_billing_table'),
      margin: EdgeInsets.zero,
      color: theme.colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: AppColors.borderLight),
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              language == AppLanguage.marathi
                  ? '$unitNumber - महिनानिहाय बिल तपशील'
                  : '$unitNumber - Month-wise Billing',
              style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Container(
              decoration: const BoxDecoration(
                color: AppColors.tealContainer,
                borderRadius: BorderRadius.only(topLeft: Radius.circular(8), topRight: Radius.circular(8)),
              ),
              padding: const EdgeInsets.all(6),
              child: Row(
                children: [
                  cell('Month', 19, bold: true),
                  cell('Total Bill', 14, bold: true, end: true),
                  cell('Paid', 12, bold: true, end: true),
                  cell('Due', 12, bold: true, end: true),
                  cell('Pending', 13, bold: true, end: true),
                ],
              ),
            ),
            for (final (bill, runningTotal) in rows) ...[
              Padding(
                key: Key('portal_bill_row_${bill.billMonth}'),
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 7),
                child: Row(
                  children: [
                    cell(bill.billMonth, 19),
                    cell(fmt0(bill.payableTotal), 14, end: true),
                    cell(fmt0(bill.amountPaid), 12, end: true, colour: AppColors.statusPaid),
                    cell(
                      fmt0(bill.pendingAmount),
                      12,
                      end: true,
                      colour: bill.pendingAmount > 0.009 ? AppColors.statusOverdue : AppColors.statusPaid,
                    ),
                    cell(
                      fmt0(runningTotal),
                      13,
                      end: true,
                      bold: true,
                      colour: runningTotal > 0.009 ? AppColors.statusOverdue : AppColors.statusPaid,
                    ),
                  ],
                ),
              ),
              const Divider(height: 1, color: AppColors.borderLight),
            ],
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 8),
              child: Row(
                children: [
                  cell('Total', 19, bold: true),
                  cell(fmt0(sum((b) => b.payableTotal)), 14, bold: true, end: true),
                  cell(fmt0(sum((b) => b.amountPaid)), 12, bold: true, end: true),
                  cell(fmt0(sum((b) => b.pendingAmount)), 12, bold: true, end: true),
                  cell(
                    fmt0(lastRunning),
                    13,
                    bold: true,
                    end: true,
                    colour: lastRunning > 0.009 ? AppColors.statusOverdue : AppColors.statusPaid,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Chronological ordering for "August 2026" style month labels.
int _portalMonthKey(String billMonth) => parseMonthYear(billMonth)?.millisecondsSinceEpoch ?? 0;

/// Dialog: Generate Passcode for visitor.
class GenerateVisitorPasscodeDialog extends StatefulWidget {
  const GenerateVisitorPasscodeDialog({
    super.key,
    required this.flatNumber,
    required this.language,
    required this.onDismiss,
    required this.onSubmit,
  });

  final String flatNumber;
  final AppLanguage language;
  final VoidCallback onDismiss;
  final void Function(String name, String phone, VisitorPurpose pur) onSubmit;

  @override
  State<GenerateVisitorPasscodeDialog> createState() => _GenerateVisitorPasscodeDialogState();
}

class _GenerateVisitorPasscodeDialogState extends State<GenerateVisitorPasscodeDialog> {
  final _name = TextEditingController();
  final _phone = TextEditingController();
  VisitorPurpose _selectedPurpose = VisitorPurpose.GUEST;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final mr = widget.language == AppLanguage.marathi;
    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        mr ? 'अभ्यागत पूर्व-मंजूरी पासकोड' : 'Generate Visitor Passcode',
        style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(
                controller: _name,
                decoration: const InputDecoration(labelText: 'Visitor Name (पाहुण्याचे नाव)'),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _phone,
                keyboardType: TextInputType.phone,
                decoration: const InputDecoration(labelText: 'Visitor Mobile'),
              ),
              const SizedBox(height: 10),
              DropdownButtonFormField<VisitorPurpose>(
                initialValue: _selectedPurpose,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Purpose'),
                items: [
                  for (final pur in VisitorPurpose.values) DropdownMenuItem(value: pur, child: Text(pur.name)),
                ],
                onChanged: (pur) {
                  if (pur != null) setState(() => _selectedPurpose = pur);
                },
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: widget.onDismiss, child: Text(tr('cancel', widget.language))),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: () {
            if (_name.text.trim().isNotEmpty) {
              widget.onSubmit(_name.text, _phone.text, _selectedPurpose);
            }
          },
          child: Text(mr ? 'पासकोड तयार करा' : 'Generate Code'),
        ),
      ],
    );
  }
}

/// Dialog: Change PIN.
class ChangePinDialog extends StatefulWidget {
  const ChangePinDialog({
    super.key,
    required this.language,
    required this.onDismiss,
    required this.onSubmit,
  });

  final AppLanguage language;
  final VoidCallback onDismiss;
  final ValueChanged<String> onSubmit;

  @override
  State<ChangePinDialog> createState() => _ChangePinDialogState();
}

class _ChangePinDialogState extends State<ChangePinDialog> {
  final _newPin = TextEditingController();
  final _confirmPin = TextEditingController();
  String? _errorMsg;

  @override
  void dispose() {
    _newPin.dispose();
    _confirmPin.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final mr = widget.language == AppLanguage.marathi;
    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        mr ? '४-अंकी पिन बदला' : 'Change 4-Digit Security PIN',
        style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(
                controller: _newPin,
                obscureText: true,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'New PIN'),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _confirmPin,
                obscureText: true,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Confirm PIN'),
              ),
              if (_errorMsg != null) ...[
                const SizedBox(height: 10),
                Text(_errorMsg!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: widget.onDismiss, child: Text(tr('cancel', widget.language))),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: () {
            final newPin = _newPin.text;
            if (newPin.length == 4 && newPin == _confirmPin.text) {
              widget.onSubmit(newPin);
            } else {
              setState(() {
                _errorMsg = mr ? 'पिन ४ अंकांचा असावा आणि दोन्ही जुळले पाहिजेत!' : 'PIN must be 4 digits and match!';
              });
            }
          },
          child: Text(tr('save', widget.language)),
        ),
      ],
    );
  }
}
