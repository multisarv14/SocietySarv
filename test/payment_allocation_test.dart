// Port of PaymentAllocationTest.kt.
//
// How one payment is spread over what a unit owes.
//
// These are the rules a chairman is actually working to at the collection desk:
// take whatever the member hands over, clear the oldest arrears first, leave
// the rest showing as pending, and carry an overpayment forward rather than
// losing it.

import 'package:flutter_test/flutter_test.dart';
import 'package:societysarv/data/models.dart';
import 'package:societysarv/data/payment_allocation.dart';

const _eps = 0.001;

MaintenanceBill _bill(String month, double base, {double paid = 0.0, double lateFee = 0.0}) => MaintenanceBill(
      docId: '$month-doc',
      flatNumber: 'A-202',
      residentName: 'Test Member',
      billMonth: month,
      baseAmount: base,
      lateFeeAmount: lateFee,
      totalAmount: base + lateFee,
      amountPaid: paid,
      status: PaymentStatus.UNPAID,
      dueDate: '10 ${month.substring(0, 3)} ${month.substring(month.length - 4)}',
    );

List<MaintenanceBill> _fiveMonthsOf(double amount) => [
      _bill('April 2026', amount),
      _bill('May 2026', amount),
      _bill('June 2026', amount),
      _bill('July 2026', amount),
      _bill('August 2026', amount),
    ];

void main() {
  // The app does this in main() before any month label is parsed or formatted.

  test('a part payment clears the oldest months and leaves the rest pending', () {
    // Unit 202: five months at 1,100 each, nothing paid, 5,500 outstanding.
    final bills = _fiveMonthsOf(1100.0);

    final allocation = allocatePayment(bills, 3300.0);

    expect(allocation.applied, closeTo(3300.0, _eps));
    expect(allocation.excess, closeTo(0.0, _eps));
    expect(allocation.settledCount, 3);
    expect(allocation.months, ['April 2026', 'May 2026', 'June 2026']);
    // What is left is still owed, to the rupee.
    expect(allocation.allocations.fold<double>(0.0, (s, a) => s + a.pendingAfter), closeTo(2200.0, _eps));
  });

  test('an amount that stops mid-month leaves that month partly paid', () {
    final bills = _fiveMonthsOf(1100.0);

    final allocation = allocatePayment(bills, 2500.0);

    expect(allocation.settledCount, 2);
    final third = allocation.allocations.firstWhere((a) => a.bill.billMonth == 'June 2026');
    expect(third.applied, closeTo(300.0, _eps));
    expect(third.pendingAfter, closeTo(800.0, _eps));
    expect(third.isSettled, isFalse);
  });

  test('paying everything owed settles every month and leaves nothing over', () {
    final bills = _fiveMonthsOf(1100.0);

    final allocation = allocatePayment(bills, 5500.0);

    expect(allocation.settledCount, 5);
    expect(allocation.excess, closeTo(0.0, _eps));
    expect(allocation.allocations.every((a) => a.isSettled), isTrue);
  });

  test('an overpayment is carried forward rather than forced onto the last bill', () {
    final bills = _fiveMonthsOf(1100.0);

    final allocation = allocatePayment(bills, 6000.0);

    expect(allocation.applied, closeTo(5500.0, _eps));
    expect(allocation.excess, closeTo(500.0, _eps));
    // No bill shows more collected against it than it ever charged.
    expect(allocation.allocations.every((a) => a.applied <= a.bill.payableTotal), isTrue);
  });

  test('a payment against a unit that owes nothing becomes credit in full', () {
    final settled = [_bill('April 2026', 1100.0, paid: 1100.0)];

    final allocation = allocatePayment(settled, 1100.0);

    expect(allocation.applied, closeTo(0.0, _eps));
    expect(allocation.excess, closeTo(1100.0, _eps));
    expect(allocation.touched, isEmpty);
  });

  test('a partly paid month is topped up only by what it still owes', () {
    final bills = [
      _bill('April 2026', 1100.0, paid: 900.0),
      _bill('May 2026', 1100.0),
    ];

    final allocation = allocatePayment(bills, 500.0);

    final april = allocation.allocations.firstWhere((a) => a.bill.billMonth == 'April 2026');
    expect(april.applied, closeTo(200.0, _eps));
    expect(april.isSettled, isTrue);
    final may = allocation.allocations.firstWhere((a) => a.bill.billMonth == 'May 2026');
    expect(may.applied, closeTo(300.0, _eps));
  });

  test('the late fee is settled along with the month it sits on', () {
    final withLateFee = [_bill('April 2026', 1100.0, lateFee: 22.0)];

    final allocation = allocatePayment(withLateFee, 1122.0);

    expect(allocation.settledCount, 1);
    expect(allocation.excess, closeTo(0.0, _eps));
  });

  test('nothing is taken from a payment of zero', () {
    final allocation = allocatePayment(_fiveMonthsOf(1100.0), 0.0);

    expect(allocation.applied, closeTo(0.0, _eps));
    expect(allocation.excess, closeTo(0.0, _eps));
    expect(allocation.touched, isEmpty);
  });

  test('months are settled in calendar order, not the order they were entered', () {
    final jumbled = [
      _bill('June 2026', 1100.0),
      _bill('April 2026', 1100.0),
      _bill('May 2026', 1100.0),
    ];

    expect(oldestMonthFirst(jumbled).map((b) => b.billMonth).toList(), ['April 2026', 'May 2026', 'June 2026']);
    expect(allocatePayment(jumbled, 1100.0).months, ['April 2026']);
  });

  test('months that straddle a year end still sort oldest first', () {
    final acrossYears = [
      _bill('January 2027', 1100.0),
      _bill('December 2026', 1100.0),
      _bill('March 2026', 1100.0),
    ];

    expect(
      oldestMonthFirst(acrossYears).map((b) => b.billMonth).toList(),
      ['March 2026', 'December 2026', 'January 2027'],
    );
  });

  test('the dues a member is shown separate this month from the arrears behind it', () {
    final bills = [
      _bill('April 2026', 1100.0),
      _bill('May 2026', 1100.0, paid: 600.0),
      _bill('June 2026', 1200.0),
    ];

    final dues = unitDues(bills);

    expect(dues.previousPending, closeTo(1600.0, _eps));
    expect(dues.currentBill, closeTo(1200.0, _eps));
    expect(dues.currentPending, closeTo(1200.0, _eps));
    expect(dues.grossOutstanding, closeTo(2800.0, _eps));
    expect(dues.netPayable, closeTo(2800.0, _eps));
    expect(dues.isSettled, isFalse);
  });

  test('credit already paid in is taken off what the member is asked for', () {
    final bills = [_bill('April 2026', 1100.0), _bill('May 2026', 1100.0)];

    final dues = unitDues(bills, 500.0);

    expect(dues.grossOutstanding, closeTo(2200.0, _eps));
    expect(dues.netPayable, closeTo(1700.0, _eps));
    expect(dues.creditInHand, closeTo(0.0, _eps));
  });

  test('credit larger than the arrears leaves the member owing nothing', () {
    final bills = [_bill('April 2026', 1100.0)];

    final dues = unitDues(bills, 1800.0);

    expect(dues.netPayable, closeTo(0.0, _eps));
    expect(dues.creditInHand, closeTo(700.0, _eps));
    expect(dues.isSettled, isTrue);
  });

  test('a unit with no bills at all owes nothing and shows nothing', () {
    final dues = unitDues(const []);

    expect(dues.currentBill, closeTo(0.0, _eps));
    expect(dues.grossOutstanding, closeTo(0.0, _eps));
    expect(dues.isSettled, isTrue);
  });
}
