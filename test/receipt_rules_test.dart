// Port of ReceiptRulesTest.kt.
//
// What a receipt does to the bill behind it. Deleting one has to put the bill
// back exactly where it was, or the ledger and the treasury drift apart.

import 'package:flutter_test/flutter_test.dart';
import 'package:societysarv/data/models.dart';

const _eps = 0.001;

MaintenanceBill _bill({required double paid}) => MaintenanceBill(
      flatNumber: '101',
      residentName: 'Test Member',
      billMonth: 'August 2026',
      baseAmount: 715.0,
      waterCharges: 110.0,
      sinkingFund: 55.0,
      repairFund: 220.0,
      totalAmount: 1100.0,
      amountPaid: paid,
      paymentMode: paid > 0 ? PaymentMode.UPI : null,
      receiptNumber: paid > 0 ? 'REC-2026-08-1234' : '',
      dueDate: '10 Aug 2026',
    );

void main() {
  test('a fully settled bill reads as paid', () {
    final bill = _bill(paid: 1100.0);

    expect(bill.derivedStatus, PaymentStatus.PAID);
    expect(bill.pendingAmount, closeTo(0.0, _eps));
  });

  test('a part payment reads as partial, not paid', () {
    final bill = _bill(paid: 400.0);

    expect(bill.derivedStatus, PaymentStatus.PARTIAL);
    expect(bill.pendingAmount, closeTo(700.0, _eps));
  });

  test('withdrawing a receipt returns the bill to unpaid', () {
    final paid = _bill(paid: 1100.0);

    final reversed = paid.copyWith(
      amountPaid: 0.0,
      status: PaymentStatus.UNPAID,
      paidDate: '',
      paymentMode: null,
      transactionRef: '',
      receiptNumber: '',
      receiptImageBase64: '',
    );

    expect(reversed.derivedStatus, PaymentStatus.UNPAID);
    expect(reversed.pendingAmount, closeTo(reversed.payableTotal, _eps));
    expect(reversed.receiptNumber, isEmpty);
    expect(reversed.receiptImageBase64, isEmpty);
    // Dart-specific: copyWith(paymentMode: null) must really clear the mode.
    expect(reversed.paymentMode, isNull);
  });

  test('only a bill carrying a payment appears in the receipt list', () {
    final all = [_bill(paid: 1100.0), _bill(paid: 0.0), _bill(paid: 250.0)];

    final receipts = all.where((b) => b.amountPaid > 0.0).toList();

    expect(receipts.length, 2);
    expect(receipts.any((b) => b.amountPaid <= 0.0), isFalse);
  });

  test('editing a receipt replaces the amount rather than adding to it', () {
    final original = _bill(paid: 400.0);

    // The committee restates the same receipt as 1,100.
    final corrected = original.copyWith(amountPaid: 1100.0);
    final duesDelta = corrected.amountPaid - original.amountPaid;

    expect(corrected.amountPaid, closeTo(1100.0, _eps));
    expect(duesDelta, closeTo(700.0, _eps));
    expect(corrected.derivedStatus, PaymentStatus.PAID);
  });
}
