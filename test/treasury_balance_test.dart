// Port of TreasuryBalanceTest.kt.
//
// The society's balance is worked out from its own ledger, so it can never
// drift away from the entries behind it.

import 'package:flutter_test/flutter_test.dart';
import 'package:societysarv/data/models.dart';

const _eps = 0.001;

MaintenanceBill _bill(double paid, PaymentMode? mode) => MaintenanceBill(
      flatNumber: '101',
      residentName: 'Test Member',
      billMonth: 'August 2026',
      baseAmount: 1100.0,
      totalAmount: 1100.0,
      amountPaid: paid,
      paymentMode: mode,
      dueDate: '10 Aug 2026',
    );

ExpenseRecord _expense(double amount, PaymentMode mode) => ExpenseRecord(
      title: 'Test',
      category: ExpenseCategory.MISCELLANEOUS,
      amount: amount,
      date: '01 Aug 2026',
      vendorName: 'Vendor',
      paymentMode: mode,
    );

NonFeeRevenue _revenue(double amount, PaymentMode mode) => NonFeeRevenue(
      title: 'Test',
      sourceCategory: RevenueSource.HALL_RENTAL,
      amount: amount,
      date: '01 Aug 2026',
      payerName: 'Payer',
      paymentMode: mode,
    );

SocietyInfo _society({double openingBank = 0.0, double openingCash = 0.0}) => SocietyInfo(
      name: 'Test Society',
      registrationNumber: 'REG-1',
      societyCode: 'TS001',
      address: '',
      totalWings: 1,
      totalFlats: 0,
      bankAccountName: '',
      bankAccountNumber: '',
      ifscCode: '',
      currentBalanceCash: openingCash,
      currentBalanceBank: openingBank,
    );

void main() {
  test('a society with no entries at all holds nothing', () {
    final balance = _society().treasuryBalance(const [], const [], const []);

    expect(balance.cash, closeTo(0.0, _eps));
    expect(balance.bank, closeTo(0.0, _eps));
    expect(balance.total, closeTo(0.0, _eps));
  });

  test('deleting the bills takes their collections back out again', () {
    final paid = [_bill(1100.0, PaymentMode.UPI), _bill(1100.0, PaymentMode.UPI)];

    final withBills = _society().treasuryBalance(paid, const [], const []);
    final afterDeletion = _society().treasuryBalance(const [], const [], const []);

    expect(withBills.bank, closeTo(2200.0, _eps));
    // This is the case that used to leave 2,200 stranded on the society.
    expect(afterDeletion.bank, closeTo(0.0, _eps));
  });

  test('cash and bank are kept apart', () {
    final balance = _society().treasuryBalance(
      [_bill(500.0, PaymentMode.CASH), _bill(800.0, PaymentMode.BANK_TRANSFER)],
      [_expense(200.0, PaymentMode.CASH)],
      [_revenue(300.0, PaymentMode.UPI)],
    );

    expect(balance.cash, closeTo(300.0, _eps));
    expect(balance.bank, closeTo(1100.0, _eps));
  });

  test('an opening balance is carried forward', () {
    final balance = _society(openingBank: 5000.0, openingCash: 250.0).treasuryBalance(
      [_bill(1100.0, PaymentMode.UPI)],
      [_expense(600.0, PaymentMode.BANK_TRANSFER)],
      const [],
    );

    expect(balance.cash, closeTo(250.0, _eps));
    expect(balance.bank, closeTo(5500.0, _eps));
  });

  test('an unpaid bill adds nothing', () {
    final balance = _society().treasuryBalance([_bill(0.0, null)], const [], const []);

    expect(balance.total, closeTo(0.0, _eps));
  });
}
